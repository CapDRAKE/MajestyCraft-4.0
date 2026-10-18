package fr.majestycraft.launcher;

import fr.majestycraft.util.Icons;
import fr.majestycraft.util.UiStyle;
import fr.majestycraft.util.AmbientBackground;
import fr.majestycraft.util.ToggleSwitch;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
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

    private final ToggleSwitch autoLogin;
    private final ToggleSwitch connect;
    private static ToggleButton useVanilla;
    private static ToggleButton useForge;
    private static ToggleButton useFabric;
    private static ToggleButton useQuilt;
    private static ToggleButton useNeoForge;
    private static ToggleButton useOptifine;
    private final ToggleSwitch useDiscord;
    private final ToggleSwitch useMusic;
    private final ToggleSwitch useVMArguments;

    private final LauncherTextField vmArguments;

    private ToggleSwitch includeSnapshots;
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

    public static final List<String> VANILLA_SUPPORTED_RELEASES = Arrays.asList(
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

        AmbientBackground.installResponsive(root, false);
        pane.getConfig().loadConfiguration();


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
        this.heroLine1.setText("• Version et modloader");
        this.heroLine1.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.heroLine1.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.70)");
        this.heroLine1.setPosition(120, 362);
        this.heroLine1.setSize(260, 20);

        this.heroLine2 = new LauncherLabel(root);
        this.heroLine2.setText("• Mémoire, fenêtre, langue");
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

        final int cardW = 600;
        final int cardH = 618;
        final int cardX = 360;
        final int cardY = 66;

        LauncherRectangle card = new LauncherRectangle(root, cardX, cardY, cardW, cardH);
        card.setArcWidth(34);
        card.setArcHeight(34);
        card.setFill(Color.rgb(8, 12, 18, 0.78));
        card.setStroke(Color.rgb(255, 255, 255, 0.10));
        card.setStrokeWidth(1);
        card.setEffect(new DropShadow(36, Color.rgb(0, 0, 0, 0.72)));
        card.setMouseTransparent(true);

        /* ===== En-tête ===== */
        LauncherLabel titleLabel = new LauncherLabel(root);
        titleLabel.setText(LABEL_SETTINGS);
        titleLabel.setStyle("-fx-text-fill: white;");
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 24F));
        titleLabel.setSize(cardW - 110, 34);
        titleLabel.setPosition(cardX + 28, cardY + 22);

        LauncherLabel subTitleLabel = new LauncherLabel(root);
        fr.majestycraft.profiles.ProfileManager pmForTitle = fr.majestycraft.profiles.ProfileManager.get();
        String activeProfileName = (pmForTitle != null && pmForTitle.getActive() != null)
                ? pmForTitle.getActive().getName() : null;
        subTitleLabel.setText(activeProfileName != null
                ? "Profil actif : " + activeProfileName + "  •  version, modloader et RAM s'appliquent à ce profil"
                : "Configuration du jeu et du launcher");
        subTitleLabel.getStyleClass().add("ml-hint");
        subTitleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        subTitleLabel.setSize(cardW - 110, 18);
        subTitleLabel.setPosition(cardX + 30, cardY + 58);

        JFXButton closeButton = new JFXButton();
        closeButton.setGraphic(Icons.stroke(Icons.CLOSE, 16));
        closeButton.setPrefSize(36, 32);
        UiStyle.apply(closeButton, "ml-window", "ml-window-close");
        closeButton.setLayoutX(cardX + cardW - 54);
        closeButton.setLayoutY(cardY + 20);
        closeButton.setOnAction(event -> closeWithAnimation(root, (Stage) closeButton.getScene().getWindow()));
        root.getChildren().add(closeButton);

        /* ===== Deux blocs : JEU (à gauche) et LAUNCHER (à droite) ===== */
        final int gap = 16;
        final int sectionW = (cardW - 48 - gap) / 2;
        final int sectionH = 432;
        final int sectionY = cardY + 94;
        final int leftSection = cardX + 24;
        final int rightSection = leftSection + sectionW + gap;
        final int pad = 18;
        final int leftX = leftSection + pad;
        final int rightX = rightSection + pad;
        final int fieldW = sectionW - pad * 2;

        Region gameSection = createSection(root, leftSection, sectionY, sectionW, sectionH);
        Region launcherSection = createSection(root, rightSection, sectionY, sectionW, sectionH);
        LauncherLabel sectionGame = createSectionTitle(root, "JEU", Icons.PLAY, leftX, sectionY + 16);
        LauncherLabel sectionLauncher = createSectionTitle(root, "LAUNCHER", Icons.SETTINGS, rightX, sectionY + 16);

        /* ===== Version ===== */
        LauncherLabel versionLabel = createFieldLabel(root, LABEL_CHOOSE_VERSION, leftX, sectionY + 50, fieldW);

        this.versionList = new JFXComboBox<>();
        styleCombo(this.versionList);
        this.versionList.setPrefSize(fieldW, 34);
        this.versionList.setLayoutX(leftX);
        this.versionList.setLayoutY(sectionY + 74);
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
                header.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11px; -fx-padding: 6 0 2 0;");
                value.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 13px; -fx-padding: 0 0 6 0;");
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
                setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
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

        this.includeSnapshots = createSwitch(root, LABEL_INCLUDE_SNAPSHOTS, includeSnapshotsEnabled, leftX, sectionY + 116, fieldW);
        this.includeSnapshots.setOnAction(e -> {
            includeSnapshotsEnabled = includeSnapshots.isSelected();
            pane.getConfig().updateValue(CFG_INCLUDE_SNAPSHOTS, includeSnapshotsEnabled);

            String preferred = this.versionList.getValue();
            populateVersionListFromMojang(pane, preferred);
        });

        loadForgeAvailableVersionsAsync(pane);
        loadFabricAvailableVersionsAsync(pane);
        loadQuiltAvailableVersionsAsync(pane);
        loadNeoForgeAvailableVersionsAsync(pane);
        loadOptiFineAvailableVersionsAsync(pane);
        populateVersionListFromMojang(pane, (String) pane.getConfig().getValue(EnumConfig.VERSION));
        this.versionList.setOnAction(event -> applyModRestrictionsForVersion(versionList.getValue(), pane));

        /* ===== Modloader : une pastille par chargeur, un seul choix possible ===== */
        this.modloaderLabel = createFieldLabel(root, "Modloader", leftX, sectionY + 156, fieldW);

        final int chipGap = 8;
        final int chipW = (fieldW - chipGap * 2) / 3;
        final int chipRow1 = sectionY + 180;
        final int chipRow2 = chipRow1 + 30 + chipGap;
        useVanilla = createModloaderChip("Vanilla", leftX, chipRow1, chipW, Utils.ModloaderType.VANILLA);
        useForge = createModloaderChip("Forge", leftX + chipW + chipGap, chipRow1, chipW, Utils.ModloaderType.FORGE);
        useFabric = createModloaderChip("Fabric", leftX + (chipW + chipGap) * 2, chipRow1, chipW, Utils.ModloaderType.FABRIC);
        useQuilt = createModloaderChip("Quilt", leftX, chipRow2, chipW, Utils.ModloaderType.QUILT);
        useNeoForge = createModloaderChip("NeoForge", leftX + chipW + chipGap, chipRow2, chipW, Utils.ModloaderType.NEOFORGE);
        useOptifine = createModloaderChip("OptiFine", leftX + (chipW + chipGap) * 2, chipRow2, chipW, Utils.ModloaderType.OPTIFINE);

        root.getChildren().add(useVanilla);
        root.getChildren().add(useForge);
        root.getChildren().add(useFabric);
        root.getChildren().add(useQuilt);
        root.getChildren().add(useNeoForge);
        root.getChildren().add(useOptifine);

        setSelectedModloader(resolveConfiguredModloader(pane));
        applyModRestrictionsForVersion(this.versionList.getValue(), pane);

        /* ===== RAM ===== */
        final int systemRamGb = detectSystemRamGb();
        // Plafond du slider : on ne laisse pas allouer plus que la RAM physique
        // (cause classique de crash), tout en gardant une marge basse raisonnable.
        final int sliderMax = Math.max(2, Math.min(systemRamGb, 24));
        // Seuil au-delà duquel on avertit (il faut laisser de la RAM à l'OS).
        final int safeMax = Math.max(2, systemRamGb - 2);

        LauncherLabel ramLabel = createFieldLabel(root, LABEL_RAM_ALLOC, leftX, sectionY + 270, fieldW - 80);

        this.memorySliderLabel = new LauncherLabel(root);
        this.memorySliderLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.memorySliderLabel.getStyleClass().add("ml-badge");
        this.memorySliderLabel.setPrefHeight(22);
        this.memorySliderLabel.setMinWidth(Region.USE_PREF_SIZE);
        // Pastille calée à droite du bloc, quelle que soit la largeur du texte.
        this.memorySliderLabel.layoutXProperty().bind(
                this.memorySliderLabel.widthProperty().multiply(-1).add(leftX + fieldW));
        this.memorySliderLabel.setLayoutY(sectionY + 268);

        this.memorySlider = new JFXSlider();
        this.memorySlider.setMin(1);
        this.memorySlider.setMax(sliderMax);

        double initialRam = 2.0D;
        if (pane.getConfig().getValue(EnumConfig.RAM) != null) {
            try { initialRam = Double.parseDouble((String) pane.getConfig().getValue(EnumConfig.RAM)); } catch (Exception ignored) {}
        }
        // Borne la valeur enregistrée au nouveau plafond (config héritée d'une autre machine).
        this.memorySlider.setValue(Math.max(1, Math.min(initialRam, sliderMax)));

        this.memorySlider.setLayoutX(leftX);
        this.memorySlider.setLayoutY(sectionY + 302);
        this.memorySlider.setPrefWidth(fieldW);
        this.memorySlider.setBlockIncrement(1);
        this.memorySlider.setStyle(
                "-jfx-default-thumb: #ff9800;" +
                "-jfx-default-track: rgba(255,255,255,0.18);" +
                "-fx-pref-height: 10px;"
        );

        this.memorySlider.valueProperty().addListener((ov, oldVal, newVal) ->
                memorySlider.setValue(Math.round(newVal.doubleValue()))
        );
        this.memorySlider.valueProperty().addListener((obs, o, n) -> updateRamBadge(n.intValue(), safeMax));
        root.getChildren().add(this.memorySlider);
        updateRamBadge((int) this.memorySlider.getValue(), safeMax);

        LauncherLabel ramSystemHint = new LauncherLabel(root);
        ramSystemHint.setText("RAM système détectée : " + systemRamGb + " Go");
        ramSystemHint.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
        ramSystemHint.getStyleClass().add("ml-hint");
        ramSystemHint.setSize(fieldW, 16);
        ramSystemHint.setPosition(leftX, sectionY + 326);

        /* ===== Taille fenêtre ===== */
        LauncherLabel windowsSizeLabel = createFieldLabel(root, LABEL_WINDOW_SIZE, leftX, sectionY + 356, fieldW);

        this.windowsSizeList = new JFXComboBox<>();
        populateSizeList();
        styleCombo(this.windowsSizeList);
        this.windowsSizeList.setPromptText("Sélectionner...");
        this.windowsSizeList.setPrefSize(fieldW, 34);
        this.windowsSizeList.setLayoutX(leftX);
        this.windowsSizeList.setLayoutY(sectionY + 380);
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
                setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
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
        LauncherLabel languageLabel = createFieldLabel(root, LANGUAGE, rightX, sectionY + 50, fieldW);

        this.LanguageList = new JFXComboBox<>();
        languageList();
        styleCombo(this.LanguageList);
        this.LanguageList.setPrefSize(fieldW, 34);
        this.LanguageList.setLayoutX(rightX);
        this.LanguageList.setLayoutY(sectionY + 74);
        this.LanguageList.setVisibleRowCount(5);

        this.LanguageList.setButtonCell(new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
                setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
            }
        });

        this.LanguageList.setCellFactory(cb -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
                setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
            }
        });

        // Les anciennes versions enregistraient "Fran?ais" (accent perdu dans la valeur
        // par défaut) : toute langue inconnue revient au français.
        String savedLanguage = (String) pane.getConfig().getValue(EnumConfig.LANGUAGE);
        this.LanguageList.setValue(this.LanguageList.getItems().contains(savedLanguage) ? savedLanguage : "Français");
        this.LanguageList.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(Main.bundle.getString("alert.title"));
            alert.setHeaderText(Main.bundle.getString("alert.header"));
            alert.setContentText(Main.bundle.getString("alert.content"));
            alert.showAndWait();
        });
        root.getChildren().add(this.LanguageList);

        /* ===== Interrupteurs du launcher ===== */
        this.autoLogin = createSwitch(root, LABEL_AUTO_CONNECT,
                pane.getConfig().getValue(EnumConfig.AUTOLOGIN), rightX, sectionY + 126, fieldW);

        this.useDiscord = createSwitch(root, LABEL_DISCORD_STATUS,
                pane.getConfig().getValue(EnumConfig.USE_DISCORD), rightX, sectionY + 162, fieldW);
        this.useDiscord.setOnAction(e -> {
            if (useDiscord.isSelected()) pane.getRpc().start();
            else pane.getRpc().stop();
        });

        this.useMusic = createSwitch(root, LABEL_PLAY_MUSIC,
                pane.getConfig().getValue(EnumConfig.USE_MUSIC), rightX, sectionY + 198, fieldW);
        this.useMusic.setOnAction(e -> {
            pane.getConfig().updateValue("usemusic", useMusic.isSelected());
            pane.getMediaPlayer().setMute(!useMusic.isSelected());
        });

        this.connect = createSwitch(root, LABEL_CONNECT_SERVER,
                pane.getConfig().getValue(EnumConfig.USE_CONNECT), rightX, sectionY + 234, fieldW);
        this.connect.setWrapText(true);
        this.connect.setOnAction(e -> {
            pane.getConfig().updateValue("useConnect", connect.isSelected());
            if (connect.isSelected()) engine.reg(App.getGameConnect());
        });

        LauncherRectangle divider = new LauncherRectangle(root, rightX, sectionY + 300, fieldW, 1);
        divider.setFill(Color.rgb(255, 255, 255, 0.08));

        /* ===== Arguments JVM ===== */
        this.useVMArguments = createSwitch(root, LABEL_USE_JVM_ARGUMENTS,
                pane.getConfig().getValue(EnumConfig.USE_VM_ARGUMENTS), rightX, sectionY + 318, fieldW);

        this.vmArguments = new LauncherTextField(root);
        this.vmArguments.setText((String) pane.getConfig().getValue(EnumConfig.VM_ARGUMENTS));
        this.vmArguments.setPromptText("-XX:+UseG1GC ...");
        this.vmArguments.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.vmArguments.getStyleClass().add("ml-input");
        this.vmArguments.setSize(fieldW, 34);
        this.vmArguments.setPosition(rightX, sectionY + 356);
        this.useVMArguments.setOnAction(e -> vmArguments.setDisable(!useVMArguments.isSelected()));
        this.vmArguments.setDisable(!this.useVMArguments.isSelected());

        /* ===== Boutons ===== */
        final int footerY = sectionY + sectionH + 22;

        JFXButton openGameDirButton = new JFXButton(BUTTON_OPEN_GAME_DIR);
        styleSecondaryButton(openGameDirButton);
        openGameDirButton.setGraphic(Icons.stroke(Icons.FOLDER, 16));
        openGameDirButton.setGraphicTextGap(10);
        openGameDirButton.setLayoutX(leftSection);
        openGameDirButton.setLayoutY(footerY);
        openGameDirButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        openGameDirButton.setPrefSize(sectionW, 42);
        openGameDirButton.setOnAction(e -> openGameDirectory());
        root.getChildren().add(openGameDirButton);

        JFXButton saveButton = new JFXButton(BUTTON_VALIDATE);
        stylePrimaryButton(saveButton);
        saveButton.setGraphic(Icons.stroke(Icons.CHECK, 16));
        saveButton.setGraphicTextGap(10);
        saveButton.setLayoutX(rightSection + sectionW - 200);
        saveButton.setLayoutY(footerY);
        saveButton.setPrefSize(200, 42);
        saveButton.setOnAction(event -> {
            if (versionList.isDisabled() || "Chargement...".equals(versionList.getValue())) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Liste des versions en cours de chargement");
                alert.setContentText("Merci de patienter quelques secondes que la liste des versions soit chargée avant d'enregistrer.");
                alert.showAndWait();
                return;
            }

            Utils.ModloaderType selectedModloader = getSelectedModloaderFromControls();
            String selectedVersion = String.valueOf(versionList.getValue());

            // On résout d'abord les GameLinks (réseau, peut échouer) AVANT de persister
            // quoi que ce soit : si ça échoue, la config et le gameEngine restent sur
            // l'ancienne version/modloader (cohérents et fonctionnels), au lieu de rester
            // bloqués sur un état à moitié mis à jour qui obligerait à relancer le launcher.
            GameLinks links;
            try {
                if (selectedModloader == Utils.ModloaderType.OPTIFINE) {
                    App.ensureOptiFineRuntime(selectedVersion);
                }

                links = buildGameLinks(selectedVersion, selectedModloader);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Impossible de préparer la version sélectionnée");
                alert.setContentText(e.getMessage() != null ? e.getMessage() : "Erreur inconnue lors de la préparation du jeu.");
                alert.showAndWait();
                return;
            }

            HashMap<String, String> configMap = new HashMap<>();
            configMap.put("allocatedram", String.valueOf(memorySlider.getValue()));
            configMap.put("gamesize", "" + GameSize.getWindowSize(windowsSizeList.getValue()));
            configMap.put("autologin", "" + autoLogin.isSelected());
            configMap.put("usevmarguments", "" + useVMArguments.isSelected());
            configMap.put("vmarguments", "" + vmArguments.getText());
            configMap.put("version", "" + selectedVersion);
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

            // Le profil actif EST la sélection version/modloader/RAM : on le tient à jour
            fr.majestycraft.profiles.ProfileManager pm = fr.majestycraft.profiles.ProfileManager.get();
            if (pm != null && pm.getActive() != null) {
                fr.majestycraft.profiles.LauncherProfile active = pm.getActive();
                active.setVersion(selectedVersion);
                active.setModloaderType(selectedModloader);
                active.setRam(memorySlider.getValue());
                active.setGameSize("" + GameSize.getWindowSize(windowsSizeList.getValue()));
                active.setUseVmArguments(useVMArguments.isSelected());
                active.setVmArguments(vmArguments.getText() == null ? "" : vmArguments.getText());
                pm.update(active);
                if (pane != null) pane.refreshProfilesUI();
            }

            engine.reg(GameMemory.getMemory(Double.parseDouble((String) pane.getConfig().getValue(EnumConfig.RAM))));
            engine.reg(GameSize.getWindowSize(Integer.parseInt((String) pane.getConfig().getValue(EnumConfig.GAME_SIZE))));
            engine.reg(links);

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
            animateIn(subTitleLabel, 20, 0, 200);
            animateIn(closeButton, 0, 0, 200);

            // Chaque bloc entre d'un seul tenant (fond, titre et contrôles ensemble).
            Node[] gameBlock = {gameSection, sectionGame, versionLabel, this.versionList, this.includeSnapshots,
                    modloaderLabel, useVanilla, useForge, useFabric, useQuilt, useNeoForge, useOptifine,
                    ramLabel, this.memorySliderLabel, this.memorySlider, ramSystemHint,
                    windowsSizeLabel, this.windowsSizeList};
            Node[] launcherBlock = {launcherSection, sectionLauncher, languageLabel, this.LanguageList,
                    autoLogin, useDiscord, useMusic, connect, divider, useVMArguments, vmArguments};
            for (Node node : gameBlock) animateIn(node, 0, 14, 240);
            for (Node node : launcherBlock) animateIn(node, 0, 14, 320);

            animateIn(openGameDirButton, 0, 14, 420);
            animateIn(saveButton, 0, 14, 460);

            PauseTransition refreshAfterEntrance = new PauseTransition(Duration.millis(1220));
            refreshAfterEntrance.setOnFinished(event -> applyModRestrictionsForVersion(this.versionList.getValue(), pane));
            refreshAfterEntrance.play();
        });
    }

    /**
     * RAM physique totale de la machine, en Go (arrondi). Repli sur 8 Go si l'info
     * n'est pas disponible (JVM non-HotSpot).
     */
    private int detectSystemRamGb() {
        try {
            java.lang.management.OperatingSystemMXBean bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean) {
                long bytes = ((com.sun.management.OperatingSystemMXBean) bean).getTotalPhysicalMemorySize();
                if (bytes > 0) {
                    return (int) Math.max(1, Math.round(bytes / (1024.0 * 1024.0 * 1024.0)));
                }
            }
        } catch (Throwable ignored) {}
        return 8;
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
        cb.getStyleClass().addAll("combo-modern", "ml-combo");
        cb.setFocusColor(Color.TRANSPARENT);
        cb.setUnFocusColor(Color.TRANSPARENT);
    }

    /** Fond d'un bloc de réglages (JEU, LAUNCHER). */
    private Region createSection(Pane root, int x, int y, int w, int h) {
        Region section = new Region();
        section.getStyleClass().add("ml-section");
        section.setLayoutX(x);
        section.setLayoutY(y);
        section.setPrefSize(w, h);
        section.setMouseTransparent(true);
        root.getChildren().add(section);
        return section;
    }

    private LauncherLabel createSectionTitle(Pane root, String text, String icon, int x, int y) {
        LauncherLabel title = new LauncherLabel(root);
        title.setText(text);
        title.setGraphic(Icons.stroke(icon, 13));
        title.setGraphicTextGap(8);
        title.getStyleClass().add("ml-section-title");
        title.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        title.setPosition(x, y);
        title.setSize(200, 20);
        return title;
    }

    private LauncherLabel createFieldLabel(Pane root, String text, int x, int y, int w) {
        LauncherLabel label = new LauncherLabel(root);
        label.setText(withoutColon(text));
        label.getStyleClass().add("ml-field-label");
        label.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        label.setPosition(x, y);
        label.setSize(w, 20);
        return label;
    }

    /** Les libellés traduits finissent parfois par « : », inutile au-dessus d'un champ. */
    private static String withoutColon(String text) {
        return text == null ? "" : text.replaceAll("\\s*:\\s*$", "");
    }

    /** Interrupteur initialisé depuis une valeur de config (booléen ou texte "true"). */
    private ToggleSwitch createSwitch(Pane root, String text, Object configValue, int x, int y, int w) {
        ToggleSwitch toggle = new ToggleSwitch(text);
        toggle.setSelected(configValue instanceof Boolean
                ? (Boolean) configValue
                : Boolean.parseBoolean(String.valueOf(configValue)));
        toggle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        toggle.setPrefWidth(w);
        toggle.setLayoutX(x);
        toggle.setLayoutY(y);
        root.getChildren().add(toggle);
        return toggle;
    }

    private void updateRamBadge(int gb, int safeMax) {
        memorySliderLabel.setText(gb + " Go");
        // Rouge quand on dépasse le seuil sûr (trop peu de RAM laissée à l'OS).
        memorySliderLabel.getStyleClass().remove("ml-badge-warn");
        if (gb > safeMax) memorySliderLabel.getStyleClass().add("ml-badge-warn");
    }

    private void stylePrimaryButton(JFXButton btn) {
        btn.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        UiStyle.primary(btn);
    }

    private void styleSecondaryButton(JFXButton btn) {
        btn.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        UiStyle.secondary(btn);
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
                serverBase + "ignore.json",
                serverBase + "delete.json",
                serverBase + "status.json",
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

    private ToggleButton createModloaderChip(String label, int x, int y, int width, Utils.ModloaderType modloaderType) {
        ToggleButton chip = new ToggleButton(label);
        UiStyle.apply(chip, "ml-chip");
        chip.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        chip.setPadding(new Insets(0, 2, 0, 2));
        chip.setPrefSize(width, 30);
        chip.setLayoutX(x);
        chip.setLayoutY(y);
        chip.setOnAction(event -> handleModloaderToggle(modloaderType));
        return chip;
    }

    private Utils.ModloaderType resolveConfiguredModloader(LauncherPanel pane) {
        Utils.ModloaderType modloaderType = Utils.resolveSelectedModloader(pane.getConfig());
        return modloaderType == null ? Utils.ModloaderType.VANILLA : modloaderType;
    }

    private void handleModloaderToggle(Utils.ModloaderType modloaderType) {
        if (updatingModloaderSelection) {
            return;
        }

        ToggleButton target = getCheckBoxForModloader(modloaderType);
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

    private ToggleButton getCheckBoxForModloader(Utils.ModloaderType modloaderType) {
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

    private void applyModloaderAvailability(ToggleButton checkBox, boolean allowed) {
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
