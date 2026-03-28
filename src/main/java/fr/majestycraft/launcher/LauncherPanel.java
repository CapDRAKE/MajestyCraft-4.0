package fr.majestycraft.launcher;

import animatefx.animation.FadeIn;
import animatefx.animation.ZoomInDown;
import animatefx.animation.ZoomInLeft;
import animatefx.animation.ZoomOutDown;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;

import fr.majestycraft.App;
import fr.majestycraft.Discord;
import fr.majestycraft.Main;
import fr.majestycraft.Utils;
import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.updater.*;
import fr.trxyy.alternative.alternative_api.utils.*;
import fr.trxyy.alternative.alternative_api.utils.config.*;
import fr.trxyy.alternative.alternative_api_ui.LauncherAlert;
import fr.trxyy.alternative.alternative_api_ui.LauncherPane;
import fr.trxyy.alternative.alternative_api_ui.base.*;
import fr.trxyy.alternative.alternative_api_ui.components.*;
import fr.trxyy.alternative.alternative_auth.account.*;
import fr.trxyy.alternative.alternative_auth.base.*;
import fr.trxyy.alternative.alternative_auth.microsoft.MicrosoftXboxAuth;
import fr.trxyy.alternative.alternative_auth.microsoft.MicrosoftOAuthClient;
import fr.trxyy.alternative.alternative_auth.microsoft.model.MicrosoftModel;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.*;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.Desktop;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

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
    private GameUpdater gameUpdater;

    private MediaPlayer mediaPlayer;

    private LauncherImage titleImage;
    private LauncherImage heroLogo;

    private LauncherLabel heroTitleLabel;
    private LauncherLabel heroSubtitleLabel;
    private LauncherLabel heroTextLine1;
    private LauncherLabel heroTextLine2;
    private LauncherLabel connectionEyebrowLabel;
    private LauncherLabel connectionSubtitleLabel;
    private LauncherLabel microsoftHintLabel;

    private LauncherButton infoButton;
    private LauncherButton microsoftButton;
    private LauncherButton settingsButton;
    private LauncherButton packsButton;
    private LauncherButton modsButton;
    private LauncherButton shadersButton;

    private LauncherButton minestratorButton;
    private LauncherButton twitterButton;
    private LauncherButton tiktokButton;
    private LauncherButton youtubeButton;

    private LauncherButton voteButton;
    private LauncherButton boutiqueButton;

    private LauncherRectangle connexionRectangle;
    private LauncherLabel titleCrack;
    private JFXTextField usernameField;
    private JFXToggleButton rememberMe;
    private JFXButton loginButton;
    private JFXButton microsoftInlineButton;

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

    private int connX, connY, connW, connH;
    private int centerX;
    private int socialY;

    private final Preferences uiPreferences = Preferences.userNodeForPackage(LauncherPanel.class);
    private static final String SIDEBAR_HINT_PREFIX = "sidebar_hint_closed.";

    public LauncherPanel(Pane root, GameEngine engine) {
        this.engine = engine;

        this.drawBackgroundImage(engine, root, "heading.jpg");
        Platform.runLater(root::requestFocus);

        this.config = new LauncherConfig(engine);
        this.config.loadConfiguration();

        computeLayout();

        setupBackGround(root);
        initMusic();

        setupButtons(root);
        installSidebarHints(root);
        setupConnectionsGUI(root);
        setupUpdateGUI(root);

        initConfig(root);

        FadeIn fade = new FadeIn(root);
        fade.setSpeed(1.10);
        fade.play();

        Platform.runLater(() -> {
            playEntranceAnimations();
            playAmbientAnimations();
        });
    }

    private void computeLayout() {
        setCenterX(engine.getWidth() / 2);

        connW = 430;
        connH = 398;
        connX = engine.getWidth() - connW - 85;
        connY = 168;

        socialY = engine.getHeight() - 95;
    }

    private void applyModernCardStyle(LauncherRectangle r, double opacity) {
        r.setArcWidth(34);
        r.setArcHeight(34);
        r.setFill(Color.rgb(8, 12, 18, opacity));
        r.setStroke(Color.rgb(255, 255, 255, 0.10));
        r.setStrokeWidth(1.0);
        r.setEffect(new DropShadow(35, Color.rgb(0, 0, 0, 0.68)));
    }

    private void installHoverScale(Node node) {
        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(javafx.util.Duration.millis(120), node);
            st.setToX(1.06);
            st.setToY(1.06);
            st.play();
        });
        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(javafx.util.Duration.millis(120), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private void styleSidebarButton(LauncherButton button) {
        button.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.10);" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1;"
        );
    }

    private void styleGhostButton(LauncherButton button) {
        button.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.12);" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1;" +
                "-fx-text-fill: white;"
        );
    }

    private void stylePrimaryButton(JFXButton button) {
        button.setStyle(
                "-fx-background-color: linear-gradient(#ffb347, #ff7a00);" +
                "-fx-background-radius: 20;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: rgba(255,255,255,0.16);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 20;"
        );
        button.setEffect(new DropShadow(18, Color.rgb(255, 128, 0, 0.34)));
    }

    private void styleSecondaryButton(JFXButton button) {
        button.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-background-radius: 20;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: rgba(255,255,255,0.16);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 20;"
        );
        button.setEffect(new DropShadow(16, Color.rgb(0, 0, 0, 0.20)));
    }

    private void styleUsernameField(JFXTextField field) {
        field.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10);" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: rgba(255,255,255,0.14);" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 1;" +
                "-fx-text-fill: rgba(255,255,255,0.95);" +
                "-fx-prompt-text-fill: rgba(255,255,255,0.52);" +
                "-fx-padding: 0 18 0 18;" +
                "-jfx-focus-color: #ff8a00;" +
                "-jfx-unfocus-color: rgba(255,255,255,0.18);"
        );
    }

    private void styleModalActionButton(Button button, boolean primary) {
        String style = primary
                ? "-fx-background-color: linear-gradient(#ffb347, #ff7a00);"
                + "-fx-text-fill: white;"
                + "-fx-border-color: rgba(255,255,255,0.14);"
                : "-fx-background-color: rgba(255,255,255,0.08);"
                + "-fx-text-fill: white;"
                + "-fx-border-color: rgba(255,255,255,0.16);";
        button.setStyle(
                style +
                "-fx-background-radius: 18;" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1;" +
                "-fx-padding: 10 18 10 18;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;"
        );
    }

    private void animateFromLeft(Node node, int delayMs) {
        if (node == null) return;
        node.setOpacity(0);
        node.setTranslateX(-24);

        FadeTransition ft = new FadeTransition(javafx.util.Duration.millis(420), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(javafx.util.Duration.millis(420), node);
        tt.setFromX(-24);
        tt.setToX(0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setDelay(javafx.util.Duration.millis(delayMs));
        pt.play();
    }

    private void animateFromBottom(Node node, int delayMs) {
        if (node == null) return;
        node.setOpacity(0);
        node.setTranslateY(18);

        FadeTransition ft = new FadeTransition(javafx.util.Duration.millis(460), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(javafx.util.Duration.millis(460), node);
        tt.setFromY(18);
        tt.setToY(0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setDelay(javafx.util.Duration.millis(delayMs));
        pt.play();
    }

    private void animateFromRight(Node node, int delayMs) {
        if (node == null) return;
        node.setOpacity(0);
        node.setTranslateX(28);

        FadeTransition ft = new FadeTransition(javafx.util.Duration.millis(460), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(javafx.util.Duration.millis(460), node);
        tt.setFromX(28);
        tt.setToX(0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setDelay(javafx.util.Duration.millis(delayMs));
        pt.play();
    }

    private void playEntranceAnimations() {
        animateFromLeft(titleImage, 0);
        animateFromLeft(microsoftButton, 60);
        animateFromLeft(infoButton, 120);
        animateFromLeft(settingsButton, 180);
        animateFromLeft(packsButton, 240);
        animateFromLeft(modsButton, 300);
        animateFromLeft(shadersButton, 360);

        animateFromBottom(heroLogo, 90);
        animateFromBottom(heroTitleLabel, 160);
        animateFromBottom(heroSubtitleLabel, 220);
        animateFromBottom(heroTextLine1, 280);
        animateFromBottom(heroTextLine2, 330);
        animateFromBottom(voteButton, 380);
        animateFromBottom(boutiqueButton, 430);

        animateFromRight(connexionRectangle, 120);
        animateFromRight(connectionEyebrowLabel, 170);
        animateFromRight(titleCrack, 210);
        animateFromRight(connectionSubtitleLabel, 250);
        animateFromRight(avatar, 230);
        animateFromRight(usernameField, 300);
        animateFromRight(rememberMe, 350);
        animateFromRight(loginButton, 400);
        animateFromRight(microsoftInlineButton, 450);
        animateFromRight(microsoftHintLabel, 500);

        animateFromBottom(tiktokButton, 470);
        animateFromBottom(minestratorButton, 520);
        animateFromBottom(twitterButton, 570);
        animateFromBottom(youtubeButton, 620);
    }

    private void playAmbientAnimations() {
        if (heroLogo != null) {
            TranslateTransition floatLogo = new TranslateTransition(javafx.util.Duration.seconds(3.2), heroLogo);
            floatLogo.setFromY(0);
            floatLogo.setToY(10);
            floatLogo.setAutoReverse(true);
            floatLogo.setCycleCount(Animation.INDEFINITE);

            ScaleTransition pulseLogo = new ScaleTransition(javafx.util.Duration.seconds(3.2), heroLogo);
            pulseLogo.setFromX(1.0);
            pulseLogo.setFromY(1.0);
            pulseLogo.setToX(1.03);
            pulseLogo.setToY(1.03);
            pulseLogo.setAutoReverse(true);
            pulseLogo.setCycleCount(Animation.INDEFINITE);

            new ParallelTransition(floatLogo, pulseLogo).play();
        }

        if (titleImage != null) {
            ScaleTransition pulseMini = new ScaleTransition(javafx.util.Duration.seconds(2.6), titleImage);
            pulseMini.setFromX(1.0);
            pulseMini.setFromY(1.0);
            pulseMini.setToX(1.05);
            pulseMini.setToY(1.05);
            pulseMini.setAutoReverse(true);
            pulseMini.setCycleCount(Animation.INDEFINITE);
            pulseMini.play();
        }
    }

    public void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {}
    }

    private void checkAutoLogin(Pane root) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> checkAutoLogin(root));
            return;
        }

        if (!isAutoLoginEnabled()) return;

        String username = usernameField.getText();
        boolean isPasswordEmpty = true;

        if (isOfflineAccount(username, isPasswordEmpty)) {
            authenticateOffline(username);
            update();
            return;
        }

        if (isOnlineAccount()) {
            if (isMicrosoftAccount()) authenticateMicrosoft(root);
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
        return cfgBool(EnumConfig.USE_MICROSOFT);
    }

    private void authenticateOffline(String username) {
        auth = new GameAuth(username, "", AccountType.OFFLINE);
    }

    private void authenticateMicrosoft(Pane root) {
        auth = new GameAuth(AccountType.MICROSOFT);

        if (auth.trySilentRefresh(engine)) {
            Session s = auth.getSession();
            connectAccountPremiumCO(s.getUsername(), root);
            config.updateValue("username", s.getUsername());
            config.updateValue("useMicrosoft", true);
            update();
            return;
        }

        showMicrosoftAuth(root);
    }

    private void launchMicrosoftFlow(Pane root) {
        if (!App.netIsAvailable()) {
            showConnectionErrorAlert();
            return;
        }
        authenticateMicrosoft(root);
    }

    private void showOfflineError() {
        Platform.runLater(() -> new LauncherAlert(ERROR_AUTH_FAILED, ERROR_OFFLINE_MODE));
    }

    private void setupBackGround(Pane root) {
        Rectangle overlay = new Rectangle(engine.getWidth(), engine.getHeight());
        overlay.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 0, 0, 0.14)),
                new Stop(1, Color.rgb(0, 0, 0, 0.70))
        ));
        root.getChildren().add(overlay);

        LauncherRectangle leftDock = new LauncherRectangle(root, 0, 0, 84, engine.getHeight());
        leftDock.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(8, 12, 18, 0.85)),
                new Stop(1, Color.rgb(8, 12, 18, 0.28))
        ));

        LauncherRectangle separator = new LauncherRectangle(root, 83, 0, 1, engine.getHeight());
        separator.setFill(Color.rgb(255, 255, 255, 0.08));

        if (root.getScene() != null && !root.getScene().getStylesheets().contains("css/design.css")) {
            root.getScene().getStylesheets().add("css/design.css");
        }

        this.titleImage = new LauncherImage(root);
        this.titleImage.setImage(getResourceLocation().loadImage(engine, "launchergifpng.png"));
        this.titleImage.setSize(48, 48);
        this.titleImage.setBounds(18, 10, 48, 48);

        this.heroLogo = new LauncherImage(root);
        this.heroLogo.setImage(getResourceLocation().loadImage(engine, "launchergifpng.png"));
        this.heroLogo.setSize(180, 180);
        this.heroLogo.setBounds(170, 82, 180, 180);

        this.heroTitleLabel = new LauncherLabel(root);
        this.heroTitleLabel.setText("Rejoins MajestyCraft");
        this.heroTitleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        this.heroTitleLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.96)");
        this.heroTitleLabel.setPosition(135, 300);
        this.heroTitleLabel.setSize(320, 40);

        this.heroSubtitleLabel = new LauncherLabel(root);
        this.heroSubtitleLabel.setText("Launcher nouvelle génération");
        this.heroSubtitleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.heroSubtitleLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.95)");
        this.heroSubtitleLabel.setPosition(138, 344);
        this.heroSubtitleLabel.setSize(320, 26);

        this.heroTextLine1 = new LauncherLabel(root);
        this.heroTextLine1.setText("Multi-version • Forge • Optifine");
        this.heroTextLine1.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        this.heroTextLine1.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.70)");
        this.heroTextLine1.setPosition(138, 376);
        this.heroTextLine1.setSize(320, 24);

        this.heroTextLine2 = new LauncherLabel(root);
        this.heroTextLine2.setText("Choisis ton mode de jeu et lance-toi.");
        this.heroTextLine2.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        this.heroTextLine2.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.55)");
        this.heroTextLine2.setPosition(138, 400);
        this.heroTextLine2.setSize(320, 24);

        LauncherLabel titleLabel = new LauncherLabel(root);
        titleLabel.setText("MajestyLauncher");
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 17F));
        titleLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.92)");
        titleLabel.setPosition(engine.getWidth() / 2 - 85, 10);
        titleLabel.setSize(170, 26);
        titleLabel.setAlignment(Pos.CENTER);

        LauncherLabel subtitleLabel = new LauncherLabel(root);
        subtitleLabel.setText("Projet MajestyCraft");
        subtitleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        subtitleLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.42)");
        subtitleLabel.setPosition(engine.getWidth() / 2 - 85, 31);
        subtitleLabel.setSize(170, 18);
        subtitleLabel.setAlignment(Pos.CENTER);

        LauncherButton closeButton = new LauncherButton(root);
        LauncherImage closeImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "close.png"));
        closeImg.setSize(14, 14);
        closeButton.setGraphic(closeImg);
        closeButton.setBackground(null);
        closeButton.setPosition(engine.getWidth() - 32, 10);
        closeButton.setSize(16, 16);
        closeButton.setOnAction(event -> System.exit(0));

        LauncherButton reduceButton = new LauncherButton(root);
        LauncherImage reduceImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "reduce.png"));
        reduceImg.setSize(14, 14);
        reduceButton.setGraphic(reduceImg);
        reduceButton.setBackground(null);
        reduceButton.setPosition(engine.getWidth() - 60, 10);
        reduceButton.setSize(16, 16);
        reduceButton.setOnAction(event -> {
            Stage stage = (Stage) ((LauncherButton) event.getSource()).getScene().getWindow();
            stage.setIconified(true);
        });
    }

    private void initMusic() {
        Media media = getResourceLocation().getMedia(this.engine, "Minecraft.mp3");
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(0.05);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.play();
    }

    private void initConfig(Pane root) {
        boolean useDiscord = cfgBool(EnumConfig.USE_DISCORD);
        boolean useMusic = cfgBool(EnumConfig.USE_MUSIC);
        boolean useConnect = cfgBool(EnumConfig.USE_CONNECT);
        boolean useMicrosoft = cfgBool(EnumConfig.USE_MICROSOFT);
        boolean usePremium = cfgBool(EnumConfig.USE_PREMIUM);
        String username = (String) config.getValue(EnumConfig.USERNAME);

        if (useDiscord) rpc.start();
        else rpc.stop();

        mediaPlayer.setMute(!useMusic);

        if (useConnect) engine.reg(App.getGameConnect());

        if (useMicrosoft) {
            connectAccountPremium(username, root);
            connectAccountPremiumCO(username, root);
        } else if (usePremium) {
            connectAccountPremiumOFF(root);
            connectAccountCrackCO(root);
        } else {
            rememberMe.setSelected(false);
            connectAccountCrack(root);
            connectAccountCrackCO(root);
        }

        Utils.regGameStyle(engine, config);
    }

    private void setupButtons(Pane root) {
        int sbX = 17;
        int sbY = 122;
        int step = 72;
        int size = 50;

        this.microsoftButton = new LauncherButton(root);
        styleSidebarButton(this.microsoftButton);
        LauncherImage microsoftImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "microsoft.png"));
        microsoftImg.setSize(22, 22);
        this.microsoftButton.setGraphic(microsoftImg);
        this.microsoftButton.setPosition(sbX, sbY);
        this.microsoftButton.setSize(size, size);
        this.microsoftButton.setOnAction(event -> launchMicrosoftFlow(root));
        installHoverScale(this.microsoftButton);

        this.infoButton = new LauncherButton(root);
        styleSidebarButton(this.infoButton);
        LauncherImage infoImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "info.png"));
        infoImg.setSize(22, 22);
        this.infoButton.setGraphic(infoImg);
        this.infoButton.setPosition(sbX, sbY + step);
        this.infoButton.setSize(size, size);
        this.infoButton.setOnAction(event -> {
            Scene scene = new Scene(createInfoPanel());
            Stage stage = new Stage();
            scene.setFill(Color.TRANSPARENT);
            stage.setResizable(false);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Infos Launcher");
            stage.setWidth(1000);
            stage.setHeight(720);
            stage.setScene(scene);
            stage.show();
        });
        installHoverScale(this.infoButton);

        this.settingsButton = new LauncherButton(root);
        styleSidebarButton(this.settingsButton);
        LauncherImage settingsImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "settings.png"));
        settingsImg.setSize(22, 22);
        this.settingsButton.setGraphic(settingsImg);
        this.settingsButton.setPosition(sbX, sbY + step * 2);
        this.settingsButton.setSize(size, size);
        this.settingsButton.setOnAction(event -> {
            Scene scene = new Scene(createSettingsPanel(root));
            scene.setFill(Color.TRANSPARENT);
            if (!scene.getStylesheets().contains("css/design.css")) {
                scene.getStylesheets().add("css/design.css");
            }

            Stage stage = new Stage();
            stage.setResizable(false);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Paramètres Launcher");
            stage.setWidth(1000);
            stage.setHeight(750);
            stage.setScene(scene);
            stage.showAndWait();
        });
        installHoverScale(this.settingsButton);

        this.packsButton = new LauncherButton(root);
        styleSidebarButton(this.packsButton);
        LauncherImage packImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "pack.png"));
        packImg.setSize(22, 22);
        this.packsButton.setGraphic(packImg);
        this.packsButton.setPosition(sbX, sbY + step * 3);
        this.packsButton.setSize(size, size);
        this.packsButton.setOnAction(event -> {
            Scene scene = new Scene(createPacksPanel(root));
            Stage stage = new Stage();
            scene.setFill(Color.TRANSPARENT);
            stage.setResizable(false);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Packs Launcher");
            stage.setWidth(1180);
            stage.setHeight(820);
            stage.setScene(scene);
            stage.showAndWait();
        });
        installHoverScale(this.packsButton);

        this.modsButton = new LauncherButton(root);
        styleSidebarButton(this.modsButton);
        LauncherImage modsImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "mods.png"));
        modsImg.setSize(22, 22);
        this.modsButton.setGraphic(modsImg);
        this.modsButton.setPosition(sbX, sbY + step * 4);
        this.modsButton.setSize(size, size);
        this.modsButton.setOnAction(event -> {
            Scene scene = new Scene(createModsPanel(root));
            Stage stage = new Stage();
            scene.setFill(Color.TRANSPARENT);
            stage.setResizable(false);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Mods Launcher");
            stage.setWidth(1240);
            stage.setHeight(840);
            stage.setScene(scene);
            stage.showAndWait();
        });
        installHoverScale(this.modsButton);

        this.shadersButton = new LauncherButton(root);
        styleSidebarButton(this.shadersButton);
        LauncherImage shadersImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "shaderpacks.png"));
        shadersImg.setSize(22, 22);
        this.shadersButton.setGraphic(shadersImg);
        this.shadersButton.setPosition(sbX, sbY + step * 5);
        this.shadersButton.setSize(size, size);
        this.shadersButton.setOnAction(event -> {
            Scene scene = new Scene(createShadersPanel(root));
            Stage stage = new Stage();
            scene.setFill(Color.TRANSPARENT);
            stage.setResizable(false);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Shaderpacks Launcher");
            stage.setWidth(1180);
            stage.setHeight(820);
            stage.setScene(scene);
            stage.showAndWait();
        });
        installHoverScale(this.shadersButton);

        int quickW = 155;
        int quickH = 40;
        int quickGap = 16;
        int quickY = 468;

        this.voteButton = new LauncherButton(root);
        this.voteButton.setText(BUTTON_SITE);
        this.voteButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        this.voteButton.setPosition(138, quickY);
        this.voteButton.setSize(quickW, quickH);
        styleGhostButton(this.voteButton);
        this.voteButton.setOnAction(event -> openLink(SITE_URL));
        installHoverScale(this.voteButton);

        this.boutiqueButton = new LauncherButton(root);
        this.boutiqueButton.setText(BUTTON_DISCORD);
        this.boutiqueButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        this.boutiqueButton.setPosition(138 + quickW + quickGap, quickY);
        this.boutiqueButton.setSize(quickW, quickH);
        styleGhostButton(this.boutiqueButton);
        this.boutiqueButton.setOnAction(event -> openLink(DISCORD_URL));
        installHoverScale(this.boutiqueButton);

        int iconSize = 56;
        int iconGap = 84;
        int socialStartX = 145;

        this.tiktokButton = new LauncherButton(root);
        this.tiktokButton.setInvisible();
        this.tiktokButton.setPosition(socialStartX, socialY);
        LauncherImage tiktokImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "tiktok.png"));
        tiktokImg.setSize(iconSize, iconSize);
        this.tiktokButton.setGraphic(tiktokImg);
        this.tiktokButton.setSize(iconSize, iconSize);
        this.tiktokButton.setBackground(null);
        this.tiktokButton.setOnAction(event -> openLink(INSTAGRAM_URL));
        installHoverScale(this.tiktokButton);

        this.minestratorButton = new LauncherButton(root);
        this.minestratorButton.setInvisible();
        this.minestratorButton.setPosition(socialStartX + iconGap, socialY);
        LauncherImage minestratorImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "minestrator.png"));
        minestratorImg.setSize(iconSize, iconSize);
        this.minestratorButton.setGraphic(minestratorImg);
        this.minestratorButton.setSize(iconSize, iconSize);
        this.minestratorButton.setBackground(null);
        this.minestratorButton.setOnAction(event -> openLink(MINESTRATOR_URL));
        installHoverScale(this.minestratorButton);

        this.twitterButton = new LauncherButton(root);
        this.twitterButton.setInvisible();
        this.twitterButton.setPosition(socialStartX + iconGap * 2, socialY);
        LauncherImage twitterImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "twitter_icon.png"));
        twitterImg.setSize(iconSize, iconSize);
        this.twitterButton.setGraphic(twitterImg);
        this.twitterButton.setSize(iconSize, iconSize);
        this.twitterButton.setBackground(null);
        this.twitterButton.setOnAction(event -> openLink(TWITTER_URL));
        installHoverScale(this.twitterButton);

        this.youtubeButton = new LauncherButton(root);
        this.youtubeButton.setInvisible();
        this.youtubeButton.setPosition(socialStartX + iconGap * 3, socialY);
        LauncherImage youtubeImg = new LauncherImage(root, getResourceLocation().loadImage(engine, "yt_icon.png"));
        youtubeImg.setSize(iconSize, iconSize);
        this.youtubeButton.setGraphic(youtubeImg);
        this.youtubeButton.setSize(iconSize, iconSize);
        this.youtubeButton.setBackground(null);
        this.youtubeButton.setOnAction(event -> openLink(YOUTUBE_URL));
        installHoverScale(this.youtubeButton);
    }

    private void setupConnectionsGUI(Pane root) {
        Rectangle connectionGlow = new Rectangle(connX - 22, connY - 22, connW + 44, connH + 44);
        connectionGlow.setArcWidth(46);
        connectionGlow.setArcHeight(46);
        connectionGlow.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 166, 0, 0.15)),
                new Stop(0.50, Color.rgb(255, 255, 255, 0.03)),
                new Stop(1, Color.rgb(255, 122, 0, 0.08))
        ));
        connectionGlow.setEffect(new DropShadow(54, Color.rgb(255, 132, 0, 0.18)));
        connectionGlow.setMouseTransparent(true);
        root.getChildren().add(connectionGlow);

        this.connexionRectangle = new LauncherRectangle(root, connX, connY, connW, connH);
        applyModernCardStyle(this.connexionRectangle, 0.80);
        this.connexionRectangle.setMouseTransparent(true);

        LauncherRectangle avatarCard = new LauncherRectangle(root, connX + 28, connY + 148, 82, 82);
        avatarCard.setArcWidth(24);
        avatarCard.setArcHeight(24);
        avatarCard.setFill(Color.rgb(255, 255, 255, 0.06));
        avatarCard.setStroke(Color.rgb(255, 255, 255, 0.08));
        avatarCard.setStrokeWidth(1);
        avatarCard.setMouseTransparent(true);

        this.connectionEyebrowLabel = new LauncherLabel(root);
        this.connectionEyebrowLabel.setText("ESPACE COMPTE");
        this.connectionEyebrowLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.connectionEyebrowLabel.setStyle(
                "-fx-background-color: rgba(255,159,26,0.14);" +
                "-fx-background-radius: 999;" +
                "-fx-text-fill: rgba(255,207,138,0.95);"
        );
        this.connectionEyebrowLabel.setAlignment(Pos.CENTER);
        this.connectionEyebrowLabel.setPosition(connX + 28, connY + 26);
        this.connectionEyebrowLabel.setSize(118, 24);

        this.titleCrack = new LauncherLabel(root);
        this.titleCrack.setText(LABEL_CONNECTION);
        this.titleCrack.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        this.titleCrack.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        this.titleCrack.setAlignment(Pos.CENTER_LEFT);
        this.titleCrack.setPosition(connX + 28, connY + 58);
        this.titleCrack.setSize(connW - 56, 40);

        this.connectionSubtitleLabel = new LauncherLabel(root);
        this.connectionSubtitleLabel.setText("Connecte-toi avec ton pseudo hors-ligne ou passe par Microsoft pour retrouver ton profil.");
        this.connectionSubtitleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.connectionSubtitleLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.62);");
        this.connectionSubtitleLabel.setAlignment(Pos.CENTER_LEFT);
        this.connectionSubtitleLabel.setPosition(connX + 28, connY + 98);
        this.connectionSubtitleLabel.setSize(connW - 56, 38);

        this.usernameField = new JFXTextField();
        this.usernameField.setLayoutX(connX + 126);
        this.usernameField.setLayoutY(connY + 162);
        this.usernameField.setPrefWidth(connW - 156);
        this.usernameField.setPrefHeight(54);
        this.usernameField.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        this.usernameField.setPromptText(INPUT_PSEUDO_OR_EMAIL);
        styleUsernameField(this.usernameField);

        if (!cfgBool(EnumConfig.USE_MICROSOFT)) {
            this.usernameField.setText((String) this.config.getValue(EnumConfig.USERNAME));
        }
        root.getChildren().add(this.usernameField);

        this.rememberMe = new JFXToggleButton();
        this.rememberMe.setText(LABEL_REMEMBER_ME);
        this.rememberMe.setSelected(cfgBool(EnumConfig.REMEMBER_ME));
        this.rememberMe.getStyleClass().add("jfx-toggle-button");
        this.rememberMe.setLayoutX(connX + 112);
        this.rememberMe.setLayoutY(connY + 246);
        this.rememberMe.setOnAction(event -> config.updateValue("rememberme", rememberMe.isSelected()));
        root.getChildren().add(this.rememberMe);

        this.loginButton = new JFXButton(BUTTON_LOGIN);
        this.loginButton.setLayoutX(connX + 28);
        this.loginButton.setLayoutY(connY + 288);
        this.loginButton.setPrefWidth(176);
        this.loginButton.setPrefHeight(50);
        this.loginButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        stylePrimaryButton(this.loginButton);
        installHoverScale(this.loginButton);

        this.loginButton.setOnAction(event -> {
            if (!App.netIsAvailable()) {
                Platform.runLater(() -> new LauncherAlert(AUTH_FAILED, OFFLINE_MODE_ALERT));
                return;
            }

            config.updateValue("useMicrosoft", false);

            String username = usernameField.getText();

            if (username.length() <= 3) {
                new LauncherAlert(AUTH_FAILED, USERNAME_ALERT);
                return;
            }

            auth = new GameAuth(username, "", AccountType.OFFLINE);
            connectAccountCrackCO(root);

            if (auth.isLogged()) {
                config.updateValue("username", username);
                update();
            } else {
                new LauncherAlert(AUTH_FAILED, ONLINE_MODE_ALERT);
            }
        });
        root.getChildren().add(this.loginButton);

        this.microsoftInlineButton = new JFXButton("Connexion Microsoft");
        this.microsoftInlineButton.setLayoutX(connX + 224);
        this.microsoftInlineButton.setLayoutY(connY + 288);
        this.microsoftInlineButton.setPrefWidth(connW - 252);
        this.microsoftInlineButton.setPrefHeight(50);
        this.microsoftInlineButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        styleSecondaryButton(this.microsoftInlineButton);
        ImageView microsoftInlineIcon = new ImageView(getResourceLocation().loadImage(engine, "microsoft.png"));
        microsoftInlineIcon.setFitWidth(18);
        microsoftInlineIcon.setFitHeight(18);
        this.microsoftInlineButton.setGraphic(microsoftInlineIcon);
        this.microsoftInlineButton.setGraphicTextGap(10);
        this.microsoftInlineButton.setContentDisplay(ContentDisplay.LEFT);
        this.microsoftInlineButton.setOnAction(event -> launchMicrosoftFlow(root));
        installHoverScale(this.microsoftInlineButton);
        root.getChildren().add(this.microsoftInlineButton);

        this.microsoftHintLabel = new LauncherLabel(root);
        this.microsoftHintLabel.setText("Connexion Microsoft via l’icône à gauche");
        this.microsoftHintLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.microsoftHintLabel.setText("Tu peux aussi utiliser l'icone Microsoft dans la barre laterale.");
        this.microsoftHintLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.48);");
        this.microsoftHintLabel.setPosition(connX + 28, connY + 352);
        this.microsoftHintLabel.setSize(connW - 56, 18);
        this.microsoftHintLabel.setAlignment(Pos.CENTER);

        int autoCardWidth = 620;
        int autoCardX = engine.getWidth() / 2 - autoCardWidth / 2;
        int autoCardY = engine.getHeight() - 102;

        this.autoLoginRectangle = new LauncherRectangle(root, autoCardX, autoCardY, autoCardWidth, 76);
        this.autoLoginRectangle.setArcWidth(28);
        this.autoLoginRectangle.setArcHeight(28);
        this.autoLoginRectangle.setFill(Color.rgb(8, 12, 18, 0.90));
        this.autoLoginRectangle.setStroke(Color.rgb(255, 255, 255, 0.12));
        this.autoLoginRectangle.setStrokeWidth(1.0);
        this.autoLoginRectangle.setEffect(new DropShadow(28, Color.rgb(0, 0, 0, 0.42)));
        this.autoLoginRectangle.setOpacity(1.0);
        this.autoLoginRectangle.setVisible(false);

        this.autoLoginLabel = new LauncherLabel(root);
        this.autoLoginLabel.setText(LABEL_OFFLINE_CONNECTION);
        this.autoLoginLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 18F));
        this.autoLoginLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.92);");
        this.autoLoginLabel.setPosition(autoCardX + 28, autoCardY + 18);
        this.autoLoginLabel.setOpacity(0.95);
        this.autoLoginLabel.setSize(340, 40);
        this.autoLoginLabel.setVisible(false);

        this.autoLoginButton = new LauncherButton(root);
        this.autoLoginButton.setText(BUTTON_CANCEL);
        this.autoLoginButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.autoLoginButton.setPosition(autoCardX + autoCardWidth - 244, autoCardY + 20);
        this.autoLoginButton.setSize(104, 34);
        this.autoLoginButton.setStyle(
                "-fx-background-color: rgba(255, 92, 92, 0.22);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.12);" +
                "-fx-border-radius: 18;" +
                "-fx-text-fill: white;"
        );
        this.autoLoginButton.setVisible(false);
        this.autoLoginButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                if (autoLoginTimer != null) autoLoginTimer.cancel();
                autoLoginLabel.setVisible(false);
                autoLoginButton.setVisible(false);
                autoLoginRectangle.setVisible(false);
                autoLoginButton2.setVisible(false);
            }
        });

        this.autoLoginButton2 = new LauncherButton(root);
        this.autoLoginButton2.setText(AUTOLOGIN_START);
        this.autoLoginButton2.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.autoLoginButton2.setPosition(autoCardX + autoCardWidth - 126, autoCardY + 20);
        this.autoLoginButton2.setSize(104, 34);
        this.autoLoginButton2.setStyle(
                "-fx-background-color: rgba(255, 166, 0, 0.26);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.12);" +
                "-fx-border-radius: 18;" +
                "-fx-text-fill: white;"
        );
        this.autoLoginButton2.setVisible(false);
        this.autoLoginButton2.setOnAction(event -> {
            if (!engine.getGameMaintenance().isAccessBlocked()) {
                if (autoLoginTimer != null) autoLoginTimer.cancel();
                autoLoginLabel.setVisible(false);
                autoLoginButton.setVisible(false);
                autoLoginRectangle.setVisible(false);
                autoLoginButton2.setVisible(false);
                if (cfgBool(EnumConfig.USE_CONNECT)) engine.reg(App.getGameConnect());
                checkAutoLogin(root);
            }
        });

        if (this.config.getValue(EnumConfig.AUTOLOGIN).equals(true)) {
            Platform.runLater(() -> {
                autoLoginTimer = new Timer("MajestyLauncher-AutoLogin", true);
                TimerTask timerTask = new TimerTask() {
                    final int waitTime = 7;
                    int elapsed = 0;

                    @Override
                    public void run() {
                        elapsed++;
                        if (elapsed % waitTime == 0) {
                            if (!engine.getGameMaintenance().isAccessBlocked()) {
                                Platform.runLater(() -> {
                                    autoLoginTimer.cancel();
                                    autoLoginLabel.setVisible(false);
                                    autoLoginButton.setVisible(false);
                                    autoLoginRectangle.setVisible(false);
                                    autoLoginButton2.setVisible(false);
                                    checkAutoLogin(root);
                                });
                            }
                        } else {
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
        this.updateRectangle = new LauncherRectangle(root, engine.getWidth() / 2 - 175, engine.getHeight() / 2 - 80, 350, 180);
        applyModernCardStyle(this.updateRectangle, 0.62);
        this.updateRectangle.setVisible(false);

        this.updateLabel = new LauncherLabel(root);
        this.updateLabel.setText(UPDATE_LABEL_TEXT);
        this.updateLabel.setAlignment(Pos.CENTER);
        this.updateLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 22F));
        this.updateLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: orange;");
        this.updateLabel.setPosition(engine.getWidth() / 2 - 95, engine.getHeight() / 2 - 75);
        this.updateLabel.setSize(190, 40);
        this.updateLabel.setVisible(false);

        this.currentStep = new LauncherLabel(root);
        this.currentStep.setText(UPDATE_STEP_TEXT);
        this.currentStep.setFont(Font.font("Verdana", FontPosture.ITALIC, 18F));
        this.currentStep.setStyle("-fx-background-color: transparent; -fx-text-fill: orange;");
        this.currentStep.setAlignment(Pos.CENTER);
        this.currentStep.setPosition(engine.getWidth() / 2 - 160, engine.getHeight() / 2 + 63);
        this.currentStep.setOpacity(0.4);
        this.currentStep.setSize(320, 40);
        this.currentStep.setVisible(false);

        this.currentFileLabel = new LauncherLabel(root);
        this.currentFileLabel.setText("launchwrapper-12.0.jar");
        this.currentFileLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 18F));
        this.currentFileLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: orange;");
        this.currentFileLabel.setAlignment(Pos.CENTER);
        this.currentFileLabel.setPosition(engine.getWidth() / 2 - 160, engine.getHeight() / 2 + 5);
        this.currentFileLabel.setOpacity(0.8);
        this.currentFileLabel.setSize(320, 40);
        this.currentFileLabel.setVisible(false);

        this.percentageLabel = new LauncherLabel(root);
        this.percentageLabel.setText("0%");
        this.percentageLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 30F));
        this.percentageLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: orange;");
        this.percentageLabel.setAlignment(Pos.CENTER);
        this.percentageLabel.setPosition(engine.getWidth() / 2 - 50, engine.getHeight() / 2 - 25);
        this.percentageLabel.setOpacity(0.8);
        this.percentageLabel.setSize(100, 40);
        this.percentageLabel.setVisible(false);

        this.bar = new JFXProgressBar();
        this.bar.setLayoutX(engine.getWidth() / 2 - 125);
        this.bar.setLayoutY(engine.getHeight() / 2 + 40);
        this.bar.getStyleClass().add("jfx-progress-bar");
        this.bar.setVisible(false);
        root.getChildren().add(this.bar);
    }

    public void update() {
        if (microsoftButton != null) new ZoomOutDown(microsoftButton).setResetOnFinished(false).play();
        if (infoButton != null) new ZoomOutDown(infoButton).setResetOnFinished(false).play();
        if (settingsButton != null) new ZoomOutDown(settingsButton).setResetOnFinished(false).play();
        if (packsButton != null) new ZoomOutDown(packsButton).setResetOnFinished(false).play();
        if (modsButton != null) new ZoomOutDown(modsButton).setResetOnFinished(false).play();
        if (shadersButton != null) new ZoomOutDown(shadersButton).setResetOnFinished(false).play();
        
        if (microsoftHintLabel != null) new ZoomOutDown(microsoftHintLabel).setResetOnFinished(false).play();
        if (connectionEyebrowLabel != null) new ZoomOutDown(connectionEyebrowLabel).setResetOnFinished(false).play();
        if (connectionSubtitleLabel != null) new ZoomOutDown(connectionSubtitleLabel).setResetOnFinished(false).play();

        if (heroLogo != null) new ZoomOutDown(heroLogo).setResetOnFinished(false).play();
        if (heroTitleLabel != null) new ZoomOutDown(heroTitleLabel).setResetOnFinished(false).play();
        if (heroSubtitleLabel != null) new ZoomOutDown(heroSubtitleLabel).setResetOnFinished(false).play();
        if (heroTextLine1 != null) new ZoomOutDown(heroTextLine1).setResetOnFinished(false).play();
        if (heroTextLine2 != null) new ZoomOutDown(heroTextLine2).setResetOnFinished(false).play();

        if (titleCrack != null) new ZoomOutDown(titleCrack).setResetOnFinished(false).play();
        if (usernameField != null) new ZoomOutDown(usernameField).setResetOnFinished(false).play();
        if (rememberMe != null) new ZoomOutDown(rememberMe).setResetOnFinished(false).play();
        if (loginButton != null) new ZoomOutDown(loginButton).setResetOnFinished(false).play();
        if (microsoftInlineButton != null) new ZoomOutDown(microsoftInlineButton).setResetOnFinished(false).play();
        if (connexionRectangle != null) new ZoomOutDown(connexionRectangle).setResetOnFinished(false).play();

        if (voteButton != null) new ZoomOutDown(voteButton).setResetOnFinished(false).play();
        if (boutiqueButton != null) new ZoomOutDown(boutiqueButton).setResetOnFinished(false).play();

        if (tiktokButton != null) new ZoomOutDown(tiktokButton).setResetOnFinished(false).play();
        if (minestratorButton != null) new ZoomOutDown(minestratorButton).setResetOnFinished(false).play();
        if (twitterButton != null) new ZoomOutDown(twitterButton).setResetOnFinished(false).play();
        if (youtubeButton != null) new ZoomOutDown(youtubeButton).setResetOnFinished(false).play();

        if (avatar != null) new ZoomOutDown(avatar).setResetOnFinished(false).play();

        if (usernameField != null) usernameField.setDisable(true);
        if (connexionRectangle != null) connexionRectangle.setDisable(true);
        if (rememberMe != null) rememberMe.setDisable(true);
        if (loginButton != null) loginButton.setDisable(true);
        if (microsoftInlineButton != null) microsoftInlineButton.setDisable(true);
        if (settingsButton != null) settingsButton.setDisable(true);
        if (microsoftHintLabel != null) microsoftHintLabel.setVisible(false);
        if (connectionSubtitleLabel != null) connectionSubtitleLabel.setVisible(false);

        updateRectangle.setVisible(true);
        updateLabel.setVisible(true);
        currentStep.setVisible(true);
        currentFileLabel.setVisible(true);
        percentageLabel.setVisible(true);
        bar.setVisible(true);

        if (avatar != null) avatar.setVisible(false);

        new ZoomInDown(updateRectangle).play();
        new ZoomInDown(updateLabel).play();
        new ZoomInDown(currentStep).play();
        new ZoomInDown(currentFileLabel).play();
        new ZoomInDown(percentageLabel).play();
        new ZoomInDown(bar).play();

        if (updateAvatar != null) {
            new ZoomInDown(updateAvatar).play();
            updateAvatar.setVisible(true);
        }

        // IMPORTANT:
        // Do NOT override GameLinks here with the MajestyCraft JSON URL.
        // The selected links are already resolved earlier:
        // - vanilla => Mojang source JSON
        // - forge => Mojang version JSON + official Forge metadata/libraries
        // - optifine => MajestyCraft server JSON
        // LauncherSettings and App already register the correct GameLinks in the engine.

        this.gameUpdater = new GameUpdater();
        this.gameUpdater.reg(engine);
        this.gameUpdater.reg(auth.getSession());

        engine.reg(GameMemory.getMemory(Double.parseDouble((String) this.config.getValue(EnumConfig.RAM))));
        engine.reg(GameSize.getWindowSize(Integer.parseInt((String) this.config.getValue(EnumConfig.GAME_SIZE))));

        if (cfgBool(EnumConfig.USE_CONNECT)) engine.reg(App.getGameConnect());

        boolean useVmArgs = (Boolean) config.getValue(EnumConfig.USE_VM_ARGUMENTS);
        String vmArgs = (String) config.getValue(EnumConfig.VM_ARGUMENTS);
        if (useVmArgs && vmArgs != null && vmArgs.length() > 3) {
            String[] s = vmArgs.split(" ");
            engine.reg(new JVMArguments(s));
        }

        engine.reg(this.gameUpdater);

        this.gameUpdater.start();

        Timeline timeline = new Timeline(
                new KeyFrame(javafx.util.Duration.seconds(0.0D), event -> timelineUpdate(engine)),
                new KeyFrame(javafx.util.Duration.seconds(0.1D))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private double percent;

    public void timelineUpdate(GameEngine engine) {
        if (engine.getGameUpdater() != null && engine.getGameUpdater().filesToDownload > 0) {
            this.percent = engine.getGameUpdater().downloadedFiles * 100.0D / engine.getGameUpdater().filesToDownload;
            this.percentageLabel.setText(decimalFormat.format(percent) + "%");
        } else {
            this.percent = 0;
            this.percentageLabel.setText("0%");
        }
        if (engine.getGameUpdater() != null) {
            this.currentFileLabel.setText(engine.getGameUpdater().getCurrentFile());
            this.currentStep.setText(engine.getGameUpdater().getCurrentInfo());
        }
        this.bar.setProgress(percent / 100.0D);
    }

    private Parent createSettingsPanel(Pane root) {
        root = new LauncherPane(engine);
        Rectangle rect = new Rectangle(1000, 1000);
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
        Rectangle rect = new Rectangle(1500, 900);
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
        Rectangle rect = new Rectangle(1180, 820);
        rect.setArcHeight(15.0);
        rect.setArcWidth(15.0);
        root.setClip(rect);
        root.setStyle("-fx-background-color: transparent;");
        new LauncherPacks(root, engine, this);
        new ZoomInLeft(rect).play();
        return root;
    }

    private Parent createModsPanel(Pane root) {
        root = new LauncherPane(engine);
        Rectangle rect = new Rectangle(1240, 840);
        rect.setArcHeight(15.0);
        rect.setArcWidth(15.0);
        root.setClip(rect);
        root.setStyle("-fx-background-color: transparent;");
        new LauncherMods(root, engine, this);
        new ZoomInLeft(rect).play();
        return root;
    }

    private Parent createShadersPanel(Pane root) {
        root = new LauncherPane(engine);
        Rectangle rect = new Rectangle(1180, 820);
        rect.setArcHeight(15.0);
        rect.setArcWidth(15.0);
        root.setClip(rect);
        root.setStyle("-fx-background-color: transparent;");
        new LauncherShaders(root, engine, this);
        new ZoomInLeft(rect).play();
        return root;
    }


    public void connectAccountCrack(Pane root) {
        avatar = new LauncherImage(root, new Image("https://minotar.net/cube/MHF_Steve.png"));
        avatar.setBounds(connX + 35, connY + 156, 68, 68);
    }

    public void connectAccountPremium(String username, Pane root) {
        avatar = new LauncherImage(root, new Image("https://minotar.net/cube/" + username + ".png"));
        avatar.setBounds(connX + 35, connY + 156, 68, 68);
    }

    public void connectAccountPremiumOFF(Pane root) {
        avatar = new LauncherImage(root, new Image("https://minotar.net/cube/MHF_Steve.png"));
        avatar.setBounds(connX + 35, connY + 156, 68, 68);
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

    private void showMicrosoftAuth(Pane root) {
        startMicrosoftLogin(root);
    }

    private String urlModifier(String version) {
        return Utils.resolveServerPath(version, Utils.resolveSelectedModloader(config));
    }

    public LauncherConfig getConfig() {
        return config;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public Discord getRpc() {
        return rpc;
    }

    private void showConnectionErrorAlert() {
        Platform.runLater(() -> new LauncherAlert(AUTH_ERROR_TITLE, CONNECTION_ERROR_MSG));
    }

    private void showAuthErrorAlert() {
        Platform.runLater(() -> new LauncherAlert(AUTH_ERROR_TITLE, AUTH_ERROR_MSG));
    }

    private void startMicrosoftLogin(Pane root) {
        Stage authStage = new Stage();
        authStage.initModality(Modality.APPLICATION_MODAL);
        authStage.initStyle(StageStyle.TRANSPARENT);
        authStage.setResizable(false);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(44, 44);
        spinner.setStyle("-fx-progress-color: #ff8a00;");

        Label badge = new Label("MICROSOFT");
        badge.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        badge.setStyle(
                "-fx-background-color: rgba(255,159,26,0.14);" +
                "-fx-background-radius: 999;" +
                "-fx-text-fill: rgba(255,207,138,0.95);" +
                "-fx-padding: 6 12 6 12;"
        );

        Label title = new Label("Connexion Microsoft");
        title.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 22F));
        title.setStyle("-fx-text-fill: white;");

        Label info = new Label(
                "Un navigateur s’est ouvert automatiquement.\n" +
                "Saisis le code ci-dessous sur la page Microsoft :"
        );
        info.setText(
                "Un navigateur s'ouvre automatiquement.\n" +
                "Valide le code ci-dessous sur la page Microsoft pour connecter ton compte."
        );
        info.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        info.setStyle("-fx-text-fill: rgba(255,255,255,0.68);");
        info.setWrapText(true);
        info.setAlignment(Pos.CENTER);

        Label codeLabel = new Label("-----");
        codeLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 28));
        codeLabel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.14);" +
                "-fx-border-radius: 18;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 12 24 12 24;"
        );

        Button copyBtn = new Button("📋 Copier le code");
        copyBtn.setDisable(true);
        copyBtn.setText("Copier le code");
        styleModalActionButton(copyBtn, true);

        Button openBtn = new Button("Ouvrir Microsoft");
        openBtn.setDisable(true);
        styleModalActionButton(openBtn, false);

        Label waitLabel = new Label("⏳ En attente de validation…");

        waitLabel.setText("En attente de validation Microsoft...");
        waitLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        waitLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.60);");

        HBox actions = new HBox(12, copyBtn, openBtn);
        actions.setAlignment(Pos.CENTER);

        VBox box = new VBox(15, title, info, codeLabel, copyBtn, spinner, waitLabel);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);

        box.getChildren().setAll(badge, title, info, codeLabel, actions, spinner, waitLabel);
        box.setSpacing(16);
        box.setPadding(new Insets(28));

        Rectangle modalCard = new Rectangle(500, 340);
        modalCard.setArcWidth(36);
        modalCard.setArcHeight(36);
        modalCard.setFill(Color.rgb(8, 12, 18, 0.96));
        modalCard.setStroke(Color.rgb(255, 255, 255, 0.12));
        modalCard.setStrokeWidth(1.0);
        modalCard.setEffect(new DropShadow(45, Color.rgb(0, 0, 0, 0.48)));

        StackPane modalRoot = new StackPane(modalCard, box);
        modalRoot.setPadding(new Insets(18));

        Scene authScene = new Scene(modalRoot, 540, 380);
        authScene.setFill(Color.TRANSPARENT);

        authStage.setScene(authScene);
        authStage.show();

        new Thread(() -> {
            try {
                MicrosoftOAuthClient deviceAuth = new MicrosoftOAuthClient();
                MicrosoftOAuthClient.DeviceCode deviceCode = deviceAuth.requestDeviceCode();

                Platform.runLater(() -> {
                    codeLabel.setText(deviceCode.getUserCode());
                    copyBtn.setDisable(false);
                    openBtn.setDisable(false);

                    copyBtn.setOnAction(e -> {
                        ClipboardContent content = new ClipboardContent();
                        content.putString(deviceCode.getUserCode());
                        Clipboard.getSystemClipboard().setContent(content);
                        copyBtn.setText("Code copie");
                    });

                    openBtn.setOnAction(e -> openLink(deviceCode.getVerificationUri()));
                });

                Desktop.getDesktop().browse(URI.create(deviceCode.getVerificationUri()));

                MicrosoftModel model = deviceAuth.pollForToken(deviceCode);

                AuthConfig authConfig = new AuthConfig(engine);
                authConfig.createConfigFile(model);

                MicrosoftXboxAuth msAuth = new MicrosoftXboxAuth();
                Session session = msAuth.getLiveToken(model.getAccess_token());

                Platform.runLater(() -> {
                    authStage.close();
                    connectAccountPremiumCO(session.getUsername(), root);
                    config.updateValue("username", session.getUsername());
                    config.updateValue("useMicrosoft", true);
                    update();
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    authStage.close();
                    showAuthErrorAlert();
                });
            }
        }).start();
    }

	public int getCenterX() {
		return centerX;
	}

	public void setCenterX(int centerX) {
		this.centerX = centerX;
	}
    private void installSidebarHints(Pane root) {
        addSidebarHint(root, microsoftButton, "Compte", "Connecte ton compte Microsoft pour récupérer ton pseudo et ton skin.", "microsoft", 0);
        addSidebarHint(root, infoButton, "Infos", "Retrouve les nouveautés du launcher et les informations utiles du serveur.", "info", 1);
        addSidebarHint(root, settingsButton, "Paramètres", "Ajuste la RAM, la taille de fenêtre et les options du launcher.", "settings", 2);
        addSidebarHint(root, packsButton, "Ressources", "Ajoute et gère tes resource packs directement depuis le launcher.", "packs", 3);
        addSidebarHint(root, modsButton, "Mods", "Parcours un catalogue de mods, ajoute des .jar ou des .zip et gère-les facilement.", "mods", 4);
        addSidebarHint(root, shadersButton, "Shaders", "Installe tes shaderpacks, explore le catalogue et améliore le rendu du jeu.", "shaders", 5);
    }

    private void addSidebarHint(Pane root, Node anchor, String title, String description, String keySuffix, int order) {
        if (anchor == null || isSidebarHintDismissed(keySuffix)) {
            return;
        }

        Pane bubble = buildSidebarHintBubble(title, description, keySuffix);
        bubble.setMouseTransparent(false);
        bubble.setManaged(false);
        bubble.setOpacity(0);

        double anchorX = anchor.getLayoutX();
        double anchorY = anchor.getLayoutY();
        double anchorHeight = anchor instanceof Region ? ((Region) anchor).getPrefHeight() : 50;

        bubble.setLayoutX(anchorX + 66);
        bubble.setLayoutY(anchorY + Math.max(0, (anchorHeight - 72) / 2.0));

        root.getChildren().add(bubble);

        FadeTransition fadeTransition = new FadeTransition(javafx.util.Duration.millis(320), bubble);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);
        fadeTransition.setDelay(javafx.util.Duration.millis(850 + order * 140L));
        fadeTransition.play();
    }

    private Pane buildSidebarHintBubble(String title, String description, String keySuffix) {
        final double bubbleWidth = 332;
        final double bubbleHeight = 72;

        Pane bubble = new Pane();
        bubble.setPrefSize(bubbleWidth, bubbleHeight);
        bubble.setMinSize(bubbleWidth, bubbleHeight);
        bubble.setMaxSize(bubbleWidth, bubbleHeight);

        Rectangle background = new Rectangle(bubbleWidth, bubbleHeight);
        background.setArcWidth(28);
        background.setArcHeight(28);
        background.setFill(Color.rgb(10, 14, 22, 0.94));
        background.setStroke(Color.rgb(255, 255, 255, 0.14));
        background.setStrokeWidth(1.0);
        background.setEffect(new DropShadow(24, Color.rgb(0, 0, 0, 0.48)));

        Polygon pointer = new Polygon(
                0.0, bubbleHeight / 2.0,
                16.0, bubbleHeight / 2.0 - 11,
                16.0, bubbleHeight / 2.0 + 11
        );
        pointer.setFill(Color.rgb(10, 14, 22, 0.94));
        pointer.setStroke(Color.rgb(255, 255, 255, 0.14));
        pointer.setStrokeWidth(1.0);
        pointer.setLayoutX(-14);
        pointer.setLayoutY(0);

        Label titleLabel = new Label(title);
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-font-weight: bold;");
        titleLabel.setLayoutX(18);
        titleLabel.setLayoutY(10);

        Label descriptionLabel = new Label(description);
        descriptionLabel.setFont(FontLoader.loadFont("Roboto-Light.ttf", "Roboto", 12F));
        descriptionLabel.setTextFill(Color.rgb(230, 235, 240, 0.94));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setPrefWidth(258);
        descriptionLabel.setMaxWidth(258);
        descriptionLabel.setLayoutX(18);
        descriptionLabel.setLayoutY(31);

        Button closeButton = new Button("×");
        closeButton.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 14));
        closeButton.setTextFill(Color.WHITE);
        closeButton.setContentDisplay(ContentDisplay.CENTER);
        closeButton.setMnemonicParsing(false);
        closeButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10);" +
                "-fx-background-radius: 16;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 0;" +
                "-fx-border-color: rgba(255,255,255,0.16);" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;"
        );
        closeButton.setLayoutX(288);
        closeButton.setLayoutY(20);
        closeButton.setPrefSize(28, 28);
        closeButton.setMinSize(28, 28);
        closeButton.setMaxSize(28, 28);
        closeButton.setFocusTraversable(false);
        installHoverScale(closeButton);
        closeButton.setOnAction(event -> dismissSidebarHint(keySuffix, bubble));

        bubble.getChildren().addAll(pointer, background, titleLabel, descriptionLabel, closeButton);
        return bubble;
    }

    private boolean isSidebarHintDismissed(String keySuffix) {
        return uiPreferences.getBoolean(SIDEBAR_HINT_PREFIX + keySuffix, false);
    }

    private boolean cfgBool(EnumConfig key) {
        Object v = config.getValue(key);
        return v instanceof Boolean ? (Boolean) v : false;
    }

    private void dismissSidebarHint(String keySuffix, Node bubble) {
        uiPreferences.putBoolean(SIDEBAR_HINT_PREFIX + keySuffix, true);

        FadeTransition fadeTransition = new FadeTransition(javafx.util.Duration.millis(180), bubble);
        fadeTransition.setFromValue(bubble.getOpacity());
        fadeTransition.setToValue(0);
        fadeTransition.setOnFinished(e -> {
            Parent parent = bubble.getParent();
            if (parent instanceof Pane) {
                ((Pane) parent).getChildren().remove(bubble);
            }
        });
        fadeTransition.play();
    }

}
