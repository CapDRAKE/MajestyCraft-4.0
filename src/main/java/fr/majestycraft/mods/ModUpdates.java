package fr.majestycraft.mods;

import com.google.gson.JsonObject;
import fr.majestycraft.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mises à jour des mods installés, d'après Modrinth : pour chaque jar reconnu, la
 * dernière version compatible avec la version de Minecraft et le loader du profil.
 */
public final class ModUpdates {

    public static final class Update {
        public final ModJar jar;
        public final JsonObject current;
        public final JsonObject latest;
        public final String title;

        Update(ModJar jar, JsonObject current, JsonObject latest, String title) {
            this.jar = jar;
            this.current = current;
            this.latest = latest;
            this.title = title;
        }

        public String currentName() {
            String n = ModrinthApi.str(current, "version_number");
            return n != null ? n : "?";
        }

        public String latestName() {
            String n = ModrinthApi.str(latest, "version_number");
            return n != null ? n : "?";
        }
    }

    private ModUpdates() {
    }

    /** Mises à jour disponibles (mods actifs et désactivés). Bloquant, demande le réseau. */
    public static List<Update> find(File modsDir, String mcVersion, Utils.ModloaderType loader) throws IOException {
        List<Update> out = new ArrayList<>();
        List<String> loaders = ModrinthApi.loadersFor(loader);
        if (loaders.isEmpty()) return out;

        Map<String, ModJar> byHash = new LinkedHashMap<>();
        for (ModJar jar : ModJar.scan(modsDir)) {
            // Mod d'un autre loader : à désactiver, pas à « mettre à jour »
            if (jar.supportsLoader(loader)) byHash.put(jar.sha1(), jar);
        }
        if (byHash.isEmpty()) return out;

        Map<String, JsonObject> current = ModrinthApi.versionsByHash(byHash.keySet());
        Map<String, JsonObject> latest = ModrinthApi.latestByHash(current.keySet(), loaders, mcVersion);

        Map<String, JsonObject> worth = new LinkedHashMap<>();
        Set<String> projectIds = new HashSet<>();
        for (Map.Entry<String, JsonObject> e : latest.entrySet()) {
            JsonObject cur = current.get(e.getKey());
            JsonObject next = e.getValue();
            if (cur == null) continue;
            // Version stable installée : pas de bascule vers une bêta ou une alpha
            if (isRelease(cur) && !isRelease(next)) {
                next = ModrinthApi.bestVersion(ModrinthApi.str(cur, "project_id"), loaders, mcVersion);
                if (next == null || !isRelease(next)) continue;
            }
            if (sameVersion(cur, next) || ModrinthApi.primaryFile(next) == null) continue;
            if (!ModrinthApi.supportsGameVersion(next, mcVersion) || !ModrinthApi.supportsAnyLoader(next, loaders)) continue;
            // Plus récente, ou version adaptée quand l'actuelle ne correspond pas au profil
            boolean newer = published(next).compareTo(published(cur)) > 0;
            boolean adapts = !ModrinthApi.supportsGameVersion(cur, mcVersion) || !ModrinthApi.supportsAnyLoader(cur, loaders);
            if (!newer && !adapts) continue;
            worth.put(e.getKey(), next);
            projectIds.add(ModrinthApi.str(cur, "project_id"));
        }
        Map<String, String> titles = projectIds.isEmpty() ? new LinkedHashMap<>() : ModrinthApi.projectTitles(projectIds);

        for (Map.Entry<String, JsonObject> e : worth.entrySet()) {
            JsonObject cur = current.get(e.getKey());
            JsonObject next = e.getValue();
            String projectId = ModrinthApi.str(cur, "project_id");
            ModJar jar = byHash.get(e.getKey());
            String title = titles.containsKey(projectId) ? titles.get(projectId) : jar.displayName;
            out.add(new Update(jar, cur, next, title));
        }
        return out;
    }

    /**
     * Installe la nouvelle version à la place de l'ancienne (même état actif /
     * désactivé). L'ancien fichier n'est supprimé qu'une fois le nouveau vérifié.
     */
    public static File apply(Update update) throws IOException {
        File dir = update.jar.file.getParentFile();
        File installed = ModrinthApi.download(ModrinthApi.primaryFile(update.latest), dir, !update.jar.enabled);
        if (!installed.equals(update.jar.file) && update.jar.file.exists() && !update.jar.file.delete()) {
            throw new IOException("Ancienne version de " + update.title + " impossible à supprimer");
        }
        return installed;
    }

    /** Date de publication ISO 8601 (comparable comme texte). */
    private static String published(JsonObject v) {
        String d = ModrinthApi.str(v, "date_published");
        return d != null ? d : "";
    }

    private static boolean isRelease(JsonObject v) {
        return "release".equals(ModrinthApi.str(v, "version_type"));
    }

    private static boolean sameVersion(JsonObject a, JsonObject b) {
        String ia = ModrinthApi.str(a, "id");
        return ia != null && ia.equals(ModrinthApi.str(b, "id"));
    }
}
