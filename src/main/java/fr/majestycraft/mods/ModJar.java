package fr.majestycraft.mods;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.majestycraft.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Un mod du dossier mods/ (.jar, ou .jar.disabled s'il est désactivé), avec ce que
 * son fichier déclare : identifiants (y compris ceux fournis et les bibliothèques
 * embarquées), nom affiché, loader visé et dépendances obligatoires.
 */
public final class ModJar {

    /** Loader déclaré par les métadonnées du jar. */
    public enum Kind { FABRIC, QUILT, FORGE, NEOFORGE, UNKNOWN }

    /** Identifiants fournis par le loader ou le jeu lui-même : jamais « manquants ». */
    public static final Set<String> BUILT_IN_IDS = new HashSet<>(Arrays.asList(
            "minecraft", "java", "fabricloader", "fabric-loader", "quilt_loader", "quilt_base", "forge", "neoforge",
            "javafml", "lowcodefml", "mixinextras", "mixin"));

    private static final Pattern TOML_MOD_ID = Pattern.compile("(?m)^\\s*modId\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern TOML_NAME = Pattern.compile("(?m)^\\s*displayName\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern TOML_DEP_BLOCK = Pattern.compile("\\[\\[\\s*dependencies\\.[^\\]]+\\]\\]([^\\[]*)");
    private static final Pattern TOML_DEP_REQUIRED = Pattern.compile("(?m)^\\s*(?:mandatory\\s*=\\s*true|type\\s*=\\s*\"required\")");

    public final File file;
    public final boolean enabled;
    public final Kind kind;
    /** Identifiants du mod lui-même. */
    public final Set<String> modIds;
    /** Identifiants disponibles grâce à ce jar : les siens, ceux qu'il fournit, ses jars embarqués. */
    public final Set<String> providedIds;
    /** Identifiants de mods obligatoires selon les métadonnées du jar. */
    public final Set<String> requiredIds;
    public final String displayName;

    private String sha1;

    private ModJar(File file, Kind kind, Set<String> modIds, Set<String> providedIds, Set<String> requiredIds, String displayName) {
        this.file = file;
        this.enabled = !file.getName().toLowerCase(Locale.ROOT).endsWith(".disabled");
        this.kind = kind;
        this.modIds = modIds;
        this.providedIds = providedIds;
        this.requiredIds = requiredIds;
        this.displayName = displayName;
    }

    /** Mods du dossier (actifs et désactivés), triés par nom. */
    public static List<ModJar> scan(File modsDir) {
        List<ModJar> out = new ArrayList<>();
        File[] files = modsDir == null ? null : modsDir.listFiles();
        if (files == null) return out;
        for (File f : files) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (f.isFile() && (n.endsWith(".jar") || n.endsWith(".jar.disabled"))) {
                out.add(read(f));
            }
        }
        out.sort(Comparator.comparing(j -> j.displayName.toLowerCase(Locale.ROOT)));
        return out;
    }

    public static ModJar read(File f) {
        Kind kind = Kind.UNKNOWN;
        Set<String> ids = new LinkedHashSet<>();
        Set<String> provided = new LinkedHashSet<>();
        Set<String> required = new LinkedHashSet<>();
        String name = null;
        try (ZipFile zip = new ZipFile(f)) {
            String quilt = text(zip, "quilt.mod.json");
            String fabric = text(zip, "fabric.mod.json");
            String neo = text(zip, "META-INF/neoforge.mods.toml");
            String forge = text(zip, "META-INF/mods.toml");
            if (quilt != null) {
                kind = Kind.QUILT;
                JsonObject loader = json(quilt).getAsJsonObject("quilt_loader");
                if (loader != null) {
                    addId(ids, ModrinthApi.str(loader, "id"));
                    if (loader.has("metadata")) name = ModrinthApi.str(loader.getAsJsonObject("metadata"), "name");
                    readQuiltList(loader.get("provides"), provided);
                    readQuiltList(loader.get("depends"), required);
                }
            } else if (fabric != null) {
                kind = Kind.FABRIC;
                JsonObject o = json(fabric);
                addId(ids, ModrinthApi.str(o, "id"));
                name = ModrinthApi.str(o, "name");
                readFabricMetadata(o, provided, required);
            } else if (neo != null || forge != null) {
                String toml = neo != null ? neo : forge;
                kind = neo != null ? Kind.NEOFORGE : Kind.FORGE;
                // Les modId des [[mods]] ; ceux des dépendances sont lus à part
                String modsPart = toml.split("\\[\\[\\s*dependencies")[0];
                Matcher mm = TOML_MOD_ID.matcher(modsPart);
                while (mm.find()) addId(ids, mm.group(1));
                Matcher n = TOML_NAME.matcher(toml);
                if (n.find()) name = n.group(1);
                Matcher block = TOML_DEP_BLOCK.matcher(toml);
                while (block.find()) {
                    Matcher id = TOML_MOD_ID.matcher(block.group(1));
                    if (id.find() && TOML_DEP_REQUIRED.matcher(block.group(1)).find()) addId(required, id.group(1));
                }
            } else if (zip.getEntry("mcmod.info") != null) {
                kind = Kind.FORGE; // Forge 1.12 et avant
            }
            readNestedJars(zip, provided);
        } catch (Exception ignored) {
            // jar illisible : reste « inconnu »
        }
        provided.addAll(ids);
        required.removeAll(BUILT_IN_IDS);
        required.removeAll(provided);
        if (name == null || name.trim().isEmpty()) name = baseName(f);
        return new ModJar(f, kind, ids, provided, required, name);
    }

    /** Ce jar peut-il être chargé par ce modloader ? (inconnu = on ne se prononce pas) */
    public boolean supportsLoader(Utils.ModloaderType loader) {
        if (kind == Kind.UNKNOWN || loader == null) return true;
        switch (loader) {
            case FABRIC:
                return kind == Kind.FABRIC;
            case QUILT:
                return kind == Kind.QUILT || kind == Kind.FABRIC;
            case FORGE:
                return kind == Kind.FORGE;
            case NEOFORGE:
                // NeoForge 1.20.1 à 1.20.4 lit encore les mods.toml de Forge
                return kind == Kind.NEOFORGE || kind == Kind.FORGE;
            default:
                return true;
        }
    }

    public String kindLabel() {
        switch (kind) {
            case FABRIC: return "Fabric";
            case QUILT: return "Quilt";
            case FORGE: return "Forge";
            case NEOFORGE: return "NeoForge";
            default: return "inconnu";
        }
    }

    public String sha1() throws IOException {
        if (sha1 == null) sha1 = ModrinthApi.sha1(file);
        return sha1;
    }

    /** Désactive le mod (ajoute « .disabled »). Renvoie le nouveau fichier. */
    public File disable() throws IOException {
        if (!enabled) return file;
        File target = new File(file.getParentFile(), file.getName() + ".disabled");
        if (target.exists() || !file.renameTo(target)) {
            throw new IOException("Impossible de désactiver " + file.getName());
        }
        return target;
    }

    /**
     * Identifiant de mod (dépendance) -> projet Modrinth. Les modules de Fabric API
     * (fabric-xxx-api-v1...) viennent tous du projet « fabric-api ».
     */
    public static String projectForModId(String modId) {
        String id = modId.toLowerCase(Locale.ROOT);
        if (id.equals("fabric-language-kotlin")) return "fabric-language-kotlin";
        if (id.equals("fabric") || id.startsWith("fabric-")) return "fabric-api";
        if (id.equals("cloth_config") || id.equals("cloth-config2")) return "cloth-config";
        if (id.equals("geckolib3")) return "geckolib";
        if (id.equals("qsl") || id.startsWith("quilted_fabric_api") || id.startsWith("quilt_")) return "qsl";
        return id.replace('_', '-');
    }

    // ------------------------------------------------------------------ lecture

    private static void readFabricMetadata(JsonObject o, Set<String> provided, Set<String> required) {
        if (o.has("provides") && o.get("provides").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("provides")) addId(provided, e.getAsString());
        }
        if (o.has("depends") && o.get("depends").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("depends").entrySet()) addId(required, e.getKey());
        }
    }

    private static void readQuiltList(JsonElement list, Set<String> out) {
        if (list == null || !list.isJsonArray()) return;
        for (JsonElement e : list.getAsJsonArray()) {
            if (e.isJsonPrimitive()) {
                addId(out, e.getAsString());
            } else if (e.isJsonObject()) {
                JsonObject d = e.getAsJsonObject();
                boolean optional = d.has("optional") && d.get("optional").getAsBoolean();
                if (!optional) addId(out, ModrinthApi.str(d, "id"));
            }
        }
    }

    /** Jars embarqués (Fabric : META-INF/jars, Forge : META-INF/jarjar) : leurs identifiants sont fournis. */
    private static void readNestedJars(ZipFile zip, Set<String> provided) {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            String n = e.getName();
            if (!(n.startsWith("META-INF/jars/") || n.startsWith("META-INF/jarjar/")) || !n.endsWith(".jar")) continue;
            try (ZipInputStream nested = new ZipInputStream(zip.getInputStream(e))) {
                ZipEntry inner;
                while ((inner = nested.getNextEntry()) != null) {
                    String in = inner.getName();
                    if (in.equals("fabric.mod.json") || in.equals("quilt.mod.json")) {
                        JsonObject o = json(new String(readAll(nested), StandardCharsets.UTF_8));
                        JsonObject quilt = o.getAsJsonObject("quilt_loader");
                        addId(provided, quilt != null ? ModrinthApi.str(quilt, "id") : ModrinthApi.str(o, "id"));
                        if (o.has("provides") && o.get("provides").isJsonArray()) {
                            for (JsonElement p : o.getAsJsonArray("provides")) addId(provided, p.getAsString());
                        }
                    } else if (in.equals("META-INF/mods.toml") || in.equals("META-INF/neoforge.mods.toml")) {
                        String toml = new String(readAll(nested), StandardCharsets.UTF_8).split("\\[\\[\\s*dependencies")[0];
                        Matcher m = TOML_MOD_ID.matcher(toml);
                        while (m.find()) addId(provided, m.group(1));
                    }
                }
            } catch (Exception ignored) {
                // jar embarqué illisible : ignoré
            }
        }
    }

    private static void addId(Set<String> ids, String id) {
        if (id != null && !id.trim().isEmpty()) ids.add(id.trim().toLowerCase(Locale.ROOT));
    }

    private static String baseName(File f) {
        String n = f.getName();
        for (String suffix : Arrays.asList(".disabled", ".jar")) {
            if (n.toLowerCase(Locale.ROOT).endsWith(suffix)) n = n.substring(0, n.length() - suffix.length());
        }
        return n;
    }

    private static String text(ZipFile zip, String entry) throws IOException {
        ZipEntry e = zip.getEntry(entry);
        if (e == null) return null;
        try (InputStream in = zip.getInputStream(e)) {
            return new String(readAll(in), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0 && out.size() < 1_000_000) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private static JsonObject json(String s) {
        JsonElement e = new JsonParser().parse(s);
        return e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }
}
