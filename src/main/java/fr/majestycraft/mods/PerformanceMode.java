package fr.majestycraft.mods;

import com.google.gson.JsonObject;
import fr.majestycraft.Utils;
import fr.trxyy.alternative.alternative_api.utils.OperatingSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Mode performance en un clic : mémoire adaptée à la machine et mods d'optimisation
 * reconnus (rendu, logique du jeu, mémoire, culling) pour le loader du profil.
 */
public final class PerformanceMode {

    /** Un « rôle » d'optimisation : le premier projet disponible pour la version est retenu. */
    private static final class Role {
        final String label;
        final List<String> candidates;

        Role(String label, String... candidates) {
            this.label = label;
            this.candidates = Arrays.asList(candidates);
        }
    }

    /** Identifiants de mods (dans les jars) qui remplacent déjà le moteur de rendu. */
    private static final Set<String> RENDERER_MOD_IDS = new HashSet<>(Arrays.asList(
            "sodium", "embeddium", "rubidium", "optifabric", "optifine", "xenon", "magnesium"));

    public static final class Machine {
        public final int totalRamGb;
        public final int cores;
        public final boolean chromebook;
        public final boolean arm;

        Machine(int totalRamGb, int cores, boolean chromebook, boolean arm) {
            this.totalRamGb = totalRamGb;
            this.cores = cores;
            this.chromebook = chromebook;
            this.arm = arm;
        }

        public boolean isLowEnd() {
            return totalRamGb <= 8 || cores <= 4 || chromebook;
        }

        public String describe() {
            String kind = chromebook ? "Chromebook" : (arm ? "machine ARM" : "PC");
            return kind + " · " + totalRamGb + " Go de RAM · " + cores + " cœurs";
        }
    }

    public static final class Result {
        public final List<String> installed = new ArrayList<>();
        public final List<String> alreadyPresent = new ArrayList<>();
        public final List<String> unavailable = new ArrayList<>();
        public int ramGb;
        public boolean vmArgumentsCleaned;
    }

    /** Mods prévus : versions Modrinth à installer (titre dans "_title"), déjà là, indisponibles. */
    public static final class Plan {
        String mcVersion;
        Utils.ModloaderType loader;
        public final List<JsonObject> toInstall = new ArrayList<>();
        public final List<String> alreadyPresent = new ArrayList<>();
        public final List<String> unavailable = new ArrayList<>();

        public List<String> titles() {
            List<String> t = new ArrayList<>();
            for (JsonObject v : toInstall) t.add(ModrinthApi.str(v, "_title"));
            return t;
        }
    }

    /** Configuration conseillée pour une machine et un profil. */
    public static final class Advice {
        public int ramGb;
        public String ramReason;
        public String vmArguments;
        public boolean vmArgumentsChange;
        /** Distances conseillées, ou -1 si les réglages actuels conviennent. */
        public int renderDistance = -1;
        public int simulationDistance = -1;
        public int currentRenderDistance = -1;
        public final List<String> extraSlugs = new ArrayList<>();
    }

    /**
     * Options du ramasse-miettes G1 (que le launcher active déjà) adaptées à un client :
     * moins de pauses et de micro-saccades. Toutes reconnues de Java 8 à Java 25.
     */
    static final List<String> TUNED_JVM_ARGUMENTS = Arrays.asList(
            "-XX:+ParallelRefProcEnabled", "-XX:+DisableExplicitGC", "-XX:+PerfDisableSharedMem",
            "-XX:MaxTenuringThreshold=1", "-XX:SurvivorRatio=32");

    private PerformanceMode() {
    }

