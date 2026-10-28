package fr.majestycraft.launcher;

import fr.majestycraft.util.Icons;
import fr.majestycraft.util.UiStyle;
import fr.majestycraft.util.AmbientBackground;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import fr.majestycraft.App;
import fr.majestycraft.Utils;
import fr.majestycraft.profiles.LauncherProfile;
import fr.majestycraft.profiles.ProfileManager;
import fr.trxyy.alternative.alternative_api.GameEngine;
import fr.trxyy.alternative.alternative_api_ui.base.IScreen;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherImage;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherLabel;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherRectangle;
import fr.trxyy.alternative.alternative_api.utils.FontLoader;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestionnaire de profils de jeu : créer, activer, dupliquer, renommer, supprimer.
 * La modification fine du profil actif (version/modloader/RAM/arguments) se fait
 * dans l'écran Paramètres, qui édite toujours le profil actif.
 */
public class LauncherProfiles extends IScreen {

    private static final int W = 1000;
    private static final int H = 760;

    private final GameEngine engine;
    private final LauncherPanel panel;

    private VBox listBox;
    private JFXTextField nameField;
    private JFXComboBox<String> versionCombo;
    private JFXComboBox<String> loaderCombo;
    private JFXButton createButton;

    /** Couleur sélectionnée pour le nouveau profil + pastilles de sélection. */
    private String selectedColor = ProfileManager.COLOR_PALETTE[0];
    private final java.util.List<javafx.scene.shape.Circle> colorSwatches = new java.util.ArrayList<>();

    private double xOffSet;
    private double yOffSet;

