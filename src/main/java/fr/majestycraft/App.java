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
    private static final String FABRIC_LOADER_VERSIONS_URL = "https://meta.fabricmc.net/v2/versions/loader/";
    private static final String QUILT_LOADER_VERSIONS_URL = "https://meta.quiltmc.org/v3/versions/loader/";
    private static final String NEOFORGE_MAVEN_METADATA_URL =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
    private static final String NEOFORGE_INSTALLER_BASE_URL =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/";

    private static final Pattern OPTIFINE_REMOTE_FILE_PATTERN = Pattern.compile(
            "adloadx\\?f=((?:preview_)?OptiFine_([0-9][0-9.]+)_HD_U_[A-Za-z0-9_]+\\.jar)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern OPTIFINE_DOWNLOAD_LINK_PATTERN = Pattern.compile(
            "href\\s*=\\s*[\"'](downloadx\\?f=[^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
    );
    

    // Pool principal pour les résolutions Mojang / autres tâches
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(2);
    // Pool dédié à la vérification réseau (évite les deadlocks si appelé depuis EXECUTOR_SERVICE)
    private static final ExecutorService NET_CHECK_EXECUTOR = Executors.newSingleThreadExecutor();


    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            EXECUTOR_SERVICE.shutdownNow();
            NET_CHECK_EXECUTOR.shutdownNow();
        }, "MajestyLauncher-ShutdownHook"));
    }

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
        return new LauncherPreferences("MajestyLauncher Modloaders", 1050, 750, Mover.MOVE);
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

        Utils.ModloaderType modloaderType = Utils.resolveSelectedModloader(this.panel.getConfig());

        final String finalVersion = version;
        final Utils.ModloaderType finalModloaderType = modloaderType;

        EXECUTOR_SERVICE.submit(() -> {
            try {
                if (finalModloaderType == Utils.ModloaderType.OPTIFINE) {
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

                if (isOfficialGeneratedModloader(finalModloaderType)) {
                    GameLinks links = buildOfficialModloaderGameLinks(finalVersion, finalModloaderType);
                    Platform.runLater(() -> this.gameEngine.reg(links));
                    return;
                }

                if (!netIsAvailable()) {
                    GameLinks fallback = buildServerGameLinks(finalVersion, finalModloaderType);
                    Platform.runLater(() -> this.gameEngine.reg(fallback));
                    return;
                }

                String mojangJsonUrl = resolveMojangVersionJsonUrlStatic(finalVersion);
                if (mojangJsonUrl == null) {
                    GameLinks fallback = buildServerGameLinks(finalVersion, finalModloaderType);
                    Platform.runLater(() -> this.gameEngine.reg(fallback));
                    return;
                }

                String serverBaseForVersion = GAME_LINK_BASE_URL + Utils.resolveServerPath(finalVersion, finalModloaderType);

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
                if (finalModloaderType != Utils.ModloaderType.OPTIFINE && !isOfficialGeneratedModloader(finalModloaderType)) {
                    GameLinks fallback = buildServerGameLinks(finalVersion, finalModloaderType);
                    Platform.runLater(() -> this.gameEngine.reg(fallback));
                }
            }
        });
    }

    private GameLinks buildServerGameLinks(String version, Utils.ModloaderType modloaderType) {
        String base = GAME_LINK_BASE_URL + Utils.resolveServerPath(version, modloaderType);
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

    public static GameLinks buildOfficialModloaderGameLinks(String version, Utils.ModloaderType modloaderType) throws IOException {
        if (modloaderType == Utils.ModloaderType.FABRIC) {
            return buildFabricGameLinks(version);
        }
        if (modloaderType == Utils.ModloaderType.QUILT) {
            return buildQuiltGameLinks(version);
        }
        if (modloaderType == Utils.ModloaderType.NEOFORGE) {
            return buildNeoForgeGameLinks(version);
        }
        throw new IOException("Modloader officiel non géré: " + modloaderType);
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

    private static GameLinks buildFabricGameLinks(String version) throws IOException {
        return buildMergedProfileGameLinks(
                version,
                "fabric",
                "fabric-" + version + ".json",
                downloadTextStatic(FABRIC_LOADER_VERSIONS_URL + version),
                true,
                null
        );
    }

    private static GameLinks buildQuiltGameLinks(String version) throws IOException {
        return buildMergedProfileGameLinks(
                version,
                "quilt",
                "quilt-" + version + ".json",
                downloadTextStatic(QUILT_LOADER_VERSIONS_URL + version),
                false,
                null
        );
    }

    private static GameLinks buildNeoForgeGameLinks(String version) throws IOException {
        Path base = getLauncherRootPath().resolve(Paths.get("cache", "links", "neoforge", version));
        Path jsonFile = base.resolve("neoforge-" + version + ".json");
        if (!netIsAvailable() && Files.exists(jsonFile)) {
            return buildLocalOnlyGameLinks(jsonFile.toUri().toString(), version, "neoforge");
        }

        String selectedNeoForgeVersion = resolveLatestNeoForgeVersion(version);
        if (selectedNeoForgeVersion == null || selectedNeoForgeVersion.trim().isEmpty()) {
            throw new IOException("Aucune version NeoForge officielle trouvée pour " + version);
        }

        Path installerJar = base.resolve("neoforge-" + selectedNeoForgeVersion + "-installer.jar");
        if (!Files.exists(installerJar) || Files.size(installerJar) == 0L) {
            Files.createDirectories(installerJar.getParent());
            downloadBinary(NEOFORGE_INSTALLER_BASE_URL + selectedNeoForgeVersion
                    + "/neoforge-" + selectedNeoForgeVersion + "-installer.jar", installerJar, null, null);
        }

        String profileJson = readZipEntry(installerJar, "version.json");
        if (profileJson == null || profileJson.trim().isEmpty()) {
            throw new IOException("Le profil NeoForge officiel est introuvable pour " + version);
        }

        return buildMergedJsonGameLinks(version, "neoforge", jsonFile.getFileName().toString(), profileJson);
    }

    private static GameLinks buildMergedProfileGameLinks(
            String version,
            String profile,
            String jsonFileName,
            String loaderListJson,
            boolean preferStable,
            String fixedLoaderVersion
    ) throws IOException {
        Path base = getLauncherRootPath().resolve(Paths.get("cache", "links", profile, version));
        Path jsonFile = base.resolve(jsonFileName);
        if (!netIsAvailable() && Files.exists(jsonFile)) {
            return buildLocalOnlyGameLinks(jsonFile.toUri().toString(), version, profile);
        }

        String loaderVersion = fixedLoaderVersion != null ? fixedLoaderVersion : resolveLoaderVersionFromMeta(loaderListJson, preferStable);
        if (loaderVersion == null || loaderVersion.trim().isEmpty()) {
            throw new IOException("Aucune version " + profile + " officielle trouvée pour " + version);
        }

        String profileEndpoint = ("fabric".equals(profile) ? FABRIC_LOADER_VERSIONS_URL : QUILT_LOADER_VERSIONS_URL)
                + version + "/" + loaderVersion + "/profile/json";
        String profileJson = downloadTextStatic(profileEndpoint);
        return buildMergedJsonGameLinks(version, profile, jsonFileName, profileJson);
    }

    private static GameLinks buildMergedJsonGameLinks(String version, String profile, String jsonFileName, String profileJson) throws IOException {
        Path base = getLauncherRootPath().resolve(Paths.get("cache", "links", profile, version));
        Path jsonFile = base.resolve(jsonFileName);
        String mojangJsonUrl = resolveMojangVersionJsonUrlStatic(version);
        if (mojangJsonUrl == null || mojangJsonUrl.trim().isEmpty()) {
            throw new IOException("JSON Mojang introuvable pour " + version);
        }

        String mergedJson = mergeVersionJson(downloadTextStatic(mojangJsonUrl), profileJson);
        writeTextFile(jsonFile, mergedJson);
        return buildLocalOnlyGameLinks(jsonFile.toUri().toString(), version, profile);
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

    private static boolean isOfficialGeneratedModloader(Utils.ModloaderType modloaderType) {
        return modloaderType == Utils.ModloaderType.FABRIC
                || modloaderType == Utils.ModloaderType.QUILT
                || modloaderType == Utils.ModloaderType.NEOFORGE;
    }

    private static String resolveLoaderVersionFromMeta(String loaderListJson, boolean preferStable) {
        JsonArray root = JsonParser.parseString(loaderListJson).getAsJsonArray();
        String fallback = null;
        for (JsonElement element : root) {
            JsonObject object = element.getAsJsonObject();
            if (object == null || !object.has("loader")) {
                continue;
            }
            JsonObject loader = object.getAsJsonObject("loader");
            if (loader == null || !loader.has("version")) {
                continue;
            }

            String version = loader.get("version").getAsString();
            if (version == null || version.trim().isEmpty()) {
                continue;
            }
            if (fallback == null) {
                fallback = version;
            }
            if (preferStable && loader.has("stable") && loader.get("stable").getAsBoolean()) {
                return version;
            }
            if (!preferStable) {
                return version;
            }
        }
        return fallback;
    }

    private static String resolveLatestNeoForgeVersion(String minecraftVersion) throws IOException {
        String metadata = downloadTextStatic(NEOFORGE_MAVEN_METADATA_URL);
        String latestStable = null;
        String latestAny = null;
        int start = 0;
        while (start >= 0) {
            int openTag = metadata.indexOf("<version>", start);
            if (openTag < 0) {
                break;
            }
            int closeTag = metadata.indexOf("</version>", openTag);
            if (closeTag < 0) {
                break;
            }

            String candidate = metadata.substring(openTag + "<version>".length(), closeTag).trim();
            if (minecraftVersion.equals(toNeoForgeMinecraftVersion(candidate))) {
                latestAny = candidate;
                if (!candidate.contains("-")) {
                    latestStable = candidate;
                }
            }
            start = closeTag + "</version>".length();
        }
        return latestStable != null ? latestStable : latestAny;
    }

    private static String toNeoForgeMinecraftVersion(String artifactVersion) {
        if (artifactVersion == null || artifactVersion.trim().isEmpty()) {
            return null;
        }

        String normalized = artifactVersion.trim();
        int qualifierIndex = normalized.indexOf('-');
        if (qualifierIndex >= 0) {
            normalized = normalized.substring(0, qualifierIndex);
        }

        String[] parts = normalized.split("\\.");
        if (parts.length < 2) {
            return null;
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            if (major < 20) {
                return null;
            }
            if (minor == 0) {
                return "1." + major;
            }
            return "1." + major + "." + minor;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String readZipEntry(Path archive, String entryName) throws IOException {
        if (archive == null || !Files.exists(archive) || entryName == null || entryName.trim().isEmpty()) {
            return null;
        }

        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(archive.toFile())) {
            java.util.zip.ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                return readAllText(inputStream);
            }
        }
    }

    private static String mergeVersionJson(String baseJson, String overlayJson) {
        JsonObject baseObject = JsonParser.parseString(baseJson).getAsJsonObject();
        JsonObject overlayObject = JsonParser.parseString(overlayJson).getAsJsonObject();
        JsonObject merged = mergeJsonObjects(baseObject, overlayObject);
        merged.remove("inheritsFrom");
        return merged.toString();
    }

    private static JsonObject mergeJsonObjects(JsonObject baseObject, JsonObject overlayObject) {
        JsonObject merged = baseObject == null ? new JsonObject() : baseObject.deepCopy();
        if (overlayObject == null) {
            return merged;
        }

        for (Map.Entry<String, JsonElement> entry : overlayObject.entrySet()) {
            String key = entry.getKey();
            JsonElement overlayValue = entry.getValue();
            JsonElement baseValue = merged.get(key);

            if ("libraries".equals(key) && baseValue != null && baseValue.isJsonArray() && overlayValue.isJsonArray()) {
                merged.add(key, mergeLibraryArrays(baseValue.getAsJsonArray(), overlayValue.getAsJsonArray()));
                continue;
            }

            if (baseValue != null && baseValue.isJsonObject() && overlayValue.isJsonObject()) {
                merged.add(key, mergeJsonObjects(baseValue.getAsJsonObject(), overlayValue.getAsJsonObject()));
                continue;
            }

            if (baseValue != null && baseValue.isJsonArray() && overlayValue.isJsonArray()
                    && ("game".equals(key) || "jvm".equals(key))) {
                merged.add(key, mergeArgumentArrays(baseValue.getAsJsonArray(), overlayValue.getAsJsonArray()));
                continue;
            }

            merged.add(key, overlayValue == null ? null : overlayValue.deepCopy());
        }

        return merged;
    }

    private static JsonArray mergeLibraryArrays(JsonArray baseLibraries, JsonArray overlayLibraries) {
        LinkedHashMap<String, JsonElement> merged = new LinkedHashMap<>();
        if (baseLibraries != null) {
            for (JsonElement element : baseLibraries) {
                String key = resolveLibraryKey(element);
                merged.put(key, element.deepCopy());
            }
        }
        if (overlayLibraries != null) {
            for (JsonElement element : overlayLibraries) {
                String key = resolveLibraryKey(element);
                merged.put(key, element.deepCopy());
            }
        }

        JsonArray out = new JsonArray();
        for (JsonElement element : merged.values()) {
            out.add(element);
        }
        return out;
    }

    private static JsonArray mergeArgumentArrays(JsonArray baseArguments, JsonArray overlayArguments) {
        JsonArray out = new JsonArray();
        if (baseArguments != null) {
            for (JsonElement element : baseArguments) {
                out.add(element.deepCopy());
            }
        }
        if (overlayArguments != null) {
            for (JsonElement element : overlayArguments) {
                out.add(element.deepCopy());
            }
        }
        return out;
    }

    private static String resolveLibraryKey(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("name")) {
                return object.get("name").getAsString();
            }
        }
        return element == null ? java.util.UUID.randomUUID().toString() : element.toString();
    }

    private static String htmlDecode(String value) {
        return value == null ? null : value.replace("&amp;", "&");
    }

    public static String downloadTextStatic(String urlStr) throws IOException {
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
        Future<Boolean> future = NET_CHECK_EXECUTOR.submit(() -> {
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
