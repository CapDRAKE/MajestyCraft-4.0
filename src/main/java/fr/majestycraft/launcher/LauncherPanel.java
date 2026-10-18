package fr.majestycraft.launcher;

import animatefx.animation.FadeIn;
import animatefx.animation.ZoomInDown;
import animatefx.animation.ZoomInLeft;
import animatefx.animation.ZoomOutDown;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;

import fr.majestycraft.App;
import fr.majestycraft.Discord;
import fr.majestycraft.Main;
import fr.majestycraft.Utils;
import fr.majestycraft.profiles.LauncherProfile;
import fr.majestycraft.profiles.ProfileManager;
import fr.majestycraft.util.AmbientBackground;
import fr.majestycraft.util.Icons;
import fr.majestycraft.util.UiStyle;
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
import fr.trxyy.alternative.alternative_auth.microsoft.MicrosoftBrowserAuth;
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
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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
import javafx.scene.text.TextAlignment;
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

    private LauncherButton infoButton;
    private LauncherButton microsoftButton;
    private LauncherButton settingsButton;
    private LauncherButton profilesButton;
    private LauncherButton packsButton;
    private LauncherButton modsButton;
    private LauncherButton shadersButton;
    /** Petit texte d'état sous l'icône Microsoft ("Hors ligne" / "Connecté"). */
    private LauncherLabel microsoftStateCaption;
    /** Toutes les légendes sous les icônes de la sidebar (masquées au lancement du jeu). */
    private final java.util.List<LauncherLabel> sidebarCaptions = new java.util.ArrayList<>();

    private LauncherButton minestratorButton;
    private LauncherButton twitterButton;
    private LauncherButton tiktokButton;
    private LauncherButton youtubeButton;

    private LauncherButton voteButton;
    private LauncherButton boutiqueButton;

    private Rectangle connectionGlow;
    private LauncherRectangle connexionRectangle;
    private LauncherRectangle avatarCard;
    private LauncherLabel titleCrack;
    private JFXTextField usernameField;
    private JFXToggleButton rememberMe;
    private JFXButton playButton;
    private JFXButton microsoftInlineButton;
    private JFXComboBox<String> profileCombo;
    private JFXButton manageProfilesButton;
    private LauncherLabel profileCaptionLabel;
    private Timeline updateTimeline;
    /** true quand une session Microsoft valide est active (JOUER lance avec ce compte). */
    private boolean microsoftSessionActive = false;
    /** évite deux connexions Microsoft simultanées (double clic, silent + manuel). */
    private boolean microsoftFlowRunning = false;
    /** l'auto-login a demandé un lancement pendant qu'une connexion était déjà en cours. */
    private boolean launchWhenConnected = false;
    /** évite de re-déclencher un lancement si un update est déjà en cours. */
    private boolean launchInProgress = false;

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
    /** Largeur de la fenetre "Compte Microsoft" ; borne aussi le retour a la ligne des textes. */
    private static final double MODAL_WIDTH = 500;
    /** Largeur utile du texte dans les cartes : largeur modale - marges modale - marges carte. */
    private static final double CARD_TEXT_WIDTH = MODAL_WIDTH - (28 * 2) - (18 * 2);

    public LauncherPanel(Pane root, GameEngine engine) {
        this.engine = engine;

        // Fond dessiné en code (lueurs, terrain en blocs, braises) : plus d'image de fond.
        AmbientBackground.install(root, engine.getWidth(), engine.getHeight(), true);
        Platform.runLater(root::requestFocus);

        this.config = new LauncherConfig(engine);
        this.config.loadConfiguration();

        // Système de profils : init (migration incluse) AVANT toute résolution de
        // GameLinks pour que le pont config + dossier de profil soient en place.
        ProfileManager.init(
                engine.getGameFolder().getGameDir(),
                engine.getGameFolder().getPlayDir(),
                this.config
        );
        ProfileManager.get().activateInitial(engine);

        // Fin de session de jeu : crédite le temps de jeu au profil de la session.
        fr.trxyy.alternative.alternative_api.build.GameRunner.gameSessionCallback = playedMs -> {
            ProfileManager pm = ProfileManager.get();
            if (pm != null) pm.addSessionPlaytime(playedMs);
        };

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
        connH = 430;
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
        animateFromLeft(profilesButton, 240);
        animateFromLeft(packsButton, 300);
        animateFromLeft(modsButton, 360);
        animateFromLeft(shadersButton, 420);

        animateFromBottom(heroLogo, 90);
        animateFromBottom(heroTitleLabel, 160);
        animateFromBottom(heroSubtitleLabel, 220);
        animateFromBottom(heroTextLine1, 280);
        animateFromBottom(heroTextLine2, 330);
        animateFromBottom(voteButton, 380);
        animateFromBottom(boutiqueButton, 430);

        animateFromRight(connectionGlow, 90);
        animateFromRight(connexionRectangle, 120);
        animateFromRight(connectionEyebrowLabel, 170);
        animateFromRight(titleCrack, 210);
        animateFromRight(connectionSubtitleLabel, 250);
        animateFromRight(avatarCard, 200);
        animateFromRight(avatar, 230);
        animateFromRight(usernameField, 300);
        animateFromRight(rememberMe, 340);
        animateFromRight(profileCaptionLabel, 370);
        animateFromRight(profileCombo, 390);
        animateFromRight(manageProfilesButton, 410);
        animateFromRight(playButton, 440);
        animateFromRight(microsoftInlineButton, 480);

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

        // Auto-login = seul chemin de lancement automatique (compte à rebours annulable)
        if (isMicrosoftAccount()) {
            if (isOnlineAccount()) {
                connectMicrosoftAsync(root, true, true);
            } else {
                showOfflineError();
            }
            return;
        }

        String username = usernameField.getText();
        if (username != null && username.trim().length() >= 2) {
            authenticateOffline(username.trim());
            startGame();
        }
    }

    private boolean isAutoLoginEnabled() {
        return this.config.getValue(EnumConfig.AUTOLOGIN).equals(true);
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

    /**
     * Bouton Microsoft : connecte le compte, ou propose de changer de compte /
     * se déconnecter si un compte est déjà actif. Ne lance JAMAIS le jeu : le
     * lancement passe uniquement par le bouton JOUER (ou l'auto-login).
     */
    private void launchMicrosoftFlow(Pane root) {
        if (microsoftSessionActive) {
            showAccountOptions(root);
            return;
        }
        if (!App.netIsAvailable()) {
            showConnectionErrorAlert();
            return;
        }
        connectMicrosoftAsync(root, false, true);
    }

    /**
     * Compte déjà connecté : propose de changer de compte ou de se déconnecter.
     * Seul le changement de compte supprime les jetons enregistrés (sinon la
     * connexion suivante se referait en silence sur le même compte) ; une simple
     * déconnexion les conserve pour permettre une reconnexion automatique.
     */
    private void showAccountOptions(Pane root) {
        String currentName = usernameField != null ? usernameField.getText() : "";

        // 0 = annulé, 1 = changer de compte, 2 = se déconnecter
        final int[] choice = { 0 };

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setResizable(false);

        Label badge = new Label("COMPTE MICROSOFT");
        badge.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
        badge.setStyle(
                "-fx-background-color: rgba(255,159,26,0.14);" +
                "-fx-background-radius: 999;" +
                "-fx-text-fill: rgba(255,207,138,0.95);" +
                "-fx-padding: 6 14 6 14;"
        );

        // Bandeau du compte connecté : avatar + pseudo + état
        ImageView head = new ImageView(new Image("https://minotar.net/helm/" + currentName + "/64.png", true));
        head.setFitWidth(46);
        head.setFitHeight(46);

        Label nameLabel = new Label(currentName);
        nameLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 18F));
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label stateLabel = new Label("● Connecté");
        stateLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        stateLabel.setStyle("-fx-text-fill: rgba(110,225,140,0.95);");

        VBox identity = new VBox(2, nameLabel, stateLabel);
        identity.setAlignment(Pos.CENTER_LEFT);

        HBox accountRow = new HBox(14, head, identity);
        accountRow.setAlignment(Pos.CENTER_LEFT);
        accountRow.setPadding(new Insets(12, 16, 12, 12));
        accountRow.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: rgba(255,255,255,0.10);" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;"
        );

        // Les deux actions, en cartes cliquables plutôt qu'en boutons plats
        Pane switchCard = buildAccountOptionCard(
                "Changer de compte",
                "Efface la session enregistrée et ouvre la page Microsoft pour te connecter avec un autre compte.",
                true,
                () -> { choice[0] = 1; dialog.close(); });

        Pane signOutCard = buildAccountOptionCard(
                "Se déconnecter",
                "Repasse en mode hors-ligne mais garde ta session : la prochaine connexion sera automatique.",
                false,
                () -> { choice[0] = 2; dialog.close(); });

        Label tip = new Label(
                "Si Microsoft te reconnecte automatiquement au même compte, choisis "
                + "« Utiliser un autre compte » sur sa page, ou ouvre-la en navigation privée.");
        tip.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
        tip.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.52);" +
                "-fx-background-color: rgba(255,159,26,0.06);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(255,159,26,0.16);" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-padding: 10 14 10 14;"
        );
        tip.setWrapText(true);
        tip.setMaxWidth(MODAL_WIDTH - (28 * 2));
        tip.setPrefWidth(MODAL_WIDTH - (28 * 2));

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: rgba(255,255,255,0.55);" +
                "-fx-font-family: 'Comfortaa';" +
                "-fx-font-size: 12px;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 6 18 6 18;"
        );
        cancelBtn.setOnAction(e -> dialog.close());
        HBox cancelRow = new HBox(cancelBtn);
        cancelRow.setAlignment(Pos.CENTER);

        // La carte porte elle-meme son fond : elle s'adapte ainsi a la hauteur reelle
        // du texte (un Rectangle de taille fixe tronquait les descriptions).
        VBox box = new VBox(14, badge, accountRow, switchCard, signOutCard, tip, cancelRow);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(26, 28, 20, 28));
        box.setPrefWidth(MODAL_WIDTH);
        box.setMaxWidth(MODAL_WIDTH);
        box.setStyle(
                "-fx-background-color: rgba(8,12,18,0.97);" +
                "-fx-background-radius: 28;" +
                "-fx-border-color: rgba(255,255,255,0.12);" +
                "-fx-border-radius: 28;" +
                "-fx-border-width: 1;"
        );
        box.setEffect(new DropShadow(45, Color.rgb(0, 0, 0, 0.48)));

        StackPane modalRoot = new StackPane(box);
        modalRoot.setPadding(new Insets(18));
        modalRoot.setStyle("-fx-background-color: transparent;");

        // Pas de dimensions imposees : la scene se dimensionne sur le contenu.
        Scene scene = new Scene(modalRoot);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        // Centrage une fois la fenetre affichee : avant, la scene etant auto-dimensionnee,
        // ses dimensions ne sont pas encore connues et le centrage serait fausse.
        dialog.setOnShown(e -> dialog.centerOnScreen());
        dialog.showAndWait();

        if (choice[0] == 0) {
            return;
        }

        boolean wantsSwitch = choice[0] == 1;
        if (wantsSwitch && !App.netIsAvailable()) {
            showConnectionErrorAlert();
            return;
        }

        // Seul le changement de compte purge les jetons : une simple déconnexion
        // les conserve pour permettre une reconnexion automatique.
        disconnectMicrosoft(root, wantsSwitch);

        if (wantsSwitch) {
            // Jetons supprimés : le refresh silencieux échouera forcément et le
            // device flow s'ouvrira, permettant de choisir un autre compte.
            connectMicrosoftAsync(root, false, true);
        }
    }

    /**
     * Carte d'action cliquable de la fenêtre "Compte Microsoft" : un titre, une
     * description, et un effet au survol. Plus lisible qu'un bouton seul, dont le
     * libellé ne suffit pas à expliquer ce qui va se passer.
     *
     * @param accent true pour l'action principale (bordure orangée)
     */
    private Pane buildAccountOptionCard(String title, String description, boolean accent, Runnable onClick) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        titleLabel.setStyle("-fx-text-fill: " + (accent ? "rgba(255,205,130,0.98)" : "rgba(255,255,255,0.92)") + "; -fx-font-weight: bold;");

        Label descLabel = new Label(description);
        descLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.58);");
        descLabel.setWrapText(true);
        // Largeur explicite : sans elle le Label garde sa largeur "une seule ligne"
        // et le texte est tronque au lieu de passer a la ligne.
        descLabel.setMaxWidth(CARD_TEXT_WIDTH);
        descLabel.setPrefWidth(CARD_TEXT_WIDTH);

        VBox cardBox = new VBox(4, titleLabel, descLabel);
        cardBox.setPadding(new Insets(14, 18, 16, 18));
        cardBox.setMaxWidth(Double.MAX_VALUE);
        cardBox.setCursor(javafx.scene.Cursor.HAND);

        final String idle = accent
                ? "-fx-background-color: rgba(255,150,0,0.10);-fx-background-radius: 16;-fx-border-color: rgba(255,170,0,0.45);-fx-border-radius: 16;-fx-border-width: 1;"
                : "-fx-background-color: rgba(255,255,255,0.05);-fx-background-radius: 16;-fx-border-color: rgba(255,255,255,0.10);-fx-border-radius: 16;-fx-border-width: 1;";
        final String hover = accent
                ? "-fx-background-color: rgba(255,150,0,0.20);-fx-background-radius: 16;-fx-border-color: rgba(255,180,40,0.75);-fx-border-radius: 16;-fx-border-width: 1;"
                : "-fx-background-color: rgba(255,255,255,0.10);-fx-background-radius: 16;-fx-border-color: rgba(255,255,255,0.22);-fx-border-radius: 16;-fx-border-width: 1;";

        cardBox.setStyle(idle);
        cardBox.setOnMouseEntered(e -> cardBox.setStyle(hover));
        cardBox.setOnMouseExited(e -> cardBox.setStyle(idle));
        cardBox.setOnMouseClicked(e -> onClick.run());
        return cardBox;
    }

    /**
     * Connexion Microsoft : d'abord un refresh silencieux (en arrière-plan, sans
     * bloquer l'UI), sinon le device flow avec la fenêtre de code.
     * @param launchAfter true = lance le jeu une fois connecté (auto-login)
     * @param interactive true = ouvre le device flow si le refresh silencieux échoue ;
     *                    false (reconnexion au démarrage) = échec discret, l'utilisateur
     *                    pourra cliquer sur le bouton Microsoft.
     */
    private void connectMicrosoftAsync(Pane root, boolean launchAfter, boolean interactive) {
        if (microsoftFlowRunning) {
            if (launchAfter) launchWhenConnected = true;
            return;
        }
        if (microsoftSessionActive && auth != null && auth.isLogged()) {
            if (launchAfter) startGame();
            return;
        }
        microsoftFlowRunning = true;
        setAccountStatus("Connexion Microsoft en cours...");

        auth = new GameAuth(AccountType.MICROSOFT);
        new Thread(() -> {
            boolean refreshed = auth.trySilentRefresh(engine);
            Platform.runLater(() -> {
                if (refreshed) {
                    microsoftFlowRunning = false;
                    onMicrosoftConnected(auth.getSession(), root, launchAfter);
                } else if (interactive) {
                    startMicrosoftLogin(root, launchAfter);
                } else {
                    microsoftFlowRunning = false;
                    setAccountStatus("Session Microsoft expirée : clique sur « Se connecter avec Microsoft ».");
                    // Le refresh a échoué : on rend la case au cas où l'utilisateur
                    // préfère finalement jouer avec un pseudo hors-ligne.
                    setRememberMeVisible(true);
                    setMicrosoftButtonConnected(false);
                    // JOUER reste bloqué (compte Microsoft non connecté), grisé mais
                    // sans le libellé « CONNEXION… » puisque plus aucune tentative en cours.
                    if (playButton != null) {
                        playButton.setDisable(true);
                        playButton.setText("JOUER");
                        playButton.setOpacity(0.55);
                    }
                }
            });
        }, "MajestyLauncher-MsSilentRefresh").start();
    }

    /**
     * Etat "connecté Microsoft" : avatar + pseudo affichés, JOUER prêt.
     * Ne lance le jeu que si launchAfter (auto-login).
     */
    private void onMicrosoftConnected(Session session, Pane root, boolean launchAfter) {
        microsoftSessionActive = true;
        config.updateValue("username", session.getUsername());
        config.updateValue("useMicrosoft", true);

        usernameField.setText(session.getUsername());
        usernameField.setDisable(true);
        microsoftInlineButton.setText("Compte : " + session.getUsername() + "  (changer)");
        setAccountStatus("● Connecté via Microsoft : " + session.getUsername(), true);
        // "Se souvenir de moi" ne concerne que le pseudo hors-ligne : la session
        // Microsoft est persistée par son refresh token, la case n'a aucun sens ici.
        setRememberMeVisible(false);
        setMicrosoftButtonConnected(true);
        setPlayReady(true);

        if (avatar != null) avatar.setVisible(false);
        connectAccountPremium(session.getUsername(), root);
        connectAccountPremiumCO(session.getUsername(), root);

        if (launchAfter || launchWhenConnected) {
            launchWhenConnected = false;
            startGame();
        }
    }

    /**
     * Repasse en mode hors-ligne.
     *
     * @param purgeTokens true (changement de compte) : supprime les jetons Microsoft
     *                    enregistrés, pour que la connexion suivante propose vraiment
     *                    un autre compte au lieu de reprendre celui-ci en silence.
     *                    false (simple déconnexion) : les jetons sont conservés, donc
     *                    une reconnexion se fera automatiquement sans ressaisie.
     */
    private void disconnectMicrosoft(Pane root, boolean purgeTokens) {
        if (purgeTokens) {
            GameAuth logoutAuth = auth != null ? auth : new GameAuth(AccountType.MICROSOFT);
            logoutAuth.logout(engine);
        }

        microsoftSessionActive = false;
        launchWhenConnected = false;
        auth = null;
        config.updateValue("useMicrosoft", false);

        usernameField.setDisable(false);
        usernameField.setText("");
        microsoftInlineButton.setText("Se connecter avec Microsoft");
        setAccountStatus(purgeTokens
                ? "Pseudo ou compte Microsoft, choisis ton profil et joue."
                : "Déconnecté. La reconnexion Microsoft sera automatique.");
        setRememberMeVisible(true);
        setMicrosoftButtonConnected(false);
        // Retour en mode hors-ligne : JOUER redevient utilisable (pseudo).
        setPlayReady(true);

        if (avatar != null) avatar.setVisible(false);
        connectAccountCrack(root);
    }

    /** Masque/affiche la case "Se souvenir de moi" (inutile en mode Microsoft). */
    private void setRememberMeVisible(boolean visible) {
        if (rememberMe != null) {
            rememberMe.setVisible(visible);
            rememberMe.setManaged(visible);
        }
    }

    /** Reflète l'état de connexion sur l'icône Microsoft de la sidebar. */
    private void setMicrosoftButtonConnected(boolean connected) {
        if (microsoftButton != null) {
            // Bordure et fond verts : classe ml-connected de css/design.css.
            microsoftButton.getStyleClass().remove("ml-connected");
            if (connected) {
                microsoftButton.getStyleClass().add("ml-connected");
            }
            microsoftButton.setTooltip(new Tooltip(connected ? "Compte Microsoft connecté" : "Connexion Microsoft"));
        }
        if (microsoftStateCaption != null) {
            microsoftStateCaption.setText(connected ? "Connecté" : "Compte");
            microsoftStateCaption.setStyle(connected
                    ? "-fx-background-color: transparent; -fx-text-fill: rgba(110,225,140,0.95);"
                    : "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.55);");
        }
    }

    /**
     * Active/désactive le bouton JOUER. Utilisé pour empêcher un lancement tant que
     * la connexion Microsoft n'est pas établie (compte Microsoft uniquement).
     */
    private void setPlayReady(boolean ready) {
        if (playButton != null) {
            playButton.setDisable(!ready);
            playButton.setText(ready ? "JOUER" : "CONNEXION…");
            playButton.setOpacity(ready ? 1.0 : 0.55);
        }
    }

    private void setAccountStatus(String text) {
        setAccountStatus(text, false);
    }

    /** @param connected true = statut affiché en vert (compte Microsoft actif) */
    private void setAccountStatus(String text, boolean connected) {
        if (connectionSubtitleLabel != null) {
            connectionSubtitleLabel.setText(text);
            connectionSubtitleLabel.setStyle(connected
                    ? "-fx-background-color: transparent; -fx-text-fill: rgba(120,230,140,0.95); -fx-font-weight: bold;"
                    : "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.62);");
        }
    }

    /**
     * Bouton JOUER : le seul déclencheur manuel du lancement.
     * Microsoft connecté → lance avec la session. Sinon pseudo hors-ligne.
     */
    private void onPlayClicked(Pane root) {
        if (launchInProgress) return;

        if (engine.getGameMaintenance() != null && engine.getGameMaintenance().isAccessBlocked()) {
            new LauncherAlert("Maintenance", "Le jeu est actuellement en maintenance, réessaie plus tard.");
            return;
        }

        if (microsoftSessionActive && auth != null && auth.isLogged()) {
            startGame();
            return;
        }

        if (!App.netIsAvailable()) {
            Platform.runLater(() -> new LauncherAlert(AUTH_FAILED, OFFLINE_MODE_ALERT));
            return;
        }

        String username = usernameField.getText();
        if (username == null || username.trim().length() <= 3) {
            new LauncherAlert(AUTH_FAILED, USERNAME_ALERT);
            return;
        }

        config.updateValue("useMicrosoft", false);
        config.updateValue("username", username.trim());
        authenticateOffline(username.trim());
        connectAccountCrackCO(root);

        if (auth.isLogged()) {
            startGame();
        } else {
            new LauncherAlert(AUTH_FAILED, ONLINE_MODE_ALERT);
        }
    }

    /** Point d'entrée unique du lancement (protégé contre les doubles clics). */
    private void startGame() {
        if (launchInProgress) return;
        launchInProgress = true;
        ProfileManager pm = ProfileManager.get();
        if (pm != null) pm.beginSession();
        updateDiscordForActiveProfile(true);
        update();
    }

    /**
     * Met à jour la présence Discord selon le profil actif.
     * @param playing true = en jeu ("Joue en Fabric 1.21"), false = dans le launcher
     */
    private void updateDiscordForActiveProfile(boolean playing) {
        if (rpc == null) return;
        ProfileManager pm = ProfileManager.get();
        LauncherProfile active = pm == null ? null : pm.getActive();
        if (active == null) return;
        if (playing) {
            rpc.updatePresence("En jeu sur MajestyCraft", "Joue en " + active.describeTarget());
        } else {
            rpc.updatePresence("Dans le launcher", "Profil : " + active.describeTarget());
        }
    }

    /* ─────────────────── Profils ─────────────────── */

    /** Liste des profils correspondant à l'ordre actuel du combo (pour les pastilles de couleur). */
    private java.util.List<LauncherProfile> comboProfiles = new java.util.ArrayList<>();

    /** Recharge le contenu du sélecteur de profils depuis le ProfileManager. */
    public void refreshProfilesUI() {
        ProfileManager pm = ProfileManager.get();
        if (pm == null || profileCombo == null) return;

        java.util.List<LauncherProfile> profiles = pm.getProfiles();
        this.comboProfiles = profiles;
        LauncherProfile active = pm.getActive();

        profileCombo.setOnAction(null);
        profileCombo.getItems().setAll(
                profiles.stream()
                        .map(p -> p.getName().equalsIgnoreCase(p.describeTarget())
                                ? p.getName()
                                : p.getName() + "  —  " + p.describeTarget())
                        .toArray(String[]::new)
        );

        // Cellules avec pastille de couleur du profil.
        profileCombo.setButtonCell(profileColorCell(false));
        profileCombo.setCellFactory(cb -> profileColorCell(true));

        if (active != null) {
            int idx = -1;
            for (int i = 0; i < profiles.size(); i++) {
                if (profiles.get(i).getId().equals(active.getId())) { idx = i; break; }
            }
            if (idx >= 0) profileCombo.getSelectionModel().select(idx);
        }
        profileCombo.setOnAction(event -> onProfileComboChanged());
    }

    /**
     * Cellule de combo affichant une pastille de couleur du profil devant son nom.
     * @param dropdown true = cellule de la liste déroulante (texte noir), false = bouton (texte clair)
     */
    private javafx.scene.control.ListCell<String> profileColorCell(boolean dropdown) {
        return new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item);
                setStyle(dropdown
                        ? "-fx-text-fill: rgba(255,255,255,0.92);"
                        : "-fx-text-fill: rgba(255,255,255,0.94);");
                String hex = null;
                int i = getIndex();
                if (comboProfiles != null && i >= 0 && i < comboProfiles.size()) {
                    hex = ProfileManager.get().colorOf(comboProfiles.get(i));
                }
                if (hex != null) {
                    javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(6, Color.web(hex));
                    setGraphic(dot);
                    setGraphicTextGap(9);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private void onProfileComboChanged() {
        ProfileManager pm = ProfileManager.get();
        if (pm == null) return;

        int idx = profileCombo.getSelectionModel().getSelectedIndex();
        java.util.List<LauncherProfile> profiles = pm.getProfiles();
        if (idx < 0 || idx >= profiles.size()) return;

        LauncherProfile selected = profiles.get(idx);
        LauncherProfile active = pm.getActive();
        if (active != null && active.getId().equals(selected.getId())) return;

        pm.activate(selected, engine);
        App.getInstance().applyGameLinksFromConfigAsync();
        updateDiscordForActiveProfile(false);
    }

    private void openProfilesPanel() {
        Pane profilesRoot = new LauncherPane(engine);
        Rectangle rect = new Rectangle(1000, 760);
        rect.setArcHeight(15.0);
        rect.setArcWidth(15.0);
        profilesRoot.setClip(rect);
        profilesRoot.setStyle("-fx-background-color: transparent;");
        new LauncherProfiles(profilesRoot, engine, this);

        Scene scene = new Scene(profilesRoot);
        scene.setFill(Color.TRANSPARENT);
        if (!scene.getStylesheets().contains("css/design.css")) {
            scene.getStylesheets().add("css/design.css");
        }

        Stage stage = new Stage();
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Profils de jeu");
        stage.setWidth(1000);
        stage.setHeight(760);
        stage.setScene(scene);
        stage.showAndWait();

        refreshProfilesUI();
        updateDiscordForActiveProfile(false);
    }

    private void showOfflineError() {
        Platform.runLater(() -> new LauncherAlert(ERROR_AUTH_FAILED, ERROR_OFFLINE_MODE));
    }

    private void setupBackGround(Pane root) {
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

        // Commandes de fenêtre : icônes au trait, fond au survol (rouge pour fermer).
        LauncherButton closeButton = new LauncherButton(root);
        UiStyle.apply(closeButton, "ml-window", "ml-window-close");
        closeButton.setGraphic(Icons.stroke(Icons.CLOSE, 15));
        closeButton.setPosition(engine.getWidth() - 46, 8);
        closeButton.setSize(36, 30);
        closeButton.setOnAction(event -> System.exit(0));
        closeButton.setTooltip(new Tooltip("Fermer"));

        LauncherButton reduceButton = new LauncherButton(root);
        UiStyle.apply(reduceButton, "ml-window");
        reduceButton.setGraphic(Icons.stroke(Icons.MINUS, 15));
        reduceButton.setPosition(engine.getWidth() - 86, 8);
        reduceButton.setSize(36, 30);
        reduceButton.setOnAction(event -> {
            Stage stage = (Stage) ((LauncherButton) event.getSource()).getScene().getWindow();
            stage.setIconified(true);
        });
        reduceButton.setTooltip(new Tooltip("Réduire"));
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

        if (useDiscord) {
            rpc.start();
            updateDiscordForActiveProfile(false);
        } else {
            rpc.stop();
        }

        mediaPlayer.setMute(!useMusic);

        if (useConnect) engine.reg(App.getGameConnect());

        if (useMicrosoft) {
            connectAccountPremium(username, root);
            connectAccountPremiumCO(username, root);
            // Mode Microsoft : la case "Se souvenir de moi" (pseudo hors-ligne) n'a
            // pas lieu d'être. On la masque tout de suite, avant même le refresh.
            setRememberMeVisible(false);
            // Reconnexion silencieuse au démarrage : le compte est prêt AVANT de
            // jouer. En cas d'échec, message discret (pas de popup intrusive).
            if (App.netIsAvailable()) {
                // JOUER bloqué tant que le compte Microsoft n'est pas reconnecté.
                // (l'auto-login lance le jeu par programme, indépendamment du bouton)
                setPlayReady(false);
                connectMicrosoftAsync(root, false, false);
            }
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
        // Barre latérale : 7 boutons au même pas, chacun avec sa légende, centrés
        // verticalement sous le logo.
        int sbX = 17;
        int sbY = 116;
        int step = 86;
        int size = 50;

        this.microsoftButton = new LauncherButton(root);
        UiStyle.apply(this.microsoftButton, "ml-side");
        this.microsoftButton.setGraphic(Icons.microsoftLogo(22));
        this.microsoftButton.setPosition(sbX, sbY);
        this.microsoftButton.setSize(size, size);
        this.microsoftButton.setOnAction(event -> launchMicrosoftFlow(root));
        this.microsoftButton.setTooltip(new Tooltip("Connexion Microsoft"));

        // Légende du bouton Compte : "Compte", puis "Connecté" en vert une fois
        // connecté (voir setMicrosoftButtonConnected). Animée à part dans update().
        this.microsoftStateCaption = addSidebarCaption(root, "Compte", sbX, sbY, size);
        sidebarCaptions.remove(this.microsoftStateCaption);

        this.infoButton = new LauncherButton(root);
        UiStyle.apply(this.infoButton, "ml-side");
        this.infoButton.setGraphic(Icons.stroke(Icons.INFO, 22));
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
        this.infoButton.setTooltip(new Tooltip("Informations"));
        addSidebarCaption(root, "Infos", sbX, sbY + step, size);

        this.settingsButton = new LauncherButton(root);
        UiStyle.apply(this.settingsButton, "ml-side");
        this.settingsButton.setGraphic(Icons.stroke(Icons.SETTINGS, 22));
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
        this.settingsButton.setTooltip(new Tooltip("Paramètres"));
        addSidebarCaption(root, "Réglages", sbX, sbY + step * 2, size);

        int profilesY = sbY + step * 3;
        int packsY = sbY + step * 4;
        int modsY = sbY + step * 5;
        int shadersY = sbY + step * 6;

        this.profilesButton = new LauncherButton(root);
        UiStyle.apply(this.profilesButton, "ml-side");
        this.profilesButton.setGraphic(Icons.stroke(Icons.LAYERS, 22));
        this.profilesButton.setPosition(sbX, profilesY);
        this.profilesButton.setSize(size, size);
        this.profilesButton.setOnAction(event -> openProfilesPanel());
        this.profilesButton.setTooltip(new Tooltip("Profils de jeu"));
        addSidebarCaption(root, "Profils", sbX, profilesY, size);

        this.packsButton = new LauncherButton(root);
        UiStyle.apply(this.packsButton, "ml-side");
        this.packsButton.setGraphic(Icons.stroke(Icons.IMAGE, 22));
        this.packsButton.setPosition(sbX, packsY);
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
        this.packsButton.setTooltip(new Tooltip("Packs de textures"));
        addSidebarCaption(root, "Packs", sbX, packsY, size);

        this.modsButton = new LauncherButton(root);
        UiStyle.apply(this.modsButton, "ml-side");
        this.modsButton.setGraphic(Icons.stroke(Icons.BOX, 22));
        this.modsButton.setPosition(sbX, modsY);
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
        this.modsButton.setTooltip(new Tooltip("Mods"));
        addSidebarCaption(root, "Mods", sbX, modsY, size);

        this.shadersButton = new LauncherButton(root);
        UiStyle.apply(this.shadersButton, "ml-side");
        this.shadersButton.setGraphic(Icons.stroke(Icons.SUN, 22));
        this.shadersButton.setPosition(sbX, shadersY);
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
        this.shadersButton.setTooltip(new Tooltip("Shaders"));
        addSidebarCaption(root, "Shaders", sbX, shadersY, size);

        int quickW = 155;
        int quickH = 40;
        int quickGap = 16;
        int quickY = 468;

        this.voteButton = new LauncherButton(root);
        this.voteButton.setText(BUTTON_SITE);
        this.voteButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        this.voteButton.setPosition(138, quickY);
        this.voteButton.setSize(quickW, quickH);
        UiStyle.apply(this.voteButton, "ml-secondary");
        this.voteButton.setGraphic(Icons.stroke(Icons.GLOBE, 17));
        this.voteButton.setGraphicTextGap(10);
        this.voteButton.setOnAction(event -> openLink(SITE_URL));

        this.boutiqueButton = new LauncherButton(root);
        this.boutiqueButton.setText(BUTTON_DISCORD);
        this.boutiqueButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        this.boutiqueButton.setPosition(138 + quickW + quickGap, quickY);
        this.boutiqueButton.setSize(quickW, quickH);
        UiStyle.apply(this.boutiqueButton, "ml-secondary");
        this.boutiqueButton.setGraphic(Icons.stroke(Icons.MESSAGE, 17));
        this.boutiqueButton.setGraphicTextGap(10);
        this.boutiqueButton.setOnAction(event -> openLink(DISCORD_URL));

        // Réseaux : pastilles rondes identiques, alignées sur le texte d'accueil.
        int socialSize = 46;
        int socialGap = 14;
        int socialStartX = 138;
        this.tiktokButton = createSocialButton(root, "tiktok.png", "TikTok", INSTAGRAM_URL,
                socialStartX, socialY, socialSize);
        this.minestratorButton = createSocialButton(root, "minestrator.png", "Minestrator", MINESTRATOR_URL,
                socialStartX + (socialSize + socialGap), socialY, socialSize);
        this.twitterButton = createSocialButton(root, "twitter_icon.png", "Twitter", TWITTER_URL,
                socialStartX + (socialSize + socialGap) * 2, socialY, socialSize);
        this.youtubeButton = createSocialButton(root, "yt_icon.png", "YouTube", YOUTUBE_URL,
                socialStartX + (socialSize + socialGap) * 3, socialY, socialSize);
    }

    /**
     * Bouton réseau social : pastille ronde, logo en gris discret au repos, couleurs
     * d'origine et léger agrandissement au survol.
     */
    private LauncherButton createSocialButton(Pane root, String imageName, String tooltip, String url,
                                              int x, int y, int size) {
        String idle = "-fx-background-color: rgba(255,255,255,0.06);"
                + "-fx-background-radius: 999;"
                + "-fx-border-color: rgba(255,255,255,0.12);"
                + "-fx-border-radius: 999;"
                + "-fx-border-width: 1;"
                + "-fx-padding: 0;"
                + "-fx-cursor: hand;";
        String hover = "-fx-background-color: rgba(255,255,255,0.12);"
                + "-fx-background-radius: 999;"
                + "-fx-border-color: rgba(255,170,60,0.55);"
                + "-fx-border-radius: 999;"
                + "-fx-border-width: 1;"
                + "-fx-padding: 0;"
                + "-fx-cursor: hand;";

        LauncherButton button = new LauncherButton(root);
        Image colored = getResourceLocation().loadImage(engine, imageName);
        Image muted = toMonochrome(colored);
        LauncherImage icon = new LauncherImage(root, muted);
        icon.setSize(24, 24);
        icon.setPreserveRatio(true);
        icon.setOpacity(0.85);

        button.setGraphic(icon);
        button.setPosition(x, y);
        button.setSize(size, size);
        button.setStyle(idle);
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(event -> openLink(url));
        button.setOnMouseEntered(e -> {
            button.setStyle(hover);
            icon.setImage(colored);
            icon.setOpacity(1);
            ScaleTransition st = new ScaleTransition(javafx.util.Duration.millis(120), button);
            st.setToX(1.08);
            st.setToY(1.08);
            st.play();
        });
        button.setOnMouseExited(e -> {
            button.setStyle(idle);
            icon.setImage(muted);
            icon.setOpacity(0.85);
            ScaleTransition st = new ScaleTransition(javafx.util.Duration.millis(120), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        return button;
    }

    /**
     * Version gris clair d'un logo, d'après la luminance perçue de chaque pixel
     * (ramenée entre 55 % et 100 % de blanc pour rester lisible sur fond sombre).
     * ColorAdjust ne convient pas : il désature en gardant la luminosité HSB, et un
     * rouge ou un bleu purs deviennent blancs (le triangle de YouTube disparaissait).
     */
    private static Image toMonochrome(Image source) {
        if (source == null || source.getPixelReader() == null) {
            return source;
        }
        int w = (int) source.getWidth();
        int h = (int) source.getHeight();
        javafx.scene.image.PixelReader reader = source.getPixelReader();
        javafx.scene.image.WritableImage out = new javafx.scene.image.WritableImage(w, h);
        javafx.scene.image.PixelWriter writer = out.getPixelWriter();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                Color c = reader.getColor(x, y);
                double luma = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
                double grey = 0.55 + 0.45 * luma;
                writer.setColor(x, y, new Color(grey, grey, grey, c.getOpacity()));
            }
        }
        return out;
    }

    /**
     * Petit texte centré sous une icône de la barre latérale (ex: "Mods", "Shaders").
     */
    private LauncherLabel addSidebarCaption(Pane root, String text, int iconX, int iconY, int iconSize) {
        int labelWidth = 80;
        LauncherLabel caption = new LauncherLabel(root);
        caption.setText(text);
        caption.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
        caption.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.55);");
        caption.setAlignment(Pos.CENTER);
        caption.setPosition(iconX - (labelWidth - iconSize) / 2, iconY + iconSize + 2);
        caption.setSize(labelWidth, 14);
        sidebarCaptions.add(caption);
        return caption;
    }

    private void setupConnectionsGUI(Pane root) {
        // Halo diffus derrière la carte : le dégradé radial s'éteint avant les bords du
        // rectangle, il n'y a donc aucun contour visible (l'ancien halo rectangulaire
        // dépassait de la carte et se lisait comme un second cadre).
        int glowMargin = 90;
        this.connectionGlow = new Rectangle(connX - glowMargin, connY - glowMargin,
                connW + glowMargin * 2, connH + glowMargin * 2);
        this.connectionGlow.setFill(new RadialGradient(
                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 138, 0, 0.16)),
                new Stop(0.55, Color.rgb(255, 120, 0, 0.07)),
                new Stop(1, Color.TRANSPARENT)
        ));
        this.connectionGlow.setMouseTransparent(true);
        root.getChildren().add(this.connectionGlow);

        this.connexionRectangle = new LauncherRectangle(root, connX, connY, connW, connH);
        applyModernCardStyle(this.connexionRectangle, 0.80);
        this.connexionRectangle.setMouseTransparent(true);

        this.avatarCard = new LauncherRectangle(root, connX + 28, connY + 134, 72, 72);
        this.avatarCard.setArcWidth(24);
        this.avatarCard.setArcHeight(24);
        this.avatarCard.setFill(Color.rgb(255, 255, 255, 0.06));
        this.avatarCard.setStroke(Color.rgb(255, 255, 255, 0.08));
        this.avatarCard.setStrokeWidth(1);
        this.avatarCard.setMouseTransparent(true);

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
        this.connectionSubtitleLabel.setText("Pseudo ou compte Microsoft, choisis ton profil et joue.");
        this.connectionSubtitleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.connectionSubtitleLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.62);");
        this.connectionSubtitleLabel.setAlignment(Pos.CENTER_LEFT);
        this.connectionSubtitleLabel.setWrapText(true);
        this.connectionSubtitleLabel.setPosition(connX + 28, connY + 98);
        this.connectionSubtitleLabel.setSize(connW - 56, 38);

        this.usernameField = new JFXTextField();
        this.usernameField.setLayoutX(connX + 118);
        this.usernameField.setLayoutY(connY + 146);
        this.usernameField.setPrefWidth(connW - 146);
        this.usernameField.setPrefHeight(48);
        this.usernameField.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        this.usernameField.setPromptText(INPUT_PSEUDO_OR_EMAIL);
        this.usernameField.getStyleClass().add("ml-input");

        if (!cfgBool(EnumConfig.USE_MICROSOFT)) {
            this.usernameField.setText((String) this.config.getValue(EnumConfig.USERNAME));
        }
        root.getChildren().add(this.usernameField);

        this.rememberMe = new JFXToggleButton();
        this.rememberMe.setText(LABEL_REMEMBER_ME);
        this.rememberMe.setSelected(cfgBool(EnumConfig.REMEMBER_ME));
        this.rememberMe.getStyleClass().add("jfx-toggle-button");
        this.rememberMe.setLayoutX(connX + 104);
        this.rememberMe.setLayoutY(connY + 196);
        this.rememberMe.setOnAction(event -> config.updateValue("rememberme", rememberMe.isSelected()));
        root.getChildren().add(this.rememberMe);

        /* ===== Sélecteur de profil ===== */
        this.profileCaptionLabel = new LauncherLabel(root);
        this.profileCaptionLabel.setText("PROFIL DE JEU");
        this.profileCaptionLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
        this.profileCaptionLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,207,138,0.85);");
        this.profileCaptionLabel.setPosition(connX + 30, connY + 244);
        this.profileCaptionLabel.setSize(200, 16);

        this.profileCombo = new JFXComboBox<>();
        this.profileCombo.getStyleClass().add("combo-modern");
        this.profileCombo.setLayoutX(connX + 28);
        this.profileCombo.setLayoutY(connY + 262);
        this.profileCombo.setPrefSize(connW - 106, 38);
        this.profileCombo.setVisibleRowCount(8);
        this.profileCombo.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: rgba(255,255,255,0.14);" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;" +
                "-fx-font-family: 'Comfortaa';"
        );
        this.profileCombo.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: rgba(255,255,255,0.94);");
            }
        });
        this.profileCombo.setCellFactory(cb -> new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
            }
        });
        this.profileCombo.setOnAction(event -> onProfileComboChanged());
        root.getChildren().add(this.profileCombo);

        this.manageProfilesButton = new JFXButton();
        this.manageProfilesButton.setLayoutX(connX + connW - 68);
        this.manageProfilesButton.setLayoutY(connY + 262);
        this.manageProfilesButton.setPrefSize(40, 38);
        UiStyle.apply(this.manageProfilesButton, "ml-secondary");
        this.manageProfilesButton.setGraphic(Icons.stroke(Icons.SETTINGS, 18));
        this.manageProfilesButton.setTooltip(new Tooltip("Gérer les profils"));
        this.manageProfilesButton.setOnAction(event -> openProfilesPanel());
        root.getChildren().add(this.manageProfilesButton);

        refreshProfilesUI();

        /* ===== JOUER ===== */
        this.playButton = new JFXButton("JOUER");
        this.playButton.setLayoutX(connX + 28);
        this.playButton.setLayoutY(connY + 314);
        this.playButton.setPrefWidth(connW - 56);
        this.playButton.setPrefHeight(52);
        UiStyle.apply(this.playButton, "ml-primary", "ml-cta");
        this.playButton.setGraphic(Icons.filled(Icons.PLAY, 16));
        this.playButton.setGraphicTextGap(12);
        this.playButton.setOnAction(event -> onPlayClicked(root));
        root.getChildren().add(this.playButton);

        /* ===== Microsoft (connexion uniquement, ne lance PAS le jeu) ===== */
        this.microsoftInlineButton = new JFXButton("Se connecter avec Microsoft");
        this.microsoftInlineButton.setLayoutX(connX + 28);
        this.microsoftInlineButton.setLayoutY(connY + 376);
        this.microsoftInlineButton.setPrefWidth(connW - 56);
        this.microsoftInlineButton.setPrefHeight(38);
        UiStyle.apply(this.microsoftInlineButton, "ml-secondary", "ml-strong");
        this.microsoftInlineButton.setGraphic(Icons.microsoftLogo(15));
        this.microsoftInlineButton.setGraphicTextGap(10);
        this.microsoftInlineButton.setContentDisplay(ContentDisplay.LEFT);
        this.microsoftInlineButton.setOnAction(event -> launchMicrosoftFlow(root));
        root.getChildren().add(this.microsoftInlineButton);

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
        if (profilesButton != null) new ZoomOutDown(profilesButton).setResetOnFinished(false).play();
        if (packsButton != null) new ZoomOutDown(packsButton).setResetOnFinished(false).play();
        if (modsButton != null) new ZoomOutDown(modsButton).setResetOnFinished(false).play();
        if (shadersButton != null) new ZoomOutDown(shadersButton).setResetOnFinished(false).play();

        // Légendes sous les icônes (Profils/Packs/Mods/Shaders) + état Microsoft :
        // elles doivent disparaître avec leurs icônes quand le jeu se lance.
        for (LauncherLabel caption : sidebarCaptions) {
            if (caption != null) new ZoomOutDown(caption).setResetOnFinished(false).play();
        }
        if (microsoftStateCaption != null) new ZoomOutDown(microsoftStateCaption).setResetOnFinished(false).play();

        if (connectionGlow != null) new ZoomOutDown(connectionGlow).setResetOnFinished(false).play();
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
        if (playButton != null) new ZoomOutDown(playButton).setResetOnFinished(false).play();
        if (microsoftInlineButton != null) new ZoomOutDown(microsoftInlineButton).setResetOnFinished(false).play();
        if (profileCombo != null) new ZoomOutDown(profileCombo).setResetOnFinished(false).play();
        if (manageProfilesButton != null) new ZoomOutDown(manageProfilesButton).setResetOnFinished(false).play();
        if (profileCaptionLabel != null) new ZoomOutDown(profileCaptionLabel).setResetOnFinished(false).play();
        if (connexionRectangle != null) new ZoomOutDown(connexionRectangle).setResetOnFinished(false).play();
        if (avatarCard != null) new ZoomOutDown(avatarCard).setResetOnFinished(false).play();

        if (voteButton != null) new ZoomOutDown(voteButton).setResetOnFinished(false).play();
        if (boutiqueButton != null) new ZoomOutDown(boutiqueButton).setResetOnFinished(false).play();

        if (tiktokButton != null) new ZoomOutDown(tiktokButton).setResetOnFinished(false).play();
        if (minestratorButton != null) new ZoomOutDown(minestratorButton).setResetOnFinished(false).play();
        if (twitterButton != null) new ZoomOutDown(twitterButton).setResetOnFinished(false).play();
        if (youtubeButton != null) new ZoomOutDown(youtubeButton).setResetOnFinished(false).play();

        if (avatar != null) new ZoomOutDown(avatar).setResetOnFinished(false).play();

        if (usernameField != null) usernameField.setDisable(true);
        if (connectionGlow != null) connectionGlow.setDisable(true);
        if (connexionRectangle != null) connexionRectangle.setDisable(true);
        if (avatarCard != null) avatarCard.setDisable(true);
        if (rememberMe != null) rememberMe.setDisable(true);
        if (playButton != null) playButton.setDisable(true);
        if (microsoftInlineButton != null) microsoftInlineButton.setDisable(true);
        if (profileCombo != null) profileCombo.setDisable(true);
        if (manageProfilesButton != null) manageProfilesButton.setDisable(true);
        if (settingsButton != null) settingsButton.setDisable(true);
        if (profilesButton != null) profilesButton.setDisable(true);
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

        if (this.updateTimeline != null) {
            this.updateTimeline.stop();
        }
        this.updateTimeline = new Timeline(
                new KeyFrame(javafx.util.Duration.seconds(0.0D), event -> timelineUpdate(engine)),
                new KeyFrame(javafx.util.Duration.seconds(0.1D))
        );
        this.updateTimeline.setCycleCount(Animation.INDEFINITE);
        this.updateTimeline.play();
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
        avatar = new LauncherImage(root, new Image("https://minotar.net/cube/MHF_Steve.png", true));
        avatar.setBounds(connX + 34, connY + 140, 60, 60);
    }

    public void connectAccountPremium(String username, Pane root) {
        avatar = new LauncherImage(root, new Image("https://minotar.net/cube/" + username + ".png", true));
        avatar.setBounds(connX + 34, connY + 140, 60, 60);
    }

    public void connectAccountPremiumOFF(Pane root) {
        avatar = new LauncherImage(root, new Image("https://minotar.net/cube/MHF_Steve.png", true));
        avatar.setBounds(connX + 34, connY + 140, 60, 60);
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

    /**
     * Connexion Microsoft par défaut : la page de connexion Microsoft s'ouvre dans le
     * navigateur du joueur, qui revient ensuite tout seul vers le launcher (aucun code
     * à recopier). Le code d'appareil reste disponible en secours.
     */
    private void startMicrosoftLogin(Pane root, boolean launchAfter) {
        final MicrosoftBrowserAuth browserAuth = new MicrosoftBrowserAuth().setLauncherName("MajestyLauncher");
        // true quand le joueur (ou un échec d'ouverture du navigateur) bascule sur le
        // code d'appareil : le flux navigateur s'arrête alors sans toucher à l'interface.
        final boolean[] switchedToCode = {false};
        final boolean[] cancelled = {false};
        final String[] authorizeUrl = {null};

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
                "Ton navigateur s'ouvre sur la page de connexion Microsoft.\n" +
                "Connecte-toi puis reviens ici : la suite est automatique."
        );
        info.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        info.setStyle("-fx-text-fill: rgba(255,255,255,0.68);");
        info.setWrapText(true);
        info.setAlignment(Pos.CENTER);
        info.setTextAlignment(TextAlignment.CENTER);

        Label waitLabel = new Label("En attente de ta connexion dans le navigateur...");
        waitLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        waitLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.60);");

        Button reopenBtn = new Button("Rouvrir la page");
        reopenBtn.setDisable(true);
        styleModalActionButton(reopenBtn, false);
        reopenBtn.setOnAction(e -> {
            if (authorizeUrl[0] != null) openLink(authorizeUrl[0]);
        });

        Button codeBtn = new Button("Utiliser un code");
        styleModalActionButton(codeBtn, false);

        HBox actions = new HBox(12, reopenBtn, codeBtn);
        actions.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: rgba(255,255,255,0.55);" +
                "-fx-font-family: 'Comfortaa';" +
                "-fx-font-size: 12px;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 6 18 6 18;"
        );

        VBox box = new VBox(16, badge, title, info, spinner, waitLabel, actions, cancelBtn);
        box.setPadding(new Insets(28));
        box.setAlignment(Pos.CENTER);

        Rectangle modalCard = new Rectangle(500, 390);
        modalCard.setArcWidth(36);
        modalCard.setArcHeight(36);
        modalCard.setFill(Color.rgb(8, 12, 18, 0.96));
        modalCard.setStroke(Color.rgb(255, 255, 255, 0.12));
        modalCard.setStrokeWidth(1.0);
        modalCard.setEffect(new DropShadow(45, Color.rgb(0, 0, 0, 0.48)));

        StackPane modalRoot = new StackPane(modalCard, box);
        modalRoot.setPadding(new Insets(18));
        // Voir startMicrosoftDeviceCodeLogin : evite le cadre blanc de la classe ".root".
        modalRoot.setStyle("-fx-background-color: transparent;");

        Scene authScene = new Scene(modalRoot, 540, 430);
        authScene.setFill(Color.TRANSPARENT);
        authStage.setScene(authScene);

        codeBtn.setOnAction(e -> {
            switchedToCode[0] = true;
            browserAuth.cancel();
            authStage.close();
            startMicrosoftDeviceCodeLogin(root, launchAfter);
        });
        cancelBtn.setOnAction(e -> {
            cancelled[0] = true;
            browserAuth.cancel();
        });

        authStage.show();

        new Thread(() -> {
            try {
                String url = browserAuth.start();
                authorizeUrl[0] = url;
                Platform.runLater(() -> reopenBtn.setDisable(false));
                try {
                    Desktop.getDesktop().browse(URI.create(url));
                } catch (Exception browserError) {
                    // Navigateur impossible à ouvrir : on bascule sur le code d'appareil,
                    // qui se termine depuis n'importe quel appareil.
                    System.err.println("Navigateur indisponible, bascule sur le code : " + browserError.getMessage());
                    switchedToCode[0] = true;
                    browserAuth.cancel();
                    Platform.runLater(() -> {
                        authStage.close();
                        startMicrosoftDeviceCodeLogin(root, launchAfter);
                    });
                    return;
                }

                MicrosoftModel model = browserAuth.awaitTokens(10, java.util.concurrent.TimeUnit.MINUTES);

                AuthConfig authConfig = new AuthConfig(engine);
                authConfig.createConfigFile(model);

                MicrosoftXboxAuth msAuth = new MicrosoftXboxAuth();
                Session session = msAuth.getLiveToken(model.getAccess_token());

                Platform.runLater(() -> {
                    authStage.close();
                    microsoftFlowRunning = false;
                    auth.setSession(session);
                    onMicrosoftConnected(session, root, launchAfter);
                });

            } catch (MicrosoftBrowserAuth.LoginCancelledException ex) {
                if (switchedToCode[0]) {
                    return; // le code d'appareil a pris le relais
                }
                Platform.runLater(() -> {
                    authStage.close();
                    microsoftFlowRunning = false;
                    launchWhenConnected = false;
                    setAccountStatus(cancelled[0]
                            ? "Connexion Microsoft annulée."
                            : "Connexion refusée sur la page Microsoft.");
                });
            } catch (java.util.concurrent.TimeoutException ex) {
                Platform.runLater(() -> {
                    authStage.close();
                    microsoftFlowRunning = false;
                    setAccountStatus("Connexion Microsoft expirée, réessaie.");
                });
            } catch (Exception ex) {
                if (switchedToCode[0]) {
                    return;
                }
                ex.printStackTrace();
                Platform.runLater(() -> {
                    authStage.close();
                    microsoftFlowRunning = false;
                    setAccountStatus("Connexion Microsoft impossible, réessaie.");
                    showAuthErrorAlert();
                });
            }
        }, "MajestyLauncher-MsBrowserLogin").start();
    }

    /**
     * Connexion de secours par code d'appareil : le joueur saisit un code sur
     * microsoft.com/link (utile si le navigateur ne peut pas revenir vers le launcher).
     */
    private void startMicrosoftDeviceCodeLogin(Pane root, boolean launchAfter) {
        final boolean[] cancelled = {false};
        final Thread[] worker = {null};

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

        // Sans ce bouton, le joueur restait bloqué jusqu'à l'expiration du code (15 min).
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: rgba(255,255,255,0.55);" +
                "-fx-font-family: 'Comfortaa';" +
                "-fx-font-size: 12px;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 6 18 6 18;"
        );
        cancelBtn.setOnAction(e -> {
            cancelled[0] = true;
            if (worker[0] != null) worker[0].interrupt(); // interrompt l'attente de pollForToken
            authStage.close();
            microsoftFlowRunning = false;
            launchWhenConnected = false;
            setAccountStatus("Connexion Microsoft annulée.");
        });

        VBox box = new VBox(15, title, info, codeLabel, copyBtn, spinner, waitLabel);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);

        box.getChildren().setAll(badge, title, info, codeLabel, actions, spinner, waitLabel, cancelBtn);
        box.setSpacing(16);
        box.setPadding(new Insets(28));

        Rectangle modalCard = new Rectangle(500, 470);
        modalCard.setArcWidth(36);
        modalCard.setArcHeight(36);
        modalCard.setFill(Color.rgb(8, 12, 18, 0.96));
        modalCard.setStroke(Color.rgb(255, 255, 255, 0.12));
        modalCard.setStrokeWidth(1.0);
        modalCard.setEffect(new DropShadow(45, Color.rgb(0, 0, 0, 0.48)));

        StackPane modalRoot = new StackPane(modalCard, box);
        modalRoot.setPadding(new Insets(18));
        // Le noeud racine d'une Scene recoit la classe CSS ".root", que la feuille
        // par defaut de JavaFX (modena) peint en gris clair : sans cette ligne, un
        // cadre blanc apparait autour de la carte arrondie malgre la scene transparente.
        modalRoot.setStyle("-fx-background-color: transparent;");

        Scene authScene = new Scene(modalRoot, 540, 510);
        authScene.setFill(Color.TRANSPARENT);

        authStage.setScene(authScene);
        authStage.show();

        worker[0] = new Thread(() -> {
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
                        copyBtn.setText("Code copié");
                    });

                    openBtn.setOnAction(e -> openLink(deviceCode.getVerificationUri()));
                });

                Desktop.getDesktop().browse(URI.create(deviceCode.getVerificationUri()));

                MicrosoftModel model = deviceAuth.pollForToken(deviceCode);
                if (cancelled[0]) return;

                AuthConfig authConfig = new AuthConfig(engine);
                authConfig.createConfigFile(model);

                MicrosoftXboxAuth msAuth = new MicrosoftXboxAuth();
                Session session = msAuth.getLiveToken(model.getAccess_token());

                Platform.runLater(() -> {
                    authStage.close();
                    microsoftFlowRunning = false;
                    auth.setSession(session);
                    onMicrosoftConnected(session, root, launchAfter);
                });

            } catch (Exception ex) {
                if (cancelled[0]) {
                    return; // annulé par le joueur : l'interface est déjà à jour
                }
                ex.printStackTrace();
                Platform.runLater(() -> {
                    authStage.close();
                    microsoftFlowRunning = false;
                    setAccountStatus("Connexion Microsoft impossible, réessaie.");
                    showAuthErrorAlert();
                });
            }
        }, "MajestyLauncher-MsDeviceCode");
        worker[0].start();
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
        addSidebarHint(root, profilesButton, "Profils", "Crée plusieurs profils de jeu : chacun sa version, son modloader et ses mods.", "profiles", 3);
        addSidebarHint(root, packsButton, "Ressources", "Ajoute et gère tes resource packs directement depuis le launcher.", "packs", 4);
        addSidebarHint(root, modsButton, "Mods", "Parcours un catalogue de mods, ajoute des .jar ou des .zip et gère-les facilement.", "mods", 5);
        addSidebarHint(root, shadersButton, "Shaders", "Installe tes shaderpacks, explore le catalogue et améliore le rendu du jeu.", "shaders", 6);
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
