package fr.majestycraft.launcher;

import fr.majestycraft.App;
import fr.majestycraft.Utils;
import fr.majestycraft.mods.ModrinthApi;
import fr.majestycraft.profiles.LauncherProfile;
import fr.majestycraft.profiles.ProfileManager;
import fr.majestycraft.util.Icons;
import fr.majestycraft.util.MajestyDialog;
import fr.trxyy.alternative.alternative_api.utils.FontLoader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Assistant de premier lancement : « Comment veux-tu jouer ? ». Le launcher prépare le
 * profil correspondant (version, loader, réglages pour le PC) ; le joueur n'a qu'à
 * cliquer sur JOUER. Affiché une seule fois, et seulement aux nouveaux joueurs.
 */
public final class Onboarding {

    private static final String PREF_DONE = "onboardingDone";
    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private Onboarding() {
    }

    /**
     * Nouveau joueur : assistant jamais vu, aucun temps de jeu et aucun options.txt dans
     * les dossiers de jeu (Minecraft jamais lancé par ce launcher). Un joueur existant est
     * marqué tout de suite, pour ne jamais lui montrer l'assistant.
     */
    public static boolean isNewPlayer() {
        Preferences prefs = Preferences.userNodeForPackage(App.class);
        if (prefs.getBoolean(PREF_DONE, false)) return false;
        ProfileManager pm = ProfileManager.get();
        if (pm == null || pm.getProfiles().isEmpty()) return false;
        File playDir = pm.resolveProfileDirectory(pm.getProfiles().get(0)).getParentFile();
        boolean played = new File(playDir, "options.txt").isFile()
                || new File(playDir, "_shared-userdata" + File.separator + "options.txt").isFile();
        for (LauncherProfile p : pm.getProfiles()) {
            if (p.getLastPlayedAt() > 0 || p.getTotalPlaytimeMs() > 0
                    || new File(pm.resolveProfileDirectory(p), "options.txt").isFile()) {
                played = true;
            }
        }
        if (played) prefs.putBoolean(PREF_DONE, true);
        return !played;
    }

    public static void show(LauncherPanel pane) {
        Preferences.userNodeForPackage(App.class).putBoolean(PREF_DONE, true);
        createDialog(pane).show();
    }

    /** Fenêtre de l'assistant, sans l'afficher (rendus hors écran). */
    static MajestyDialog createDialog(LauncherPanel pane) {
        MajestyDialog dialog = new MajestyDialog(pane.getStageOrNull(), "BIENVENUE", "Comment veux-tu jouer ?",
                "Choisis une façon de jouer : le launcher prépare le profil pour toi. Tu pourras tout changer ensuite.");
        VBox choices = new VBox(8);
        choices.getChildren().addAll(
                choice(Icons.COMPASS, "Survie, fluide et jolie",
                        "La dernière version avec Fabric et des mods d'optimisation, réglée pour ton PC.",
                        () -> { dialog.close(); prepare(pane, "Survie", Utils.ModloaderType.FABRIC, null, true); }),
                choice(Icons.TARGET, "PvP",
                        "Minecraft 1.8.9 avec OptiFine, la référence du combat.",
                        () -> { dialog.close(); prepare(pane, "PvP 1.8.9", Utils.ModloaderType.OPTIFINE, "1.8.9", false); }),
                choice(Icons.STAR, "La toute dernière version",
                        "Minecraft tel quel, sans mod.",
                        () -> { dialog.close(); prepare(pane, null, Utils.ModloaderType.VANILLA, null, false); }),
                choice(Icons.CLOCK, "Voyage dans le temps",
                        "Rejoue une époque de Minecraft : Beta, Alpha, Classic...",
                        () -> { dialog.close(); Platform.runLater(pane::openTimeTravelPanel); }),
                choice(Icons.SLIDERS, "Je règle tout moi-même",
                        "Garder le profil par défaut.",
                        dialog::close));
        return dialog.content(choices);
    }

    /**
     * Crée le profil choisi, l'active et applique les réglages conseillés pour ce PC
     * (mémoire, arguments Java, distance de rendu ; mods d'optimisation si withMods).
     */
    private static void prepare(LauncherPanel pane, String name, Utils.ModloaderType loader, String fixedVersion, boolean withMods) {
        new MajestyDialog(pane.getStageOrNull(), "BIENVENUE", "Préparation de ton profil", null)
                .autoWork(progress -> {
                    String version = fixedVersion != null ? fixedVersion : latestVersion(loader, progress);
                    String profileName = name != null ? name : "Minecraft " + version;
                    ProfileManager pm = ProfileManager.get();
                    LauncherProfile profile = pm.create(profileName, version, loader);
                    runOnFx(() -> {
                        pm.activate(profile, pane.getEngine());
                        App.getInstance().applyGameLinksFromConfigAsync();
                        pane.refreshProfilesUI();
                    });
                    String done = PerformanceFlow.applyAllToActive(pane, pane.getEngine(), progress, withMods);
                    return "Ton profil « " + profileName + " » est prêt. " + done;
                })
                .show();
    }

    /**
     * Dernière version publiée ; pour Fabric, la plus récente qui a déjà Sodium (les mods
     * d'optimisation arrivent quelques jours après une nouvelle version de Minecraft).
     */
    private static String latestVersion(Utils.ModloaderType loader, Consumer<String> progress) throws Exception {
        progress.accept("Recherche de la dernière version...");
        com.google.gson.JsonObject manifest = com.google.gson.JsonParser.parseString(App.downloadTextStatic(MANIFEST)).getAsJsonObject();
        String latest = manifest.getAsJsonObject("latest").get("release").getAsString();
        if (loader != Utils.ModloaderType.FABRIC) return latest;
        int checked = 0;
        for (com.google.gson.JsonElement e : manifest.getAsJsonArray("versions")) {
            com.google.gson.JsonObject v = e.getAsJsonObject();
            if (!"release".equals(v.get("type").getAsString())) continue;
            String id = v.get("id").getAsString();
            if (ModrinthApi.bestVersion("sodium", Collections.singletonList("fabric"), id) != null) return id;
            if (++checked >= 8) break;
        }
        return latest;
    }

    private static void runOnFx(Runnable r) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    /** Carte de choix cliquable : icône, titre, description. */
    private static Node choice(String icon, String title, String text, Runnable onClick) {
        Label t = new Label(title);
        t.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        t.setStyle("-fx-text-fill: rgba(255,255,255,0.95); -fx-font-weight: bold;");
        Label d = MajestyDialog.text(text, 11F, "rgba(255,255,255,0.58)");
        VBox texts = new VBox(3, t, d);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Node glyph = Icons.stroke(icon, 20);
        VBox iconBox = new VBox(glyph);
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinSize(40, 40);
        iconBox.setMaxSize(40, 40);
        iconBox.setStyle("-fx-background-color: rgba(255,159,26,0.14); -fx-background-radius: 12;");

        HBox card = new HBox(14, iconBox, texts);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 12));
        String idle = "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 14;"
                + "-fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 14; -fx-border-width: 1;";
        String hover = "-fx-background-color: rgba(255,159,26,0.12); -fx-background-radius: 14;"
                + "-fx-border-color: rgba(255,170,0,0.55); -fx-border-radius: 14; -fx-border-width: 1;";
        card.setStyle(idle);
        card.setCursor(Cursor.HAND);
        card.setOnMouseEntered(e -> card.setStyle(hover));
        card.setOnMouseExited(e -> card.setStyle(idle));
        card.setOnMouseClicked(e -> onClick.run());
        return card;
    }
}
