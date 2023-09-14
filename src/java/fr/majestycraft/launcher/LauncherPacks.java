package fr.majestycraft.launcher;

import fr.majestycraft.Main;
import fr.trxyy.alternative.alternative_api.GameEngine;
import fr.trxyy.alternative.alternative_api.utils.FontLoader;
import fr.trxyy.alternative.alternative_api_ui.base.IScreen;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherButton;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherLabel;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherRectangle;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.jfoenix.controls.JFXButton;

import animatefx.animation.ZoomOutDown;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class LauncherPacks extends IScreen {
    private ListView<ResourcePackItem> resourcePacksList;
    private JFXButton addButton;
    private File resourcePacksDir;
    Stage stage;
	private LauncherButton quit;
	private static final String LABEL_TITLE_LISTE = Main.bundle.getString("LABEL_TITLE_LISTE");
	private static final String BUTTON_ADD_PACK = Main.bundle.getString("BUTTON_ADD_PACK");
	private static final String DIALOG_SELECT_RESOURCE_PACK = Main.bundle.getString("DIALOG_SELECT_RESOURCE_PACK");
	private static final String DIALOG_CONFIRMATION_REMOVAL = Main.bundle.getString("DIALOG_CONFIRMATION_REMOVAL");
	private static final String DIALOG_CONFIRMATION_REMOVAL_MSG = Main.bundle.getString("DIALOG_CONFIRMATION_REMOVAL_MSG");
	private static final String ERROR_DELETION = Main.bundle.getString("ERROR_DELETION");
	private static final String ERROR_DELETION_MSG = Main.bundle.getString("ERROR_DELETION_MSG");
    private static final String QUITTER_BUTTON_TEXT = "Retour";
    
    public LauncherPacks(final Pane root, final GameEngine engine, final LauncherPanel pane) {

        this.drawBackgroundImage(engine, root, "background.png");

        /* ===================== RECTANGLE NOIR EN HAUT ===================== */
        LauncherRectangle topRectangle = new LauncherRectangle(root, 0, 0, 1500, 15);
        topRectangle.setOpacity(0.7);

        /* ===================== LABEL TITRE ===================== */
        LauncherLabel titleLabel = new LauncherLabel(root);
        titleLabel.setText(LABEL_TITLE_LISTE);
        titleLabel.setStyle("-fx-text-fill: white;");
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        titleLabel.setPosition(250, 20);
        titleLabel.setSize(400, 100);

        /* ===================== LISTVIEW DES PACKS DE RESSOURCES ===================== */
        resourcePacksList = new ListView<>();
        resourcePacksList.setPrefSize(400, 300);
        resourcePacksList.setLayoutX((1000 - 400) / 2); // Centrer horizontalement
        resourcePacksList.setLayoutY((750 - 300) / 2); // Centrer verticalement
        resourcePacksList.setStyle("-fx-background-color: rgba(0, 0, 0, 0); -fx-control-inner-background: rgba(0, 0, 0, 0);");
        root.getChildren().add(resourcePacksList);
        // Configure the ListView to display ResourcePackItems
        resourcePacksDir = new File("./resourcepacks");
        configureResourcePacksList();
        /* ===================== BOUTON AJOUTER ===================== */
        addButton = new JFXButton(BUTTON_ADD_PACK);
        addButton.setLayoutX(60); // Ajustez la position en X
        addButton.setLayoutY(550); // Ajustez la position en Y
        addButton.setStyle("-fx-background-color: rgba(53, 89, 119, 0.4); -fx-text-fill: white;");
        addButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        addButton.setOnAction(event -> addResourcePack());
        root.getChildren().add(addButton);

        resourcePacksDir = new File(System.getenv("APPDATA") + "/.majestycraft/bin/game/resourcepacks");
        loadResourcePacks();
        
        /** ===================== BOUTON QUITTER ===================== */
		this.quit = new LauncherButton(root);
		this.quit.setText(QUITTER_BUTTON_TEXT);
		this.quit.setStyle("-fx-background-color: rgba(53, 89, 119, 0.4); -fx-text-fill: white;");
		this.quit.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
		this.quit.setPosition(700, 550);
		this.quit.setSize(130, 35);
		this.quit.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				final ZoomOutDown animation = new ZoomOutDown(root);
				animation.setOnFinished(new EventHandler<ActionEvent>() {
					@Override
					public void handle(final ActionEvent actionEvent) {
						Stage stage = (Stage) ((LauncherButton) event.getSource()).getScene().getWindow();
						stage.close();
					}
				});
				animation.setResetOnFinished(true);
				animation.play();
			}
		});
    }

    private void loadResourcePacks() {
        resourcePacksList.getItems().clear();
        if (resourcePacksDir.exists() && resourcePacksDir.isDirectory()) {
            for (File file : resourcePacksDir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".zip")) {
                    try (ZipFile zipFile = new ZipFile(file)) {
                        ZipEntry packMetaEntry = zipFile.getEntry("pack.mcmeta");
                        ZipEntry packIconEntry = zipFile.getEntry("pack.png");
                        if (packMetaEntry != null) {
                            // Load the pack icon if available
                            Image packIcon = null;
                            if (packIconEntry != null) {
                                packIcon = new Image(zipFile.getInputStream(packIconEntry));
                            }
                            // Remove the ".zip" extension from the name
                            String packName = file.getName().substring(0, file.getName().length() - 4);
                            resourcePacksList.getItems().add(new ResourcePackItem(packName, packIcon));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void addResourcePack() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(DIALOG_SELECT_RESOURCE_PACK);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Packs de ressources", "*.zip"));
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            try {
                File destFile = new File(resourcePacksDir, selectedFile.getName());
                if (!destFile.exists()) {
                    Files.copy(selectedFile.toPath(), destFile.toPath());
                    loadResourcePacks();
                } else {
                    // Afficher un message d'erreur si le fichier existe déjà
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void configureResourcePacksList() {
        resourcePacksList.setCellFactory(param -> new ListCell<ResourcePackItem>() {
            private ImageView packImageView = new ImageView();
            private ImageView deleteImageView = new ImageView(getClass().getResource("/resources/delete_icon.png").toExternalForm());
            {
            	deleteImageView.setFitWidth(16);
            	deleteImageView.setFitHeight(16);
            }
            private Button deleteButton = new Button();
            private HBox hbox = new HBox(10, packImageView, new Label(), deleteButton); // Add spacing between elements

            @Override
            protected void updateItem(ResourcePackItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Configure pack image view
                    packImageView.setImage(item.getIcon());
                    packImageView.setFitWidth(50);
                    packImageView.setFitHeight(50);

                    // Configure pack name label
                    Label nameLabel = (Label) hbox.getChildren().get(1);
                    nameLabel.setText(item.getName());
                    nameLabel.setFont(Font.font("Arial", 16));

                    // Configure delete button
                    deleteButton.setGraphic(deleteImageView);
                    deleteButton.setOnAction(event -> {
                    	if (showConfirmationDialog(DIALOG_CONFIRMATION_REMOVAL, DIALOG_CONFIRMATION_REMOVAL_MSG)) {
                            File packFile = new File(resourcePacksDir, item.getName() + ".zip");
                            if (packFile.delete()) {
                                resourcePacksList.getItems().remove(item);
                            } else {
                            	showErrorDialog(ERROR_DELETION, ERROR_DELETION_MSG);
                            }
                        }
                    });

                    setGraphic(hbox);
                }
            }
        });
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
    
}