    /**
     * Configuration conseillée : mémoire selon la machine, la version et le nombre de mods,
     * arguments Java optimisés, mods en plus selon la carte graphique, distance de rendu
     * réduite sur les petites machines.
     */
    public static Advice advise(SystemProbe.Result pc, Utils.ModloaderType loader, String mcVersion, int modCount,
                                String currentVmArguments, int currentRenderDistance) {
        Advice a = new Advice();
        boolean modded = supportsMods(loader);
        boolean modern = versionAtLeast(mcVersion, 18);

        // Mémoire : le besoin du jeu, plafonné pour toujours laisser de la place au système
        int need = modded ? (modern ? 4 : 3) + modCount / 50 : (modern ? 3 : 2);
        int cap = pc.totalRamGb <= 4 ? Math.max(1, pc.totalRamGb - 2)
                : pc.totalRamGb <= 8 ? pc.totalRamGb / 2
                : Math.min(pc.totalRamGb - 4, 10);
        a.ramGb = Math.max(Math.min(2, cap), Math.min(need, cap));
        int left = pc.totalRamGb - a.ramGb;
        a.ramReason = (modded ? loaderLabel(loader) + " " + mcVersion + " avec " + modCount + " mod" + (modCount > 1 ? "s" : "")
                : "Minecraft " + mcVersion) + (a.ramGb < need ? ", limité par la mémoire de ton PC" : "")
                + " ; il reste " + left + " Go à ton système. Trop de mémoire ralentit aussi le jeu.";

        // Arguments Java : options obsolètes retirées, réglages du ramasse-miettes ajoutés
        a.vmArguments = mergeVmArguments(currentVmArguments, TUNED_JVM_ARGUMENTS);
        a.vmArgumentsChange = !a.vmArguments.equals(currentVmArguments == null ? "" : currentVmArguments.trim());

        // Mods en plus : FPS réduits en arrière-plan partout, moins de faces dessinées sur les petites cartes
        a.extraSlugs.add("dynamic-fps");
        boolean weakGpu = pc.gpuKind == SystemProbe.GpuKind.INTEGRATED || pc.gpuKind == SystemProbe.GpuKind.SOFTWARE || pc.chromebook;
        if ((weakGpu || pc.totalRamGb <= 8) && loader != Utils.ModloaderType.FORGE) a.extraSlugs.add("moreculling");

        // Distance de rendu : seulement réduite, jamais augmentée
        int render = pc.gpuKind == SystemProbe.GpuKind.SOFTWARE || pc.chromebook ? 6 : weakGpu || pc.totalRamGb <= 8 ? 8 : -1;
        // Pas encore d'options.txt : le jeu partira de sa distance par défaut (12)
        a.currentRenderDistance = currentRenderDistance < 0 ? 12 : currentRenderDistance;
        if (render > 0 && a.currentRenderDistance > render) {
            a.renderDistance = render;
            a.simulationDistance = Math.max(5, render - 2);
        }
        return a;
    }

