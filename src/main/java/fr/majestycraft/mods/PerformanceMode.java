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

    private PerformanceMode() {
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
        Result result = new Result();
        List<String> loaders = ModrinthApi.loadersFor(loader);

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

        List<JsonObject> toInstall = new ArrayList<>();
        for (Role role : rolesFor(loader)) {
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
            for (String dep : ModrinthApi.requiredDependencies(v)) {
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

        for (JsonObject v : toInstall) {
            String title = ModrinthApi.str(v, "_title");
            progress.accept("Installation de " + title + "...");
            JsonObject file = ModrinthApi.primaryFile(v);
            if (file == null) continue;
            ModrinthApi.download(file, modsDir, false);
            result.installed.add(title);
        }
        return result;
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
