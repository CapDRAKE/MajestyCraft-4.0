package fr.majestycraft.launcher;

import animatefx.animation.*;
import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.utils.*;
import fr.trxyy.alternative.alternative_api_ui.base.*;
import fr.trxyy.alternative.alternative_api_ui.components.*;
import javafx.event.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;

public class LauncherInfo extends IScreen {
	
    private static final String DEVELOPPEUR_TITLE = "Développeur";
    private static final String DEVELOPPEUR_INFO = "Développeur : Capdrake, Ostix360";
    private static final String HELPER_INFO = "Helper : aucun";
    private static final String REMARQUE_TITLE = "Remarque";
    private static final String REMARQUE_INFO_1 = "1 => Si une version ne démarre pas, allez dans votre %appdata% et ";
    private static final String REMARQUE_INFO_2 = "supprimez les fichiers 'Launcher_config.json' et '.majestycraft'";
    private static final String CHANGELOGS_TITLE = "Changelogs";
    private static final String CHANGELOG_1 = "- La connexion automatique des comptes microsoft est entièrement fonctionnelles";
    private static final boolean CHANGE_1 = true;
    private static final String CHANGELOG_2 = "- Ajout d'Optifine sur la 1.19.4";
    private static final boolean CHANGE_2 = true;
    private static final String CHANGELOG_3 = "- Correction du bug de la tête du joueur mal affichée";
    private static final boolean CHANGE_3 = false;
    private static final String CHANGELOG_4 = "- Ajout d'un bouton démarrer lors de la connexion auto";
    private static final boolean CHANGE_4 = false;
    private static final String VERSION_TITLE = "Version";
    private static final String VERSION_INFO = "Version : 3.2 (27/03/2023)";
    private static final String QUITTER_BUTTON_TEXT = "Retour";

	private LauncherLabel titleLabel;
	private LauncherLabel developpeur;
	private LauncherLabel DEV;
	private LauncherLabel HELPER;
	private LauncherLabel remarque;
	private LauncherLabel REM;
	private LauncherLabel REM2;
	private LauncherLabel version;
	private LauncherLabel VERSION;

	private LauncherLabel CHANGE1;
	private LauncherLabel CHANGE2;
	private LauncherLabel CHANGE3;
	private LauncherLabel CHANGE4;
	// private LauncherLabel CHANGE5;
	// private LauncherLabel CHANGE6;
	private LauncherLabel changelogs;
	//private Slider volume;

	private LauncherRectangle topRectangle;

	private LauncherButton quit;

