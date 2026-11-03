package fr.majestycraft.launcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import fr.majestycraft.Utils;
import fr.majestycraft.util.Icons;
import fr.trxyy.alternative.alternative_api.utils.FontLoader;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Écran de chargement du jeu. Le skin du joueur flotte sous son pseudo, comme en jeu ;
 * la progression est une rangée de blocs qui se posent un à un, de la terre jusqu'au
 * minerai de diamant ; l'étape en cours est dite en clair et des astuces défilent.
 */
public class LoadingView extends Pane {

    static final int W = 760;
    static final int H = 340;

    /* Barre de blocs : face avant S x S (texture 8 x 8), profondeur D, un bloc tous les STEP pixels. */
    private static final int BLOCKS = 14;
    private static final int S = 24;
    private static final int D = 7;
    private static final int STEP = 32;
    private static final int BAR_W = BLOCKS * STEP;
    private static final int BAR_H = 84;
    private static final int BLOCK_Y = 44;

    private static final int RIGHT_X = 272;
    private static final int RIGHT_W = W - RIGHT_X - 36;

    private static final Color AMBER = Color.web("#ffb000");

    private static final String[] TIPS = {
            "Le Voyage dans le temps te fait rejouer la Beta 1.7.3, ou même la toute première version de 2009.",
            "L'éclair à côté de ton profil règle la mémoire, Java et les mods selon ton PC.",
            "Jouer avec un ami : ouvre ton monde solo à un ami par Internet, sans serveur.",
            "Si le jeu plante, le launcher t'explique la cause et propose une correction en un clic.",
            "Chaque profil a ses propres mods : garde un profil Vanilla à côté de ton profil moddé.",
            "F3 + T recharge les packs de ressources sans quitter la partie.",
            "F5 change la vue : première personne, derrière ou devant ton personnage.",
            "F2 prend une capture d'écran, rangée dans le dossier screenshots du jeu.",
    };

    private final Label eyebrow = new Label("PRÉPARATION DU JEU");
    private final Label title = new Label("Minecraft");
    private final Label subtitle = new Label("");
    private final Label percentNumber = new Label("0");
    private final Label layerLabel = new Label("");
    private final Label stepLabel = new Label("Préparation du jeu");
    private final Label detailLabel = new Label("");
    private final Label tipLabel = new Label("");
    private final Label nameTagText = new Label("Steve");
    private final StackPane nameTag;
    private final ImageView skin = new ImageView();
    private final Ellipse shadow = new Ellipse(40, 8);
    private final Canvas bar = new Canvas(BAR_W + D + 8, BAR_H);

    private final AnimationTimer timer;
    private final Timeline tips;
    /** Animations en boucle, mises en pause pendant que le jeu tourne (fenêtre masquée). */
    private final List<Animation> loops = new ArrayList<>();
    private boolean active;
    private int tipIndex;

    /* État de la barre */
    private double target;
    private double shown;
    private boolean busy;
    private boolean complete;
    private int landed;
    private final double[] landTime = new double[BLOCKS];
    private final List<double[]> particles = new ArrayList<>();
    private final Random random = new Random(7);
    private double now;
    private long lastNanos;

