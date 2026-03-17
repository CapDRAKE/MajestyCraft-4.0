package fr.majestycraft.launcher;

import animatefx.animation.ZoomOutDown;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import fr.majestycraft.Main;
import fr.trxyy.alternative.alternative_api.GameEngine;
import fr.trxyy.alternative.alternative_api.utils.FontLoader;
import fr.trxyy.alternative.alternative_api.utils.config.EnumConfig;
import fr.trxyy.alternative.alternative_api_ui.base.IScreen;
import java.net.URI;

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
import javafx.scene.control.Button;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
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
import java.nio.file.Paths;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LauncherPacks extends IScreen {

    private static final int W = 1180;
    private static final int H = 820;

    private static final String LABEL_TITLE_LISTE = Main.bundle.getString("LABEL_TITLE_LISTE");
    private static final String BUTTON_ADD_PACK = Main.bundle.getString("BUTTON_ADD_PACK");
    private static final String DIALOG_SELECT_RESOURCE_PACK = Main.bundle.getString("DIALOG_SELECT_RESOURCE_PACK");
    private static final String DIALOG_CONFIRMATION_REMOVAL = Main.bundle.getString("DIALOG_CONFIRMATION_REMOVAL");
    private static final String DIALOG_CONFIRMATION_REMOVAL_MSG = Main.bundle.getString("DIALOG_CONFIRMATION_REMOVAL_MSG");
    private static final String ERROR_DELETION = Main.bundle.getString("ERROR_DELETION");
    private static final String ERROR_DELETION_MSG = Main.bundle.getString("ERROR_DELETION_MSG");
    private static final String QUITTER_BUTTON_TEXT = "Retour";

    private static final String MODRINTH_API_SEARCH = "https://api.modrinth.com/v2/search";
    private static final String MODRINTH_API_PROJECT_VERSIONS = "https://api.modrinth.com/v2/project/";
    private static final String MODRINTH_WEB_RESOURCEPACK = "https://modrinth.com/resourcepack/";
    private static final String MODRINTH_USER_AGENT = "CapDRAKE/MajestyLauncher/3.7 (majestycraft.com)";

    private final LauncherPanel paneRef;

    private ListView<ResourcePackItem> localListView;
    private ListView<OnlinePackItem> onlineListView;

    private Pane localPane;
    private Pane onlinePane;

    private JFXButton localTabButton;
    private JFXButton onlineTabButton;
    private JFXButton addButton;
    private JFXButton refreshButton;
    private JFXButton openFolderButton;
    private LauncherButton quitButton;

    private JFXTextField searchField;
    private LauncherLabel onlineStatusLabel;
    private LauncherLabel versionHintLabel;

    private LauncherImage heroLogo;
    private LauncherLabel heroTitle;
    private LauncherLabel heroSubtitle;
    private LauncherLabel heroLine1;
    private LauncherLabel heroLine2;
    private LauncherLabel heroLine3;

    private File resourcePacksDir;

    private double xOffSet;
    private double yOffSet;

    public LauncherPacks(final Pane root, final GameEngine engine, final LauncherPanel pane) {
        this.paneRef = pane;

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

        buildHero(root, engine);
        buildMain(root);

        this.resourcePacksDir = resolveResourcePacksDir();
        ensureResourcePackDir();

        configureLocalList();
        configureOnlineList();

        setDiscoverMode(true);
        loadResourcePacks();
        searchOnlinePacks("", getSelectedMinecraftVersion());

        Platform.runLater(() -> {
            animateIn(heroLogo, -14, 10, 60);
            animateIn(heroTitle, -14, 10, 110);
            animateIn(heroSubtitle, -14, 10, 160);
            animateIn(heroLine1, -14, 10, 210);
            animateIn(heroLine2, -14, 10, 260);
            animateIn(heroLine3, -14, 10, 310);

            animateIn(localTabButton, 18, 0, 120);
            animateIn(onlineTabButton, 18, 0, 160);
            animateIn(searchField, 18, 0, 200);
            animateIn(refreshButton, 18, 0, 240);
            animateIn(versionHintLabel, 18, 0, 280);
            animateIn(localPane, 18, 0, 320);
            animateIn(onlinePane, 18, 0, 360);
            animateIn(addButton, 0, 12, 400);
            animateIn(openFolderButton, 0, 12, 450);
            animateIn(quitButton, 0, 12, 500);
        });
    }

    private void buildHero(Pane root, GameEngine engine) {
        final int heroX = 120;
        final int heroY = 70;
        final int heroW = 250;
        final int heroH = 610;

        LauncherRectangle heroCard = new LauncherRectangle(root, heroX, heroY, heroW, heroH);
        styleCard(heroCard, 0.70);

        this.heroLogo = new LauncherImage(root);
        this.heroLogo.setImage(getResourceLocation().loadImage(engine, "launchergifpng.png"));
        this.heroLogo.setSize(130, 130);
        this.heroLogo.setBounds(heroX + 60, heroY + 36, 130, 130);

        this.heroTitle = new LauncherLabel(root);
        this.heroTitle.setText("Ressource Packs");
        this.heroTitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 27F));
        this.heroTitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.96)");
        this.heroTitle.setPosition(heroX, heroY + 186);
        this.heroTitle.setSize(heroW, 34);
        this.heroTitle.setAlignment(Pos.CENTER);

        this.heroSubtitle = new LauncherLabel(root);
        this.heroSubtitle.setText("Locaux + catalogue en ligne");
        this.heroSubtitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        this.heroSubtitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.96)");
        this.heroSubtitle.setPosition(heroX, heroY + 220);
        this.heroSubtitle.setSize(heroW, 22);
        this.heroSubtitle.setAlignment(Pos.CENTER);

        this.heroLine1 = new LauncherLabel(root);
        this.heroLine1.setText("• Importe tes packs localement");
        this.heroLine1.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.heroLine1.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.68)");
        this.heroLine1.setPosition(heroX + 24, heroY + 300);
        this.heroLine1.setSize(heroW - 48, 20);

        this.heroLine2 = new LauncherLabel(root);
        this.heroLine2.setText("• Découvre des packs via Modrinth");
        this.heroLine2.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.heroLine2.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.56)");
        this.heroLine2.setPosition(heroX + 24, heroY + 328);
        this.heroLine2.setSize(heroW - 48, 20);

        this.heroLine3 = new LauncherLabel(root);
        this.heroLine3.setText("• Télécharge directement dans le launcher");
        this.heroLine3.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.heroLine3.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.44)");
        this.heroLine3.setPosition(heroX + 24, heroY + 356);
        this.heroLine3.setSize(heroW - 48, 20);
    }

    private void buildMain(Pane root) {
        final int mainX = 392;
        final int mainY = 62;
        final int mainW = 640;
        final int mainH = 650;

        LauncherRectangle mainCard = new LauncherRectangle(root, mainX, mainY, mainW, mainH);
        styleCard(mainCard, 0.76);

        LauncherLabel titleLabel = new LauncherLabel(root);
        titleLabel.setText(LABEL_TITLE_LISTE);
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        titleLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        titleLabel.setPosition(mainX, mainY + 22);
        titleLabel.setSize(mainW, 34);
        titleLabel.setAlignment(Pos.CENTER);

        LauncherLabel subTitleLabel = new LauncherLabel(root);
        subTitleLabel.setText("Gère tes packs ou explore des packs compatibles");
        subTitleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        subTitleLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.45);");
        subTitleLabel.setPosition(mainX, mainY + 54);
        subTitleLabel.setSize(mainW, 18);
        subTitleLabel.setAlignment(Pos.CENTER);

        this.localTabButton = new JFXButton("Locaux");
        styleTabButton(this.localTabButton, false);
        this.localTabButton.setLayoutX(mainX + 34);
        this.localTabButton.setLayoutY(mainY + 96);
        this.localTabButton.setPrefWidth(116);
        this.localTabButton.setOnAction(e -> setDiscoverMode(false));
        root.getChildren().add(this.localTabButton);

        this.onlineTabButton = new JFXButton("Explorer");
        styleTabButton(this.onlineTabButton, true);
        this.onlineTabButton.setLayoutX(mainX + 160);
        this.onlineTabButton.setLayoutY(mainY + 96);
        this.onlineTabButton.setPrefWidth(116);
        this.onlineTabButton.setOnAction(e -> setDiscoverMode(true));
        root.getChildren().add(this.onlineTabButton);

        this.searchField = new JFXTextField();
        this.searchField.setPromptText("Rechercher un pack...");
        this.searchField.setPrefWidth(180);
        this.searchField.setLayoutX(mainX + mainW - 340);
        this.searchField.setLayoutY(mainY + 100);
        this.searchField.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        this.searchField.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: rgba(255,255,255,0.12);" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;" +
                "-fx-text-fill: rgba(255,255,255,0.92);" +
                "-fx-prompt-text-fill: rgba(255,255,255,0.42);" +
                "-fx-padding: 0 12 0 12;" +
                "-jfx-focus-color: #ff9800;" +
                "-jfx-unfocus-color: rgba(255,255,255,0.18);"
        );
        this.searchField.setOnAction(e -> searchOnlinePacks(searchField.getText(), getSelectedMinecraftVersion()));
        root.getChildren().add(this.searchField);

        this.refreshButton = new JFXButton("Rechercher");
        styleSecondaryButton(this.refreshButton);
        this.refreshButton.setLayoutX(mainX + mainW - 60 - 95);
        this.refreshButton.setLayoutY(mainY + 98);
        this.refreshButton.setPrefWidth(120);
        this.refreshButton.setOnAction(e -> searchOnlinePacks(searchField.getText(), getSelectedMinecraftVersion()));
        root.getChildren().add(this.refreshButton);

        this.versionHintLabel = new LauncherLabel(root);
        this.versionHintLabel.setText("Version ciblée : " + getSelectedMinecraftVersion());
        this.versionHintLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.versionHintLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.90);");
        this.versionHintLabel.setPosition(mainX + 34, mainY + 142);
        this.versionHintLabel.setSize(mainW - 68, 18);

        this.localPane = new Pane();
        this.localPane.setLayoutX(mainX + 28);
        this.localPane.setLayoutY(mainY + 170);
        this.localPane.setPrefSize(mainW - 72, 405);
        root.getChildren().add(this.localPane);

        this.onlinePane = new Pane();
        this.onlinePane.setLayoutX(mainX + 28);
        this.onlinePane.setLayoutY(mainY + 176);
        this.onlinePane.setPrefSize(mainW - 72, 405);
        root.getChildren().add(this.onlinePane);

        this.localListView = new ListView<>();
        this.localListView.setPrefSize(mainW - 72, 405);
        this.localListView.setStyle(
                "-fx-background-color: rgba(0,0,0,0);" +
                "-fx-control-inner-background: rgba(255,255,255,0.04);" +
                "-fx-background-insets: 0;" +
                "-fx-background-radius: 18;"
        );
        this.localPane.getChildren().add(this.localListView);

        this.onlineListView = new ListView<>();
        this.onlineListView.setPrefSize(mainW - 72, 405);
        this.onlineListView.setStyle(
                "-fx-background-color: rgba(0,0,0,0);" +
                "-fx-control-inner-background: rgba(255,255,255,0.04);" +
                "-fx-background-insets: 0;" +
                "-fx-background-radius: 18;"
        );
        this.onlinePane.getChildren().add(this.onlineListView);

        this.onlineStatusLabel = new LauncherLabel(root);
        this.onlineStatusLabel.setText("");
        this.onlineStatusLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.onlineStatusLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.52);");
        this.onlineStatusLabel.setPosition(mainX + 34, mainY + 592);
        this.onlineStatusLabel.setSize(280, 18);

        this.addButton = new JFXButton(BUTTON_ADD_PACK);
        styleSecondaryButton(this.addButton);
        this.addButton.setLayoutX(mainX + 34);
        this.addButton.setLayoutY(mainY + mainH - 54);
        this.addButton.setPrefWidth(150);
        this.addButton.setOnAction(e -> addResourcePack());
        root.getChildren().add(this.addButton);

        this.openFolderButton = new JFXButton("Ouvrir dossier");
        styleSecondaryButton(this.openFolderButton);
        this.openFolderButton.setLayoutX(mainX + 300);
        this.openFolderButton.setLayoutY(mainY + mainH - 54);
        this.openFolderButton.setPrefWidth(150);
        this.openFolderButton.setOnAction(e -> openResourcePackFolder());
        root.getChildren().add(this.openFolderButton);

        this.quitButton = new LauncherButton(root);
        this.quitButton.setText(QUITTER_BUTTON_TEXT);
        this.quitButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff9800, #ff6d00);" +
                "-fx-background-radius: 18;" +
                "-fx-text-fill: white;"
        );
        this.quitButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        this.quitButton.setPosition(mainX + mainW - 150, mainY + mainH - 54);
        this.quitButton.setSize(110, 38);
        this.quitButton.setOnAction(event -> {
            final ZoomOutDown animation = new ZoomOutDown(root);
            animation.setOnFinished(actionEvent -> {
                Stage st = (Stage) ((LauncherButton) event.getSource()).getScene().getWindow();
                st.close();
            });
            animation.setResetOnFinished(true);
            animation.play();
        });
    }

    private void setDiscoverMode(boolean discover) {
        onlinePane.setVisible(discover);
        onlinePane.setManaged(discover);
        searchField.setVisible(discover);
        searchField.setManaged(discover);
        refreshButton.setVisible(discover);
        refreshButton.setManaged(discover);
        versionHintLabel.setVisible(discover);
        versionHintLabel.setManaged(discover);
        onlineStatusLabel.setVisible(discover);

        localPane.setVisible(!discover);
        localPane.setManaged(!discover);
        addButton.setVisible(!discover);
        addButton.setManaged(!discover);

        applyTabState(this.localTabButton, !discover);
        applyTabState(this.onlineTabButton, discover);
    }

    private void applyTabState(JFXButton button, boolean active) {
        if (active) {
            button.setStyle(
                    "-fx-background-radius: 16;" +
                    "-fx-text-fill: white;" +
                    "-fx-background-color: linear-gradient(to right, #ff9800, #ff6d00);"
            );
        } else {
            button.setStyle(
                    "-fx-background-radius: 16;" +
                    "-fx-text-fill: rgba(255,255,255,0.86);" +
                    "-fx-background-color: rgba(255,255,255,0.08);" +
                    "-fx-border-color: rgba(255,255,255,0.10);" +
                    "-fx-border-radius: 16;" +
                    "-fx-border-width: 1;"
            );
        }
    }

    private void styleTabButton(JFXButton button, boolean active) {
        button.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        applyTabState(button, active);
    }

    private void styleCard(LauncherRectangle r, double opacity) {
        r.setArcWidth(34);
        r.setArcHeight(34);
        r.setFill(Color.rgb(8, 12, 18, opacity));
        r.setStroke(Color.rgb(255, 255, 255, 0.10));
        r.setStrokeWidth(1);
        r.setEffect(new DropShadow(36, Color.rgb(0, 0, 0, 0.72)));
        r.setMouseTransparent(true);
    }

    private void styleSecondaryButton(JFXButton btn) {
        btn.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        btn.setStyle(
                "-fx-background-radius: 18;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: rgba(255,255,255,0.10);" +
                "-fx-border-color: rgba(255,255,255,0.16);" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1;"
        );
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

    private File resolveResourcePacksDir() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new File(System.getenv("APPDATA") + "/.majestycraft/bin/game/resourcepacks");
        } else if (os.contains("mac")) {
            return new File(System.getProperty("user.home") + "/Library/Application Support/.majestycraft/bin/game/resourcepacks");
        } else {
            return new File(System.getProperty("user.home") + "/.majestycraft/bin/game/resourcepacks");
        }
    }

    private void ensureResourcePackDir() {
        if (!resourcePacksDir.exists()) {
            resourcePacksDir.mkdirs();
        }
    }

    private void loadResourcePacks() {
        localListView.getItems().clear();

        if (resourcePacksDir.exists() && resourcePacksDir.isDirectory()) {
            File[] files = resourcePacksDir.listFiles();
            if (files == null) return;

            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
                    try (ZipFile zipFile = new ZipFile(file)) {
                        ZipEntry packMetaEntry = zipFile.getEntry("pack.mcmeta");
                        ZipEntry packIconEntry = zipFile.getEntry("pack.png");

                        if (packMetaEntry != null) {
                            Image packIcon = null;
                            if (packIconEntry != null) {
                                packIcon = new Image(zipFile.getInputStream(packIconEntry));
                            }

                            String displayName = file.getName().substring(0, file.getName().length() - 4);
                            localListView.getItems().add(new ResourcePackItem(displayName, file.getName(), packIcon));
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    private void addResourcePack() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(DIALOG_SELECT_RESOURCE_PACK);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Packs de ressources", "*.zip"));

        Stage currentStage = null;
        if (addButton.getScene() != null) {
            currentStage = (Stage) addButton.getScene().getWindow();
        }

        File selectedFile = fileChooser.showOpenDialog(currentStage);
        if (selectedFile != null) {
            try {
                File destFile = uniqueDestination(new File(resourcePacksDir, selectedFile.getName()));
                Files.copy(selectedFile.toPath(), destFile.toPath());
                loadResourcePacks();
            } catch (IOException e) {
                showErrorDialog("Erreur", "Impossible d'ajouter le pack.");
            }
        }
    }

    private void openResourcePackFolder() {
        try {
            Desktop.getDesktop().open(resourcePacksDir);
        } catch (IOException e) {
            showErrorDialog("Erreur", "Impossible d'ouvrir le dossier des packs.");
        }
    }

    private void configureLocalList() {
        localListView.setCellFactory(param -> new ListCell<ResourcePackItem>() {
            private final ImageView packImageView = new ImageView();
            private final Label nameLabel = new Label();
            private final JFXButton deleteButton = new JFXButton("Supprimer");
            private final VBox textBox = new VBox(nameLabel);
            private final HBox hbox = new HBox(14, packImageView, textBox, deleteButton);

            {
                packImageView.setFitWidth(46);
                packImageView.setFitHeight(46);

                nameLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
                nameLabel.setStyle("-fx-text-fill: white;");

                deleteButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
                deleteButton.setStyle(
                        "-fx-background-color: rgba(255,80,80,0.18);" +
                        "-fx-background-radius: 14;" +
                        "-fx-text-fill: white;" +
                        "-fx-border-color: rgba(255,255,255,0.10);" +
                        "-fx-border-radius: 14;"
                );

                hbox.setAlignment(Pos.CENTER_LEFT);
                textBox.setPrefWidth(320);
            }

            @Override
            protected void updateItem(ResourcePackItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                packImageView.setImage(item.icon != null ? item.icon : new Image(getClass().getResource("/resources/launchergifpng.png").toExternalForm()));
                nameLabel.setText(item.displayName);

                deleteButton.setOnAction(event -> {
                    if (showConfirmationDialog(DIALOG_CONFIRMATION_REMOVAL, DIALOG_CONFIRMATION_REMOVAL_MSG)) {
                        File packFile = new File(resourcePacksDir, item.fileName);
                        if (packFile.delete()) {
                            localListView.getItems().remove(item);
                        } else {
                            showErrorDialog(ERROR_DELETION, ERROR_DELETION_MSG);
                        }
                    }
                });

                setStyle("-fx-background-color: transparent;");
                setGraphic(hbox);
            }
        });
    }

    private void configureOnlineList() {
        onlineListView.setCellFactory(param -> new ListCell<OnlinePackItem>() {
            private final ImageView iconView = new ImageView();
            private final Label titleLabel = new Label();
            private final Label authorLabel = new Label();
            private final Label descLabel = new Label();
            private final Label statsLabel = new Label();

            private final VBox textBox = new VBox(titleLabel, authorLabel, descLabel, statsLabel);

            private final JFXButton openButton = new JFXButton("Page");
            private final JFXButton downloadButton = new JFXButton("Télécharger");
            private final VBox actionsBox = new VBox(openButton, downloadButton);

            private final HBox row = new HBox(14, iconView, textBox, actionsBox);

            {
                iconView.setFitWidth(54);
                iconView.setFitHeight(54);

                titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
                titleLabel.setStyle("-fx-text-fill: white;");

                authorLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
                authorLabel.setStyle("-fx-text-fill: rgba(255,176,0,0.90);");

                descLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
                descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.66);");

                statsLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
                statsLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.46);");

                openButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
                openButton.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.10);" +
                        "-fx-background-radius: 14;" +
                        "-fx-text-fill: white;"
                );

                downloadButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
                downloadButton.setStyle(
                        "-fx-background-color: linear-gradient(to right, #ff9800, #ff6d00);" +
                        "-fx-background-radius: 14;" +
                        "-fx-text-fill: white;"
                );

                textBox.setPrefWidth(330);
                actionsBox.setSpacing(8);
                actionsBox.setPrefWidth(110);
                actionsBox.setAlignment(Pos.CENTER);
                row.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(OnlinePackItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (item.iconUrl != null && !item.iconUrl.isEmpty()) {
                    iconView.setImage(loadRemoteImageSafe(item.iconUrl, new Image(getClass().getResource("/resources/launchergifpng.png").toExternalForm())));
                } else {
                    iconView.setImage(new Image(getClass().getResource("/resources/launchergifpng.png").toExternalForm()));
                }

                titleLabel.setText(item.title);
                authorLabel.setText("par " + item.author);
                descLabel.setText(item.description == null || item.description.isEmpty() ? "Aucune description." : item.description);
                statsLabel.setText(item.downloads + " téléchargements");

                openButton.setOnAction(e -> openExternalLink(MODRINTH_WEB_RESOURCEPACK + item.slug));
                downloadButton.setOnAction(e -> downloadOnlinePack(item));

                setStyle("-fx-background-color: transparent;");
                setGraphic(row);
            }
        });
    }

    private void searchOnlinePacks(String query, String mcVersion) {
        onlineStatusLabel.setText("Chargement des packs");
        onlineListView.getItems().clear();

        Thread searchThread = new Thread(() -> {
            try {
                String facets = "[[\"project_type:resourcepack\"],[\"versions:" + escapeFacetValue(mcVersion) + "\"]]";
                String url = MODRINTH_API_SEARCH
                        + "?query=" + URLEncoder.encode(query == null ? "" : query, "UTF-8")
                        + "&index=downloads"
                        + "&limit=20"
                        + "&facets=" + URLEncoder.encode(facets, "UTF-8");

                String json = downloadText(url);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonArray hits = root.getAsJsonArray("hits");

                Platform.runLater(() -> onlineListView.getItems().clear());

                if (hits != null) {
                    for (JsonElement el : hits) {
                        JsonObject o = el.getAsJsonObject();
                        OnlinePackItem item = new OnlinePackItem(
                                getAsString(o, "slug"),
                                getAsString(o, "title"),
                                getAsString(o, "author"),
                                getAsString(o, "description"),
                                getAsString(o, "icon_url"),
                                getAsInt(o, "downloads")
                        );
                        Platform.runLater(() -> onlineListView.getItems().add(item));
                    }
                }

                Platform.runLater(() -> {
                    if (onlineListView.getItems().isEmpty()) {
                        onlineStatusLabel.setText("Aucun pack trouvé pour " + mcVersion + ".");
                    } else {
                        onlineStatusLabel.setText(onlineListView.getItems().size() + " pack(s) trouvés pour " + mcVersion + ".");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> onlineStatusLabel.setText("Impossible de charger les packs en ligne."));
            }
        }, "MajestyLauncher-PacksSearch");

        searchThread.setDaemon(true);
        searchThread.start();
    }

    private void downloadOnlinePack(OnlinePackItem item) {
        onlineStatusLabel.setText("Téléchargement de " + item.title + "");

        Thread downloadThread = new Thread(() -> {
            try {
                String mcVersion = getSelectedMinecraftVersion();
                String versionsUrl = MODRINTH_API_PROJECT_VERSIONS + item.slug + "/version"
                        + "?game_versions=" + URLEncoder.encode("[\"" + mcVersion + "\"]", "UTF-8")
                        + "&loaders=" + URLEncoder.encode("[\"minecraft\"]", "UTF-8");

                String json = downloadText(versionsUrl);
                JsonArray versions = JsonParser.parseString(json).getAsJsonArray();

                if (versions == null || versions.size() == 0) {
                    Platform.runLater(() -> showErrorDialog("Aucun fichier", "Aucune version compatible trouvée pour " + mcVersion + "."));
                    return;
                }

                JsonObject selectedVersion = versions.get(0).getAsJsonObject();
                JsonArray files = selectedVersion.getAsJsonArray("files");

                if (files == null || files.size() == 0) {
                    Platform.runLater(() -> showErrorDialog("Aucun fichier", "Aucun fichier téléchargeable trouvé."));
                    return;
                }

                JsonObject chosenFile = null;
                for (JsonElement fileEl : files) {
                    JsonObject fileObj = fileEl.getAsJsonObject();
                    if (fileObj.has("primary") && fileObj.get("primary").getAsBoolean()) {
                        chosenFile = fileObj;
                        break;
                    }
                }
                if (chosenFile == null) chosenFile = files.get(0).getAsJsonObject();

                String fileUrl = getAsString(chosenFile, "url");
                String fileName = getAsString(chosenFile, "filename");
                if (fileUrl == null || fileUrl.trim().isEmpty() || fileName == null || fileName.trim().isEmpty()) {
                    Platform.runLater(() -> showErrorDialog("Erreur", "Impossible de récupérer le fichier du pack."));
                    return;
                }

                File destination = uniqueDestination(new File(resourcePacksDir, fileName));
                downloadFile(fileUrl, destination);

                Platform.runLater(() -> {
                    loadResourcePacks();
                    onlineStatusLabel.setText(item.title + " téléchargé.");
                    showInfoDialog("Téléchargement terminé", item.title + " a été ajouté dans tes resource packs.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    onlineStatusLabel.setText("Échec du téléchargement.");
                    showErrorDialog("Erreur", "Impossible de télécharger ce pack.");
                });
            }
        }, "MajestyLauncher-PackDownload");

        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    private String getSelectedMinecraftVersion() {
        Object value = paneRef.getConfig().getValue(EnumConfig.VERSION);
        if (value == null) return "1.21.11";
        return String.valueOf(value);
    }

    private String escapeFacetValue(String s) {
        return s == null ? "" : s.replace("\"", "");
    }

    private String downloadText(String urlStr) throws IOException {
        HttpURLConnection connection = null;
        InputStream is = null;

        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", MODRINTH_USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");

            int code = connection.getResponseCode();
            is = (code >= 200 && code < 300) ? connection.getInputStream() : connection.getErrorStream();
            if (is == null) throw new IOException("HTTP " + code);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);

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

    private void downloadFile(String fileUrl, File destination) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", MODRINTH_USER_AGENT);

            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                throw new IOException("HTTP " + connection.getResponseCode());
            }

            try (InputStream in = new BufferedInputStream(connection.getInputStream());
                 OutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private File uniqueDestination(File target) {
        if (!target.exists()) return target;

        String name = target.getName();
        String base = name;
        String ext = "";

        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }

        int i = 1;
        File candidate;
        do {
            candidate = new File(target.getParentFile(), base + " (" + i + ")" + ext);
            i++;
        } while (candidate.exists());

        return candidate;
    }

    private void openExternalLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
        }
    }

    private String getAsString(JsonObject o, String key) {
        return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private int getAsInt(JsonObject o, String key) {
        return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : 0;
    }

    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean showConfirmationDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static class ResourcePackItem {
        private final String displayName;
        private final String fileName;
        private final Image icon;

        private ResourcePackItem(String displayName, String fileName, Image icon) {
            this.displayName = displayName;
            this.fileName = fileName;
            this.icon = icon;
        }
    }

    private static class OnlinePackItem {
        private final String slug;
        private final String title;
        private final String author;
        private final String description;
        private final String iconUrl;
        private final int downloads;

        private OnlinePackItem(String slug, String title, String author, String description, String iconUrl, int downloads) {
            this.slug = slug;
            this.title = title;
            this.author = author;
            this.description = description;
            this.iconUrl = iconUrl;
            this.downloads = downloads;
        }
    }
    private Image loadRemoteImageSafe(String url, Image fallback) {
        if (url == null || url.trim().isEmpty()) return fallback;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "MajestyCraft-Launcher/4.0");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int read;
                while ((read = in.read(chunk)) != -1) buffer.write(chunk, 0, read);
                byte[] bytes = buffer.toByteArray();
                Image img = new Image(new ByteArrayInputStream(bytes), 52, 52, true, true);
                if (!img.isError() && img.getWidth() > 1) return img;
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
                if (bi != null) {
                    Image fxImg = SwingFXUtils.toFXImage(bi, null);
                    if (fxImg != null && !fxImg.isError() && fxImg.getWidth() > 1) return fxImg;
                }
            }
        } catch (Exception ignored) {}
        return fallback;
    }

}