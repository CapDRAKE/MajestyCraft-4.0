package fr.majestycraft.launcher;

import com.jfoenix.controls.JFXButton;
import fr.majestycraft.App;
import fr.majestycraft.Utils;
import fr.majestycraft.mods.PerformanceMode;
import fr.majestycraft.mods.SystemProbe;
import fr.majestycraft.profiles.LauncherProfile;
import fr.majestycraft.profiles.ProfileManager;
import fr.majestycraft.util.MajestyDialog;
import fr.majestycraft.util.ToggleSwitch;
import fr.trxyy.alternative.alternative_api.GameEngine;
import fr.trxyy.alternative.alternative_api.utils.FontLoader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * Mode performance : analyse du PC (processeur, mémoire, carte graphique), puis rapport
 * avec la configuration conseillée pour le profil actif (mémoire, arguments Java, mods
 * d'optimisation, distance de rendu) ; chaque point peut être décoché avant d'appliquer.
 */
public final class PerformanceFlow {

    /** Résultat de l'analyse, prêt à afficher et à appliquer. */
    static final class Analysis {
        SystemProbe.Result pc;
        LauncherProfile profile;
        Utils.ModloaderType modLoader;
        boolean viaFabricCopy;
        int modCount;
        PerformanceMode.Advice advice;
        PerformanceMode.Plan plan;
        boolean offline;
        File profileOptions;
        File sharedOptions;
    }

    private PerformanceFlow() {
    }

    /** Analyse en arrière-plan (le bouton affiche « Analyse de ton PC... »), puis rapport. */
    public static void start(Window owner, LauncherPanel pane, GameEngine engine, JFXButton button, Runnable afterApply) {
        ProfileManager pm = ProfileManager.get();
        LauncherProfile profile = pm == null ? null : pm.getActive();
        if (profile == null) return;
        String label = button != null ? button.getText() : null;
        // Bouton icône seule (accueil) : un indicateur de chargement plutôt qu'un texte qui déborderait
        boolean showWaitText = label != null && !label.isEmpty();
        Node icon = button != null ? button.getGraphic() : null;
        if (button != null) {
            button.setDisable(true);
            if (showWaitText) {
                button.setText("Analyse de ton PC...");
            } else {
                ProgressIndicator spinner = new ProgressIndicator();
                spinner.setPrefSize(18, 18);
                spinner.setStyle("-fx-progress-color: #ffb000;");
                button.setGraphic(spinner);
            }
        }
        Thread t = new Thread(() -> {
            Analysis a = analyze(profile);
            Platform.runLater(() -> {
                if (button != null) {
                    button.setDisable(false);
                    if (showWaitText) button.setText(label);
                    else button.setGraphic(icon);
                }
                showReport(owner, a, pane, engine, afterApply);
            });
        }, "MajestyLauncher-PerformanceAnalysis");
        t.setDaemon(true);
        t.start();
    }

    /** Analyse complète pour un profil. Bloquant (commandes système et Modrinth). */
    static Analysis analyze(LauncherProfile profile) {
        ProfileManager pm = ProfileManager.get();
        Analysis a = new Analysis();
        a.profile = profile;
        a.pc = SystemProbe.probe();
        Utils.ModloaderType loader = profile.getModloaderType();
        a.viaFabricCopy = loader == Utils.ModloaderType.VANILLA;
        a.modLoader = a.viaFabricCopy ? Utils.ModloaderType.FABRIC : loader;

        File profileDir = pm.resolveProfileDirectory(profile);
        File modsDir = new File(profileDir, "mods");
        a.modCount = a.viaFabricCopy ? 0 : countMods(modsDir);
        a.profileOptions = new File(profileDir, "options.txt");
        a.sharedOptions = new File(profileDir.getParentFile(), "_shared-userdata" + File.separator + "options.txt");
        int render = PerformanceMode.readOption(a.profileOptions, "renderDistance");
        if (render < 0) render = PerformanceMode.readOption(a.sharedOptions, "renderDistance");

        a.advice = PerformanceMode.advise(a.pc, a.modLoader, profile.getVersion(), a.modCount, profile.getVmArguments(), render);
        if (PerformanceMode.supportsMods(a.modLoader)) {
            try {
                // Copie Fabric : dossier encore inexistant, donc tous les mods sont à installer
                File planDir = a.viaFabricCopy ? new File(profileDir, "mods-copie-inexistante") : modsDir;
                a.plan = PerformanceMode.planMods(planDir, profile.getVersion(), a.modLoader, a.advice.extraSlugs, s -> { });
            } catch (IOException e) {
                a.offline = true;
            }
        }
        return a;
    }

    private static int countMods(File modsDir) {
        File[] files = modsDir.listFiles((d, n) -> n.toLowerCase().endsWith(".jar"));
        return files == null ? 0 : files.length;
    }

    /* ─────────────────────────── Rapport ─────────────────────────── */

    private static void showReport(Window owner, Analysis a, LauncherPanel pane, GameEngine engine, Runnable afterApply) {
        buildReport(owner, a, pane, engine, afterApply).show();
    }

    /** Fenêtre du rapport, sans l'afficher (rendus hors écran). */
    static MajestyDialog buildReport(Window owner, Analysis a, LauncherPanel pane, GameEngine engine, Runnable afterApply) {
        VBox content = new VBox(6);
        content.getChildren().add(section("TON PC"));
        content.getChildren().add(line("Processeur : " + a.pc.describeCpu()));
        content.getChildren().add(line("Mémoire : " + a.pc.describeRam()));
        content.getChildren().add(line("Carte graphique : " + a.pc.describeGpu()));
        content.getChildren().add(line("Système : " + a.pc.system));

        content.getChildren().add(section("CONFIGURATION CONSEILLÉE"));
        int currentRam = (int) Math.round(a.profile.getRam());
        ToggleSwitch ram = toggle("Mémoire : " + a.advice.ramGb + " Go (actuellement " + currentRam + " Go)", a.advice.ramGb != currentRam);
        content.getChildren().addAll(ram, note(a.advice.ramReason));

        ToggleSwitch args = toggle("Arguments Java optimisés", a.advice.vmArgumentsChange);
        content.getChildren().addAll(args, note(a.advice.vmArgumentsChange
                ? "Ramasse-miettes réglé pour le jeu : moins de saccades. Options obsolètes retirées."
                : "Déjà en place."));

        ToggleSwitch mods = null;
        if (a.plan != null) {
            List<String> titles = a.plan.titles();
            mods = toggle(titles.isEmpty() ? "Mods d'optimisation : déjà installés"
                    : "Mods d'optimisation : " + titles.size() + " à installer", !titles.isEmpty());
            mods.setDisable(titles.isEmpty());
            String detail = titles.isEmpty() ? "" : String.join(", ", titles) + ".";
            if (a.viaFabricCopy) detail += " Via une copie Fabric de ce profil (« " + copyName(a.profile) + " »), le profil actuel reste intact.";
            if (!a.plan.alreadyPresent.isEmpty()) detail += " Déjà là : " + String.join(", ", a.plan.alreadyPresent) + ".";
            content.getChildren().addAll(mods, note(detail.trim()));
        } else if (a.offline) {
            content.getChildren().add(note("Mods : Modrinth injoignable, réessaie plus tard."));
        } else if (a.profile.getModloaderType() == Utils.ModloaderType.OPTIFINE) {
            content.getChildren().add(note("Mods : incompatibles avec OptiFine. Un profil Fabric permet d'utiliser Sodium et les autres."));
        }

        ToggleSwitch video = null;
        if (a.advice.renderDistance > 0) {
            video = toggle("Distance de rendu : " + a.advice.renderDistance + " (actuellement " + a.advice.currentRenderDistance + ")", true);
            content.getChildren().addAll(video, note("Plus fluide sur ta carte graphique. S'applique à tous tes profils."));
        }

        final ToggleSwitch fRam = ram, fArgs = args, fMods = mods, fVideo = video;
        return new MajestyDialog(owner, "MODE PERFORMANCE", "Optimiser « " + a.profile.getName() + " »", null)
                .content(content)
                .cancel("Annuler")
                .work("Appliquer", "primary", progress -> apply(a, fRam.isSelected(), fArgs.isSelected(),
                        fMods != null && fMods.isSelected(), fVideo != null && fVideo.isSelected(), pane, engine, progress))
                .afterWork(() -> {
                    if (afterApply != null) afterApply.run();
                });
    }

    private static String copyName(LauncherProfile p) {
        return p.getName() + " · Performance";
    }

    /* ─────────────────────────── Application ─────────────────────────── */

    /** Applique les points choisis puis active le profil (ou sa copie Fabric). Bloquant. */
    static String apply(Analysis a, boolean ram, boolean args, boolean mods, boolean video,
                        LauncherPanel pane, GameEngine engine, Consumer<String> progress) throws Exception {
        ProfileManager pm = ProfileManager.get();
        LauncherProfile target = a.profile;
        List<String> done = new ArrayList<>();

        if (mods && a.viaFabricCopy) {
            target = pm.create(copyName(a.profile), a.profile.getVersion(), Utils.ModloaderType.FABRIC);
            target.setRam(a.profile.getRam());
            target.setGameSize(a.profile.getGameSize());
            target.setVmArguments(a.profile.getVmArguments());
            target.setUseVmArguments(a.profile.isUseVmArguments());
            done.add("profil « " + target.getName() + " » créé");
        }
        if (ram) {
            target.setRam(a.advice.ramGb);
            done.add("mémoire " + a.advice.ramGb + " Go");
        }
        if (args) {
            target.setVmArguments(a.advice.vmArguments);
            target.setUseVmArguments(true);
            done.add("arguments Java optimisés");
        }
        pm.update(target);

        if (mods && a.plan != null) {
            File modsDir = new File(pm.resolveProfileDirectory(target), "mods");
            PerformanceMode.Result r = PerformanceMode.installPlan(a.plan, modsDir, progress);
            if (!r.installed.isEmpty()) done.add(r.installed.size() + " mod" + (r.installed.size() > 1 ? "s" : "") + " installé" + (r.installed.size() > 1 ? "s" : ""));
        }
        if (video) {
            Map<String, String> options = new LinkedHashMap<>();
            options.put("renderDistance", String.valueOf(a.advice.renderDistance));
            options.put("simulationDistance", String.valueOf(a.advice.simulationDistance));
            PerformanceMode.writeOptions(options, new File(pm.resolveProfileDirectory(target), "options.txt"), a.sharedOptions);
            done.add("distance de rendu " + a.advice.renderDistance);
        }

        // Activation (recopie dans la configuration utilisée au lancement) sur le thread JavaFX
        final LauncherProfile activated = target;
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                pm.activate(activated, engine);
                if (App.getInstance() != null) App.getInstance().applyGameLinksFromConfigAsync();
                if (pane != null) pane.refreshProfilesUI();
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        return done.isEmpty() ? "Rien à changer : ce profil est déjà bien réglé."
                : "C'est prêt : " + String.join(", ", done) + ". Clique sur JOUER.";
    }

    /**
     * Pour l'assistant : analyse et applique au profil actif tout ce qui est conseillé
     * (mods d'optimisation seulement si withMods ; jamais de copie d'un profil vanilla). Bloquant.
     */
    static String applyAllToActive(LauncherPanel pane, GameEngine engine, Consumer<String> progress, boolean withMods) throws Exception {
        LauncherProfile profile = ProfileManager.get().getActive();
        progress.accept("Analyse de ton PC...");
        Analysis a = analyze(profile);
        boolean mods = withMods && !a.viaFabricCopy && a.plan != null && !a.plan.toInstall.isEmpty();
        return apply(a, true, true, mods, a.advice.renderDistance > 0, pane, engine, progress);
    }

    /* ─────────────────────────── Mise en forme ─────────────────────────── */

    private static Label section(String text) {
        Label l = new Label(text);
        l.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        l.setStyle("-fx-text-fill: rgba(255,176,0,0.95);");
        l.setPadding(new Insets(8, 0, 2, 0));
        return l;
    }

    private static Label line(String text) {
        return MajestyDialog.text(text, 12F, "rgba(255,255,255,0.86)");
    }

    private static Label note(String text) {
        Label l = MajestyDialog.text(text, 10.5F, "rgba(255,255,255,0.50)");
        l.setPadding(new Insets(0, 0, 4, 46));
        return l;
    }

    private static ToggleSwitch toggle(String text, boolean selected) {
        ToggleSwitch t = new ToggleSwitch(text);
        t.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12.5F));
        t.setStyle("-fx-text-fill: rgba(255,255,255,0.92);");
        t.setWrapText(true);
        t.setMaxWidth(MajestyDialog.WIDTH - 56);
        t.setSelectedInstantly(selected);
        return t;
    }
}
