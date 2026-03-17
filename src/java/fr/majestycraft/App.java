package fr.majestycraft;

import fr.majestycraft.launcher.*;
import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.maintenance.*;
import fr.trxyy.alternative.alternative_api.utils.*;
import fr.trxyy.alternative.alternative_api.utils.config.EnumConfig;
import fr.trxyy.alternative.alternative_api.utils.file.FileUtil;
import fr.trxyy.alternative.alternative_api_ui.*;
import fr.trxyy.alternative.alternative_api_ui.base.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.*;

/**
 * Classe principale de l'application MajestyLauncher
 */
public class App extends AlternativeBase {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    private static final String PARTNER_IP = "91.197.6.34";
    private static final String PARTNER_PORT = "25601";
    private static final String ICON_IMAGE = "launchergifpng.png";

    // Base serveur (SANS slash final pour maîtriser les concat)
    private static final String GAME_LINK_BASE_URL = "https://majestycraft.com/minecraft";

    // Default si aucune config
    private static final String DEFAULT_VERSION_ID = "1.21.11";

    // Mojang manifests
    private static final String MOJANG_MANIFEST_PRIMARY =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String MOJANG_MANIFEST_FALLBACK =
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    // Sources OptiFine
    private static final String OPTIFINE_DOWNLOADS_URL = "https://optifine.net/downloads";
    private static final String OPTIFINE_SITE_BASE = "https://optifine.net/";
    private static final String MULTIMC_LAUNCHWRAPPER_OF_MAVEN_BASE =
            "https://files.multimc.org/maven/net/minecraft/launchwrapper/";

    private static final Pattern OPTIFINE_REMOTE_FILE_PATTERN = Pattern.compile(
            "adloadx\\?f=((?:preview_)?OptiFine_([0-9][0-9.]+)_HD_U_[A-Za-z0-9_]+\\.jar)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern OPTIFINE_DOWNLOAD_LINK_PATTERN = Pattern.compile(
            "href\\s*=\\s*[\"'](downloadx\\?f=[^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
    );

    private static App instance;
    private Scene scene;

    private final GameFolder gameFolder = createGameFolder();
    private final LauncherPreferences launcherPreferences = createLauncherPreferences();

    // Valeur par défaut “safe” au boot (on ré-écrase via config ensuite)
    private final GameLinks gameLinks = createDefaultGameLinks();
    private final GameEngine gameEngine = createGameEngine();
    private final GameMaintenance gameMaintenance = createGameMaintenance();
    private LauncherPanel panel;

    private static final GameConnect GAME_CONNECT = new GameConnect(PARTNER_IP, PARTNER_PORT);

    // 2 threads : un pour netIsAvailable, un pour les résolutions Mojang / autres tâches
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(2);

    /**
     * Lance le launcher.
     */
    public void launcher() {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        setInstance(this);
        try {
            createContent();

            // IMPORTANT : applique la config (version + forge/opti) dès le démarrage
            applyGameLinksFromConfigAsync();

            registerGameEngine(primaryStage);

            LauncherBase launcherBase = setupLauncherBase(primaryStage);
            launcherBase.setIconImage(primaryStage, ICON_IMAGE);

            Platform.runLater(Main::showStartupPopup);
        } catch (IOException e) {
            LOGGER.severe("Erreur lors de la création du contenu: " + e.getMessage());
        }
    }

    private GameFolder createGameFolder() {
        return new GameFolder("majestycraft");
    }

    private LauncherPreferences createLauncherPreferences() {
        return new LauncherPreferences("MajestyLauncher Optifine + Forge", 1050, 750, Mover.MOVE);
    }

    /**
     * GameLinks par défaut (avant lecture config)
     * -> correspond à la structure /<version>/<version>.json
     */
    private GameLinks createDefaultGameLinks() {
        String base = GAME_LINK_BASE_URL + "/" + DEFAULT_VERSION_ID + "/";
        String jsonName = DEFAULT_VERSION_ID + ".json";
        return new GameLinks(base, jsonName);
    }

    private GameEngine createGameEngine() {
        return new GameEngine(gameFolder, gameLinks, launcherPreferences, GameStyle.VANILLA_1_19_HIGHER);
    }

    private GameMaintenance createGameMaintenance() {
        return new GameMaintenance(Maintenance.USE, gameEngine);
    }

