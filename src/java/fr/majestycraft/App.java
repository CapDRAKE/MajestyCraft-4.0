package fr.majestycraft;

import fr.majestycraft.launcher.*;
import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.maintenance.*;
import fr.trxyy.alternative.alternative_api.utils.*;
import fr.trxyy.alternative.alternative_api_ui.*;
import fr.trxyy.alternative.alternative_api_ui.base.*;
import javafx.scene.*;
import javafx.scene.shape.*;
import javafx.stage.*;

import java.io.*;
import java.net.*;

public class App extends AlternativeBase {

    private static App instance;
    private Scene scene;
    private final GameFolder gameFolder = createGameFolder();
    private final LauncherPreferences launcherPreferences = createLauncherPreferences();
    private final GameLinks gameLinks = createGameLinks();
    private final GameEngine gameEngine = createGameEngine();
    private final GameMaintenance gameMaintenance = createGameMaintenance();
    private LauncherPanel panel;
    public static final GameConnect GAME_CONNECT = new GameConnect("play.majestycraft.com", "25565");

    public void launcher(){
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setInstance(this);
        createContent();
        this.gameEngine.reg(primaryStage);
        if(netIsAvailable()) {
            this.gameEngine.reg(this.gameMaintenance);
        }
        LauncherBase launcherBase = new LauncherBase(primaryStage, scene, StageStyle.TRANSPARENT, this.gameEngine);
        launcherBase.setIconImage(primaryStage, "launchergifpng.png");
    }

    private GameFolder createGameFolder() {
        return new GameFolder("majestycraft");
    }

    private LauncherPreferences createLauncherPreferences() {
        return new LauncherPreferences("MajestyLauncher Optifine + Forge", 1050, 750, Mover.MOVE);
    }

    private GameLinks createGameLinks() {
        return new GameLinks("https://majestycraft.com/minecraft/", "1.19.3.json");
    }

    private GameEngine createGameEngine() {
        return new GameEngine(gameFolder, gameLinks, launcherPreferences, GameStyle.VANILLA_1_19_HIGHER);
    }

    private GameMaintenance createGameMaintenance() {
        return new GameMaintenance(Maintenance.USE, gameEngine);
    }

    private void createContent() throws IOException {
        LauncherPane contentPane = new LauncherPane(this.gameEngine);
        scene = new Scene(contentPane);
        Rectangle rectangle = new Rectangle(this.gameEngine.getLauncherPreferences().getWidth(),
                this.gameEngine.getLauncherPreferences().getHeight());
        this.gameEngine.reg(gameLinks);
        rectangle.setArcWidth(15.0);
        rectangle.setArcHeight(15.0);
        contentPane.setClip(rectangle);
        contentPane.setStyle("-fx-background-color: transparent;");
        setPanel(new LauncherPanel(contentPane, this.gameEngine));
    }

    public static boolean netIsAvailable() {
        try {
            final HttpURLConnection urlConnection = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
            urlConnection.setRequestMethod("HEAD");
            urlConnection.connect();
            return (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (IOException e) {
            System.err.println("Erreur lors de la vérification de la connexion Internet: " + e.getMessage());
            return false;
        }
    }

    public static App getInstance() {
        return instance;
    }

    private static void setInstance(App instance) {
        App.instance = instance;
    }

    public LauncherPanel getPanel() {
        return panel;
    }

    private void setPanel(LauncherPanel panel) {
        this.panel = panel;
    }
}
