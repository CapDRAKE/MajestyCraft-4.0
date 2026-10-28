package fr.majestycraft.util;

import com.jfoenix.controls.JFXButton;
import fr.trxyy.alternative.alternative_api.utils.FontLoader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.function.Consumer;

/**
 * Fenêtre modale aux couleurs du launcher (carte sombre, pastille, titre, texte,
 * boutons ml-*). Une action peut lancer un travail long en arrière-plan : la
 * progression s'affiche dans la fenêtre et les boutons sont bloqués pendant ce temps.
 */
public final class MajestyDialog {

    public static final double WIDTH = 520;

    /** Travail long d'une action : reçoit de quoi afficher sa progression, renvoie un message final. */
    public interface Work {
        String run(Consumer<String> progress) throws Exception;
    }

    private final Stage stage = new Stage();
    private final VBox box = new VBox(14);
    private final VBox body = new VBox(10);
    private final Label status = new Label();
    private final FlowPane actions = new FlowPane(10, 10);
    private final StackPane root;
    private Runnable afterWork;

    public MajestyDialog(Window owner, String badge, String title, String message) {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);
        if (owner != null) stage.initOwner(owner);

        Label badgeLabel = new Label(badge);
        badgeLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
        badgeLabel.setStyle("-fx-background-color: rgba(255,159,26,0.14); -fx-background-radius: 999;"
                + "-fx-text-fill: rgba(255,207,138,0.95); -fx-padding: 6 14 6 14;");

        Label titleLabel = new Label(title);
        titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 19F));
        titleLabel.setStyle("-fx-text-fill: white;");
        titleLabel.setWrapText(true);

        body.getChildren().add(titleLabel);
        if (message != null && !message.isEmpty()) body.getChildren().add(text(message, 12.5F, "rgba(255,255,255,0.78)"));

        status.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11.5F));
        status.setStyle("-fx-text-fill: rgba(255,207,138,0.95);");
        status.setWrapText(true);
        status.setMaxWidth(WIDTH - 56);
        status.setVisible(false);
        status.setManaged(false);

        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPrefWrapLength(WIDTH - 56);

        box.getChildren().addAll(badgeLabel, body, status, actions);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(24, 28, 22, 28));
        box.setPrefWidth(WIDTH);
        box.setMaxWidth(WIDTH);
        box.setStyle("-fx-background-color: rgba(8,12,18,0.97); -fx-background-radius: 26;"
                + "-fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 26; -fx-border-width: 1;");
        box.setEffect(new DropShadow(45, Color.rgb(0, 0, 0, 0.5)));

        root = new StackPane(box);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root, Color.TRANSPARENT);
        scene.getStylesheets().add("css/design.css");
        stage.setScene(scene);

        // Déplacement à la souris (fenêtre sans barre de titre)
        final double[] drag = new double[2];
        box.setOnMousePressed(e -> { drag[0] = e.getScreenX() - stage.getX(); drag[1] = e.getScreenY() - stage.getY(); });
        box.setOnMouseDragged(e -> { stage.setX(e.getScreenX() - drag[0]); stage.setY(e.getScreenY() - drag[1]); });
    }

    /** Texte d'appoint (paragraphe) aux couleurs de la fenêtre. */
    public static Label text(String s, float size, String color) {
        Label l = new Label(s);
        l.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", size));
        l.setStyle("-fx-text-fill: " + color + ";");
        l.setWrapText(true);
        l.setMaxWidth(WIDTH - 56);
        return l;
    }

    /** Ajoute un élément sous le texte (liste, champ...). */
    public MajestyDialog content(Node node) {
        body.getChildren().add(node);
        return this;
    }

    /** Bouton qui ferme la fenêtre puis exécute l'action. style : primary, secondary ou danger. */
    public MajestyDialog action(String label, String style, Runnable run) {
        JFXButton b = button(label, style);
        b.setOnAction(e -> {
            stage.close();
            if (run != null) run.run();
        });
        actions.getChildren().add(b);
        return this;
    }

    /**
     * Bouton qui lance un travail long : la fenêtre reste ouverte, affiche la
     * progression puis le message final, et ne propose plus que « Fermer ».
     */
    public MajestyDialog work(String label, String style, Work work) {
        JFXButton b = button(label, style);
        b.setOnAction(e -> runWork(work));
        actions.getChildren().add(b);
        return this;
    }

    /** Action exécutée sur le thread JavaFX quand un travail se termine (succès ou échec). */
    public MajestyDialog afterWork(Runnable r) {
        this.afterWork = r;
        return this;
    }

    public MajestyDialog cancel(String label) {
        JFXButton b = button(label, "secondary");
        b.setOnAction(e -> stage.close());
        actions.getChildren().add(0, b);
        return this;
    }

    public void show() {
        stage.show();
    }

    public void showAndWait() {
        stage.showAndWait();
    }

    public void close() {
        stage.close();
    }

    /** Racine affichable (rendus hors écran). */
    public Parent root() {
        return root;
    }

    private void runWork(Work work) {
        actions.setDisable(true);
        setStatus("Préparation...");
        Thread t = new Thread(() -> {
            String done;
            try {
                done = work.run(msg -> Platform.runLater(() -> setStatus(msg)));
            } catch (Exception ex) {
                done = "Échec : " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            }
            final String message = done;
            Platform.runLater(() -> {
                setStatus(message);
                actions.getChildren().clear();
                JFXButton close = button("Fermer", "primary");
                close.setOnAction(e -> stage.close());
                actions.getChildren().add(close);
                actions.setDisable(false);
                if (afterWork != null) afterWork.run();
                stage.sizeToScene();
            });
        }, "MajestyLauncher-Dialog");
        t.setDaemon(true);
        t.start();
    }

    private void setStatus(String msg) {
        status.setText(msg);
        status.setVisible(true);
        status.setManaged(true);
        stage.sizeToScene();
    }

    private static JFXButton button(String label, String style) {
        JFXButton b = new JFXButton(label);
        b.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        b.setPrefHeight(36);
        b.setMinWidth(Region.USE_PREF_SIZE);
        if ("primary".equals(style)) UiStyle.primary(b);
        else if ("danger".equals(style)) UiStyle.danger(b);
        else UiStyle.secondary(b);
        b.setStyle("-fx-padding: 6 16 6 16;");
        return b;
    }
}
