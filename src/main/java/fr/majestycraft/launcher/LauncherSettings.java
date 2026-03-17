package fr.majestycraft.launcher;

import animatefx.animation.ZoomOutDown;
import com.jfoenix.controls.*;

import fr.majestycraft.*;
import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.utils.*;
import fr.trxyy.alternative.alternative_api.utils.config.*;
import fr.trxyy.alternative.alternative_api_ui.base.*;
import fr.trxyy.alternative.alternative_api_ui.components.*;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.*;

public class LauncherSettings extends IScreen {

    private final LauncherLabel memorySliderLabel;
    private JFXSlider memorySlider;

    private final JFXComboBox<String> windowsSizeList;
    private final JFXComboBox<String> versionList;
    private final JFXComboBox<String> LanguageList;

    private final JFXCheckBox autoLogin;
    private final JFXCheckBox connect;
    private static JFXCheckBox useForge;
    private static JFXCheckBox useOptifine;
    private final JFXCheckBox useDiscord;
    private final JFXCheckBox useMusic;
    private final JFXCheckBox useVMArguments;

    private final LauncherTextField vmArguments;

    private JFXCheckBox includeSnapshots;
    private boolean includeSnapshotsEnabled = true;

    private double xOffSet;
    private double yOffSet;

    private LauncherImage heroLogo;
    private LauncherLabel heroTitle;
    private LauncherLabel heroSubtitle;
    private LauncherLabel heroLine1;
    private LauncherLabel heroLine2;
    private LauncherLabel heroLine3;

    private static final String LABEL_SETTINGS = Main.bundle.getString("LABEL_SETTINGS");
    private static final String LABEL_WINDOW_SIZE = Main.bundle.getString("LABEL_WINDOW_SIZE");
    private static final String LABEL_RAM_ALLOC = Main.bundle.getString("LABEL_RAM_ALLOC");
    private static final String LABEL_CHOOSE_VERSION = Main.bundle.getString("LABEL_CHOOSE_VERSION");
    private static final String LABEL_USE_JVM_ARGUMENTS = Main.bundle.getString("LABEL_USE_JVM_ARGUMENTS");
    private static final String LABEL_DISCORD_STATUS = Main.bundle.getString("LABEL_DISCORD_STATUS");
    private static final String LABEL_AUTO_CONNECT = Main.bundle.getString("LABEL_AUTO_CONNECT");
    private static final String LABEL_CONNECT_SERVER = Main.bundle.getString("LABEL_CONNECT_SERVER");
    private static final String LABEL_PLAY_MUSIC = Main.bundle.getString("LABEL_PLAY_MUSIC");
    private static final String BUTTON_OPEN_GAME_DIR = Main.bundle.getString("BUTTON_OPEN_GAME_DIR");
    private static final String BUTTON_VALIDATE = Main.bundle.getString("BUTTON_VALIDATE");
    private static final String LANGUAGE = Main.bundle.getString("LANGUAGE");

    private static final String CFG_INCLUDE_SNAPSHOTS = EnumConfig.CFG_INCLUDE_SNAPSHOTS.getOption();
    private static final String LABEL_INCLUDE_SNAPSHOTS =
            Main.bundle.containsKey("LABEL_INCLUDE_SNAPSHOTS")
                    ? Main.bundle.getString("LABEL_INCLUDE_SNAPSHOTS")
                    : "Afficher les snapshots";

    private static final String MOJANG_MANIFEST_PRIMARY =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String MOJANG_MANIFEST_FALLBACK =
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    private static final int MAX_SNAPSHOTS = 50;
    private static final String FORGE_PROMOTIONS_URL =
            "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";

    private final Map<String, String> mojangVersionUrlById = new HashMap<>();
    private final Map<String, String> mojangVersionTypeById = new HashMap<>();
    private final Set<String> forgeVersionsAvailable = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> optifineVersionsAvailable = Collections.synchronizedSet(new HashSet<>());
    private volatile boolean optifineVersionsLoaded = false;

    @SuppressWarnings("unused")
    private final Gson gson = new Gson();

    private static final List<String> VANILLA_SUPPORTED_RELEASES = Arrays.asList(
            "1.8", "1.9", "1.10.2", "1.11.2", "1.12.2", "1.13.2", "1.14.4", "1.15.2",
            "1.16.2", "1.16.3", "1.16.4", "1.16.5", "1.17", "1.17.1", "1.18", "1.18.1",
            "1.18.2", "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4", "1.20", "1.20.1", "1.20.2",
            "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4",
            "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11"
    );

    private static final Set<String> FORGE_UNSUPPORTED_VERSIONS = new HashSet<>(Collections.singletonList(
            "1.8"
    ));

    private static final Set<String> FORGE_SUPPORTED_VERSIONS = new HashSet<>();
    static {
        FORGE_SUPPORTED_VERSIONS.addAll(VANILLA_SUPPORTED_RELEASES);
        FORGE_SUPPORTED_VERSIONS.removeAll(FORGE_UNSUPPORTED_VERSIONS);
    }

