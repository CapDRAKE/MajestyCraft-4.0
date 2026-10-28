package fr.majestycraft.util;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Icônes vectorielles du launcher : tracés au trait sur une grille 24 x 24, dans le
 * style du jeu d'icônes Feather (licence MIT). Contrairement aux PNG, elles restent
 * nettes à toutes les tailles et leur couleur se règle en CSS : chaque tracé porte la
 * classe "ml-icon" (propriété -fx-stroke), ce qui permet par exemple de les éclaircir
 * au survol d'un bouton.
 */
public final class Icons {

    public static final String INFO = "M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0z M12 16v-4 M12 8h.01";
    public static final String SETTINGS = "M15 12a3 3 0 1 1-6 0a3 3 0 1 1 6 0z "
            + "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06"
            + "a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09"
            + "A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83"
            + "l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09"
            + "A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0"
            + "l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09"
            + "a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83"
            + "l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09"
            + "a1.65 1.65 0 0 0-1.51 1z";
    /** Profils de jeu : des calques empilés. */
    public static final String LAYERS = "M12 2L2 7l10 5 10-5-10-5z M2 17l10 5 10-5 M2 12l10 5 10-5";
    /** Packs de ressources : une image. */
    public static final String IMAGE = "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z "
            + "M10 8.5a1.5 1.5 0 1 1-3 0a1.5 1.5 0 1 1 3 0z M21 15l-5-5L5 21";
    /** Mods : un bloc. */
    public static final String BOX = "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8"
            + "a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z M3.27 6.96L12 12.01l8.73-5.05 M12 22.08V12";
    /** Shaders : la lumière. */
    public static final String SUN = "M17 12a5 5 0 1 1-10 0a5 5 0 1 1 10 0z M12 1v2 M12 21v2 M4.22 4.22l1.42 1.42 "
            + "M18.36 18.36l1.42 1.42 M1 12h2 M21 12h2 M4.22 19.78l1.42-1.42 M18.36 5.64l1.42-1.42";
    public static final String GLOBE = "M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0z M2 12h20 "
            + "M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z";
    public static final String MESSAGE = "M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9"
            + "L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5"
            + "a8.48 8.48 0 0 1 8 8v.5z";
    public static final String PLAY = "M7 4.5v15l12.5-7.5z";
    public static final String MINUS = "M5 12h14";
    public static final String CLOSE = "M18 6L6 18 M6 6l12 12";
    public static final String CHECK = "M20 6L9 17l-5-5";
    public static final String FOLDER = "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z";
    public static final String DOWNLOAD = "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 M7 10l5 5 5-5 M12 15V3";
    public static final String TRASH = "M3 6h18 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2";
    /** Ouvrir une page web. */
    public static final String EXTERNAL = "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6 M15 3h6v6 M10 14L21 3";
    public static final String SEARCH = "M19 11a8 8 0 1 1-16 0a8 8 0 1 1 16 0z M21 21l-4.35-4.35";
    public static final String PLUS = "M12 5v14 M5 12h14";
    public static final String REFRESH = "M23 4v6h-6 M1 20v-6h6 M3.51 9a9 9 0 0 1 14.85-3.36L23 10 M1 14l4.64 4.36A9 9 0 0 0 20.49 15";
    /** Mode performance : un éclair. */
    public static final String ZAP = "M13 2L3 14h9l-1 8 10-12h-9l1-8z";
    /** Activer / désactiver. */
    public static final String POWER = "M18.36 6.64a9 9 0 1 1-12.73 0 M12 2v10";

    private Icons() {
    }

    /** Icône au trait (couleur réglable en CSS via la classe "ml-icon"). */
    public static Node stroke(String path, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFill(Color.TRANSPARENT);
        svg.setStroke(Color.rgb(255, 255, 255, 0.85));
        svg.setStrokeWidth(2);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        svg.getStyleClass().add("ml-icon");
        return scaled(svg, size);
    }

    /** Icône pleine (couleur réglable en CSS via la classe "ml-icon-fill"). */
    public static Node filled(String path, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFill(Color.WHITE);
        svg.setStroke(Color.WHITE);
        svg.setStrokeWidth(1.5);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        svg.getStyleClass().add("ml-icon-fill");
        return scaled(svg, size);
    }

    /** Logo Microsoft (quatre carrés aux couleurs de la marque), net à toute taille. */
    public static Node microsoftLogo(double size) {
        double cell = size * 0.47;
        double gap = size - cell * 2;
        Group logo = new Group(
                square(0, 0, cell, "#f25022"),
                square(cell + gap, 0, cell, "#7fba00"),
                square(0, cell + gap, cell, "#00a4ef"),
                square(cell + gap, cell + gap, cell, "#ffb900"));
        logo.getStyleClass().add("ml-brand");
        return logo;
    }

    private static Rectangle square(double x, double y, double size, String color) {
        Rectangle square = new Rectangle(x, y, size, size);
        square.setFill(Color.web(color));
        return square;
    }

    /**
     * Ramène un tracé de la grille 24 x 24 à la taille voulue. Un carré transparent de
     * 24 x 24 fixe l'emprise : toutes les icônes ont ainsi la même taille et se centrent
     * de la même façon, même un simple trait (MINUS).
     */
    private static Node scaled(SVGPath svg, double size) {
        double factor = size / 24.0;
        Rectangle box = new Rectangle(24, 24, Color.TRANSPARENT);
        Group grid = new Group(box, svg);
        grid.setScaleX(factor);
        grid.setScaleY(factor);
        Group icon = new Group(grid);
        icon.setMouseTransparent(true);
        return icon;
    }
}
