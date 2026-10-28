package fr.majestycraft.mods;

import com.google.gson.JsonObject;
import fr.majestycraft.Utils;
import fr.majestycraft.profiles.LauncherProfile;
import fr.majestycraft.profiles.ProfileManager;
import fr.majestycraft.util.SystemOpen;
import fr.trxyy.alternative.alternative_api.GameEngine;
import fr.trxyy.alternative.alternative_api.GameProfilePaths;
import fr.trxyy.alternative.alternative_api.utils.config.LauncherConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Correctifs proposés par le diagnostic de crash et par le contrôle avant lancement,
 * appliqués au profil actif. Méthodes bloquantes (réseau, fichiers) sauf mention.
 */
public final class ModActions {

    private ModActions() {
    }

    /** Dossier mods/ du profil actif. */
    public static File modsDir(GameEngine engine) {
        return new File(GameProfilePaths.resolveRuntimeGameDirectory(engine), "mods");
    }

    /** Règle la mémoire du profil actif et de la configuration (utilisée au lancement). */
    public static void setRam(int gb, LauncherConfig config) {
        ProfileManager pm = ProfileManager.get();
        LauncherProfile active = pm == null ? null : pm.getActive();
        if (active != null) {
            active.setRam(gb);
            pm.update(active);
        }
        if (config != null) config.updateValue("allocatedram", String.valueOf((double) gb));
    }

    /** Installe la meilleure version d'un projet Modrinth (et ses dépendances obligatoires absentes). */
    public static String installProject(String project, File modsDir, String mcVersion, Utils.ModloaderType loader,
                                        Consumer<String> progress) throws IOException {
        List<String> loaders = ModrinthApi.loadersFor(loader);
        JsonObject version = ModrinthApi.bestVersion(project, loaders, mcVersion);
        if (version == null) throw new IOException(project + " n'existe pas pour " + mcVersion + ".");
        JsonObject p = ModrinthApi.project(project);
        String title = p != null ? ModrinthApi.str(p, "title") : project;
        progress.accept("Installation de " + title + "...");
        ModrinthApi.download(ModrinthApi.primaryFile(version), modsDir, false);
        return title;
    }

    /** Applique un correctif du diagnostic de crash. Renvoie un message pour le joueur. */
    public static String apply(CrashDoctor.Fix fix, CrashDoctor.Context ctx, LauncherConfig config,
                               Consumer<String> progress) throws IOException {
        switch (fix.kind) {
            case DISABLE_MOD:
                fix.jar.disable();
                return fix.jar.displayName + " est désactivé. Relance le jeu.";
            case INSTALL_PROJECT:
                return installProject(fix.project, ctx.modsDir, ctx.mcVersion, ctx.loader, progress) + " est installé. Relance le jeu.";
            case SET_RAM:
                setRam(fix.ramGb, config);
                return "Mémoire réglée sur " + fix.ramGb + " Go. Relance le jeu.";
            case OPEN_LOG:
                SystemOpen.openAsync(ctx.gameLog, null);
                return null;
            case OPEN_MODS:
                ctx.modsDir.mkdirs();
                SystemOpen.openAsync(ctx.modsDir, null);
                return null;
            default:
                return null;
        }
    }

    /**
     * Corrige les problèmes trouvés avant lancement : mods d'un autre loader
     * désactivés, mods d'une autre version remplacés par la bonne version (ou
     * désactivés), dépendances manquantes installées.
     */
    public static String fixAll(ModCompatibility.Report report, File modsDir, String mcVersion,
                                Utils.ModloaderType loader, Consumer<String> progress) {
        List<String> done = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (ModCompatibility.Issue issue : report.issues) {
            try {
                switch (issue.kind) {
                    case WRONG_LOADER:
                        issue.jar.disable();
                        done.add(issue.jar.displayName + " désactivé");
                        break;
                    case WRONG_GAME_VERSION:
                        if (issue.fixVersion != null) {
                            progress.accept("Version adaptée de " + issue.jar.displayName + "...");
                            File installed = ModrinthApi.download(ModrinthApi.primaryFile(issue.fixVersion), modsDir, false);
                            if (!installed.equals(issue.jar.file)) issue.jar.file.delete();
                            done.add(issue.jar.displayName + " remplacé par la bonne version");
                        } else {
                            issue.jar.disable();
                            done.add(issue.jar.displayName + " désactivé (aucune version pour " + mcVersion + ")");
                        }
                        break;
                    case MISSING_DEPENDENCY:
                        done.add(installProject(issue.missingProjectId, modsDir, mcVersion, loader, progress) + " installé");
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                failed.add((issue.jar != null ? issue.jar.displayName : issue.missingName) + " : " + e.getMessage());
            }
        }
        StringBuilder sb = new StringBuilder();
        if (!done.isEmpty()) sb.append("Corrigé : ").append(String.join(", ", done)).append('.');
        if (!failed.isEmpty()) sb.append(sb.length() > 0 ? "\n" : "").append("Impossible : ").append(String.join(" ; ", failed)).append('.');
        return sb.toString();
    }
}
