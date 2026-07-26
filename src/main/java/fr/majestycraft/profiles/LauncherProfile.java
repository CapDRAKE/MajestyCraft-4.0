package fr.majestycraft.profiles;

import fr.majestycraft.Utils;

import java.util.UUID;

/**
 * Un profil de jeu nommé : une combinaison version/modloader/RAM/arguments avec
 * son propre dossier de jeu (mods, configs, resource packs...).
 * Simple POJO sérialisé en JSON par {@link ProfileManager}.
 */
public class LauncherProfile {

    private String id;
    private String name;
    /**
     * Nom du dossier sous bin/game/. Ne change jamais après création (le renommage
     * du profil ne déplace pas les fichiers), ce qui évite toute perte de mods.
     */
    private String dirName;
    private String version;
    private String modloader;
    private double ram = 2.0D;
    private String gameSize = "0";
    private boolean useVmArguments = false;
    private String vmArguments = "";
    private long createdAt;
    private long lastUsedAt;
    /** Dernier lancement réel du jeu avec ce profil (0 = jamais lancé). */
    private long lastPlayedAt = 0L;
    /** Temps de jeu total cumulé avec ce profil, en millisecondes. */
    private long totalPlaytimeMs = 0L;
    /** Couleur d'identité du profil (hex #RRGGBB), assignée à la création. */
    private String color;

    public LauncherProfile() {
    }

    public LauncherProfile(String name, String dirName, String version, Utils.ModloaderType modloader) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.dirName = dirName;
        this.version = version;
        this.modloader = modloader == null ? Utils.ModloaderType.VANILLA.name() : modloader.name();
        this.createdAt = System.currentTimeMillis();
        this.lastUsedAt = this.createdAt;
    }

    public LauncherProfile copyWith(String newName, String newDirName) {
        LauncherProfile copy = new LauncherProfile(newName, newDirName, this.version, getModloaderType());
        copy.ram = this.ram;
        copy.gameSize = this.gameSize;
        copy.useVmArguments = this.useVmArguments;
        copy.vmArguments = this.vmArguments;
        copy.color = this.color;
        return copy;
    }

    /**
     * Libellé relatif de la dernière session ("joué il y a 2 h", "jamais lancé"…).
     */
    public String describeLastPlayed() {
        if (lastPlayedAt <= 0L) {
            return "jamais lancé";
        }
        long deltaMs = System.currentTimeMillis() - lastPlayedAt;
        if (deltaMs < 0) deltaMs = 0;
        long min = deltaMs / 60000L;
        if (min < 1) return "joué à l'instant";
        if (min < 60) return "joué il y a " + min + " min";
        long hours = min / 60L;
        if (hours < 24) return "joué il y a " + hours + " h";
        long days = hours / 24L;
        if (days < 7) return "joué il y a " + days + " j";
        long weeks = days / 7L;
        if (weeks < 5) return "joué il y a " + weeks + " sem";
        long months = days / 30L;
        if (months < 12) return "joué il y a " + months + " mois";
        return "joué il y a " + (days / 365L) + " an(s)";
    }

    /** Clé de tri : plus récent d'abord (dernier lancement, sinon création). */
    public long recencyKey() {
        return Math.max(lastPlayedAt, createdAt);
    }

    /** Temps de jeu total lisible ("3 h 12", "45 min", "—" si jamais joué). */
    public String describeTotalPlaytime() {
        if (totalPlaytimeMs <= 0L) return "—";
        long totalMin = totalPlaytimeMs / 60000L;
        if (totalMin < 1) return "< 1 min";
        long hours = totalMin / 60L;
        long min = totalMin % 60L;
        if (hours < 1) return min + " min";
        return hours + " h" + (min > 0 ? String.format(" %02d", min) : "");
    }

    public Utils.ModloaderType getModloaderType() {
        try {
            return Utils.ModloaderType.valueOf(this.modloader);
        } catch (Exception e) {
            return Utils.ModloaderType.VANILLA;
        }
    }

    public void setModloaderType(Utils.ModloaderType type) {
        this.modloader = type == null ? Utils.ModloaderType.VANILLA.name() : type.name();
    }

    /** Libellé court du couple loader+version, ex: "Fabric 1.21.11". */
    public String describeTarget() {
        String loaderName = getModloaderType().name().toLowerCase();
        loaderName = Character.toUpperCase(loaderName.charAt(0)) + loaderName.substring(1);
        if (getModloaderType() == Utils.ModloaderType.NEOFORGE) loaderName = "NeoForge";
        if (getModloaderType() == Utils.ModloaderType.OPTIFINE) loaderName = "OptiFine";
        return loaderName + " " + (version == null ? "?" : version);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDirName() { return dirName; }
    public void setDirName(String dirName) { this.dirName = dirName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getModloader() { return modloader; }
    public void setModloader(String modloader) { this.modloader = modloader; }
    public double getRam() { return ram; }
    public void setRam(double ram) { this.ram = ram; }
    public String getGameSize() { return gameSize; }
    public void setGameSize(String gameSize) { this.gameSize = gameSize; }
    public boolean isUseVmArguments() { return useVmArguments; }
    public void setUseVmArguments(boolean useVmArguments) { this.useVmArguments = useVmArguments; }
    public String getVmArguments() { return vmArguments; }
    public void setVmArguments(String vmArguments) { this.vmArguments = vmArguments; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(long lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public long getLastPlayedAt() { return lastPlayedAt; }
    public void setLastPlayedAt(long lastPlayedAt) { this.lastPlayedAt = lastPlayedAt; }
    public long getTotalPlaytimeMs() { return totalPlaytimeMs; }
    public void setTotalPlaytimeMs(long totalPlaytimeMs) { this.totalPlaytimeMs = totalPlaytimeMs; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
