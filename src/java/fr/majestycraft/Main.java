package fr.majestycraft;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.LogManager;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Region;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import java.util.prefs.Preferences;
import java.util.Locale;

/**
 * Classe principale du launcher MajestyLauncher
 */
public class Main {
	
	public static ResourceBundle bundle;

    /**
     * Point d'entr�e du programme
     *
     * @param args Les arguments de la ligne de commande
     */
    public static void main(String[] args) {
        configureLogging();
        bundle = ResourceBundle.getBundle("resources.messages", Locale.getDefault());
        try {
            App app = new App();
            app.launcher();
        } catch (Exception e) {
            System.err.println("Une erreur est survenue lors du lancement de l'application : " + e.getMessage());
        }
    }

    /**
     * Configure le gestionnaire de journalisation pour supprimer les avertissements JavaFX
     */
    private static void configureLogging() {
        try {
            LogManager.getLogManager().readConfiguration(
                    App.class.getResourceAsStream("/logging.properties"));
        } catch (IOException e) {
            System.err.println("Erreur lors de la configuration du gestionnaire de journalisation: " + e.getMessage());
        }
    }

    /**
     * Affiche la pop-up au d�marrage de l'application
     */
    static void showStartupPopup() {
        Preferences prefs = Preferences.userNodeForPackage(App.class);
        boolean showDialog = prefs.getBoolean("showStartupPopup", true);

        if (showDialog) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(bundle.getString("specialOffer"));
            alert.setHeaderText(bundle.getString("discountHeader"));

            Hyperlink link = new Hyperlink(bundle.getString("clickHereLink"));
            link.setOnAction(event -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://minestrator.com/partenaire/eus561rkso"));
                } catch (Exception e) {
                    System.err.println(bundle.getString("errorOpeningLink") + e.getMessage());
                }
            });

            CheckBox checkBox = new CheckBox(bundle.getString("doNotShowAgain"));
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                prefs.putBoolean("showStartupPopup", !newVal);
            });

            VBox vbox = new VBox(link, checkBox);
            vbox.setSpacing(10);
            alert.getDialogPane().setContent(vbox);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        }
    }

}