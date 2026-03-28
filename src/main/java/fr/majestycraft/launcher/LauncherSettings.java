package fr.majestycraft.launcher;

import com.jfoenix.controls.*;

import fr.majestycraft.*;
import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.utils.*;
import fr.trxyy.alternative.alternative_api.utils.config.*;
import fr.trxyy.alternative.alternative_api_ui.base.*;
import fr.trxyy.alternative.alternative_api_ui.components.*;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
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
    private static JFXCheckBox useVanilla;
    private static JFXCheckBox useForge;
    private static JFXCheckBox useFabric;
    private static JFXCheckBox useQuilt;
    private static JFXCheckBox useNeoForge;
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
    private LauncherLabel modloaderLabel;
    private boolean updatingModloaderSelection;

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
    private static final String FABRIC_GAME_VERSIONS_URL =
            "https://meta.fabricmc.net/v2/versions/game";
    private static final String QUILT_GAME_VERSIONS_URL =
            "https://meta.quiltmc.org/v3/versions/game";
    private static final String NEOFORGE_MAVEN_METADATA_URL =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
    private static final String SERVER_GAME_BASE_URL = "https://majestycraft.com/minecraft";

    private final Map<String, String> mojangVersionUrlById = new HashMap<>();
    private final Map<String, String> mojangVersionTypeById = new HashMap<>();
    private final Set<String> forgeVersionsAvailable = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> fabricVersionsAvailable = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> quiltVersionsAvailable = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> neoForgeVersionsAvailable = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> optifineVersionsAvailable = Collections.synchronizedSet(new HashSet<>());
    private volatile boolean fabricVersionsLoaded = false;
    private volatile boolean quiltVersionsLoaded = false;
    private volatile boolean neoForgeVersionsLoaded = false;
    private volatile boolean optifineVersionsLoaded = false;

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
        this.heroLine1.setText("• Vanilla, Forge, Fabric, Quilt, NeoForge, OptiFine");
        this.heroLine1.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.heroLine1.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.70)");
        this.heroLine1.setPosition(120, 362);
        this.heroLine1.setSize(260, 20);

        this.heroLine2 = new LauncherLabel(root);
        this.heroLine2.setText("• RAM, fenêtre, langue...");
        this.heroLine2.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.heroLine2.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.58)");
        this.heroLine2.setPosition(120, 388);
        this.heroLine2.setSize(280, 20);

        this.heroLine3 = new LauncherLabel(root);
        this.heroLine3.setText("• Connexion auto et Discord");
        this.heroLine3.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.heroLine3.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.46)");
        this.heroLine3.setPosition(120, 414);
        this.heroLine3.setSize(270, 20);

        final int cardW = 565;
        final int cardH = 660;
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
        closeButton.setOnAction(event -> closeWithAnimation(root, (Stage) closeButton.getScene().getWindow()));
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
        loadFabricAvailableVersionsAsync(pane);
        loadQuiltAvailableVersionsAsync(pane);
        loadNeoForgeAvailableVersionsAsync(pane);
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
        this.modloaderLabel = new LauncherLabel(root);
        this.modloaderLabel.setText("Modloader");
        this.modloaderLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.modloaderLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
        this.modloaderLabel.setSize(fieldW, 22);
        this.modloaderLabel.setPosition(leftX, cardY + 356);

        final int modloaderCol2X = leftX + 118;
        useVanilla = createModloaderCheckBox("Vanilla", leftX, cardY + 382, Utils.ModloaderType.VANILLA);
        useForge = createModloaderCheckBox("Forge", leftX, cardY + 414, Utils.ModloaderType.FORGE);
        useFabric = createModloaderCheckBox("Fabric", leftX, cardY + 446, Utils.ModloaderType.FABRIC);
        useQuilt = createModloaderCheckBox("Quilt", modloaderCol2X, cardY + 382, Utils.ModloaderType.QUILT);
        useNeoForge = createModloaderCheckBox("NeoForge", modloaderCol2X, cardY + 414, Utils.ModloaderType.NEOFORGE);
        useOptifine = createModloaderCheckBox("OptiFine", modloaderCol2X, cardY + 446, Utils.ModloaderType.OPTIFINE);

        root.getChildren().add(useVanilla);
        root.getChildren().add(useForge);
        root.getChildren().add(useFabric);
        root.getChildren().add(useQuilt);
        root.getChildren().add(useNeoForge);
        root.getChildren().add(useOptifine);

        setSelectedModloader(resolveConfiguredModloader(pane));
        applyModRestrictionsForVersion(this.versionList.getValue(), pane);

        this.useMusic = new JFXCheckBox(LABEL_PLAY_MUSIC);
        Object cfgMusic = pane.getConfig().getValue(EnumConfig.USE_MUSIC);
        this.useMusic.setSelected(cfgMusic instanceof Boolean ? (Boolean) cfgMusic : false);
        styleCheckBox(this.useMusic);
        this.useMusic.setLayoutX(rightX);
        this.useMusic.setLayoutY(cardY + 478);
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
        this.vmArguments.setPosition(leftX, cardY + 526);
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
        this.connect.setLayoutY(cardY + 570);
        this.connect.setPrefWidth(cardW - 80);
        this.connect.setWrapText(true);
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
            Utils.ModloaderType selectedModloader = getSelectedModloaderFromControls();
            configMap.put("allocatedram", String.valueOf(memorySlider.getValue()));
            configMap.put("gamesize", "" + GameSize.getWindowSize(windowsSizeList.getValue()));
            configMap.put("autologin", "" + autoLogin.isSelected());
            configMap.put("usevmarguments", "" + useVMArguments.isSelected());
            configMap.put("vmarguments", "" + vmArguments.getText());
            configMap.put("version", "" + versionList.getValue());
            configMap.put("language", "" + LanguageList.getValue());
            configMap.put(EnumConfig.USE_FORGE.getOption(), "" + (selectedModloader == Utils.ModloaderType.FORGE));
            configMap.put(EnumConfig.USE_FABRIC.getOption(), "" + (selectedModloader == Utils.ModloaderType.FABRIC));
            configMap.put(EnumConfig.USE_QUILT.getOption(), "" + (selectedModloader == Utils.ModloaderType.QUILT));
            configMap.put(EnumConfig.USE_NEOFORGE.getOption(), "" + (selectedModloader == Utils.ModloaderType.NEOFORGE));
            configMap.put(EnumConfig.USE_OPTIFINE.getOption(), "" + (selectedModloader == Utils.ModloaderType.OPTIFINE));
            configMap.put("usemusic", "" + useMusic.isSelected());
            configMap.put("usediscord", "" + useDiscord.isSelected());
            configMap.put(EnumConfig.USE_CONNECT.getOption(), "" + connect.isSelected());
            configMap.put(CFG_INCLUDE_SNAPSHOTS, "" + includeSnapshots.isSelected());

            pane.getConfig().updateValues(configMap);

            engine.reg(GameMemory.getMemory(Double.parseDouble((String) pane.getConfig().getValue(EnumConfig.RAM))));
            engine.reg(GameSize.getWindowSize(Integer.parseInt((String) pane.getConfig().getValue(EnumConfig.GAME_SIZE))));

            String selectedVersion = String.valueOf(pane.getConfig().getValue(EnumConfig.VERSION));
            try {
                if (selectedModloader == Utils.ModloaderType.OPTIFINE) {
                    App.ensureOptiFineRuntime(selectedVersion);
                }

                GameLinks links = buildGameLinks(selectedVersion, selectedModloader);
                engine.reg(links);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Impossible de préparer la version sélectionnée");
                alert.setContentText(e.getMessage() != null ? e.getMessage() : "Erreur inconnue lors de la préparation du jeu.");
                alert.showAndWait();
                return;
            }

            Utils.regGameStyle(engine, pane.getConfig());

            closeWithAnimation(root, (Stage) ((JFXButton) event.getSource()).getScene().getWindow());
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

            animateIn(modloaderLabel, 12, 0, 520);
            animateIn(useVanilla, 12, 0, 545);
            animateIn(useForge, 12, 0, 570);
            animateIn(useFabric, 12, 0, 595);
            animateIn(useQuilt, 12, 0, 620);
            animateIn(useNeoForge, 12, 0, 645);
            animateIn(useOptifine, 12, 0, 670);
            animateIn(autoLogin, 12, 0, 545);
            animateIn(useDiscord, 12, 0, 570);
            animateIn(useVMArguments, 12, 0, 595);
            animateIn(useMusic, 12, 0, 620);
            animateIn(vmArguments, 12, 0, 695);
            animateIn(connect, 12, 0, 720);
            animateIn(openGameDirButton, 0, 14, 745);
            animateIn(saveButton, 0, 14, 770);

            PauseTransition refreshAfterEntrance = new PauseTransition(Duration.millis(1220));
            refreshAfterEntrance.setOnFinished(event -> applyModRestrictionsForVersion(this.versionList.getValue(), pane));
            refreshAfterEntrance.play();
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

    private void closeWithAnimation(Pane root, Stage stage) {
        if (root == null || stage == null) {
            if (stage != null) stage.close();
            return;
        }

        root.setDisable(true);

        FadeTransition fade = new FadeTransition(Duration.millis(230), root);
        fade.setFromValue(root.getOpacity());
        fade.setToValue(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(230), root);
        slide.setFromY(root.getTranslateY());
        slide.setToY(root.getTranslateY() + 18);

        ScaleTransition scale = new ScaleTransition(Duration.millis(230), root);
        scale.setFromX(root.getScaleX() == 0 ? 1.0 : root.getScaleX());
        scale.setFromY(root.getScaleY() == 0 ? 1.0 : root.getScaleY());
        scale.setToX(0.985);
        scale.setToY(0.985);

        ParallelTransition animation = new ParallelTransition(fade, slide, scale);

        Node clip = root.getClip();
        if (clip != null) {
            TranslateTransition clipSlide = new TranslateTransition(Duration.millis(230), clip);
            clipSlide.setFromY(clip.getTranslateY());
            clipSlide.setToY(clip.getTranslateY() + 18);

            ScaleTransition clipScale = new ScaleTransition(Duration.millis(230), clip);
            clipScale.setFromX(clip.getScaleX() == 0 ? 1.0 : clip.getScaleX());
            clipScale.setFromY(clip.getScaleY() == 0 ? 1.0 : clip.getScaleY());
            clipScale.setToX(0.985);
            clipScale.setToY(0.985);

            animation.getChildren().addAll(clipSlide, clipScale);
        }

        animation.setOnFinished(actionEvent -> stage.close());
        animation.play();
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

    private GameLinks buildGameLinks(String version, Utils.ModloaderType modloaderType) throws IOException {
        if (modloaderType == Utils.ModloaderType.FABRIC
                || modloaderType == Utils.ModloaderType.QUILT
                || modloaderType == Utils.ModloaderType.NEOFORGE) {
            return App.buildOfficialModloaderGameLinks(version, modloaderType);
        }

        String serverBase = SERVER_GAME_BASE_URL + Utils.resolveServerPath(version, modloaderType);

        if (modloaderType == Utils.ModloaderType.OPTIFINE) {
            String fullUrl = mojangVersionUrlById.get(version);
            if ((fullUrl == null || fullUrl.trim().isEmpty()) && App.netIsAvailable()) {
                fullUrl = App.resolveMojangVersionJsonUrlStatic(version);
            }
            if (fullUrl == null || fullUrl.trim().isEmpty()) {
                throw new IOException("Impossible de rÃ©soudre le JSON Mojang pour OptiFine " + version);
            }
            return App.buildOptiFineGameLinksFromResolvedJson(fullUrl, version);
        }


        String fullUrl = mojangVersionUrlById.get(version);
        if ((fullUrl == null || fullUrl.trim().isEmpty()) && App.netIsAvailable()) {
            fullUrl = App.resolveMojangVersionJsonUrlStatic(version);
        }

        if (modloaderType == Utils.ModloaderType.OPTIFINE) {
            if (fullUrl == null || fullUrl.trim().isEmpty()) {
                throw new IOException("Impossible de résoudre le JSON Mojang pour OptiFine " + version);
            }
            return App.buildOptiFineGameLinksFromResolvedJson(fullUrl, version);
        }

        if (fullUrl == null || fullUrl.trim().isEmpty()) {
            return new GameLinks(serverBase, version + ".json");
        }

        serverBase = serverBase;
        return new GameLinks(
                fullUrl,
                serverBase + "ignore.cfg",
                serverBase + "delete.cfg",
                serverBase + "status.cfg",
                serverBase + "files/"
        );
    }

    private String urlModifier() {
        return Utils.resolveServerPath(versionList.getValue(), getSelectedModloaderFromControls());
    }

    private String urlModifier(String version, Utils.ModloaderType modloaderType) {
        return Utils.resolveServerPath(version, modloaderType);
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
        return App.downloadTextStatic(urlStr);
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
        boolean fabricAllowed = !snapshot && isFabricAvailableForVersion(version);
        boolean quiltAllowed = !snapshot && isQuiltAvailableForVersion(version);
        boolean neoForgeAllowed = !snapshot && isNeoForgeAvailableForVersion(version);
        boolean optifineAllowed = !snapshot && isOptiFineAvailableForVersion(version);
        Utils.ModloaderType selectedModloader = getSelectedModloaderFromControls();

        applyModloaderAvailability(useVanilla, true);
        applyModloaderAvailability(useForge, forgeAllowed);
        applyModloaderAvailability(useFabric, fabricAllowed);
        applyModloaderAvailability(useQuilt, quiltAllowed);
        applyModloaderAvailability(useNeoForge, neoForgeAllowed);
        applyModloaderAvailability(useOptifine, optifineAllowed);

        boolean selectedUnavailable =
                (selectedModloader == Utils.ModloaderType.FORGE && !forgeAllowed)
                        || (selectedModloader == Utils.ModloaderType.FABRIC && !fabricAllowed)
                        || (selectedModloader == Utils.ModloaderType.QUILT && !quiltAllowed)
                        || (selectedModloader == Utils.ModloaderType.NEOFORGE && !neoForgeAllowed)
                        || (selectedModloader == Utils.ModloaderType.OPTIFINE && !optifineAllowed);

        if (selectedUnavailable || !hasNonVanillaSelection()) {
            setSelectedModloader(Utils.ModloaderType.VANILLA);
        }
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

    private void loadFabricAvailableVersionsAsync(final LauncherPanel pane) {
        loadGameVersionSetAsync(
                pane,
                "MajestyLauncher-FabricVersions",
                FABRIC_GAME_VERSIONS_URL,
                fabricVersionsAvailable,
                () -> fabricVersionsLoaded = true
        );
    }

    private boolean isFabricAvailableForVersion(String version) {
        return isVersionAvailableFromRemoteSet(version, fabricVersionsAvailable, fabricVersionsLoaded);
    }

    private void loadQuiltAvailableVersionsAsync(final LauncherPanel pane) {
        loadGameVersionSetAsync(
                pane,
                "MajestyLauncher-QuiltVersions",
                QUILT_GAME_VERSIONS_URL,
                quiltVersionsAvailable,
                () -> quiltVersionsLoaded = true
        );
    }

    private boolean isQuiltAvailableForVersion(String version) {
        return isVersionAvailableFromRemoteSet(version, quiltVersionsAvailable, quiltVersionsLoaded);
    }

    private void loadNeoForgeAvailableVersionsAsync(final LauncherPanel pane) {
        Thread loader = new Thread(() -> {
            Set<String> loaded = new HashSet<>();
            try {
                String metadata = downloadText(NEOFORGE_MAVEN_METADATA_URL);
                int start = 0;
                while (start >= 0) {
                    int versionTagStart = metadata.indexOf("<version>", start);
                    if (versionTagStart < 0) {
                        break;
                    }
                    int versionTagEnd = metadata.indexOf("</version>", versionTagStart);
                    if (versionTagEnd < 0) {
                        break;
                    }

                    String artifactVersion = metadata
                            .substring(versionTagStart + "<version>".length(), versionTagEnd)
                            .trim();
                    String minecraftVersion = toNeoForgeMinecraftVersion(artifactVersion);
                    if (minecraftVersion != null) {
                        loaded.add(minecraftVersion);
                    }
                    start = versionTagEnd + "</version>".length();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!loaded.isEmpty()) {
                synchronized (neoForgeVersionsAvailable) {
                    neoForgeVersionsAvailable.clear();
                    neoForgeVersionsAvailable.addAll(loaded);
                    neoForgeVersionsLoaded = true;
                }
            }

            Platform.runLater(() -> applyModRestrictionsForVersion(versionList.getValue(), pane));
        }, "MajestyLauncher-NeoForgeVersions");

        loader.setDaemon(true);
        loader.start();
    }

    private boolean isNeoForgeAvailableForVersion(String version) {
        return isVersionAvailableFromRemoteSet(version, neoForgeVersionsAvailable, neoForgeVersionsLoaded);
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

    private JFXCheckBox createModloaderCheckBox(String label, int x, int y, Utils.ModloaderType modloaderType) {
        JFXCheckBox checkBox = new JFXCheckBox(label);
        styleCheckBox(checkBox);
        checkBox.setPrefWidth(108);
        checkBox.setLayoutX(x);
        checkBox.setLayoutY(y);
        checkBox.setOnAction(event -> handleModloaderToggle(modloaderType));
        return checkBox;
    }

    private Utils.ModloaderType resolveConfiguredModloader(LauncherPanel pane) {
        Utils.ModloaderType modloaderType = Utils.resolveSelectedModloader(pane.getConfig());
        return modloaderType == null ? Utils.ModloaderType.VANILLA : modloaderType;
    }

    private void handleModloaderToggle(Utils.ModloaderType modloaderType) {
        if (updatingModloaderSelection) {
            return;
        }

        JFXCheckBox target = getCheckBoxForModloader(modloaderType);
        if (target == null) {
            return;
        }

        if (target.isDisabled()) {
            setSelectedModloader(Utils.ModloaderType.VANILLA);
            return;
        }

        if (target.isSelected()) {
            setSelectedModloader(modloaderType);
            return;
        }

        if (!hasNonVanillaSelection()) {
            setSelectedModloader(Utils.ModloaderType.VANILLA);
        }
    }

    private void setSelectedModloader(Utils.ModloaderType modloaderType) {
        updatingModloaderSelection = true;
        try {
            if (useVanilla != null) useVanilla.setSelected(modloaderType == Utils.ModloaderType.VANILLA);
            if (useForge != null) useForge.setSelected(modloaderType == Utils.ModloaderType.FORGE);
            if (useFabric != null) useFabric.setSelected(modloaderType == Utils.ModloaderType.FABRIC);
            if (useQuilt != null) useQuilt.setSelected(modloaderType == Utils.ModloaderType.QUILT);
            if (useNeoForge != null) useNeoForge.setSelected(modloaderType == Utils.ModloaderType.NEOFORGE);
            if (useOptifine != null) useOptifine.setSelected(modloaderType == Utils.ModloaderType.OPTIFINE);
        } finally {
            updatingModloaderSelection = false;
        }
    }

    private Utils.ModloaderType getSelectedModloaderFromControls() {
        return Utils.resolveSelectedModloader(
                useForge != null && useForge.isSelected(),
                useFabric != null && useFabric.isSelected(),
                useQuilt != null && useQuilt.isSelected(),
                useNeoForge != null && useNeoForge.isSelected(),
                useOptifine != null && useOptifine.isSelected()
        );
    }

    private boolean hasNonVanillaSelection() {
        return (useForge != null && useForge.isSelected())
                || (useFabric != null && useFabric.isSelected())
                || (useQuilt != null && useQuilt.isSelected())
                || (useNeoForge != null && useNeoForge.isSelected())
                || (useOptifine != null && useOptifine.isSelected());
    }

    private JFXCheckBox getCheckBoxForModloader(Utils.ModloaderType modloaderType) {
        if (modloaderType == Utils.ModloaderType.VANILLA) {
            return useVanilla;
        }
        if (modloaderType == Utils.ModloaderType.FORGE) {
            return useForge;
        }
        if (modloaderType == Utils.ModloaderType.FABRIC) {
            return useFabric;
        }
        if (modloaderType == Utils.ModloaderType.QUILT) {
            return useQuilt;
        }
        if (modloaderType == Utils.ModloaderType.NEOFORGE) {
            return useNeoForge;
        }
        return useOptifine;
    }

    private void applyModloaderAvailability(JFXCheckBox checkBox, boolean allowed) {
        if (checkBox == null) {
            return;
        }
        if (!allowed) {
            checkBox.setSelected(false);
        }
        checkBox.setDisable(!allowed);
        checkBox.setOpacity(allowed ? 1.0 : 0.35);
    }

    private void loadGameVersionSetAsync(
            final LauncherPanel pane,
            String threadName,
            String endpoint,
            Set<String> destination,
            Runnable onLoaded
    ) {
        Thread loader = new Thread(() -> {
            Set<String> loaded = new HashSet<>();
            try {
                loaded.addAll(fetchVersionSetFromMeta(endpoint));
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!loaded.isEmpty()) {
                synchronized (destination) {
                    destination.clear();
                    destination.addAll(loaded);
                    onLoaded.run();
                }
            }

            Platform.runLater(() -> applyModRestrictionsForVersion(versionList.getValue(), pane));
        }, threadName);

        loader.setDaemon(true);
        loader.start();
    }

    private Set<String> fetchVersionSetFromMeta(String endpoint) throws IOException {
        Set<String> versions = new HashSet<>();
        JsonArray root = JsonParser.parseString(downloadText(endpoint)).getAsJsonArray();
        for (JsonElement element : root) {
            JsonObject object = element.getAsJsonObject();
            if (object == null || !object.has("version")) {
                continue;
            }
            String version = object.get("version").getAsString();
            if (version != null && !version.trim().isEmpty()) {
                versions.add(version.trim());
            }
        }
        return versions;
    }

    private boolean isVersionAvailableFromRemoteSet(String version, Set<String> availableVersions, boolean versionsLoaded) {
        if (version == null || version.trim().isEmpty()) {
            return false;
        }
        synchronized (availableVersions) {
            if (versionsLoaded) {
                return availableVersions.contains(version);
            }
        }
        return true;
    }

    private String toNeoForgeMinecraftVersion(String artifactVersion) {
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
