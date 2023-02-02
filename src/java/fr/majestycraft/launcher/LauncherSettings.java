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
import javafx.scene.layout.*;
import javafx.stage.*;

import java.util.*;

public class LauncherSettings extends IScreen {

    private final LauncherLabel memorySliderLabel;
    private JFXSlider memorySlider;
    private final JFXComboBox<String> windowsSizeList;
    private final JFXComboBox<String> versionList;
    private final JFXCheckBox autoLogin;
    private final JFXCheckBox connect;
    private static JFXCheckBox useForge;
    private static JFXCheckBox useOptifine;
    private final JFXCheckBox useDiscord;
    private final JFXCheckBox useMusic;
    private final JFXCheckBox useVMArguments;
    private final LauncherTextField vmArguments;
    private double xOffSet; // Position x à l'instant du clic
    private double yOffSet; // Position y à l'instant du clic
    Stage stage; // Le stage qu'on voudra faire bouger (ici notre menu des paramètres)

    public LauncherSettings(final Pane root, final GameEngine engine, final LauncherPanel pane) {

        // String version = (String) pane.config.getValue(EnumConfig.VERSION);
        /* ===================== BOUGER LE MENU PARAMETRE ===================== */
        // Cet évent nous permet de récupérer les valeurs en x et en y initiales.
        root.setOnMousePressed(event -> {
            xOffSet = event.getSceneX();
            yOffSet = event.getSceneY();
        });
        // Cet évent s'occupe de faire bouger le menu
        root.setOnMouseDragged(event -> {
            stage = (Stage) memorySlider.getScene().getWindow(); // On get le stage du menu des paramètres
            stage.setX(event.getScreenX() - xOffSet); // On donne la nouvelle position en x
            stage.setY(event.getScreenY() - yOffSet); // On donne la nouvelle postion en y
        });

        this.drawBackgroundImage(engine, root, "background.png");
        pane.getConfig().loadConfiguration();

        /* ===================== RECTANGLE NOIR EN HAUT ===================== */
        LauncherRectangle topRectangle = new LauncherRectangle(root, 0, 0, 1500, 15);
        topRectangle.setOpacity(0.7);

        /* ===================== LABEL TITRE ===================== */
        LauncherLabel titleLabel = new LauncherLabel(root);
        titleLabel.setText("PARAMETRES");
        titleLabel.setStyle("-fx-text-fill: white;");
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        titleLabel.setPosition(350, 20);
        titleLabel.setSize(230, 35);

        /* ===================== MC SIZE LABEL ===================== */
        LauncherLabel windowsSizeLabel = new LauncherLabel(root);
        windowsSizeLabel.setText("Taille de la fenêtre:");
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

        /* ===================== SLIDER RAM LABEL ===================== */
        LauncherLabel sliderLabel = new LauncherLabel(root);
        sliderLabel.setText("RAM Allouée:");
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
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                root.getChildren().add(memorySlider);
            }
        });

        this.memorySliderLabel.setText(this.memorySlider.getValue() + "Gb");

        /* ===================== CHECKBOX USE Optifine ===================== */
        useOptifine = new JFXCheckBox();
        useOptifine.setText("Optifine");
        useOptifine.setSelected((boolean) pane.getConfig().getValue(EnumConfig.USE_OPTIFINE));
        useOptifine.setOpacity(1.0);
        useOptifine.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        useOptifine.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        useOptifine.setLayoutX(500);
        useOptifine.setLayoutY(305);
        useOptifine.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                useForge.setSelected(false);
            }
        });
        root.getChildren().add(useOptifine);

        /* ===================== CHECKBOX USE Forge ===================== */
        useForge = new JFXCheckBox();
        useForge.setText("Forge");
        useForge.setSelected((boolean) pane.getConfig().getValue(EnumConfig.USE_FORGE));
        useForge.setOpacity(1.0);
        useForge.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        useForge.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        useForge.setLayoutX(250);
        useForge.setLayoutY(305);
        useForge.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                LauncherSettings.useOptifine.setSelected(false);
            }
        });
        root.getChildren().add(useForge);

        /* ===================== MC VERSION LABEL ===================== */
        LauncherLabel versionListLabel = new LauncherLabel(root);
        versionListLabel.setText("Choix de la version:");
        versionListLabel.setOpacity(1.0);
        versionListLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        versionListLabel.setStyle("-fx-text-fill: white;");
        versionListLabel.setSize(370, 30);
        versionListLabel.setPosition(250, 160);

        /* ===================== MC VERSION LIST ===================== */
        this.versionList = new JFXComboBox<>();
        this.populateVersionList();
        this.versionList.setValue((String) pane.getConfig().getValue(EnumConfig.VERSION));
        List<String> disabledForgeVersions = Arrays.asList("1.8", "1.19.1", "1.19.2", "1.19.3");
        List<String> disabledOptifineVersions = Arrays.asList("1.8", "1.19.3");

        if (pane.getConfig().getValue(EnumConfig.VERSION) != null) {
            String verif = (String) pane.getConfig().getValue(EnumConfig.VERSION);
            this.versionList.setValue(verif);

            if (disabledForgeVersions.contains(verif)) {
                LauncherSettings.useForge.setDisable(true);
                LauncherSettings.useForge.setSelected(false);
                LauncherSettings.useForge.setOpacity(0.3);
                pane.getConfig().updateValue("useForge", false);
            }

            if (disabledOptifineVersions.contains(verif)) {
                LauncherSettings.useOptifine.setDisable(true);
                LauncherSettings.useOptifine.setSelected(false);
                LauncherSettings.useOptifine.setOpacity(0.3);
                pane.getConfig().updateValue("useOptifine", false);
            }
        }
        this.versionList.setPrefSize(150, 20);
        this.versionList.setLayoutX(490);
        this.versionList.setLayoutY(165);
        this.versionList.setVisibleRowCount(10);
        this.versionList.setOnAction(event -> {
            String version = versionList.getValue();
            boolean isForgeRestricted = "1.8".equals(version) || "1.19.1".equals(version) || "1.19.2".equals(version) || "1.19.3".equals(version);
            boolean isOptifineRestricted = "1.8".equals(version) || "1.19.3".equals(version);

            if (!LauncherSettings.useForge.isDisabled() || isForgeRestricted) {
                LauncherSettings.useForge.setSelected(false);
                pane.getConfig().updateValue("useForge", false);
            }

            LauncherSettings.useForge.setDisable(isForgeRestricted);
            LauncherSettings.useForge.setOpacity(isForgeRestricted ? 0.3 : 1);

            if (!LauncherSettings.useOptifine.isDisabled() || isOptifineRestricted) {
                LauncherSettings.useOptifine.setSelected(false);
                pane.getConfig().updateValue("useOptifine", false);
            }

            LauncherSettings.useOptifine.setDisable(isOptifineRestricted);
            LauncherSettings.useOptifine.setOpacity(isOptifineRestricted ? 0.3 : 1);
        });
        root.getChildren().add(this.versionList);

        /* ===================== VM ARGUMENTS TEXTFIELD ===================== */
        this.vmArguments = new LauncherTextField(root);
        this.vmArguments.setText((String) pane.getConfig().getValue(EnumConfig.VM_ARGUMENTS));
        this.vmArguments.setSize(390, 20);
        this.vmArguments.setPosition(250, 425);

        /* ===================== CHECKBOX USE VM ARGUMENTS ===================== */
        this.useVMArguments = new JFXCheckBox();
        this.useVMArguments.setText("Utiliser les Arguments JVM");
        this.useVMArguments.setSelected((Boolean) pane.getConfig().getValue(EnumConfig.USE_VM_ARGUMENTS));
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
        this.useDiscord.setText("Statut discord");
        this.useDiscord.setSelected((Boolean) pane.getConfig().getValue(EnumConfig.USE_DISCORD));
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
        this.autoLogin.setText("Connexion automatique");
        this.autoLogin.setSelected((Boolean) pane.getConfig().getValue(EnumConfig.AUTOLOGIN));
        this.autoLogin.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.autoLogin.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        this.autoLogin.setLayoutX(250);
        this.autoLogin.setLayoutY(335);
        root.getChildren().add(autoLogin);

        /* ===================== MICROSOFT CHECK BOX ===================== */
        JFXCheckBox microsoft = new JFXCheckBox();
        microsoft.setText("Connexion auto MICROSOFT (desactiver si 'session invalide')");
        microsoft.setSelected((Boolean) pane.getConfig().getValue(EnumConfig.USE_MICROSOFT));
        microsoft.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        microsoft.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        microsoft.setLayoutX(250);
        microsoft.setLayoutY(465);
        microsoft.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
				if (!microsoft.isSelected()) {
					pane.getConfig().updateValue("useMicrosoft", false);
					pane.getConfig().updateValue("autologin", false);
				}
            }
        });
        root.getChildren().add(microsoft);

        /* ===================== MICROSOFT CHECK BOX ===================== */
        this.connect = new JFXCheckBox();
        this.connect.setText("Connexion auto au serveur MajestyCraft (EXPERIMENTAL)");
        this.connect.setSelected((Boolean) pane.getConfig().getValue(EnumConfig.USE_CONNECT));
        this.connect.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.connect.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        this.connect.setLayoutX(250);
        this.connect.setLayoutY(495);
        this.connect.setOnAction(event -> {
            if (connect.isSelected()) {
                engine.reg(App.GAME_CONNECT);
            } else {
                engine.reg((GameConnect) null);
            }

        });
        root.getChildren().add(this.connect);

        /* ===================== AUTO LOGIN CHECK BOX ===================== */
        this.useMusic = new JFXCheckBox();
        this.useMusic.setText("Jouer la musique");
        this.useMusic.setSelected((Boolean) pane.getConfig().getValue(EnumConfig.USE_MUSIC));
        this.useMusic.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.useMusic.setStyle("-fx-text-fill: white; -jfx-checked-color: RED; -jfx-unchecked-color: BLACK");
        this.useMusic.setLayoutX(250);
        this.useMusic.setLayoutY(365);
        this.useMusic.setOnAction(event -> {
            pane.getConfig().updateValue("usemusic", useMusic.isSelected());
            if (useMusic.isSelected()) {
                pane.getMediaPlayer().setMute(false);
            } else {
                pane.getMediaPlayer().setMute(true);
            }
        });
        root.getChildren().add(this.useMusic);

        /* ===================== BOUTON DE VALIDATION ===================== */
        JFXButton saveButton = new JFXButton("Valider");
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
            configMap.put("useforge", "" + useForge.isSelected());
            configMap.put("useOptifine", "" + useOptifine.isSelected());
            configMap.put("usemusic", "" + useMusic.isSelected());
            configMap.put("usediscord", "" + useDiscord.isSelected());
            configMap.put(EnumConfig.USE_CONNECT.getOption(), "" + connect.isSelected());
            pane.getConfig().updateValues(configMap);
            engine.reg(GameMemory.getMemory(Double.parseDouble((String) pane.getConfig().getValue(EnumConfig.RAM))));
            engine.reg(GameSize.getWindowSize(Integer.parseInt((String) pane.getConfig().getValue(EnumConfig.GAME_SIZE))));
            GameLinks links = new GameLinks("https://majestycraft.com/minecraft" + urlModifier(), pane.getConfig().getValue(EnumConfig.VERSION) + ".json");
            engine.reg(links);
            Utils.regGameStyle(engine,pane.getConfig());
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



    private String urlModifier() {
        return "/" + versionList.getValue() + (useForge.isSelected() ? "/forge/" : (useOptifine.isSelected() ? "/" : "/"));
    }

    private void populateSizeList() {
        for (GameSize size : GameSize.values()) {
            this.windowsSizeList.getItems().add(size.getDesc());
        }
    }

    private void populateVersionList() {
        String[] versions = new String[] {
            "1.8", "1.9", "1.10.2", "1.11.2", "1.12.2", "1.13.2", "1.14.4", "1.15.2",
            "1.16.2", "1.16.3", "1.16.4", "1.16.5", "1.17", "1.17.1", "1.18", "1.18.1",
            "1.18.2", "1.19", "1.19.1", "1.19.2", "1.19.3"
        };
        this.versionList.getItems().addAll(Arrays.asList(versions));
    }
}
