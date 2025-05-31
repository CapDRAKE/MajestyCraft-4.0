package fr.majestycraft.launcher;

import animatefx.animation.*;
import com.jfoenix.controls.*;
import fr.majestycraft.*;
import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.updater.*;
import fr.trxyy.alternative.alternative_api.utils.*;
import fr.trxyy.alternative.alternative_api.utils.config.*;
import fr.trxyy.alternative.alternative_api_ui.*;
import fr.trxyy.alternative.alternative_api_ui.base.*;
import fr.trxyy.alternative.alternative_api_ui.components.*;
import fr.trxyy.alternative.alternative_auth.account.*;
import fr.trxyy.alternative.alternative_auth.base.*;
import fr.trxyy.alternative.alternative_auth.microsoft.MicrosoftAuth;
import fr.trxyy.alternative.alternative_auth.microsoft.ParamType;
import fr.trxyy.alternative.alternative_auth.microsoft.model.MicrosoftModel;
import javafx.animation.*;
import javafx.application.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.scene.text.Font;
import javafx.stage.*;

import java.text.*;
import java.util.*;

public class LauncherPanel extends IScreen {
	
    private final String MINESTRATOR_URL = "https://minestrator.com/?partner=eus561rkso";
    private final String INSTAGRAM_URL = "https://www.tiktok.com/@majestycraft?lang=fr";
    private final String TWITTER_URL = "http://twitter.com/craftsurvie";
    private final String YOUTUBE_URL = "https://www.youtube.com/channel/UCWtD5WQZKiHO7NLSSs-WOQg";
    private final String SITE_URL = "https://majestycraft.com/index.php";
    private final String DISCORD_URL = "https://discord.gg/qyuuHk4udD";

    private final GameEngine engine;

    private final LauncherConfig config;

    private final Discord rpc = new Discord();
    private GameAuth auth;
    private final GameUpdater gameUpdater = new GameUpdater();


    private MediaPlayer mediaPlayer;


    private LauncherImage titleImage;
    private LauncherButton infoButton;
    private LauncherButton microsoftButton;

    private LauncherButton settingsButton;
    private LauncherButton packsButton;
    private LauncherButton minestratorButton;
    private LauncherButton twitterButton;
    private LauncherButton tiktokButton;
    private LauncherButton youtubeButton;
    private LauncherButton siteButton;
    private LauncherButton voteButton;
    private LauncherButton boutiqueButton;
    private LauncherButton lolButton2;
    private LauncherButton deadButton;

    private LauncherRectangle connexionRectangle;
    private LauncherLabel titleCrack;
    private JFXTextField usernameField;
    private JFXToggleButton rememberMe;
    private JFXButton loginButton;

    private LauncherRectangle autoLoginRectangle;
    private LauncherLabel autoLoginLabel;
    private LauncherButton autoLoginButton;
    private LauncherButton autoLoginButton2;
    private Timer autoLoginTimer;

    private LauncherRectangle updateRectangle;
    private LauncherLabel updateLabel;
    private LauncherLabel currentFileLabel;
    private LauncherLabel percentageLabel;
    private LauncherLabel currentStep;
    private final DecimalFormat decimalFormat = new DecimalFormat(".#");
    public JFXProgressBar bar;

    private LauncherImage updateAvatar;
    private LauncherImage avatar;

    private static final String ERROR_AUTH_FAILED = Main.bundle.getString("ERROR_AUTH_FAILED");
    private static final String ERROR_OFFLINE_MODE = Main.bundle.getString("ERROR_OFFLINE_MODE");
    private static final String BUTTON_SITE = Main.bundle.getString("BUTTON_SITE");
    private static final String BUTTON_DISCORD = Main.bundle.getString("BUTTON_DISCORD");
    private static final String LABEL_CONNECTION = Main.bundle.getString("LABEL_CONNECTION");
    private static final String INPUT_PSEUDO_OR_EMAIL = Main.bundle.getString("INPUT_PSEUDO_OR_EMAIL");
    private static final String LABEL_REMEMBER_ME = Main.bundle.getString("LABEL_REMEMBER_ME");
    private static final String BUTTON_LOGIN = Main.bundle.getString("BUTTON_LOGIN");
    private static final String LABEL_OFFLINE_CONNECTION = Main.bundle.getString("LABEL_OFFLINE_CONNECTION");
    private static final String BUTTON_CANCEL = Main.bundle.getString("BUTTON_CANCEL");
    private static final String AUTH_FAILED = Main.bundle.getString("AUTH_FAILED");
    private static final String OFFLINE_MODE_ALERT = Main.bundle.getString("OFFLINE_MODE_ALERT");
    private static final String USERNAME_ALERT = Main.bundle.getString("USERNAME_ALERT");
    private static final String ONLINE_MODE_ALERT = Main.bundle.getString("ONLINE_MODE_ALERT");
    private static final String AUTOLOGIN_START = Main.bundle.getString("AUTOLOGIN_START");
    private static final String AUTOLOGIN_COUNTDOWN = Main.bundle.getString("AUTOLOGIN_COUNTDOWN");
    private static final String UPDATE_LABEL_TEXT = Main.bundle.getString("UPDATE_LABEL");
    private static final String UPDATE_STEP_TEXT = Main.bundle.getString("UPDATE_STEP");
    private static final String AUTH_ERROR_TITLE = Main.bundle.getString("AUTH_ERROR_TITLE");
    private static final String CONNECTION_ERROR_MSG = Main.bundle.getString("CONNECTION_ERROR_MSG");
    private static final String AUTH_ERROR_MSG = Main.bundle.getString("AUTH_ERROR_MSG");

