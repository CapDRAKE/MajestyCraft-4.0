package fr.majestycraft.launcher;

import fr.majestycraft.Main;
import fr.trxyy.alternative.alternative_api.utils.*;
import animatefx.animation.ZoomOutDown;
import fr.trxyy.alternative.alternative_api.GameEngine;
import fr.trxyy.alternative.alternative_api_ui.base.IScreen;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherButton;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherImage;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherLabel;
import fr.trxyy.alternative.alternative_api_ui.components.LauncherRectangle;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LauncherInfo extends IScreen {

    private static final String DEVELOPPEUR_TITLE = Main.bundle.getString("DEVELOPPEUR_TITLE");
    private static final String DEVELOPPEUR_INFO = Main.bundle.getString("DEVELOPPEUR_INFO");
    private static final String HELPER_INFO = Main.bundle.getString("HELPER_INFO");
    private static final String REMARQUE_TITLE = Main.bundle.getString("REMARQUE_TITLE");
    private static final String REMARQUE_INFO_1 = Main.bundle.getString("REMARQUE_INFO_1");
    private static final String REMARQUE_INFO_2 = Main.bundle.getString("REMARQUE_INFO_2");
    private static final String CHANGELOGS_TITLE = Main.bundle.getString("CHANGELOGS_TITLE");
    private static final String CHANGELOG_1 = Main.bundle.getString("CHANGELOG_1");
    private static final String CHANGELOG_2 = Main.bundle.getString("CHANGELOG_2");
    private static final String CHANGELOG_3 = Main.bundle.getString("CHANGELOG_3");
    private static final String CHANGELOG_4 = Main.bundle.getString("CHANGELOG_4");
    private static final String VERSION_INFO = Main.bundle.getString("VERSION_INFO");
    private static final String QUITTER_BUTTON_TEXT = Main.bundle.getString("QUITTER_BUTTON_TEXT");

    private static final boolean CHANGE_1 = true;
    private static final boolean CHANGE_2 = true;
    private static final boolean CHANGE_3 = true;
    private static final boolean CHANGE_4 = true;

    private LauncherImage heroLogo;
    private LauncherLabel heroTitle;
    private LauncherLabel heroSubtitle;
    private LauncherLabel heroVersion;
    private LauncherLabel heroTag1;
    private LauncherLabel heroTag2;

    private LauncherLabel pageTitle;
    private LauncherLabel pageSubtitle;

    private LauncherLabel devTitle;
    private LauncherLabel devInfo;
    private LauncherLabel helperInfo;

    private LauncherLabel remarkTitle;
    private LauncherLabel remarkInfo1;
    private LauncherLabel remarkInfo2;

    private LauncherLabel changelogTitle;

    private LauncherLabel change1;
    private LauncherLabel change2;
    private LauncherLabel change3;
    private LauncherLabel change4;

    private LauncherButton quitButton;

    public LauncherInfo(final Pane root, final GameEngine engine, final LauncherPanel pane) {
        this.drawBackgroundImage(engine, root, "background.png");

        final int W = engine.getWidth();
        final int H = engine.getHeight();

        Rectangle overlay = new Rectangle(W, H);
        overlay.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 0, 0, 0.18)),
                new Stop(1, Color.rgb(0, 0, 0, 0.74))
        ));
        root.getChildren().add(overlay);

        LauncherRectangle leftDock = new LauncherRectangle(root, 0, 0, 84, H);
        leftDock.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(8, 12, 18, 0.84)),
                new Stop(1, Color.rgb(8, 12, 18, 0.28))
        ));

        LauncherRectangle separator = new LauncherRectangle(root, 83, 0, 1, H);
        separator.setFill(Color.rgb(255, 255, 255, 0.08));

        final int heroX = 120;
        final int heroY = 72;
        final int heroW = 250;
        final int heroH = 520;

        LauncherRectangle heroCard = new LauncherRectangle(root, heroX, heroY, heroW, heroH);
        styleCard(heroCard, 0.70);

        this.heroLogo = new LauncherImage(root);
        this.heroLogo.setImage(getResourceLocation().loadImage(engine, "launchergifpng.png"));
        this.heroLogo.setSize(120, 120);
        this.heroLogo.setBounds(heroX + 65, heroY + 34, 120, 120);

        this.heroTitle = new LauncherLabel(root);
        this.heroTitle.setText("Informations");
        this.heroTitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 26F));
        this.heroTitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.96)");
        this.heroTitle.setPosition(heroX, heroY + 175);
        this.heroTitle.setSize(heroW, 34);
        this.heroTitle.setAlignment(Pos.CENTER);

        this.heroSubtitle = new LauncherLabel(root);
        this.heroSubtitle.setText("MajestyLauncher");
        this.heroSubtitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        this.heroSubtitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.95)");
        this.heroSubtitle.setPosition(heroX, heroY + 210);
        this.heroSubtitle.setSize(heroW, 20);
        this.heroSubtitle.setAlignment(Pos.CENTER);

        this.heroVersion = new LauncherLabel(root);
        this.heroVersion.setText(VERSION_INFO);
        this.heroVersion.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        this.heroVersion.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.68)");
        this.heroVersion.setPosition(heroX + 18, heroY + 272);
        this.heroVersion.setSize(heroW - 36, 42);
        this.heroVersion.setAlignment(Pos.CENTER);

        LauncherRectangle tag1Bg = new LauncherRectangle(root, heroX + 30, heroY + 350, 190, 34);
        styleMiniCard(tag1Bg);
        this.heroTag1 = new LauncherLabel(root);
        this.heroTag1.setText("Interface modernisée");
        this.heroTag1.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.heroTag1.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.90)");
        this.heroTag1.setPosition(heroX + 30, heroY + 357);
        this.heroTag1.setSize(190, 20);
        this.heroTag1.setAlignment(Pos.CENTER);

        LauncherRectangle tag2Bg = new LauncherRectangle(root, heroX + 30, heroY + 394, 190, 34);
        styleMiniCard(tag2Bg);
        this.heroTag2 = new LauncherLabel(root);
        this.heroTag2.setText("Suivi des nouveautés");
        this.heroTag2.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.heroTag2.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.90)");
        this.heroTag2.setPosition(heroX + 30, heroY + 401);
        this.heroTag2.setSize(190, 20);
        this.heroTag2.setAlignment(Pos.CENTER);

        final int mainX = 400;
        final int mainY = 72;
        final int mainW = W - 460;
        final int mainH = 520;

        LauncherRectangle mainCard = new LauncherRectangle(root, mainX, mainY, mainW, mainH);
        styleCard(mainCard, 0.76);

        this.pageTitle = new LauncherLabel(root);
        this.pageTitle.setText("INFORMATIONS");
        this.pageTitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 28F));
        this.pageTitle.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        this.pageTitle.setPosition(mainX, mainY + 22);
        this.pageTitle.setSize(mainW, 34);
        this.pageTitle.setAlignment(Pos.CENTER);

        this.pageSubtitle = new LauncherLabel(root);
        this.pageSubtitle.setText("Equipe, remarques et changelogs");
        this.pageSubtitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.pageSubtitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.45);");
        this.pageSubtitle.setPosition(mainX, mainY + 54);
        this.pageSubtitle.setSize(mainW, 18);
        this.pageSubtitle.setAlignment(Pos.CENTER);

        int contentX = mainX + 34;
        int contentW = mainW - 68;

        this.devTitle = createSectionTitle(root, DEVELOPPEUR_TITLE, contentX, mainY + 98, 220);
        this.devInfo = createBodyLabel(root, DEVELOPPEUR_INFO, contentX, mainY + 130, contentW, 24, 18);
        this.helperInfo = createBodyLabel(root, HELPER_INFO, contentX, mainY + 160, contentW, 24, 18);

        this.remarkTitle = createSectionTitle(root, REMARQUE_TITLE, contentX, mainY + 208, 220);
        this.remarkInfo1 = createBodyLabel(root, REMARQUE_INFO_1, contentX, mainY + 240, contentW, 24, 14);
        this.remarkInfo2 = createBodyLabel(root, REMARQUE_INFO_2, contentX + 34, mainY + 262, contentW - 34, 24, 14);

        this.changelogTitle = createSectionTitle(root, CHANGELOGS_TITLE, contentX, mainY + 308, 220);

        LauncherRectangle c1 = new LauncherRectangle(root, contentX, mainY + 344, contentW, 36);
        LauncherRectangle c2 = new LauncherRectangle(root, contentX, mainY + 388, contentW, 36);
        LauncherRectangle c3 = new LauncherRectangle(root, contentX, mainY + 432, contentW, 36);
        LauncherRectangle c4 = new LauncherRectangle(root, contentX, mainY + 476, contentW, 36);
        styleMiniCard(c1);
        styleMiniCard(c2);
        styleMiniCard(c3);
        styleMiniCard(c4);

        this.change1 = createBodyLabel(root, CHANGELOG_1, contentX + 14, mainY + 352, contentW - 28, 20, 15);
        this.change1.setVisible(CHANGE_1);

        this.change2 = createBodyLabel(root, CHANGELOG_2, contentX + 14, mainY + 396, contentW - 28, 20, 15);
        this.change2.setVisible(CHANGE_2);

        this.change3 = createBodyLabel(root, CHANGELOG_3, contentX + 14, mainY + 440, contentW - 28, 20, 15);
        this.change3.setVisible(CHANGE_3);

        this.change4 = createBodyLabel(root, CHANGELOG_4, contentX + 14, mainY + 484, contentW - 28, 20, 15);
        this.change4.setVisible(CHANGE_4);

        this.quitButton = new LauncherButton(root);
        this.quitButton.setText(QUITTER_BUTTON_TEXT);
        this.quitButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff9800, #ff6d00);" +
                "-fx-background-radius: 18;" +
                "-fx-text-fill: white;"
        );
        this.quitButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        this.quitButton.setPosition(mainX + mainW - 150, mainY + mainH +30);
        this.quitButton.setSize(110, 36);
        this.quitButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                final ZoomOutDown animation = new ZoomOutDown(root);
                animation.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent actionEvent) {
                        Stage stage = (Stage) ((LauncherButton) event.getSource()).getScene().getWindow();
                        stage.close();
                    }
                });
                animation.setResetOnFinished(true);
                animation.play();
            }
        });

        Platform.runLater(() -> {
            animateIn(heroLogo, -14, 10, 60);
            animateIn(heroTitle, -14, 10, 110);
            animateIn(heroSubtitle, -14, 10, 160);
            animateIn(heroVersion, -14, 10, 210);
            animateIn(heroTag1, -14, 10, 260);
            animateIn(heroTag2, -14, 10, 310);

            animateIn(mainCard, 22, 0, 100);
            animateIn(pageTitle, 18, 0, 150);
            animateIn(pageSubtitle, 18, 0, 190);

            animateIn(devTitle, 18, 0, 240);
            animateIn(devInfo, 18, 0, 280);
            animateIn(helperInfo, 18, 0, 320);

            animateIn(remarkTitle, 18, 0, 360);
            animateIn(remarkInfo1, 18, 0, 400);
            animateIn(remarkInfo2, 18, 0, 440);

            animateIn(changelogTitle, 18, 0, 480);
            animateIn(change1, 18, 0, 520);
            animateIn(change2, 18, 0, 550);
            animateIn(change3, 18, 0, 580);
            animateIn(change4, 18, 0, 610);

            animateIn(quitButton, 0, 12, 650);
        });
    }

    private void styleCard(LauncherRectangle r, double opacity) {
        r.setArcWidth(34);
        r.setArcHeight(34);
        r.setFill(Color.rgb(8, 12, 18, opacity));
        r.setStroke(Color.rgb(255, 255, 255, 0.10));
        r.setStrokeWidth(1);
        r.setEffect(new DropShadow(36, Color.rgb(0, 0, 0, 0.72)));
        r.setMouseTransparent(true);
    }

    private void styleMiniCard(LauncherRectangle r) {
        r.setArcWidth(20);
        r.setArcHeight(20);
        r.setFill(Color.rgb(255, 255, 255, 0.06));
        r.setStroke(Color.rgb(255, 255, 255, 0.08));
        r.setStrokeWidth(1);
        r.setMouseTransparent(true);
    }

    private LauncherLabel createSectionTitle(Pane root, String text, int x, int y, int w) {
        LauncherLabel label = new LauncherLabel(root);
        label.setText(text);
        label.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 20F));
        label.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.95);");
        label.setPosition(x, y);
        label.setSize(w, 26);
        return label;
    }

    private LauncherLabel createBodyLabel(Pane root, String text, int x, int y, int w, int h, int size) {
        LauncherLabel label = new LauncherLabel(root);
        label.setText(text);
        label.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", (float) size));
        label.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.84);");
        label.setPosition(x, y);
        label.setSize(w, h);
        return label;
    }

    private void animateIn(Node node, double fromX, double fromY, int delayMs) {
        if (node == null) return;

        node.setOpacity(0);
        node.setTranslateX(fromX);
        node.setTranslateY(fromY);

        FadeTransition ft = new FadeTransition(Duration.millis(420), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(420), node);
        tt.setFromX(fromX);
        tt.setToX(0);
        tt.setFromY(fromY);
        tt.setToY(0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }
}