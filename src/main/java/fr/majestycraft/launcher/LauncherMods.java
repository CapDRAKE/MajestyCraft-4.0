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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LauncherMods extends IScreen {

    private static final int W = 1240;
    private static final int H = 840;

    private static final String MODRINTH_API_SEARCH = "https://api.modrinth.com/v2/search";
    private static final String MODRINTH_API_PROJECT_VERSIONS = "https://api.modrinth.com/v2/project/";
    private static final String MODRINTH_WEB_MOD = "https://modrinth.com/mod/";
    private static final String MODRINTH_USER_AGENT = "CapDRAKE/MajestyLauncher/4.0 (majestycraft.com)";

    private static final String LOADER_ANY = "any";
    private static final String LOADER_FORGE = "forge";
    private static final String LOADER_NEOFORGE = "neoforge";
    private static final String LOADER_FABRIC = "fabric";
    private static final String LOADER_QUILT = "quilt";

    private final LauncherPanel paneRef;
    private final List<LocalModItem> localMods = new ArrayList<>();
    private final Image fallbackIcon = new Image(getClass().getResource("/resources/mods.png").toExternalForm());
    private final DecimalFormat decimalFormat = new DecimalFormat("0.0");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);

    private ListView<LocalModItem> localListView;
    private ListView<OnlineModItem> onlineListView;

    private Pane localPane;
    private Pane onlinePane;

    private JFXButton localTabButton;
    private JFXButton onlineTabButton;
    private JFXButton addButton;
    private JFXButton refreshButton;
    private JFXButton openFolderButton;
    private LauncherButton quitButton;

    private JFXButton loaderAnyButton;
    private JFXButton loaderForgeButton;
    private JFXButton loaderNeoForgeButton;
    private JFXButton loaderFabricButton;
    private JFXButton loaderQuiltButton;

    private JFXTextField searchField;
    private JFXTextField versionField;
    private LauncherLabel onlineStatusLabel;
    private LauncherLabel localStatusLabel;
    private LauncherLabel versionHintLabel;
    private JFXButton versionApplyButton;
    private Rectangle versionPanelBackground;

    private LauncherImage heroLogo;
    private LauncherLabel heroTitle;
    private LauncherLabel heroSubtitle;
    private LauncherLabel heroLine1;
    private LauncherLabel heroLine2;
    private LauncherLabel heroLine3;
    private LauncherLabel heroLine4;

    private File modsDir;
    private String selectedLoader = LOADER_FORGE;
    private String selectedMinecraftVersion;

    private double xOffSet;
    private double yOffSet;

    public LauncherMods(final Pane root, final GameEngine engine, final LauncherPanel pane) {
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

        this.selectedMinecraftVersion = resolveInitialMinecraftVersion();

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

        this.modsDir = resolveModsDir();
        ensureModsDir();

        configureLocalList();
        configureOnlineList();

        setDiscoverMode(true);
        loadLocalMods();
        searchOnlineMods("", getSelectedMinecraftVersion(), selectedLoader);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                animateIn(heroLogo, -14, 10, 60);
                animateIn(heroTitle, -14, 10, 110);
                animateIn(heroSubtitle, -14, 10, 160);
                animateIn(heroLine1, -14, 10, 210);
                animateIn(heroLine2, -14, 10, 250);
                animateIn(heroLine3, -14, 10, 290);
                animateIn(heroLine4, -14, 10, 330);

                animateIn(localTabButton, 18, 0, 120);
                animateIn(onlineTabButton, 18, 0, 160);
                animateIn(searchField, 18, 0, 200);
                animateIn(refreshButton, 18, 0, 240);
                animateIn(loaderAnyButton, 18, 0, 280);
                animateIn(loaderForgeButton, 18, 0, 300);
                animateIn(loaderNeoForgeButton, 18, 0, 320);
                animateIn(loaderFabricButton, 18, 0, 340);
                animateIn(loaderQuiltButton, 18, 0, 360);
                animateIn(versionPanelBackground, 18, 0, 360);
                animateIn(versionHintLabel, 18, 0, 380);
                animateIn(versionField, 18, 0, 400);
                animateIn(versionApplyButton, 18, 0, 420);
                animateIn(localPane, 18, 0, 440);
                animateIn(onlinePane, 18, 0, 440);
                animateIn(addButton, 0, 12, 470);
                animateIn(openFolderButton, 0, 12, 510);
                animateIn(quitButton, 0, 12, 550);
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
        this.heroLogo.setImage(getResourceLocation().loadImage(engine, "mods.png"));
        this.heroLogo.setSize(130, 130);
        this.heroLogo.setBounds(heroX + 60, heroY + 36, 130, 130);

        this.heroTitle = new LauncherLabel(root);
        this.heroTitle.setText("Mods");
        this.heroTitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 27F));
        this.heroTitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.96)");
        this.heroTitle.setPosition(heroX, heroY + 186);
        this.heroTitle.setSize(heroW, 34);
        this.heroTitle.setAlignment(Pos.CENTER);

        this.heroSubtitle = new LauncherLabel(root);
        this.heroSubtitle.setText("Gestion locale + catalogue en ligne");
        this.heroSubtitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        this.heroSubtitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.96)");
        this.heroSubtitle.setPosition(heroX, heroY + 220);
        this.heroSubtitle.setSize(heroW, 22);
        this.heroSubtitle.setAlignment(Pos.CENTER);

        this.heroLine1 = createHeroLine(root, heroX, heroY + 300, heroW, "• Liste les mods installés");
        this.heroLine2 = createHeroLine(root, heroX, heroY + 330, heroW, "• Ajoute des .jar ou .zip depuis ton PC");
        this.heroLine3 = createHeroLine(root, heroX, heroY + 360, heroW, "• Recherche et télécharge via Modrinth");
        this.heroLine4 = createHeroLine(root, heroX, heroY + 390, heroW, "• Active, désactive ou supprime un mod");
    }

    private LauncherLabel createHeroLine(Pane root, int heroX, int y, int heroW, String text) {
        LauncherLabel label = new LauncherLabel(root);
        label.setText(text);
        label.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        label.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.62)");
        label.setPosition(heroX + 22, y);
        label.setSize(heroW - 44, 20);
        return label;
    }

    private void buildMain(final Pane root) {
        final int mainX = 392;
        final int mainY = 54;
        final int mainW = 770;
        final int mainH = 690;

        LauncherRectangle mainCard = new LauncherRectangle(root, mainX, mainY, mainW, mainH);
        styleCard(mainCard, 0.76);

        LauncherLabel titleLabel = new LauncherLabel(root);
        titleLabel.setText("Gestionnaire de mods");
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        titleLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        titleLabel.setPosition(mainX, mainY + 22);
        titleLabel.setSize(mainW, 34);
        titleLabel.setAlignment(Pos.CENTER);

        LauncherLabel subTitleLabel = new LauncherLabel(root);
        subTitleLabel.setText("Un panneau façon CurseForge pour rechercher, installer et gérer tes mods");
        subTitleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        subTitleLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.45);");
        subTitleLabel.setPosition(mainX, mainY + 54);
        subTitleLabel.setSize(mainW, 18);
        subTitleLabel.setAlignment(Pos.CENTER);

        this.localTabButton = new JFXButton("Installés");
        styleTabButton(this.localTabButton, false);
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

        this.searchField = new JFXTextField();
        this.searchField.setPromptText("Rechercher un mod...");
        this.searchField.setPrefWidth(204);
        this.searchField.setLayoutX(mainX + mainW - 360);
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
        this.searchField.setOnAction(event -> performSearch());
        root.getChildren().add(this.searchField);

        this.refreshButton = new JFXButton("Rechercher");
        styleSecondaryButton(this.refreshButton);
        this.refreshButton.setLayoutX(mainX + mainW - 144);
        this.refreshButton.setLayoutY(mainY + 98);
        this.refreshButton.setPrefWidth(110);
        this.refreshButton.setOnAction(event -> performSearch());
        root.getChildren().add(this.refreshButton);

        int loaderY = mainY + 148;
        this.loaderAnyButton = createLoaderButton(root, mainX + 34, loaderY, 68, "Tous", LOADER_ANY);
        this.loaderForgeButton = createLoaderButton(root, mainX + 112, loaderY, 78, "Forge", LOADER_FORGE);
        this.loaderNeoForgeButton = createLoaderButton(root, mainX + 200, loaderY, 104, "NeoForge", LOADER_NEOFORGE);
        this.loaderFabricButton = createLoaderButton(root, mainX + 314, loaderY, 82, "Fabric", LOADER_FABRIC);
        this.loaderQuiltButton = createLoaderButton(root, mainX + 406, loaderY, 74, "Quilt", LOADER_QUILT);
        applyLoaderState();

        this.versionPanelBackground = new Rectangle();
        this.versionPanelBackground.setX(mainX + mainW - 258);
        this.versionPanelBackground.setY(mainY + 134);
        this.versionPanelBackground.setWidth(224);
        this.versionPanelBackground.setHeight(54);
        this.versionPanelBackground.setArcWidth(22);
        this.versionPanelBackground.setArcHeight(22);
        this.versionPanelBackground.setFill(Color.rgb(255, 255, 255, 0.05));
        this.versionPanelBackground.setStroke(Color.rgb(255, 255, 255, 0.10));
        this.versionPanelBackground.setStrokeWidth(1);
        root.getChildren().add(this.versionPanelBackground);

        this.versionHintLabel = new LauncherLabel(root);
        this.versionHintLabel.setText("Version Minecraft");
        this.versionHintLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10.5F));
        this.versionHintLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.92);");
        this.versionHintLabel.setPosition(mainX + mainW - 244, mainY + 141);
        this.versionHintLabel.setSize(188, 14);
        this.versionHintLabel.setAlignment(Pos.CENTER_LEFT);

        this.versionField = new JFXTextField();
        this.versionField.setText(getSelectedMinecraftVersion());
        this.versionField.setPromptText("1.21.1");
        this.versionField.setPrefWidth(118);
        this.versionField.setLayoutX(mainX + mainW - 244);
        this.versionField.setLayoutY(mainY + 156);
        this.versionField.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.versionField.setStyle(
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
        this.versionField.setOnAction(event -> applyTargetVersion());
        root.getChildren().add(this.versionField);

        this.versionApplyButton = new JFXButton("Valider");
        styleSecondaryButton(this.versionApplyButton);
        this.versionApplyButton.setLayoutX(mainX + mainW - 118);
        this.versionApplyButton.setLayoutY(mainY + 156);
        this.versionApplyButton.setPrefWidth(84);
        this.versionApplyButton.setOnAction(event -> applyTargetVersion());
        root.getChildren().add(this.versionApplyButton);

        this.localPane = new Pane();
        this.localPane.setLayoutX(mainX + 28);
        this.localPane.setLayoutY(mainY + 202);
        this.localPane.setPrefSize(mainW - 72, 373);
        root.getChildren().add(this.localPane);

        this.onlinePane = new Pane();
        this.onlinePane.setLayoutX(mainX + 28);
        this.onlinePane.setLayoutY(mainY + 202);
        this.onlinePane.setPrefSize(mainW - 72, 373);
        root.getChildren().add(this.onlinePane);

        this.localListView = new ListView<LocalModItem>();
        this.localListView.setPrefSize(mainW - 72, 373);
        this.localListView.setStyle(
                "-fx-background-color: rgba(0,0,0,0);" +
                "-fx-control-inner-background: rgba(255,255,255,0.04);" +
                "-fx-background-insets: 0;" +
                "-fx-background-radius: 18;"
        );
        this.localPane.getChildren().add(this.localListView);

        this.onlineListView = new ListView<OnlineModItem>();
        this.onlineListView.setPrefSize(mainW - 72, 373);
        this.onlineListView.setStyle(
                "-fx-background-color: rgba(0,0,0,0);" +
                "-fx-control-inner-background: rgba(255,255,255,0.04);" +
                "-fx-background-insets: 0;" +
                "-fx-background-radius: 18;"
        );
        this.onlinePane.getChildren().add(this.onlineListView);

        this.localStatusLabel = new LauncherLabel(root);
        this.localStatusLabel.setText("");
        this.localStatusLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.localStatusLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.52);");
        this.localStatusLabel.setPosition(mainX + 34, mainY + mainH - 82);
        this.localStatusLabel.setSize(mainW - 300, 18);

        this.onlineStatusLabel = new LauncherLabel(root);
        this.onlineStatusLabel.setText("");
        this.onlineStatusLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.onlineStatusLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.52);");
        this.onlineStatusLabel.setPosition(mainX + 34, mainY + mainH - 82);
        this.onlineStatusLabel.setSize(mainW - 300, 18);

        this.addButton = new JFXButton("Ajouter mod / zip");
        styleSecondaryButton(this.addButton);
        this.addButton.setLayoutX(mainX + 34);
        this.addButton.setLayoutY(mainY + mainH - 54);
        this.addButton.setPrefWidth(150);
        this.addButton.setOnAction(event -> addLocalMod());
        root.getChildren().add(this.addButton);

        this.openFolderButton = new JFXButton("Ouvrir dossier");
        styleSecondaryButton(this.openFolderButton);
        this.openFolderButton.setLayoutX(mainX + 198);
        this.openFolderButton.setLayoutY(mainY + mainH - 54);
        this.openFolderButton.setPrefWidth(150);
        this.openFolderButton.setOnAction(event -> openModsFolder());
        root.getChildren().add(this.openFolderButton);

        this.quitButton = new LauncherButton(root);
        this.quitButton.setText("Retour");
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

    private JFXButton createLoaderButton(Pane root, int x, int y, int w, String text, final String loader) {
        JFXButton button = new JFXButton(text);
        button.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        button.setLayoutX(x);
        button.setLayoutY(y);
        button.setPrefWidth(w);
        button.setOnAction(event -> {
            selectedLoader = loader;
            applyLoaderState();
            if (isDiscoverMode()) {
                searchOnlineMods(searchField.getText(), getSelectedMinecraftVersion(), selectedLoader);
            }
        });
        root.getChildren().add(button);
        return button;
    }

    private void setDiscoverMode(boolean discover) {
        onlinePane.setVisible(discover);
        onlinePane.setManaged(discover);
        onlineStatusLabel.setVisible(discover);
        onlineStatusLabel.setManaged(discover);

        localPane.setVisible(!discover);
        localPane.setManaged(!discover);
        localStatusLabel.setVisible(!discover);
        localStatusLabel.setManaged(!discover);
        addButton.setVisible(true);
        addButton.setManaged(true);

        loaderAnyButton.setVisible(discover);
        loaderForgeButton.setVisible(discover);
        loaderNeoForgeButton.setVisible(discover);
        loaderFabricButton.setVisible(discover);
        loaderQuiltButton.setVisible(discover);
        if (versionPanelBackground != null) versionPanelBackground.setVisible(discover);
        versionHintLabel.setVisible(discover);
        versionField.setVisible(discover);
        versionField.setManaged(discover);
        versionApplyButton.setVisible(discover);
        versionApplyButton.setManaged(discover);

        searchField.setPromptText(discover ? "Rechercher un mod..." : "Filtrer les mods installés...");
        refreshButton.setText(discover ? "Rechercher" : "Filtrer");

        applyTabState(this.localTabButton, !discover);
        applyTabState(this.onlineTabButton, discover);

        if (!discover) {
            filterLocalMods(searchField.getText());
        }
    }

    private boolean isDiscoverMode() {
        return onlinePane != null && onlinePane.isVisible();
    }

    private void performSearch() {
        if (isDiscoverMode()) {
            searchOnlineMods(searchField.getText(), getSelectedMinecraftVersion(), selectedLoader);
        } else {
            filterLocalMods(searchField.getText());
        }
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

    private void applyLoaderState() {
        styleLoaderButton(loaderAnyButton, LOADER_ANY.equals(selectedLoader));
        styleLoaderButton(loaderForgeButton, LOADER_FORGE.equals(selectedLoader));
        styleLoaderButton(loaderNeoForgeButton, LOADER_NEOFORGE.equals(selectedLoader));
        styleLoaderButton(loaderFabricButton, LOADER_FABRIC.equals(selectedLoader));
        styleLoaderButton(loaderQuiltButton, LOADER_QUILT.equals(selectedLoader));
    }

    private void styleLoaderButton(JFXButton button, boolean active) {
        if (button == null) return;
        if (active) {
            button.setStyle(
                    "-fx-background-radius: 14;" +
                    "-fx-text-fill: white;" +
                    "-fx-background-color: rgba(255,152,0,0.28);" +
                    "-fx-border-color: rgba(255,176,0,0.50);" +
                    "-fx-border-radius: 14;" +
                    "-fx-border-width: 1;"
            );
        } else {
            button.setStyle(
                    "-fx-background-radius: 14;" +
                    "-fx-text-fill: rgba(255,255,255,0.86);" +
                    "-fx-background-color: rgba(255,255,255,0.06);" +
                    "-fx-border-color: rgba(255,255,255,0.10);" +
                    "-fx-border-radius: 14;" +
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

    private File resolveModsDir() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new File(System.getenv("APPDATA") + "/.majestycraft/bin/game/mods");
        } else if (os.contains("mac")) {
            return new File(System.getProperty("user.home") + "/Library/Application Support/.majestycraft/bin/game/mods");
        } else {
            return new File(System.getProperty("user.home") + "/.majestycraft/bin/game/mods");
        }
    }

    private void ensureModsDir() {
        if (!modsDir.exists()) {
            modsDir.mkdirs();
        }
    }

    private void loadLocalMods() {
        localMods.clear();

        if (modsDir.exists() && modsDir.isDirectory()) {
            File[] files = modsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!isModFile(file)) continue;
                    localMods.add(createLocalModItem(file));
                }
            }
        }

        Collections.sort(localMods, new Comparator<LocalModItem>() {
            @Override
            public int compare(LocalModItem o1, LocalModItem o2) {
                if (o1.enabled != o2.enabled) {
                    return o1.enabled ? -1 : 1;
                }
                return o1.displayName.compareToIgnoreCase(o2.displayName);
            }
        });

        filterLocalMods(searchField != null ? searchField.getText() : "");
    }

    private void filterLocalMods(String query) {
        if (localListView == null) return;

        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        localListView.getItems().clear();

        int enabledCount = 0;
        int shownCount = 0;
        for (LocalModItem item : localMods) {
            if (item.enabled) enabledCount++;

            String haystack = (item.displayName + " " + item.fileName + " " + item.loaderLabel).toLowerCase(Locale.ROOT);
            if (q.isEmpty() || haystack.contains(q)) {
                localListView.getItems().add(item);
                shownCount++;
            }
        }

        localStatusLabel.setText(shownCount + " mod(s) affiché(s) • " + enabledCount + " actif(s) • " + shortenPath(modsDir, 34));
    }

    private boolean isModFile(File file) {
        if (file == null || !file.isFile()) return false;
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jar") || name.endsWith(".jar.disabled");
    }

    private LocalModItem createLocalModItem(File file) {
        boolean enabled = file.getName().toLowerCase(Locale.ROOT).endsWith(".jar");
        ModMetadata metadata = readLocalMetadata(file);
        String cleanFileName = stripDisabledSuffix(file.getName());
        String displayName = metadata.name != null && !metadata.name.trim().isEmpty() ? metadata.name.trim() : stripJarSuffix(cleanFileName);
        String loader = metadata.loader != null ? metadata.loader : detectLoaderFromFilename(cleanFileName);
        String loaderLabel = formatLoader(loader);

        return new LocalModItem(
                file,
                displayName,
                cleanFileName,
                enabled,
                loaderLabel,
                file.length(),
                dateFormat.format(new java.util.Date(file.lastModified()))
        );
    }

    private String stripDisabledSuffix(String name) {
        if (name == null) return "";
        if (name.toLowerCase(Locale.ROOT).endsWith(".disabled")) {
            return name.substring(0, name.length() - 9);
        }
        return name;
    }

    private String stripJarSuffix(String name) {
        if (name == null) return "";
        if (name.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private ModMetadata readLocalMetadata(File file) {
        ModMetadata metadata = new ModMetadata();
        ZipFile zipFile = null;

        try {
            zipFile = new ZipFile(file);

            ZipEntry neoforgeEntry = zipFile.getEntry("META-INF/neoforge.mods.toml");
            if (neoforgeEntry != null) {
                String toml = readZipEntry(zipFile, neoforgeEntry);
                metadata.name = extractTomlValue(toml, "displayName");
                if (metadata.name == null) metadata.name = extractTomlValue(toml, "modId");
                metadata.loader = LOADER_NEOFORGE;
                return metadata;
            }

            ZipEntry forgeEntry = zipFile.getEntry("META-INF/mods.toml");
            if (forgeEntry != null) {
                String toml = readZipEntry(zipFile, forgeEntry);
                metadata.name = extractTomlValue(toml, "displayName");
                if (metadata.name == null) metadata.name = extractTomlValue(toml, "modId");
                metadata.loader = LOADER_FORGE;
                return metadata;
            }

            ZipEntry fabricEntry = zipFile.getEntry("fabric.mod.json");
            if (fabricEntry != null) {
                String json = readZipEntry(zipFile, fabricEntry);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                metadata.name = getAsString(obj, "name");
                if (metadata.name == null) metadata.name = getAsString(obj, "id");
                metadata.loader = LOADER_FABRIC;
                return metadata;
            }

            ZipEntry quiltEntry = zipFile.getEntry("quilt.mod.json");
            if (quiltEntry != null) {
                String json = readZipEntry(zipFile, quiltEntry);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                JsonObject quiltLoader = obj.has("quilt_loader") && obj.get("quilt_loader").isJsonObject() ? obj.getAsJsonObject("quilt_loader") : null;
                JsonObject metadataObj = quiltLoader != null && quiltLoader.has("metadata") && quiltLoader.get("metadata").isJsonObject() ? quiltLoader.getAsJsonObject("metadata") : null;
                metadata.name = metadataObj != null ? getAsString(metadataObj, "name") : null;
                if (metadata.name == null && quiltLoader != null) metadata.name = getAsString(quiltLoader, "id");
                metadata.loader = LOADER_QUILT;
                return metadata;
            }
        } catch (Exception ignored) {
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ignored) {
                }
            }
        }

        metadata.loader = null;
        metadata.name = null;
        return metadata;
    }

    private String readZipEntry(ZipFile zipFile, ZipEntry entry) throws IOException {
        InputStream is = zipFile.getInputStream(entry);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
        }
    }

    private String extractTomlValue(String toml, String key) {
        if (toml == null || key == null) return null;
        String[] lines = toml.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || !trimmed.startsWith(key)) continue;
            int idx = trimmed.indexOf('=');
            if (idx < 0) continue;
            String value = trimmed.substring(idx + 1).trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
            return value;
        }
        return null;
    }

    private String detectLoaderFromFilename(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.contains("neoforge")) return LOADER_NEOFORGE;
        if (lower.contains("forge")) return LOADER_FORGE;
        if (lower.contains("fabric")) return LOADER_FABRIC;
        if (lower.contains("quilt")) return LOADER_QUILT;
        return LOADER_ANY;
    }

    private void addLocalMod() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un mod ou un pack zip");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Mods Minecraft", "*.jar", "*.zip"));

        Stage currentStage = null;
        if (addButton.getScene() != null) {
            currentStage = (Stage) addButton.getScene().getWindow();
        }

        File selectedFile = fileChooser.showOpenDialog(currentStage);
        if (selectedFile != null) {
            try {
                String lowerName = selectedFile.getName().toLowerCase(Locale.ROOT);
                if (lowerName.endsWith(".zip")) {
                    int imported = importModsFromZip(selectedFile);
                    loadLocalMods();
                    if (imported <= 0) {
                        showErrorDialog("Archive vide", "Aucun .jar trouvé dans cette archive.");
                    } else {
                        showInfoDialog("Import terminé", imported + " mod(s) ont été ajoutés depuis l'archive.");
                    }
                } else {
                    File destFile = uniqueDestination(new File(modsDir, selectedFile.getName()));
                    Files.copy(selectedFile.toPath(), destFile.toPath());
                    loadLocalMods();
                    showInfoDialog("Mod ajouté", destFile.getName() + " a bien été ajouté.");
                }
            } catch (IOException e) {
                showErrorDialog("Erreur", "Impossible d'ajouter ce mod ou cette archive.");
            }
        }
    }

    private int importModsFromZip(File zipArchive) throws IOException {
        int imported = 0;

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zipArchive);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry == null || entry.isDirectory()) continue;

                String entryName = entry.getName();
                String lower = entryName == null ? "" : entryName.toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".jar") && !lower.endsWith(".jar.disabled")) {
                    continue;
                }

                String fileName = new File(entryName).getName();
                if (fileName.trim().isEmpty()) continue;

                File destination = uniqueDestination(new File(modsDir, fileName));
                InputStream is = null;
                OutputStream os = null;
                try {
                    is = zipFile.getInputStream(entry);
                    os = new FileOutputStream(destination);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    imported++;
                } finally {
                    if (os != null) {
                        try { os.close(); } catch (IOException ignored) {}
                    }
                    if (is != null) {
                        try { is.close(); } catch (IOException ignored) {}
                    }
                }
            }
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ignored) {
                }
            }
        }

        return imported;
    }

    private void openModsFolder() {
        try {
            if (!modsDir.exists()) modsDir.mkdirs();
            Desktop.getDesktop().open(modsDir);
        } catch (IOException e) {
            showErrorDialog("Erreur", "Impossible d'ouvrir le dossier des mods.");
        }
    }

    private void toggleLocalMod(LocalModItem item) {
        String currentName = item.file.getName();
        String targetName;

        if (item.enabled) {
            targetName = currentName + ".disabled";
        } else {
            targetName = stripDisabledSuffix(currentName);
        }

        File target = new File(item.file.getParentFile(), targetName);
        if (target.exists()) {
            showErrorDialog("Conflit", "Un fichier avec ce nom existe déjà dans le dossier mods.");
            return;
        }

        if (!item.file.renameTo(target)) {
            showErrorDialog("Erreur", "Impossible de modifier l'état de ce mod.");
            return;
        }

        loadLocalMods();
    }

    private void deleteLocalMod(LocalModItem item) {
        if (!showConfirmationDialog("Suppression", "Supprimer définitivement ce mod du dossier ?")) {
            return;
        }

        if (!item.file.delete()) {
            showErrorDialog("Erreur", "Impossible de supprimer ce mod.");
            return;
        }

        loadLocalMods();
    }

    private void configureLocalList() {
        localListView.setCellFactory(param -> new ListCell<LocalModItem>() {
            private final ImageView iconView = new ImageView(fallbackIcon);
            private final Label titleLabel = new Label();
            private final Label metaLabel = new Label();
            private final Label fileLabel = new Label();
            private final VBox textBox = new VBox(titleLabel, metaLabel, fileLabel);
            private final JFXButton toggleButton = new JFXButton();
            private final JFXButton deleteButton = new JFXButton("Supprimer");
            private final HBox actionsBox = new HBox(8, toggleButton, deleteButton);
            private final HBox row = new HBox(14, iconView, textBox, actionsBox);

            {
                iconView.setFitWidth(42);
                iconView.setFitHeight(42);

                titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
                titleLabel.setStyle("-fx-text-fill: white;");

                metaLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
                metaLabel.setStyle("-fx-text-fill: rgba(255,176,0,0.92);");

                fileLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
                fileLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.56);");

                stylePillButton(toggleButton, false);
                styleDangerButton(deleteButton);

                HBox.setHgrow(textBox, Priority.ALWAYS);
                textBox.setPrefWidth(430);
                textBox.setSpacing(3);
                actionsBox.setAlignment(Pos.CENTER_RIGHT);
                row.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(LocalModItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                titleLabel.setText(item.displayName);
                metaLabel.setText((item.enabled ? "Actif" : "Inactif") + " • " + item.loaderLabel + " • " + readableSize(item.size));
                fileLabel.setText(item.fileName + " • modifié le " + item.modifiedAt);

                toggleButton.setText(item.enabled ? "Désactiver" : "Activer");
                stylePillButton(toggleButton, item.enabled);
                toggleButton.setOnAction(event -> toggleLocalMod(item));
                deleteButton.setOnAction(event -> deleteLocalMod(item));

                setStyle("-fx-background-color: transparent;");
                setGraphic(row);
            }
        });
    }

    private void configureOnlineList() {
        onlineListView.setCellFactory(param -> new ListCell<OnlineModItem>() {
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
                descLabel.setWrapText(true);

                statsLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
                statsLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.46);");

                stylePillButton(openButton, false);
                downloadButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
                downloadButton.setStyle(
                        "-fx-background-color: linear-gradient(to right, #ff9800, #ff6d00);" +
                        "-fx-background-radius: 14;" +
                        "-fx-text-fill: white;"
                );

                textBox.setPrefWidth(430);
                textBox.setSpacing(3);
                actionsBox.setSpacing(8);
                actionsBox.setPrefWidth(110);
                actionsBox.setAlignment(Pos.CENTER);
                row.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(OnlineModItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (item.iconUrl != null && !item.iconUrl.isEmpty()) {
                    iconView.setImage(new Image(item.iconUrl, true));
                } else {
                    iconView.setImage(fallbackIcon);
                }

                titleLabel.setText(item.title);
                authorLabel.setText("par " + item.author);
                descLabel.setText(item.description == null || item.description.trim().isEmpty() ? "Aucune description." : item.description);
                statsLabel.setText(item.loaderLabel + " • " + item.downloads + " téléchargements");

                openButton.setOnAction(event -> openExternalLink(MODRINTH_WEB_MOD + item.slug));
                downloadButton.setOnAction(event -> downloadOnlineMod(item));

                setStyle("-fx-background-color: transparent;");
                setGraphic(row);
            }
        });
    }

    private void stylePillButton(JFXButton button, boolean enabledAccent) {
        button.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        if (enabledAccent) {
            button.setStyle(
                    "-fx-background-color: rgba(255,176,0,0.18);" +
                    "-fx-background-radius: 14;" +
                    "-fx-text-fill: white;" +
                    "-fx-border-color: rgba(255,176,0,0.24);" +
                    "-fx-border-radius: 14;"
            );
        } else {
            button.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.10);" +
                    "-fx-background-radius: 14;" +
                    "-fx-text-fill: white;" +
                    "-fx-border-color: rgba(255,255,255,0.10);" +
                    "-fx-border-radius: 14;"
            );
        }
    }

    private void styleDangerButton(JFXButton button) {
        button.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        button.setStyle(
                "-fx-background-color: rgba(255,80,80,0.18);" +
                "-fx-background-radius: 14;" +
                "-fx-text-fill: white;" +
                "-fx-border-color: rgba(255,255,255,0.10);" +
                "-fx-border-radius: 14;"
        );
    }

    private String readableSize(long bytes) {
        double mb = bytes / 1024.0D / 1024.0D;
        if (mb >= 1.0D) {
            return decimalFormat.format(mb) + " Mo";
        }
        double kb = bytes / 1024.0D;
        return decimalFormat.format(kb) + " Ko";
    }

    private void searchOnlineMods(final String query, final String mcVersion, final String loader) {
        onlineStatusLabel.setText("Recherche en cours...");
        onlineListView.getItems().clear();

        Thread searchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String facets = "[[\"project_type:mod\"],[\"versions:" + escapeFacetValue(mcVersion) + "\"]]";
                    if (loader != null && !LOADER_ANY.equals(loader)) {
                        facets = "[[\"project_type:mod\"],[\"versions:" + escapeFacetValue(mcVersion) + "\"],[\"categories:" + escapeFacetValue(loader) + "\"]]";
                    }

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
                            String projectId = getAsString(obj, "project_id");
                            String slug = getAsString(obj, "slug");
                            String title = getAsString(obj, "title");
                            String author = getAsString(obj, "author");
                            String description = getAsString(obj, "description");
                            String iconUrl = getAsString(obj, "icon_url");
                            int downloads = getAsInt(obj, "downloads");
                            String loaderLabel = extractOnlineLoaderLabel(obj.getAsJsonArray("categories"));

                            final OnlineModItem item = new OnlineModItem(projectId, slug, title, author, description, iconUrl, downloads, loaderLabel);
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
                                onlineStatusLabel.setText("Aucun mod trouvé pour " + mcVersion + " en " + formatLoader(loader) + ".");
                            } else {
                                onlineStatusLabel.setText(onlineListView.getItems().size() + " mod(s) trouvés pour " + mcVersion + " en " + formatLoader(loader) + ".");
                            }
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            onlineStatusLabel.setText("Impossible de charger les mods en ligne.");
                        }
                    });
                }
            }
        }, "MajestyLauncher-ModsSearch");

        searchThread.setDaemon(true);
        searchThread.start();
    }

    private String extractOnlineLoaderLabel(JsonArray categories) {
        if (categories == null) return "Loader inconnu";

        boolean forge = false;
        boolean neoforge = false;
        boolean fabric = false;
        boolean quilt = false;

        for (JsonElement el : categories) {
            String value = el.getAsString();
            if (LOADER_FORGE.equalsIgnoreCase(value)) forge = true;
            if (LOADER_NEOFORGE.equalsIgnoreCase(value)) neoforge = true;
            if (LOADER_FABRIC.equalsIgnoreCase(value)) fabric = true;
            if (LOADER_QUILT.equalsIgnoreCase(value)) quilt = true;
        }

        List<String> labels = new ArrayList<String>();
        if (forge) labels.add("Forge");
        if (neoforge) labels.add("NeoForge");
        if (fabric) labels.add("Fabric");
        if (quilt) labels.add("Quilt");

        if (labels.isEmpty()) return "Loader inconnu";
        return join(labels, " / ");
    }

    private void downloadOnlineMod(final OnlineModItem item) {
        onlineStatusLabel.setText("Téléchargement de " + item.title + "...");

        Thread downloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String mcVersion = getSelectedMinecraftVersion();
                    String versionsUrl = MODRINTH_API_PROJECT_VERSIONS + item.projectId + "/version"
                            + "?game_versions=" + URLEncoder.encode("[\"" + mcVersion + "\"]", "UTF-8");

                    if (selectedLoader != null && !LOADER_ANY.equals(selectedLoader)) {
                        versionsUrl += "&loaders=" + URLEncoder.encode("[\"" + selectedLoader + "\"]", "UTF-8");
                    }

                    String json = downloadText(versionsUrl);
                    JsonArray versions = JsonParser.parseString(json).getAsJsonArray();

                    if (versions == null || versions.size() == 0) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                showErrorDialog("Aucun fichier", "Aucune version compatible trouvée pour " + mcVersion + ".");
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
                                showErrorDialog("Aucun fichier", "Aucun fichier téléchargeable trouvé.");
                            }
                        });
                        return;
                    }

                    JsonObject chosenFile = null;
                    for (JsonElement fileEl : files) {
                        JsonObject fileObj = fileEl.getAsJsonObject();
                        String filename = getAsString(fileObj, "filename");
                        boolean primary = fileObj.has("primary") && fileObj.get("primary").getAsBoolean();
                        if (primary && filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                            chosenFile = fileObj;
                            break;
                        }
                    }
                    if (chosenFile == null) {
                        for (JsonElement fileEl : files) {
                            JsonObject fileObj = fileEl.getAsJsonObject();
                            String filename = getAsString(fileObj, "filename");
                            if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".jar")) {
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
                                showErrorDialog("Erreur", "Impossible de récupérer le fichier du mod.");
                            }
                        });
                        return;
                    }

                    File destination = uniqueDestination(new File(modsDir, fileName));
                    downloadFile(fileUrl, destination);

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            loadLocalMods();
                            onlineStatusLabel.setText(item.title + " téléchargé.");
                            showInfoDialog("Téléchargement terminé", item.title + " a été ajouté dans le dossier mods.");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            onlineStatusLabel.setText("Échec du téléchargement.");
                            showErrorDialog("Erreur", "Impossible de télécharger ce mod.");
                        }
                    });
                }
            }
        }, "MajestyLauncher-ModDownload");

        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    private void applyTargetVersion() {
        if (versionField == null) return;

        String raw = versionField.getText() == null ? "" : versionField.getText().trim();
        if (raw.isEmpty()) {
            versionField.setText(getSelectedMinecraftVersion());
            return;
        }

        if (!raw.matches("\\d+(\\.\\d+){1,2}")) {
            showErrorDialog("Version invalide", "Entre une version du style 1.20.1, 1.19.2 ou 1.21.1.");
            versionField.setText(getSelectedMinecraftVersion());
            return;
        }

        selectedMinecraftVersion = raw;
        versionField.setText(raw);

        if (isDiscoverMode()) {
            searchOnlineMods(searchField.getText(), selectedMinecraftVersion, selectedLoader);
        }
    }

    private String resolveInitialMinecraftVersion() {
        Object value = paneRef.getConfig().getValue(EnumConfig.VERSION);
        if (value == null) return "1.21.11";
        return String.valueOf(value);
    }

    private String getSelectedMinecraftVersion() {
        if (selectedMinecraftVersion == null || selectedMinecraftVersion.trim().isEmpty()) {
            selectedMinecraftVersion = resolveInitialMinecraftVersion();
        }
        return selectedMinecraftVersion;
    }

    private String shortenPath(File file, int maxLen) {
        if (file == null) return "";
        String path = file.getAbsolutePath();
        if (path.length() <= maxLen) return path;
        return "..." + path.substring(path.length() - maxLen);
    }

    private String escapeFacetValue(String s) {
        return s == null ? "" : s.replace("\"", "");
    }

    private String formatLoader(String loader) {
        if (loader == null || loader.trim().isEmpty() || LOADER_ANY.equalsIgnoreCase(loader)) return "Tous loaders";
        if (LOADER_FORGE.equalsIgnoreCase(loader)) return "Forge";
        if (LOADER_NEOFORGE.equalsIgnoreCase(loader)) return "NeoForge";
        if (LOADER_FABRIC.equalsIgnoreCase(loader)) return "Fabric";
        if (LOADER_QUILT.equalsIgnoreCase(loader)) return "Quilt";
        return loader;
    }

    private String join(List<String> values, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(separator);
            sb.append(values.get(i));
        }
        return sb.toString();
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

            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            try {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);

                if (code < 200 || code >= 300) {
                    throw new IOException("HTTP " + code + " -> " + sb.toString());
                }
                return sb.toString();
            } finally {
                try { br.close(); } catch (Exception ignored) {}
            }
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
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

            InputStream in = new BufferedInputStream(connection.getInputStream());
            OutputStream out = new FileOutputStream(destination);
            try {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } finally {
                try { in.close(); } catch (Exception ignored) {}
                try { out.close(); } catch (Exception ignored) {}
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

    private static class ModMetadata {
        private String name;
        private String loader;
    }

    private static class LocalModItem {
        private final File file;
        private final String displayName;
        private final String fileName;
        private final boolean enabled;
        private final String loaderLabel;
        private final long size;
        private final String modifiedAt;

        private LocalModItem(File file, String displayName, String fileName, boolean enabled, String loaderLabel, long size, String modifiedAt) {
            this.file = file;
            this.displayName = displayName;
            this.fileName = fileName;
            this.enabled = enabled;
            this.loaderLabel = loaderLabel;
            this.size = size;
            this.modifiedAt = modifiedAt;
        }
    }

    private static class OnlineModItem {
        private final String projectId;
        private final String slug;
        private final String title;
        private final String author;
        private final String description;
        private final String iconUrl;
        private final int downloads;
        private final String loaderLabel;

        private OnlineModItem(String projectId, String slug, String title, String author, String description, String iconUrl, int downloads, String loaderLabel) {
            this.projectId = projectId;
            this.slug = slug;
            this.title = title;
            this.author = author;
            this.description = description;
            this.iconUrl = iconUrl;
            this.downloads = downloads;
            this.loaderLabel = loaderLabel;
        }
    }
}
