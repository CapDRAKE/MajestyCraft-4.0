package fr.majestycraft.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;

/**
 * Cache disque des réponses de recherche Modrinth (mods et shaders).
 *
 * Objectif : quand l'API Modrinth est indisponible (panne, réseau coupé), le
 * catalogue reste consultable en servant la dernière réponse mise en cache pour
 * la même requête. Chaque requête réussie écrase son entrée ; les entrées ne
 * périment jamais d'elles-mêmes (une réponse « vieille » vaut mieux qu'un
 * catalogue vide), elles sont simplement rafraîchies au prochain succès.
 */
public final class ModrinthCache {

    private final File dir;

    public ModrinthCache(File baseCacheDir) {
        this.dir = new File(baseCacheDir, "modrinth");
        try { this.dir.mkdirs(); } catch (Exception ignored) {}
    }

    /** @return le JSON en cache pour cette clé, ou null s'il n'existe pas. */
    public String get(String key) {
        File f = fileFor(key);
        if (f == null || !f.isFile()) return null;
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** Écrit (ou remplace) l'entrée de cache pour cette clé. */
    public void put(String key, String json) {
        File f = fileFor(key);
        if (f == null || json == null) return;
        try {
            File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
            Files.write(tmp.toPath(), json.getBytes(StandardCharsets.UTF_8));
            tmp.renameTo(f);
        } catch (Exception ignored) {}
    }

    private File fileFor(String key) {
        String hash = sha1(key);
        return hash == null ? null : new File(dir, hash + ".json");
    }

    private static String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
