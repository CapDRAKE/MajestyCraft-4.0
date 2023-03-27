package fr.majestycraft;

import java.io.IOException;
import java.util.logging.LogManager;

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
        try {
            LogManager.getLogManager().readConfiguration(
                    App.class.getResourceAsStream("/logging.properties"));
        } catch (IOException e) {
            System.err.println("Erreur lors de la configuration du gestionnaire de journalisation: " + e.getMessage());
        }
    	
        try {
            new App().launcher();
        } catch (Exception e) {
            System.err.println("Une erreur est survenue lors du lancement de l'application : " + e.getMessage());
        }
    }
}