    public LauncherPanel(Pane root, GameEngine engine) {
        this.engine = engine;
        this.drawBackgroundImage(engine, root, "heading.jpg");
        // Dï¿½selectionne la textfield par dï¿½faut
        Platform.runLater(root::requestFocus);

        this.config = new LauncherConfig(engine);
        this.config.loadConfiguration();

        setupBackGround(root);

        initMusic();

        setupButtons(root);

        setupConnectionsGUI(root);
        
        setupUpdateGUI(root);

        initConfig(root);

        final JackInTheBox animationOUVERTURE = new JackInTheBox(root);
        animationOUVERTURE.setSpeed(0.5);
        animationOUVERTURE.setOnFinished(actionEvent -> {
            new Tada(tiktokButton).play();
            new Tada(minestratorButton).play();
            new Tada(twitterButton).play();
            new Tada(youtubeButton).play();
        });
        animationOUVERTURE.play();
    }

    private void checkAutoLogin(Pane root) {
        if (!isAutoLoginEnabled()) {
            return;
        }

        String username = usernameField.getText();
        boolean isPasswordEmpty = true;

        if (isOfflineAccount(username, isPasswordEmpty)) {
            authenticateOffline(username);
            update();
            return;
        }

        if (isOnlineAccount()) {
            if (isMicrosoftAccount()) {
                authenticateMicrosoft(root);
            }
        } else {
            showOfflineError();
        }
    }

    private boolean isAutoLoginEnabled() {
        return this.config.getValue(EnumConfig.AUTOLOGIN).equals(true);
    }

    private boolean isOfflineAccount(String username, boolean isPasswordEmpty) {
        return username.length() >= 2 && isPasswordEmpty;
    }

    private boolean isOnlineAccount() {
        return App.netIsAvailable();
    }

    private boolean isMicrosoftAccount() {
        return (boolean) config.getValue(EnumConfig.USE_MICROSOFT);
    }

    private void authenticateOffline(String username) {
        auth = new GameAuth(username, "", AccountType.OFFLINE);
    }

    private void authenticateMicrosoft(Pane root) {
        auth = new GameAuth(AccountType.MICROSOFT);
        showMicrosoftAuth(root);
    }

    private void showOfflineError() {
        Platform.runLater(() -> new LauncherAlert(ERROR_AUTH_FAILED, ERROR_OFFLINE_MODE));
    }


