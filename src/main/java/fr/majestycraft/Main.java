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

import fr.trxyy.alternative.alternative_api.GameEngine;
import fr.trxyy.alternative.alternative_api.utils.config.EnumConfig;
import fr.trxyy.alternative.alternative_api.utils.config.LauncherConfig;

import java.util.Locale;

/**
 * Classe principale du launcher MajestyLauncher
 */
public class Main {
	
	public static ResourceBundle bundle;
    public static LauncherConfig config;
    public static String language;


    public static void loadConfiguration(GameEngine engine) {
        config = new LauncherConfig(engine);
        config.loadConfiguration();
    }
    /**
     * Point d'entr�e du programme
     *
     * @param args Les arguments de la ligne de commande
     */
    public static void main(String[] args) {
        configureLogging();
        // Jetons Microsoft chiffrés (DPAPI) avant toute reconnexion
        fr.majestycraft.util.WindowsTokenProtector.install();
        loadConfiguration(new GameEngine(null, null, null, null));
        language = (String) config.getValue(EnumConfig.LANGUAGE);
        if (language == null) language = "Français";
        switch (language) {
            case "Français":
            	Locale.setDefault(new Locale("fr", "FR"));
                break;
            case "English":
            	Locale.setDefault(new Locale("en", "US"));
                break;
            case "Español":
            	Locale.setDefault(new Locale("es", "ES"));
                break;
            default:
            	Locale.setDefault(new Locale("fr", "FR"));
                break;
        }
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

    /** Version courante du launcher (pour le popup "Quoi de neuf"). */
    public static final String CURRENT_VERSION = "5.3";

    /**
     * Affiche une pop-up "Quoi de neuf" une seule fois par version. Montr�e tant
     * que la version vue diff�re de la version courante — y compris quand aucune
     * n'est enregistr�e (joueurs mis � jour depuis une version ant�rieure � ce
     * m�canisme, qui sont justement ceux � qui pr�senter les profils).
     */
    static void showWhatsNewIfUpdated() {
        Preferences prefs = Preferences.userNodeForPackage(App.class);
        String lastSeen = prefs.get("lastSeenVersion", null);

        if (CURRENT_VERSION.equals(lastSeen)) {
            return;
        }

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Quoi de neuf");
        alert.setHeaderText("MajestyLauncher " + CURRENT_VERSION + " — nouveautés");

        VBox content = new VBox(8);
        content.getChildren().addAll(
                new javafx.scene.control.Label("• Accueil repensé : ton profil, ton temps de jeu et les nouveautés du launcher en un coup d'œil."),
                new javafx.scene.control.Label("• Voyage dans le temps : rejoue chaque grande époque de Minecraft, de 2009 à aujourd'hui, en un clic depuis l'accueil."),
                new javafx.scene.control.Label("• Jouer avec un ami : ouvre ton monde solo à un ami par internet, sans serveur ni redirection de ports."),
                new javafx.scene.control.Label("• Mode performance repensé : le launcher analyse ton PC (processeur, mémoire, carte graphique) et te propose la meilleure configuration. Bouton éclair à côté de ton profil."),
                new javafx.scene.control.Label("• Nouvel écran de chargement : ton skin et une rangée de blocs qui creuse jusqu'aux diamants."),
                new javafx.scene.control.Label("• NeoForge 1.21 et Quilt démarrent de nouveau correctement."),
                new javafx.scene.control.Label("\nLe détail complet est dans l'onglet « Infos » de la barre latérale.")
        );
        content.getChildren().forEach(n -> ((javafx.scene.control.Label) n).setWrapText(true));
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setPrefWidth(520);
        alert.showAndWait();

        prefs.put("lastSeenVersion", CURRENT_VERSION);
    }

    /**
     * Affiche la pop-up au d�marrage de l'application
     */
    static void showStartupPopup() {
        // Nouveau joueur : l'assistant de démarrage remplace les pop-ups habituelles
        if (App.getInstance() != null && App.getInstance().getPanel() != null
                && fr.majestycraft.launcher.Onboarding.isNewPlayer()) {
            Preferences.userNodeForPackage(App.class).put("lastSeenVersion", CURRENT_VERSION);
            fr.majestycraft.launcher.Onboarding.show(App.getInstance().getPanel());
            return;
        }
        showWhatsNewIfUpdated();

        Preferences prefs = Preferences.userNodeForPackage(App.class);
        boolean showDialog = prefs.getBoolean("showStartupPopup", true);

        if (showDialog) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(bundle.getString("specialOffer"));
            alert.setHeaderText(bundle.getString("discountHeader"));

            Hyperlink link = new Hyperlink(bundle.getString("clickHereLink"));
            link.setOnAction(event -> {
                try {
                    fr.majestycraft.util.SystemOpen.browseAsync("https://minestrator.com/partenaire/eus561rkso", null);
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