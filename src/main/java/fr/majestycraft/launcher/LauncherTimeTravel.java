package fr.majestycraft.launcher;

import com.jfoenix.controls.JFXButton;
import fr.majestycraft.App;
import fr.majestycraft.Utils;
import fr.majestycraft.profiles.LauncherProfile;
import fr.majestycraft.profiles.ProfileManager;
import fr.majestycraft.util.AmbientBackground;
import fr.majestycraft.util.Icons;
import fr.majestycraft.util.MajestyDialog;
import fr.majestycraft.util.UiStyle;
import fr.trxyy.alternative.alternative_api.GameEngine;
import fr.trxyy.alternative.alternative_api.updater.LinuxArmSupport;
import fr.trxyy.alternative.alternative_api.utils.FontLoader;
import fr.trxyy.alternative.alternative_api_ui.base.IScreen;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherImage;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherLabel;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherRectangle;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

/**
 * « Voyage dans le temps » : l'histoire de Minecraft par époques, de la toute première
 * version publique (2009) à aujourd'hui. Un clic crée (ou réactive) le profil vanilla de
 * l'époque ; chaque profil garde ses propres mondes.
 */
public class LauncherTimeTravel extends IScreen {

    private static final int W = 1000;
    private static final int H = 760;

    /** Une époque : année, nom, version jouée, nom du profil créé, description. */
    static final class Era {
        final String year;
        final String title;
        String version;
        final String profileName;
        final String text;

        Era(String year, String title, String version, String profileName, String text) {
            this.year = year;
            this.title = title;
            this.version = version;
            this.profileName = profileName;
            this.text = text;
        }
    }

    static final List<Era> ERAS = Arrays.asList(
            new Era("2009", "Les tout débuts", "rd-132211", "Pre-Classic",
                    "Le premier Minecraft rendu public, en mai 2009 : un monde d'herbe où l'on pose et casse des blocs. Ni objets, ni créatures."),
            new Era("2009", "Classic", "c0.30_01c", "Classic",
                    "Le Minecraft des débuts, en créatif : une palette de blocs limitée et un monde de taille fixe."),
            new Era("2010", "Infdev", "inf-20100618", "Infdev",
                    "Le premier monde infini : on peut marcher sans jamais en atteindre le bord."),
            new Era("2010", "Alpha", "a1.2.6", "Alpha 1.2.6",
                    "La survie prend forme, et le Nether arrive avec la mise à jour d'Halloween."),
            new Era("2011", "Beta 1.7.3", "b1.7.3", "Beta 1.7.3",
                    "La Beta culte : pistons et lits, mais pas encore de faim ni de villages."),
            new Era("2011", "La sortie officielle", "1.0", "Minecraft 1.0",
                    "Minecraft 1.0 : l'End, l'Ender Dragon, l'alchimie et l'enchantement."),
            new Era("2015", "L'ère du PvP", "1.8.9", "1.8.9",
                    "Le combat sans temps de recharge, toujours la référence de nombreux serveurs PvP."),
            new Era("2017", "L'âge d'or des mods", "1.12.2", "1.12.2",
                    "L'une des versions les plus moddées, base d'innombrables modpacks."),
            new Era("2018", "Update Aquatic", "1.13.2", "1.13.2",
                    "Des océans vivants : dauphins, tortues et tridents."),
            new Era("2020", "Nether Update", "1.16.5", "1.16.5",
                    "Un Nether repensé : nouveaux biomes, piglins et netherite."),
            new Era("2021", "Caves & Cliffs", "1.18.2", "1.18.2",
                    "Des grottes immenses et des montagnes : le monde s'étend désormais de -64 à 320."),
            new Era("2023", "Trails & Tales", "1.20.1", "1.20.1",
                    "Archéologie, cerisiers, chameaux et renifleurs."),
            new Era("Aujourd'hui", "La dernière version", null, null,
                    "La version la plus récente de Minecraft, avec toutes ses nouveautés."));

    private final GameEngine engine;
    private final LauncherPanel panel;

    private double xOffSet;
    private double yOffSet;

