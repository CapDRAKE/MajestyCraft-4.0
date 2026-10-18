package fr.majestycraft.util;

import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Interrupteur (style « switch ») à utiliser à la place d'une case à cocher : même API
 * que les cases (isSelected, setSelected, setOnAction, selectedProperty), le texte est
 * affiché à droite de l'interrupteur. Apparence dans css/design.css (classe ml-switch) :
 * piste grise, orange quand activé, pastille blanche qui glisse.
 */
public class ToggleSwitch extends ToggleButton {

    private static final double TRACK_WIDTH = 34;
    private static final double TRACK_HEIGHT = 18;
    private static final double KNOB_RADIUS = 7;
    private static final double KNOB_OFF = 2;
    private static final double KNOB_ON = TRACK_WIDTH - KNOB_RADIUS * 2 - 2;

    private final Circle knob = new Circle(KNOB_RADIUS, Color.WHITE);
    private TranslateTransition slide;
    private boolean instant;

    public ToggleSwitch(String text) {
        super(text);
        Rectangle track = new Rectangle(TRACK_WIDTH, TRACK_HEIGHT);
        track.setArcWidth(TRACK_HEIGHT);
        track.setArcHeight(TRACK_HEIGHT);
        track.setFill(Color.rgb(255, 255, 255, 0.16));
        track.getStyleClass().add("ml-switch-track");
        knob.getStyleClass().add("ml-switch-knob");

        StackPane graphic = new StackPane(track, knob);
        graphic.setMinSize(TRACK_WIDTH, TRACK_HEIGHT);
        graphic.setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);
        StackPane.setAlignment(knob, Pos.CENTER_LEFT);
        graphic.setMouseTransparent(true);

        setGraphic(graphic);
        setContentDisplay(ContentDisplay.LEFT);
        setGraphicTextGap(12);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("ml-switch");

        knob.setTranslateX(isSelected() ? KNOB_ON : KNOB_OFF);
        selectedProperty().addListener((obs, was, on) -> {
            if (slide != null) slide.stop();
            double target = on ? KNOB_ON : KNOB_OFF;
            // Glissement seulement à l'écran ; sinon (construction, cellule réutilisée
            // au défilement) la pastille est posée directement à sa place.
            if (instant || getScene() == null || getScene().getWindow() == null || !getScene().getWindow().isShowing()) {
                knob.setTranslateX(target);
                return;
            }
            slide = new TranslateTransition(Duration.millis(140), knob);
            slide.setToX(target);
            slide.play();
        });
    }

    /** Change l'état sans animation (cellules de liste : l'état suit l'élément affiché). */
    public void setSelectedInstantly(boolean selected) {
        instant = true;
        try {
            setSelected(selected);
        } finally {
            instant = false;
        }
    }
}
