package fr.majestycraft.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import fr.majestycraft.Utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client minimal de l'API Modrinth (v2), partagé par le diagnostic de crash, le mode
 * performance, les mises à jour de mods et le partage de profil.
 *
 * Toutes les méthodes sont bloquantes : à appeler hors du thread JavaFX.
 */
public final class ModrinthApi {

    private static final String API = "https://api.modrinth.com/v2";
    /** Modrinth demande un User-Agent identifiant l'application. */
    private static final String USER_AGENT = "MajestyCraft/MajestyLauncher (majestylauncher.com)";

    private ModrinthApi() {
    }

    /** Loaders Modrinth acceptés pour un modloader du launcher (Quilt charge aussi les mods Fabric). */
    public static List<String> loadersFor(Utils.ModloaderType type) {
        if (type == null) return Collections.emptyList();
        switch (type) {
            case FABRIC:
                return Collections.singletonList("fabric");
            case QUILT:
                return Arrays.asList("quilt", "fabric");
            case FORGE:
                return Collections.singletonList("forge");
            case NEOFORGE:
                return Collections.singletonList("neoforge");
            default:
                return Collections.emptyList();
        }
    }

    /** Versions Modrinth des fichiers donnés (clé : empreinte SHA-1). Les fichiers inconnus sont absents. */
    public static Map<String, JsonObject> versionsByHash(Collection<String> sha1s) throws IOException {
        if (sha1s == null || sha1s.isEmpty()) return Collections.emptyMap();
        JsonObject body = new JsonObject();
        body.add("hashes", toArray(sha1s));
        body.addProperty("algorithm", "sha1");
        return toMap(request("POST", API + "/version_files", body.toString()));
    }

    /** Dernière version compatible (version de Minecraft + loader) de chaque fichier donné. */
    public static Map<String, JsonObject> latestByHash(Collection<String> sha1s, List<String> loaders, String mcVersion) throws IOException {
        if (sha1s == null || sha1s.isEmpty()) return Collections.emptyMap();
        JsonObject body = new JsonObject();
        body.add("hashes", toArray(sha1s));
        body.addProperty("algorithm", "sha1");
        body.add("loaders", toArray(loaders));
        body.add("game_versions", toArray(Collections.singletonList(mcVersion)));
        return toMap(request("POST", API + "/version_files/update", body.toString()));
    }

    /**
     * Meilleure version d'un projet pour cette version de Minecraft et ces loaders :
     * la plus récente « release », à défaut la plus récente tout court. Null si aucune.
     */
    public static JsonObject bestVersion(String projectIdOrSlug, List<String> loaders, String mcVersion) throws IOException {
        String url = API + "/project/" + enc(projectIdOrSlug) + "/version"
                + "?loaders=" + enc(toArray(loaders).toString())
                + "&game_versions=" + enc(toArray(Collections.singletonList(mcVersion)).toString());
        JsonElement res;
        try {
            res = request("GET", url, null);
        } catch (NotFoundException e) {
            return null;
        }
        if (res == null || !res.isJsonArray() || res.getAsJsonArray().size() == 0) return null;
        JsonArray versions = res.getAsJsonArray();
        for (JsonElement v : versions) {
            if ("release".equals(str(v.getAsJsonObject(), "version_type"))) return v.getAsJsonObject();
        }
        return versions.get(0).getAsJsonObject();
    }

    /** Versions Modrinth par identifiants. */
    public static List<JsonObject> versionsByIds(Collection<String> ids) throws IOException {
        List<JsonObject> out = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return out;
        JsonElement res = request("GET", API + "/versions?ids=" + enc(toArray(ids).toString()), null);
        if (res != null && res.isJsonArray()) {
            for (JsonElement e : res.getAsJsonArray()) out.add(e.getAsJsonObject());
        }
        return out;
    }

