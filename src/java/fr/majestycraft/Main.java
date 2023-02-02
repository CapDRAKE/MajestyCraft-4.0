package fr.majestycraft;

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
            new App().launcher();
        } catch (Exception e) {
            System.err.println("Une erreur est survenue lors du lancement de l'application : " + e.getMessage());
        }
    }
}