    private void createContent() throws IOException {
        LauncherPane contentPane = new LauncherPane(this.gameEngine);
        scene = new Scene(contentPane, launcherPreferences.getWidth(), launcherPreferences.getHeight());

        Rectangle clipRect = new Rectangle(launcherPreferences.getWidth(), launcherPreferences.getHeight());
        clipRect.setArcWidth(15.0);
        clipRect.setArcHeight(15.0);
        contentPane.setClip(clipRect);
        contentPane.setStyle("-fx-background-color: transparent;");

        setPanel(new LauncherPanel(contentPane, this.gameEngine));
    }

    private void registerGameEngine(Stage primaryStage) {
        gameEngine.reg(primaryStage);
        if (netIsAvailable()) {
            gameEngine.reg(gameMaintenance);
        }
    }

    private LauncherBase setupLauncherBase(Stage primaryStage) {
        return new LauncherBase(primaryStage, scene, StageStyle.TRANSPARENT, this.gameEngine);
    }

    /**
     * Applique les GameLinks selon la config sauvegardée.
     */
    private void applyGameLinksFromConfigAsync() {
        if (this.panel == null || this.panel.getConfig() == null) {
            return;
        }

        this.panel.getConfig().loadConfiguration();

        String version = (String) this.panel.getConfig().getValue(EnumConfig.VERSION);
        if (version == null || version.trim().isEmpty()) {
            version = DEFAULT_VERSION_ID;
        }

        boolean useForge = getBooleanConfig(EnumConfig.USE_FORGE);
        boolean useOptifine = getBooleanConfig(EnumConfig.USE_OPTIFINE);

        final String finalVersion = version;
        final boolean finalUseForge = useForge;
        final boolean finalUseOptifine = useOptifine;

        EXECUTOR_SERVICE.submit(() -> {
            try {
                if (finalUseOptifine) {
                    String mojangJsonUrl = resolveMojangVersionJsonUrlStatic(finalVersion);
                    if (mojangJsonUrl == null) {
                        throw new IOException("JSON Mojang introuvable pour OptiFine " + finalVersion);
                    }

                    try {
                        ensureOptiFineRuntime(finalVersion);
                    } catch (Exception e) {
                        LOGGER.warning("Impossible de préparer OptiFine " + finalVersion + " : " + e.getMessage());
                    }

                    GameLinks links = buildOptiFineGameLinksFromResolvedJson(mojangJsonUrl, finalVersion);
                    Platform.runLater(() -> this.gameEngine.reg(links));
                    return;
                }

                if (!netIsAvailable()) {
                    GameLinks fallback = buildServerGameLinks(finalVersion, finalUseForge);
                    Platform.runLater(() -> this.gameEngine.reg(fallback));
                    return;
                }

                String mojangJsonUrl = resolveMojangVersionJsonUrlStatic(finalVersion);
                if (mojangJsonUrl == null) {
                    GameLinks fallback = buildServerGameLinks(finalVersion, finalUseForge);
                    Platform.runLater(() -> this.gameEngine.reg(fallback));
                    return;
                }

                String serverBaseForVersion = GAME_LINK_BASE_URL + "/" + finalVersion + (finalUseForge ? "/forge/" : "/");

                GameLinks links = new GameLinks(
                        mojangJsonUrl,
                        serverBaseForVersion + "ignore.cfg",
                        serverBaseForVersion + "delete.cfg",
                        serverBaseForVersion + "status.cfg",
                        serverBaseForVersion + "files/"
                );

                Platform.runLater(() -> this.gameEngine.reg(links));

            } catch (Exception e) {
                LOGGER.warning("Impossible d'appliquer les GameLinks configurés : " + e.getMessage());
                if (!finalUseOptifine) {
                    GameLinks fallback = buildServerGameLinks(finalVersion, finalUseForge);
                    Platform.runLater(() -> this.gameEngine.reg(fallback));
                }
            }
        });
    }

    private boolean getBooleanConfig(EnumConfig key) {
        Object v = this.panel.getConfig().getValue(key);
        return (v instanceof Boolean) ? (Boolean) v : false;
    }

    /**
     * Forge => /<version>/forge/
     * Vanilla => /<version>/
     */
    private GameLinks buildServerGameLinks(String version, boolean forge) {
        String base = GAME_LINK_BASE_URL + "/" + version + (forge ? "/forge/" : "/");
        String jsonName = version + ".json";
        return new GameLinks(base, jsonName);
    }

    public static String resolveMojangVersionJsonUrlStatic(String versionId) throws IOException {
        try {
            String url = resolveMojangVersionJsonUrlFromManifest(MOJANG_MANIFEST_PRIMARY, versionId);
            if (url != null) return url;
        } catch (IOException ignored) { }

        return resolveMojangVersionJsonUrlFromManifest(MOJANG_MANIFEST_FALLBACK, versionId);
    }

