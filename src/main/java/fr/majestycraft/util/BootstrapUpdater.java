package fr.majestycraft.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.zip.ZipFile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Met à jour le bootstrap installé chez le joueur (le petit programme qui télécharge puis
 * lance le launcher). Personne n'a à réinstaller : le launcher, lui-même tenu à jour par le
 * bootstrap, remplace bootstrap.jar par la version publiée, seulement si elle est signée
 * par MajestyCraft. Le nouveau bootstrap n'installe ensuite que des launchers signés.
 */
public final class BootstrapUpdater {

    private static final String BASE = "https://majestycraft.com/minecraft/bootstrap/";

    /** Clé publique RSA 3072 bits de MajestyCraft (la même que dans le bootstrap). */
    private static final String MODULUS = "zj1kJidaXhpKL19rJGk89DhrEUegjp0E00xy1Sk+gkJYHf1VV0mfAU+ubSwGR4QuEXla2hmHo2RTxJOYdURxI0FAk6vb97VssYW8UxSr403rweYXj23sxWYeQ75UUtOYXNEom/qemJpoQ+UksVz9fwB0YVGC46xfukDnw32aLw25OZZ7H4dd7gZ2EXCA16fS4PanLvuucWgFUtyf61Avy5JGmvwIGvpXHaOXVCepMmqP2bMGnD0lnjbszq/8JBAnP4+U8NxkAagoNIR7RrsJKDpr6cJOOfXdx+9baEndBJx2juG1w5G0xzpxZKNsWR6PtjHgzLQrOrYgG83X66ypZUWnW7CTuTRehbavcqwb/tKESpAHKfJuhrzUcu9dHIy1elsR/W+5H05B2ozNIlLxH+oW4cWh1+KY7NY18z3XofpsNVvS4zZ2XiD0ftKJWFqzDh6fiSUAUrqHBCRG4D68BlxxVvjVd7pUAjLVjLldyTQXHIULZlTnK3p8YCsp0yuZ";
    private static final String EXPONENT = "AQAB";

    private BootstrapUpdater() {
    }

    /**
     * Vérifie en arrière-plan, juste après le démarrage : le bootstrap se ferme 1,5 s après
     * avoir lancé le launcher, son fichier est alors libre. Si Windows le tient encore, nouvel
     * essai un peu plus tard ; sinon au prochain lancement.
     */
    public static void checkInBackground() {
        Thread t = new Thread(() -> {
            for (long attente : new long[] { 3_000, 10_000, 30_000 }) {
                try {
                    Thread.sleep(attente);
                    update(BASE);
                    return;
                } catch (InterruptedException e) {
                    return;
                } catch (Throwable e) {
                    System.err.println("Mise à jour du bootstrap impossible pour l'instant : " + e);
                }
            }
        }, "MajestyLauncher-BootstrapUpdate");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    static void update(String base) throws Exception {
        File installed = installedBootstrap();
        if (installed == null) return;
        JsonObject info = JsonParser.parseString(fetchText(base + "bootstrap.json")).getAsJsonObject();
        String md5 = info.has("md5") ? info.get("md5").getAsString() : null;
        String sig = info.has("sig") ? info.get("sig").getAsString() : null;
        if (md5 == null || sig == null || md5.equalsIgnoreCase(md5(installed))) return;

        File temp = new File(installed.getParentFile(), "bootstrap.jar.part");
        try {
            download(base + "bootstrap.jar", temp);
            if (!isSigned(temp, md5, sig) || !isBootstrap(temp)) {
                System.err.println("Bootstrap publié non vérifié : il n'est pas installé.");
                return;
            }
            try {
                Files.move(temp.toPath(), installed.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp.toPath(), installed.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("Bootstrap mis à jour : " + installed);
        } finally {
            temp.delete();
        }
    }

    /**
     * Le bootstrap lance le launcher avec le Java installé à côté de lui : dossier
     * d'installation/jre et dossier d'installation/bootstrap.jar, sur tous les systèmes.
     * Rien en développement (Java du JDK).
     */
    static File installedBootstrap() {
        File jre = new File(System.getProperty("java.home"));
        File candidate = new File(jre.getParentFile(), "bootstrap.jar");
        return candidate.isFile() && candidate.canWrite() && isBootstrap(candidate) ? candidate : null;
    }

    /** C'est bien le bootstrap de MajestyCraft, et pas un autre programme du même nom. */
    private static boolean isBootstrap(File jar) {
        try (ZipFile zip = new ZipFile(jar)) {
            return zip.getEntry("fr/trxyy/alternative/bootstrap/Home.class") != null;
        } catch (IOException e) {
            return false;
        }
    }

    /** Fichier complet (MD5) et signé (RSA SHA-256) par la clé de MajestyCraft. */
    static boolean isSigned(File file, String md5, String signatureBase64) {
        try {
            Base64.Decoder b64 = Base64.getDecoder();
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                    new BigInteger(1, b64.decode(MODULUS)), new BigInteger(1, b64.decode(EXPONENT))));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (InputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[65536];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    signature.update(buffer, 0, read);
                    digest.update(buffer, 0, read);
                }
            }
            return String.format("%032x", new BigInteger(1, digest.digest())).equalsIgnoreCase(md5.trim())
                    && signature.verify(b64.decode(signatureBase64.trim()));
        } catch (Exception e) {
            return false;
        }
    }

    private static String md5(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int read;
            while ((read = in.read(buffer)) > 0) digest.update(buffer, 0, read);
        }
        return String.format("%032x", new BigInteger(1, digest.digest()));
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", "MajestyCraft-Launcher");
        c.setConnectTimeout(10_000);
        c.setReadTimeout(30_000);
        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode() + " " + url);
        return c;
    }

    private static String fetchText(String url) throws IOException {
        try (InputStream in = new BufferedInputStream(open(url).getInputStream())) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void download(String url, File target) throws IOException {
        try (InputStream in = new BufferedInputStream(open(url).getInputStream());
             OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[65536];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
        }
    }
}
