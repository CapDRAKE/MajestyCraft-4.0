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
import javafx.animation.*;
import javafx.application.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.*;
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
    private LauncherButton minestratorButton;
    private LauncherButton twitterButton;
    private LauncherButton tiktokButton;
    private LauncherButton youtubeButton;
    private LauncherButton siteButton;
    private LauncherButton voteButton;
    private LauncherButton boutiqueButton;
    private LauncherButton lolButton;
    private LauncherButton lolButton2;
    private LauncherButton deadButton;

    private LauncherRectangle connexionRectangle;
    private LauncherLabel titleCrack;
    private JFXTextField usernameField;
    private JFXPasswordField passwordField;
    private JFXToggleButton rememberMe;
    private JFXButton loginButton;

    private LauncherRectangle autoLoginRectangle;
    private LauncherLabel autoLoginLabel;
    private LauncherButton autoLoginButton;
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

        checkAutoLogin(root);

    }



    private void checkAutoLogin(Pane root) {
        if (usernameField.getText().length() <= 2) {
            return;
        }

        if (!this.config.getValue(EnumConfig.AUTOLOGIN).equals(true)) {
            return;
        }

        boolean useMicrosoft = (boolean) config.getValue(EnumConfig.USE_MICROSOFT);
        boolean isOnline = App.netIsAvailable();
        String username = usernameField.getText();
        String password = passwordField.getText();
        boolean isPasswordEmpty = password.isEmpty();

        if (isOnline && useMicrosoft) {
            showMicrosoftAuth();
            auth = new GameAuth(AccountType.MICROSOFT);

            if (auth.isLogged()) {
                config.updateValue("useMicrosoft", true);
                connectAccountPremiumCO(auth.getSession().getUsername(), root);
                update();
                return;
            }
        }

        if (isPasswordEmpty) {
            auth = new GameAuth(username, "", AccountType.OFFLINE);
            update();
            return;
        }

        auth = new GameAuth(username, password, AccountType.MOJANG);

        if (auth.isLogged()) {
            update();
            return;
        }

        autoLoginLabel.setVisible(false);
        autoLoginButton.setVisible(false);
        autoLoginRectangle.setVisible(false);

        if (!isOnline) {
            Platform.runLater(() -> new LauncherAlert("Authentification échouée!",
                    "Impossible de se connecter, vous êtes en mode offline"
                            + " \nMerci de vous connecter en crack."));
            return;
        }

        Platform.runLater(() -> new LauncherAlert("Authentification échouée!",
                "Impossible de se connecter, l'authentification semble être une authentification 'en-ligne'"
                        + " \nIl y a un problème lors de la tentative de connexion. \n\n-Vérifiez que le pseudonyme comprenne au minimum 3 caractères. (compte non migré)"
                        + "\n-Faites bien attention aux majuscules et minuscules. \nAssurez-vous d'utiliser un compte Mojang. \nAssurez-vous de bien utiliser votre email dans le  cas d'une connexion avec un compte Mojang !"));
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
    	  boolean rememberMe = (boolean) config.getValue(EnumConfig.REMEMBER_ME);
    	  boolean useMicrosoft = (boolean) config.getValue(EnumConfig.USE_MICROSOFT);
    	  boolean usePremium = (boolean) config.getValue(EnumConfig.USE_PREMIUM);
    	  String password = (String) config.getValue(EnumConfig.PASSWORD);
    	  String username = (String) config.getValue(EnumConfig.USERNAME);
    	  String version = (String) config.getValue(EnumConfig.VERSION);
    	  String email = usernameField.getText();

    	  if (useDiscord) {
    	    rpc.start();
    	  } else {
    	    rpc.stop();
    	  }

    	  mediaPlayer.setMute(!useMusic);

    	  if (useConnect) {
    	    engine.reg(App.GAME_CONNECT);
    	  }

    	  if (rememberMe) {
    	    passwordField.setText(password);
    	  } else {
    	    passwordField.setText("");
    	  }

    	  if (useMicrosoft) {
    		connectAccountPremium(username, root);
    	    connectAccountPremiumCO(username, root);
    	  } else if (email.length() > 3 && email.contains("@")) {
    	    if (!passwordField.getText().isEmpty()) {
    	      GameAuth auth = new GameAuth(email, password, AccountType.MOJANG);
    	      if (auth.isLogged()) {
    	        connectAccountPremium(auth.getSession().getUsername(), root);
    	        connectAccountPremiumCO(auth.getSession().getUsername(), root);
    	      } else {
    	        connectAccountPremiumOFF(root);
    	        connectAccountCrackCO(root);
    	      }
    	    } else {
    	      connectAccountPremiumOFF(root);
    	      connectAccountCrackCO(root);
    	    }
    	  } else if (usePremium) {
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
        	  showMicrosoftAuth();
        	  if (auth.isLogged()) {
        	    connectAccountPremiumCO(auth.getSession().getUsername(), root);
        	    config.updateValue("useMicrosoft", true);
        	    update();
        	  } else {
        	    showAuthErrorAlert();
        	  }
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

        /* ===================== BOUTON easter egg 1 ===================== */
        this.lolButton = new LauncherButton(root);
        this.lolButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0); -fx-text-fill: orange");
        settingsImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "LOL.png"));
        settingsImg.setSize(27, 27);
        this.lolButton.setGraphic(settingsImg);
        this.lolButton.setPosition(engine.getWidth() / 2 - 522, engine.getHeight() / 2 + 50);
        this.lolButton.setSize(60, 46);
        this.lolButton.setOnAction(event -> openLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));

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
                new Hinge(lolButton).setResetOnFinished(true).play();
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
                new Hinge(passwordField).setResetOnFinished(true).play();
                new Hinge(loginButton).setResetOnFinished(true).play();
            }
        });

        /* ===================== BOUTON URL VOTE ===================== */
        this.voteButton = new LauncherButton(root);
        this.voteButton.setText("Site");
        this.voteButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 22F));
        this.voteButton.setPosition(engine.getWidth() / 2 - 260, engine.getHeight() / 2 + 190);
        this.voteButton.setSize(250, 45);
        this.voteButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0.4); -fx-text-fill: orange");
        this.voteButton.setOnAction(event -> openLink(SITE_URL));

        /* ===================== BOUTON URL BOUTIQUE ===================== */
        this.boutiqueButton = new LauncherButton(root);
        this.boutiqueButton.setText("Discord");
        this.boutiqueButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 22F));
        this.boutiqueButton.setPosition(engine.getWidth() / 2 - 125 + 130, engine.getHeight() / 2 + 190);
        this.boutiqueButton.setSize(250, 45);
        this.boutiqueButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0.4); -fx-text-fill: orange");
        this.boutiqueButton.setOnAction(event -> openLink(DISCORD_URL));

        /* ===================== BOUTON URL SITE ===================== */
        this.siteButton = new LauncherButton(root);
        this.siteButton.setText("Site");
        this.siteButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 22F));
        this.siteButton.setPosition(engine.getWidth() / 2 - 500, engine.getHeight() - 107);
        this.siteButton.setSize(200, 45);
        this.siteButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0.4); -fx-text-fill: orange");
        this.siteButton.setOnAction(event -> openLink(SITE_URL));
        this.siteButton.setVisible(false);

        /* ===================== BOUTON URL FORUM ===================== */
        LauncherButton forumButton = new LauncherButton(root);
        forumButton.setText("Discord");
        forumButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 22F));
        forumButton.setPosition(engine.getWidth() / 2 + 300, engine.getHeight() - 107);
        forumButton.setSize(200, 45);
        forumButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0.4); -fx-text-fill: orange");
        forumButton.setOnAction(event -> openLink(DISCORD_URL));
        forumButton.setVisible(false);

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

    private void setupConnectionsGUI(Pane root) {
        /* ===================== RECTANGLE DES CONNEXIONS ===================== */
        this.connexionRectangle = new LauncherRectangle(root, engine.getWidth() / 2 - 188, engine.getHeight() / 2 - 150,
                380, 320);
        this.connexionRectangle.setArcWidth(50.0);
        this.connexionRectangle.setArcHeight(50.0);
        this.connexionRectangle.setFill(Color.rgb(0, 0, 0, 0.30));
        this.connexionRectangle.setVisible(true);


        this.titleCrack = new LauncherLabel(root);
        this.titleCrack.setText("Connexion");
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
        this.usernameField.setPromptText("Pseudo or Email");
        if (!(boolean) config.getValue(EnumConfig.USE_MICROSOFT)) {
            this.usernameField.setText((String) this.config.getValue(EnumConfig.USERNAME));
        }
        root.getChildren().add(this.usernameField);

        this.passwordField = new JFXPasswordField();
        this.passwordField.setLayoutX((float) engine.getWidth() / 2 - 126);
        this.passwordField.setLayoutY((float) engine.getHeight() / 2 + 15);
        this.passwordField.getStyleClass().add("input");
        this.passwordField.setFont(FontLoader.loadFont("Roboto-Light.ttf", "Roboto Light", 14F));
        this.passwordField.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0.4); -fx-text-fill: orange");
        this.passwordField.setPromptText("Mot de passe (laisser vide pour crack)");
        root.getChildren().add(this.passwordField);

        this.rememberMe = new JFXToggleButton();
        this.rememberMe.setText("Se souvenir de moi");
        this.rememberMe.setSelected((boolean) config.getValue(EnumConfig.REMEMBER_ME));
        this.rememberMe.getStyleClass().add("jfx-toggle-button");
        this.rememberMe.setLayoutX(385);
        this.rememberMe.setLayoutY(427);
        this.rememberMe.setOnAction(event -> config.updateValue("rememberme", rememberMe.isSelected()));

        root.getChildren().add(this.rememberMe);

        /* ===================== BOUTON DE CONNEXION ===================== */
        this.loginButton = new JFXButton("Se connecter");
        this.loginButton.getStyleClass().add("button-raised");
        this.loginButton.setLayoutX(400);
        this.loginButton.setLayoutY(480);
        this.loginButton.setFont(FontLoader.loadFont("../resources/leadcoat.ttf", "leadcoat", 22F));
        // this.loginButton.setStyle("-fx-background-color: rgba(0 ,0 ,0 , 0.4);
        // -fx-text-fill: orange");
        this.loginButton.setOnAction(event -> {
            if (!App.netIsAvailable()) {
                Platform.runLater(() -> new LauncherAlert("Authentification échouée!", "Impossible de se connecter, vous êtes hors ligne. Merci de vous connecter en crack."));
                return;
            }

            config.updateValue("useMicrosoft", false);

            String username = usernameField.getText();
            String password = passwordField.getText();

            if (username.length() <= 3) {
                new LauncherAlert("Authentification échouée!",
                                    "Impossible de se connecter, l'authentification semble être une authentification 'hors-ligne'"
                                    + " \nIl y a un problème lors de la tentative de connexion. \n\n-Vérifiez que le pseudonyme comprenne au minimum 3 caractères.");
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
                new LauncherAlert("Authentification échouée!",
                                        "Impossible de se connecter, l'authentification semble être une authentification 'en-ligne'"
                                        + " \nIl y a un problème lors de la tentative de connexion. \n\n-Vérifiez que le pseudonyme comprenne au minimum 3 caractères. (compte non migré)"
                                        + "\n-Faites bien attention aux majuscules et minuscules. \nAssurez-vous d'utiliser un compte Mojang. \nAssurez-vous de bien utiliser votre email dans le cas d'une connexion avec un compte Mojang !");
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
        this.autoLoginLabel.setText("Connexion auto dans 3 secondes. Appuyez sur ECHAP pour annuler.");
        this.autoLoginLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 18F));
        this.autoLoginLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: red;");
        this.autoLoginLabel.setPosition(engine.getWidth() / 2 - 280, engine.getHeight() - 34);
        this.autoLoginLabel.setOpacity(0.7);
        this.autoLoginLabel.setSize(700, 40);
        this.autoLoginLabel.setVisible(false);

        /* ===================== ANNULER AUTOLOGIN ===================== */
        this.autoLoginButton = new LauncherButton(root);
        this.autoLoginButton.setText("Annuler");
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
            }
        });
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
        this.updateLabel.setText("- MISE A JOUR -");
        this.updateLabel.setAlignment(Pos.CENTER);
        this.updateLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 22F));
        this.updateLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: orange;");
        this.updateLabel.setPosition(engine.getWidth() / 2 - 95, engine.getHeight() / 2 - 75);
        this.updateLabel.setOpacity(1);
        this.updateLabel.setSize(190, 40);
        this.updateLabel.setVisible(false);

        /* =============== ETAPE DE MISE A JOUR =============== **/
        this.currentStep = new LauncherLabel(root);
        this.currentStep.setText("Preparation de la mise a jour.");
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
        new ZoomOutDown(this.passwordField).setResetOnFinished(false).play();
        new ZoomOutDown(this.boutiqueButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.avatar).setResetOnFinished(false).play();
        new ZoomOutDown(this.minestratorButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.twitterButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.tiktokButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.youtubeButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.deadButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.rememberMe).setResetOnFinished(false).play();
        new ZoomOutDown(this.loginButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.siteButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.voteButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.connexionRectangle).setResetOnFinished(false).play();
        new ZoomOutDown(this.lolButton).setResetOnFinished(false).play();
        new ZoomOutDown(this.lolButton2).setResetOnFinished(false).play();

        this.usernameField.setDisable(true);
        this.connexionRectangle.setDisable(true);
        this.rememberMe.setDisable(true);
        this.passwordField.setDisable(true);
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


    private void showMicrosoftAuth() {
        Scene scene = new Scene(createMicrosoftPanel());
        Stage stage = new Stage();
        scene.setFill(Color.TRANSPARENT);
        stage.setResizable(false);
        stage.setTitle("Microsoft Authentication");
        stage.setWidth(500);
        stage.setHeight(600);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();
    }

    private Parent createMicrosoftPanel() {
        LauncherPane contentPane = new LauncherPane(engine);
        auth.connectMicrosoft(engine, contentPane);
        return contentPane;
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
    	  Platform.runLater(() -> new LauncherAlert("Erreur d'authentification", "Impossible de se connecter, vous n'êtes pas connecté à internet."));
    }

    private void showAuthErrorAlert() {
    	  Platform.runLater(() -> new LauncherAlert("Erreur d'authentification", "Impossible de se connecter, identifiant ou mot de passe incorrect."));
    }
}
