package fr.majestycraft;

import fr.majestycraft.launcher.*;
import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.maintenance.*;
import fr.trxyy.alternative.alternative_api.utils.*;
import fr.trxyy.alternative.alternative_api.utils.config.EnumConfig;
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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.logging.Logger;

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
     * Applique les GameLinks selon la config sauvegardée :
     * - Forge/Optifine => JSON serveur (structure /<version>/forge/ ou /<version>/)
     * - Vanilla => JSON Mojang (si possible), sinon fallback serveur
     *
     * On le fait en async pour ne pas bloquer le thread JavaFX.
     */
    private void applyGameLinksFromConfigAsync() {
        if (this.panel == null || this.panel.getConfig() == null) {
            return;
        }

        // S'assure que la config est chargée
        this.panel.getConfig().loadConfiguration();

        String version = (String) this.panel.getConfig().getValue(EnumConfig.VERSION);
        if (version == null || version.trim().isEmpty()) {
            version = DEFAULT_VERSION_ID;
        }

        boolean useForge = getBooleanConfig(EnumConfig.USE_FORGE);
        boolean useOptifine = getBooleanConfig(EnumConfig.USE_OPTIFINE);

        // Si Forge/Optifine => serveur direct (pas Mojang)
        if (useForge || useOptifine) {
            GameLinks serverLinks = buildServerGameLinks(version, useForge);
            this.gameEngine.reg(serverLinks);
            return;
        }

        // Vanilla : on tente Mojang en arrière-plan, sinon fallback serveur
        final String finalVersion = version;

        EXECUTOR_SERVICE.submit(() -> {
            try {
                // si pas de réseau, inutile de tenter Mojang
                if (!netIsAvailable()) {
                    GameLinks fallback = buildServerGameLinks(finalVersion, false);
                    Platform.runLater(() -> this.gameEngine.reg(fallback));
                    return;
                }

                String mojangJsonUrl = resolveMojangVersionJsonUrl(finalVersion);
                if (mojangJsonUrl == null) {
                    GameLinks fallback = buildServerGameLinks(finalVersion, false);
                    Platform.runLater(() -> this.gameEngine.reg(fallback));
                    return;
                }

                // On garde tes URLs custom sur ton serveur (si tu as ignore/delete/status/files)
                String serverBaseForVersion = GAME_LINK_BASE_URL + "/" + finalVersion + "/";

                GameLinks links = new GameLinks(
                        mojangJsonUrl,
                        serverBaseForVersion + "ignore.cfg",
                        serverBaseForVersion + "delete.cfg",
                        serverBaseForVersion + "status.cfg",
                        serverBaseForVersion + "files/"
                );

                Platform.runLater(() -> this.gameEngine.reg(links));

            } catch (Exception e) {
                LOGGER.warning("Impossible d'appliquer les GameLinks Mojang, fallback serveur. " + e.getMessage());
                GameLinks fallback = buildServerGameLinks(finalVersion, false);
                Platform.runLater(() -> this.gameEngine.reg(fallback));
            }
        });
    }

    private boolean getBooleanConfig(EnumConfig key) {
        Object v = this.panel.getConfig().getValue(key);
        return (v instanceof Boolean) ? (Boolean) v : false;
    }

    /**
     * Forge => /<version>/forge/
     * Vanilla/Optifine => /<version>/
     */
    private GameLinks buildServerGameLinks(String version, boolean forge) {
        String base = GAME_LINK_BASE_URL + "/" + version + (forge ? "/forge/" : "/");
        String jsonName = version + ".json";
        return new GameLinks(base, jsonName);
    }

    /**
     * Résout l'URL Mojang du JSON de version via le manifest.
     * Retourne null si non trouvé.
     */
    private String resolveMojangVersionJsonUrl(String versionId) throws IOException {
        try {
            String url = resolveMojangVersionJsonUrlFromManifest(MOJANG_MANIFEST_PRIMARY, versionId);
            if (url != null) return url;
        } catch (IOException ignored) { }

        return resolveMojangVersionJsonUrlFromManifest(MOJANG_MANIFEST_FALLBACK, versionId);
    }

    private String resolveMojangVersionJsonUrlFromManifest(String manifestUrl, String versionId) throws IOException {
        String json = downloadText(manifestUrl);

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

    private String downloadText(String urlStr) throws IOException {
        HttpURLConnection connection = null;
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("User-Agent", "MajestyLauncher");

            int code = connection.getResponseCode();
            is = (code >= 200 && code < 300) ? connection.getInputStream() : connection.getErrorStream();
            if (is == null) throw new IOException("HTTP " + code + " sans body");

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);

                if (code < 200 || code >= 300) {
                    throw new IOException("HTTP " + code + " -> " + sb);
                }
                return sb.toString();
            }
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
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

    @Override
    public void stop() throws Exception {
        super.stop();
        EXECUTOR_SERVICE.shutdownNow();
    }
}
