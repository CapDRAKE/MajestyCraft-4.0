package fr.majestycraft.launcher;

import animatefx.animation.ZoomOutDown;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import fr.trxyy.alternative.alternative_api.GameEngine;
import fr.trxyy.alternative.alternative_api.utils.FontLoader;
import fr.trxyy.alternative.alternative_api.utils.config.EnumConfig;
import fr.trxyy.alternative.alternative_api_ui.base.IScreen;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherButton;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherImage;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherLabel;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherRectangle;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LauncherShaders extends IScreen {

    private static final int W = 1240;
    private static final int H = 840;

    private static final String MODRINTH_API_SEARCH = "https://api.modrinth.com/v2/search";
    private static final String MODRINTH_API_PROJECT_VERSIONS = "https://api.modrinth.com/v2/project/";
    private static final String MODRINTH_WEB_SHADER = "https://modrinth.com/shader/";
    private static final String MODRINTH_USER_AGENT = "CapDRAKE/MajestyLauncher/4.0 (majestycraft.com)";

    private final LauncherPanel paneRef;
    private final DecimalFormat decimalFormat = new DecimalFormat("0.0");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);
    private final List<LocalShaderItem> shaderItems = new ArrayList<LocalShaderItem>();
    private final Image fallbackIcon = new Image(getClass().getResource("/resources/shaderpacks.png").toExternalForm());
    private final Map<String, Image> remoteImageCache = new ConcurrentHashMap<String, Image>();
    private final Map<String, String> projectPreviewUrlCache = new ConcurrentHashMap<String, String>();

    private ListView<LocalShaderItem> localListView;
    private ListView<OnlineShaderItem> onlineListView;
    private Pane localPane;
    private Pane onlinePane;
    private JFXTextField localFilterField;
    private JFXTextField onlineSearchField;
    private JFXTextField versionField;
    private JFXButton localTabButton;
    private JFXButton onlineTabButton;
    private JFXButton localFilterButton;
    private JFXButton onlineSearchButton;
    private JFXButton versionApplyButton;
    private JFXButton addButton;
    private JFXButton openFolderButton;
    private JFXButton refreshButton;
    private LauncherButton quitButton;
    private LauncherLabel localStatusLabel;
    private LauncherLabel onlineStatusLabel;
    private LauncherLabel versionCaptionLabel;

    private LauncherImage heroLogo;
    private LauncherLabel heroTitle;
    private LauncherLabel heroSubtitle;
    private LauncherLabel heroLine1;
    private LauncherLabel heroLine2;
    private LauncherLabel heroLine3;
    private LauncherLabel heroLine4;

    private File shaderpacksDir;
    private double xOffSet;
    private double yOffSet;

    public LauncherShaders(final Pane root, final GameEngine engine, final LauncherPanel pane) {
        this.paneRef = pane;

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (root.getScene() != null && !root.getScene().getStylesheets().contains("css/design.css")) {
                    root.getScene().getStylesheets().add("css/design.css");
                }
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

        installResponsiveBackground(root, engine, "background.png");

        Rectangle overlay = new Rectangle();
        overlay.widthProperty().bind(root.widthProperty());
        overlay.heightProperty().bind(root.heightProperty());
        overlay.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 0, 0, 0.18)),
                new Stop(1, Color.rgb(0, 0, 0, 0.75))
        ));
        root.getChildren().add(overlay);

        Rectangle leftDock = new Rectangle();
        leftDock.setWidth(84);
        leftDock.heightProperty().bind(root.heightProperty());
        leftDock.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(8, 12, 18, 0.84)),
                new Stop(1, Color.rgb(8, 12, 18, 0.28))
        ));
        root.getChildren().add(leftDock);

        Rectangle separator = new Rectangle();
        separator.setX(83);
        separator.setWidth(1);
        separator.heightProperty().bind(root.heightProperty());
        separator.setFill(Color.rgb(255, 255, 255, 0.08));
        root.getChildren().add(separator);

        buildHero(root, engine);
        buildMain(root);

        this.shaderpacksDir = resolveShaderpacksDir();
        ensureShaderpacksDir();
        configureLocalList();
        configureOnlineList();
        loadShaders();
        setDiscoverMode(false);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                versionField.setText(getSelectedMinecraftVersion());
                searchOnlineShaders("", getSelectedMinecraftVersion());

                animateIn(heroLogo, -14, 10, 60);
                animateIn(heroTitle, -14, 10, 110);
                animateIn(heroSubtitle, -14, 10, 160);
                animateIn(heroLine1, -14, 10, 210);
                animateIn(heroLine2, -14, 10, 250);
                animateIn(heroLine3, -14, 10, 290);
                animateIn(heroLine4, -14, 10, 330);

                animateIn(localTabButton, 18, 0, 140);
                animateIn(onlineTabButton, 18, 0, 180);
                animateIn(localFilterField, 18, 0, 220);
                animateIn(localFilterButton, 18, 0, 260);
                animateIn(onlineSearchField, 18, 0, 220);
                animateIn(onlineSearchButton, 18, 0, 260);
                animateIn(versionCaptionLabel, 18, 0, 300);
                animateIn(versionField, 18, 0, 340);
                animateIn(versionApplyButton, 18, 0, 380);
                animateIn(refreshButton, 18, 0, 220);
                animateIn(localPane, 18, 0, 420);
                animateIn(onlinePane, 18, 0, 420);
                animateIn(addButton, 0, 12, 460);
                animateIn(openFolderButton, 0, 12, 500);
                animateIn(quitButton, 0, 12, 540);
            }
        });
    }

    private void buildHero(Pane root, GameEngine engine) {
        final int heroX = 120;
        final int heroY = 70;
        final int heroW = 250;
        final int heroH = 640;

        LauncherRectangle heroCard = new LauncherRectangle(root, heroX, heroY, heroW, heroH);
        styleCard(heroCard, 0.70);

        this.heroLogo = new LauncherImage(root);
        this.heroLogo.setImage(getResourceLocation().loadImage(engine, "shaderpacks.png"));
        this.heroLogo.setSize(130, 130);
        this.heroLogo.setBounds(heroX + 60, heroY + 36, 130, 130);

        this.heroTitle = new LauncherLabel(root);
        this.heroTitle.setText("Shaders");
        this.heroTitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 27F));
        this.heroTitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.96)");
        this.heroTitle.setPosition(heroX, heroY + 186);
        this.heroTitle.setSize(heroW, 34);
        this.heroTitle.setAlignment(Pos.CENTER);

        this.heroSubtitle = new LauncherLabel(root);
        this.heroSubtitle.setText("Gestion locale + catalogue en ligne");
        this.heroSubtitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        this.heroSubtitle.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffb400");
        this.heroSubtitle.setPosition(heroX + 10, heroY + 226);
        this.heroSubtitle.setSize(heroW - 20, 22);
        this.heroSubtitle.setAlignment(Pos.CENTER);

        this.heroLine1 = createHeroLine(root, heroX + 22, heroY + 302, "• Liste les shaderpacks installés");
        this.heroLine2 = createHeroLine(root, heroX + 22, heroY + 332, "• Ajoute des .zip ou dossiers");
        this.heroLine3 = createHeroLine(root, heroX + 22, heroY + 362, "• Recherche et télécharge en ligne");
        this.heroLine4 = createHeroLine(root, heroX + 22, heroY + 392, "• Active, désactive ou supprime");
    }

    private LauncherLabel createHeroLine(Pane root, int x, int y, String text) {
        LauncherLabel label = new LauncherLabel(root);
        label.setText(text);
        label.setFont(FontLoader.loadFont("Poppins-Regular.ttf", "Poppins", 13F));
        label.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.86)");
        label.setPosition(x, y);
        label.setSize(204, 22);
        return label;
    }

    private void buildMain(Pane root) {
        final int mainX = 392;
        final int mainY = 52;
        final int mainW = 770;
        final int mainH = 690;

        LauncherRectangle mainCard = new LauncherRectangle(root, mainX, mainY, mainW, mainH);
        styleCard(mainCard, 0.64);

        LauncherLabel title = new LauncherLabel(root);
        title.setText("Gestionnaire de shaderpacks");
        title.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 29F));
        title.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.98)");
        title.setPosition(mainX + 118, mainY + 26);
        title.setSize(mainW - 236, 38);
        title.setAlignment(Pos.CENTER);

        LauncherLabel subtitle = new LauncherLabel(root);
        subtitle.setText("Ajoute, explore et gère tes shaders pour OptiFine / Iris");
        subtitle.setFont(FontLoader.loadFont("Poppins-Regular.ttf", "Poppins", 13F));
        subtitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.52)");
        subtitle.setPosition(mainX + 118, mainY + 64);
        subtitle.setSize(mainW - 236, 20);
        subtitle.setAlignment(Pos.CENTER);

        this.localTabButton = new JFXButton("Installés");
        styleTabButton(this.localTabButton, true);
        this.localTabButton.setLayoutX(mainX + 34);
        this.localTabButton.setLayoutY(mainY + 96);
        this.localTabButton.setPrefWidth(116);
        this.localTabButton.setOnAction(event -> setDiscoverMode(false));
        root.getChildren().add(this.localTabButton);

        this.onlineTabButton = new JFXButton("Explorer");
        styleTabButton(this.onlineTabButton, true);
        this.onlineTabButton.setLayoutX(mainX + 160);
        this.onlineTabButton.setLayoutY(mainY + 96);
        this.onlineTabButton.setPrefWidth(116);
        this.onlineTabButton.setOnAction(event -> setDiscoverMode(true));
        root.getChildren().add(this.onlineTabButton);

        this.localFilterField = new JFXTextField();
        this.localFilterField.setPromptText("Filtrer les shaderpacks installés...");
        styleSearchField(this.localFilterField);
        this.localFilterField.setLayoutX(mainX + 410);
        this.localFilterField.setLayoutY(mainY + 98);
        this.localFilterField.setPrefWidth(210);
        this.localFilterField.setPrefHeight(34);
        root.getChildren().add(this.localFilterField);

        this.localFilterButton = createGhostButton("Filtrer", 96, 34);
        this.localFilterButton.setLayoutX(mainX + 640);
        this.localFilterButton.setLayoutY(mainY + 98);
        this.localFilterButton.setOnAction(e -> applyLocalFilter());
        root.getChildren().add(this.localFilterButton);

        this.onlineSearchField = new JFXTextField();
        this.onlineSearchField.setPromptText("Rechercher un shader...");
        styleSearchField(this.onlineSearchField);
        this.onlineSearchField.setLayoutX(mainX + 410);
        this.onlineSearchField.setLayoutY(mainY + 98);
        this.onlineSearchField.setPrefWidth(210);
        this.onlineSearchField.setPrefHeight(34);
        root.getChildren().add(this.onlineSearchField);

        this.onlineSearchButton = createGhostButton("Rechercher", 112, 34);
        this.onlineSearchButton.setLayoutX(mainX + 640);
        this.onlineSearchButton.setLayoutY(mainY + 98);
        this.onlineSearchButton.setOnAction(e -> searchOnlineShaders(onlineSearchField.getText(), getChosenMinecraftVersion()));
        root.getChildren().add(this.onlineSearchButton);

        this.refreshButton = createGhostButton("Actualiser", 112, 34);
        this.refreshButton.setLayoutX(mainX + 34);
        this.refreshButton.setLayoutY(mainY + 146);
        this.refreshButton.setOnAction(e -> {
            if (isDiscoverMode()) {
                searchOnlineShaders(onlineSearchField.getText(), getChosenMinecraftVersion());
            } else {
                loadShaders();
            }
        });
        root.getChildren().add(this.refreshButton);

        this.versionCaptionLabel = new LauncherLabel(root);
        this.versionCaptionLabel.setText("Version Minecraft");
        this.versionCaptionLabel.setFont(FontLoader.loadFont("Poppins-Regular.ttf", "Poppins", 12F));
        this.versionCaptionLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffb400");
        this.versionCaptionLabel.setPosition(mainX + 390, mainY + 144);
        this.versionCaptionLabel.setSize(120, 18);

        this.versionField = new JFXTextField();
        styleSearchField(this.versionField);
        this.versionField.setPromptText("1.21.11");
        this.versionField.setLayoutX(mainX + 516);
        this.versionField.setLayoutY(mainY + 140);
        this.versionField.setPrefWidth(100);
        this.versionField.setPrefHeight(32);
        this.versionField.setOnAction(e -> applyVersionChange());
        root.getChildren().add(this.versionField);

        this.versionApplyButton = createGhostButton("Valider", 94, 32);
        this.versionApplyButton.setLayoutX(mainX + 626);
        this.versionApplyButton.setLayoutY(mainY + 140);
        this.versionApplyButton.setOnAction(e -> applyVersionChange());
        root.getChildren().add(this.versionApplyButton);

        this.localPane = new Pane();
        this.localPane.setLayoutX(mainX + 24);
        this.localPane.setLayoutY(mainY + 190);
        this.localPane.setPrefSize(mainW - 48, 394);
        root.getChildren().add(this.localPane);

        LauncherRectangle localBg = new LauncherRectangle(this.localPane, 0, 0, mainW - 48, 394);
        styleCard(localBg, 0.28);

        this.localListView = new ListView<LocalShaderItem>();
        this.localListView.setLayoutX(12);
        this.localListView.setLayoutY(12);
        this.localListView.setPrefSize(mainW - 72, 370);
        this.localListView.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-control-inner-background: transparent;" +
                "-fx-background-insets: 0;" +
                "-fx-padding: 6;"
        );
        this.localPane.getChildren().add(this.localListView);

        this.onlinePane = new Pane();
        this.onlinePane.setLayoutX(mainX + 24);
        this.onlinePane.setLayoutY(mainY + 190);
        this.onlinePane.setPrefSize(mainW - 48, 394);
        root.getChildren().add(this.onlinePane);

        LauncherRectangle onlineBg = new LauncherRectangle(this.onlinePane, 0, 0, mainW - 48, 394);
        styleCard(onlineBg, 0.28);

        this.onlineListView = new ListView<OnlineShaderItem>();
        this.onlineListView.setLayoutX(12);
        this.onlineListView.setLayoutY(12);
        this.onlineListView.setPrefSize(mainW - 72, 370);
        this.onlineListView.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-control-inner-background: transparent;" +
                "-fx-background-insets: 0;" +
                "-fx-padding: 6;"
        );
        this.onlinePane.getChildren().add(this.onlineListView);

        this.localStatusLabel = new LauncherLabel(root);
        this.localStatusLabel.setFont(FontLoader.loadFont("Poppins-Regular.ttf", "Poppins", 12F));
        this.localStatusLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.58)");
        this.localStatusLabel.setPosition(mainX + 34, mainY + mainH - 80);
        this.localStatusLabel.setSize(mainW - 320, 18);

        this.onlineStatusLabel = new LauncherLabel(root);
        this.onlineStatusLabel.setFont(FontLoader.loadFont("Poppins-Regular.ttf", "Poppins", 12F));
        this.onlineStatusLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.58)");
        this.onlineStatusLabel.setPosition(mainX + 34, mainY + mainH - 80);
        this.onlineStatusLabel.setSize(mainW - 320, 18);

        this.addButton = createGhostButton("Ajouter shader / zip", 166, 40);
        this.addButton.setLayoutX(mainX + 34);
        this.addButton.setLayoutY(mainY + mainH - 46);
        this.addButton.setOnAction(e -> importShaderpack());
        root.getChildren().add(this.addButton);

        this.openFolderButton = createGhostButton("Ouvrir dossier", 148, 40);
        this.openFolderButton.setLayoutX(mainX + 212);
        this.openFolderButton.setLayoutY(mainY + mainH - 46);
        this.openFolderButton.setOnAction(e -> openShaderpacksFolder());
        root.getChildren().add(this.openFolderButton);

        this.quitButton = new LauncherButton(root);
        styleSidebarFooterButton(this.quitButton, true);
        this.quitButton.setText("Retour");
        this.quitButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 18F));
        this.quitButton.setPosition(mainX + mainW - 150, mainY + mainH - 46);
        this.quitButton.setSize(112, 40);
        this.quitButton.setOnAction(event -> {
            if (root.getScene() != null && root.getScene().getWindow() != null) {
                new ZoomOutDown(root).setSpeed(1.2).play();
                root.getScene().getWindow().hide();
            }
        });
    }

    private void configureLocalList() {
        this.localListView.setCellFactory(param -> new ListCell<LocalShaderItem>() {
            @Override
            protected void updateItem(LocalShaderItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                VBox textBox = new VBox(2);

                Label title = new Label(item.displayName);
                title.setStyle("-fx-text-fill: rgba(255,255,255,0.96); -fx-font-size: 15px; -fx-font-family: 'Comfortaa';");

                Label meta = new Label(item.typeLabel + " • " + formatSize(item.size) + " • " + dateFormat.format(item.lastModified));
                meta.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 11px; -fx-font-family: 'Poppins';");

                Label state = new Label(item.enabled ? "Actif" : "Désactivé");
                state.setStyle(item.enabled
                        ? "-fx-text-fill: #ffb400; -fx-font-size: 12px; -fx-font-family: 'Poppins';"
                        : "-fx-text-fill: rgba(255,255,255,0.42); -fx-font-size: 12px; -fx-font-family: 'Poppins';");

                textBox.getChildren().addAll(title, meta, state);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                JFXButton toggleButton = createCellButton(item.enabled ? "Désactiver" : "Activer", false);
                toggleButton.setOnAction(e -> toggleShader(item));

                JFXButton deleteButton = createCellButton("Supprimer", true);
                deleteButton.setOnAction(e -> deleteShader(item));

                HBox actions = new HBox(8, toggleButton, deleteButton);
                actions.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(14, textBox, actions);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPrefWidth(660);
                row.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.04);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(255,255,255,0.05);" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 12 14 12 14;"
                );

                setGraphic(row);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 6 6 6 6;");
            }
        });
    }

    private void configureOnlineList() {
        this.onlineListView.setCellFactory(param -> new ListCell<OnlineShaderItem>() {
            @Override
            protected void updateItem(final OnlineShaderItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                ImageView iconView = createOnlinePreview(item);

                VBox textBox = new VBox(3);

                Label title = new Label(item.title);
                title.setStyle("-fx-text-fill: rgba(255,255,255,0.96); -fx-font-size: 15px; -fx-font-family: 'Comfortaa';");
                title.setWrapText(true);

                Label author = new Label("par " + item.author);
                author.setStyle("-fx-text-fill: #ffb400; -fx-font-size: 12px; -fx-font-family: 'Poppins';");

                Label desc = new Label(item.description == null || item.description.trim().isEmpty() ? "Aucune description." : item.description);
                desc.setWrapText(true);
                desc.setMaxWidth(430);
                desc.setStyle("-fx-text-fill: rgba(255,255,255,0.80); -fx-font-size: 12px; -fx-font-family: 'Poppins';");

                Label meta = new Label(item.categoriesLabel + " • " + item.downloads + " téléchargements");
                meta.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11px; -fx-font-family: 'Poppins';");

                textBox.getChildren().addAll(title, author, desc, meta);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                JFXButton pageButton = createCellButton("Page", false);
                pageButton.setPrefSize(78, 32);
                pageButton.setMinSize(78, 32);
                pageButton.setMaxSize(78, 32);
                pageButton.setOnAction(e -> openProjectPage(item));

                JFXButton downloadButton = createCellButton("Télécharger", false);
                downloadButton.setPrefSize(118, 32);
                downloadButton.setMinSize(118, 32);
                downloadButton.setMaxSize(118, 32);
                downloadButton.setOnAction(e -> downloadOnlineShader(item));

                VBox actions = new VBox(8, pageButton, downloadButton);
                actions.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(12, iconView, textBox, actions);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPrefWidth(660);
                row.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.04);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(255,255,255,0.05);" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 12 14 12 14;"
                );

                setGraphic(row);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 6 6 6 6;");
            }
        });
    }

    private void loadShaders() {
        ensureShaderpacksDir();
        this.shaderItems.clear();

        File[] files = this.shaderpacksDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (isVisibleShaderpack(file)) {
                    this.shaderItems.add(new LocalShaderItem(file));
                }
            }
        }

        Collections.sort(this.shaderItems, Comparator.comparing((LocalShaderItem i) -> i.enabled).reversed().thenComparing(i -> i.displayName.toLowerCase(Locale.ROOT)));
        this.localListView.getItems().setAll(this.shaderItems);
        updateLocalStatus(this.shaderItems.size());
    }

    private void applyLocalFilter() {
        String query = localFilterField.getText() == null ? "" : localFilterField.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            this.localListView.getItems().setAll(this.shaderItems);
            updateLocalStatus(this.shaderItems.size());
            return;
        }

        List<LocalShaderItem> filtered = new ArrayList<LocalShaderItem>();
        for (LocalShaderItem item : this.shaderItems) {
            if (item.displayName.toLowerCase(Locale.ROOT).contains(query) || item.file.getName().toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(item);
            }
        }
        this.localListView.getItems().setAll(filtered);
        updateLocalStatus(filtered.size());
    }

    private void updateLocalStatus(int shownCount) {
        int enabledCount = 0;
        for (LocalShaderItem item : this.localListView.getItems()) {
            if (item.enabled) enabledCount++;
        }
        this.localStatusLabel.setText(shownCount + " shaderpack(s) affiché(s) • " + enabledCount + " actif(s) • " + shortenPath(shaderpacksDir, 34));
    }

    private void setDiscoverMode(boolean discover) {
        localPane.setVisible(!discover);
        localPane.setManaged(!discover);
        localFilterField.setVisible(!discover);
        localFilterField.setManaged(!discover);
        localFilterButton.setVisible(!discover);
        localFilterButton.setManaged(!discover);
        localStatusLabel.setVisible(!discover);
        localStatusLabel.setManaged(!discover);

        onlinePane.setVisible(discover);
        onlinePane.setManaged(discover);
        onlineSearchField.setVisible(discover);
        onlineSearchField.setManaged(discover);
        onlineSearchButton.setVisible(discover);
        onlineSearchButton.setManaged(discover);
        onlineStatusLabel.setVisible(discover);
        onlineStatusLabel.setManaged(discover);
        versionCaptionLabel.setVisible(discover);
        versionCaptionLabel.setManaged(discover);
        versionField.setVisible(discover);
        versionField.setManaged(discover);
        versionApplyButton.setVisible(discover);
        versionApplyButton.setManaged(discover);

        applyTabState(this.localTabButton, !discover);
        applyTabState(this.onlineTabButton, discover);
    }

    private boolean isDiscoverMode() {
        return onlinePane != null && onlinePane.isVisible();
    }

    private void applyVersionChange() {
        if (!isDiscoverMode()) return;
        searchOnlineShaders(onlineSearchField.getText(), getChosenMinecraftVersion());
    }

    private String getChosenMinecraftVersion() {
        String value = versionField.getText() == null ? "" : versionField.getText().trim();
        if (value.isEmpty()) return getSelectedMinecraftVersion();
        return value;
    }

    private void searchOnlineShaders(final String query, final String mcVersion) {
        onlineStatusLabel.setText("Recherche en cours...");
        onlineListView.getItems().clear();

        Thread searchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String facets = "[[\"project_type:shader\"],[\"versions:" + escapeFacetValue(mcVersion) + "\"]]";
                    String url = MODRINTH_API_SEARCH
                            + "?limit=24"
                            + "&index=" + URLEncoder.encode((query == null || query.trim().isEmpty()) ? "downloads" : "relevance", "UTF-8")
                            + "&facets=" + URLEncoder.encode(facets, "UTF-8")
                            + "&query=" + URLEncoder.encode(query == null ? "" : query.trim(), "UTF-8");

                    String json = downloadText(url);
                    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                    JsonArray hits = root.getAsJsonArray("hits");

                    if (hits != null) {
                        for (JsonElement el : hits) {
                            JsonObject obj = el.getAsJsonObject();
                            final OnlineShaderItem item = new OnlineShaderItem(
                                    getAsString(obj, "project_id"),
                                    getAsString(obj, "slug"),
                                    getAsString(obj, "title"),
                                    getAsString(obj, "author"),
                                    getAsString(obj, "description"),
                                    getAsString(obj, "icon_url"),
                                    getAsInt(obj, "downloads"),
                                    extractOnlineCategories(obj.getAsJsonArray("categories"))
                            );
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    onlineListView.getItems().add(item);
                                }
                            });
                        }
                    }

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if (onlineListView.getItems().isEmpty()) {
                                onlineStatusLabel.setText("Aucun shader trouvé pour " + mcVersion + ".");
                            } else {
                                onlineStatusLabel.setText(onlineListView.getItems().size() + " shader(s) trouvés pour " + mcVersion + ".");
                            }
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            onlineStatusLabel.setText("Impossible de charger les shaders en ligne.");
                        }
                    });
                }
            }
        }, "MajestyLauncher-ShadersSearch");

        searchThread.setDaemon(true);
        searchThread.start();
    }


    private ImageView createOnlinePreview(final OnlineShaderItem item) {
        final ImageView iconView = new ImageView(fallbackIcon);
        iconView.setFitWidth(52);
        iconView.setFitHeight(52);
        iconView.setPreserveRatio(true);
        iconView.setSmooth(true);

        Thread imageThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Image image = resolveBestProjectImage(item);
                if (image != null) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            iconView.setImage(image);
                        }
                    });
                }
            }
        }, "MajestyLauncher-ShaderImage-" + sanitizeThreadName(item.projectId));
        imageThread.setDaemon(true);
        imageThread.start();

        return iconView;
    }

    private Image resolveBestProjectImage(OnlineShaderItem item) {
        if (item == null) return fallbackIcon;

        Image direct = loadRemoteImage(item.iconUrl);
        if (direct != null && direct != fallbackIcon) {
            return direct;
        }

        String previewUrl = resolveBestProjectPreviewUrl(item);
        Image preview = loadRemoteImage(previewUrl);
        if (preview != null) {
            return preview;
        }

        return fallbackIcon;
    }

    private String resolveBestProjectPreviewUrl(OnlineShaderItem item) {
        if (item == null || item.projectId == null || item.projectId.trim().isEmpty()) return item == null ? null : item.iconUrl;

        String cached = projectPreviewUrlCache.get(item.projectId);
        if (cached != null) {
            return cached;
        }

        String resolved = fetchBestProjectPreviewUrl(item.projectId);
        if (resolved == null || resolved.trim().isEmpty()) {
            resolved = item.iconUrl;
        }
        if (resolved != null) {
            projectPreviewUrlCache.put(item.projectId, resolved);
        }
        return resolved;
    }

    private String fetchBestProjectPreviewUrl(String projectId) {
        try {
            String json = downloadText("https://api.modrinth.com/v2/project/" + URLEncoder.encode(projectId, "UTF-8"));
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String galleryUrl = extractBestGalleryUrl(root.getAsJsonArray("gallery"));
            if (galleryUrl != null && !galleryUrl.trim().isEmpty()) {
                return galleryUrl;
            }
            return getAsString(root, "icon_url");
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractBestGalleryUrl(JsonArray gallery) {
        if (gallery == null || gallery.size() == 0) return null;
        String first = null;
        for (JsonElement el : gallery) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            String url = getAsString(obj, "url");
            if (url == null || url.trim().isEmpty()) continue;
            if (first == null) first = url;
            if (obj.has("featured") && !obj.get("featured").isJsonNull()) {
                try {
                    if (obj.get("featured").getAsBoolean()) {
                        return url;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return first;
    }

    private Image loadRemoteImage(String url) {
        if (url == null || url.trim().isEmpty()) return fallbackIcon;
        Image cached = remoteImageCache.get(url);
        if (cached != null) return cached;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", MODRINTH_USER_AGENT);
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int read;
                while ((read = in.read(chunk)) != -1) buffer.write(chunk, 0, read);
                byte[] bytes = buffer.toByteArray();
                Image img = new Image(new ByteArrayInputStream(bytes), 52, 52, true, true);
                if (!img.isError() && img.getWidth() > 1) { remoteImageCache.put(url, img); return img; }
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
                if (bi != null) {
                    Image fxImg = SwingFXUtils.toFXImage(bi, null);
                    if (fxImg != null && !fxImg.isError() && fxImg.getWidth() > 1) { remoteImageCache.put(url, fxImg); return fxImg; }
                }
            }
        } catch (Exception ignored) {}
        remoteImageCache.put(url, fallbackIcon);
        return fallbackIcon;
    }

    private String sanitizeThreadName(String value) {
        if (value == null || value.trim().isEmpty()) return "unknown";
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractOnlineCategories(JsonArray categories) {
        if (categories == null || categories.size() == 0) return "Shader";
        List<String> labels = new ArrayList<String>();
        for (JsonElement el : categories) {
            String value = el.getAsString();
            if (value != null && !value.trim().isEmpty() && !"shader".equalsIgnoreCase(value)) {
                labels.add(value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1));
            }
            if (labels.size() >= 3) break;
        }
        if (labels.isEmpty()) return "Shader";
        return join(labels, " / ");
    }

    private void downloadOnlineShader(final OnlineShaderItem item) {
        onlineStatusLabel.setText("Téléchargement de " + item.title + "...");

        Thread downloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String mcVersion = getChosenMinecraftVersion();
                    String versionsUrl = MODRINTH_API_PROJECT_VERSIONS + item.projectId + "/version"
                            + "?game_versions=" + URLEncoder.encode("[\"" + mcVersion + "\"]", "UTF-8");

                    String json = downloadText(versionsUrl);
                    JsonArray versions = JsonParser.parseString(json).getAsJsonArray();

                    if (versions == null || versions.size() == 0) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                showError("Aucun fichier", "Aucune version compatible trouvée pour " + mcVersion + ".");
                            }
                        });
                        return;
                    }

                    JsonObject chosenVersion = versions.get(0).getAsJsonObject();
                    JsonArray files = chosenVersion.getAsJsonArray("files");
                    if (files == null || files.size() == 0) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                showError("Aucun fichier", "Aucun fichier téléchargeable trouvé.");
                            }
                        });
                        return;
                    }

                    JsonObject chosenFile = null;
                    for (JsonElement fileEl : files) {
                        JsonObject fileObj = fileEl.getAsJsonObject();
                        String filename = getAsString(fileObj, "filename");
                        boolean primary = fileObj.has("primary") && fileObj.get("primary").getAsBoolean();
                        if (primary && filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                            chosenFile = fileObj;
                            break;
                        }
                    }
                    if (chosenFile == null) {
                        for (JsonElement fileEl : files) {
                            JsonObject fileObj = fileEl.getAsJsonObject();
                            String filename = getAsString(fileObj, "filename");
                            if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                                chosenFile = fileObj;
                                break;
                            }
                        }
                    }
                    if (chosenFile == null) {
                        chosenFile = files.get(0).getAsJsonObject();
                    }

                    final String fileUrl = getAsString(chosenFile, "url");
                    final String fileName = getAsString(chosenFile, "filename");
                    if (fileUrl == null || fileUrl.trim().isEmpty() || fileName == null || fileName.trim().isEmpty()) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                showError("Erreur", "Impossible de récupérer le fichier du shader.");
                            }
                        });
                        return;
                    }

                    File destination = uniqueDestination(new File(shaderpacksDir, fileName));
                    downloadFile(fileUrl, destination);

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            loadShaders();
                            onlineStatusLabel.setText(item.title + " téléchargé.");
                            showInfo("Téléchargement terminé", item.title + " a été ajouté dans le dossier shaderpacks.");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            onlineStatusLabel.setText("Échec du téléchargement.");
                            showError("Erreur", "Impossible de télécharger ce shader.");
                        }
                    });
                }
            }
        }, "MajestyLauncher-ShadersDownload");

        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    private void importShaderpack() {
        if (localListView.getScene() == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Ajouter un shaderpack (.zip)");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Shaderpacks", "*.zip"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File chosen = chooser.showOpenDialog(localListView.getScene().getWindow());
        if (chosen != null) {
            try {
                copyFileIntoDirectory(chosen.toPath(), shaderpacksDir.toPath());
                showInfo("Shaderpack ajouté", chosen.getName() + " a bien été copié dans le dossier shaderpacks.");
                loadShaders();
            } catch (IOException e) {
                showError("Import impossible", "Impossible de copier le shaderpack : " + e.getMessage());
            }
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Ajouter un dossier de shaderpack");
        File chosenDirectory = directoryChooser.showDialog(localListView.getScene().getWindow());
        if (chosenDirectory != null) {
            try {
                copyDirectoryIntoDirectory(chosenDirectory.toPath(), shaderpacksDir.toPath());
                showInfo("Dossier ajouté", chosenDirectory.getName() + " a bien été copié dans le dossier shaderpacks.");
                loadShaders();
            } catch (IOException e) {
                showError("Import impossible", "Impossible de copier le dossier : " + e.getMessage());
            }
        }
    }

    private void toggleShader(LocalShaderItem item) {
        try {
            File target;
            if (item.enabled) {
                target = new File(item.file.getAbsolutePath() + ".disabled");
            } else {
                String restoredName = item.file.getName().endsWith(".disabled")
                        ? item.file.getName().substring(0, item.file.getName().length() - 9)
                        : item.file.getName();
                target = new File(item.file.getParentFile(), restoredName);
            }
            Files.move(item.file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            loadShaders();
        } catch (IOException e) {
            showError("Action impossible", "Impossible de changer l'état du shaderpack : " + e.getMessage());
        }
    }

    private void deleteShader(LocalShaderItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le shaderpack");
        alert.setHeaderText(item.displayName);
        alert.setContentText("Confirmer la suppression de cet élément ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent() || result.get() != ButtonType.OK) return;

        try {
            if (item.file.isDirectory()) {
                deleteDirectory(item.file.toPath());
            } else {
                Files.deleteIfExists(item.file.toPath());
            }
            loadShaders();
        } catch (IOException e) {
            showError("Suppression impossible", "Impossible de supprimer le shaderpack : " + e.getMessage());
        }
    }

    private void openShaderpacksFolder() {
        try {
            ensureShaderpacksDir();
            Desktop.getDesktop().open(shaderpacksDir);
        } catch (Exception e) {
            showError("Ouverture impossible", "Impossible d'ouvrir le dossier shaderpacks.");
        }
    }

    private void openProjectPage(OnlineShaderItem item) {
        try {
            Desktop.getDesktop().browse(new URI(MODRINTH_WEB_SHADER + item.slug));
        } catch (Exception e) {
            showError("Ouverture impossible", "Impossible d'ouvrir la page du shader.");
        }
    }

    private boolean isVisibleShaderpack(File file) {
        if (file == null) return false;
        if (file.isDirectory()) return true;
        String lower = file.getName().toLowerCase(Locale.ROOT);
        return lower.endsWith(".zip") || lower.endsWith(".zip.disabled");
    }

    private File resolveShaderpacksDir() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return new File(System.getenv("APPDATA") + "/.majestycraft/bin/game/shaderpacks");
        } else if (os.contains("mac")) {
            return new File(System.getProperty("user.home") + "/Library/Application Support/.majestycraft/bin/game/shaderpacks");
        }
        return new File(System.getProperty("user.home") + "/.majestycraft/bin/game/shaderpacks");
    }

    private void ensureShaderpacksDir() {
        if (!shaderpacksDir.exists()) {
            shaderpacksDir.mkdirs();
        }
    }

    private void copyFileIntoDirectory(Path source, Path targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory);
        String fileName = source.getFileName().toString();
        Path target = createUniqueTarget(targetDirectory, fileName);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyDirectoryIntoDirectory(Path sourceDirectory, Path targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory);
        Path targetRoot = createUniqueTarget(targetDirectory, sourceDirectory.getFileName().toString());
        Files.walk(sourceDirectory).forEach(path -> {
            try {
                Path relative = sourceDirectory.relativize(path);
                Path target = targetRoot.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Path createUniqueTarget(Path dir, String originalName) throws IOException {
        Path target = dir.resolve(originalName);
        if (!Files.exists(target)) {
            return target;
        }
        String base = originalName;
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > 0 && dot < originalName.length() - 1) {
            base = originalName.substring(0, dot);
            ext = originalName.substring(dot);
        }
        int i = 2;
        while (true) {
            target = dir.resolve(base + " (" + i + ")" + ext);
            if (!Files.exists(target)) {
                return target;
            }
            i++;
        }
    }

    private void deleteDirectory(Path root) throws IOException {
        List<Path> paths = new ArrayList<Path>();
        Files.walk(root).forEach(paths::add);
        Collections.reverse(paths);
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }

    private String downloadText(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", MODRINTH_USER_AGENT);
        connection.setRequestProperty("Accept", "application/json");
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        if (stream == null) throw new IOException("HTTP " + code);

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " - " + sb.toString());
        }
        return sb.toString();
    }

    private void downloadFile(String fileUrl, File destination) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", MODRINTH_USER_AGENT);
        connection.connect();

        if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
            throw new IOException("HTTP " + connection.getResponseCode());
        }

        InputStream input = new BufferedInputStream(connection.getInputStream());
        OutputStream output = new FileOutputStream(destination);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        output.flush();
        output.close();
        input.close();
    }

    private File uniqueDestination(File destination) {
        if (!destination.exists()) return destination;

        String name = destination.getName();
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }

        int i = 2;
        while (true) {
            File candidate = new File(destination.getParentFile(), base + " (" + i + ")" + ext);
            if (!candidate.exists()) return candidate;
            i++;
        }
    }

    private String getSelectedMinecraftVersion() {
        Object value = paneRef.getConfig().getValue(EnumConfig.VERSION);
        if (value == null) return "1.21.11";
        return String.valueOf(value);
    }

    private String escapeFacetValue(String s) {
        return s == null ? "" : s.replace("\"", "");
    }

    private String join(List<String> values, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(separator);
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    private int getAsInt(JsonObject obj, String key) {
        try {
            return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String getAsString(JsonObject obj, String key) {
        try {
            return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private void styleSearchField(JFXTextField field) {
        field.setLabelFloat(false);
        field.setFocusColor(Color.web("#ff9800"));
        field.setUnFocusColor(Color.rgb(255, 255, 255, 0.18));
        field.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: rgba(255,255,255,0.08);" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;" +
                "-fx-prompt-text-fill: rgba(255,255,255,0.35);" +
                "-fx-text-fill: rgba(255,255,255,0.95);"
        );
    }

    private void styleTabButton(JFXButton button, boolean enabled) {
        button.setButtonType(JFXButton.ButtonType.RAISED);
        button.setPrefHeight(34);
        button.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        button.setTextFill(Color.WHITE);
        applyTabState(button, enabled);
        installHoverScale(button);
    }

    private void applyTabState(JFXButton button, boolean active) {
        if (button == null) return;
        button.setStyle(active
                ? "-fx-background-color: linear-gradient(#ff9f1a, #ff7a00); -fx-background-radius: 16; -fx-text-fill: white;"
                : "-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 16; -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 16; -fx-border-width: 1; -fx-text-fill: white;");
    }

    private JFXButton createGhostButton(String text, double w, double h) {
        JFXButton button = new JFXButton(text);
        button.setButtonType(JFXButton.ButtonType.RAISED);
        button.setPrefSize(w, h);
        button.setMinSize(w, h);
        button.setMaxSize(w, h);
        button.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        button.setTextFill(Color.WHITE);
        button.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.10);" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1;" +
                "-fx-text-fill: white;"
        );
        button.setRipplerFill(Color.rgb(255, 152, 0, 0.35));
        installHoverScale(button);
        return button;
    }

    private JFXButton createCellButton(String text, boolean danger) {
        JFXButton button = new JFXButton(text);
        button.setButtonType(JFXButton.ButtonType.RAISED);
        button.setPrefSize(danger ? 98 : 104, 32);
        button.setMinSize(danger ? 98 : 104, 32);
        button.setMaxSize(danger ? 98 : 104, 32);
        button.setFont(FontLoader.loadFont("Poppins-Regular.ttf", "Poppins", 12F));
        button.setTextFill(Color.WHITE);
        button.setStyle(danger
                ? "-fx-background-color: rgba(255,120,120,0.20); -fx-background-radius: 16; -fx-border-color: rgba(255,140,140,0.24); -fx-border-radius: 16; -fx-border-width: 1;"
                : "-fx-background-color: linear-gradient(#ff9f1a, #ff7a00); -fx-background-radius: 16;");
        return button;
    }

    private void styleCard(LauncherRectangle rectangle, double opacity) {
        rectangle.setArcWidth(34);
        rectangle.setArcHeight(34);
        rectangle.setFill(Color.rgb(8, 12, 18, opacity));
        rectangle.setStroke(Color.rgb(255, 255, 255, 0.10));
        rectangle.setStrokeWidth(1.0);
        rectangle.setEffect(new DropShadow(35, Color.rgb(0, 0, 0, 0.68)));
    }

    private void styleSidebarFooterButton(LauncherButton button, boolean primary) {
        String style = primary
                ? "-fx-background-color: linear-gradient(#ff9f1a, #ff7a00); -fx-background-radius: 18; -fx-text-fill: white;"
                : "-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 18; -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 18; -fx-border-width: 1; -fx-text-fill: white;";
        button.setStyle(style);
        installHoverScale(button);
    }

    private void installResponsiveBackground(Pane root, GameEngine engine, String imageName) {
        Image backgroundImage = getResourceLocation().loadImage(engine, imageName);
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(root.widthProperty());
        backgroundView.fitHeightProperty().bind(root.heightProperty());
        backgroundView.setLayoutX(0);
        backgroundView.setLayoutY(0);
        root.getChildren().add(backgroundView);
    }

    private void installHoverScale(Node node) {
        node.setOnMouseEntered(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(120), node);
            st.setToX(1.04);
            st.setToY(1.04);
            st.play();
        });
        node.setOnMouseExited(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(120), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private void animateIn(Node node, double fromX, double fromY, int delayMs) {
        if (node == null) return;
        node.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(420), node);
        tt.setFromX(fromX);
        tt.setFromY(fromY);
        tt.setToX(0);
        tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));

        FadeTransition ft = new FadeTransition(Duration.millis(420), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setDelay(Duration.millis(delayMs));

        new ParallelTransition(tt, ft).play();
    }

    private String formatSize(long size) {
        double mb = size / 1024.0 / 1024.0;
        if (mb >= 1.0) {
            return decimalFormat.format(mb) + " Mo";
        }
        double kb = size / 1024.0;
        return decimalFormat.format(kb) + " Ko";
    }

    private String shortenPath(File dir, int maxLen) {
        String path = dir.getAbsolutePath();
        if (path.length() <= maxLen) return path;
        return "..." + path.substring(Math.max(0, path.length() - maxLen));
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class LocalShaderItem {
        private final File file;
        private final String displayName;
        private final boolean enabled;
        private final long size;
        private final long lastModified;
        private final String typeLabel;

        private LocalShaderItem(File file) {
            this.file = file;
            this.enabled = !file.getName().toLowerCase(Locale.ROOT).endsWith(".disabled");
            String name = file.getName();
            if (name.toLowerCase(Locale.ROOT).endsWith(".disabled")) {
                name = name.substring(0, name.length() - 9);
            }
            this.displayName = name;
            this.size = file.isDirectory() ? directorySize(file.toPath()) : file.length();
            this.lastModified = file.lastModified();
            this.typeLabel = file.isDirectory() ? "Dossier" : "Archive ZIP";
        }

        private static long directorySize(Path root) {
            try {
                final long[] size = {0L};
                Files.walk(root).filter(Files::isRegularFile).forEach(p -> {
                    try {
                        size[0] += Files.size(p);
                    } catch (IOException ignored) {
                    }
                });
                return size[0];
            } catch (IOException e) {
                return 0L;
            }
        }
    }

    private static class OnlineShaderItem {
        private final String projectId;
        private final String slug;
        private final String title;
        private final String author;
        private final String description;
        private final String iconUrl;
        private final int downloads;
        private final String categoriesLabel;

        private OnlineShaderItem(String projectId, String slug, String title, String author, String description, String iconUrl, int downloads, String categoriesLabel) {
            this.projectId = projectId;
            this.slug = slug == null ? "" : slug;
            this.title = title == null || title.trim().isEmpty() ? "Shader sans nom" : title;
            this.author = author == null || author.trim().isEmpty() ? "inconnu" : author;
            this.description = description == null ? "" : description;
            this.iconUrl = iconUrl;
            this.downloads = downloads;
            this.categoriesLabel = categoriesLabel == null || categoriesLabel.trim().isEmpty() ? "Shader" : categoriesLabel;
        }
    }
}