    public LauncherProfiles(final Pane root, final GameEngine engine, final LauncherPanel panel) {
        this.engine = engine;
        this.panel = panel;

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


        LauncherRectangle leftDock = new LauncherRectangle(root, 0, 0, 84, H);
        leftDock.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(8, 12, 18, 0.84)),
                new Stop(1, Color.rgb(8, 12, 18, 0.28))
        ));

        LauncherRectangle separator = new LauncherRectangle(root, 83, 0, 1, H);
        separator.setFill(Color.rgb(255, 255, 255, 0.08));

        LauncherImage heroLogo = new LauncherImage(root);
        heroLogo.setImage(getResourceLocation().loadImage(engine, "launchergifpng.png"));
        heroLogo.setSize(150, 150);
        heroLogo.setBounds(128, 90, 150, 150);

        LauncherLabel heroTitle = new LauncherLabel(root);
        heroTitle.setText("Profils");
        heroTitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        heroTitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.96)");
        heroTitle.setPosition(118, 270);
        heroTitle.setSize(260, 40);

        LauncherLabel heroSubtitle = new LauncherLabel(root);
        heroSubtitle.setText("Un profil par façon de jouer");
        heroSubtitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        heroSubtitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.96)");
        heroSubtitle.setPosition(120, 314);
        heroSubtitle.setSize(280, 24);

        LauncherLabel heroLine1 = new LauncherLabel(root);
        heroLine1.setText("• Chaque profil a ses mods et configs");
        heroLine1.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        heroLine1.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.70)");
        heroLine1.setPosition(120, 362);
        heroLine1.setSize(280, 20);

        LauncherLabel heroLine2 = new LauncherLabel(root);
        heroLine2.setText("• Version et modloader au choix");
        heroLine2.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        heroLine2.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.58)");
        heroLine2.setPosition(120, 388);
        heroLine2.setSize(280, 20);

        LauncherLabel heroLine3 = new LauncherLabel(root);
        heroLine3.setText("• Réglages fins : écran Paramètres");
        heroLine3.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        heroLine3.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.46)");
        heroLine3.setPosition(120, 414);
        heroLine3.setSize(280, 20);

        final int cardW = 565;
        final int cardH = 660;
        final int cardX = 360;
        final int cardY = 50;

        LauncherRectangle card = new LauncherRectangle(root, cardX, cardY, cardW, cardH);
        card.setArcWidth(34);
        card.setArcHeight(34);
        card.setFill(Color.rgb(8, 12, 18, 0.74));
        card.setStroke(Color.rgb(255, 255, 255, 0.10));
        card.setStrokeWidth(1);
        card.setEffect(new DropShadow(36, Color.rgb(0, 0, 0, 0.72)));
        card.setMouseTransparent(true);

        LauncherLabel titleLabel = new LauncherLabel(root);
        titleLabel.setText("Profils de jeu");
        titleLabel.setStyle("-fx-text-fill: white;");
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        titleLabel.setSize(cardW, 38);
        titleLabel.setPosition(cardX, cardY + 24);
        titleLabel.setAlignment(Pos.CENTER);

        LauncherLabel subTitleLabel = new LauncherLabel(root);
        subTitleLabel.setText("Le profil actif détermine version, modloader, mods et RAM au lancement");
        subTitleLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.45);");
        subTitleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        subTitleLabel.setSize(cardW, 20);
        subTitleLabel.setPosition(cardX, cardY + 58);
        subTitleLabel.setAlignment(Pos.CENTER);

        JFXButton closeButton = new JFXButton("✕");
        closeButton.setText("");
        closeButton.setGraphic(Icons.stroke(Icons.CLOSE, 16));
        closeButton.setPrefSize(36, 32);
        UiStyle.apply(closeButton, "ml-window", "ml-window-close");
        closeButton.setLayoutX(cardX + cardW - 50);
        closeButton.setLayoutY(cardY + 18);
        closeButton.setOnAction(event -> ((Stage) closeButton.getScene().getWindow()).close());
        root.getChildren().add(closeButton);

        /* ===== Formulaire "Nouveau profil" ===== */
        LauncherLabel newLabel = new LauncherLabel(root);
        newLabel.setText("NOUVEAU PROFIL");
        newLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        newLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.95);");
        newLabel.setPosition(cardX + 40, cardY + 92);
        newLabel.setSize(200, 20);

        this.nameField = new JFXTextField();
        this.nameField.setPromptText("Nom du profil (ex: Moddé survie)");
        this.nameField.setLayoutX(cardX + 40);
        this.nameField.setLayoutY(cardY + 118);
        this.nameField.setPrefSize(220, 36);
        this.nameField.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        this.nameField.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(255,255,255,0.14);" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-text-fill: rgba(255,255,255,0.95);" +
                "-fx-prompt-text-fill: rgba(255,255,255,0.45);" +
                "-fx-padding: 0 10 0 10;"
        );
        root.getChildren().add(this.nameField);

        this.versionCombo = new JFXComboBox<>();
        List<String> versions = new ArrayList<>(LauncherSettings.VANILLA_SUPPORTED_RELEASES);
        java.util.Collections.reverse(versions);
        this.versionCombo.getItems().setAll(versions);
        this.versionCombo.setPromptText("Version");
        styleSmallCombo(this.versionCombo);
        this.versionCombo.setLayoutX(cardX + 270);
        this.versionCombo.setLayoutY(cardY + 118);
        this.versionCombo.setPrefSize(110, 36);
        this.versionCombo.setVisibleRowCount(12);
        this.versionCombo.getSelectionModel().selectFirst();
        root.getChildren().add(this.versionCombo);

        this.loaderCombo = new JFXComboBox<>();
        this.loaderCombo.getItems().setAll("Vanilla", "Forge", "Fabric", "Quilt", "NeoForge", "OptiFine");
        this.loaderCombo.setPromptText("Modloader");
        styleSmallCombo(this.loaderCombo);
        this.loaderCombo.setLayoutX(cardX + 390);
        this.loaderCombo.setLayoutY(cardY + 118);
        this.loaderCombo.setPrefSize(135, 36);
        this.loaderCombo.getSelectionModel().selectFirst();
        root.getChildren().add(this.loaderCombo);

        /* ===== Pastilles de couleur ===== */
        HBox swatchRow = new HBox(9);
        swatchRow.setAlignment(Pos.CENTER_LEFT);
        swatchRow.setLayoutX(cardX + 40);
        swatchRow.setLayoutY(cardY + 166);
        colorSwatches.clear();
        for (String hex : ProfileManager.COLOR_PALETTE) {
            javafx.scene.shape.Circle sw = new javafx.scene.shape.Circle(10);
            sw.setFill(Color.web(hex));
            sw.setCursor(javafx.scene.Cursor.HAND);
            sw.setOnMouseClicked(e -> { selectedColor = hex; refreshSwatchSelection(); });
            colorSwatches.add(sw);
            swatchRow.getChildren().add(sw);
        }
        root.getChildren().add(swatchRow);
        refreshSwatchSelection();

        /* ===== Créer / Importer ===== */
        this.createButton = new JFXButton("Créer le profil");
        this.createButton.setLayoutX(cardX + 40);
        this.createButton.setLayoutY(cardY + 198);
        this.createButton.setPrefSize(300, 38);
        // font-family + font-size figés EN INLINE : sinon le survol laisse le CSS
        // JFoenix/.button-raised reprendre le dessus et change police + taille.
        this.createButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        UiStyle.primary(this.createButton);
        this.createButton.setOnAction(event -> onCreateProfile());
        root.getChildren().add(this.createButton);

        JFXButton importButton = new JFXButton("Importer un profil");
        importButton.setLayoutX(cardX + 40 + 315);
        importButton.setLayoutY(cardY + 198);
        importButton.setPrefSize(cardW - 80 - 315, 38);
        importButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        UiStyle.secondary(importButton);
        importButton.setOnAction(event -> onImportProfile());
        root.getChildren().add(importButton);

        LauncherRectangle divider = new LauncherRectangle(root, cardX + 40, cardY + 250, cardW - 80, 1);
        divider.setFill(Color.rgb(255, 255, 255, 0.10));

        LauncherLabel listLabel = new LauncherLabel(root);
        listLabel.setText("MES PROFILS");
        listLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        listLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.95);");
        listLabel.setPosition(cardX + 40, cardY + 264);
        listLabel.setSize(200, 20);

        /* ===== Liste ===== */
        this.listBox = new VBox(12);
        this.listBox.setPadding(new Insets(4, 8, 12, 2));
        this.listBox.setStyle("-fx-background-color: transparent;");

        ScrollPane scroll = new ScrollPane(this.listBox);
        scroll.setLayoutX(cardX + 40);
        scroll.setLayoutY(cardY + 290);
        scroll.setPrefSize(cardW - 80, cardH - 290 - 24);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-control-inner-background: transparent;"
        );
        root.getChildren().add(scroll);

        rebuildList();
    }

    /* ─────────────────────────── Actions ─────────────────────────── */

    private void onCreateProfile() {
        ProfileManager pm = ProfileManager.get();
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String version = versionCombo.getValue();
        String loaderLabel = loaderCombo.getValue();

        if (name.isEmpty()) {
            info("Nom manquant", "Donne un nom à ton profil (ex: \"Moddé survie\").");
            return;
        }
        if (version == null || loaderLabel == null) {
            info("Sélection incomplète", "Choisis une version et un modloader.");
            return;
        }
        for (LauncherProfile p : pm.getProfiles()) {
            if (p.getName().equalsIgnoreCase(name)) {
                info("Nom déjà utilisé", "Un profil s'appelle déjà comme ça.");
                return;
            }
        }

        Utils.ModloaderType loader = parseLoader(loaderLabel);
        LauncherProfile created = pm.create(name, version, loader, selectedColor);
        pm.activate(created, engine);
        App.getInstance().applyGameLinksFromConfigAsync();

        nameField.setText("");
        rebuildList();
        panel.refreshProfilesUI();
    }

    /** Met en évidence la pastille de couleur sélectionnée (anneau blanc). */
    private void refreshSwatchSelection() {
        for (int i = 0; i < colorSwatches.size(); i++) {
            javafx.scene.shape.Circle sw = colorSwatches.get(i);
            boolean sel = ProfileManager.COLOR_PALETTE[i].equalsIgnoreCase(selectedColor);
            sw.setStroke(sel ? Color.WHITE : Color.rgb(255, 255, 255, 0.25));
            sw.setStrokeWidth(sel ? 2.5 : 1.0);
            sw.setRadius(sel ? 11 : 10);
        }
    }

    private void onActivate(LauncherProfile p) {
        ProfileManager pm = ProfileManager.get();
        pm.activate(p, engine);
        App.getInstance().applyGameLinksFromConfigAsync();
        rebuildList();
        panel.refreshProfilesUI();
    }

    private void onRename(LauncherProfile p) {
        TextInputDialog dialog = new TextInputDialog(p.getName());
        dialog.setTitle("Renommer le profil");
        dialog.setHeaderText(null);
        dialog.setContentText("Nouveau nom :");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                ProfileManager.get().rename(p, newName);
                rebuildList();
                panel.refreshProfilesUI();
            }
        });
    }

    private void onExportProfile(LauncherProfile p) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le profil");
        chooser.setInitialFileName(sanitizeFileName(p.getName()) + ".majestyprofile.zip");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Profil MajestyLauncher (*.zip)", "*.zip"));
        File dest = chooser.showSaveDialog(listBox.getScene() == null ? null : listBox.getScene().getWindow());
        if (dest == null) return;

        listBox.setDisable(true);
        new Thread(() -> {
            try {
                ProfileManager.get().exportProfile(p, dest);
                Platform.runLater(() -> {
                    listBox.setDisable(false);
                    info("Profil exporté", "« " + p.getName() + " » a été exporté :\n" + dest.getAbsolutePath()
                            + "\n\nTu peux partager ce fichier ; il sera réimportable via « Importer un profil ».");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    listBox.setDisable(false);
                    info("Export impossible", e.getMessage() == null ? "Erreur inconnue" : e.getMessage());
                });
            }
        }, "MajestyLauncher-ProfileExport").start();
    }

    private void onImportProfile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer un profil");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Profil MajestyLauncher (*.zip)", "*.zip"));
        File src = chooser.showOpenDialog(listBox.getScene() == null ? null : listBox.getScene().getWindow());
        if (src == null) return;

        listBox.setDisable(true);
        new Thread(() -> {
            try {
                LauncherProfile imported = ProfileManager.get().importProfile(src);
                Platform.runLater(() -> {
                    listBox.setDisable(false);
                    rebuildList();
                    panel.refreshProfilesUI();
                    info("Profil importé", "« " + imported.getName() + " » a été ajouté à tes profils.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    listBox.setDisable(false);
                    info("Import impossible", e.getMessage() == null ? "Erreur inconnue" : e.getMessage());
                });
            }
        }, "MajestyLauncher-ProfileImport").start();
    }

    private String sanitizeFileName(String name) {
        String s = name == null ? "profil" : name.trim().replaceAll("[\\\\/:*?\"<>|]", "-");
        return s.isEmpty() ? "profil" : s;
    }

    private void onDuplicate(LauncherProfile p) {
        TextInputDialog dialog = new TextInputDialog(p.getName() + " (copie)");
        dialog.setTitle("Dupliquer le profil");
        dialog.setHeaderText("Les mods, configs, resource packs et shaders seront copiés.");
        dialog.setContentText("Nom de la copie :");
        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) return;

        String newName = result.get().trim();
        listBox.setDisable(true);
        new Thread(() -> {
            try {
                ProfileManager.get().duplicate(p, newName);
                Platform.runLater(() -> {
                    listBox.setDisable(false);
                    rebuildList();
                    panel.refreshProfilesUI();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    listBox.setDisable(false);
                    info("Duplication impossible", e.getMessage() == null ? "Erreur inconnue" : e.getMessage());
                });
            }
        }, "MajestyLauncher-ProfileDuplicate").start();
    }

    private void onDelete(LauncherProfile p) {
        ProfileManager pm = ProfileManager.get();
        if (pm.getProfiles().size() <= 1) {
            info("Impossible", "Tu dois garder au moins un profil.");
            return;
        }

        ButtonType deleteAll = new ButtonType("Profil + fichiers");
        ButtonType deleteOnly = new ButtonType("Profil seulement");
        ButtonType cancel = ButtonType.CANCEL;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + p.getName() + "\" ?\n\n" +
                "\"Profil + fichiers\" supprime aussi son dossier de jeu (mods, configs...).\n" +
                "\"Profil seulement\" garde le dossier sur le disque.",
                deleteAll, deleteOnly, cancel);
        alert.setTitle("Supprimer le profil");
        alert.setHeaderText(null);

        Optional<ButtonType> choice = alert.showAndWait();
        if (!choice.isPresent() || choice.get() == cancel) return;

        pm.delete(p, choice.get() == deleteAll);
        App.getInstance().applyGameLinksFromConfigAsync();
        rebuildList();
        panel.refreshProfilesUI();
    }

    /* ─────────────────────────── Liste ─────────────────────────── */

    private void rebuildList() {
        ProfileManager pm = ProfileManager.get();
        listBox.getChildren().clear();

        LauncherProfile active = pm.getActive();
        String activeId = active == null ? null : active.getId();

        for (LauncherProfile p : pm.getProfiles()) {
            listBox.getChildren().add(buildRow(p, p.getId().equals(activeId)));
        }
    }

    private Pane buildRow(LauncherProfile p, boolean isActive) {
        String hex = ProfileManager.get().colorOf(p);

        VBox texts = new VBox(6);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Label name = new Label(p.getName() + (isActive ? "   —   ACTIF" : ""));
        name.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        name.setStyle("-fx-text-fill: " + (isActive ? "rgba(255,205,130,0.98)" : "rgba(255,255,255,0.94)") + "; -fx-font-weight: bold;");

        String playtime = p.getTotalPlaytimeMs() > 0 ? "   •   " + p.describeTotalPlaytime() + " de jeu" : "";
        Label detail = new Label(p.describeTarget() + "   •   " + (int) p.getRam() + " Go de RAM   •   " + p.describeLastPlayed() + playtime);
        detail.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        detail.setStyle("-fx-text-fill: rgba(255,255,255,0.55);");

        // Boutons alignés à gauche, tous de même largeur et avec le MÊME écart (10px).
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(actions, new Insets(4, 0, 0, 0));

        // « Activer » à droite du nom : la rangée du bas garde ses 4 boutons, qui
        // tiennent en entier (avec 5 boutons, les libellés étaient tronqués).
        Node header = name;
        if (!isActive) {
            JFXButton activate = smallButton("Activer", true);
            activate.setPrefHeight(28);
            activate.setOnAction(e -> onActivate(p));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox top = new HBox(10, name, spacer, activate);
            top.setAlignment(Pos.CENTER_LEFT);
            header = top;
        }

        JFXButton duplicate = smallButton("Dupliquer", false);
        duplicate.setOnAction(e -> onDuplicate(p));

        JFXButton export = smallButton("Exporter", false);
        export.setOnAction(e -> onExportProfile(p));

        JFXButton rename = smallButton("Renommer", false);
        rename.setOnAction(e -> onRename(p));

        JFXButton delete = smallButton("Supprimer", false);
        delete.setStyle(delete.getStyle() + "-fx-text-fill: rgba(255,120,120,0.92);");
        delete.setOnAction(e -> onDelete(p));

        actions.getChildren().addAll(duplicate, export, rename, delete);

        texts.getChildren().addAll(header, detail, actions);

        // Barre de couleur d'identité à gauche de la carte.
        javafx.scene.layout.Region colorBar = new javafx.scene.layout.Region();
        colorBar.setMinWidth(5);
        colorBar.setPrefWidth(5);
        colorBar.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 4;");

        HBox row = new HBox(14, colorBar, texts);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 12));
        row.setStyle(
                "-fx-background-color: " + (isActive ? "rgba(255,150,0,0.14)" : "rgba(255,255,255,0.05)") + ";" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: " + (isActive ? "rgba(255,170,0,0.55)" : "rgba(255,255,255,0.10)") + ";" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;"
        );
        colorBar.prefHeightProperty().bind(row.heightProperty().subtract(20));
        return row;
    }

    private JFXButton smallButton(String text, boolean primary) {
        JFXButton b = new JFXButton(text);
        b.setPrefHeight(32);
        // Largeur auto (s'ajuste au texte) : avec un espacement HBox uniforme, les
        // écarts restent réguliers sans risque de débordement quand il y a 5 boutons.
        b.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        b.setMinWidth(Region.USE_PREF_SIZE);
        if (primary) UiStyle.primary(b);
        else UiStyle.secondary(b);
        b.setStyle("-fx-padding: 4 12 4 12;");
        return b;
    }

    private void styleSmallCombo(JFXComboBox<String> combo) {
        combo.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(255,255,255,0.14);" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-font-family: 'Comfortaa';"
        );
        combo.setButtonCell(new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: rgba(255,255,255,0.94);");
            }
        });
        combo.setCellFactory(cb -> new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
            }
        });
    }

    private Utils.ModloaderType parseLoader(String label) {
        switch (label) {
            case "Forge": return Utils.ModloaderType.FORGE;
            case "Fabric": return Utils.ModloaderType.FABRIC;
            case "Quilt": return Utils.ModloaderType.QUILT;
            case "NeoForge": return Utils.ModloaderType.NEOFORGE;
            case "OptiFine": return Utils.ModloaderType.OPTIFINE;
            default: return Utils.ModloaderType.VANILLA;
        }
    }

    private void info(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Profils");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
