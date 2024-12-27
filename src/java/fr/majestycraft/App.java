package fr.majestycraft;

import fr.majestycraft.launcher.*;
import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.maintenance.*;
import fr.trxyy.alternative.alternative_api.utils.*;
import fr.trxyy.alternative.alternative_api_ui.*;
import fr.trxyy.alternative.alternative_api_ui.base.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Classe principale de l'application MajestyLauncher
 */
public class App extends AlternativeBase {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static final String PARTNER_IP = "91.197.6.34";
    private static final String PARTNER_PORT = "25601";
    private static final String ICON_IMAGE = "launchergifpng.png";
    private static final String DEFAULT_JSON_VERSION = "1.21.4.json";
    private static final String GAME_LINK_BASE_URL = "https://majestycraft.com/minecraft/";

    private static App instance;
    private Scene scene;
    private final GameFolder gameFolder = createGameFolder();
    private final LauncherPreferences launcherPreferences = createLauncherPreferences();
    private final GameLinks gameLinks = createGameLinks();
    private final GameEngine gameEngine = createGameEngine();
    private final GameMaintenance gameMaintenance = createGameMaintenance();
    private LauncherPanel panel;

    private static final GameConnect GAME_CONNECT = new GameConnect(PARTNER_IP, PARTNER_PORT);
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    /**
     * Lance le launcher.
     */
    public void launcher(){
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        setInstance(this);
        try {
            createContent();
            registerGameEngine(primaryStage);
            LauncherBase launcherBase = setupLauncherBase(primaryStage);
            launcherBase.setIconImage(primaryStage, ICON_IMAGE);
            Platform.runLater(Main::showStartupPopup);
        } catch (IOException e) {
            LOGGER.severe("Erreur lors de la création du contenu: " + e.getMessage());
        }
    }

    /**
     * Crée le dossier de jeu.
     *
     * @return une instance de GameFolder
     */
    private GameFolder createGameFolder() {
        return new GameFolder("majestycraft");
    }

    /**
     * Crée les préférences du launcher.
     *
     * @return une instance de LauncherPreferences
     */
    private LauncherPreferences createLauncherPreferences() {
        return new LauncherPreferences("MajestyLauncher Optifine + Forge", 1050, 750, Mover.MOVE);
    }

    /**
     * Crée les liens de jeu.
     *
     * @return une instance de GameLinks
     */
    private GameLinks createGameLinks() {
        return new GameLinks(GAME_LINK_BASE_URL, DEFAULT_JSON_VERSION);
    }

    /**
     * Crée le moteur de jeu.
     *
     * @return une instance de GameEngine
     */
    private GameEngine createGameEngine() {
        return new GameEngine(gameFolder, gameLinks, launcherPreferences, GameStyle.VANILLA_1_19_HIGHER);
    }

    /**
     * Crée la maintenance du jeu.
     *
     * @return une instance de GameMaintenance
     */
    private GameMaintenance createGameMaintenance() {
        return new GameMaintenance(Maintenance.USE, gameEngine);
    }

    /**
     * Crée le contenu de l'application.
     *
     * @throws IOException si une erreur d'E/S se produit
     */
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

    /**
     * Enregistre le GameEngine et la maintenance.
     *
     * @param primaryStage le stage principal
     */
    private void registerGameEngine(Stage primaryStage) {
        gameEngine.reg(primaryStage);
        if(netIsAvailable()) {
            gameEngine.reg(gameMaintenance);
        }
    }

    /**
     * Configure et retourne le LauncherBase.
     *
     * @param primaryStage le stage principal
     * @return une instance de LauncherBase
     */
    private LauncherBase setupLauncherBase(Stage primaryStage) {
        LauncherBase launcherBase = new LauncherBase(primaryStage, scene, StageStyle.TRANSPARENT, this.gameEngine);
        return launcherBase;
    }

    /**
     * Vérifie si la connexion Internet est disponible.
     *
     * @return vrai si la connexion est disponible, faux sinon
     */
    public static boolean netIsAvailable() {
        Future<Boolean> future = EXECUTOR_SERVICE.submit(() -> {
            try {
                URL url = new URL("http://www.google.com");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("HEAD");
                urlConnection.setConnectTimeout(3000); // Timeout de connexion de 3 secondes
                urlConnection.setReadTimeout(3000);    // Timeout de lecture de 3 secondes
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_OK;
            } catch (IOException e) {
                LOGGER.warning("Erreur lors de la vérification de la connexion Internet: " + e.getMessage());
                return false;
            }
        });

        try {
            return future.get(5, TimeUnit.SECONDS); // Timeout total de 5 secondes
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.warning("Erreur lors de la vérification asynchrone de la connexion Internet: " + e.getMessage());
            future.cancel(true);
            return false;
        }
    }

    /**
     * Retourne l'instance unique de l'application.
     *
     * @return l'instance de l'application
     */
    public static App getInstance() {
        return instance;
    }

    /**
     * Définit l'instance unique de l'application.
     *
     * @param app l'instance de l'application
     */
    private static void setInstance(App app) {
        instance = app;
    }

    /**
     * Retourne le panneau du launcher.
     *
     * @return le panneau du launcher
     */
    public LauncherPanel getPanel() {
        return panel;
    }

    /**
     * Définit le panneau du launcher.
     *
     * @param panel le panneau du launcher
     */
    private void setPanel(LauncherPanel panel) {
        this.panel = panel;
    }

    /**
     * Retourne la connexion du jeu.
     *
     * @return une instance de GameConnect
     */
    public static GameConnect getGameConnect() {
        return GAME_CONNECT;
    }

    /**
     * Ferme l'ExecutorService lors de l'arrêt de l'application.
     */
    @Override
    public void stop() throws Exception {
        super.stop();
        EXECUTOR_SERVICE.shutdownNow();
    }
}
