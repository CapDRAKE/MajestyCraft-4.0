package fr.majestycraft.mods;

import fr.majestycraft.Utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/**
 * Diagnostic de crash en français : lit le journal du jeu et le dernier rapport de
 * crash, reconnaît les causes courantes et propose des correctifs applicables en un
 * clic par le launcher.
 */
public final class CrashDoctor {

    public enum FixKind { DISABLE_MOD, INSTALL_PROJECT, SET_RAM, OPEN_LOG, OPEN_MODS }

    /** Correctif proposé ; appliqué par le launcher (qui connaît profil et config). */
    public static final class Fix {
        public final FixKind kind;
        public final String label;
        public final ModJar jar;          // DISABLE_MOD
        public final String project;      // INSTALL_PROJECT : identifiant ou slug Modrinth
        public final int ramGb;           // SET_RAM

        private Fix(FixKind kind, String label, ModJar jar, String project, int ramGb) {
            this.kind = kind;
            this.label = label;
            this.jar = jar;
            this.project = project;
            this.ramGb = ramGb;
        }

        static Fix disable(ModJar jar) {
            return new Fix(FixKind.DISABLE_MOD, "Désactiver " + jar.displayName, jar, null, 0);
        }

        static Fix install(String project, String name) {
            return new Fix(FixKind.INSTALL_PROJECT, "Installer " + name, null, project, 0);
        }

        static Fix ram(int gb) {
            return new Fix(FixKind.SET_RAM, "Passer la mémoire à " + gb + " Go", null, null, gb);
        }

        static Fix openLog() {
            return new Fix(FixKind.OPEN_LOG, "Ouvrir le journal", null, null, 0);
        }

        static Fix openMods() {
            return new Fix(FixKind.OPEN_MODS, "Ouvrir le dossier des mods", null, null, 0);
        }
    }

    public static final class Diagnosis {
        public final String title;
        public final String explanation;
        public final List<Fix> fixes = new ArrayList<>();
        /** Ligne du journal qui a permis le diagnostic. */
        public String evidence;

        Diagnosis(String title, String explanation) {
            this.title = title;
            this.explanation = explanation;
        }
    }

    /** Contexte du lancement qui a planté. */
    public static final class Context {
        public File gameLog;
        public File gameDir;
        public File modsDir;
        public String mcVersion;
        public Utils.ModloaderType loader;
        public int ramGb;
        public int systemRamGb;
        public int exitCode;
        /** Rapports de crash plus anciens que ce moment : ignorés. */
        public long sessionStart;
    }

    /** Fabric / Quilt : « Mod 'A' (a) 1.0 requires version 2.0 or later of mod 'B' (b), which is missing! » */
    private static final Pattern FABRIC_MISSING = Pattern.compile(
            "Mod '([^']+)' \\(([^)]+)\\) \\S+ requires .+? of (?:mod '([^']+)' \\(([^)]+)\\)|([a-z0-9_\\-]+)), which is missing");
    private static final Pattern FABRIC_INCOMPATIBLE = Pattern.compile(
            "Mod '([^']+)' \\(([^)]+)\\) \\S+ is incompatible with .+? of mod '([^']+)' \\(([^)]+)\\)");
    private static final Pattern FORGE_MISSING = Pattern.compile(
            "Mod ID: '([^']+)', Requested by: '([^']+)', Expected range: '[^']*', Actual version: '\\[MISSING\\]'");
    private static final Pattern CLASS_VERSION = Pattern.compile(
            "(?:UnsupportedClassVersionError: ([^ ]+) has been compiled by a more recent version of the Java Runtime \\(class file version (\\d+)\\.0\\))");
    private static final Pattern MIXIN_FROM_MOD = Pattern.compile("(?i)mixin[^\\n]*?from mod ([a-z0-9_\\-]+)");
    private static final Pattern MIXIN_CONFIG = Pattern.compile("(?i)([a-z0-9_\\-]+)\\.mixins?\\.json|mixins\\.([a-z0-9_\\-]+)\\.json");
    private static final Pattern SUSPECTED = Pattern.compile("(?m)^\\s*Suspected Mods?:\\s*(.+)$");
    private static final Pattern DUPLICATE = Pattern.compile("(?i)duplicate mods? (?:found|for)[^\\n]*?['\"]?([a-z0-9_\\-]+)['\"]?");
    private static final Pattern DESCRIPTION = Pattern.compile("(?m)^Description: (.+)$");

    private CrashDoctor() {
    }

