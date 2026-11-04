package fr.majestycraft.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Répare les raccourcis Bureau et menu Démarrer des installations Windows faites avec l'ancien
 * MajestyLauncher.bat : ils lancent MajestyLauncher.vbs, un script VBScript, que Microsoft
 * désactivera par défaut dans Windows (annoncé pour 2027 environ). Ces raccourcis cesseraient
 * alors de marcher. Ils sont repointés une fois pour toutes sur le javaw.exe du Java installé
 * (signé par Azul) avec « -jar bootstrap.jar », comme les crée désormais le .bat ; le .vbs est
 * ensuite supprimé. Rien ne se passe sur les installations récentes, ni hors de Windows.
 */
public final class WindowsShortcutFixer {

    /** Ancien lanceur créé par MajestyLauncher.bat, à côté de bootstrap.jar. */
    static final String OLD_LAUNCHER = "MajestyLauncher.vbs";
    /** Posé quand un raccourci n'a pas pu être modifié, pour ne pas réessayer à chaque lancement. */
    static final String DONE_MARKER = "raccourcis-repares.txt";

    /**
     * Script PowerShell sur une ligne, sans guillemet double (ProcessBuilder les échapperait mal
     * sous Windows). Les chemins arrivent par l'environnement : accents, apostrophes et espaces
     * restent intacts. Un dossier est soit un chemin, soit un dossier spécial de Windows
     * (Desktop, Programs), que Windows résout lui-même, Bureau OneDrive compris. Un raccourci est
     * reconnu au nom de sa cible, même quand l'ancien script en avait abîmé le chemin.
     */
    static final String SCRIPT = String.join(" ",
            "$q = [char]34; $s = New-Object -ComObject WScript.Shell; $ok = 0; $ko = 0;",
            "foreach ($d in $env:MAJESTY_DOSSIERS.Split(';')) {",
            "  if (-not $d) { continue };",
            "  if (-not [IO.Path]::IsPathRooted($d)) { $d = [Environment]::GetFolderPath($d) };",
            "  if (-not $d -or -not (Test-Path -LiteralPath $d)) { continue };",
            "  foreach ($f in (Get-ChildItem -LiteralPath $d -Filter *.lnk -File)) {",
            "    try {",
            "      $l = $s.CreateShortcut($f.FullName);",
            "      if ([IO.Path]::GetFileName($l.TargetPath) -ine $env:MAJESTY_ANCIEN) { continue };",
            "      $l.TargetPath = $env:MAJESTY_JAVAW;",
            "      $l.Arguments = '-jar ' + $q + $env:MAJESTY_JAR + $q;",
            "      $l.WorkingDirectory = $env:MAJESTY_DIR;",
            "      $l.Save(); $ok++",
            "    } catch { $ko++ }",
            "  }",
            "};",
            "Write-Output ('RESULTAT ' + $ok + ' ' + $ko)");

    private WindowsShortcutFixer() {
    }

    /** Lancé au démarrage, en arrière-plan et un peu après, pour ne pas ralentir l'ouverture. */
    public static void repairInBackground() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return;
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(20_000);
                String resultat = repair(new File(System.getProperty("java.home")), "Desktop;Programs");
                if (resultat != null) System.out.println("Raccourcis Windows : " + resultat);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                System.err.println("Réparation des raccourcis impossible pour l'instant : " + e);
            }
        }, "MajestyLauncher-Raccourcis");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /**
     * @param javaHome dossier du Java qui fait tourner le launcher (installDir\jre)
     * @param dossiers dossiers à parcourir, séparés par « ; »
     * @return le bilan, ou null s'il n'y avait rien à faire
     */
    static String repair(File javaHome, String dossiers) throws IOException, InterruptedException {
        File installDir = javaHome.getParentFile();
        if (installDir == null) return null;
        File ancien = new File(installDir, OLD_LAUNCHER);
        File javaw = new File(new File(javaHome, "bin"), "javaw.exe");
        File jar = new File(installDir, "bootstrap.jar");
        File fait = new File(installDir, DONE_MARKER);
        if (!ancien.isFile() || fait.exists() || !javaw.isFile() || !jar.isFile()) return null;

        String systemRoot = System.getenv("SystemRoot");
        File powershell = new File(systemRoot != null ? systemRoot : "C:\\Windows",
                "System32\\WindowsPowerShell\\v1.0\\powershell.exe");
        ProcessBuilder pb = new ProcessBuilder(powershell.getPath(), "-NoProfile", "-NonInteractive", "-Command", SCRIPT);
        Map<String, String> env = pb.environment();
        env.put("MAJESTY_DOSSIERS", dossiers);
        env.put("MAJESTY_ANCIEN", OLD_LAUNCHER);
        env.put("MAJESTY_JAVAW", javaw.getAbsolutePath());
        env.put("MAJESTY_JAR", jar.getAbsolutePath());
        env.put("MAJESTY_DIR", installDir.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getOutputStream().close();
        if (!p.waitFor(60, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("PowerShell ne répond pas");
        }
        String sortie = lire(p.getInputStream());
        if (p.exitValue() != 0) throw new IOException("PowerShell a échoué (" + p.exitValue() + ") : " + sortie.trim());

        int ok = -1, ko = -1;
        for (String ligne : sortie.split("\\r?\\n")) {
            if (ligne.startsWith("RESULTAT ")) {
                String[] parts = ligne.trim().split(" ");
                ok = Integer.parseInt(parts[1]);
                ko = Integer.parseInt(parts[2]);
            }
        }
        if (ok < 0) throw new IOException("Réponse inattendue de PowerShell : " + sortie.trim());
        if (ko == 0) {
            // Plus aucun raccourci ne s'en sert : le .vbs part, et la vérification avec lui.
            if (!ancien.delete()) ancien.deleteOnExit();
        } else {
            // Raccourci impossible à modifier (lecture seule...) : on ne réessaie pas à chaque lancement.
            java.nio.file.Files.write(fait.toPath(),
                    ("Raccourcis repointés : " + ok + ", échecs : " + ko + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        }
        return ok + " repointé(s), " + ko + " échec(s)";
    }

    private static String lire(InputStream in) throws IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) > 0) out.write(buffer, 0, read);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