    /** Titres des projets (clé : identifiant de projet). */
    public static Map<String, String> projectTitles(Collection<String> projectIds) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        if (projectIds == null || projectIds.isEmpty()) return out;
        JsonElement res = request("GET", API + "/projects?ids=" + enc(toArray(projectIds).toString()), null);
        if (res != null && res.isJsonArray()) {
            for (JsonElement e : res.getAsJsonArray()) {
                JsonObject p = e.getAsJsonObject();
                out.put(str(p, "id"), str(p, "title"));
            }
        }
        return out;
    }

    /** Projet Modrinth (titre, slug, id...) ou null s'il n'existe pas. */
    public static JsonObject project(String idOrSlug) throws IOException {
        try {
            JsonElement res = request("GET", API + "/project/" + enc(idOrSlug), null);
            return res != null && res.isJsonObject() ? res.getAsJsonObject() : null;
        } catch (NotFoundException e) {
            return null;
        }
    }

    /** Fichier principal d'une version (celui marqué « primary », sinon le premier). */
    public static JsonObject primaryFile(JsonObject version) {
        if (version == null || !version.has("files")) return null;
        JsonArray files = version.getAsJsonArray("files");
        for (JsonElement f : files) {
            if (f.getAsJsonObject().has("primary") && f.getAsJsonObject().get("primary").getAsBoolean()) return f.getAsJsonObject();
        }
        return files.size() > 0 ? files.get(0).getAsJsonObject() : null;
    }

    /** Identifiants de projets des dépendances obligatoires d'une version. */
    public static List<String> requiredDependencies(JsonObject version) {
        List<String> out = new ArrayList<>();
        if (version == null || !version.has("dependencies")) return out;
        for (JsonElement e : version.getAsJsonArray("dependencies")) {
            JsonObject d = e.getAsJsonObject();
            String projectId = str(d, "project_id");
            if ("required".equals(str(d, "dependency_type")) && projectId != null && !out.contains(projectId)) {
                out.add(projectId);
            }
        }
        return out;
    }

    /** La version liste-t-elle cette version de Minecraft ? */
    public static boolean supportsGameVersion(JsonObject version, String mcVersion) {
        return contains(version, "game_versions", mcVersion);
    }

    /** La version liste-t-elle un de ces loaders ? */
    public static boolean supportsAnyLoader(JsonObject version, List<String> loaders) {
        for (String loader : loaders) {
            if (contains(version, "loaders", loader)) return true;
        }
        return false;
    }

    /**
     * Télécharge le fichier dans le dossier (nom d'origine, éventuellement suivi de
     * « .disabled ») en vérifiant son empreinte SHA-1. Le fichier n'apparaît qu'une fois
     * complet et vérifié.
     */
    public static File download(JsonObject file, File dir, boolean disabled) throws IOException {
        String url = str(file, "url");
        String name = str(file, "filename");
        if (url == null || name == null || name.contains("/") || name.contains("\\")) {
            throw new IOException("Fichier Modrinth invalide");
        }
        String expected = file.has("hashes") ? str(file.getAsJsonObject("hashes"), "sha1") : null;
        dir.mkdirs();
        File target = new File(dir, disabled ? name + ".disabled" : name);
        File temp = new File(dir, name + ".part");
        HttpURLConnection c = open(url, "GET");
        try (InputStream in = new BufferedInputStream(c.getInputStream()); OutputStream out = new FileOutputStream(temp)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        if (expected != null && !expected.equalsIgnoreCase(sha1(temp))) {
            temp.delete();
            throw new IOException("Fichier corrompu : " + name);
        }
        if (target.exists() && !target.delete()) {
            temp.delete();
            throw new IOException("Impossible de remplacer " + target.getName());
        }
        if (!temp.renameTo(target)) {
            temp.delete();
            throw new IOException("Impossible d'enregistrer " + target.getName());
        }
        return target;
    }

    public static String sha1(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    public static String str(JsonObject o, String key) {
        return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    // ------------------------------------------------------------------ HTTP

    /** Réponse 404 de Modrinth (projet ou version inexistants). */
    public static final class NotFoundException extends IOException {
        NotFoundException(String url) {
            super("Introuvable : " + url);
        }
    }

    private static JsonElement request(String method, String url, String body) throws IOException {
        HttpURLConnection c = open(url, method);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            try (OutputStream out = c.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        int code = c.getResponseCode();
        if (code == 404) throw new NotFoundException(url);
        if (code < 200 || code >= 300) throw new IOException("Modrinth a répondu " + code);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new JsonParser().parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    private static HttpURLConnection open(String url, String method) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(10000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", USER_AGENT);
        c.setRequestProperty("Accept", "application/json");
        return c;
    }

    private static Map<String, JsonObject> toMap(JsonElement res) {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        if (res != null && res.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : res.getAsJsonObject().entrySet()) {
                if (e.getValue().isJsonObject()) out.put(e.getKey(), e.getValue().getAsJsonObject());
            }
        }
        return out;
    }

    private static JsonArray toArray(Collection<String> values) {
        JsonArray a = new JsonArray();
        for (String v : values) a.add(new JsonPrimitive(v));
        return a;
    }

    private static boolean contains(JsonObject o, String key, String value) {
        if (o == null || value == null || !o.has(key)) return false;
        for (JsonElement e : o.getAsJsonArray(key)) {
            if (value.equalsIgnoreCase(e.getAsString())) return true;
        }
        return false;
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
