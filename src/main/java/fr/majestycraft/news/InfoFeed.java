package fr.majestycraft.news;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fournit le contenu du panel Infos :
 * - le changelog détaillé, embarqué dans le launcher (resources/changelog.json) —
 *   il décrit les versions du launcher, il vit donc avec le launcher ;
 * - les news du serveur, récupérées directement du site majestycraft.com via
 *   l'API publique du CMS Azuriom (/api/posts) : le staff publie sur le site,
 *   le launcher les affiche sans rien d'autre à maintenir.
 *
 * Les chargements réseau sont à timeout court et doivent être appelés hors du
 * thread JavaFX.
 */
public final class InfoFeed {

    private static final String NEWS_API_URL = "https://majestycraft.com/api/posts";
    private static final String LOCAL_CHANGELOG = "/resources/changelog.json";
    private static final String USER_AGENT = "MajestyLauncher";
    private static final int MAX_NEWS = 12;

    private static final Gson GSON = new Gson();

    private InfoFeed() {
    }

    /* ─────────────────────────── Modèles ─────────────────────────── */

    /** Une entrée de changelog : type ("new", "fix", "improve") + texte. */
    public static class Entry {
        public String type;
        public String text;

        /** Libellé court affiché dans la pastille. */
        public String badge() {
            if (type == null) return "INFO";
            switch (type.toLowerCase()) {
                case "new": return "NOUVEAU";
                case "fix": return "CORRECTIF";
                case "improve": return "AMÉLIORÉ";
                default: return type.toUpperCase();
            }
        }

        /** Couleur de la pastille selon le type. */
        public String badgeColor() {
            if (type == null) return "#8a8a9a";
            switch (type.toLowerCase()) {
                case "new": return "#5bd97a";
                case "fix": return "#ff7a7a";
                case "improve": return "#4dd2ff";
                default: return "#8a8a9a";
            }
        }
    }

    /** Une version publiée avec ses entrées. */
    public static class Release {
        public String version;
        public String date;
        public List<Entry> entries;
    }

    public static class Changelog {
        public List<Release> versions;
    }

    /** Une annonce du serveur (mappée depuis un post Azuriom). */
    public static class NewsItem {
        public String title;
        public String date;
        public String body;
        public String url;
    }

    public static class News {
        public List<NewsItem> items;
    }

    /* ─────────────────────────── Changelog (local) ─────────────────────────── */

    /**
     * @return le changelog embarqué dans le jar, sinon une liste vide
     *         (jamais null : le panel doit toujours pouvoir s'afficher).
     */
    public static Changelog loadChangelog() {
        try (InputStream in = InfoFeed.class.getResourceAsStream(LOCAL_CHANGELOG)) {
            if (in != null) {
                Changelog local = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Changelog.class);
                if (local != null && local.versions != null) {
                    return local;
                }
            }
        } catch (Exception ignored) {
        }

        Changelog empty = new Changelog();
        empty.versions = new ArrayList<>();
        return empty;
    }

    /* ─────────────────────────── News (API du site) ─────────────────────────── */

    /**
     * Récupère les annonces publiées sur majestycraft.com (API Azuriom /api/posts).
     * @return les news, ou null si le site est injoignable (le panel affiche alors
     *         un état vide explicite plutôt que des données inventées).
     */
    public static News loadNews() {
        try {
            JsonElement parsed = JsonParser.parseString(fetch(NEWS_API_URL));
            if (!parsed.isJsonArray()) return null;

            JsonArray posts = parsed.getAsJsonArray();
            News news = new News();
            news.items = new ArrayList<>();

            for (JsonElement el : posts) {
                if (!el.isJsonObject()) continue;
                JsonObject post = el.getAsJsonObject();

                NewsItem item = new NewsItem();
                item.title = getString(post, "title");
                item.body = getString(post, "description");
                item.url = getString(post, "url");
                item.date = formatDate(getString(post, "published_at"));

                if (item.title != null && !item.title.trim().isEmpty()) {
                    news.items.add(item);
                }
                if (news.items.size() >= MAX_NEWS) break;
            }
            return news;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    /** "2026-07-20T17:03:54+00:00" -> "20/07/2026" (best effort, sinon brut). */
    private static String formatDate(String iso) {
        if (iso == null || iso.length() < 10) return iso;
        try {
            String datePart = iso.substring(0, 10); // yyyy-MM-dd
            String[] parts = datePart.split("-");
            return parts[2] + "/" + parts[1] + "/" + parts[0];
        } catch (Exception e) {
            return iso;
        }
    }

    private static String fetch(String urlStr) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(urlStr).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(6000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
