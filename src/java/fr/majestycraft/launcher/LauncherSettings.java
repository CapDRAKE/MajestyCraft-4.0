package fr.majestycraft.launcher;

import animatefx.animation.*;
import com.jfoenix.controls.*;

import fr.majestycraft.*;
import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.utils.*;
import fr.trxyy.alternative.alternative_api.utils.config.*;
import fr.trxyy.alternative.alternative_api_ui.base.*;
import fr.trxyy.alternative.alternative_api_ui.components.*;
import javafx.application.*;
import javafx.beans.value.*;
import javafx.event.*;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.util.*;

import java.awt.Desktop;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    // NOUVEAU : checkbox snapshots
    private JFXCheckBox includeSnapshots;
    private boolean includeSnapshotsEnabled = true;

    private double xOffSet; // Position x à l'instant du clic
    private double yOffSet; // Position y à l'instant du clic
    Stage stage; // Le stage qu'on voudra faire bouger (ici notre menu des paramètres)

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
    // Clé config (issue de EnumConfig) => PAS dans le bundle
    private static final String CFG_INCLUDE_SNAPSHOTS = EnumConfig.CFG_INCLUDE_SNAPSHOTS.getOption();

    // Label UI i18n (optionnel) + fallback
    private static final String LABEL_INCLUDE_SNAPSHOTS =
            Main.bundle.containsKey("LABEL_INCLUDE_SNAPSHOTS")
                    ? Main.bundle.getString("LABEL_INCLUDE_SNAPSHOTS")
                    : "Afficher les snapshots";


    // ===================== Mojang manifest (vanilla) =====================
    private static final String MOJANG_MANIFEST_PRIMARY =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String MOJANG_MANIFEST_FALLBACK =
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    // Limite snapshots (sinon la liste est énorme)
    private static final int MAX_SNAPSHOTS = 50;

    // versionId -> URL du json Mojang pour cette version
    private final Map<String, String> mojangVersionUrlById = new HashMap<>();
    // versionId -> "release"/"snapshot"
    private final Map<String, String> mojangVersionTypeById = new HashMap<>();

    @SuppressWarnings("unused")
    private final Gson gson = new Gson();

    // ===================== Liste vanilla supportée (ancienne liste) =====================
    // => sert à filtrer/ordonner les releases
    private static final List<String> VANILLA_SUPPORTED_RELEASES = Arrays.asList(
            "1.8", "1.9", "1.10.2", "1.11.2", "1.12.2", "1.13.2", "1.14.4", "1.15.2",
            "1.16.2", "1.16.3", "1.16.4", "1.16.5", "1.17", "1.17.1", "1.18", "1.18.1",
            "1.18.2", "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4", "1.20", "1.20.1", "1.20.2",
            "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4",
            "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11"
    );
    private static final Set<String> VANILLA_SUPPORTED_RELEASE_SET = new HashSet<>(VANILLA_SUPPORTED_RELEASES);

    // ===================== Anciennes "restrictions" => on va en déduire les supports =====================
    private static final Set<String> FORGE_UNSUPPORTED_VERSIONS = new HashSet<>(Arrays.asList(
            "1.8", "1.19.2", "1.19.3", "1.19.4", "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4",
            "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5",
            "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11"
    ));

    private static final Set<String> OPTIFINE_UNSUPPORTED_VERSIONS = new HashSet<>(Arrays.asList(
            "1.8", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.2", "1.21.5", "1.21.11"
    ));

    // ===================== NOUVEAU : versions qui ONT Forge/Optifine =====================
    private static final Set<String> FORGE_SUPPORTED_VERSIONS = new HashSet<>();
    private static final Set<String> OPTIFINE_SUPPORTED_VERSIONS = new HashSet<>();
    static {
        FORGE_SUPPORTED_VERSIONS.addAll(VANILLA_SUPPORTED_RELEASES);
        FORGE_SUPPORTED_VERSIONS.removeAll(FORGE_UNSUPPORTED_VERSIONS);

        OPTIFINE_SUPPORTED_VERSIONS.addAll(VANILLA_SUPPORTED_RELEASES);
        OPTIFINE_SUPPORTED_VERSIONS.removeAll(OPTIFINE_UNSUPPORTED_VERSIONS);
    }

    public LauncherSettings(final Pane root, final GameEngine engine, final LauncherPanel pane) {

        /* ===================== BOUGER LE MENU PARAMETRE ===================== */
        root.setOnMousePressed(event -> {
            xOffSet = event.getSceneX();
            yOffSet = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            stage = (Stage) memorySlider.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffSet);
            stage.setY(event.getScreenY() - yOffSet);
        });

        this.drawBackgroundImage(engine, root, "background.png");
        pane.getConfig().loadConfiguration();

        /* ===================== RECTANGLE NOIR EN HAUT ===================== */
        LauncherRectangle topRectangle = new LauncherRectangle(root, 0, 0, 1500, 15);
        topRectangle.setOpacity(0.7);

        /* ===================== LABEL TITRE ===================== */
        LauncherLabel titleLabel = new LauncherLabel(root);
        titleLabel.setText(LABEL_SETTINGS);
        titleLabel.setStyle("-fx-text-fill: white;");
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        titleLabel.setPosition(350, 20);
        titleLabel.setSize(230, 35);

        /* ===================== MC SIZE LABEL ===================== */
        LauncherLabel windowsSizeLabel = new LauncherLabel(root);
        windowsSizeLabel.setText(LABEL_WINDOW_SIZE);
        windowsSizeLabel.setOpacity(1.0);
        windowsSizeLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        windowsSizeLabel.setStyle("-fx-text-fill: white;");
        windowsSizeLabel.setSize(370, 30);
        windowsSizeLabel.setPosition(250, 110);

        /* ===================== MC SIZE LIST ===================== */
        this.windowsSizeList = new JFXComboBox<>();
        this.populateSizeList();
        if (pane.getConfig().getValue(EnumConfig.GAME_SIZE) != null) {
            this.windowsSizeList.setValue(GameSize
                    .getWindowSize(Integer.parseInt((String) pane.getConfig().getValue(EnumConfig.GAME_SIZE))).getDesc());
        }
        this.windowsSizeList.setPrefSize(150, 20);
        this.windowsSizeList.setLayoutX(490);
        this.windowsSizeList.setLayoutY(115);
        this.windowsSizeList.setVisibleRowCount(5);
        root.getChildren().add(this.windowsSizeList);

        /* ===================== LAUNCHER LANGUAGE SELECTION LABEL ===================== */
        LauncherLabel LanguageLabel = new LauncherLabel(root);
        LanguageLabel.setText(LANGUAGE);
        LanguageLabel.setOpacity(1.0);
        LanguageLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        LanguageLabel.setStyle("-fx-text-fill: white;");
        LanguageLabel.setSize(490, 30);
        LanguageLabel.setPosition(500, 360);

        /* ===================== LAUNCHER LANGUAGE SELECTION ===================== */
        this.LanguageList = new JFXComboBox<>();
        this.languageList();
        this.LanguageList.setPrefSize(150, 20);
        this.LanguageList.setLayoutX(500);
        this.LanguageList.setLayoutY(385);
        this.LanguageList.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white;");
                }
            }
        });

        this.LanguageList.setVisibleRowCount(5);
        this.LanguageList.setValue((String) pane.getConfig().getValue(EnumConfig.LANGUAGE));
        this.LanguageList.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(Main.bundle.getString("alert.title"));
            alert.setHeaderText(Main.bundle.getString("alert.header"));
            alert.setContentText(Main.bundle.getString("alert.content"));
            alert.showAndWait();
        });
        root.getChildren().add(this.LanguageList);

        /* ===================== SLIDER RAM LABEL ===================== */
        LauncherLabel sliderLabel = new LauncherLabel(root);
        sliderLabel.setText(LABEL_RAM_ALLOC);
        sliderLabel.setOpacity(1.0);
        sliderLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        sliderLabel.setStyle("-fx-text-fill: white;");
        sliderLabel.setSize(370, 30);
        sliderLabel.setPosition(250, 220);

        /* ===================== SLIDER RAM LABEL SELECTIONNED ===================== */
        this.memorySliderLabel = new LauncherLabel(root);
        this.memorySliderLabel.setOpacity(1.0);
        this.memorySliderLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        this.memorySliderLabel.setStyle("-fx-text-fill: white;");
        this.memorySliderLabel.setSize(370, 30);
        this.memorySliderLabel.setPosition(540, 220);

        /* ===================== SLIDER RAM ===================== */
        this.memorySlider = new JFXSlider();
        this.memorySlider.setStyle(
                "    -jfx-default-thumb: #FF0000;\r\n" + "    -jfx-default-track: #212121; -fx-pref-height: 10px;");
        this.memorySlider.setMin(1);
        this.memorySlider.setMax(10);
        if (pane.getConfig().getValue(EnumConfig.RAM) != null) {
            double d = Double.parseDouble((String) pane.getConfig().getValue(EnumConfig.RAM));
            this.memorySlider.setValue(d);
        }
        this.memorySlider.setLayoutX(250);
        this.memorySlider.setLayoutY(260);
        this.memorySlider.setPrefWidth(395);
        this.memorySlider.setBlockIncrement(1);
        memorySlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                memorySlider.setValue(Math.round(new_val.doubleValue()));
            }
        });
        this.memorySlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                memorySliderLabel.setText(newValue + "GB");
            }
        });
        Platform.runLater(() -> root.getChildren().add(memorySlider));

        this.memorySliderLabel.setText(this.memorySlider.getValue() + "Gb");

        /* ===================== CHECKBOX USE Forge ===================== */
        useForge = new JFXCheckBox();
        useForge.setText("Forge");
        Object cfgForge = pane.getConfig().getValue(EnumConfig.USE_FORGE);
        useForge.setSelected(cfgForge instanceof Boolean ? (Boolean) cfgForge : false);
        useForge.setOpacity(1.0);
        useForge.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        useForge.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        useForge.setLayoutX(250);
        useForge.setLayoutY(305);
        useForge.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (useOptifine != null) useOptifine.setSelected(false);
                pane.getConfig().updateValue("useForge", useForge.isSelected());
            }
        });
        root.getChildren().add(useForge);

        /* ===================== CHECKBOX USE Optifine ===================== */
        useOptifine = new JFXCheckBox();
        useOptifine.setText("Optifine");
        Object cfgOpti = pane.getConfig().getValue(EnumConfig.USE_OPTIFINE);
        useOptifine.setSelected(cfgOpti instanceof Boolean ? (Boolean) cfgOpti : false);
        useOptifine.setOpacity(1.0);
        useOptifine.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        useOptifine.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        useOptifine.setLayoutX(500);
        useOptifine.setLayoutY(305);
        useOptifine.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (useForge != null) useForge.setSelected(false);
                pane.getConfig().updateValue("useOptifine", useOptifine.isSelected());
            }
        });
        root.getChildren().add(useOptifine);

        /* ===================== MC VERSION LABEL ===================== */
        LauncherLabel versionListLabel = new LauncherLabel(root);
        versionListLabel.setText(LABEL_CHOOSE_VERSION);
        versionListLabel.setOpacity(1.0);
        versionListLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        versionListLabel.setStyle("-fx-text-fill: white;");
        versionListLabel.setSize(370, 30);
        versionListLabel.setPosition(250, 160);

        /* ===================== MC VERSION LIST ===================== */
        this.versionList = new JFXComboBox<>();
        // Reprend l'ancienne taille/position
        this.versionList.setPrefSize(150, 20);
        this.versionList.setLayoutX(490);
        this.versionList.setLayoutY(165);
        this.versionList.setVisibleRowCount(10);

        // Champ sélectionné : blanc (sur fond du launcher)
        this.versionList.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item + (isSnapshot(item) ? " (snapshot)" : ""));
                }
                setStyle("-fx-text-fill: white;");
            }
        });

        // Dropdown : noir + séparateurs visuels
        this.versionList.setCellFactory(cb -> new ListCell<String>() {

            private final Label header = new Label();
            private final Label value = new Label();
            private final VBox box = new VBox(header, value);

            {
                // Style du séparateur
                header.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px; -fx-padding: 6 0 2 0;");
                // Style de la valeur (dropdown fond blanc)
                value.setStyle("-fx-text-fill: black; -fx-font-size: 13px; -fx-padding: 0 0 6 0;");
                setGraphic(box);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // Remet le graphic (JavaFX peut le "perdre" sur refresh)
                setGraphic(box);

                // Texte version
                value.setText(item + (isSnapshot(item) ? " (snapshot)" : ""));

                // Ne pas afficher de header pour "Chargement..."
                if ("Chargement...".equals(item)) {
                    header.setManaged(false);
                    header.setVisible(false);
                    return;
                }

                int idx = getIndex();
                List<String> items = getListView() != null ? getListView().getItems() : null;

                boolean showHeader = false;

                if (idx <= 0 || items == null || idx >= items.size()) {
                    showHeader = true;
                } else {
                    String prev = items.get(idx - 1);
                    if (prev == null || "Chargement...".equals(prev)) {
                        showHeader = true;
                    } else {
                        boolean currSnap = isSnapshot(item);
                        boolean prevSnap = isSnapshot(prev);
                        showHeader = (currSnap != prevSnap); // changement de "type" => nouveau bloc
                    }
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

        /* ===================== CHECKBOX INCLUDE SNAPSHOTS (NOUVEAU) ===================== */
        Object cfgSnap = pane.getConfig().getValue(EnumConfig.CFG_INCLUDE_SNAPSHOTS);
        if (cfgSnap instanceof Boolean) {
            includeSnapshotsEnabled = (Boolean) cfgSnap;
        } else if (cfgSnap instanceof String) {
            includeSnapshotsEnabled = Boolean.parseBoolean((String) cfgSnap);
        } else {
            includeSnapshotsEnabled = true; // default
        }

        this.includeSnapshots = new JFXCheckBox();
        this.includeSnapshots.setText(LABEL_INCLUDE_SNAPSHOTS);
        this.includeSnapshots.setSelected(includeSnapshotsEnabled);
        this.includeSnapshots.setOpacity(1.0);
        this.includeSnapshots.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.includeSnapshots.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");

        // Position (ajuste si besoin)
        this.includeSnapshots.setLayoutX(660);
        this.includeSnapshots.setLayoutY(165);

        this.includeSnapshots.setOnAction(e -> {
            includeSnapshotsEnabled = includeSnapshots.isSelected();
            pane.getConfig().updateValue(CFG_INCLUDE_SNAPSHOTS, includeSnapshotsEnabled);

            String preferred = this.versionList.getValue();
            populateVersionListFromMojang(pane, preferred);
        });

        root.getChildren().add(this.includeSnapshots);

        // Charge la liste depuis Mojang (releases triées comme avant + snapshots selon checkbox)
        this.populateVersionListFromMojang(pane, (String) pane.getConfig().getValue(EnumConfig.VERSION));

        this.versionList.setOnAction(event -> applyModRestrictionsForVersion(versionList.getValue(), pane));

        /* ===================== VM ARGUMENTS TEXTFIELD ===================== */
        this.vmArguments = new LauncherTextField(root);
        this.vmArguments.setText((String) pane.getConfig().getValue(EnumConfig.VM_ARGUMENTS));
        this.vmArguments.setSize(390, 20);
        this.vmArguments.setPosition(250, 425);

        /* ===================== CHECKBOX USE VM ARGUMENTS ===================== */
        this.useVMArguments = new JFXCheckBox();
        this.useVMArguments.setText(LABEL_USE_JVM_ARGUMENTS);
        Object cfgUseVmArgs = pane.getConfig().getValue(EnumConfig.USE_VM_ARGUMENTS);
        this.useVMArguments.setSelected(cfgUseVmArgs instanceof Boolean ? (Boolean) cfgUseVmArgs : false);
        this.useVMArguments.setOpacity(1.0);
        this.useVMArguments.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.useVMArguments.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        this.useVMArguments.setLayoutX(250);
        this.useVMArguments.setLayoutY(395);
        this.useVMArguments.setOnAction(event -> vmArguments.setDisable(!useVMArguments.isSelected()));
        root.getChildren().add(useVMArguments);
        this.vmArguments.setDisable(!this.useVMArguments.isSelected());

        /* ===================== CHECKBOX Discord statut ===================== */
        this.useDiscord = new JFXCheckBox();
        this.useDiscord.setText(LABEL_DISCORD_STATUS);
        Object cfgDiscord = pane.getConfig().getValue(EnumConfig.USE_DISCORD);
        this.useDiscord.setSelected(cfgDiscord instanceof Boolean ? (Boolean) cfgDiscord : false);
        this.useDiscord.setOpacity(1.0);
        this.useDiscord.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.useDiscord.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        this.useDiscord.setLayoutX(500);
        this.useDiscord.setLayoutY(335);
        this.useDiscord.setOnAction(event -> {
            if (useDiscord.isSelected()) {
                pane.getRpc().start();
            } else {
                pane.getRpc().stop();
            }
        });
        root.getChildren().add(this.useDiscord);

        /* ===================== AUTO LOGIN CHECK BOX ===================== */
        this.autoLogin = new JFXCheckBox();
        this.autoLogin.setText(LABEL_AUTO_CONNECT);
        Object cfgAutoLogin = pane.getConfig().getValue(EnumConfig.AUTOLOGIN);
        this.autoLogin.setSelected(cfgAutoLogin instanceof Boolean ? (Boolean) cfgAutoLogin : false);
        this.autoLogin.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.autoLogin.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        this.autoLogin.setLayoutX(250);
        this.autoLogin.setLayoutY(335);
        root.getChildren().add(autoLogin);

        /* ===================== CONNECT AUTO SERVER CHECK BOX ===================== */
        this.connect = new JFXCheckBox();
        this.connect.setText(LABEL_CONNECT_SERVER);
        Object cfgConnect = pane.getConfig().getValue(EnumConfig.USE_CONNECT);
        this.connect.setSelected(cfgConnect instanceof Boolean ? (Boolean) cfgConnect : false);
        this.connect.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.connect.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        this.connect.setLayoutX(250);
        this.connect.setLayoutY(465);
        this.connect.setOnAction(event -> {
            pane.getConfig().updateValue("useConnect", connect.isSelected());
            if (connect.isSelected()) {
                engine.reg(App.getGameConnect());
            }
        });
        root.getChildren().add(this.connect);

        /* ===================== MUSIC CHECK BOX ===================== */
        this.useMusic = new JFXCheckBox();
        this.useMusic.setText(LABEL_PLAY_MUSIC);
        Object cfgMusic = pane.getConfig().getValue(EnumConfig.USE_MUSIC);
        this.useMusic.setSelected(cfgMusic instanceof Boolean ? (Boolean) cfgMusic : false);
        this.useMusic.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.useMusic.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        this.useMusic.setLayoutX(250);
        this.useMusic.setLayoutY(365);
        this.useMusic.setOnAction(event -> {
            pane.getConfig().updateValue("usemusic", useMusic.isSelected());
            pane.getMediaPlayer().setMute(!useMusic.isSelected());
        });
        root.getChildren().add(this.useMusic);

        /* ===================== BOUTON D'OUVERTURE DU REPERTOIRE DU JEU  ===================== */
        JFXButton openGameDirButton = new JFXButton(BUTTON_OPEN_GAME_DIR);
        openGameDirButton.setStyle("-fx-background-color: rgba(53, 89, 119, 0.4); -fx-text-fill: white;");
        openGameDirButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        openGameDirButton.setLayoutX(60);
        openGameDirButton.setLayoutY(550);
        openGameDirButton.setOnAction(event -> openGameDirectory());
        root.getChildren().add(openGameDirButton);

        /* ===================== BOUTON DE VALIDATION ===================== */
        JFXButton saveButton = new JFXButton(BUTTON_VALIDATE);
        saveButton.setStyle("-fx-background-color: rgba(53, 89, 119, 0.4); -fx-text-fill: white;");
        saveButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        saveButton.setLayoutX(740);
        saveButton.setLayoutY(550);
        saveButton.setOnAction(event -> {
            HashMap<String, String> configMap = new HashMap<String, String>();
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

            // NOUVEAU : sauvegarde de l'option snapshots
            configMap.put(CFG_INCLUDE_SNAPSHOTS, "" + includeSnapshots.isSelected());

            pane.getConfig().updateValues(configMap);

            engine.reg(GameMemory.getMemory(Double.parseDouble((String) pane.getConfig().getValue(EnumConfig.RAM))));
            engine.reg(GameSize.getWindowSize(Integer.parseInt((String) pane.getConfig().getValue(EnumConfig.GAME_SIZE))));

            String selectedVersion = String.valueOf(pane.getConfig().getValue(EnumConfig.VERSION));
            GameLinks links = buildGameLinks(selectedVersion, useForge.isSelected(), useOptifine.isSelected());
            engine.reg(links);

            Utils.regGameStyle(engine, pane.getConfig());

            final ZoomOutDown animation = new ZoomOutDown(root);
            animation.setOnFinished(actionEvent -> {
                Stage stage = (Stage) ((JFXButton) event.getSource()).getScene().getWindow();
                stage.close();
            });
            animation.setResetOnFinished(true);
            animation.play();
        });
        root.getChildren().add(saveButton);
    }

    /**
     * Si Forge/Optifine sélectionné : on garde ton serveur distant.
     * Sinon : on utilise le JSON officiel Mojang (vanilla release/snapshot).
     */
    private GameLinks buildGameLinks(String version, boolean forge, boolean optifine) {
        if (forge || optifine) {
            return new GameLinks("https://majestycraft.com/minecraft" + urlModifier(), version + ".json");
        }

        String fullUrl = mojangVersionUrlById.get(version);
        if (fullUrl == null || fullUrl.trim().isEmpty()) {
            // Fallback sécurité : si la map n'est pas chargée / version inconnue
            return new GameLinks("https://majestycraft.com/minecraft" + urlModifier(), version + ".json");
        }

        int idx = fullUrl.lastIndexOf('/');
        if (idx <= 0 || idx >= fullUrl.length() - 1) {
            return new GameLinks("https://majestycraft.com/minecraft" + urlModifier(), version + ".json");
        }

        String base = fullUrl.substring(0, idx + 1);
        String file = fullUrl.substring(idx + 1);
        return new GameLinks(base, file);
    }

    private String urlModifier() {
        return "/" + versionList.getValue() + (useForge.isSelected() ? "/forge/" : "/");
    }

    // ===================== Versions Mojang (releases triées + snapshots selon checkbox) =====================

    private void populateVersionListFromMojang(final LauncherPanel pane) {
        populateVersionListFromMojang(pane, null);
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
                    // fallback : liste d'origine (releases uniquement)
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

                // 1) On tente de conserver la sélection préférée (utile lors du toggle)
                if (preferredSelection != null && finalVersions.contains(preferredSelection)) {
                    this.versionList.setValue(preferredSelection);
                } else {
                    // 2) Sinon la version sauvée (si elle existe)
                    String saved = (String) pane.getConfig().getValue(EnumConfig.VERSION);
                    if (saved != null && finalVersions.contains(saved)) {
                        this.versionList.setValue(saved);
                    } else if (!finalVersions.isEmpty()) {
                        this.versionList.setValue(finalVersions.get(0));
                    }
                }

                applyModRestrictionsForVersion(this.versionList.getValue(), pane);
            });
        }, "MajestyLauncher-MojangVersions");

        loader.setDaemon(true);
        loader.start();
    }

    /**
     * Releases + snapshots dans l'ordre du manifest Mojang
     * => plus récent -> plus ancien, snapshots intercalés selon leur date.
     *
     * Releases : on garde uniquement celles présentes dans ta liste VANILLA_SUPPORTED_RELEASE_SET
     * Snapshots : on en prend maxSnapshots (si includeSnapshots=true).
     */
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
            String url = o.get("url").getAsString();

            if ("release".equalsIgnoreCase(type)) {
                // On garde UNIQUEMENT les releases que tu supportes (ta liste historique)
                if (!VANILLA_SUPPORTED_RELEASE_SET.contains(id)) continue;

                out.add(id);
                mojangVersionUrlById.put(id, url);
                mojangVersionTypeById.put(id, "release");
                continue;
            }

            if ("snapshot".equalsIgnoreCase(type)) {
                if (!includeSnapshots) continue;
                if (snapshotCount >= maxSnapshots) continue;

                snapshotCount++;
                out.add(id);
                mojangVersionUrlById.put(id, url);
                mojangVersionTypeById.put(id, "snapshot");
            }
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
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                if (code < 200 || code >= 300) {
                    throw new IOException("HTTP " + code + " -> " + sb);
                }
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

    // ===================== Restrictions Forge/Optifine (via "versions supportées") =====================

    private void applyModRestrictionsForVersion(String version, LauncherPanel pane) {
        if (version == null || version.trim().isEmpty() || "Chargement...".equals(version)) return;

        boolean snapshot = isSnapshot(version);

        // On procède "à l'envers" : on autorise uniquement si la version est dans la liste supportée.
        boolean forgeAllowed = !snapshot && FORGE_SUPPORTED_VERSIONS.contains(version);
        boolean optifineAllowed = !snapshot && OPTIFINE_SUPPORTED_VERSIONS.contains(version);

        boolean forgeRestricted = !forgeAllowed;
        boolean optifineRestricted = !optifineAllowed;

        // Forge
        if (forgeRestricted) {
            useForge.setSelected(false);
            pane.getConfig().updateValue("useForge", false);
        }
        useForge.setDisable(forgeRestricted);
        useForge.setOpacity(forgeRestricted ? 0.3 : 1.0);

        // Optifine
        if (optifineRestricted) {
            useOptifine.setSelected(false);
            pane.getConfig().updateValue("useOptifine", false);
        }
        useOptifine.setDisable(optifineRestricted);
        useOptifine.setOpacity(optifineRestricted ? 0.3 : 1.0);
    }

    // ===================== Taille fenêtre =====================

    private void populateSizeList() {
        for (GameSize size : GameSize.values()) {
            this.windowsSizeList.getItems().add(size.getDesc());
        }
    }

    private void languageList() {
        String[] language = new String[] { "Français", "English", "Español", };
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