    public static Diagnosis analyze(Context ctx) {
        String log = tail(ctx.gameLog, 400_000);
        String report = newestCrashReport(ctx);
        String all = log + "\n" + report;
        List<ModJar> mods = ctx.modsDir == null ? new ArrayList<>() : ModJar.scan(ctx.modsDir);
        List<ModJar> enabled = new ArrayList<>();
        for (ModJar j : mods) if (j.enabled) enabled.add(j);

        Diagnosis d;
        // D'abord ce que le journal prouve, ensuite seulement les soupçons tirés des fichiers
        if ((d = memory(all, ctx)) != null) return finish(d, ctx);
        if ((d = heapTooBig(all, ctx)) != null) return finish(d, ctx);
        if ((d = missingDependencies(all, enabled)) != null) return finish(d, ctx);
        if ((d = incompatibleMods(all, enabled)) != null) return finish(d, ctx);
        if ((d = javaTooOld(all, enabled)) != null) return finish(d, ctx);
        if ((d = duplicates(all, enabled)) != null) return finish(d, ctx);
        if ((d = mixinFailure(all, enabled)) != null) return finish(d, ctx);
        if ((d = suspectedMods(report, enabled)) != null) return finish(d, ctx);
        if ((d = graphics(all)) != null) return finish(d, ctx);
        if ((d = wrongLoaderMods(enabled, ctx)) != null) return finish(d, ctx);

        Matcher desc = DESCRIPTION.matcher(report);
        String detail = desc.find() ? " Le rapport indique : « " + desc.group(1).trim() + " »." : "";
        d = new Diagnosis("Cause non identifiée",
                "Le jeu s'est arrêté (code " + ctx.exitCode + ") sans cause connue du launcher." + detail
                        + (enabled.isEmpty() ? "" : " Si tu as ajouté un mod récemment, essaie de le désactiver."));
        return finish(d, ctx);
    }

    private static Diagnosis finish(Diagnosis d, Context ctx) {
        if (ctx.modsDir != null && ctx.loader != null && ModrinthApi.loadersFor(ctx.loader).size() > 0) d.fixes.add(Fix.openMods());
        d.fixes.add(Fix.openLog());
        return d;
    }

    // ------------------------------------------------------------------ causes

    private static Diagnosis memory(String all, Context ctx) {
        int i = all.indexOf("java.lang.OutOfMemoryError");
        if (i < 0) return null;
        int target = Math.min(ctx.ramGb + 2, Math.max(2, ctx.systemRamGb - 2));
        Diagnosis d;
        if (target > ctx.ramGb) {
            d = new Diagnosis("Mémoire insuffisante",
                    "Le jeu a manqué de mémoire avec " + ctx.ramGb + " Go. Les mods et les packs de textures en demandent davantage.");
            d.fixes.add(Fix.ram(target));
        } else {
            d = new Diagnosis("Mémoire insuffisante",
                    "Le jeu a manqué de mémoire, et ta machine ne peut pas lui en donner plus. Retire des mods lourds "
                            + "ou un pack de textures en haute résolution, ou active le mode performance.");
        }
        d.evidence = lineAt(all, i);
        return d;
    }

    private static Diagnosis heapTooBig(String all, Context ctx) {
        String l = all.toLowerCase(Locale.ROOT);
        int i = l.indexOf("could not reserve enough space for");
        if (i < 0) i = l.indexOf("invalid maximum heap size");
        if (i < 0) return null;
        int target = Math.max(1, Math.min(ctx.ramGb - 1, Math.max(1, ctx.systemRamGb / 2)));
        Diagnosis d = new Diagnosis("Trop de mémoire demandée",
                "Java n'a pas pu réserver " + ctx.ramGb + " Go : ta machine n'a pas assez de mémoire libre.");
        if (target < ctx.ramGb) d.fixes.add(Fix.ram(target));
        d.evidence = lineAt(all, i);
        return d;
    }

    private static Diagnosis wrongLoaderMods(List<ModJar> enabled, Context ctx) {
        if (ctx.loader == null) return null;
        List<ModJar> wrong = new ArrayList<>();
        for (ModJar j : enabled) if (!j.supportsLoader(ctx.loader)) wrong.add(j);
        if (wrong.isEmpty()) return null;
        Diagnosis d = new Diagnosis("Mod pour un autre loader",
                names(wrong) + (wrong.size() > 1 ? " sont prévus" : " est prévu") + " pour " + wrong.get(0).kindLabel()
                        + ", pas pour " + loaderLabel(ctx.loader) + ".");
        for (ModJar j : wrong) d.fixes.add(Fix.disable(j));
        return d;
    }