    public LauncherTimeTravel(final Pane root, final GameEngine engine, final LauncherPanel panel) {
        this.engine = engine;
        this.panel = panel;

        root.setOnMousePressed(event -> {
            xOffSet = event.getSceneX();
            yOffSet = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            if (root.getScene() == null) return;
            Stage st = (Stage) root.getScene().getWindow();
            st.setX(event.getScreenX() - xOffSet);
            st.setY(event.getScreenY() - yOffSet);
        });

        AmbientBackground.installResponsive(root, false);

        LauncherRectangle leftDock = new LauncherRectangle(root, 0, 0, 84, H);
        leftDock.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(8, 12, 18, 0.84)), new Stop(1, Color.rgb(8, 12, 18, 0.28))));
        LauncherRectangle separator = new LauncherRectangle(root, 83, 0, 1, H);
        separator.setFill(Color.rgb(255, 255, 255, 0.08));

        LauncherImage heroLogo = new LauncherImage(root);
        heroLogo.setImage(getResourceLocation().loadImage(engine, "launchergifpng.png"));
        heroLogo.setSize(150, 150);
        heroLogo.setBounds(128, 90, 150, 150);

        heroLabel(root, "Époques", 28F, "rgba(255,255,255,0.96)", 118, 270, 260, 40);
        heroLabel(root, "Toute l'histoire de Minecraft", 14F, "rgba(255,176,0,0.96)", 120, 314, 280, 24);
        heroLabel(root, "• De 2009 à aujourd'hui", 11F, "rgba(255,255,255,0.70)", 120, 362, 280, 20);
        heroLabel(root, "• Un clic crée le profil", 11F, "rgba(255,255,255,0.58)", 120, 388, 280, 20);
        heroLabel(root, "• Chaque époque garde ses mondes", 11F, "rgba(255,255,255,0.46)", 120, 414, 280, 20);

        final int cardW = 565;
        final int cardH = 660;
        final int cardX = 360;
        final int cardY = 50;

        LauncherRectangle card = new LauncherRectangle(root, cardX, cardY, cardW, cardH);
        card.setArcWidth(34);
        card.setArcHeight(34);
        card.setFill(Color.rgb(8, 12, 18, 0.74));
        card.setStroke(Color.rgb(255, 255, 255, 0.10));
        card.setStrokeWidth(1);
        card.setEffect(new DropShadow(36, Color.rgb(0, 0, 0, 0.72)));
        card.setMouseTransparent(true);

        LauncherLabel title = new LauncherLabel(root);
        title.setText("Voyage dans le temps");
        title.setStyle("-fx-text-fill: white;");
        title.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 26F));
        title.setSize(cardW, 38);
        title.setPosition(cardX, cardY + 24);
        title.setAlignment(Pos.CENTER);

        LauncherLabel subtitle = new LauncherLabel(root);
        subtitle.setText("Chaque époque de Minecraft, jouable en un clic");
        subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.45);");
        subtitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        subtitle.setSize(cardW, 20);
        subtitle.setPosition(cardX, cardY + 58);
        subtitle.setAlignment(Pos.CENTER);

        JFXButton closeButton = new JFXButton("");
        closeButton.setGraphic(Icons.stroke(Icons.CLOSE, 16));
        closeButton.setPrefSize(36, 32);
        UiStyle.apply(closeButton, "ml-window", "ml-window-close");
        closeButton.setLayoutX(cardX + cardW - 50);
        closeButton.setLayoutY(cardY + 18);
        closeButton.setOnAction(event -> ((Stage) closeButton.getScene().getWindow()).close());
        root.getChildren().add(closeButton);

        VBox list = new VBox(10);
        list.setPadding(new Insets(4, 8, 12, 2));
        list.setStyle("-fx-background-color: transparent;");
        boolean arm = LinuxArmSupport.isLinuxArm();
        for (Era era : ERAS) list.getChildren().add(buildRow(era, arm));

        ScrollPane scroll = new ScrollPane(list);
        scroll.setLayoutX(cardX + 34);
        scroll.setLayoutY(cardY + 96);
        scroll.setPrefSize(cardW - 68, cardH - 96 - 24);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-control-inner-background: transparent;");
        root.getChildren().add(scroll);
    }

    private static void heroLabel(Pane root, String text, float size, String color, int x, int y, int w, int h) {
        LauncherLabel l = new LauncherLabel(root);
        l.setText(text);
        l.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", size));
        l.setStyle("-fx-background-color: transparent; -fx-text-fill: " + color);
        l.setPosition(x, y);
        l.setSize(w, h);
    }

    private HBox buildRow(Era era, boolean arm) {
        Label year = new Label(era.year);
        year.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", era.year.length() > 4 ? 11F : 17F));
        year.setStyle("-fx-text-fill: rgba(255,176,0,0.95);");
        year.setMinWidth(72);
        year.setPrefWidth(72);
        year.setWrapText(true);

        Label name = new Label(era.title);
        name.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        name.setStyle("-fx-text-fill: rgba(255,255,255,0.95); -fx-font-weight: bold;");

        Label version = new Label(era.version != null ? era.version : "…");
        version.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
        version.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 999;"
                + "-fx-text-fill: rgba(255,255,255,0.70); -fx-padding: 2 8 2 8;");
        HBox head = new HBox(8, name, version);
        head.setAlignment(Pos.CENTER_LEFT);

        Label text = new Label(era.text);
        text.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        text.setStyle("-fx-text-fill: rgba(255,255,255,0.58);");
        text.setWrapText(true);

        VBox middle = new VBox(5, head, text);
        HBox.setHgrow(middle, Priority.ALWAYS);

        JFXButton play = new JFXButton("Jouer");
        play.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        play.setGraphic(Icons.filled(Icons.PLAY, 11));
        play.setGraphicTextGap(6);
        play.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        UiStyle.primary(play);
        play.setStyle("-fx-padding: 6 14 6 12;");

        boolean unavailable = arm && era.version != null && !LauncherSettings.isArmCompatibleVersion(era.version);
        if (unavailable) {
            play.setDisable(true);
            text.setText(era.text + " Indisponible sur processeur ARM (Minecraft 1.19 et plus uniquement).");
        }
        if (era.version == null) {
            play.setDisable(true);
            loadLatestRelease(era, version, play, arm);
        }
        play.setOnAction(e -> travel(era, (Stage) play.getScene().getWindow()));

        HBox row = new HBox(12, year, middle, play);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 14, 12, 14));
        row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 16;"
                + "-fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 16; -fx-border-width: 1;");
        return row;
    }

    /** Dernière version publiée, lue dans le manifeste Mojang (en arrière-plan). */
    private static void loadLatestRelease(Era era, Label versionTag, JFXButton play, boolean arm) {
        Thread t = new Thread(() -> {
            String latest = null;
            try {
                String json = App.downloadTextStatic("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
                latest = com.google.gson.JsonParser.parseString(json).getAsJsonObject()
                        .getAsJsonObject("latest").get("release").getAsString();
            } catch (Exception ignored) {
            }
            final String found = latest;
            Platform.runLater(() -> {
                if (found == null) {
                    versionTag.setText("hors ligne");
                    return;
                }
                era.version = found;
                versionTag.setText(found);
                play.setDisable(arm && !LauncherSettings.isArmCompatibleVersion(found));
            });
        }, "MajestyLauncher-LatestRelease");
        t.setDaemon(true);
        t.start();
    }

    /** Active le profil vanilla de l'époque (créé s'il n'existe pas encore). */
    private void travel(Era era, Stage stage) {
        ProfileManager pm = ProfileManager.get();
        if (pm == null || era.version == null) return;
        LauncherProfile target = null;
        for (LauncherProfile p : pm.getProfiles()) {
            if (era.version.equals(p.getVersion()) && p.getModloaderType() == Utils.ModloaderType.VANILLA) {
                target = p;
                break;
            }
        }
        String name = era.profileName != null ? era.profileName : "Minecraft " + era.version;
        if (target == null) target = pm.create(name, era.version, Utils.ModloaderType.VANILLA);
        pm.activate(target, engine);
        App.getInstance().applyGameLinksFromConfigAsync();
        if (panel != null) panel.refreshProfilesUI();
        stage.close();

        boolean noMenu = era.version.startsWith("rd-") || era.version.startsWith("c0.");
        new MajestyDialog(panel != null ? panel.getStageOrNull() : null, "VOYAGE DANS LE TEMPS",
                "Destination : " + (era.year.equals("Aujourd'hui") ? "aujourd'hui" : era.year),
                "Le profil « " + target.getName() + " » (" + era.version + ") est actif : clique sur JOUER."
                        + (noMenu ? "\nCette version n'a pas de menu : elle démarre directement dans un monde." : "")
                        + "\nSes mondes restent rangés dans ce profil.")
                .action("C'est parti", "primary", null)
                .show();
    }
}
