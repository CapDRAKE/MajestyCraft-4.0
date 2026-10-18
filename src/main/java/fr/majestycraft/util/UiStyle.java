package fr.majestycraft.util;

import javafx.scene.control.ButtonBase;

/**
 * Styles de boutons communs à tout le launcher : chaque méthode pose une classe CSS
 * ml-* de css/design.css, où sont définis les états survol, appui, focus et
 * désactivé. Elle retire au passage le style en ligne et les effets de survol codés à
 * la main (opacité de LauncherButton, agrandissement), qui masqueraient ces états.
 *
 * La police reste celle donnée par le code (setFont), sauf pour les classes ml-cta et
 * ml-strong qui la fixent en CSS.
 */
public final class UiStyle {

    private static final String[] VARIANTS = {
            "ml-primary", "ml-secondary", "ml-side", "ml-window", "ml-window-close", "ml-tab",
            "ml-tab-active", "ml-chip", "ml-chip-active", "ml-soft", "ml-danger", "ml-cta", "ml-strong"
    };

    private UiStyle() {
    }

    /** Action principale : dégradé orange. */
    public static void primary(ButtonBase button) {
        apply(button, "ml-primary");
    }

    /** Action secondaire : fond translucide et bordure fine. */
    public static void secondary(ButtonBase button) {
        apply(button, "ml-secondary");
    }

    /** Onglet en pilule ; l'onglet actif est orange. */
    public static void tab(ButtonBase button, boolean active) {
        if (active) apply(button, "ml-tab", "ml-tab-active");
        else apply(button, "ml-tab");
    }

    /** Filtre (choix d'un loader...) ; le filtre actif est teinté d'orange. */
    public static void chip(ButtonBase button, boolean active) {
        if (active) apply(button, "ml-chip", "ml-chip-active");
        else apply(button, "ml-chip");
    }

    /** Petite action mise en avant sans être principale : orange doux. */
    public static void soft(ButtonBase button) {
        apply(button, "ml-soft");
    }

    /** Action destructrice (supprimer...) : rouge doux. */
    public static void danger(ButtonBase button) {
        apply(button, "ml-danger");
    }

    /**
     * Remplace la variante de style du bouton par celles indiquées (les variantes ml-*
     * précédentes sont retirées, l'appel peut donc être répété pour changer d'état).
     */
    public static void apply(ButtonBase button, String... styleClasses) {
        if (button == null) return;
        button.setStyle(null);
        button.setEffect(null);
        button.setOnMouseEntered(null);
        button.setOnMouseExited(null);
        button.getStyleClass().removeAll(VARIANTS);
        button.getStyleClass().addAll(styleClasses);
    }
}