    private static Diagnosis missingDependencies(String all, List<ModJar> enabled) {
        Set<String> missing = new LinkedHashSet<>();
        Set<String> requesters = new LinkedHashSet<>();
        String evidence = null;
        Matcher m = FABRIC_MISSING.matcher(all);
        while (m.find()) {
            requesters.add(m.group(1));
            missing.add(m.group(4) != null ? m.group(4) : m.group(5));
            if (evidence == null) evidence = m.group();
        }
        Matcher f = FORGE_MISSING.matcher(all);
        while (f.find()) {
            missing.add(f.group(1));
            requesters.add(f.group(2));
            if (evidence == null) evidence = f.group();
        }
        missing.removeIf(id -> id.equals("minecraft") || id.equals("java") || id.equals("fabricloader")
                || id.equals("forge") || id.equals("neoforge"));
        if (missing.isEmpty()) return null;
        // Noms lisibles depuis Modrinth (sinon l'identifiant du mod)
        List<String> names = new ArrayList<>();
        List<Fix> installs = new ArrayList<>();
        for (String id : missing) {
            String slug = ModJar.projectForModId(id);
            String name = id;
            try {
                com.google.gson.JsonObject project = ModrinthApi.project(slug);
                if (project != null) name = ModrinthApi.str(project, "title");
            } catch (IOException ignored) {
                // hors ligne : identifiant brut
            }
            names.add(name);
            installs.add(Fix.install(slug, name));
        }
        boolean many = missing.size() > 1;
        Diagnosis d = new Diagnosis(many ? "Mods manquants" : "Mod manquant",
                (requesters.isEmpty() ? "Un mod" : joinFr(new ArrayList<>(requesters)))
                        + (requesters.size() > 1 ? " ont" : " a") + " besoin de " + joinFr(names)
                        + (many ? ", qui ne sont pas installés." : ", qui n'est pas installé."));
        d.fixes.addAll(installs);
        d.evidence = evidence;
        return d;
    }

    private static Diagnosis incompatibleMods(String all, List<ModJar> enabled) {
        Matcher m = FABRIC_INCOMPATIBLE.matcher(all);
        if (!m.find()) return null;
        Diagnosis d = new Diagnosis("Mods incompatibles",
                m.group(1) + " et " + m.group(3) + " ne peuvent pas être utilisés ensemble. Désactive l'un des deux.");
        ModJar a = byModId(enabled, m.group(2));
        ModJar b = byModId(enabled, m.group(4));
        if (a != null) d.fixes.add(Fix.disable(a));
        if (b != null) d.fixes.add(Fix.disable(b));
        d.evidence = m.group();
        return d;
    }

    private static Diagnosis javaTooOld(String all, List<ModJar> enabled) {
        Matcher m = CLASS_VERSION.matcher(all);
        if (!m.find()) return null;
        int javaVersion = Integer.parseInt(m.group(2)) - 44;
        ModJar culprit = jarContainingClass(enabled, m.group(1));
        Diagnosis d = new Diagnosis("Mod prévu pour une autre version",
                (culprit != null ? culprit.displayName : "Un mod") + " demande Java " + javaVersion
                        + " : il est fait pour une version plus récente de Minecraft. Installe la version du mod "
                        + "qui correspond à ton profil, ou désactive-le.");
        if (culprit != null) d.fixes.add(Fix.disable(culprit));
        d.evidence = m.group();
        return d;
    }

    private static Diagnosis duplicates(String all, List<ModJar> enabled) {
        Matcher m = DUPLICATE.matcher(all);
        if (!m.find()) return null;
        String id = m.group(1).toLowerCase(Locale.ROOT);
        List<ModJar> same = new ArrayList<>();
        for (ModJar j : enabled) if (j.modIds.contains(id)) same.add(j);
        Diagnosis d = new Diagnosis("Mod installé en double",
                "Le mod « " + id + " » est présent " + Math.max(2, same.size()) + " fois. Garde une seule version.");
        for (int i = 1; i < same.size(); i++) d.fixes.add(Fix.disable(same.get(i)));
        d.evidence = m.group();
        return d;
    }

    private static Diagnosis mixinFailure(String all, List<ModJar> enabled) {
        String lower = all.toLowerCase(Locale.ROOT);
        int i = lower.indexOf("mixin apply failed");
        if (i < 0) i = lower.indexOf("mixinapplyerror");
        if (i < 0) i = lower.indexOf("mixin transformation of");
        if (i < 0) return null;
        String around = all.substring(i, Math.min(all.length(), i + 2000));
        ModJar culprit = null;
        Matcher fromMod = MIXIN_FROM_MOD.matcher(around);
        if (fromMod.find()) culprit = byModId(enabled, fromMod.group(1));
        if (culprit == null) {
            Matcher cfg = MIXIN_CONFIG.matcher(around);
            while (cfg.find() && culprit == null) {
                culprit = byModId(enabled, cfg.group(1) != null ? cfg.group(1) : cfg.group(2));
            }
        }
        Diagnosis d = new Diagnosis("Mod incompatible avec cette version",
                (culprit != null ? culprit.displayName : "Un mod") + " modifie le jeu d'une façon qui ne fonctionne pas "
                        + "avec cette version de Minecraft ou avec un autre mod.");
        if (culprit != null) d.fixes.add(Fix.disable(culprit));
        d.evidence = lineAt(all, i);
        return d;
    }

