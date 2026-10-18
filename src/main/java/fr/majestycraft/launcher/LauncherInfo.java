package fr.majestycraft.launcher;

import fr.majestycraft.util.UiStyle;
import fr.majestycraft.util.AmbientBackground;
import com.jfoenix.controls.JFXButton;

import fr.majestycraft.Main;
import fr.majestycraft.news.InfoFeed;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

/**
 * Panel Infos : changelog détaillé (multi-versions) et news du serveur, présentés
 * en deux onglets. Les contenus sont chargés en arrière-plan (distant avec repli
 * local pour le changelog) pour ne jamais bloquer l'ouverture du panel.
 */
public class LauncherInfo extends IScreen {

    private static final String DEVELOPPEUR_INFO = Main.bundle.getString("DEVELOPPEUR_INFO");
    private static final String HELPER_INFO = Main.bundle.getString("HELPER_INFO");
    private static final String REMARQUE_INFO_1 = Main.bundle.getString("REMARQUE_INFO_1");
    private static final String REMARQUE_INFO_2 = Main.bundle.getString("REMARQUE_INFO_2");
    private static final String VERSION_INFO = Main.bundle.getString("VERSION_INFO");
    private static final String QUITTER_BUTTON_TEXT = Main.bundle.getString("QUITTER_BUTTON_TEXT");

    private static final String SITE_URL = "https://majestycraft.com";
    private static final String DISCORD_URL = "https://discord.gg/qyuuHk4udD";

    private LauncherImage heroLogo;
    private LauncherLabel heroTitle;
    private LauncherLabel heroSubtitle;
    private LauncherLabel heroVersion;

    private LauncherLabel pageTitle;
    private LauncherLabel pageSubtitle;

    private JFXButton tabChangelog;
    private JFXButton tabNews;
    private VBox contentBox;
    private ScrollPane scroll;

    private LauncherButton quitButton;

    /** Contenus chargés (null tant que le chargement asynchrone n'a pas abouti). */
    private InfoFeed.Changelog changelog;
    private InfoFeed.News news;
    private boolean showingNews = false;