    /** La version est-elle au moins 1.{minor} (les versions 26.x et plus le sont toutes) ? */
    static boolean versionAtLeast(String version, int minor) {
        if (version == null) return false;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+)\\.(\\d+)").matcher(version);
        if (!m.find()) return false;
        int major = Integer.parseInt(m.group(1));
        return major > 1 || Integer.parseInt(m.group(2)) >= minor;
    }

    private static String loaderLabel(Utils.ModloaderType t) {
        switch (t) {
            case FABRIC: return "Fabric";
            case QUILT: return "Quilt";
            case FORGE: return "Forge";
            case NEOFORGE: return "NeoForge";
            default: return t.name();
        }
    }

    /** Arguments actuels nettoyés, puis réglages conseillés (une option déjà présente est remplacée). */
    static String mergeVmArguments(String current, List<String> tuned) {
        java.util.Set<String> tunedKeys = new HashSet<>();
        for (String t : tuned) tunedKeys.add(optionKey(t));
        List<String> out = new ArrayList<>();
        String cleaned = cleanVmArguments(current);
        if (!cleaned.isEmpty()) {
            for (String a : cleaned.split("\\s+")) {
                if (!tunedKeys.contains(optionKey(a))) out.add(a);
            }
        }
        out.addAll(tuned);
        return String.join(" ", out);
    }

    /** "-XX:+Foo" / "-XX:-Foo" / "-XX:Foo=1" -> "Foo" ; les autres arguments restent tels quels. */
    private static String optionKey(String arg) {
        if (!arg.startsWith("-XX:")) return arg;
        String k = arg.substring(4);
        if (k.startsWith("+") || k.startsWith("-")) k = k.substring(1);
        int eq = k.indexOf('=');
        return eq >= 0 ? k.substring(0, eq) : k;
    }

    public static Machine detectMachine() {
        int ram = 8;
        try {
            java.lang.management.OperatingSystemMXBean bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean) {
                long bytes = ((com.sun.management.OperatingSystemMXBean) bean).getTotalPhysicalMemorySize();
                if (bytes > 0) ram = (int) Math.max(1, Math.round(bytes / (1024.0 * 1024 * 1024)));
            }
        } catch (Throwable ignored) {
        }
        return new Machine(ram, Runtime.getRuntime().availableProcessors(),
                OperatingSystem.isChromeOS(), OperatingSystem.isHostArm64());
    }

    /**
     * Mémoire conseillée : assez pour un jeu moddé fluide, sans priver le système
     * (au moins 2 Go laissés à l'OS, davantage sur les petites machines).
     */
    public static int recommendedRamGb(Machine m, boolean modded) {
        int ram;
        if (m.totalRamGb <= 4) ram = 2;
        else if (m.totalRamGb <= 8) ram = 3;
        else if (m.totalRamGb <= 16) ram = 4;
        else ram = modded ? 6 : 4;
        return Math.max(1, Math.min(ram, m.totalRamGb - 2));
    }

    /** Mods d'optimisation, par rôle, pour ce loader. */
    private static List<Role> rolesFor(Utils.ModloaderType loader) {
        switch (loader) {
            case FABRIC:
            case QUILT:
                return Arrays.asList(
                        new Role("rendu", "sodium"),
                        new Role("logique du jeu", "lithium"),
                        new Role("mémoire", "ferrite-core"),
                        new Role("affichage des entités", "entityculling"),
                        new Role("interface et texte", "immediatelyfast"),
                        new Role("démarrage", "modernfix"));
            case NEOFORGE:
                return Arrays.asList(
                        new Role("rendu", "sodium", "embeddium"),
                        new Role("mémoire", "ferrite-core"),
                        new Role("affichage des entités", "entityculling"),
                        new Role("interface et texte", "immediatelyfast"),
                        new Role("démarrage", "modernfix"));
            case FORGE:
                return Arrays.asList(
                        new Role("rendu", "embeddium", "rubidium"),
                        new Role("mémoire", "ferrite-core"),
                        new Role("affichage des entités", "entityculling"),
                        new Role("démarrage", "modernfix"));
            default:
                return Collections.emptyList();
        }
    }

    /** Le mode performance peut-il installer des mods pour ce loader ? */
    public static boolean supportsMods(Utils.ModloaderType loader) {
        return !rolesFor(loader).isEmpty();
    }

    /**
     * Installe les mods d'optimisation manquants (et leurs dépendances obligatoires)
     * dans modsDir. Bloquant ; progress reçoit des messages à afficher.
     */
    public static Result installMods(File modsDir, String mcVersion, Utils.ModloaderType loader, Consumer<String> progress) throws IOException {
        return installPlan(planMods(modsDir, mcVersion, loader, Collections.emptyList(), progress), modsDir, progress);
    }

    /** Télécharge les mods du plan (fichiers vérifiés). Bloquant. */
    public static Result installPlan(Plan plan, File modsDir, Consumer<String> progress) throws IOException {
        Result result = new Result();
        result.alreadyPresent.addAll(plan.alreadyPresent);
        result.unavailable.addAll(plan.unavailable);
        for (JsonObject v : plan.toInstall) {
            String title = ModrinthApi.str(v, "_title");
            progress.accept("Installation de " + title + "...");
            JsonObject file = ModrinthApi.primaryFile(v);
            if (file == null) continue;
            ModrinthApi.download(file, modsDir, false);
            result.installed.add(title);
        }
        if (!plan.toInstall.isEmpty() && plan.mcVersion != null) {
            progress.accept("Vérification des dépendances...");
            result.installed.addAll(ModActions.installMissingDependencies(modsDir, plan.mcVersion, plan.loader, progress));
        }
        return result;
    }

    /**
     * Mods d'optimisation à installer (et leurs dépendances obligatoires), sans rien
     * télécharger : ce qui manque dans modsDir pour ce loader et cette version, plus les
     * projets extraSlugs (Dynamic FPS...). Bloquant, demande le réseau.
     */
    public static Plan planMods(File modsDir, String mcVersion, Utils.ModloaderType loader, List<String> extraSlugs,
                                Consumer<String> progress) throws IOException {
        Plan result = new Plan();
        result.mcVersion = mcVersion;
        result.loader = loader;
        List<String> loaders = ModrinthApi.loadersFor(loader);
        List<Role> roles = new ArrayList<>(rolesFor(loader));
        if (!roles.isEmpty()) {
            for (String slug : extraSlugs) {
                roles.add(new Role("dynamic-fps".equals(slug) ? "FPS en arrière-plan"
                        : "moreculling".equals(slug) ? "faces cachées" : slug, slug));
            }
        }

        // Ce qui est déjà là : projets Modrinth et identifiants de mods des jars actifs
        Set<String> installedProjects = new HashSet<>();
        Set<String> installedModIds = new HashSet<>();
        Map<String, ModJar> byHash = new LinkedHashMap<>();
        for (ModJar jar : ModJar.scan(modsDir)) {
            if (!jar.enabled) continue;
            installedModIds.addAll(jar.modIds);
            byHash.put(jar.sha1(), jar);
        }
        for (JsonObject v : ModrinthApi.versionsByHash(byHash.keySet()).values()) {
            installedProjects.add(ModrinthApi.str(v, "project_id"));
        }

        List<JsonObject> toInstall = result.toInstall;
        for (Role role : roles) {
            progress.accept("Recherche : " + role.label + "...");
            if (role.label.equals("rendu") && !Collections.disjoint(installedModIds, RENDERER_MOD_IDS)) {
                result.alreadyPresent.add("moteur de rendu optimisé");
                continue;
            }
            JsonObject chosen = null;
            String chosenTitle = null;
            boolean present = false;
            for (String slug : role.candidates) {
                JsonObject project = ModrinthApi.project(slug);
                if (project == null) continue;
                String projectId = ModrinthApi.str(project, "id");
                if (installedProjects.contains(projectId)) {
                    present = true;
                    result.alreadyPresent.add(ModrinthApi.str(project, "title"));
                    break;
                }
                JsonObject version = ModrinthApi.bestVersion(projectId, loaders, mcVersion);
                if (version != null) {
                    chosen = version;
                    chosenTitle = ModrinthApi.str(project, "title");
                    break;
                }
            }
            if (present) continue;
            if (chosen == null) {
                result.unavailable.add(role.label);
                continue;
            }
            toInstall.add(chosen);
            installedProjects.add(ModrinthApi.str(chosen, "project_id"));
            chosen.addProperty("_title", chosenTitle);
        }

        // Dépendances obligatoires (ex. Fabric API) des mods retenus
        List<JsonObject> deps = new ArrayList<>();
        for (JsonObject v : toInstall) {
            for (String required : ModrinthApi.requiredDependencies(v)) {
                String dep = ModrinthApi.forLoader(required, loader);
                if (installedProjects.contains(dep)) continue;
                JsonObject depVersion = ModrinthApi.bestVersion(dep, loaders, mcVersion);
                if (depVersion == null) continue;
                JsonObject project = ModrinthApi.project(dep);
                depVersion.addProperty("_title", project != null ? ModrinthApi.str(project, "title") : dep);
                deps.add(depVersion);
                installedProjects.add(dep);
            }
        }
        toInstall.addAll(deps);
        return result;
    }

    /* ─────────────────────────── Réglages vidéo (options.txt) ─────────────────────────── */

    /** Valeur d'une option de options.txt (-1 si absente ou illisible). */
    public static int readOption(File optionsFile, String key) {
        try {
            if (!optionsFile.isFile()) return -1;
            for (String line : java.nio.file.Files.readAllLines(optionsFile.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                if (line.startsWith(key + ":")) return Integer.parseInt(line.substring(key.length() + 1).trim());
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    /**
     * Écrit des options dans les fichiers donnés (créés si absents), avec la même date de
     * modification : la synchronisation des réglages partagés n'en remplace aucun.
     */
    public static void writeOptions(Map<String, String> values, File... files) throws IOException {
        long stamp = System.currentTimeMillis();
        for (File f : files) {
            List<String> lines = f.isFile()
                    ? new ArrayList<>(java.nio.file.Files.readAllLines(f.toPath(), java.nio.charset.StandardCharsets.UTF_8))
                    : new ArrayList<>();
            for (Map.Entry<String, String> e : values.entrySet()) {
                boolean found = false;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).startsWith(e.getKey() + ":")) {
                        lines.set(i, e.getKey() + ":" + e.getValue());
                        found = true;
                    }
                }
                if (!found) lines.add(e.getKey() + ":" + e.getValue());
            }
            f.getParentFile().mkdirs();
            java.nio.file.Files.write(f.toPath(), lines, java.nio.charset.StandardCharsets.UTF_8);
            f.setLastModified(stamp);
        }
    }

    /**
     * Arguments JVM personnalisés sans les options obsolètes qui empêchent Java 17+
     * de démarrer (CMS, PermGen...) ; le launcher applique déjà un profil G1 optimisé.
     */
    public static String cleanVmArguments(String args) {
        if (args == null) return "";
        List<String> kept = new ArrayList<>();
        for (String a : args.trim().split("\\s+")) {
            if (a.isEmpty()) continue;
            String l = a.toLowerCase();
            if (l.contains("concmarksweep") || l.contains("cmsincremental") || l.contains("permsize")
                    || l.contains("aggressiveopts") || l.equals("-xincgc") || l.contains("useparnewgc")
                    || l.startsWith("-xmx") || l.startsWith("-xms")) {
                continue; // -Xmx / -Xms : la mémoire est réglée par le launcher
            }
            kept.add(a);
        }
        return String.join(" ", kept);
    }
}
