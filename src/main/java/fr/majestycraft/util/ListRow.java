package fr.majestycraft.util;

import com.jfoenix.controls.JFXButton;

import fr.trxyy.alternative.alternative_api.utils.FontLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.Locale;

/**
 * Carte d'une ligne de liste (mods, shaders, packs) : icône arrondie, titre, sous-titre
 * (auteur, état...), description sur deux lignes au plus, étiquettes, puis les actions
 * alignées à droite. Une seule instance par cellule : updateItem ne fait que changer
 * les textes. Apparence dans css/design.css (classes ml-row*, ml-tag*).
 */
public class ListRow extends HBox {

    private final ImageView icon = new ImageView();
    private final StackPane iconBox = new StackPane(icon);
    private final Label title = new Label();
    private final Label subtitle = new Label();
    private final Label description = new Label();
    private final HBox tags = new HBox(6);
    private final HBox actions = new HBox(8);

    /**
     * @param list     la liste qui affiche la carte : la carte suit sa largeur (pas de
     *                 défilement horizontal)
     * @param iconSize taille de l'icône, ou 0 pour une carte sans icône
     */
    public ListRow(ListView<?> list, double iconSize) {
        super(14);
        getStyleClass().add("ml-row");
        setAlignment(Pos.CENTER_LEFT);

        if (iconSize > 0) {
            icon.setFitWidth(iconSize);
            icon.setFitHeight(iconSize);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            Rectangle clip = new Rectangle(iconSize, iconSize);
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            icon.setClip(clip);
            iconBox.getStyleClass().add("ml-row-icon");
            iconBox.setMinSize(iconSize, iconSize);
            iconBox.setMaxSize(iconSize, iconSize);
            getChildren().add(iconBox);
        }

        title.getStyleClass().add("ml-row-title");
        title.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        subtitle.getStyleClass().add("ml-row-subtitle");
        subtitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        description.getStyleClass().add("ml-row-desc");
        description.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        description.setWrapText(true);
        // Deux lignes au plus : le reste est remplacé par « ... ».
        description.setMaxHeight(30);
        tags.setAlignment(Pos.CENTER_LEFT);
        // Sous-titre et description n'occupent de place que s'ils ont un texte.
        show(subtitle, null);
        show(description, null);

        VBox text = new VBox(3, title, subtitle, description, tags);
        text.setMinWidth(0);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);
        VBox.setMargin(tags, new javafx.geometry.Insets(3, 0, 0, 0));

        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setMinWidth(Region.USE_PREF_SIZE);

        getChildren().addAll(text, actions);

        if (list != null) {
            // Largeur de la liste moins la barre de défilement et la marge des cellules.
            prefWidthProperty().bind(list.widthProperty().subtract(30));
            setMinWidth(0);
        }
    }

    public void setIcon(Image image) {
        icon.setImage(image);
    }

    public ImageView getIconView() {
        return icon;
    }

    public void setTitle(String text) {
        title.setText(text);
    }

    public void setSubtitle(String text) {
        show(subtitle, text);
    }

    public void setDescription(String text) {
        show(description, text);
    }

    /** Remplace les étiquettes ; les textes vides sont ignorés. */
    public void setTags(String... texts) {
        tags.getChildren().clear();
        for (String text : texts) {
            addTag(text);
        }
    }

    /** Ajoute une étiquette (classes supplémentaires possibles : ml-tag-on, ml-tag-off). */
    public Label addTag(String text, String... extraClasses) {
        if (text == null || text.trim().isEmpty()) return null;
        Label tag = new Label(text.trim());
        tag.getStyleClass().add("ml-tag");
        tag.getStyleClass().addAll(extraClasses);
        tag.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
        tag.setMinWidth(Region.USE_PREF_SIZE);
        tags.getChildren().add(tag);
        tags.setVisible(true);
        tags.setManaged(true);
        return tag;
    }

    public void clearTags() {
        tags.getChildren().clear();
    }

    public void setActions(Node... nodes) {
        actions.getChildren().setAll(nodes);
    }

    private static void show(Label label, String text) {
        boolean visible = text != null && !text.trim().isEmpty();
        label.setText(visible ? text.trim() : "");
        label.setVisible(visible);
        label.setManaged(visible);
    }

    /** Bouton d'action d'une ligne : icône + texte (primaire = orange). */
    public static JFXButton actionButton(String text, String iconPath, boolean primary) {
        JFXButton button = new JFXButton(text);
        if (iconPath != null) {
            button.setGraphic(Icons.stroke(iconPath, 14));
            button.setGraphicTextGap(8);
        }
        button.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        if (primary) UiStyle.primary(button);
        else UiStyle.secondary(button);
        button.setPrefHeight(34);
        button.setMinWidth(Region.USE_PREF_SIZE);
        return button;
    }

    /** Petit bouton carré avec une seule icône (bulle d'aide au survol). */
    public static JFXButton iconButton(String iconPath, String tooltip, String... styleClasses) {
        JFXButton button = new JFXButton();
        button.setGraphic(Icons.stroke(iconPath, 15));
        UiStyle.apply(button, styleClasses.length == 0 ? new String[]{"ml-secondary"} : styleClasses);
        button.setPrefSize(36, 34);
        button.setMinSize(36, 34);
        if (tooltip != null) button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    /** Liste transparente (cartes posées sur le panneau) avec un message quand elle est vide. */
    public static void styleList(ListView<?> list, String emptyText) {
        list.setStyle(null);
        if (!list.getStyleClass().contains("ml-list")) list.getStyleClass().add("ml-list");
        Label placeholder = new Label(emptyText);
        placeholder.getStyleClass().add("ml-hint");
        placeholder.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        placeholder.setWrapText(true);
        placeholder.setMaxWidth(360);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setStyle("-fx-text-alignment: center;");
        list.setPlaceholder(placeholder);
    }

    /** 282073685 -> « 282 M », 15400 -> « 15,4 k ». */
    public static String formatCount(long count) {
        if (count < 1000) return String.valueOf(Math.max(0, count));
        double value;
        String unit;
        if (count < 1000000L) {
            value = count / 1000.0;
            unit = " k";
        } else if (count < 1000000000L) {
            value = count / 1000000.0;
            unit = " M";
        } else {
            value = count / 1000000000.0;
            unit = " Md";
        }
        String number = value >= 100 ? String.valueOf(Math.round(value)) : String.format(Locale.FRANCE, "%.1f", value);
        if (number.endsWith(",0")) number = number.substring(0, number.length() - 2);
        return number + unit;
    }
}