    private static final int W = 1000;
    private static final int H = 1000;

    public LauncherSettings(final Pane root, final GameEngine engine, final LauncherPanel pane) {

        Platform.runLater(() -> {
            if (root.getScene() != null && !root.getScene().getStylesheets().contains("css/design.css")) {
                root.getScene().getStylesheets().add("css/design.css");
            }
        });

        root.setOnMousePressed(event -> {
            xOffSet = event.getSceneX();
            yOffSet = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            if (root.getScene() == null) return;
            Stage st = (Stage) root.getScene().getWindow();
            st.setX(event.getScreenX() - xOffSet);
            st.setY(event.getScreenY() - yOffSet);
        });

        this.drawBackgroundImage(engine, root, "background.png");
        pane.getConfig().loadConfiguration();

        Rectangle overlay = new Rectangle(W, H);
        overlay.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 0, 0, 0.18)),
                new Stop(1, Color.rgb(0, 0, 0, 0.72))
        ));
        root.getChildren().add(overlay);

        LauncherRectangle leftDock = new LauncherRectangle(root, 0, 0, 84, H);
        leftDock.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(8, 12, 18, 0.84)),
                new Stop(1, Color.rgb(8, 12, 18, 0.28))
        ));

        LauncherRectangle separator = new LauncherRectangle(root, 83, 0, 1, H);
        separator.setFill(Color.rgb(255, 255, 255, 0.08));

        this.heroLogo = new LauncherImage(root);
        this.heroLogo.setImage(getResourceLocation().loadImage(engine, "launchergifpng.png"));
        this.heroLogo.setSize(150, 150);
        this.heroLogo.setBounds(128, 90, 150, 150);

        this.heroTitle = new LauncherLabel(root);
        this.heroTitle.setText("Réglages");
        this.heroTitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        this.heroTitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.96)");
        this.heroTitle.setPosition(118, 270);
        this.heroTitle.setSize(260, 40);

        this.heroSubtitle = new LauncherLabel(root);
        this.heroSubtitle.setText("Personnalise ton launcher");
        this.heroSubtitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.heroSubtitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.96)");
        this.heroSubtitle.setPosition(120, 314);
        this.heroSubtitle.setSize(260, 24);

        this.heroLine1 = new LauncherLabel(root);
        this.heroLine1.setText("• Versions vanilla, Forge et Optifine");
        this.heroLine1.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.heroLine1.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.70)");
        this.heroLine1.setPosition(120, 362);
        this.heroLine1.setSize(260, 20);

        this.heroLine2 = new LauncherLabel(root);
        this.heroLine2.setText("• RAM, fenêtre, langue...");
        this.heroLine2.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.heroLine2.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.58)");
        this.heroLine2.setPosition(120, 388);
        this.heroLine2.setSize(280, 20);

        this.heroLine3 = new LauncherLabel(root);
        this.heroLine3.setText("• Connexion auto et Discord");
        this.heroLine3.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.heroLine3.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.46)");
        this.heroLine3.setPosition(120, 414);
        this.heroLine3.setSize(270, 20);

        final int cardW = 565;
        final int cardH = 620;
        final int cardX = 360;
        final int cardY = 65;

        LauncherRectangle card = new LauncherRectangle(root, cardX, cardY, cardW, cardH);
        card.setArcWidth(34);
        card.setArcHeight(34);
        card.setFill(Color.rgb(8, 12, 18, 0.74));
        card.setStroke(Color.rgb(255, 255, 255, 0.10));
        card.setStrokeWidth(1);
        card.setEffect(new DropShadow(36, Color.rgb(0, 0, 0, 0.72)));
        card.setMouseTransparent(true);

        LauncherLabel titleLabel = new LauncherLabel(root);
        titleLabel.setText(LABEL_SETTINGS);
        titleLabel.setStyle("-fx-text-fill: white;");
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        titleLabel.setSize(cardW, 38);
        titleLabel.setPosition(cardX, cardY + 24);
        titleLabel.setAlignment(Pos.CENTER);

        LauncherLabel subTitleLabel = new LauncherLabel(root);
        subTitleLabel.setText("Configuration du jeu et du launcher");
        subTitleLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.45);");
        subTitleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        subTitleLabel.setSize(cardW, 20);
        subTitleLabel.setPosition(cardX, cardY + 58);
        subTitleLabel.setAlignment(Pos.CENTER);

        JFXButton closeButton = new JFXButton("✕");
        closeButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: rgba(255,255,255,0.85);" +
                "-fx-font-size: 18px;" +
                "-fx-cursor: hand;"
        );
        closeButton.setLayoutX(cardX + cardW - 50);
        closeButton.setLayoutY(cardY + 18);
        closeButton.setOnAction(event -> {
            final ZoomOutDown animation = new ZoomOutDown(root);
            animation.setOnFinished(actionEvent -> {
                Stage st = (Stage) closeButton.getScene().getWindow();
                st.close();
            });
            animation.setResetOnFinished(true);
            animation.play();
        });
        root.getChildren().add(closeButton);

        LauncherLabel sectionGame = new LauncherLabel(root);
        sectionGame.setText("JEU");
        sectionGame.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        sectionGame.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.95);");
        sectionGame.setPosition(cardX + 40, cardY + 96);
        sectionGame.setSize(120, 20);

        LauncherLabel sectionLauncher = new LauncherLabel(root);
        sectionLauncher.setText("LAUNCHER");
        sectionLauncher.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        sectionLauncher.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.95);");
        sectionLauncher.setPosition(cardX + 290, cardY + 96);
        sectionLauncher.setSize(120, 20);

        final int leftX = cardX + 40;
        final int rightX = cardX + 290;
        final int fieldW = 220;

        /* ===== Taille fenêtre ===== */
        LauncherLabel windowsSizeLabel = new LauncherLabel(root);
        windowsSizeLabel.setText(LABEL_WINDOW_SIZE);
        windowsSizeLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        windowsSizeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
        windowsSizeLabel.setSize(fieldW, 22);
        windowsSizeLabel.setPosition(leftX, cardY + 126);

        this.windowsSizeList = new JFXComboBox<>();
        populateSizeList();
        styleCombo(this.windowsSizeList);
        this.windowsSizeList.getStyleClass().add("combo-modern");
        this.windowsSizeList.setPromptText("Sélectionner...");
        this.windowsSizeList.setPrefSize(fieldW, 32);
        this.windowsSizeList.setLayoutX(leftX);
        this.windowsSizeList.setLayoutY(cardY + 152);
        this.windowsSizeList.setVisibleRowCount(6);

        this.windowsSizeList.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
            }
        });

        this.windowsSizeList.setCellFactory(cb -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: black;");
            }
        });

        String sizeDesc = null;
        Object cfgSize = pane.getConfig().getValue(EnumConfig.GAME_SIZE);
        if (cfgSize != null) {
            String s = String.valueOf(cfgSize).trim();
            if (s.contains("x")) {
                sizeDesc = s;
            } else {
                try {
                    sizeDesc = GameSize.getWindowSize(Integer.parseInt(s)).getDesc();
                } catch (Exception ignored) {}
            }
        }

        if (sizeDesc == null || !this.windowsSizeList.getItems().contains(sizeDesc)) {
            if (!this.windowsSizeList.getItems().isEmpty()) {
                sizeDesc = this.windowsSizeList.getItems().get(0);
            }
        }

        if (sizeDesc != null) {
            this.windowsSizeList.setValue(sizeDesc);
            this.windowsSizeList.getSelectionModel().select(sizeDesc);
        }

        root.getChildren().add(this.windowsSizeList);

        /* ===== Langue ===== */
        LauncherLabel languageLabel = new LauncherLabel(root);
        languageLabel.setText(LANGUAGE);
        languageLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        languageLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
        languageLabel.setSize(fieldW, 22);
        languageLabel.setPosition(rightX, cardY + 126);

        this.LanguageList = new JFXComboBox<>();
        languageList();
        styleCombo(this.LanguageList);
        this.LanguageList.getStyleClass().add("combo-modern");
        this.LanguageList.setPrefSize(fieldW, 32);
        this.LanguageList.setLayoutX(rightX);
        this.LanguageList.setLayoutY(cardY + 152);
        this.LanguageList.setVisibleRowCount(5);

        this.LanguageList.setButtonCell(new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
            }
        });

        this.LanguageList.setCellFactory(cb -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: black;");
            }
        });

        this.LanguageList.setValue((String) pane.getConfig().getValue(EnumConfig.LANGUAGE));
        this.LanguageList.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(Main.bundle.getString("alert.title"));
            alert.setHeaderText(Main.bundle.getString("alert.header"));
            alert.setContentText(Main.bundle.getString("alert.content"));
            alert.showAndWait();
        });
        root.getChildren().add(this.LanguageList);

        /* ===== Version ===== */
        LauncherLabel versionLabel = new LauncherLabel(root);
        versionLabel.setText(LABEL_CHOOSE_VERSION);
        versionLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        versionLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
        versionLabel.setSize(fieldW, 22);
        versionLabel.setPosition(leftX, cardY + 212);

        this.versionList = new JFXComboBox<>();
        styleCombo(this.versionList);
        this.versionList.getStyleClass().add("combo-modern");
        this.versionList.setPrefSize(fieldW, 32);
        this.versionList.setLayoutX(leftX);
        this.versionList.setLayoutY(cardY + 238);
        this.versionList.setVisibleRowCount(12);

        this.versionList.setButtonCell(new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) setText(null);
                else setText(item + (isSnapshot(item) ? " (snapshot)" : ""));
                setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
            }
        });

        this.versionList.setCellFactory(cb -> new ListCell<String>() {
            private final Label header = new Label();
            private final Label value = new Label();
            private final VBox box = new VBox(header, value);

            {
                header.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px; -fx-padding: 6 0 2 0;");
                value.setStyle("-fx-text-fill: black; -fx-font-size: 13px; -fx-padding: 0 0 6 0;");
                setGraphic(box);
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setGraphic(box);
                setStyle("-fx-text-fill: black;");
                value.setText(item + (isSnapshot(item) ? " (snapshot)" : ""));

                if ("Chargement...".equals(item)) {
                    header.setManaged(false);
                    header.setVisible(false);
                    return;
                }

                int idx = getIndex();
                List<String> items = getListView() != null ? getListView().getItems() : null;
                boolean showHeader;

                if (idx <= 0 || items == null || idx >= items.size()) {
                    showHeader = true;
                } else {
                    String prev = items.get(idx - 1);
                    boolean currSnap = isSnapshot(item);
                    boolean prevSnap = prev != null && isSnapshot(prev);
                    showHeader = (currSnap != prevSnap) || "Chargement...".equals(prev);
                }

                if (showHeader) {
                    header.setText(isSnapshot(item) ? "— Snapshots —" : "— Releases —");
                    header.setManaged(true);
                    header.setVisible(true);
                } else {
                    header.setManaged(false);
                    header.setVisible(false);
                }
            }
        });
        root.getChildren().add(this.versionList);

        /* ===== Snapshots ===== */
        Object cfgSnap = pane.getConfig().getValue(EnumConfig.CFG_INCLUDE_SNAPSHOTS);
        if (cfgSnap instanceof Boolean) includeSnapshotsEnabled = (Boolean) cfgSnap;
        else if (cfgSnap instanceof String) includeSnapshotsEnabled = Boolean.parseBoolean((String) cfgSnap);
        else includeSnapshotsEnabled = true;

        this.includeSnapshots = new JFXCheckBox(LABEL_INCLUDE_SNAPSHOTS);
        this.includeSnapshots.setSelected(includeSnapshotsEnabled);
        styleCheckBox(this.includeSnapshots);
        this.includeSnapshots.setLayoutX(rightX);
        this.includeSnapshots.setLayoutY(cardY + 236);
        this.includeSnapshots.setOnAction(e -> {
            includeSnapshotsEnabled = includeSnapshots.isSelected();
            pane.getConfig().updateValue(CFG_INCLUDE_SNAPSHOTS, includeSnapshotsEnabled);

            String preferred = this.versionList.getValue();
            populateVersionListFromMojang(pane, preferred);
        });
        root.getChildren().add(this.includeSnapshots);

        loadForgeAvailableVersionsAsync(pane);
        loadOptiFineAvailableVersionsAsync(pane);
        populateVersionListFromMojang(pane, (String) pane.getConfig().getValue(EnumConfig.VERSION));
        this.versionList.setOnAction(event -> applyModRestrictionsForVersion(versionList.getValue(), pane));

        /* ===== RAM ===== */
        LauncherLabel ramLabel = new LauncherLabel(root);
        ramLabel.setText(LABEL_RAM_ALLOC);
        ramLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        ramLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
        ramLabel.setSize(180, 22);
        ramLabel.setPosition(leftX, cardY + 296);

        this.memorySliderLabel = new LauncherLabel(root);
        this.memorySliderLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.memorySliderLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
        this.memorySliderLabel.setSize(80, 22);
        this.memorySliderLabel.setPosition(cardX + cardW - 100, cardY + 296);
        this.memorySliderLabel.setAlignment(Pos.CENTER_RIGHT);

        this.memorySlider = new JFXSlider();
        this.memorySlider.setMin(1);
        this.memorySlider.setMax(10);

        if (pane.getConfig().getValue(EnumConfig.RAM) != null) {
            double d = Double.parseDouble((String) pane.getConfig().getValue(EnumConfig.RAM));
            this.memorySlider.setValue(d);
        }

        this.memorySlider.setLayoutX(leftX);
        this.memorySlider.setLayoutY(cardY + 330);
        this.memorySlider.setPrefWidth(cardW - 80);
        this.memorySlider.setBlockIncrement(1);
        this.memorySlider.setStyle(
                "-jfx-default-thumb: #ff9800;" +
                "-jfx-default-track: rgba(255,255,255,0.18);" +
                "-fx-pref-height: 10px;"
        );

        this.memorySlider.valueProperty().addListener((ov, oldVal, newVal) ->
                memorySlider.setValue(Math.round(newVal.doubleValue()))
        );
        this.memorySlider.valueProperty().addListener((obs, o, n) ->
                memorySliderLabel.setText(n.intValue() + " GB")
        );
        root.getChildren().add(this.memorySlider);
        this.memorySliderLabel.setText((int) this.memorySlider.getValue() + " GB");

        /* ===== Toggles ===== */
        useForge = new JFXCheckBox("Forge");
        Object cfgForge = pane.getConfig().getValue(EnumConfig.USE_FORGE);
        useForge.setSelected(cfgForge instanceof Boolean ? (Boolean) cfgForge : false);
        styleCheckBox(useForge);
        useForge.setLayoutX(leftX);
        useForge.setLayoutY(cardY + 382);
        useForge.setOnAction(e -> {
            if (useOptifine != null) useOptifine.setSelected(false);
            pane.getConfig().updateValue("useForge", useForge.isSelected());
        });
        root.getChildren().add(useForge);

        useOptifine = new JFXCheckBox("Optifine");
        Object cfgOpti = pane.getConfig().getValue(EnumConfig.USE_OPTIFINE);
        useOptifine.setSelected(cfgOpti instanceof Boolean ? (Boolean) cfgOpti : false);
        styleCheckBox(useOptifine);
        useOptifine.setLayoutX(leftX);
        useOptifine.setLayoutY(cardY + 414);
        useOptifine.setOnAction(e -> {
            if (useForge != null) useForge.setSelected(false);
            pane.getConfig().updateValue("useOptifine", useOptifine.isSelected());
        });
        root.getChildren().add(useOptifine);

        this.useMusic = new JFXCheckBox(LABEL_PLAY_MUSIC);
        Object cfgMusic = pane.getConfig().getValue(EnumConfig.USE_MUSIC);
        this.useMusic.setSelected(cfgMusic instanceof Boolean ? (Boolean) cfgMusic : false);
        styleCheckBox(this.useMusic);
        this.useMusic.setLayoutX(leftX);
        this.useMusic.setLayoutY(cardY + 446);
        this.useMusic.setOnAction(e -> {
            pane.getConfig().updateValue("usemusic", useMusic.isSelected());
            pane.getMediaPlayer().setMute(!useMusic.isSelected());
        });
        root.getChildren().add(this.useMusic);

        this.autoLogin = new JFXCheckBox(LABEL_AUTO_CONNECT);
        Object cfgAutoLogin = pane.getConfig().getValue(EnumConfig.AUTOLOGIN);
        this.autoLogin.setSelected(cfgAutoLogin instanceof Boolean ? (Boolean) cfgAutoLogin : false);
        styleCheckBox(this.autoLogin);
        this.autoLogin.setLayoutX(rightX);
        this.autoLogin.setLayoutY(cardY + 382);
        root.getChildren().add(this.autoLogin);

        this.useDiscord = new JFXCheckBox(LABEL_DISCORD_STATUS);
        Object cfgDiscord = pane.getConfig().getValue(EnumConfig.USE_DISCORD);
        this.useDiscord.setSelected(cfgDiscord instanceof Boolean ? (Boolean) cfgDiscord : false);
        styleCheckBox(this.useDiscord);
        this.useDiscord.setLayoutX(rightX);
        this.useDiscord.setLayoutY(cardY + 414);
        this.useDiscord.setOnAction(e -> {
            if (useDiscord.isSelected()) pane.getRpc().start();
            else pane.getRpc().stop();
        });
        root.getChildren().add(this.useDiscord);

        this.useVMArguments = new JFXCheckBox(LABEL_USE_JVM_ARGUMENTS);
        Object cfgUseVmArgs = pane.getConfig().getValue(EnumConfig.USE_VM_ARGUMENTS);
        this.useVMArguments.setSelected(cfgUseVmArgs instanceof Boolean ? (Boolean) cfgUseVmArgs : false);
        styleCheckBox(this.useVMArguments);
        this.useVMArguments.setLayoutX(rightX);
        this.useVMArguments.setLayoutY(cardY + 446);
        root.getChildren().add(this.useVMArguments);

        this.vmArguments = new LauncherTextField(root);
        this.vmArguments.setText((String) pane.getConfig().getValue(EnumConfig.VM_ARGUMENTS));
        this.vmArguments.setSize(cardW - 80, 30);
        this.vmArguments.setPosition(leftX, cardY + 496);
        this.vmArguments.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-text-fill: rgba(255,255,255,0.92);" +
                "-fx-prompt-text-fill: rgba(255,255,255,0.42);" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: rgba(255,255,255,0.14);" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;"
        );
        this.useVMArguments.setOnAction(e -> vmArguments.setDisable(!useVMArguments.isSelected()));
        this.vmArguments.setDisable(!this.useVMArguments.isSelected());

        this.connect = new JFXCheckBox(LABEL_CONNECT_SERVER);
        Object cfgConnect = pane.getConfig().getValue(EnumConfig.USE_CONNECT);
        this.connect.setSelected(cfgConnect instanceof Boolean ? (Boolean) cfgConnect : false);
        styleCheckBox(this.connect);
        this.connect.setLayoutX(leftX);
        this.connect.setLayoutY(cardY + 540);
        this.connect.setOnAction(e -> {
            pane.getConfig().updateValue("useConnect", connect.isSelected());
            if (connect.isSelected()) engine.reg(App.getGameConnect());
        });
        root.getChildren().add(this.connect);

        /* ===== Boutons ===== */
        JFXButton openGameDirButton = new JFXButton(BUTTON_OPEN_GAME_DIR);
        styleSecondaryButton(openGameDirButton);
        openGameDirButton.setLayoutX(leftX);
        openGameDirButton.setLayoutY(cardY + cardH - 58);
        openGameDirButton.setPrefWidth(210);
        openGameDirButton.setOnAction(e -> openGameDirectory());
        root.getChildren().add(openGameDirButton);

        JFXButton saveButton = new JFXButton(BUTTON_VALIDATE);
        stylePrimaryButton(saveButton);
        saveButton.setLayoutX(cardX + cardW - 40 - 210);
        saveButton.setLayoutY(cardY + cardH - 58);
        saveButton.setPrefWidth(210);
        saveButton.setOnAction(event -> {
            HashMap<String, String> configMap = new HashMap<>();
            configMap.put("allocatedram", String.valueOf(memorySlider.getValue()));
            configMap.put("gamesize", "" + GameSize.getWindowSize(windowsSizeList.getValue()));
            configMap.put("autologin", "" + autoLogin.isSelected());
            configMap.put("usevmarguments", "" + useVMArguments.isSelected());
            configMap.put("vmarguments", "" + vmArguments.getText());
            configMap.put("version", "" + versionList.getValue());
            configMap.put("language", "" + LanguageList.getValue());
            configMap.put("useforge", "" + useForge.isSelected());
            configMap.put("useOptifine", "" + useOptifine.isSelected());
            configMap.put("usemusic", "" + useMusic.isSelected());
            configMap.put("usediscord", "" + useDiscord.isSelected());
            configMap.put(EnumConfig.USE_CONNECT.getOption(), "" + connect.isSelected());
            configMap.put(CFG_INCLUDE_SNAPSHOTS, "" + includeSnapshots.isSelected());

            pane.getConfig().updateValues(configMap);

            engine.reg(GameMemory.getMemory(Double.parseDouble((String) pane.getConfig().getValue(EnumConfig.RAM))));
            engine.reg(GameSize.getWindowSize(Integer.parseInt((String) pane.getConfig().getValue(EnumConfig.GAME_SIZE))));

            String selectedVersion = String.valueOf(pane.getConfig().getValue(EnumConfig.VERSION));
            try {
                if (useOptifine.isSelected()) {
                    App.ensureOptiFineRuntime(selectedVersion);
                }

                GameLinks links = buildGameLinks(selectedVersion, useForge.isSelected(), useOptifine.isSelected());
                engine.reg(links);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Impossible de préparer la version sélectionnée");
                alert.setContentText(e.getMessage() != null ? e.getMessage() : "Erreur inconnue lors de la préparation du jeu.");
                alert.showAndWait();
                return;
            }

            Utils.regGameStyle(engine, pane.getConfig());

            final ZoomOutDown animation = new ZoomOutDown(root);
            animation.setOnFinished(actionEvent -> {
                Stage st = (Stage) ((JFXButton) event.getSource()).getScene().getWindow();
                st.close();
            });
            animation.setResetOnFinished(true);
            animation.play();
        });
        root.getChildren().add(saveButton);

        Platform.runLater(() -> {
            animateIn(heroLogo, -20, 16, 60);
            animateIn(heroTitle, -16, 12, 120);
            animateIn(heroSubtitle, -16, 12, 170);
            animateIn(heroLine1, -16, 12, 220);
            animateIn(heroLine2, -16, 12, 270);
            animateIn(heroLine3, -16, 12, 320);

            animateIn(card, 28, 0, 100);
            animateIn(titleLabel, 20, 0, 160);
            animateIn(subTitleLabel, 20, 0, 210);

            animateIn(windowsSizeLabel, 18, 0, 240);
            animateIn(this.windowsSizeList, 18, 0, 270);
            animateIn(languageLabel, 18, 0, 300);
            animateIn(this.LanguageList, 18, 0, 330);

            animateIn(versionLabel, 18, 0, 360);
            animateIn(this.versionList, 18, 0, 390);
            animateIn(this.includeSnapshots, 18, 0, 420);

            animateIn(ramLabel, 18, 0, 450);
            animateIn(this.memorySlider, 18, 0, 480);
            animateIn(this.memorySliderLabel, 18, 0, 500);

            animateIn(useForge, 12, 0, 520);
            animateIn(useOptifine, 12, 0, 545);
            animateIn(useMusic, 12, 0, 570);
            animateIn(autoLogin, 12, 0, 595);
            animateIn(useDiscord, 12, 0, 620);
            animateIn(useVMArguments, 12, 0, 645);
            animateIn(vmArguments, 12, 0, 670);
            animateIn(connect, 12, 0, 695);
            animateIn(openGameDirButton, 0, 14, 720);
            animateIn(saveButton, 0, 14, 750);
        });
    }

    private void animateIn(Node node, double fromX, double fromY, int delayMs) {
        if (node == null) return;

        node.setOpacity(0);
        node.setTranslateX(fromX);
        node.setTranslateY(fromY);

        FadeTransition ft = new FadeTransition(Duration.millis(420), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(420), node);
        tt.setFromX(fromX);
        tt.setToX(0);
        tt.setFromY(fromY);
        tt.setToY(0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    private void styleCombo(JFXComboBox<String> cb) {
        cb.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: rgba(255,255,255,0.14);" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;" +
                "-fx-text-fill: white;"
        );
    }

    private void styleCheckBox(JFXCheckBox box) {
        box.setOpacity(1.0);
        box.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        box.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -jfx-checked-color: #ff9800; -jfx-unchecked-color: rgba(255,255,255,0.42)");
    }

    private void stylePrimaryButton(JFXButton btn) {
        btn.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        btn.setStyle(
                "-fx-background-radius: 18;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: linear-gradient(to right, #ff9800, #ff6d00);" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 18, 0.2, 0, 6);"
        );
    }

    private void styleSecondaryButton(JFXButton btn) {
        btn.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        btn.setStyle(
                "-fx-background-radius: 18;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: rgba(255,255,255,0.10);" +
                "-fx-border-color: rgba(255,255,255,0.16);" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1;"
        );
    }

    private GameLinks buildGameLinks(String version, boolean forge, boolean optifine) throws IOException {
        String fullUrl = mojangVersionUrlById.get(version);
        if ((fullUrl == null || fullUrl.trim().isEmpty()) && App.netIsAvailable()) {
            fullUrl = App.resolveMojangVersionJsonUrlStatic(version);
        }

        if (optifine) {
            if (fullUrl == null || fullUrl.trim().isEmpty()) {
                throw new IOException("Impossible de résoudre le JSON Mojang pour OptiFine " + version);
            }
            return App.buildOptiFineGameLinksFromResolvedJson(fullUrl, version);
        }

        if (fullUrl == null || fullUrl.trim().isEmpty()) {
            return new GameLinks("https://majestycraft.com/minecraft" + urlModifier(version, forge), version + ".json");
        }

        String serverBase = "https://majestycraft.com/minecraft/" + version + (forge ? "/forge/" : "/");
        return new GameLinks(
                fullUrl,
                serverBase + "ignore.cfg",
                serverBase + "delete.cfg",
                serverBase + "status.cfg",
                serverBase + "files/"
        );
    }

    private String urlModifier() {
        return urlModifier(versionList.getValue(), useForge.isSelected());
    }

    private String urlModifier(String version, boolean forge) {
        return "/" + version + (forge ? "/forge/" : "/");
    }

    private void populateVersionListFromMojang(final LauncherPanel pane, final String preferredSelection) {
        this.versionList.getItems().clear();
        this.versionList.getItems().add("Chargement...");
        this.versionList.setValue("Chargement...");
        this.versionList.setDisable(true);

        final boolean includeSnapshotsNow = this.includeSnapshotsEnabled;

        Thread loader = new Thread(() -> {
            List<String> versions;
            try {
                versions = fetchMojangVersions(MOJANG_MANIFEST_PRIMARY, includeSnapshotsNow, MAX_SNAPSHOTS);
            } catch (Exception primaryFail) {
                try {
                    versions = fetchMojangVersions(MOJANG_MANIFEST_FALLBACK, includeSnapshotsNow, MAX_SNAPSHOTS);
                } catch (Exception fallbackFail) {
                    versions = new ArrayList<>(VANILLA_SUPPORTED_RELEASES);
                    mojangVersionUrlById.clear();
                    mojangVersionTypeById.clear();
                    for (String v : versions) mojangVersionTypeById.put(v, "release");
                }
            }

            final List<String> finalVersions = versions;

            Platform.runLater(() -> {
                this.versionList.getItems().setAll(finalVersions);
                this.versionList.setDisable(false);

                if (preferredSelection != null && finalVersions.contains(preferredSelection)) {
                    this.versionList.setValue(preferredSelection);
                } else {
                    String saved = (String) pane.getConfig().getValue(EnumConfig.VERSION);
                    if (saved != null && finalVersions.contains(saved)) this.versionList.setValue(saved);
                    else if (!finalVersions.isEmpty()) this.versionList.setValue(finalVersions.get(0));
                }

                applyModRestrictionsForVersion(this.versionList.getValue(), pane);
            });
        }, "MajestyLauncher-MojangVersions");

        loader.setDaemon(true);
        loader.start();
    }

    private List<String> fetchMojangVersions(String manifestUrl, boolean includeSnapshots, int maxSnapshots) throws IOException {
        String json = downloadText(manifestUrl);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray versions = root.getAsJsonArray("versions");

        mojangVersionUrlById.clear();
        mojangVersionTypeById.clear();

        List<String> out = new ArrayList<>();
        int snapshotCount = 0;

        for (JsonElement el : versions) {
            JsonObject o = el.getAsJsonObject();

            String id = o.get("id").getAsString();
            String type = o.get("type").getAsString();
            String url  = o.get("url").getAsString();

            if ("snapshot".equalsIgnoreCase(type)) {
                if (!includeSnapshots) continue;
                if (snapshotCount >= maxSnapshots) continue;

                snapshotCount++;
                out.add(id);
                mojangVersionUrlById.put(id, url);
                mojangVersionTypeById.put(id, "snapshot");
                continue;
            }

            out.add(id);
            mojangVersionUrlById.put(id, url);
            mojangVersionTypeById.put(id, type.toLowerCase(Locale.ROOT));
        }

        return out;
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
                if (code < 200 || code >= 300) throw new IOException("HTTP " + code + " -> " + sb);
                return sb.toString();
            }
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
    }

    private boolean isSnapshot(String versionId) {
        String t = mojangVersionTypeById.get(versionId);
        if (t != null) return "snapshot".equalsIgnoreCase(t);
        return versionId != null && versionId.matches("\\d{2}w\\d{2}[a-z]");
    }

    private void applyModRestrictionsForVersion(String version, LauncherPanel pane) {
        if (version == null || version.trim().isEmpty() || "Chargement...".equals(version)) return;

        boolean snapshot = isSnapshot(version);

        boolean forgeAllowed = !snapshot && isForgeAvailableForVersion(version);
        boolean optifineAllowed = !snapshot && isOptiFineAvailableForVersion(version);

        boolean forgeRestricted = !forgeAllowed;
        boolean optifineRestricted = !optifineAllowed;

        if (forgeRestricted) {
            useForge.setSelected(false);
            pane.getConfig().updateValue("useForge", false);
        }
        useForge.setDisable(forgeRestricted);
        useForge.setOpacity(forgeRestricted ? 0.35 : 1.0);

        if (optifineRestricted) {
            useOptifine.setSelected(false);
            pane.getConfig().updateValue("useOptifine", false);
        }
        useOptifine.setDisable(optifineRestricted);
        useOptifine.setOpacity(optifineRestricted ? 0.35 : 1.0);
    }



    private void loadForgeAvailableVersionsAsync(final LauncherPanel pane) {
        Thread loader = new Thread(() -> {
            Set<String> loaded = new HashSet<>();
            try {
                String json = downloadText(FORGE_PROMOTIONS_URL);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonObject promos = root.getAsJsonObject("promos");
                if (promos != null) {
                    for (Map.Entry<String, JsonElement> entry : promos.entrySet()) {
                        String key = entry.getKey();
                        if (key.endsWith("-latest")) {
                            loaded.add(key.substring(0, key.length() - 7));
                        } else if (key.endsWith("-recommended")) {
                            loaded.add(key.substring(0, key.length() - 12));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            synchronized (forgeVersionsAvailable) {
                forgeVersionsAvailable.clear();
                if (!loaded.isEmpty()) {
                    forgeVersionsAvailable.addAll(loaded);
                } else {
                    forgeVersionsAvailable.addAll(FORGE_SUPPORTED_VERSIONS);
                }
            }

            Platform.runLater(() -> applyModRestrictionsForVersion(versionList.getValue(), pane));
        }, "MajestyLauncher-ForgeVersions");

        loader.setDaemon(true);
        loader.start();
    }

    private boolean isForgeAvailableForVersion(String version) {
        if (version == null || version.trim().isEmpty()) return false;
        if (FORGE_UNSUPPORTED_VERSIONS.contains(version)) return false;

        synchronized (forgeVersionsAvailable) {
            if (!forgeVersionsAvailable.isEmpty()) {
                return forgeVersionsAvailable.contains(version);
            }
        }

        return FORGE_SUPPORTED_VERSIONS.contains(version);
    }

    private void loadOptiFineAvailableVersionsAsync(final LauncherPanel pane) {
        Thread loader = new Thread(() -> {
            Set<String> loaded = new HashSet<>();
            try {
                loaded.addAll(App.fetchAvailableOptiFineVersions());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!loaded.isEmpty()) {
                synchronized (optifineVersionsAvailable) {
                    optifineVersionsAvailable.clear();
                    optifineVersionsAvailable.addAll(loaded);
                    optifineVersionsLoaded = true;
                }
            }

            Platform.runLater(() -> applyModRestrictionsForVersion(versionList.getValue(), pane));
        }, "MajestyLauncher-OptiFineVersions");

        loader.setDaemon(true);
        loader.start();
    }

    private boolean isOptiFineAvailableForVersion(String version) {
        if (version == null || version.trim().isEmpty()) return false;

        synchronized (optifineVersionsAvailable) {
            if (optifineVersionsLoaded) {
                return optifineVersionsAvailable.contains(version);
            }
        }

        return true;
    }

    private void populateSizeList() {
        for (GameSize size : GameSize.values()) {
            this.windowsSizeList.getItems().add(size.getDesc());
        }
    }

    private void languageList() {
        String[] language = new String[]{"Français", "English", "Español"};
        this.LanguageList.getItems().addAll(Arrays.asList(language));
    }

    private void openGameDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        Path gameDirectory;

        if (os.contains("win")) {
            gameDirectory = Paths.get(System.getenv("APPDATA"), ".majestycraft", "bin", "game");
        } else if (os.contains("mac")) {
            gameDirectory = Paths.get(System.getProperty("user.home"), "Library", "Application Support", ".majestycraft", "bin", "game");
        } else {
            gameDirectory = Paths.get(System.getProperty("user.home"), ".majestycraft", "bin", "game");
        }

        try {
            Desktop.getDesktop().open(gameDirectory.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}