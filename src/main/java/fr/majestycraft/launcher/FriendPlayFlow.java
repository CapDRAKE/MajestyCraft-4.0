package fr.majestycraft.launcher;

import fr.majestycraft.App;
import fr.majestycraft.Utils;
import fr.majestycraft.mods.ModActions;
import fr.majestycraft.profiles.LauncherProfile;
import fr.majestycraft.profiles.ProfileManager;
import fr.majestycraft.util.MajestyDialog;
import fr.trxyy.alternative.alternative_api.GameEngine;
import javafx.application.Platform;
import javafx.stage.Window;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * « Jouer avec un ami » : le mod e4mc (Modrinth, open source) publie le monde ouvert en
 * LAN sous une adresse Internet temporaire. Seul l'hôte installe le mod ; l'ami se
 * connecte avec un Minecraft normal de la même version. Pas de serveur ni de ports.
 */
public final class FriendPlayFlow {

    private static final String E4MC = "e4mc";

    private FriendPlayFlow() {
    }

    public static void start(Window owner, LauncherPanel pane, GameEngine engine) {
        ProfileManager pm = ProfileManager.get();
        LauncherProfile profile = pm == null ? null : pm.getActive();
        if (profile == null) return;
        String version = profile.getVersion();
        Utils.ModloaderType loader = profile.getModloaderType();

        if (!atLeast117(version)) {
            new MajestyDialog(owner, "JOUER AVEC UN AMI", "Version trop ancienne",
                    "Le jeu entre amis par Internet demande Minecraft 1.17 ou plus récent. Ton profil « "
                            + profile.getName() + " » est en " + version + " : choisis ou crée un profil plus récent.")
                    .action("Compris", "primary", null)
                    .show();
            return;
        }

        boolean needsCopy = loader == Utils.ModloaderType.VANILLA || loader == Utils.ModloaderType.OPTIFINE;
        File modsDir = new File(pm.resolveProfileDirectory(profile), "mods");
        if (!needsCopy && isInstalled(modsDir)) {
            readyDialog(owner, version).show();
            return;
        }

        String copyName = profile.getName() + " · Entre amis";
        String intro = needsCopy
                ? "Le launcher ajoute le mod e4mc, qui partage ton monde par Internet sans serveur ni réglage de box. "
                        + "Il a besoin de Fabric : une copie de ce profil est créée (« " + copyName + " »), ton profil actuel reste intact."
                : "Le launcher ajoute le mod e4mc à ce profil : il partage ton monde par Internet, sans serveur ni réglage de box.";
        new MajestyDialog(owner, "JOUER AVEC UN AMI", "Partager ton monde", intro
                + "\nSeul toi as besoin du mod : ton ami joue avec un Minecraft " + version + " normal.")
                .cancel("Annuler")
                .work(needsCopy ? "Créer et installer" : "Installer", "primary", progress -> {
                    LauncherProfile target = profile;
                    Utils.ModloaderType targetLoader = loader;
                    if (needsCopy) {
                        target = pm.create(copyName, version, Utils.ModloaderType.FABRIC);
                        target.setRam(profile.getRam());
                        target.setGameSize(profile.getGameSize());
                        target.setVmArguments(profile.getVmArguments());
                        target.setUseVmArguments(profile.isUseVmArguments());
                        pm.update(target);
                        targetLoader = Utils.ModloaderType.FABRIC;
                    }
                    File targetMods = new File(pm.resolveProfileDirectory(target), "mods");
                    ModActions.installProject(E4MC, targetMods, version, targetLoader, progress);
                    ModActions.installMissingDependencies(targetMods, version, targetLoader, progress);
                    if (needsCopy) activate(target, pane, engine);
                    return "C'est prêt" + (needsCopy ? " : profil « " + copyName + " » activé." : ".") + "\n\n" + steps(version);
                })
                .show();
    }

    /** e4mc déjà installé : les étapes pour inviter un ami (sans l'afficher). */
    static MajestyDialog readyDialog(Window owner, String version) {
        return new MajestyDialog(owner, "JOUER AVEC UN AMI", "Tout est prêt", steps(version))
                .action("Compris", "primary", null);
    }

    private static String steps(String version) {
        return "1. Clique sur JOUER et ouvre ton monde.\n"
                + "2. Échap, puis « Ouvrir au LAN » et « Ouvrir au réseau local ».\n"
                + "3. Une adresse (…e4mc.link) s'affiche dans le chat : envoie-la à ton ami.\n"
                + "4. Ton ami lance Minecraft " + version + ", puis Multijoueur, Connexion rapide, et colle l'adresse.\n\n"
                + "L'adresse ne marche que tant que ton monde est ouvert. La connexion passe par e4mc, un service gratuit et open source.";
    }

    /** e4mc déjà présent et actif dans ce dossier de mods ? */
    static boolean isInstalled(File modsDir) {
        File[] files = modsDir.listFiles((d, n) -> n.toLowerCase().startsWith(E4MC) && n.toLowerCase().endsWith(".jar"));
        return files != null && files.length > 0;
    }

    private static boolean atLeast117(String version) {
        Matcher weekly = Pattern.compile("^(\\d{2})w\\d{2}[a-z]$").matcher(version == null ? "" : version);
        if (weekly.matches()) return Integer.parseInt(weekly.group(1)) >= 21;   // snapshots 1.17 et après
        Matcher m = Pattern.compile("^(\\d+)\\.(\\d+)").matcher(version == null ? "" : version);
        if (!m.find()) return false;
        return Integer.parseInt(m.group(1)) > 1 || Integer.parseInt(m.group(2)) >= 17;
    }

    private static void activate(LauncherProfile target, LauncherPanel pane, GameEngine engine) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ProfileManager.get().activate(target, engine);
                App.getInstance().applyGameLinksFromConfigAsync();
                if (pane != null) pane.refreshProfilesUI();
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }
}
