package fr.majestycraft.mods;

import com.google.gson.JsonObject;
import fr.majestycraft.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contrôle des mods actifs avant de lancer le jeu : mauvais loader (lu dans le jar,
 * sans réseau), version de Minecraft non prise en charge et dépendances obligatoires
 * manquantes (d'après Modrinth). Les mods inconnus de Modrinth ne sont vérifiés que
 * sur le loader.
 */
public final class ModCompatibility {

    public enum Kind { WRONG_LOADER, WRONG_GAME_VERSION, MISSING_DEPENDENCY }

    public static final class Issue {
        public final Kind kind;
        /** Mod concerné (null pour une dépendance manquante). */
        public final ModJar jar;
        /** Dépendance manquante : projet Modrinth et nom. */
        public final String missingProjectId;
        public final String missingName;
        /** Mod incompatible : version Modrinth qui corrige, si elle existe. */
        public final JsonObject fixVersion;
        /** Mods qui demandent la dépendance manquante. */
        public final List<String> requiredBy;

        Issue(Kind kind, ModJar jar, String missingProjectId, String missingName, JsonObject fixVersion, List<String> requiredBy) {
            this.kind = kind;
            this.jar = jar;
            this.missingProjectId = missingProjectId;
            this.missingName = missingName;
            this.fixVersion = fixVersion;
            this.requiredBy = requiredBy;
        }

        /** Phrase en français pour le joueur. */
        public String describe(String mcVersion, String loaderLabel) {
            switch (kind) {
                case WRONG_LOADER:
                    return jar.displayName + " est un mod " + jar.kindLabel() + " : il ne fonctionne pas avec " + loaderLabel + ".";
                case WRONG_GAME_VERSION:
                    return jar.displayName + " n'est pas prévu pour Minecraft " + mcVersion
                            + (fixVersion != null ? " (une version compatible existe)." : ".");
                default:
                    return missingName + " est nécessaire à " + String.join(", ", requiredBy) + " mais n'est pas installé.";
            }
        }
    }

    public static final class Report {
        public final List<Issue> issues = new ArrayList<>();
        /** Vrai si Modrinth n'a pas pu être consulté (contrôle partiel). */
        public boolean offline;

        public boolean isEmpty() {
            return issues.isEmpty();
        }
    }

    private ModCompatibility() {
    }

    /** Vérifie les mods actifs de modsDir pour cette version et ce loader. Bloquant. */
    public static Report check(File modsDir, String mcVersion, Utils.ModloaderType loader) {
        Report report = new Report();
        List<String> loaders = ModrinthApi.loadersFor(loader);
        if (loaders.isEmpty()) return report; // vanilla / OptiFine : pas de dossier de mods à contrôler

        List<ModJar> enabled = new ArrayList<>();
        for (ModJar jar : ModJar.scan(modsDir)) {
            if (jar.enabled) enabled.add(jar);
        }
        Set<ModJar> flagged = new HashSet<>();
        for (ModJar jar : enabled) {
            if (!jar.supportsLoader(loader)) {
                report.issues.add(new Issue(Kind.WRONG_LOADER, jar, null, null, null, null));
                flagged.add(jar);
            }
        }

        // Dépendances déclarées dans les jars (Modrinth ne les liste pas toujours) :
        // clé = projet Modrinth présumé, valeur = mods qui le demandent.
        Set<String> available = new HashSet<>(ModJar.BUILT_IN_IDS);
        for (ModJar jar : enabled) available.addAll(jar.providedIds);
        Map<String, List<String>> localMissing = new LinkedHashMap<>();
        for (ModJar jar : enabled) {
            if (flagged.contains(jar)) continue;
            for (String id : jar.requiredIds) {
                if (available.contains(id)) continue;
                if (id.startsWith("fabric-") && !id.equals("fabric-language-kotlin") && available.contains("fabric-api")) continue;
                localMissing.computeIfAbsent(ModJar.projectForModId(id), k -> new ArrayList<>()).add(jar.displayName);
            }
        }

        Map<String, ModJar> byHash = new LinkedHashMap<>();
        try {
            for (ModJar jar : enabled) byHash.put(jar.sha1(), jar);
            Map<String, JsonObject> versions = ModrinthApi.versionsByHash(byHash.keySet());

            Set<String> installedProjects = new HashSet<>();
            for (JsonObject v : versions.values()) installedProjects.add(ModrinthApi.str(v, "project_id"));

            List<String> wrongVersionHashes = new ArrayList<>();
            Map<String, List<String>> missing = new LinkedHashMap<>();
            for (Map.Entry<String, JsonObject> e : versions.entrySet()) {
                ModJar jar = byHash.get(e.getKey());
                JsonObject v = e.getValue();
                if (!flagged.contains(jar) && !ModrinthApi.supportsAnyLoader(v, loaders)) {
                    report.issues.add(new Issue(Kind.WRONG_LOADER, jar, null, null, null, null));
                    flagged.add(jar);
                } else if (!flagged.contains(jar) && !ModrinthApi.supportsGameVersion(v, mcVersion)) {
                    wrongVersionHashes.add(e.getKey());
                }
                if (flagged.contains(jar)) continue;
                for (String dep : ModrinthApi.requiredDependencies(v)) {
                    if (!installedProjects.contains(dep)) {
                        missing.computeIfAbsent(dep, k -> new ArrayList<>()).add(jar.displayName);
                    }
                }
            }

            // Dépendances lues dans les jars : projet présumé -> identifiant Modrinth, pour fusionner
            for (Map.Entry<String, List<String>> e : localMissing.entrySet()) {
                JsonObject project = ModrinthApi.project(e.getKey());
                if (project == null) continue; // pas sur Modrinth : rien à proposer d'installer
                String id = ModrinthApi.str(project, "id");
                if (installedProjects.contains(id)) continue;
                List<String> by = missing.computeIfAbsent(id, k -> new ArrayList<>());
                for (String n : e.getValue()) if (!by.contains(n)) by.add(n);
            }

            Map<String, JsonObject> fixes = wrongVersionHashes.isEmpty() ? Collections.emptyMap()
                    : ModrinthApi.latestByHash(wrongVersionHashes, loaders, mcVersion);
            for (String hash : wrongVersionHashes) {
                JsonObject fix = fixes.get(hash);
                if (fix != null && !ModrinthApi.supportsGameVersion(fix, mcVersion)) fix = null;
                report.issues.add(new Issue(Kind.WRONG_GAME_VERSION, byHash.get(hash), null, null, fix, null));
            }

            Map<String, String> titles = missing.isEmpty() ? new HashMap<>() : ModrinthApi.projectTitles(missing.keySet());
            for (Map.Entry<String, List<String>> e : missing.entrySet()) {
                String title = titles.containsKey(e.getKey()) ? titles.get(e.getKey()) : e.getKey();
                report.issues.add(new Issue(Kind.MISSING_DEPENDENCY, null, e.getKey(), title, null, e.getValue()));
            }
        } catch (IOException e) {
            report.offline = true;
            Set<String> reported = new HashSet<>();
            for (Issue i : report.issues) if (i.kind == Kind.MISSING_DEPENDENCY) reported.add(i.missingProjectId);
            for (Map.Entry<String, List<String>> m : localMissing.entrySet()) {
                if (!reported.contains(m.getKey())) {
                    report.issues.add(new Issue(Kind.MISSING_DEPENDENCY, null, m.getKey(), m.getKey(), null, m.getValue()));
                }
            }
        }
        return report;
    }
}