    private void setupBackGround(Pane root) {
        LauncherRectangle topRectangle = new LauncherRectangle(root, 0, 0, 70, engine.getHeight());
        topRectangle.setFill(Color.rgb(255, 255, 255, 0.10));

        this.drawImage(engine, getResourceLocation().loadImage(engine, "launchergifpng.png"),
                engine.getWidth() / 2 - 70, 40, 150, 150, root, Mover.DONT_MOVE);


        // Titre de la fenÃ©tre
        LauncherLabel titleLabel = new LauncherLabel(root);
        titleLabel.setText("Launcher MajestyCraft Optifine + Forge");
        titleLabel.setFont(FontLoader.loadFont("Roboto-Light.ttf", "Roboto Light", 18F));
        titleLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: orange");
        titleLabel.setPosition(engine.getWidth() / 2 - 150, -4);
        titleLabel.setOpacity(0.7);
        titleLabel.setSize(500, 40);
        titleLabel.setVisible(true);

        root.getScene().getStylesheets().add("css/design.css");

        /* ===================== IMAGE DU LOGO EN HAUT ===================== */
        this.titleImage = new LauncherImage(root);
        this.titleImage.setImage(getResourceLocation().loadImage(engine, "launchergifpng.png"));
        this.titleImage.setSize(50, 50);
        this.titleImage.setBounds(12, 5, 50, 50);

        /* ===================== BOUTON FERMETURE ===================== */
        LauncherButton closeButton = new LauncherButton(root);
        // this.closeButton.setInvisible();
        LauncherImage closeImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "close.png"));
        closeImg.setSize(15, 15);
        closeButton.setGraphic(closeImg);
        closeButton.setBackground(null);
        closeButton.setPosition(engine.getWidth() - 35, 2);
        closeButton.setSize(15, 15);
        closeButton.setOnAction(event -> {
            final BounceOutDown animation = new BounceOutDown(root);
            animation.setOnFinished(actionEvent -> System.exit(0));
            animation.play();
        });

        /* ===================== BOUTON REDUIRE ===================== */
        LauncherButton reduceButton = new LauncherButton(root);
        // this.reduceButton.setInvisible();
        LauncherImage reduceImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "reduce.png"));
        reduceImg.setSize(15, 15);
        reduceButton.setGraphic(reduceImg);
        reduceButton.setBackground(null);
        reduceButton.setPosition(engine.getWidth() - 65, 2);
        reduceButton.setSize(15, 15);
        reduceButton.setOnAction(event -> {
            final ZoomOutDown animation2 = new ZoomOutDown(root);
            animation2.setOnFinished(actionEvent -> {
                Stage stage = (Stage) ((LauncherButton) event.getSource()).getScene().getWindow();
                stage.setIconified(true);
            });
            animation2.setResetOnFinished(true);
            animation2.play();
        });
    }

    private void initMusic() {
        Media media = getResourceLocation().getMedia(this.engine, "Minecraft.mp3");
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.play();
        mediaPlayer.setVolume(0.05);
    }

    private void initConfig(Pane root) {
    	  boolean useDiscord = (boolean) config.getValue(EnumConfig.USE_DISCORD);
    	  boolean useMusic = (boolean) config.getValue(EnumConfig.USE_MUSIC);
    	  boolean useConnect = (boolean) config.getValue(EnumConfig.USE_CONNECT);
    	  boolean useMicrosoft = (boolean) config.getValue(EnumConfig.USE_MICROSOFT);
    	  boolean usePremium = (boolean) config.getValue(EnumConfig.USE_PREMIUM);
    	  String username = (String) config.getValue(EnumConfig.USERNAME);
    	  String version = (String) config.getValue(EnumConfig.VERSION);

    	  if (useDiscord) {
    	    rpc.start();
    	  } else {
    	    rpc.stop();
    	  }

    	  mediaPlayer.setMute(!useMusic);

    	  if (useConnect) {
    		System.out.println("useconnect");
    	    engine.reg(App.getGameConnect());
    	  }

    	  if (useMicrosoft) {
    		connectAccountPremium(username, root);
    	    connectAccountPremiumCO(username, root);
    	  }  else if (usePremium) {
    	    connectAccountPremiumOFF(root);
    	    connectAccountCrackCO(root);
    	  } else {
    	    this.rememberMe.setSelected(false);
    	    connectAccountCrack(root);
    	    connectAccountCrackCO(root);
    	  }

    	  GameLinks links = new GameLinks("https://majestycraft.com/minecraft" + urlModifier(version), version + ".json");
    	  engine.reg(links);
    	  Utils.regGameStyle(engine, config);
    	}

    private void setupButtons(Pane root) {
        this.infoButton = new LauncherButton(root);
        this.infoButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0); -fx-text-fill: orange");
        LauncherImage settingsImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "info.png"));
        settingsImg.setSize(27, 27);
        this.infoButton.setGraphic(settingsImg);
        this.infoButton.setPosition(engine.getWidth() / 2 - 522, engine.getHeight() / 2 - 50);
        this.infoButton.setSize(60, 46);
        this.infoButton.setOnAction(event -> {
            Scene scene = new Scene(createInfoPanel());
            Stage stage = new Stage();
            scene.setFill(Color.TRANSPARENT);
            stage.setResizable(false);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Parametres Launcher");
            stage.setWidth(900);
            stage.setHeight(600);
            stage.setScene(scene);
            stage.show();
        });

        JFXRippler rippler3 = new JFXRippler(this.infoButton);
        rippler3.setLayoutX((float) engine.getWidth() / 2 - 515);
        rippler3.setLayoutY((float) engine.getHeight() / 2 - 50);
        rippler3.getStyleClass().add("rippler2");
        root.getChildren().add(rippler3);

        /* ===================== BOUTON microsoft ===================== */
        this.microsoftButton = new LauncherButton(root);
        this.microsoftButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0); -fx-text-fill: orange");
        LauncherImage microsoftImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "microsoft.png"));
        microsoftImg.setSize(27, 27);
        this.microsoftButton.setGraphic(microsoftImg);
        this.microsoftButton.setPosition(engine.getWidth() / 2 - 522, engine.getHeight() / 2 - 100);
        this.microsoftButton.setSize(60, 46);
        microsoftButton.setOnAction(event -> {
        	  if (!App.netIsAvailable()) {
        	    showConnectionErrorAlert();
        	    return;
        	  }        	
        	  
        	  auth = new GameAuth(AccountType.MICROSOFT);
        	  
        	  /* ① tentative instantanée avec le refresh_token */
        	    if (auth.trySilentRefresh(engine)) {
        	        // succès : on met à jour l’UI comme d’habitude puis on s’en va
        	        Session s = auth.getSession();
        	        connectAccountPremiumCO(s.getUsername(), root);
        	        config.updateValue("useMicrosoft", true);
        	        update();
        	        return;
        	    }
        	  
        	  /* lance la fenêtre + thread d’auth ; le résultat sera
              traité dans startMicrosoftLogin() */
        	  showMicrosoftAuth(root);
        });

        this.settingsButton = new LauncherButton(root);
        this.settingsButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0); -fx-text-fill: orange");
        settingsImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "settings.png"));
        settingsImg.setSize(27, 27);
        this.settingsButton.setGraphic(settingsImg);
        this.settingsButton.setPosition(engine.getWidth() / 2 - 522, engine.getHeight() / 2);
        this.settingsButton.setSize(60, 46);
        this.settingsButton.setOnAction(event -> {
            Scene scene = new Scene(createSettingsPanel(root));
            Stage stage = new Stage();
            scene.setFill(Color.TRANSPARENT);
            stage.setResizable(false);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Parametres Launcher");
            stage.setWidth(900);
            stage.setHeight(600);
            stage.setScene(scene);
            stage.showAndWait();
        });

        JFXRippler rippler4 = new JFXRippler(this.settingsButton);
        rippler4.setLayoutX((float) engine.getWidth() / 2 - 515);
        rippler4.setLayoutY((float) engine.getHeight() / 2);
        rippler4.getStyleClass().add("rippler2");
        root.getChildren().add(rippler4);
        
        
        this.packsButton = new LauncherButton(root);
        this.packsButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0); -fx-text-fill: orange");
        settingsImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "pack.png"));
        settingsImg.setSize(27, 27);
        this.packsButton.setGraphic(settingsImg);
        this.packsButton.setPosition(engine.getWidth() / 2 - 522, engine.getHeight() / 2+100);
        this.packsButton.setSize(60, 46);
        this.packsButton.setOnAction(event -> {
            Scene scene = new Scene(createPacksPanel(root));
            Stage stage = new Stage();
            scene.setFill(Color.TRANSPARENT);
            stage.setResizable(false);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Parametres Launcher");
            stage.setWidth(900);
            stage.setHeight(600);
            stage.setScene(scene);
            stage.showAndWait();
        });

        JFXRippler rippler5 = new JFXRippler(this.packsButton);
        rippler5.setLayoutX((float) engine.getWidth() / 2 - 515);
        rippler5.setLayoutY((float) engine.getHeight() / 2 + 50 );
        rippler5.getStyleClass().add("rippler2");
        root.getChildren().add(rippler5);

        /* ===================== BOUTON easter egg 2 ===================== */
        this.deadButton = new LauncherButton(root);
        this.deadButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0); -fx-text-fill: orange");
        settingsImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "dead.png"));
        settingsImg.setSize(22, 27);
        this.deadButton.setGraphic(settingsImg);
        this.deadButton.setPosition(engine.getWidth() / 2 + 467, engine.getHeight() / 2 + 330);
        this.deadButton.setSize(60, 46);
        this.deadButton.setOnAction(event -> openLink("https://youtu.be/koQN49gW5fE?t=31"));

        this.lolButton2 = new LauncherButton(root);
        this.lolButton2.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0); -fx-text-fill: orange");
        settingsImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "lol2.PNG"));
        settingsImg.setSize(27, 27);
        this.lolButton2.setGraphic(settingsImg);
        this.lolButton2.setPosition(engine.getWidth() / 2 - 522, engine.getHeight() / 2 + 300);
        this.lolButton2.setSize(60, 60);
        this.lolButton2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                // Les animations de l'easter egg
                new Hinge(microsoftButton).setResetOnFinished(true).play();
                new Hinge(infoButton).setResetOnFinished(true).play();
                new Hinge(settingsButton).setResetOnFinished(true).play();
                new Hinge(boutiqueButton).setResetOnFinished(true).play();
                new Hinge(avatar).setResetOnFinished(true).play();
                new Hinge(minestratorButton).setResetOnFinished(true).play();
                new Hinge(twitterButton).setResetOnFinished(true).play();
                new Hinge(tiktokButton).setResetOnFinished(true).play();
                new Hinge(youtubeButton).setResetOnFinished(true).play();
                new Hinge(deadButton).setResetOnFinished(true).play();
                new Hinge(titleImage).setResetOnFinished(true).play();
                new Hinge(siteButton).setResetOnFinished(true).play();
                new Hinge(voteButton).setResetOnFinished(true).play();
                new Hinge(connexionRectangle).setResetOnFinished(true).play();
                new Hinge(rememberMe).setResetOnFinished(true).play();
                new Hinge(usernameField).setResetOnFinished(true).play();
                new Hinge(loginButton).setResetOnFinished(true).play();
            }
        });

        /* ===================== BOUTON URL VOTE ===================== */
        this.voteButton = new LauncherButton(root);
        this.voteButton.setText(BUTTON_SITE);
        this.voteButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 22F));
        this.voteButton.setPosition(engine.getWidth() / 2 - 260, engine.getHeight() / 2 + 190);
        this.voteButton.setSize(250, 45);
        this.voteButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0.4); -fx-text-fill: orange");
        this.voteButton.setOnAction(event -> openLink(SITE_URL));

        /* ===================== BOUTON URL BOUTIQUE ===================== */
        this.boutiqueButton = new LauncherButton(root);
        this.boutiqueButton.setText(BUTTON_DISCORD);
        this.boutiqueButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 22F));
        this.boutiqueButton.setPosition(engine.getWidth() / 2 - 125 + 130, engine.getHeight() / 2 + 190);
        this.boutiqueButton.setSize(250, 45);
        this.boutiqueButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0.4); -fx-text-fill: orange");
        this.boutiqueButton.setOnAction(event -> openLink(DISCORD_URL));

        /* ===================== BOUTON URL MINESTRATOR ===================== */
        this.minestratorButton = new LauncherButton(root);
        this.minestratorButton.setInvisible();
        this.minestratorButton.setPosition(engine.getWidth() / 2 - 125, engine.getHeight() - 130);
        LauncherImage facebookImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "minestrator.png"));
        facebookImg.setSize(80, 80);
        this.minestratorButton.setGraphic(facebookImg);
        this.minestratorButton.setSize((int) facebookImg.getFitWidth(), (int) facebookImg.getFitHeight());
        this.minestratorButton.setBackground(null);
        this.minestratorButton.setOnAction(event -> openLink(MINESTRATOR_URL));

        /* ===================== BOUTON URL TWITTER ===================== */
        this.twitterButton = new LauncherButton(root);
        this.twitterButton.setInvisible();
        this.twitterButton.setPosition(engine.getWidth() / 2 + 25, engine.getHeight() - 130);
        LauncherImage twitterImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "twitter_icon.png"));
        twitterImg.setSize(80, 80);
        this.twitterButton.setGraphic(twitterImg);
        this.twitterButton.setSize((int) twitterImg.getFitWidth(), (int) twitterImg.getFitHeight());
        this.twitterButton.setBackground(null);
        this.twitterButton.setOnAction(event -> openLink(TWITTER_URL));

        /* ===================== BOUTON URL TIKTOK ===================== */
        this.tiktokButton = new LauncherButton(root);
        this.tiktokButton.setInvisible();
        this.tiktokButton.setPosition(engine.getWidth() / 2 - 125 - 150, engine.getHeight() - 130);
        LauncherImage tiktokImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "tiktok.png"));
        tiktokImg.setSize(80, 80);
        this.tiktokButton.setGraphic(tiktokImg);
        this.tiktokButton.setSize((int) tiktokImg.getFitWidth(), (int) tiktokImg.getFitHeight());
        this.tiktokButton.setBackground(null);
        this.tiktokButton.setOnAction(event -> openLink(INSTAGRAM_URL));

        /*===================== BOUTON URL YOUTUBE ===================== */
        this.youtubeButton = new LauncherButton(root);
        this.youtubeButton.setInvisible();
        this.youtubeButton.setPosition(engine.getWidth() / 2 - 125 + 300, engine.getHeight() - 130);
        LauncherImage youtubeImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "yt_icon.png"));
        youtubeImg.setSize(80, 80);
        this.youtubeButton.setGraphic(youtubeImg);
        this.youtubeButton.setSize((int) youtubeImg.getFitWidth(), (int) youtubeImg.getFitHeight());
        this.youtubeButton.setBackground(null);
        this.youtubeButton.setOnAction(event -> openLink(YOUTUBE_URL));
    }

    @SuppressWarnings("null")
	private void setupConnectionsGUI(Pane root) {
        /* ===================== RECTANGLE DES CONNEXIONS ===================== */
        this.connexionRectangle = new LauncherRectangle(root, engine.getWidth() / 2 - 188, engine.getHeight() / 2 - 150,
                380, 320);
        this.connexionRectangle.setArcWidth(50.0);
        this.connexionRectangle.setArcHeight(50.0);
        this.connexionRectangle.setFill(Color.rgb(0, 0, 0, 0.30));
        this.connexionRectangle.setVisible(true);


        this.titleCrack = new LauncherLabel(root);
        this.titleCrack.setText(LABEL_CONNECTION);
        this.titleCrack.setFont(Font.font("leadcoat.ttf", FontWeight.BOLD, 27d));
        this.titleCrack.setStyle("-fx-background-color: transparent; -fx-text-fill: orange");
        this.titleCrack.setPosition(engine.getWidth() / 2 - 116, engine.getHeight() / 2 - 130);
        this.titleCrack.setSize(500, 40);

        JFXRippler rippler2 = new JFXRippler(this.titleCrack);
        rippler2.setLayoutX((float) engine.getWidth() / 2 - 72);
        rippler2.setLayoutY((float) engine.getHeight() / 2 - 130);
        rippler2.getStyleClass().add("rippler");
        root.getChildren().add(rippler2);

        this.usernameField = new JFXTextField();

        this.usernameField.getStyleClass().add("input");
        this.usernameField.setLayoutX((float) engine.getWidth() / 2 - 126);
        this.usernameField.setLayoutY((float) engine.getHeight() / 2 - 52);
        this.usernameField.setFont(FontLoader.loadFont("leadcoat.ttf", "Lead Coat", 14F));
        this.usernameField.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0.2); -fx-text-fill: orange; -fx-font-family: leadcoat");
        this.usernameField.setPromptText(INPUT_PSEUDO_OR_EMAIL);
        if (!(boolean) config.getValue(EnumConfig.USE_MICROSOFT)) {
            this.usernameField.setText((String) this.config.getValue(EnumConfig.USERNAME));
        }
        root.getChildren().add(this.usernameField);

        this.rememberMe = new JFXToggleButton();
        this.rememberMe.setText(LABEL_REMEMBER_ME);
        this.rememberMe.setSelected((boolean) config.getValue(EnumConfig.REMEMBER_ME));
        this.rememberMe.getStyleClass().add("jfx-toggle-button");
        this.rememberMe.setLayoutX(385);
        this.rememberMe.setLayoutY(427);
        this.rememberMe.setOnAction(event -> config.updateValue("rememberme", rememberMe.isSelected()));

        root.getChildren().add(this.rememberMe);

        /* ===================== BOUTON DE CONNEXION ===================== */
        this.loginButton = new JFXButton(BUTTON_LOGIN);
        this.loginButton.getStyleClass().add("button-raised");
        this.loginButton.setLayoutX(400);
        this.loginButton.setLayoutY(480);
        this.loginButton.setFont(FontLoader.loadFont("../resources/leadcoat.ttf", "leadcoat", 22F));
        this.loginButton.setOnAction(event -> {
            if (!App.netIsAvailable()) {
            	Platform.runLater(() -> new LauncherAlert(AUTH_FAILED, OFFLINE_MODE_ALERT));
                return;
            }

            config.updateValue("useMicrosoft", false);

            String username = usernameField.getText();
            String password = "";

            if (username.length() <= 3) {
            	new LauncherAlert(AUTH_FAILED, USERNAME_ALERT);
                return;
            }

            if (password.isEmpty()) {
                auth = new GameAuth(username, password, AccountType.OFFLINE);
                connectAccountCrackCO(root);
            } else {
                auth = new GameAuth(username, password, AccountType.MOJANG);
                connectAccountPremiumCO(username, root);
                if ((boolean) config.getValue(EnumConfig.REMEMBER_ME)) {
                    config.updateValue("password", password);
                } else {
                    config.updateValue("password", "");
                }
            }

            if (auth.isLogged()) {
                config.updateValue("username", username);
                update();
            } else {
            	new LauncherAlert(AUTH_FAILED, ONLINE_MODE_ALERT);
            }
        });
        root.getChildren().add(this.loginButton);


        this.autoLoginRectangle = new LauncherRectangle(root, 0, engine.getHeight() - 32, 2000,
                engine.getHeight());
        this.autoLoginRectangle.setFill(Color.rgb(0, 0, 0, 0.70));
        this.autoLoginRectangle.setOpacity(1.0);
        this.autoLoginRectangle.setVisible(false);

        /* ===================== MESSAGE AUTOLOGIN ===================== */
        this.autoLoginLabel = new LauncherLabel(root);
        this.autoLoginLabel.setText(LABEL_OFFLINE_CONNECTION);
        this.autoLoginLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 18F));
        this.autoLoginLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: red;");
        this.autoLoginLabel.setPosition(engine.getWidth() / 2 - 280, engine.getHeight() - 34);
        this.autoLoginLabel.setOpacity(0.7);
        this.autoLoginLabel.setSize(700, 40);
        this.autoLoginLabel.setVisible(false);

        /* ===================== ANNULER AUTOLOGIN ===================== */
        this.autoLoginButton = new LauncherButton(root);
        this.autoLoginButton.setText(BUTTON_CANCEL);
        this.autoLoginButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.autoLoginButton.setPosition(engine.getWidth() / 2 + 60, engine.getHeight() - 30);
        this.autoLoginButton.setSize(100, 20);
        this.autoLoginButton.setStyle("-fx-background-color: rgba(255, 0, 0, 0.4); -fx-text-fill: black;");
        this.autoLoginButton.setVisible(false);
        this.autoLoginButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                autoLoginTimer.cancel();
                autoLoginLabel.setVisible(false);
                autoLoginButton.setVisible(false);
                autoLoginRectangle.setVisible(false);
                autoLoginButton2.setVisible(false);
            }
        });
        
        /* ===================== ANNULER AUTOLOGIN ===================== */
        this.autoLoginButton2 = new LauncherButton(root);
        this.autoLoginButton2.setText(AUTOLOGIN_START);
        this.autoLoginButton2.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.autoLoginButton2.setPosition(engine.getWidth() / 2 + 170, engine.getHeight() - 30);
        this.autoLoginButton2.setSize(100, 20);
        this.autoLoginButton2.setStyle("-fx-background-color: rgba(15, 209, 70, 0.4); -fx-text-fill: black;");
        this.autoLoginButton2.setVisible(false);
        this.autoLoginButton2.setOnAction(event -> {
        	if (!engine.getGameMaintenance().isAccessBlocked()) {
                autoLoginTimer.cancel();
                autoLoginLabel.setVisible(false);
                autoLoginButton.setVisible(false);
                autoLoginRectangle.setVisible(false);
                autoLoginButton2.setVisible(false);
                if ((boolean) config.getValue(EnumConfig.USE_CONNECT)) {
                	System.out.println("c ici");
                    engine.reg(App.getGameConnect());
                }
                checkAutoLogin(root);
        	}
        });
        
        if (this.config.getValue(EnumConfig.AUTOLOGIN).equals(true)) {
            Platform.runLater(() -> {
                autoLoginTimer = new Timer();
                TimerTask timerTask = new TimerTask() {
                    final int waitTime = 7;
                    int elapsed = 0;

                    @Override

                    public void run() {
                    	elapsed++;
                    	if (elapsed % waitTime == 0) {
                    		if (!engine.getGameMaintenance().isAccessBlocked()) {
                                autoLoginTimer.cancel();
                                autoLoginLabel.setVisible(false);
                                autoLoginButton.setVisible(false);
                                autoLoginRectangle.setVisible(false);
                                autoLoginButton2.setVisible(false);
                                checkAutoLogin(root);
                    		}
                    	}
                        else {
                            final int time = (waitTime - (elapsed % waitTime));
                            Platform.runLater(() -> autoLoginLabel.setText(String.format(AUTOLOGIN_COUNTDOWN, time)));
                        }
                    }
                    

                };
                autoLoginTimer.schedule(timerTask, 0, 1000);
                autoLoginLabel.setVisible(true);
                autoLoginRectangle.setVisible(true);
                autoLoginButton.setVisible(true);
                autoLoginButton2.setVisible(true);
               });

        }
    }

    private void setupUpdateGUI(Pane root) {
        /* ===================== RECTANGLE DE MISE A JOURS ===================== */
        this.updateRectangle = new LauncherRectangle(root, engine.getWidth() / 2 - 175, engine.getHeight() / 2 - 80,
                350, 180);
        this.updateRectangle.setArcWidth(50.0);
        this.updateRectangle.setArcHeight(50.0);
        this.updateRectangle.setFill(Color.rgb(0, 0, 0, 0.60));
        this.updateRectangle.setVisible(false);

        /* =============== LABEL TITRE MISE A JOUR =============== **/
        this.updateLabel = new LauncherLabel(root);
        this.updateLabel.setText(UPDATE_LABEL_TEXT);
        this.updateLabel.setAlignment(Pos.CENTER);
        this.updateLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 22F));
        this.updateLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: orange;");
        this.updateLabel.setPosition(engine.getWidth() / 2 - 95, engine.getHeight() / 2 - 75);
        this.updateLabel.setOpacity(1);
        this.updateLabel.setSize(190, 40);
        this.updateLabel.setVisible(false);

        /* =============== ETAPE DE MISE A JOUR =============== **/
        this.currentStep = new LauncherLabel(root);
        this.currentStep.setText(UPDATE_STEP_TEXT);
        this.currentStep.setFont(Font.font("Verdana", FontPosture.ITALIC, 18F)); // FontPosture.ITALIC
        this.currentStep.setStyle("-fx-background-color: transparent; -fx-text-fill: orange;");
        this.currentStep.setAlignment(Pos.CENTER);
        this.currentStep.setPosition(engine.getWidth() / 2 - 160, engine.getHeight() / 2 + 63);
        this.currentStep.setOpacity(0.4);
        this.currentStep.setSize(320, 40);
        this.currentStep.setVisible(false);

        /* =============== FICHIER ACTUEL EN TELECHARGEMENT =============== **/
        this.currentFileLabel = new LauncherLabel(root);
        this.currentFileLabel.setText("launchwrapper-12.0.jar");
        this.currentFileLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 18F));
        this.currentFileLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: orange;");
        this.currentFileLabel.setAlignment(Pos.CENTER);
        this.currentFileLabel.setPosition(engine.getWidth() / 2 - 160, engine.getHeight() / 2 + 5);
        this.currentFileLabel.setOpacity(0.8);
        this.currentFileLabel.setSize(320, 40);
        this.currentFileLabel.setVisible(false);

        /* =============== POURCENTAGE =============== **/
        this.percentageLabel = new LauncherLabel(root);
        this.percentageLabel.setText("0%");
        this.percentageLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 30F));
        this.percentageLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: orange;");
        this.percentageLabel.setAlignment(Pos.CENTER);
        this.percentageLabel.setPosition(engine.getWidth() / 2 - 50, engine.getHeight() / 2 - 25);
        this.percentageLabel.setOpacity(0.8);
        this.percentageLabel.setSize(100, 40);
        this.percentageLabel.setVisible(false);

        /* ===================== BARRE DE CHARGEMENT ===================== */
        this.bar = new JFXProgressBar();
        this.bar.setLayoutX((float) engine.getWidth() / 2 - 125);
        this.bar.setLayoutY((float) engine.getHeight() / 2 + 40);
        this.bar.getStyleClass().add("jfx-progress-bar");
        // this.bar.setSize(250, 20);
        this.bar.setVisible(false);
        root.getChildren().add(this.bar);
    }


    public void update() {
        new ZoomOutDown(this.microsoftButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.infoButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.settingsButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.titleCrack).setResetOnFinished(false).play();
        new ZoomOutDown(this.usernameField).setResetOnFinished(false).play();
        new ZoomOutDown(this.boutiqueButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.avatar).setResetOnFinished(false).play();
        new ZoomOutDown(this.minestratorButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.twitterButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.tiktokButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.youtubeButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.deadButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.rememberMe).setResetOnFinished(false).play();
        new ZoomOutDown(this.loginButton).setResetOnFinished(false).play();
        //new ZoomOutDown(this.siteButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.voteButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.connexionRectangle).setResetOnFinished(false).play();
        new ZoomOutDown(this.lolButton2).setResetOnFinished(false).play();

        this.usernameField.setDisable(true);
        this.connexionRectangle.setDisable(true);
        this.rememberMe.setDisable(true);
        this.loginButton.setDisable(true);
        this.settingsButton.setDisable(true);

        this.updateRectangle.setVisible(true);
        this.updateLabel.setVisible(true);
        this.currentStep.setVisible(true);
        this.currentFileLabel.setVisible(true);
        this.percentageLabel.setVisible(true);
        this.bar.setVisible(true);
        avatar.setVisible(false);
        new ZoomInDown(this.updateRectangle).play();
        new ZoomInDown(this.updateLabel).play();
        new ZoomInDown(this.currentStep).play();
        new ZoomInDown(this.currentFileLabel).play();
        new ZoomInDown(this.percentageLabel).play();
        new ZoomInDown(this.bar).play();
        new ZoomInDown(updateAvatar).play();
        updateAvatar.setVisible(true);
        engine.getGameLinks().JSON_URL = engine.getGameLinks().BASE_URL
                + this.config.getValue(EnumConfig.VERSION) + ".json";
        this.gameUpdater.reg(engine);
        this.gameUpdater.reg(auth.getSession());

        /*
         * Change settings in GameEngine from launcher_config.json
         */
        engine.reg(GameMemory.getMemory(Double.parseDouble((String) this.config.getValue(EnumConfig.RAM))));
        engine.reg(GameSize.getWindowSize(Integer.parseInt((String) this.config.getValue(EnumConfig.GAME_SIZE))));
        
        if ((boolean) config.getValue(EnumConfig.USE_CONNECT)) {
        	System.out.println("true");
            engine.reg(App.getGameConnect());
        }

        boolean useVmArgs = (Boolean) config.getValue(EnumConfig.USE_VM_ARGUMENTS);
        String vmArgs = (String) config.getValue(EnumConfig.VM_ARGUMENTS);
        String[] s = null;
        if (useVmArgs) {
            if (vmArgs.length() > 3) {
                s = vmArgs.split(" ");
            }
            assert s != null;
            JVMArguments arguments = new JVMArguments(s);
            engine.reg(arguments);
        }
        /* END */

        engine.reg(this.gameUpdater);

        Thread updateThread = new Thread(() -> engine.getGameUpdater().start());
        updateThread.start();

        /*
         * ===================== REFAICHIR LE NOM DU FICHIER, PROGRESSBAR, POURCENTAGE
         * =====================
         **/
        Timeline timeline = new Timeline(
                new KeyFrame(javafx.util.Duration.seconds(0.0D), event -> timelineUpdate(engine)),
                new KeyFrame(javafx.util.Duration.seconds(0.1D)));

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

    }

    private double percent;

    public void timelineUpdate(GameEngine engine) {
        if (engine.getGameUpdater().downloadedFiles > 0) {
            this.percent = engine.getGameUpdater().downloadedFiles * 100.0D / engine.getGameUpdater().filesToDownload;
            this.percentageLabel.setText(decimalFormat.format(percent) + "%");
        }
        this.currentFileLabel.setText(engine.getGameUpdater().getCurrentFile());
        this.currentStep.setText(engine.getGameUpdater().getCurrentInfo());
        this.bar.setProgress(percent / 100.0D);
    }

    private Parent createSettingsPanel(Pane root) {
        root = new LauncherPane(engine);
        Rectangle rect = new Rectangle(1000, 750);
        rect.setArcHeight(15.0);
        rect.setArcWidth(15.0);
        root.setClip(rect);
        root.setStyle("-fx-background-color: transparent;");
        new LauncherSettings(root, engine, this);
        new ZoomInLeft(rect).play();
        return root;
    }


    private Parent createInfoPanel() {
        Pane root = new LauncherPane(engine);
        javafx.scene.shape.Rectangle rect = new Rectangle(1500, 900);
        rect.setArcHeight(15.0);
        rect.setArcWidth(15.0);
        root.setClip(rect);
        root.setStyle("-fx-background-color: transparent;");
        new LauncherInfo(root, engine, this);
        new ZoomInLeft(rect).setResetOnFinished(true).play();
        return root;
    }
    
    private Parent createPacksPanel(Pane root) {
        root = new LauncherPane(engine);
        Rectangle rect = new Rectangle(1000, 750);
        rect.setArcHeight(15.0);
        rect.setArcWidth(15.0);
        root.setClip(rect);
        root.setStyle("-fx-background-color: transparent;");
        new LauncherPacks(root, engine, this);
        new ZoomInLeft(rect).play();
        return root;
    }

    public void connectAccountCrack(Pane root) {
        avatar = new LauncherImage(root, new Image("https://minotar.net/cube/MHF_Steve.png"));
        avatar.setBounds(engine.getWidth() / 2 - 182, engine.getHeight() / 2 - 42, 50, 60);
    }

    public void connectAccountPremium(String username, Pane root) {
        avatar = new LauncherImage(root, new Image("https://minotar.net/cube/" + username + ".png"));
        avatar.setBounds(engine.getWidth() / 2 - 182, engine.getHeight() / 2 - 42, 50, 60);
    }

    public void connectAccountPremiumOFF(Pane root) {
        avatar = new LauncherImage(root, new Image("https://minotar.net/cube/MHF_Steve.png"));
        avatar.setBounds(engine.getWidth() / 2 - 182, engine.getHeight() / 2 - 42, 50, 60);
    }

    public void connectAccountCrackCO(Pane root) {
        updateAvatar = new LauncherImage(root, new Image("https://minotar.net/body/MHF_Steve.png"));
        updateAvatar.setSize(100, 200);
        updateAvatar.setBounds(engine.getWidth() / 2 - 280, engine.getHeight() / 2 - 90, 100, 200);
        updateAvatar.setVisible(false);
    }

    public void connectAccountPremiumCO(String username, Pane root) {
        updateAvatar = new LauncherImage(root, new Image("https://minotar.net/body/" + username + ".png"));
        updateAvatar.setBounds(engine.getWidth() / 2 - 280, engine.getHeight() / 2 - 90, 100, 200);
        updateAvatar.setVisible(false);
    }


    private void showMicrosoftAuth(Pane root) {   // ← nouveau paramètre
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(80, 80);

        StackPane pane = new StackPane(spinner);
        pane.setPadding(new Insets(20));

        Stage stage = new Stage();
        stage.setScene(new Scene(pane, 300, 160));
        stage.setTitle("Connexion Microsoft");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.show();

        startMicrosoftLogin(stage, root);
    }

    private String urlModifier(String version) {
        if ((boolean)(config.getValue(EnumConfig.USE_FORGE))) {
            return "/" + version + "/forge/";
        } else if ((boolean)(config.getValue(EnumConfig.USE_OPTIFINE))) {
            return "/" + version + "/";
        } else {
            return "/";
        }
    }

    public LauncherConfig getConfig() {
        return config;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public Discord getRpc(){
        return rpc;
    }
    
    private void showConnectionErrorAlert() {
        Platform.runLater(() -> new LauncherAlert(AUTH_ERROR_TITLE, CONNECTION_ERROR_MSG));
    }

    private void showAuthErrorAlert() {
        Platform.runLater(() -> new LauncherAlert(AUTH_ERROR_TITLE, AUTH_ERROR_MSG));
    }
    
    private void startMicrosoftLogin(Stage stage, Pane root) {

        /* petit spinner d’attente pendant toute la phase d’auth */
        ProgressIndicator spinner = new ProgressIndicator();
        Stage wait = new Stage();
        wait.initOwner(stage);
        wait.initModality(Modality.APPLICATION_MODAL);
        wait.setScene(new Scene(new StackPane(spinner), 120, 120));
        wait.setTitle("Connexion Microsoft");
        wait.setResizable(false);
        wait.show();

        /* thread de travail pour ne pas bloquer JavaFX */
        new Thread(() -> {
            try {
                /* 1) URL d’autorisation Live-ID (scope XboxLive.signin + offline_access) */
                MicrosoftAuth msAuth = new MicrosoftAuth();
                String authUrl = msAuth.getAuthorizationUrl(null);
                java.awt.Desktop.getDesktop().browse(new java.net.URI(authUrl));

                /* 2) boîte de dialogue → l’utilisateur colle l’URL ou juste le code */
                final java.util.concurrent.atomic.AtomicReference<String> codeRef =
                        new java.util.concurrent.atomic.AtomicReference<>();
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

                Platform.runLater(() -> {
                    TextInputDialog dlg = new TextInputDialog();
                    dlg.setTitle("Connexion Microsoft – Dernière étape");
                    dlg.setHeaderText("Terminer la connexion");
                    dlg.setContentText(
                          "1\u2003Dans le navigateur, clique sur le bouton « Oui » de Microsoft.\n Si tu as une page blanche, passe à l'étape 2\n"
                        + "\n"
                        + "2\u2003Une page blanche s’affiche ; c’est normal.\n"
                        + "   Copie l’adresse complète de la barre d’adresse, par ex. :\n"
                        + "   https://login.live.com/oauth20_desktop.srf?code=ABCDEFGHI…\n"
                        + "   (ou copie seulement le long code après « code= »)\n"
                        + "\n"
                        + "3\u2003Reviens dans le launcher, colle ici ↓ puis valide :");
                    dlg.getEditor().setPromptText("URL ou code Microsoft ici…");
                    dlg.showAndWait().ifPresent(codeRef::set);
                    latch.countDown();
                });

                latch.await();                      // attend que l’utilisateur colle quelque chose
                String authCode = codeRef.get();
                if (authCode == null || authCode.trim().isEmpty())
                    throw new IllegalStateException("Aucun code n’a été fourni.");

                /* 3) si l’utilisateur a collé l’URL complète, on isole le code */
                authCode = authCode.trim();
                if (authCode.startsWith("http")) {
                    int p = authCode.indexOf("code=") + 5;
                    authCode = authCode.substring(p);
                    int amp = authCode.indexOf('&');
                    if (amp > 0) authCode = authCode.substring(0, amp);
                }

                /* 4) échange code → access_token → session Minecraft */
                MicrosoftModel model   = msAuth.getAuthorizationCode(ParamType.AUTH, authCode);
                Session        session = msAuth.getLiveToken(model.getAccess_token());

                /* 5) retourne sur le thread JavaFX → mise à jour UI + config */
                Platform.runLater(() -> {
                    wait.close();
                    stage.close();

                    auth = new GameAuth(AccountType.MICROSOFT);
                    auth.setSession(session);

                    connectAccountPremiumCO(session.getUsername(), root);
                    config.updateValue("useMicrosoft", true);
                    update();    // lance la mise à jour du jeu / packs
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    wait.close();
                    stage.close();
                    showAuthErrorAlert();
                });
            }
        }).start();
    }


}