    public LauncherInfo(final Pane root, final GameEngine engine, final LauncherPanel pane) {
        AmbientBackground.installResponsive(root, false);

        final int W = engine.getWidth();
        final int H = engine.getHeight();


        LauncherRectangle leftDock = new LauncherRectangle(root, 0, 0, 84, H);
        leftDock.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(8, 12, 18, 0.84)),
                new Stop(1, Color.rgb(8, 12, 18, 0.28))
        ));

        LauncherRectangle separator = new LauncherRectangle(root, 83, 0, 1, H);
        separator.setFill(Color.rgb(255, 255, 255, 0.08));

        /* ===================== Colonne gauche (identité) ===================== */
        final int heroX = 120;
        final int heroY = 72;
        final int heroW = 250;
        final int heroH = 520;

        LauncherRectangle heroCard = new LauncherRectangle(root, heroX, heroY, heroW, heroH);
        styleCard(heroCard, 0.70);

        this.heroLogo = new LauncherImage(root);
        this.heroLogo.setImage(getResourceLocation().loadImage(engine, "launchergifpng.png"));
        this.heroLogo.setSize(110, 110);
        this.heroLogo.setBounds(heroX + 70, heroY + 30, 110, 110);

        this.heroTitle = new LauncherLabel(root);
        this.heroTitle.setText("Informations");
        this.heroTitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 24F));
        this.heroTitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.96)");
        this.heroTitle.setPosition(heroX, heroY + 152);
        this.heroTitle.setSize(heroW, 32);
        this.heroTitle.setAlignment(Pos.CENTER);

        this.heroSubtitle = new LauncherLabel(root);
        this.heroSubtitle.setText("MajestyLauncher");
        this.heroSubtitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        this.heroSubtitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.95)");
        this.heroSubtitle.setPosition(heroX, heroY + 184);
        this.heroSubtitle.setSize(heroW, 20);
        this.heroSubtitle.setAlignment(Pos.CENTER);

        LauncherRectangle versionBg = new LauncherRectangle(root, heroX + 26, heroY + 216, heroW - 52, 34);
        styleMiniCard(versionBg);
        this.heroVersion = new LauncherLabel(root);
        this.heroVersion.setText(VERSION_INFO);
        this.heroVersion.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        this.heroVersion.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.88)");
        this.heroVersion.setPosition(heroX + 26, heroY + 223);
        this.heroVersion.setSize(heroW - 52, 20);
        this.heroVersion.setAlignment(Pos.CENTER);

        // Bloc équipe / remarque, compact
        LauncherLabel devTitle = createSectionTitle(root, "ÉQUIPE", heroX + 26, heroY + 272, heroW - 52, 12);
        LauncherLabel devInfo = createBodyLabel(root, DEVELOPPEUR_INFO, heroX + 26, heroY + 294, heroW - 52, 20, 12);
        LauncherLabel helperInfo = createBodyLabel(root, HELPER_INFO, heroX + 26, heroY + 316, heroW - 52, 20, 12);

        LauncherLabel remarkTitle = createSectionTitle(root, "REMARQUE", heroX + 26, heroY + 352, heroW - 52, 12);
        // La remarque est stockee en deux cles : la premiere se termine par "et" et la
        // seconde contient la suite. Les afficher ensemble evite une phrase coupee net.
        LauncherLabel remarkInfo = createBodyLabel(root,
                REMARQUE_INFO_1 + " " + REMARQUE_INFO_2,
                heroX + 26, heroY + 374, heroW - 52, 78, 11);
        remarkInfo.setWrapText(true);

        // Liens rapides : largeur calculee pour tenir cote a cote dans la carte,
        // au lieu de valeurs en dur qui se chevauchaient (110 de large pour 88 d'ecart).
        final int linkGap = 10;
        final int linkW = (heroW - 52 - linkGap) / 2;
        final int linkY = heroY + 458;

        JFXButton siteBtn = ghostButton("Site web", linkW);
        siteBtn.setLayoutX(heroX + 26);
        siteBtn.setLayoutY(linkY);
        siteBtn.setOnAction(e -> openLink(SITE_URL));
        root.getChildren().add(siteBtn);

        JFXButton discordBtn = ghostButton("Discord", linkW);
        discordBtn.setLayoutX(heroX + 26 + linkW + linkGap);
        discordBtn.setLayoutY(linkY);
        discordBtn.setOnAction(e -> openLink(DISCORD_URL));
        root.getChildren().add(discordBtn);

        /* ===================== Carte principale (onglets) ===================== */
        final int mainX = 400;
        final int mainY = 72;
        final int mainW = W - 460;
        final int mainH = 520;

        LauncherRectangle mainCard = new LauncherRectangle(root, mainX, mainY, mainW, mainH);
        styleCard(mainCard, 0.76);

        this.pageTitle = new LauncherLabel(root);
        this.pageTitle.setText("INFORMATIONS");
        this.pageTitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 26F));
        this.pageTitle.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        this.pageTitle.setPosition(mainX, mainY + 20);
        this.pageTitle.setSize(mainW, 32);
        this.pageTitle.setAlignment(Pos.CENTER);

        this.pageSubtitle = new LauncherLabel(root);
        this.pageSubtitle.setText("Nouveautés du launcher et actualités du serveur");
        this.pageSubtitle.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        this.pageSubtitle.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.45);");
        this.pageSubtitle.setPosition(mainX, mainY + 50);
        this.pageSubtitle.setSize(mainW, 18);
        this.pageSubtitle.setAlignment(Pos.CENTER);

        // Onglets
        this.tabChangelog = tabButton("Nouveautés");
        this.tabChangelog.setLayoutX(mainX + 30);
        this.tabChangelog.setLayoutY(mainY + 80);
        this.tabChangelog.setOnAction(e -> { showingNews = false; refreshTabs(); renderContent(); });
        root.getChildren().add(this.tabChangelog);

        this.tabNews = tabButton("News du serveur");
        this.tabNews.setLayoutX(mainX + 30 + 158);
        this.tabNews.setLayoutY(mainY + 80);
        this.tabNews.setOnAction(e -> { showingNews = true; refreshTabs(); renderContent(); });
        root.getChildren().add(this.tabNews);

        // Zone de contenu défilante
        this.contentBox = new VBox(12);
        this.contentBox.setPadding(new Insets(4, 10, 12, 2));
        this.contentBox.setStyle("-fx-background-color: transparent;");

        this.scroll = new ScrollPane(this.contentBox);
        this.scroll.setLayoutX(mainX + 28);
        this.scroll.setLayoutY(mainY + 126);
        this.scroll.setPrefSize(mainW - 56, mainH - 126 - 26);
        this.scroll.setFitToWidth(true);
        this.scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.scroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-control-inner-background: transparent;"
        );
        root.getChildren().add(this.scroll);

        refreshTabs();
        showLoading();
        loadFeedsAsync();

        /* ===================== Boutons bas ===================== */
        LauncherButton logsButton = new LauncherButton(root);
        logsButton.setText("Voir les logs");
        UiStyle.secondary(logsButton);
        logsButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 14F));
        logsButton.setPosition(mainX + mainW - 150 - 150, mainY + mainH + 30);
        logsButton.setSize(140, 36);
        logsButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                openGameLogs(engine);
            }
        });

        this.quitButton = new LauncherButton(root);
        this.quitButton.setText(QUITTER_BUTTON_TEXT);
        UiStyle.primary(this.quitButton);
        this.quitButton.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        this.quitButton.setPosition(mainX + mainW - 150, mainY + mainH + 30);
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
            animateIn(heroSubtitle, -14, 10, 150);
            animateIn(heroVersion, -14, 10, 190);
            animateIn(devTitle, -14, 10, 230);
            animateIn(devInfo, -14, 10, 260);
            animateIn(helperInfo, -14, 10, 285);
            animateIn(remarkTitle, -14, 10, 315);
            animateIn(remarkInfo, -14, 10, 340);
            animateIn(siteBtn, -14, 10, 370);
            animateIn(discordBtn, -14, 10, 395);

            animateIn(mainCard, 22, 0, 100);
            animateIn(pageTitle, 18, 0, 150);
            animateIn(pageSubtitle, 18, 0, 190);
            animateIn(tabChangelog, 18, 0, 230);
            animateIn(tabNews, 18, 0, 260);
            animateIn(scroll, 18, 0, 300);

            animateIn(logsButton, 0, 12, 620);
            animateIn(quitButton, 0, 12, 650);
        });
    }

    /* ─────────────────────────── Chargement ─────────────────────────── */

    private void loadFeedsAsync() {
        Thread t = new Thread(() -> {
            final InfoFeed.Changelog cl = InfoFeed.loadChangelog();
            final InfoFeed.News nw = InfoFeed.loadNews();
            Platform.runLater(() -> {
                this.changelog = cl;
                this.news = nw;
                renderContent();
            });
        }, "MajestyLauncher-InfoFeed");
        t.setDaemon(true);
        t.start();
    }

    private void showLoading() {
        contentBox.getChildren().clear();
        Label loading = new Label("Chargement...");
        loading.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        loading.setStyle("-fx-text-fill: rgba(255,255,255,0.55);");
        contentBox.getChildren().add(loading);
    }

    /* ─────────────────────────── Rendu ─────────────────────────── */

    private void renderContent() {
        contentBox.getChildren().clear();
        if (showingNews) {
            renderNews();
        } else {
            renderChangelog();
        }
        scroll.setVvalue(0);
    }

    private void renderChangelog() {
        if (changelog == null) {
            showLoading();
            return;
        }
        List<InfoFeed.Release> versions = changelog.versions;
        if (versions == null || versions.isEmpty()) {
            contentBox.getChildren().add(emptyState("Aucun changelog disponible pour le moment."));
            return;
        }

        boolean first = true;
        for (InfoFeed.Release release : versions) {
            contentBox.getChildren().add(buildReleaseBlock(release, first));
            first = false;
        }
    }

    private Pane buildReleaseBlock(InfoFeed.Release release, boolean isCurrent) {
        VBox block = new VBox(8);
        block.setPadding(new Insets(14, 16, 16, 16));
        block.setStyle(
                "-fx-background-color: " + (isCurrent ? "rgba(255,150,0,0.10)" : "rgba(255,255,255,0.045)") + ";" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: " + (isCurrent ? "rgba(255,170,0,0.45)" : "rgba(255,255,255,0.09)") + ";" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;"
        );

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label version = new Label("Version " + safe(release.version));
        version.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
        version.setStyle("-fx-text-fill: " + (isCurrent ? "rgba(255,205,130,0.98)" : "rgba(255,255,255,0.92)") + "; -fx-font-weight: bold;");

        Label date = new Label(safe(release.date));
        date.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 11F));
        date.setStyle("-fx-text-fill: rgba(255,255,255,0.45);");

        header.getChildren().addAll(version, date);

        if (isCurrent) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label current = new Label("ACTUELLE");
            current.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 9F));
            current.setStyle(
                    "-fx-background-color: rgba(255,159,26,0.20);" +
                    "-fx-background-radius: 999;" +
                    "-fx-text-fill: rgba(255,207,138,0.95);" +
                    "-fx-padding: 3 10 3 10;"
            );
            header.getChildren().addAll(spacer, current);
        }

        block.getChildren().add(header);

        if (release.entries != null) {
            for (InfoFeed.Entry entry : release.entries) {
                block.getChildren().add(buildEntryRow(entry));
            }
        }
        return block;
    }

    private Pane buildEntryRow(InfoFeed.Entry entry) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(row, new Insets(2, 0, 0, 0));

        Label badge = new Label(entry.badge());
        badge.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 9F));
        badge.setMinWidth(76);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle(
                "-fx-background-color: " + entry.badgeColor() + "22;" +
                "-fx-background-radius: 999;" +
                "-fx-text-fill: " + entry.badgeColor() + ";" +
                "-fx-padding: 3 8 3 8;" +
                "-fx-font-weight: bold;"
        );

        Label text = new Label(safe(entry.text));
        text.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        text.setStyle("-fx-text-fill: rgba(255,255,255,0.84);");
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);
        text.setMaxWidth(Double.MAX_VALUE);

        row.getChildren().addAll(badge, text);
        return row;
    }

    private void renderNews() {
        if (news == null) {
            contentBox.getChildren().add(emptyState(
                    "Actualités indisponibles.\n\nImpossible de joindre le serveur MajestyCraft pour le moment. "
                    + "Vérifie ta connexion ou consulte le site et le Discord."));
            return;
        }
        if (news.items == null || news.items.isEmpty()) {
            contentBox.getChildren().add(emptyState("Aucune actualité pour le moment."));
            return;
        }
        for (InfoFeed.NewsItem item : news.items) {
            contentBox.getChildren().add(buildNewsCard(item));
        }
    }

    private Pane buildNewsCard(InfoFeed.NewsItem item) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 16, 16));
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.045);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: rgba(255,255,255,0.09);" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;"
        );

        Label title = new Label(safe(item.title));
        title.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 15F));
        title.setStyle("-fx-text-fill: rgba(255,255,255,0.94); -fx-font-weight: bold;");
        title.setWrapText(true);

        Label date = new Label(safe(item.date));
        date.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 10F));
        date.setStyle("-fx-text-fill: rgba(255,176,0,0.80);");

        Label body = new Label(safe(item.body));
        body.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        body.setStyle("-fx-text-fill: rgba(255,255,255,0.78);");
        body.setWrapText(true);

        card.getChildren().addAll(title, date, body);

        if (item.url != null && !item.url.trim().isEmpty()) {
            JFXButton read = ghostButton("Lire la suite", 130);
            read.setOnAction(e -> openLink(item.url));
            HBox actions = new HBox(read);
            VBox.setMargin(actions, new Insets(6, 0, 0, 0));
            card.getChildren().add(actions);
        }
        return card;
    }

    private Label emptyState(String message) {
        Label label = new Label(message);
        label.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        label.setStyle("-fx-text-fill: rgba(255,255,255,0.50);");
        label.setWrapText(true);
        return label;
    }

    /* ─────────────────────────── Styles / helpers ─────────────────────────── */

    private void refreshTabs() {
        styleTab(tabChangelog, !showingNews);
        styleTab(tabNews, showingNews);
    }

    private JFXButton tabButton(String text) {
        JFXButton b = new JFXButton(text);
        b.setPrefSize(150, 34);
        return b;
    }

    private void styleTab(JFXButton b, boolean active) {
        if (b == null) return;
        b.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 13F));
        UiStyle.tab(b, active);
    }

    private JFXButton ghostButton(String text, int width) {
        JFXButton b = new JFXButton(text);
        b.setPrefSize(width, 32);
        b.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 12F));
        UiStyle.secondary(b);
        return b;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
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

    private LauncherLabel createSectionTitle(Pane root, String text, int x, int y, int w, int size) {
        LauncherLabel label = new LauncherLabel(root);
        label.setText(text);
        label.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", (float) size));
        label.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,176,0,0.95);");
        label.setPosition(x, y);
        label.setSize(w, 18);
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

    /**
     * Ouvre le fichier de log de la dernière session de jeu (ou le dossier logs).
     * Pratique pour diagnostiquer un crash ou transmettre le log au support.
     */
    private void openGameLogs(GameEngine engine) {
        java.io.File target = null;
        try {
            java.io.File gameDir = engine.getGameFolder().getGameDir();
            java.io.File logsDir = new java.io.File(gameDir, "logs");
            java.io.File latest = new java.io.File(logsDir, "latest-game.log");
            target = latest.isFile() ? latest : (logsDir.isDirectory() ? logsDir : gameDir);
        } catch (Exception ignored) {
            // dossier de jeu indisponible : message ci-dessous
        }
        if (target == null || !target.exists()) {
            showLogsUnavailable();
            return;
        }
        fr.majestycraft.util.SystemOpen.openAsync(target, this::showLogsUnavailable);
    }

    private void showLogsUnavailable() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Logs");
        alert.setHeaderText("Impossible d'ouvrir les logs");
        alert.setContentText("Aucun log de jeu n'est disponible pour le moment (lance le jeu au moins une fois).");
        alert.showAndWait();
    }
}
