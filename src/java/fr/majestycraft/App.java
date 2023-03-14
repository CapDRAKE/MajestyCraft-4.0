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
    private final GameFolder gameFolder = new GameFolder("majestycraft");
    private final LauncherPreferences launcherPreferences = new LauncherPreferences("MajestyLauncher Optifine + Forge", 1050,
            750, Mover.MOVE);
    private final GameLinks gameLinks = new GameLinks("https://majestycraft.com/minecraft/", "1.19.3.json");
    private final GameEngine gameEngine = new GameEngine(gameFolder,gameLinks,
            this.launcherPreferences, GameStyle.VANILLA_1_19_HIGHER);

    private final GameMaintenance gameMaintenance = new GameMaintenance(Maintenance.USE, gameEngine);
    public static final GameConnect GAME_CONNECT = new GameConnect("play.majestycraft.com", "25565");
	private LauncherPanel panel;


    public void launcher(){
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setInstance(this);
        createContent();
        this.gameEngine.reg(primaryStage);
        if(App.netIsAvailable()) {
            this.gameEngine.reg(this.gameMaintenance);
        }
        LauncherBase launcherBase = new LauncherBase(primaryStage, scene, StageStyle.TRANSPARENT, this.gameEngine);
        launcherBase.setIconImage(primaryStage, "launchergifpng.png");
    }

    private void createContent() throws IOException {
        LauncherPane contentPane = new LauncherPane(this.gameEngine);
        scene = new Scene(contentPane);
        Rectangle rectangle = new Rectangle(this.gameEngine.getLauncherPreferences().getWidth(),
                this.gameEngine.getLauncherPreferences().getHeight());
        this.gameEngine.reg(gameLinks);
        rectangle.setArcWidth(15.0);
        rectangle.setArcWidth(15.0);
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
            return false;
        }
    }

	public static App getInstance() {
		return instance;
	}

	public static void setInstance(App instance) {
		App.instance = instance;
	}

	public LauncherPanel getPanel() {
		return panel;
	}

	public void setPanel(LauncherPanel panel) {
		this.panel = panel;
	}

}
