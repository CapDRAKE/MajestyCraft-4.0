package fr.majestycraft;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.LogManager;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Region;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import java.io.BufferedReader;

/**
 * Classe principale du launcher MajestyLauncher
 */
public class Main {

    /**
     * Point d'entrée du programme
     *
     * @param args Les arguments de la ligne de commande
     */
    public static void main(String[] args) {
        configureLogging();
        if (isJavaFXAvailable()) {
            launchApp();
        } else {
            if (installJavaFX()) {
                showJavaFXInstalledPopup();
            } else {
                showJavaFXErrorPopup();
            }
        }
    }
    
    private static void launchApp() {
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
     * Affiche la pop-up au démarrage de l'application
     */
    static void showStartupPopup() {
        Preferences prefs = Preferences.userNodeForPackage(App.class);
        boolean showDialog = prefs.getBoolean("showStartupPopup", true);

        if (showDialog) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Offre spéciale");
            alert.setHeaderText("Profitez de -10% chez Minestrator avec le code MAJESTYCRAFT!");

            Hyperlink link = new Hyperlink("Cliquez ici pour bénéficier de l'offre chez Minestrator.");
            link.setOnAction(event -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://minestrator.com/partenaire/eus561rkso"));
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'ouverture du lien : " + e.getMessage());
                }
            });

            CheckBox checkBox = new CheckBox("Ne plus afficher ce message");
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
    
    private static boolean isJavaFXAvailable() {
        try {
            Class.forName("javafx.application.Application");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private static void showJavaFXErrorPopup() {
        new JFXPanel(); // Initialise JavaFX runtime
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Erreur JavaFX");
            alert.setHeaderText("JavaFX n'est pas installé sur votre système.");
            alert.setContentText("Pour Linux et Mac, veuillez installer JavaFX manuellement.\n\n"
                    + "Linux : sudo apt-get install openjfx\n"
                    + "Mac : brew install openjfx\n\n"
                    + "Une fois JavaFX installé, relancez le launcher.");

            alert.showAndWait();
            System.exit(1);
        });
    }
    
    private static boolean installJavaFX() {
        String osName = System.getProperty("os.name").toLowerCase();
        String[] cmd = null;

        if (osName.contains("linux")) {
            cmd = new String[] { "bash", "-c", "sudo apt-get install -y openjfx" };
        } else if (osName.contains("mac")) {
            cmd = new String[] { "bash", "-c", "brew install openjfx" };
        }

        if (cmd != null) {
            try {
                Process process = Runtime.getRuntime().exec(cmd);
                process.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                return isJavaFXAvailable();
            } catch (Exception e) {
                System.err.println("Erreur lors de l'installation de JavaFX : " + e.getMessage());
            }
        }

        return false;
    }
    
    private static void showJavaFXInstalledPopup() {
        new JFXPanel(); // Initialise JavaFX runtime
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("JavaFX Installé");
            alert.setHeaderText("JavaFX a été installé avec succès");
            alert.setContentText("Veuillez relancer le launcher.");

            alert.showAndWait();
            System.exit(0);
        });
    }
}
