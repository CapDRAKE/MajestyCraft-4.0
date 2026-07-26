package fr.majestycraft.profiles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.majestycraft.Utils;
import fr.trxyy.alternative.alternative_api.GameEngine;
import fr.trxyy.alternative.alternative_api.utils.config.EnumConfig;
import fr.trxyy.alternative.alternative_api.utils.config.LauncherConfig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Source de vérité du système de profils nommés.
 *
 * Persistance : un simple profiles.json dans le dossier du jeu, chargé une seule
 * fois au démarrage (aucune IO répétée pendant l'utilisation).
 *
 * Compatibilité : à chaque activation de profil, les valeurs du profil sont
 * recopiées dans launcher_config.json (pont de compatibilité). Tout le pipeline
 * existant (résolution des GameLinks, update(), GameUpdater, GameRunner) continue
 * de lire la config comme avant : le système de profils ne touche pas au chemin
 * critique du lancement.
 */
public final class ProfileManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ProfileManager instance;

    private final File profilesFile;
    private final File playDir;
    private final LauncherConfig config;
    private ProfilesData data;

    public static class ProfilesData {
        public int formatVersion = 1;
        public String activeProfileId;
        public List<LauncherProfile> profiles = new ArrayList<>();
    }

    private ProfileManager(File gameDir, File playDir, LauncherConfig config) {
        this.profilesFile = new File(gameDir, "profiles.json");
        this.playDir = playDir;
        this.config = config;
        load();
    }

    /**
     * Initialise le gestionnaire (idempotent). Doit être appelé après le chargement
     * de la config, avant la première résolution des GameLinks.
     */
    public static synchronized ProfileManager init(File gameDir, File playDir, LauncherConfig config) {
        if (instance == null) {
            instance = new ProfileManager(gameDir, playDir, config);
        }
        return instance;
    }

    /** @return l'instance, ou null si init() n'a pas encore été appelé */
    public static ProfileManager get() {
        return instance;
    }

    /* ─────────────────────────── Chargement / migration ─────────────────────────── */

    private void load() {
        if (profilesFile.isFile()) {
            try {
                String json = new String(Files.readAllBytes(profilesFile.toPath()), StandardCharsets.UTF_8);
                this.data = GSON.fromJson(json, ProfilesData.class);
            } catch (Exception e) {
                System.err.println("[Profiles] profiles.json illisible, recreation : " + e.getMessage());
            }
        }

        if (this.data == null || this.data.profiles == null) {
            this.data = new ProfilesData();
            migrateFromLegacy();
            save();
        }

        if (getActive() == null && !this.data.profiles.isEmpty()) {
            this.data.activeProfileId = this.data.profiles.get(0).getId();
            save();
        }
    }

    /**
     * Première exécution : importe les dossiers "style-version" historiques comme
     * profils (les mods/configs existants sont conservés tels quels), et garantit
     * qu'un profil correspondant à la sélection actuelle de la config existe.
     */
    private void migrateFromLegacy() {
        File[] children = playDir == null ? null : playDir.listFiles(File::isDirectory);
        if (children != null) {
            for (File dir : children) {
                LauncherProfile imported = parseLegacyDirName(dir.getName());
                if (imported != null) {
                    applyConfigDefaults(imported);
                    data.profiles.add(imported);
                }
            }
        }

        String cfgVersion = stringValue(config.getValue(EnumConfig.VERSION));
        Utils.ModloaderType cfgLoader = Utils.resolveSelectedModloader(config);

        LauncherProfile current = null;
        if (cfgVersion != null) {
            for (LauncherProfile p : data.profiles) {
                if (cfgVersion.equals(p.getVersion()) && p.getModloaderType() == cfgLoader) {
                    current = p;
                    break;
                }
            }
            if (current == null) {
                String niceName = buildDefaultName(cfgLoader, cfgVersion);
                current = new LauncherProfile(niceName, legacyDirNameFor(cfgLoader, cfgVersion), cfgVersion, cfgLoader);
                applyConfigDefaults(current);
                data.profiles.add(current);
            }
        }

        if (current == null && !data.profiles.isEmpty()) {
            current = data.profiles.get(0);
        }
        if (current != null) {
            data.activeProfileId = current.getId();
        }

        // Assigne une couleur d'identité stable aux profils migrés.
        for (int i = 0; i < data.profiles.size(); i++) {
            LauncherProfile p = data.profiles.get(i);
            if (p.getColor() == null) {
                p.setColor(COLOR_PALETTE[i % COLOR_PALETTE.length]);
            }
        }
    }

    /** Copie RAM/taille/arguments JVM actuels de la config dans un profil migré. */
    private void applyConfigDefaults(LauncherProfile p) {
        try {
            String ram = stringValue(config.getValue(EnumConfig.RAM));
            if (ram != null) p.setRam(Double.parseDouble(ram));
        } catch (Exception ignored) {}
        String size = stringValue(config.getValue(EnumConfig.GAME_SIZE));
        if (size != null) p.setGameSize(size);
        p.setUseVmArguments(Utils.booleanValue(config.getValue(EnumConfig.USE_VM_ARGUMENTS)));
        String vmArgs = stringValue(config.getValue(EnumConfig.VM_ARGUMENTS));
        if (vmArgs != null) p.setVmArguments(vmArgs);
    }

    /**
     * Reconnaît un nom de dossier historique "style-version" (ex: "fabric-1.21.11",
     * "forge_1_19_higher-1.20.1") et le convertit en profil. Null si non reconnu.
     */
    private LauncherProfile parseLegacyDirName(String dirName) {
        int sep = dirName.lastIndexOf('-');
        if (sep <= 0 || sep >= dirName.length() - 1) return null;

        String stylePart = dirName.substring(0, sep).toLowerCase(Locale.ROOT);
        String versionPart = dirName.substring(sep + 1);
        if (!versionPart.matches("(\\d+)(\\.\\d+)*([a-z0-9._-]*)")) return null;

        Utils.ModloaderType loader;
        if (stylePart.startsWith("forge")) loader = Utils.ModloaderType.FORGE;
        else if (stylePart.startsWith("fabric")) loader = Utils.ModloaderType.FABRIC;
        else if (stylePart.startsWith("quilt")) loader = Utils.ModloaderType.QUILT;
        else if (stylePart.startsWith("neoforge")) loader = Utils.ModloaderType.NEOFORGE;
        else if (stylePart.startsWith("optifine")) loader = Utils.ModloaderType.OPTIFINE;
        else if (stylePart.startsWith("vanilla")) loader = Utils.ModloaderType.VANILLA;
        else return null;

        LauncherProfile p = new LauncherProfile(buildDefaultName(loader, versionPart), dirName, versionPart, loader);
        return p;
    }

    private String buildDefaultName(Utils.ModloaderType loader, String version) {
        String label;
        switch (loader) {
            case FORGE: label = "Forge"; break;
            case FABRIC: label = "Fabric"; break;
            case QUILT: label = "Quilt"; break;
            case NEOFORGE: label = "NeoForge"; break;
            case OPTIFINE: label = "OptiFine"; break;
            default: label = "Vanilla"; break;
        }
        return label + " " + version;
    }

    /**
     * Reproduit le nom de dossier que l'ancien système aurait utilisé pour cette
     * combinaison : le profil migré retrouve ainsi exactement ses fichiers.
     */
    private String legacyDirNameFor(Utils.ModloaderType loader, String version) {
        fr.trxyy.alternative.alternative_api.GameStyle style = Utils.determineGameStyleFor(version, loader);
        return sanitize(style.name().toLowerCase(Locale.ROOT) + "-" + version);
    }

    /* ─────────────────────────── Accès ─────────────────────────── */

    /** Palette d'identité des profils (assignée en cycle à la création). */
    public static final String[] COLOR_PALETTE = {
            "#ff8a3d", "#4dd2ff", "#8b7bff", "#5bd97a",
            "#ff6b9d", "#ffd23d", "#3dd6c4", "#ff5c5c"
    };

    /** Profils triés du plus récemment utilisé au plus ancien. */
    public List<LauncherProfile> getProfiles() {
        List<LauncherProfile> sorted = new ArrayList<>(data.profiles);
        sorted.sort((a, b) -> Long.compare(b.recencyKey(), a.recencyKey()));
        return sorted;
    }

    /** Couleur du profil, avec repli déterministe si aucune n'est définie. */
    public String colorOf(LauncherProfile p) {
        if (p != null && p.getColor() != null && !p.getColor().trim().isEmpty()) {
            return p.getColor();
        }
        int idx = Math.max(0, data.profiles.indexOf(p));
        return COLOR_PALETTE[idx % COLOR_PALETTE.length];
    }

    /** Profil dont la session de jeu est en cours (pour créditer le temps de jeu). */
    private String sessionProfileId;

    /**
     * Démarre une session de jeu pour le profil actif : marque "joué maintenant"
     * et retient le profil auquel créditer le temps de jeu à la fin.
     */
    public void beginSession() {
        LauncherProfile active = getActive();
        if (active != null) {
            sessionProfileId = active.getId();
            active.setLastPlayedAt(System.currentTimeMillis());
            save();
        }
    }

    /**
     * Fin de session : ajoute la durée jouée (mesurée par GameRunner) au temps de
     * jeu total du profil de la session en cours.
     */
    public void addSessionPlaytime(long playedMs) {
        if (sessionProfileId == null || playedMs <= 0) return;
        for (LauncherProfile p : data.profiles) {
            if (p.getId().equals(sessionProfileId)) {
                p.setTotalPlaytimeMs(p.getTotalPlaytimeMs() + playedMs);
                break;
            }
        }
        sessionProfileId = null;
        save();
    }

    public LauncherProfile getActive() {
        if (data.activeProfileId == null) return null;
        for (LauncherProfile p : data.profiles) {
            if (data.activeProfileId.equals(p.getId())) return p;
        }
        return null;
    }

    public String getActiveDirNameOrNull() {
        LauncherProfile active = getActive();
        return active == null ? null : active.getDirName();
    }

    public File resolveProfileDirectory(LauncherProfile p) {
        return new File(playDir, p.getDirName());
    }

    /* ─────────────────────────── CRUD ─────────────────────────── */

    public LauncherProfile create(String name, String version, Utils.ModloaderType loader) {
        return create(name, version, loader, null);
    }

    public LauncherProfile create(String name, String version, Utils.ModloaderType loader, String color) {
        LauncherProfile p = new LauncherProfile(name.trim(), uniqueDirName(name), version, loader);
        applyConfigDefaults(p);
        p.setColor(color != null ? color : COLOR_PALETTE[data.profiles.size() % COLOR_PALETTE.length]);
        data.profiles.add(p);
        save();
        return p;
    }

    /**
     * Duplique un profil : mêmes réglages + copie des dossiers mods/config/
     * resourcepacks/shaderpacks (pas les saves ni les screenshots, trop lourds).
     * La copie de fichiers peut être longue : à appeler hors du thread JavaFX.
     */
    public LauncherProfile duplicate(LauncherProfile source, String newName) throws IOException {
        LauncherProfile copy = source.copyWith(newName.trim(), uniqueDirName(newName));

        File sourceDir = resolveProfileDirectory(source);
        File targetDir = resolveProfileDirectory(copy);
        String[] toCopy = {"mods", "config", "resourcepacks", "shaderpacks", "options.txt", "optionsshaders.txt", "optionsof.txt"};
        if (sourceDir.isDirectory()) {
            targetDir.mkdirs();
            for (String entry : toCopy) {
                File from = new File(sourceDir, entry);
                if (from.exists()) {
                    copyRecursive(from.toPath(), new File(targetDir, entry).toPath());
                }
            }
        }

        synchronized (this) {
            data.profiles.add(copy);
            save();
        }
        return copy;
    }

    public void rename(LauncherProfile p, String newName) {
        p.setName(newName.trim());
        save();
    }

    /* ─────────────────────────── Import / Export ─────────────────────────── */

    /** Manifeste embarqué dans un .zip de profil exporté. */
    private static class ProfileManifest {
        int formatVersion = 1;
        String name;
        String version;
        String modloader;
        double ram;
        String gameSize;
        boolean useVmArguments;
        String vmArguments;
        String color;
    }

    /**
     * Exporte un profil en .zip partageable : manifeste + dossiers mods/ et config/.
     * Opération I/O potentiellement longue : à appeler hors du thread JavaFX.
     */
    public void exportProfile(LauncherProfile p, File destZip) throws IOException {
        ProfileManifest m = new ProfileManifest();
        m.name = p.getName();
        m.version = p.getVersion();
        m.modloader = p.getModloader();
        m.ram = p.getRam();
        m.gameSize = p.getGameSize();
        m.useVmArguments = p.isUseVmArguments();
        m.vmArguments = p.getVmArguments();
        m.color = colorOf(p);

        File profileDir = resolveProfileDirectory(p);
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)))) {
            zip.putNextEntry(new ZipEntry("majesty-profile.json"));
            zip.write(GSON.toJson(m).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            for (String folder : new String[]{"mods", "config"}) {
                File dir = new File(profileDir, folder);
                if (dir.isDirectory()) {
                    zipDirectory(zip, dir, folder + "/");
                }
            }
        }
    }

    private void zipDirectory(ZipOutputStream zip, File dir, String prefix) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) return;
        byte[] buffer = new byte[8192];
        for (File child : children) {
            String entryName = prefix + child.getName();
            if (child.isDirectory()) {
                zipDirectory(zip, child, entryName + "/");
            } else {
                zip.putNextEntry(new ZipEntry(entryName));
                try (InputStream in = new BufferedInputStream(new FileInputStream(child))) {
                    int read;
                    while ((read = in.read(buffer)) != -1) zip.write(buffer, 0, read);
                }
                zip.closeEntry();
            }
        }
    }

    /**
     * Importe un profil depuis un .zip : crée un nouveau profil (nom rendu unique)
     * et extrait mods/ et config/ dans son dossier de jeu. Retourne le profil créé.
     * Opération I/O potentiellement longue : à appeler hors du thread JavaFX.
     */
    public LauncherProfile importProfile(File srcZip) throws IOException {
        ProfileManifest m = null;
        // 1re passe : lire le manifeste.
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(srcZip)))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if ("majesty-profile.json".equals(e.getName())) {
                    m = GSON.fromJson(new String(readAll(zin), StandardCharsets.UTF_8), ProfileManifest.class);
                    break;
                }
            }
        }
        if (m == null || m.name == null || m.version == null) {
            throw new IOException("Fichier de profil invalide (manifeste manquant).");
        }

        Utils.ModloaderType loader;
        try {
            loader = Utils.ModloaderType.valueOf(m.modloader);
        } catch (Exception ex) {
            loader = Utils.ModloaderType.VANILLA;
        }

        String name = uniqueName(m.name);
        LauncherProfile p = new LauncherProfile(name, uniqueDirName(name), m.version, loader);
        p.setRam(m.ram > 0 ? m.ram : 2.0D);
        p.setGameSize(m.gameSize != null ? m.gameSize : "0");
        p.setUseVmArguments(m.useVmArguments);
        p.setVmArguments(m.vmArguments != null ? m.vmArguments : "");
        p.setColor(m.color != null ? m.color : COLOR_PALETTE[data.profiles.size() % COLOR_PALETTE.length]);

        File targetDir = resolveProfileDirectory(p);
        targetDir.mkdirs();

        // 2e passe : extraire mods/ et config/ (garde-fou anti zip-slip).
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(srcZip)))) {
            ZipEntry e;
            byte[] buffer = new byte[8192];
            String targetCanonical = targetDir.getCanonicalPath() + File.separator;
            while ((e = zin.getNextEntry()) != null) {
                String entryName = e.getName();
                if (!entryName.startsWith("mods/") && !entryName.startsWith("config/")) continue;

                File out = new File(targetDir, entryName);
                if (!out.getCanonicalPath().startsWith(targetCanonical)) {
                    continue; // entrée sortant du dossier cible : ignorée
                }
                if (e.isDirectory()) {
                    out.mkdirs();
                } else {
                    File parent = out.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (OutputStream fout = new BufferedOutputStream(new FileOutputStream(out))) {
                        int read;
                        while ((read = zin.read(buffer)) != -1) fout.write(buffer, 0, read);
                    }
                }
            }
        }

        synchronized (this) {
            data.profiles.add(p);
            save();
        }
        return p;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) bos.write(buffer, 0, read);
        return bos.toByteArray();
    }

    /** Rend un nom de profil unique en suffixant " (importé)", " (2)", etc. */
    private String uniqueName(String base) {
        String candidate = base;
        int i = 1;
        while (nameTaken(candidate)) {
            candidate = i == 1 ? base + " (importé)" : base + " (" + i + ")";
            i++;
        }
        return candidate;
    }

    private boolean nameTaken(String name) {
        for (LauncherProfile p : data.profiles) {
            if (p.getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /**
     * Supprime un profil (jamais le dernier). Ne supprime le dossier de jeu que si
     * demandé explicitement.
     */
    public boolean delete(LauncherProfile p, boolean deleteFolder) {
        if (data.profiles.size() <= 1) return false;

        data.profiles.removeIf(x -> x.getId().equals(p.getId()));
        if (p.getId().equals(data.activeProfileId)) {
            data.activeProfileId = data.profiles.get(0).getId();
            applyActiveToConfig();
        }
        save();

        if (deleteFolder) {
            File dir = resolveProfileDirectory(p);
            // Garde-fou : on ne supprime que sous bin/game/, jamais la racine
            if (dir.isDirectory() && !dir.equals(playDir) && dir.getParentFile().equals(playDir)) {
                deleteRecursive(dir);
            }
        }
        return true;
    }

    /** Sauvegarde les réglages modifiés d'un profil (sans changer le profil actif). */
    public void update(LauncherProfile p) {
        save();
        if (p.getId().equals(data.activeProfileId)) {
            applyActiveToConfig();
        }
    }

    /* ─────────────────────────── Activation ─────────────────────────── */

    /**
     * Au démarrage : synchronise config + moteur avec le profil actif persisté,
     * sans en changer. Garantit la cohérence du pont avant la résolution des links.
     */
    public void activateInitial(GameEngine engine) {
        applyActiveToConfig();
        if (engine != null) {
            Utils.regGameStyle(engine, config);
        }
    }

    /**
     * Active un profil : recopie ses valeurs dans launcher_config.json (pont de
     * compatibilité lu par tout le pipeline existant) et met à jour le moteur.
     */
    public void activate(LauncherProfile p, GameEngine engine) {
        data.activeProfileId = p.getId();
        p.setLastUsedAt(System.currentTimeMillis());
        applyActiveToConfig();
        save();

        if (engine != null) {
            Utils.regGameStyle(engine, config);
        }
    }

    /** Recopie les valeurs du profil actif dans la config legacy. */
    private void applyActiveToConfig() {
        LauncherProfile p = getActive();
        if (p == null) return;

        Utils.ModloaderType loader = p.getModloaderType();
        HashMap<String, String> map = new HashMap<>();
        map.put("version", p.getVersion());
        map.put("allocatedram", String.valueOf(p.getRam()));
        map.put("gamesize", p.getGameSize());
        map.put("usevmarguments", String.valueOf(p.isUseVmArguments()));
        map.put("vmarguments", p.getVmArguments() == null ? "" : p.getVmArguments());
        map.put(EnumConfig.USE_FORGE.getOption(), String.valueOf(loader == Utils.ModloaderType.FORGE));
        map.put(EnumConfig.USE_FABRIC.getOption(), String.valueOf(loader == Utils.ModloaderType.FABRIC));
        map.put(EnumConfig.USE_QUILT.getOption(), String.valueOf(loader == Utils.ModloaderType.QUILT));
        map.put(EnumConfig.USE_NEOFORGE.getOption(), String.valueOf(loader == Utils.ModloaderType.NEOFORGE));
        map.put(EnumConfig.USE_OPTIFINE.getOption(), String.valueOf(loader == Utils.ModloaderType.OPTIFINE));
        config.updateValues(map);
    }

    /* ─────────────────────────── Helpers fichiers ─────────────────────────── */

    private void save() {
        try {
            File tmp = new File(profilesFile.getParentFile(), "profiles.json.tmp");
            Files.write(tmp.toPath(), GSON.toJson(data).getBytes(StandardCharsets.UTF_8));
            Files.move(tmp.toPath(), profilesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[Profiles] Sauvegarde impossible : " + e.getMessage());
        }
    }

    /** Nom de dossier sûr et unique dérivé du nom de profil. */
    private String uniqueDirName(String name) {
        String base = sanitize(name);
        String candidate = base;
        int i = 2;
        while (dirNameTaken(candidate) || new File(playDir, candidate).exists()) {
            candidate = base + "-" + i++;
        }
        return candidate;
    }

    private boolean dirNameTaken(String dirName) {
        for (LauncherProfile p : data.profiles) {
            if (dirName.equalsIgnoreCase(p.getDirName())) return true;
        }
        return false;
    }

    private static String sanitize(String raw) {
        if (raw == null) return "profil";
        String s = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[àâä]", "a").replaceAll("[éèêë]", "e")
                .replaceAll("[îï]", "i").replaceAll("[ôö]", "o")
                .replaceAll("[ùûü]", "u").replaceAll("ç", "c")
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        while (s.contains("..")) s = s.replace("..", ".");
        return s.isEmpty() ? "profil" : s;
    }

    private static void copyRecursive(Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            Files.createDirectories(target);
            try (java.util.stream.Stream<Path> stream = Files.list(source)) {
                for (Path child : stream.toArray(Path[]::new)) {
                    copyRecursive(child, target.resolve(child.getFileName()));
                }
            }
        } else {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteRecursive(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursive(child);
        }
        file.delete();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
