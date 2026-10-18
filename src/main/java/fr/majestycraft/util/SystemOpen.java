package fr.majestycraft.util;

import javafx.application.Platform;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Ouvre un lien dans le navigateur ou un dossier / fichier dans l'explorateur, sur
 * tous les systèmes.
 *
 * Sous Linux, java.awt.Desktop n'est pas utilisé : en Java 8 il charge GTK 2, alors
 * que JavaFX peut déjà avoir chargé GTK 3 (les deux dans un même processus font
 * planter la JVM), et il n'est de toute façon pas pris en charge sur tous les
 * bureaux. On passe par xdg-open, puis gio. Sous Windows et macOS, Desktop reste
 * la voie normale, avec une commande système en secours.
 */
public final class SystemOpen {

    private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    private static final boolean WINDOWS = OS.contains("win");
    private static final boolean MAC = OS.contains("mac") || OS.contains("darwin");

    private SystemOpen() {
    }

    /** Ouvre l'adresse dans le navigateur. Bloque au plus quelques secondes : à appeler hors du thread JavaFX. */
    public static boolean browse(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        if (!WINDOWS && !MAC) {
            return run("xdg-open", url) || run("gio", "open", url);
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return true;
            }
        } catch (Exception ignored) {
            // commande système ci-dessous
        }
        return MAC ? run("open", url) : start("rundll32", "url.dll,FileProtocolHandler", url);
    }

    /** Ouvre un dossier dans l'explorateur, ou un fichier avec l'application associée. */
    public static boolean open(File target) {
        if (target == null || !target.exists()) return false;
        String path = target.getAbsolutePath();
        if (!WINDOWS && !MAC) {
            return run("xdg-open", path) || run("gio", "open", path);
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(target);
                return true;
            }
        } catch (Exception ignored) {
            // commande système ci-dessous
        }
        // explorer.exe renvoie 1 même quand il a ouvert le dossier : seul le lancement compte.
        return MAC ? run("open", path) : start("explorer", path);
    }

    /** Comme browse, sans bloquer l'interface ; onFailure (facultatif) est appelé sur le thread JavaFX. */
    public static void browseAsync(String url, Runnable onFailure) {
        async(() -> browse(url), onFailure);
    }

    /** Comme open, sans bloquer l'interface ; onFailure (facultatif) est appelé sur le thread JavaFX. */
    public static void openAsync(File target, Runnable onFailure) {
        async(() -> open(target), onFailure);
    }

    private interface Attempt {
        boolean run();
    }

    private static void async(Attempt attempt, Runnable onFailure) {
        Thread thread = new Thread(() -> {
            boolean ok;
            try {
                ok = attempt.run();
            } catch (Exception e) {
                ok = false;
            }
            if (!ok && onFailure != null) Platform.runLater(onFailure);
        }, "MajestyLauncher-Open");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Lance la commande. Commande absente ou terminée en erreur dans les premières
     * secondes : échec. Toujours en cours ensuite (certains bureaux attendent la
     * fermeture du navigateur) : la demande est considérée comme transmise.
     */
    private static boolean run(String... command) {
        try {
            Process process = launch(command);
            if (process.waitFor(3, TimeUnit.SECONDS)) {
                return process.exitValue() == 0;
            }
            return true;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Lance la commande sans attendre son résultat. */
    private static boolean start(String... command) {
        try {
            launch(command);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static Process launch(String... command) throws IOException {
        return new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
    }
}