    /**
     * Résout l'URL Mojang du JSON de version via le manifest.
     * Retourne null si non trouvé.
     */
    private static String resolveMojangVersionJsonUrlFromManifest(String manifestUrl, String versionId) throws IOException {
        String json = downloadTextStatic(manifestUrl);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray versions = root.getAsJsonArray("versions");
        if (versions == null) return null;

        for (JsonElement el : versions) {
            JsonObject o = el.getAsJsonObject();
            if (o == null) continue;

            String id = o.has("id") ? o.get("id").getAsString() : null;
            if (versionId.equals(id)) {
                return o.has("url") ? o.get("url").getAsString() : null;
            }
        }
        return null;
    }

    public static Set<String> fetchAvailableOptiFineVersions() throws IOException {
        return new LinkedHashSet<>(fetchLatestOptiFineRemoteFilesByVersion().keySet());
    }

    public static void ensureOptiFineRuntime(String version) throws IOException {
        String remoteFileName = fetchLatestOptiFineRemoteFilesByVersion().get(version);
        if (remoteFileName == null || remoteFileName.trim().isEmpty()) {
            throw new IOException("Aucun build OptiFine officiel trouvé pour Minecraft " + version);
        }

        String artifactVersion = toLocalOptiFineArtifactVersion(remoteFileName);
        Path targetJar = getOptiFineLibrariesRoot()
                .resolve(artifactVersion)
                .resolve("OptiFine-" + artifactVersion + ".jar");

        cleanupOptiFineLibrariesKeeping(artifactVersion);

        if (!Files.exists(targetJar) || Files.size(targetJar) == 0L) {
            Files.createDirectories(targetJar.getParent());
            downloadOfficialOptiFineJar(remoteFileName, targetJar);
        }

        ensureLaunchWrapperOfInstalled(version, targetJar);
    }

    public static GameLinks buildOptiFineGameLinks(String version) throws IOException {
        String mojangJsonUrl = resolveMojangVersionJsonUrlStatic(version);
        if (mojangJsonUrl == null || mojangJsonUrl.trim().isEmpty()) {
            throw new IOException("JSON Mojang introuvable pour " + version);
        }
        return buildOptiFineGameLinksFromResolvedJson(mojangJsonUrl, version);
    }

    public static GameLinks buildOptiFineGameLinksFromResolvedJson(String mojangJsonUrl, String version) throws IOException {
        return buildLocalOnlyGameLinks(mojangJsonUrl, version, "optifine");
    }

    private static GameLinks buildLocalOnlyGameLinks(String mojangJsonUrl, String version, String profile) throws IOException {
        Path base = getLauncherRootPath().resolve(Paths.get("cache", "links", profile, version));
        Path ignore = base.resolve("ignore.cfg");
        Path delete = base.resolve("delete.cfg");
        Path status = base.resolve("status.cfg");

        Files.createDirectories(base);
        writeTextFile(ignore, "");
        writeTextFile(delete, "");
        writeTextFile(status, "Ok\n");

        return new GameLinks(
                mojangJsonUrl,
                ignore.toUri().toString(),
                delete.toUri().toString(),
                status.toUri().toString(),
                null
        );
    }

    private static LinkedHashMap<String, String> fetchLatestOptiFineRemoteFilesByVersion() throws IOException {
        String html = downloadTextStatic(OPTIFINE_DOWNLOADS_URL);
        Matcher matcher = OPTIFINE_REMOTE_FILE_PATTERN.matcher(html);

        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        while (matcher.find()) {
            String remoteFileName = htmlDecode(matcher.group(1));
            String versionFound = matcher.group(2);
            if (!out.containsKey(versionFound)) {
                out.put(versionFound, remoteFileName);
            }
        }

        return out;
    }

    private static void ensureLaunchWrapperOfInstalled(String minecraftVersion, Path downloadedOptiFineJar) throws IOException {
        String embeddedWrapperVersion = extractEmbeddedLaunchwrapperOf(downloadedOptiFineJar);
        if (embeddedWrapperVersion != null && !embeddedWrapperVersion.trim().isEmpty()) {
            cleanupLaunchWrapperLibrariesKeeping(embeddedWrapperVersion);
            return;
        }

        String fallbackWrapperVersion = resolveFallbackLaunchWrapperOfVersion(minecraftVersion);
        Path target = getLibrariesRoot().resolve(Paths.get("optifine", "launchwrapper-of", fallbackWrapperVersion,
                "launchwrapper-of-" + fallbackWrapperVersion + ".jar"));
        if (Files.exists(target) && Files.size(target) > 0L) {
            cleanupLaunchWrapperLibrariesKeeping(fallbackWrapperVersion);
            return;
        }

        Files.createDirectories(target.getParent());
        String url = MULTIMC_LAUNCHWRAPPER_OF_MAVEN_BASE + "of-" + fallbackWrapperVersion + "/launchwrapper-of-" + fallbackWrapperVersion + ".jar";
        downloadBinary(url, target, null, null);
        cleanupLaunchWrapperLibrariesKeeping(fallbackWrapperVersion);
    }

