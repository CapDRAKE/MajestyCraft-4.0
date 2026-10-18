package fr.majestycraft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Fond d'ambiance du launcher, entièrement dessiné en code (aucune image) :
 * dégradé sombre, lueurs aux couleurs du logo qui dérivent lentement, silhouette
 * de terrain en blocs façon Minecraft, braises pixelisées, grain et vignette.
 *
 * Les animations se mettent en pause quand la fenêtre est masquée (pendant que le
 * jeu tourne, notamment) pour ne rien coûter en performances.
 */
public final class AmbientBackground extends Pane {

    private static final Color EMBER = Color.rgb(255, 140, 20);
    private static final Color CRIMSON = Color.rgb(210, 40, 55);
    private static final Color VIOLET = Color.rgb(90, 70, 210);
    private static final Color[] EMBER_COLORS = {
            Color.web("#ffb347"), Color.web("#ff8a00"), Color.web("#ffd27a"), Color.web("#ff6a00")
    };
    private static final int EMBER_COUNT = 24;

    private final boolean embers;
    private final List<Animation> animations = new ArrayList<>();
    private AnimationTimer embersTimer;
    private boolean running = true;
    private double builtWidth = -1;
    private double builtHeight = -1;

    private AmbientBackground(boolean embers) {
        this.embers = embers;
        setMouseTransparent(true);
        setPickOnBounds(false);
        pauseWhileWindowHidden();
    }

    /** Fond de taille fixe, placé sous tout le contenu existant de root. */
    public static AmbientBackground install(Pane root, double width, double height, boolean embers) {
        AmbientBackground background = new AmbientBackground(embers);
        root.getChildren().add(0, background);
        background.build(width, height);
        return background;
    }

    /** Fond qui suit la taille de root (fenêtres redimensionnables). */
    public static AmbientBackground installResponsive(Pane root, boolean embers) {
        AmbientBackground background = new AmbientBackground(embers);
        root.getChildren().add(0, background);
        ChangeListener<Number> resize = (obs, oldValue, newValue) -> {
            double w = root.getWidth() > 0 ? root.getWidth() : root.getPrefWidth();
            double h = root.getHeight() > 0 ? root.getHeight() : root.getPrefHeight();
            background.build(w, h);
        };
        root.widthProperty().addListener(resize);
        root.heightProperty().addListener(resize);
        resize.changed(null, null, null);
        return background;
    }

    /** Arrête définitivement les animations. */
    public void stop() {
        running = false;
        stopAnimations();
    }