    private static Diagnosis suspectedMods(String report, List<ModJar> enabled) {
        Matcher m = SUSPECTED.matcher(report);
        if (!m.find()) return null;
        String line = m.group(1).trim();
        if (line.equalsIgnoreCase("NONE") || line.isEmpty()) return null;
        List<ModJar> culprits = new ArrayList<>();
        Matcher ids = Pattern.compile("\\(([a-z0-9_\\-]+)\\)").matcher(line);
        while (ids.find()) {
            ModJar j = byModId(enabled, ids.group(1));
            if (j != null && !culprits.contains(j)) culprits.add(j);
        }
        Diagnosis d = new Diagnosis("Plantage provoqué par un mod",
                "Le rapport de crash désigne : " + line + ".");
        for (ModJar j : culprits) d.fixes.add(Fix.disable(j));
        d.evidence = m.group();
        return d;
    }

    private static Diagnosis graphics(String all) {
        String l = all.toLowerCase(Locale.ROOT);
        for (String marker : Arrays.asList("glfw error 65542", "glfw error 65543", "pixel format not accelerated",
                "the driver does not appear to support opengl", "glxbadfbconfig", "couldn't set pixel format",
                "no opengl context", "failed to create opengl context")) {
            int i = l.indexOf(marker);
            if (i >= 0) {
                Diagnosis d = new Diagnosis("Pilote graphique",
                        "Ta carte graphique ou son pilote ne fournit pas l'OpenGL dont Minecraft a besoin. Mets à jour "
                                + "le pilote graphique (site de NVIDIA, AMD ou Intel). Sur Chromebook, active "
                                + "chrome://flags/#crostini-gpu-support puis redémarre.");
                d.evidence = lineAt(all, i);
                return d;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ outils

    private static ModJar byModId(List<ModJar> jars, String id) {
        if (id == null) return null;
        String lower = id.toLowerCase(Locale.ROOT);
        for (ModJar j : jars) if (j.modIds.contains(lower)) return j;
        for (ModJar j : jars) {
            if (j.displayName.toLowerCase(Locale.ROOT).replace(' ', '-').contains(lower)) return j;
        }
        return null;
    }

    private static ModJar jarContainingClass(List<ModJar> jars, String className) {
        String entry = className.replace('.', '/') + ".class";
        for (ModJar j : jars) {
            try (ZipFile zip = new ZipFile(j.file)) {
                if (zip.getEntry(entry) != null) return j;
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static String names(List<ModJar> jars) {
        List<String> n = new ArrayList<>();
        for (ModJar j : jars) n.add(j.displayName);
        return joinFr(n);
    }

    /** « A », « A et B », « A, B et C ». */
    static String joinFr(List<String> items) {
        if (items.isEmpty()) return "";
        if (items.size() == 1) return items.get(0);
        return String.join(", ", items.subList(0, items.size() - 1)) + " et " + items.get(items.size() - 1);
    }

    private static String loaderLabel(Utils.ModloaderType t) {
        switch (t) {
            case FABRIC: return "Fabric";
            case QUILT: return "Quilt";
            case FORGE: return "Forge";
            case NEOFORGE: return "NeoForge";
            case OPTIFINE: return "OptiFine";
            default: return "Minecraft sans mods";
        }
    }

    private static String lineAt(String text, int index) {
        int start = text.lastIndexOf('\n', index) + 1;
        int end = text.indexOf('\n', index);
        String line = text.substring(start, end < 0 ? text.length() : end).trim();
        return line.length() > 300 ? line.substring(0, 300) + "…" : line;
    }

    /** Dernier rapport de crash écrit pendant la session (vide sinon). */
    private static String newestCrashReport(Context ctx) {
        if (ctx.gameDir == null) return "";
        File[] reports = new File(ctx.gameDir, "crash-reports").listFiles((dir, name) -> name.endsWith(".txt"));
        if (reports == null) return "";
        File newest = null;
        for (File f : reports) {
            if (f.lastModified() >= ctx.sessionStart && (newest == null || f.lastModified() > newest.lastModified())) newest = f;
        }
        return newest == null ? "" : tail(newest, 400_000);
    }

    /** Fin du fichier (les causes sont en fin de journal), au plus maxBytes. */
    static String tail(File f, int maxBytes) {
        if (f == null || !f.isFile()) return "";
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            long len = raf.length();
            long start = Math.max(0, len - maxBytes);
            byte[] buf = new byte[(int) (len - start)];
            raf.seek(start);
            raf.readFully(buf);
            return new String(buf, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