	public LauncherInfo(final Pane root, final GameEngine engine, final LauncherPanel pane) {
		this.drawBackgroundImage(engine, root, "background.png");
		/** ===================== RECTANGLE NOIR EN HAUT ===================== */
		this.topRectangle = new LauncherRectangle(root, 0, 0, 1500, 15);
		this.topRectangle.setOpacity(0.7);
		this.topRectangle.setVisible(false);
		/** ===================== LABEL TITRE ===================== */
		this.titleLabel = new LauncherLabel(root);
		this.titleLabel.setText("INFORMATIONS");
		this.titleLabel.setStyle("-fx-text-fill: white;");
		this.titleLabel.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 26F));
		this.titleLabel.setPosition(350, 20);
		this.titleLabel.setSize(230, 35);

	    /** ===================== TITRE DEVELOPPEUR ===================== */
	    this.developpeur = new LauncherLabel(root);
	    this.developpeur.setText(DEVELOPPEUR_TITLE);
		this.developpeur.setFont(Font.font("FontName", FontWeight.BOLD, 24d));
		this.developpeur.setStyle("-fx-background-color: transparent; -fx-text-fill: orange");
		this.developpeur.setPosition(engine.getWidth() / 2 - 479, engine.getHeight() / 2 - 300);
		this.developpeur.setOpacity(0.7);
		this.developpeur.setSize(500, 40);
		this.developpeur.setVisible(true);

	    /** ===================== TITRE DEV ===================== */
	    this.DEV = new LauncherLabel(root);
	    this.DEV.setText(DEVELOPPEUR_INFO);
		this.DEV.setFont(Font.font("FontName", FontWeight.BOLD, 20d));
		this.DEV.setStyle("-fx-text-fill: white;");
		this.DEV.setPosition(engine.getWidth() / 2 - 449, engine.getHeight() / 2 - 250);
		this.DEV.setOpacity(0.7);
		this.DEV.setSize(500, 40);
		this.DEV.setVisible(true);

	    /** ===================== TITRE HELPER ===================== */
	    this.HELPER = new LauncherLabel(root);
	    this.HELPER.setText(HELPER_INFO);
		this.HELPER.setFont(Font.font("FontName", FontWeight.BOLD, 20d));
		this.HELPER.setStyle("-fx-text-fill: white;");
		this.HELPER.setPosition(engine.getWidth() / 2 - 449, engine.getHeight() / 2 - 220);
		this.HELPER.setOpacity(0.7);
		this.HELPER.setSize(500, 40);
		this.HELPER.setVisible(true);

	    /** ===================== TITRE REMARQUE ===================== */
	    this.remarque = new LauncherLabel(root);
	    this.remarque.setText(REMARQUE_TITLE);
		this.remarque.setFont(Font.font("FontName", FontWeight.BOLD, 24d));
		this.remarque.setStyle("-fx-background-color: transparent; -fx-text-fill: orange");
		this.remarque.setPosition(engine.getWidth() / 2 - 479, engine.getHeight() / 2 - 180);
		this.remarque.setOpacity(0.7);
		this.remarque.setSize(500, 40);
		this.remarque.setVisible(true);

	    /** ===================== TITRE remarque 1 ===================== */
	    this.REM = new LauncherLabel(root);
	    this.REM.setText(REMARQUE_INFO_1);
		this.REM.setFont(Font.font("FontName", FontWeight.BOLD, 16d));
		this.REM.setStyle("-fx-text-fill: white;");
		this.REM.setPosition(engine.getWidth() / 2 - 449, engine.getHeight() / 2 - 140);
		this.REM.setOpacity(0.7);
		this.REM.setSize(1000, 40);
		this.REM.setVisible(true);

	    /** ===================== TITRE SUITE remarque 1 ===================== */
	    this.REM2 = new LauncherLabel(root);
	    this.REM2.setText(REMARQUE_INFO_2);
		this.REM2.setFont(Font.font("FontName", FontWeight.BOLD, 16d));
		this.REM2.setStyle("-fx-text-fill: white;");
		this.REM2.setPosition(engine.getWidth() / 2 - 404, engine.getHeight() / 2 - 120);
		this.REM2.setOpacity(0.7);
		this.REM2.setSize(1000, 40);
		this.REM2.setVisible(true);

	    /** ===================== TITRE CHANGELOGS ===================== */
	    this.changelogs = new LauncherLabel(root);
	    this.changelogs.setText(CHANGELOGS_TITLE);
		this.changelogs.setFont(Font.font("FontName", FontWeight.BOLD, 24d));
		this.changelogs.setStyle("-fx-background-color: transparent; -fx-text-fill: orange");
		this.changelogs.setPosition(engine.getWidth() / 2 - 479, engine.getHeight() / 2 - 60);
		this.changelogs.setOpacity(0.7);
		this.changelogs.setSize(500, 40);
		this.changelogs.setVisible(true);

	    /** ===================== changelogs 1 ===================== */
	    this.CHANGE1 = new LauncherLabel(root);
	    this.CHANGE1.setText(CHANGELOG_1);
		this.CHANGE1.setFont(Font.font("FontName", FontWeight.BOLD, 20d));
		this.CHANGE1.setStyle("-fx-text-fill: white;");
		this.CHANGE1.setPosition(engine.getWidth() / 2 - 449, engine.getHeight() / 2 - 20);
		this.CHANGE1.setOpacity(0.7);
		this.CHANGE1.setSize(1000, 40);
		this.CHANGE1.setVisible(CHANGE_1);

		/** ===================== changelogs 2 ===================== */
		this.CHANGE2 = new LauncherLabel(root);
		this.CHANGE2.setText(CHANGELOG_2);
		this.CHANGE2.setFont(Font.font("FontName", FontWeight.BOLD, 20d));
		this.CHANGE2.setStyle("-fx-text-fill: white;");
		this.CHANGE2.setPosition(engine.getWidth() / 2 - 449, engine.getHeight() / 2 + 20);
		this.CHANGE2.setOpacity(0.7);
		this.CHANGE2.setSize(1000, 40);
		this.CHANGE2.setVisible(CHANGE_2);

		/** ===================== changelogs 3 ===================== */
		this.CHANGE3 = new LauncherLabel(root);
		this.CHANGE3.setText(CHANGELOG_3);
		this.CHANGE3.setFont(Font.font("FontName", FontWeight.BOLD, 20d));
		this.CHANGE3.setStyle("-fx-text-fill: white;");
		this.CHANGE3.setPosition(engine.getWidth() / 2 - 449, engine.getHeight() / 2 + 60);
		this.CHANGE3.setOpacity(0.7);
		this.CHANGE3.setSize(1000, 40);
		this.CHANGE3.setVisible(CHANGE_3);

		/** ===================== changelogs 4 ===================== */
		this.CHANGE4 = new LauncherLabel(root);
		this.CHANGE4.setText(CHANGELOG_4);
		this.CHANGE4.setFont(Font.font("FontName", FontWeight.BOLD, 20d));
		this.CHANGE4.setStyle("-fx-text-fill: white;");
		this.CHANGE4.setPosition(engine.getWidth() / 2 - 449, engine.getHeight() / 2 + 100);
		this.CHANGE4.setOpacity(0.7);
		this.CHANGE4.setSize(1000, 40);
		this.CHANGE4.setVisible(CHANGE_4);

		/** ===================== TITRE VERSION ===================== */
		this.version = new LauncherLabel(root);
		this.version.setText(VERSION_TITLE);
		this.version.setFont(Font.font("FontName", FontWeight.BOLD, 24d));
		this.version.setStyle("-fx-background-color: transparent; -fx-text-fill: orange");
		this.version.setPosition(engine.getWidth() / 2 - 479, engine.getHeight() / 2 + 140);
		this.version.setOpacity(0.7);
		this.version.setSize(500, 40);
		this.version.setVisible(true);

		/** ===================== VERSION INFO ===================== */
		this.VERSION = new LauncherLabel(root);
		this.VERSION.setText(VERSION_INFO);
		this.VERSION.setFont(Font.font("FontName", FontWeight.BOLD, 20d));
		this.VERSION.setStyle("-fx-text-fill: white;");
		this.VERSION.setPosition(engine.getWidth() / 2 - 449, engine.getHeight() / 2 + 180);
		this.VERSION.setOpacity(0.7);
		this.VERSION.setSize(1000, 40);
		this.VERSION.setVisible(true);

		/** ===================== BOUTON QUITTER ===================== */
		this.quit = new LauncherButton(root);
		this.quit.setText(QUITTER_BUTTON_TEXT);
		this.quit.setStyle("-fx-background-color: rgba(53, 89, 119, 0.4); -fx-text-fill: white;");
		this.quit.setFont(FontLoader.loadFont("Comfortaa-Regular.ttf", "Comfortaa", 16F));
		this.quit.setPosition(700, 550);
		this.quit.setSize(130, 35);
		this.quit.setOnAction(new EventHandler<ActionEvent>() {

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

/*		this.volume = new Slider();
		this.volume.setStyle("-fx-control-inner-background: rgba(46, 47, 48, 0.5);");
		this.volume.setMin(0.0);
		this.volume.setMax(10.0);
		this.volume.setValue(LauncherMain.getMediaPlayer().getVolume());
		this.volume.setLayoutX(50);
		this.volume.setLayoutY(210);
		this.volume.setPrefWidth(395);
		this.volume.setBlockIncrement(1);

		Platform.runLater(() -> root.getChildren().add(volume));
		this.volume.setVisible(false);*/
	}
}