    private void build(double width, double height) {
        if (width <= 0 || height <= 0 || (width == builtWidth && height == builtHeight)) {
            return;
        }
        builtWidth = width;
        builtHeight = height;
        stopAnimations();
        getChildren().clear();
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        setClip(new Rectangle(width, height));

        Rectangle base = new Rectangle(width, height);
        base.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0b0f17")),
                new Stop(0.55, Color.web("#0a0d14")),
                new Stop(1, Color.web("#160b10"))));
        getChildren().add(base);

        double size = Math.max(width, height);
        addGlow(width * 0.24, height * 0.32, size * 0.42, EMBER, 0.34, 18, width * 0.05, height * 0.04);
        addGlow(width * 0.88, height * 0.92, size * 0.50, CRIMSON, 0.30, 23, -width * 0.06, -height * 0.05);
        addGlow(width * 0.78, height * 0.08, size * 0.38, VIOLET, 0.20, 27, -width * 0.04, height * 0.05);

        // Deux plans de terrain : le plus lointain plus clair, le plus proche plus sombre.
        addTerrain(width, height, 22, 0.10, 0.21, Color.rgb(26, 20, 30, 0.88), null, 7L);
        addTerrain(width, height, 28, 0.05, 0.13, Color.rgb(7, 9, 14, 0.97), Color.rgb(255, 140, 40, 0.28), 13L);

        if (embers) {
            addEmbers(width, height);
        }

        Rectangle grain = new Rectangle(width, height);
        grain.setFill(new ImagePattern(createGrain(96), 0, 0, 96, 96, false));
        getChildren().add(grain);

        Rectangle vignette = new Rectangle(width, height);
        vignette.setFill(new RadialGradient(0, 0, 0.5, 0.45, 0.85, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.65, Color.rgb(0, 0, 0, 0.10)),
                new Stop(1, Color.rgb(0, 0, 0, 0.55))));
        getChildren().add(vignette);

        if (running) {
            playAnimations();
        }
    }

    private void addGlow(double x, double y, double radius, Color color, double alpha,
                         double seconds, double driftX, double driftY) {
        Circle glow = new Circle(x, y, radius);
        glow.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, withAlpha(color, alpha)),
                new Stop(0.45, withAlpha(color, alpha * 0.45)),
                new Stop(1, Color.TRANSPARENT)));
        getChildren().add(glow);

        TranslateTransition drift = new TranslateTransition(Duration.seconds(seconds), glow);
        drift.setByX(driftX);
        drift.setByY(driftY);
        ScaleTransition breathe = new ScaleTransition(Duration.seconds(seconds * 0.8), glow);
        breathe.setToX(1.08);
        breathe.setToY(1.08);
        for (Animation a : new Animation[] { drift, breathe }) {
            a.setAutoReverse(true);
            a.setCycleCount(Animation.INDEFINITE);
        }
        drift.setInterpolator(Interpolator.EASE_BOTH);
        breathe.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(new ParallelTransition(drift, breathe));
    }

    /**
     * Silhouette de terrain en blocs : une marche aléatoire de hauteurs (graine fixe,
     * le décor est donc toujours le même), dessinée en escalier.
     */
    private void addTerrain(double width, double height, double block, double minRatio, double maxRatio,
                            Color fill, Color rim, long seed) {
        Random random = new Random(seed);
        int minBlocks = Math.max(1, (int) Math.round(height * minRatio / block));
        int maxBlocks = Math.max(minBlocks + 1, (int) Math.round(height * maxRatio / block));
        int current = (minBlocks + maxBlocks) / 2;

        List<Double> points = new ArrayList<>();
        points.add(0.0);
        points.add(height);
        for (double x = 0; x < width + block; x += block) {
            double roll = random.nextDouble();
            if (roll < 0.30) current = Math.max(minBlocks, current - 1);
            else if (roll > 0.70) current = Math.min(maxBlocks, current + 1);
            double y = height - current * block;
            points.add(x);
            points.add(y);
            points.add(x + block);
            points.add(y);
        }
        points.add(width + block);
        points.add(height);

        Polygon terrain = new Polygon();
        terrain.getPoints().addAll(points);
        terrain.setFill(fill);
        getChildren().add(terrain);

        if (rim != null) {
            // Liseré lumineux sur le haut des blocs, comme éclairé par les braises.
            Polyline edge = new Polyline();
            edge.getPoints().addAll(points.subList(2, points.size() - 2));
            edge.setStroke(rim);
            edge.setStrokeWidth(1.5);
            edge.setFill(null);
            getChildren().add(edge);
        }
    }

    private void addEmbers(double width, double height) {
        Random random = new Random();
        Rectangle[] sparks = new Rectangle[EMBER_COUNT];
        double[] speed = new double[EMBER_COUNT];
        double[] phase = new double[EMBER_COUNT];
        double top = height * 0.15;

        for (int i = 0; i < EMBER_COUNT; i++) {
            double s = 2 + random.nextInt(3);
            sparks[i] = new Rectangle(s, s, EMBER_COLORS[random.nextInt(EMBER_COLORS.length)]);
            sparks[i].setX(random.nextDouble() * width);
            sparks[i].setY(top + random.nextDouble() * (height - top));
            speed[i] = 14 + random.nextDouble() * 22;
            phase[i] = random.nextDouble() * Math.PI * 2;
            getChildren().add(sparks[i]);
        }

        embersTimer = new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(long now) {
                if (last < 0) {
                    last = now;
                    return;
                }
                double dt = Math.min(0.1, (now - last) / 1_000_000_000.0);
                last = now;
                for (int i = 0; i < EMBER_COUNT; i++) {
                    Rectangle spark = sparks[i];
                    double y = spark.getY() - speed[i] * dt;
                    phase[i] += dt * 0.9;
                    if (y < top) {
                        // Repart du bas, ailleurs.
                        y = height + random.nextDouble() * 40;
                        spark.setX(random.nextDouble() * width);
                        speed[i] = 14 + random.nextDouble() * 22;
                    }
                    spark.setY(y);
                    spark.setTranslateX(Math.sin(phase[i]) * 10);
                    // Apparaît en bas, s'éteint en montant.
                    double progress = (height - y) / (height - top);
                    double fadeIn = Math.min(1, Math.max(0, progress / 0.12));
                    spark.setOpacity(Math.max(0, fadeIn * (1 - progress)) * 0.85);
                }
            }
        };
    }

    private void playAnimations() {
        for (Animation animation : animations) {
            animation.play();
        }
        if (embersTimer != null) {
            embersTimer.start();
        }
    }

    private void pauseAnimations() {
        for (Animation animation : animations) {
            animation.pause();
        }
        if (embersTimer != null) {
            embersTimer.stop();
        }
    }

    private void stopAnimations() {
        for (Animation animation : animations) {
            animation.stop();
        }
        animations.clear();
        if (embersTimer != null) {
            embersTimer.stop();
            embersTimer = null;
        }
    }

    /** Met les animations en pause tant que la fenêtre n'est pas affichée. */
    private void pauseWhileWindowHidden() {
        ChangeListener<Boolean> showing = (obs, wasShowing, isShowing) -> {
            if (!running) return;
            if (Boolean.TRUE.equals(isShowing)) playAnimations();
            else pauseAnimations();
        };
        sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene == null) return;
            scene.windowProperty().addListener((o, oldWindow, window) -> watch(window, showing));
            watch(scene.getWindow(), showing);
        });
    }

    private static void watch(Window window, ChangeListener<Boolean> showing) {
        if (window != null) {
            window.showingProperty().addListener(showing);
        }
    }

    private static WritableImage createGrain(int size) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter writer = image.getPixelWriter();
        Random random = new Random(42);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                double shade = random.nextBoolean() ? 1 : 0;
                writer.setColor(x, y, new Color(shade, shade, shade, random.nextDouble() * 0.035));
            }
        }
        return image;
    }

    private static Color withAlpha(Color color, double alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(1, alpha)));
    }
}