    private static String extractEmbeddedLaunchwrapperOf(Path downloadedOptiFineJar) throws IOException {
        if (downloadedOptiFineJar == null || !Files.exists(downloadedOptiFineJar)) {
            return null;
        }

        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(downloadedOptiFineJar.toFile())) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                if (entry == null || entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                String simpleName = name == null ? "" : Paths.get(name).getFileName().toString();
                if (!simpleName.startsWith("launchwrapper-of-") || !simpleName.endsWith(".jar")) {
                    continue;
                }

                String version = simpleName.substring("launchwrapper-of-".length(), simpleName.length() - 4);
                if (version.trim().isEmpty()) {
                    continue;
                }

                Path target = getLibrariesRoot().resolve(Paths.get("optifine", "launchwrapper-of", version, simpleName));
                Files.createDirectories(target.getParent());
                try (InputStream is = zipFile.getInputStream(entry)) {
                    Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return version;
            }
        } catch (java.util.zip.ZipException ignored) {
        }

        return null;
    }

    private static String resolveFallbackLaunchWrapperOfVersion(String minecraftVersion) {
        if (minecraftVersion != null) {
            if (minecraftVersion.matches("1\\.(14|15)(\\.\\d+)?")) {
                return "2.1";
            }
            if (minecraftVersion.matches("1\\.16(\\.\\d+)?")) {
                return "2.2";
            }
        }
        return "2.3";
    }

    private static void cleanupLaunchWrapperLibrariesKeeping(String wrapperVersionToKeep) throws IOException {
        Path root = getLibrariesRoot().resolve(Paths.get("optifine", "launchwrapper-of"));
        Files.createDirectories(root);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path child : stream) {
                String name = child.getFileName().toString();
                if (!name.equals(wrapperVersionToKeep)) {
                    deleteRecursively(child);
                }
            }
        }
    }

    private static void downloadOfficialOptiFineJar(String remoteFileName, Path targetJar) throws IOException {
        String adloadUrl = OPTIFINE_SITE_BASE + "adloadx?f=" + URLEncoder.encode(remoteFileName, "UTF-8").replace("+", "%20");
        TextHttpResponse adload = downloadTextResponse(adloadUrl, OPTIFINE_DOWNLOADS_URL, null);

        Matcher matcher = OPTIFINE_DOWNLOAD_LINK_PATTERN.matcher(adload.body);
        if (!matcher.find()) {
            throw new IOException("Impossible de trouver le lien de téléchargement final OptiFine pour " + remoteFileName);
        }

        String relativeDownloadUrl = htmlDecode(matcher.group(1));
        String finalDownloadUrl = relativeDownloadUrl.startsWith("http")
                ? relativeDownloadUrl
                : OPTIFINE_SITE_BASE + (relativeDownloadUrl.startsWith("/") ? relativeDownloadUrl.substring(1) : relativeDownloadUrl);

        downloadBinary(finalDownloadUrl, targetJar, adloadUrl, adload.cookies);
    }

    private static void cleanupOptiFineLibrariesKeeping(String artifactVersionToKeep) throws IOException {
        Path root = getOptiFineLibrariesRoot();
        Files.createDirectories(root);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path child : stream) {
                String name = child.getFileName().toString();
                if (!name.equals(artifactVersionToKeep)) {
                    deleteRecursively(child);
                }
            }
        }

        Path keptDir = root.resolve(artifactVersionToKeep);
        Files.createDirectories(keptDir);
        String keptJarName = "OptiFine-" + artifactVersionToKeep + ".jar";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(keptDir)) {
            for (Path child : stream) {
                if (!keptJarName.equals(child.getFileName().toString())) {
                    deleteRecursively(child);
                }
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) { }
                });
    }

    private static String toLocalOptiFineArtifactVersion(String remoteFileName) {
        String value = remoteFileName;
        if (value.startsWith("preview_")) {
            value = value.substring("preview_".length());
        }
        if (value.startsWith("OptiFine_")) {
            value = value.substring("OptiFine_".length());
        }
        if (value.endsWith(".jar")) {
            value = value.substring(0, value.length() - 4);
        }
        return value;
    }

    private static Path getLauncherRootPath() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.trim().isEmpty()) {
                return Paths.get(appData, ".majestycraft");
            }
        }
        if (os.contains("mac")) {
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support", ".majestycraft");
        }
        return Paths.get(System.getProperty("user.home"), ".majestycraft");
    }

    private static Path getLibrariesRoot() {
        return getLauncherRootPath().resolve("libraries");
    }

    private static Path getOptiFineLibrariesRoot() {
        return getLibrariesRoot().resolve(Paths.get("optifine", "OptiFine"));
    }

    private static void writeTextFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path,
                content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String ensureTrailingSlash(String value) {
        return value.endsWith("/") ? value : value + "/";
    }

    private static String htmlDecode(String value) {
        return value == null ? null : value.replace("&amp;", "&");
    }

    private static String downloadTextStatic(String urlStr) throws IOException {
        return downloadTextResponse(urlStr, null, null).body;
    }

    private static TextHttpResponse downloadTextResponse(String urlStr, String referer, String cookies) throws IOException {
        HttpURLConnection connection = null;
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "MajestyLauncher");
            if (referer != null && !referer.trim().isEmpty()) {
                connection.setRequestProperty("Referer", referer);
            }
            if (cookies != null && !cookies.trim().isEmpty()) {
                connection.setRequestProperty("Cookie", cookies);
            }

            int code = connection.getResponseCode();
            is = (code >= 200 && code < 300) ? connection.getInputStream() : connection.getErrorStream();
            if (is == null) throw new IOException("HTTP " + code + " sans body");

            String body = readAllText(is);
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " -> " + body);
            }

            return new TextHttpResponse(body, extractCookies(connection));
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) { }
            if (connection != null) connection.disconnect();
        }
    }

    private static void downloadBinary(String urlStr, Path target, String referer, String cookies) throws IOException {
        HttpURLConnection connection = null;
        InputStream is = null;
        Path temp = target.resolveSibling(target.getFileName().toString() + ".part");

        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "MajestyLauncher");
            if (referer != null && !referer.trim().isEmpty()) {
                connection.setRequestProperty("Referer", referer);
            }
            if (cookies != null && !cookies.trim().isEmpty()) {
                connection.setRequestProperty("Cookie", cookies);
            }

            int code = connection.getResponseCode();
            is = (code >= 200 && code < 300) ? connection.getInputStream() : connection.getErrorStream();
            if (is == null) {
                throw new IOException("HTTP " + code + " sans body pour " + urlStr);
            }

            String contentType = connection.getContentType();
            if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
                String body = readAllText(is);
                throw new IOException("Réponse HTML inattendue au lieu du jar : " + body);
            }

            Files.createDirectories(target.getParent());
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) { }
            if (connection != null) connection.disconnect();
            try { Files.deleteIfExists(temp); } catch (Exception ignored) { }
        }
    }

    private static String readAllText(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static String extractCookies(HttpURLConnection connection) {
        Map<String, List<String>> headers = connection.getHeaderFields();
        if (headers == null) return null;

        List<String> rawCookies = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            if (key != null && "set-cookie".equalsIgnoreCase(key) && entry.getValue() != null) {
                rawCookies.addAll(entry.getValue());
            }
        }

        if (rawCookies.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        for (String raw : rawCookies) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String cookie = raw.split(";", 2)[0].trim();
            if (cookie.isEmpty()) continue;
            if (sb.length() > 0) sb.append("; ");
            sb.append(cookie);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * Vérifie si la connexion Internet est disponible.
     */
    public static boolean netIsAvailable() {
        Future<Boolean> future = EXECUTOR_SERVICE.submit(() -> {
            try {
                URL url = new URL("http://www.google.com");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("HEAD");
                urlConnection.setConnectTimeout(3000);
                urlConnection.setReadTimeout(3000);
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_OK;
            } catch (IOException e) {
                LOGGER.warning("Erreur lors de la vérification de la connexion Internet: " + e.getMessage());
                return false;
            }
        });

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.warning("Erreur lors de la vérification asynchrone de la connexion Internet: " + e.getMessage());
            future.cancel(true);
            return false;
        }
    }

    public static App getInstance() {
        return instance;
    }

    private static void setInstance(App app) {
        instance = app;
    }

    public LauncherPanel getPanel() {
        return panel;
    }

    private void setPanel(LauncherPanel panel) {
        this.panel = panel;
    }

    public static GameConnect getGameConnect() {
        return GAME_CONNECT;
    }

    private static final class TextHttpResponse {
        private final String body;
        private final String cookies;

        private TextHttpResponse(String body, String cookies) {
            this.body = body;
            this.cookies = cookies;
        }
    }
}