    public LoadingView() {
        setPrefSize(W, H);
        setPickOnBounds(false);

        Rectangle glow = new Rectangle(-70, -70, W + 140, H + 140);
        glow.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 138, 0, 0.14)),
                new Stop(0.6, Color.rgb(255, 120, 0, 0.05)),
                new Stop(1, Color.TRANSPARENT)));
        glow.setMouseTransparent(true);

        Rectangle card = new Rectangle(W, H);
        card.setArcWidth(34);
        card.setArcHeight(34);
        card.setFill(Color.rgb(8, 12, 18, 0.82));
        card.setStroke(Color.rgb(255, 255, 255, 0.10));
        card.setEffect(new DropShadow(35, Color.rgb(0, 0, 0, 0.68)));

        /* ── Joueur ── */
        Rectangle halo = new Rectangle(10, 30, 220, 280);
        halo.setFill(new RadialGradient(0, 0, 0.5, 0.52, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 160, 40, 0.26)),
                new Stop(1, Color.TRANSPARENT)));

        shadow.setCenterX(120);
        shadow.setCenterY(292);
        shadow.setFill(Color.rgb(0, 0, 0, 0.5));

        skin.setFitWidth(100);
        skin.setFitHeight(200);
        skin.setPreserveRatio(true);
        skin.setSmooth(false);
        skin.setLayoutX(70);
        skin.setLayoutY(78);

        nameTagText.setFont(font(12));
        nameTagText.setTextFill(Color.WHITE);
        nameTag = new StackPane(nameTagText);
        nameTag.setStyle("-fx-background-color: rgba(0,0,0,0.42); -fx-background-radius: 4; -fx-padding: 3 9 3 9;");
        nameTag.setLayoutY(42);
        nameTag.layoutXProperty().bind(nameTag.widthProperty().multiply(-0.5).add(120));

        Rectangle divider = new Rectangle(244, 40, 1, H - 80);
        divider.setFill(Color.rgb(255, 255, 255, 0.07));

        /* ── Textes et barre ── */
        eyebrow.setFont(font(11));
        eyebrow.setTextFill(Color.rgb(255, 176, 0, 0.88));
        eyebrow.setLayoutX(RIGHT_X);
        eyebrow.setLayoutY(38);

        title.setFont(font(24));
        title.setTextFill(Color.WHITE);
        title.setLayoutX(RIGHT_X);
        title.setLayoutY(58);
        title.setMaxWidth(RIGHT_W - 120);
        title.setTextOverrun(OverrunStyle.ELLIPSIS);

        subtitle.setFont(font(12));
        subtitle.setTextFill(Color.rgb(255, 255, 255, 0.55));
        subtitle.setLayoutX(RIGHT_X);
        subtitle.setLayoutY(94);
        subtitle.setMaxWidth(RIGHT_W - 120);
        subtitle.setTextOverrun(OverrunStyle.ELLIPSIS);

        percentNumber.setFont(font(46));
        percentNumber.setTextFill(AMBER);
        percentNumber.setEffect(new DropShadow(18, Color.rgb(255, 140, 0, 0.45)));
        Label percentSign = new Label("%");
        percentSign.setFont(font(18));
        percentSign.setTextFill(Color.rgb(255, 176, 0, 0.75));
        HBox percent = new HBox(3, percentNumber, percentSign);
        percent.setAlignment(Pos.BASELINE_RIGHT);
        percent.setLayoutY(36);
        percent.layoutXProperty().bind(percent.widthProperty().multiply(-1).add(RIGHT_X + RIGHT_W));

        bar.setLayoutX(RIGHT_X - 2);
        bar.setLayoutY(116);

        // Plus on avance, plus on creuse : la couche atteinte, sous la rangée
        layerLabel.setFont(font(10.5));
        layerLabel.setTextFill(Color.rgb(255, 255, 255, 0.40));
        layerLabel.setLayoutY(116 + BLOCK_Y + S + 10);
        layerLabel.layoutXProperty().bind(layerLabel.widthProperty().multiply(-1).add(RIGHT_X + RIGHT_W));

        stepLabel.setFont(font(15));
        stepLabel.setTextFill(Color.rgb(255, 255, 255, 0.92));
        stepLabel.setLayoutX(RIGHT_X);
        stepLabel.setLayoutY(214);
        stepLabel.setMaxWidth(RIGHT_W);
        stepLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        detailLabel.setFont(font(11.5));
        detailLabel.setTextFill(Color.rgb(255, 255, 255, 0.48));
        detailLabel.setLayoutX(RIGHT_X);
        detailLabel.setLayoutY(240);
        detailLabel.setMaxWidth(RIGHT_W);
        detailLabel.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);

        Rectangle tipLine = new Rectangle(RIGHT_X, 272, RIGHT_W, 1);
        tipLine.setFill(Color.rgb(255, 255, 255, 0.07));
        tipLabel.setGraphic(Icons.stroke(Icons.STAR, 13));
        tipLabel.setGraphicTextGap(9);
        tipLabel.setFont(font(11.5));
        tipLabel.setTextFill(Color.rgb(255, 255, 255, 0.62));
        tipLabel.setWrapText(true);
        tipLabel.setLayoutX(RIGHT_X);
        tipLabel.setLayoutY(284);
        tipLabel.setPrefWidth(RIGHT_W);
        tipLabel.setMaxWidth(RIGHT_W);

        getChildren().addAll(glow, card, halo, shadow, skin, nameTag, divider,
                eyebrow, title, subtitle, percent, bar, layerLabel, stepLabel, detailLabel, tipLine, tipLabel);

        timer = new AnimationTimer() {
            @Override
            public void handle(long nanos) {
                double dt = lastNanos == 0 ? 0 : Math.min(0.1, (nanos - lastNanos) / 1e9);
                lastNanos = nanos;
                tick(dt);
            }
        };
        tips = new Timeline(new KeyFrame(Duration.seconds(6), e -> nextTip()));
        tips.setCycleCount(Timeline.INDEFINITE);
        setVisible(false);

        // Pendant la partie, la fenêtre du launcher est masquée : rien ne doit tourner
        ChangeListener<Boolean> showing = (obs, was, isShowing) -> {
            if (!active) return;
            if (Boolean.TRUE.equals(isShowing)) resume();
            else suspend();
        };
        sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene == null) return;
            scene.windowProperty().addListener((o, oldWindow, window) -> {
                if (window != null) window.showingProperty().addListener(showing);
            });
            if (scene.getWindow() != null) scene.getWindow().showingProperty().addListener(showing);
        });
    }

    private static javafx.scene.text.Font font(double size) {
        return FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", (float) size);
    }

    /* ─────────────────────────── Contenu ─────────────────────────── */

    /** Pseudo affiché au-dessus du skin ; skinName est le compte dont on montre le skin (null : Steve). */
    public void setPlayer(String name, String skinName) {
        nameTagText.setText(name == null || name.trim().isEmpty() ? "Steve" : name.trim());
        String who = skinName == null || skinName.trim().isEmpty() ? "MHF_Steve" : skinName.trim();
        skin.setImage(new Image("https://minotar.net/body/" + who + "/100.png", true));
    }

    /** Version, loader et nom du profil lancé. */
    public void setGame(String version, Utils.ModloaderType loader, String profileName) {
        title.setText("Minecraft " + (version == null ? "" : version));
        String loaderName = loaderName(loader);
        subtitle.setText(profileName == null || profileName.trim().isEmpty() ? loaderName
                : loaderName + "  ·  profil « " + profileName.trim() + " »");
    }

    static String loaderName(Utils.ModloaderType loader) {
        if (loader == null) return "Vanilla";
        switch (loader) {
            case FORGE: return "Forge";
            case FABRIC: return "Fabric";
            case QUILT: return "Quilt";
            case NEOFORGE: return "NeoForge";
            case OPTIFINE: return "OptiFine";
            default: return "Vanilla";
        }
    }

    /**
     * Nouvel état du téléchargement (appelé dix fois par seconde).
     * percent : de 0 à 100 ; info : message de la lib ; file : fichier en cours.
     */
    public void update(double percent, String info, String file, int done, int total) {
        target = Math.max(0, Math.min(100, percent));
        if (target + 1 < shown) {
            // Nouvelle phase repartie de zéro : la rangée se reconstruit sans animation
            shown = target;
            landed = (int) Math.floor(shown / 100.0 * BLOCKS + 1e-6);
        }
        String step = describeStep(info);
        boolean launching = info != null && info.toLowerCase(Locale.ROOT).contains("accompli");
        complete = launching;
        busy = total <= 0 && !launching;
        stepLabel.setText(step);
        eyebrow.setText(launching ? "C'EST PARTI" : "PRÉPARATION DU JEU");

        StringBuilder detail = new StringBuilder();
        if (total > 0 && !launching) {
            detail.append(group(Math.min(done, total))).append(" sur ").append(group(total)).append(" fichiers");
        }
        if (file != null && !file.trim().isEmpty() && !file.matches("[0-9a-fA-F]{40}") && !launching) {
            if (detail.length() > 0) detail.append("  ·  ");
            detail.append(file.trim());
        }
        if (launching) detail.append("La fenêtre du jeu va s'ouvrir dans un instant.");
        detailLabel.setText(detail.toString());
    }

    private static String group(int n) {
        return String.format(Locale.US, "%,d", n).replace(',', ' ');
    }

    /** Les messages de la lib (sans accents, parfois techniques) en français clair. */
    static String describeStep(String info) {
        if (info == null || info.trim().isEmpty()) return "Préparation du jeu";
        String i = info.toLowerCase(Locale.ROOT);
        if (i.contains("echec") || i.contains("impossible") || i.contains("rosetta") || i.contains("processeur arm"))
            return info.trim();
        if (i.contains("accompli")) return "Lancement de Minecraft...";
        if (i.contains("java")) return "Téléchargement de Java";
        if (i.contains("neoforge")) return "Installation de NeoForge";
        if (i.contains("indexion")) return "Lecture de la version";
        if (i.contains("perso")) return "Fichiers du serveur MajestyCraft";
        if (i.contains("copie")) return "Installation des sons et textures";
        if (i.contains("ressource")) return "Téléchargement des sons et textures";
        if (i.contains("forge")) return "Préparation de Forge";
        if (i.contains("librair")) return "Téléchargement des bibliothèques";
        if (i.contains("verification")) return "Vérification de l'installation";
        return "Préparation du jeu";
    }

    /* ─────────────────────────── Apparition ─────────────────────────── */

    public void start() {
        halt();
        target = shown = 0;
        landed = 0;
        complete = false;
        busy = true;
        particles.clear();
        lastNanos = 0;
        percentNumber.setText("0");
        tipIndex = new Random().nextInt(TIPS.length);
        tipLabel.setText(TIPS[tipIndex]);
        setVisible(true);

        setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(320), this);
        fade.setToValue(1);
        TranslateTransition rise = new TranslateTransition(Duration.millis(320), this);
        rise.setFromY(18);
        rise.setToY(0);
        rise.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, rise).play();

        // Le joueur flotte doucement ; son ombre rétrécit quand il monte
        TranslateTransition bob = new TranslateTransition(Duration.millis(1300), skin);
        bob.setFromY(4);
        bob.setToY(-6);
        bob.setAutoReverse(true);
        bob.setCycleCount(TranslateTransition.INDEFINITE);
        bob.setInterpolator(Interpolator.EASE_BOTH);
        TranslateTransition tagBob = new TranslateTransition(Duration.millis(1300), nameTag);
        tagBob.setFromY(4);
        tagBob.setToY(-6);
        tagBob.setAutoReverse(true);
        tagBob.setCycleCount(TranslateTransition.INDEFINITE);
        tagBob.setInterpolator(Interpolator.EASE_BOTH);
        javafx.animation.ScaleTransition squeeze = new javafx.animation.ScaleTransition(Duration.millis(1300), shadow);
        squeeze.setFromX(1.0);
        squeeze.setToX(0.78);
        squeeze.setFromY(1.0);
        squeeze.setToY(0.78);
        squeeze.setAutoReverse(true);
        squeeze.setCycleCount(javafx.animation.ScaleTransition.INDEFINITE);
        squeeze.setInterpolator(Interpolator.EASE_BOTH);
        loops.add(bob);
        loops.add(tagBob);
        loops.add(squeeze);
        loops.add(tips);
        tips.jumpTo(Duration.ZERO);

        active = true;
        resume();
    }

    public void stop() {
        halt();
        setVisible(false);
    }

    private void resume() {
        lastNanos = 0;
        for (Animation a : loops) a.play();
        timer.start();
    }

    private void suspend() {
        timer.stop();
        for (Animation a : loops) a.pause();
    }

    private void halt() {
        active = false;
        timer.stop();
        for (Animation a : loops) a.stop();
        loops.clear();
    }

    private void nextTip() {
        FadeTransition out = new FadeTransition(Duration.millis(260), tipLabel);
        out.setToValue(0);
        out.setOnFinished(e -> {
            tipIndex = (tipIndex + 1) % TIPS.length;
            tipLabel.setText(TIPS[tipIndex]);
            FadeTransition in = new FadeTransition(Duration.millis(260), tipLabel);
            in.setToValue(1);
            in.play();
        });
        out.play();
    }

    /* ─────────────────────────── Barre de blocs ─────────────────────────── */

    private void tick(double dt) {
        now += dt;
        // Rattrapage en douceur, avec une vitesse minimale pour finir la rangée
        double diff = target - shown;
        double move = diff * Math.min(1, dt * 4.5);
        double minMove = 18 * dt;
        if (Math.abs(move) < minMove) move = Math.signum(diff) * Math.min(Math.abs(diff), minMove);
        shown += move;
        percentNumber.setText(String.valueOf((int) Math.floor(shown + 1e-6)));

        int shouldLand = (int) Math.floor(shown / 100.0 * BLOCKS + 1e-6);
        while (landed < Math.min(BLOCKS, shouldLand)) {
            landTime[landed] = now;
            burst(landed);
            landed++;
        }
        for (int i = particles.size() - 1; i >= 0; i--) {
            double[] p = particles.get(i);
            p[4] += dt;
            if (p[4] >= p[5]) {
                particles.remove(i);
                continue;
            }
            p[0] += p[2] * dt;
            p[1] += p[3] * dt;
            p[3] += 420 * dt;
        }
        layerLabel.setText(landed >= BLOCKS ? "Diamants atteints !"
                : "Couche : " + LAYER_NAMES[TIER[Math.min(landed, BLOCKS - 1)]]);
        draw();
    }

    /** Éclats de poussière quand un bloc se pose. */
    private void burst(int i) {
        // Couleur du minerai, ou du dessus du bloc quand elle serait invisible sur le fond sombre
        Color[] m = material(i);
        Color c = m[3] != null && m[3].getBrightness() > 0.4 ? m[3] : m[1];
        double x = blockX(i);
        for (int k = 0; k < 7; k++) {
            double side = k % 2 == 0 ? -1 : 1;
            particles.add(new double[]{
                    x + (side < 0 ? 2 : S - 2), BLOCK_Y + S - 2,
                    side * (25 + random.nextDouble() * 55), -(60 + random.nextDouble() * 90),
                    0, 0.45 + random.nextDouble() * 0.25,
                    c.getRed(), c.getGreen(), c.getBlue()});
        }
    }

    private static double blockX(int i) {
        return 2 + i * STEP;
    }

    private void draw() {
        GraphicsContext g = bar.getGraphicsContext2D();
        g.clearRect(0, 0, bar.getWidth(), bar.getHeight());

        // Sol : une fine ligne sous la rangée
        g.setFill(Color.rgb(255, 255, 255, 0.06));
        g.fillRect(0, BLOCK_Y + S + 6, BAR_W + D, 1);

        int frontier = landed;
        double frac = shown / 100.0 * BLOCKS - landed;
        for (int i = 0; i < BLOCKS; i++) {
            double x = blockX(i);
            if (i < landed) {
                double e = now - landTime[i];
                double drop = 0;
                if (e < 0.3) {
                    double t = e / 0.3;
                    drop = -26 * Math.pow(1 - t, 2);
                }
                drawCube(g, x, BLOCK_Y + drop, material(i), 1.0, i);
                if (complete) shine(g, x, BLOCK_Y, i);
            } else if (i == frontier && !complete) {
                double pulse = 0.5 + 0.5 * Math.sin(now * (busy ? 5 : 3.2));
                drawCube(g, x, BLOCK_Y, material(i), Math.max(0, Math.min(1, frac)) * 0.55, i);
                drawGhost(g, x, BLOCK_Y, Color.rgb(255, 176, 0, 0.35 + 0.45 * pulse));
            } else {
                drawGhost(g, x, BLOCK_Y, Color.rgb(255, 255, 255, 0.10));
            }
        }

        for (double[] p : particles) {
            double alpha = 1 - p[4] / p[5];
            g.setFill(Color.color(p[6], p[7], p[8], Math.max(0, alpha)));
            g.fillRect(p[0], p[1], 4, 4);
        }
    }

    /** Reflet qui balaie la rangée terminée, en attendant la fenêtre du jeu. */
    private void shine(GraphicsContext g, double x, double y, int i) {
        double sweep = (now * 260) % (BAR_W + 240) - 120;
        double d = Math.abs(x + S / 2.0 - sweep);
        double a = Math.max(0, 1 - d / 46) * 0.38;
        if (a <= 0) return;
        g.setFill(Color.rgb(255, 255, 255, a));
        g.fillRect(x, y, S, S);
    }

    private static void drawGhost(GraphicsContext g, double x, double y, Color stroke) {
        g.setStroke(stroke);
        g.setLineWidth(1);
        g.setLineDashes(3, 3);
        g.strokeRect(x + 0.5, y + 0.5, S - 1, S - 1);
        g.setLineDashes((double[]) null);
        g.setStroke(stroke.deriveColor(0, 1, 1, 0.6));
        g.strokePolyline(new double[]{x + 0.5, x + D + 0.5, x + S + D - 0.5, x + S + D - 0.5, x + S - 0.5},
                new double[]{y + 0.5, y - D + 0.5, y - D + 0.5, y + S - D - 0.5, y + S - 0.5}, 5);
    }

    /**
     * Bloc en fausse 3D : face avant en texture 8 x 8, dessus plus clair, côté plus sombre.
     * m = {avant, dessus, côté, minerai (ou null)}. La texture est retournée selon le bloc
     * pour que deux blocs voisins ne soient pas identiques.
     */
    private static void drawCube(GraphicsContext g, double x, double y, Color[] m, double alpha, int seed) {
        if (alpha <= 0.01) return;
        g.setGlobalAlpha(alpha);
        g.setFill(m[1]);
        g.fillPolygon(new double[]{x, x + D, x + S + D, x + S}, new double[]{y, y - D, y - D, y}, 4);
        g.setFill(m[2]);
        g.fillPolygon(new double[]{x + S, x + S + D, x + S + D, x + S}, new double[]{y, y - D, y + S - D, y + S}, 4);

        boolean grass = m[1] == GRASS;
        String[] base = grass ? GRASS_FRONT : STONE_FRONT;
        boolean flipX = seed % 2 == 1;
        boolean flipY = !grass && seed % 3 == 1;
        double px = S / 8.0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int rr = flipY ? 7 - r : r;
                int cc = flipX ? 7 - c : c;
                char t = base[rr].charAt(cc);
                if (m[3] != null && ORE_SPOTS[rr].charAt(cc) != '.') t = ORE_SPOTS[rr].charAt(cc);
                g.setFill(texel(t, m));
                g.fillRect(x + c * px, y + r * px, px + 0.2, px + 0.2);
            }
        }
        g.setStroke(Color.rgb(0, 0, 0, 0.32));
        g.setLineWidth(1);
        g.strokeRect(x + 0.5, y + 0.5, S - 1, S - 1);
        g.setGlobalAlpha(1);
    }

    private static Color texel(char t, Color[] m) {
        switch (t) {
            case 'g': return GRASS;
            case 'G': return GRASS_DARK;
            case 'b': return DIRT;
            case 'B': return DIRT_DARK;
            case 'L': return DIRT_LIGHT;
            case 'd': return STONE_DARK;
            case 'l': return STONE_LIGHT;
            case 'o': return m[3];
            case 'O': return m[3].brighter();
            default: return STONE;
        }
    }

    /* Textures 8 x 8, dans l'esprit de celles du jeu. */
    private static final String[] GRASS_FRONT = {
            "gGgggGgg",
            "gbgGgbgG",
            "bbgbbbgb",
            "bBbbLbbb",
            "bbbBbbBb",
            "Lbbbbbbb",
            "bbBbbLbb",
            "bbbbBbbb"};
    private static final String[] STONE_FRONT = {
            "ssdsssls",
            "slsssdss",
            "sssdssss",
            "dssslsds",
            "ssslssss",
            "sdssssls",
            "ssssdsss",
            "lssdssss"};
    private static final String[] ORE_SPOTS = {
            "........",
            ".oO.....",
            ".oo...o.",
            ".....oO.",
            "..o.....",
            ".oO..o..",
            "......o.",
            "........"};

    /* Matières, de la surface vers les profondeurs : herbe, pierre, charbon, fer, or, diamant. */
    private static final Color GRASS = Color.web("#6cbb3c");
    private static final Color GRASS_DARK = Color.web("#559a2e");
    private static final Color DIRT = Color.web("#866043");
    private static final Color DIRT_DARK = Color.web("#6b4a33");
    private static final Color DIRT_LIGHT = Color.web("#9c7352");
    private static final Color DIRT_SIDE = Color.web("#5e4330");
    private static final Color STONE = Color.web("#7f7f7f");
    private static final Color STONE_DARK = Color.web("#6b6b6b");
    private static final Color STONE_LIGHT = Color.web("#939393");
    private static final Color STONE_TOP = Color.web("#9b9b9b");
    private static final Color STONE_SIDE = Color.web("#5d5d5d");
    private static final int[] TIER = {0, 0, 1, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5};
    private static final String[] LAYER_NAMES = {"herbe", "pierre", "minerai de charbon", "minerai de fer",
            "minerai d'or", "minerai de diamant"};
    private static final Color[] ORE = {null, null, Color.web("#2a2a2a"), Color.web("#d8af93"),
            Color.web("#fcee4b"), Color.web("#5decf5")};

    private static Color[] material(int i) {
        int tier = TIER[Math.max(0, Math.min(BLOCKS - 1, i))];
        if (tier == 0) return new Color[]{DIRT, GRASS, DIRT_SIDE, null};
        if (tier == 1) return new Color[]{STONE, STONE_TOP, STONE_SIDE, null};
        return new Color[]{STONE, STONE_TOP, STONE_SIDE, ORE[tier]};
    }
}
