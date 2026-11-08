package fr.majestycraft.util;

import java.time.LocalDate;
import java.util.Random;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Habillage de Noël du launcher, en pixels comme le reste du décor : neige, terrain
 * enneigé, sapins, guirlande (voir AmbientBackground) et bonnet sur le logo.
 *
 * Il s'active et se retire tout seul d'après la date de l'ordinateur, chaque année,
 * sans aucune mise à jour du launcher.
 */
public final class ThemeNoel {

    /** Décidé une fois au démarrage : tous les écrans restent cohérents entre eux. */
    private static final boolean ACTIF = estActif(LocalDate.now());

    private static final Color BLANC = Color.web("#f2f7fd");
    private static final Color OMBRE = Color.web("#c9d9ec");

    private static final String[] BONNET = {
            "......RRRR.......",
            "....RRRRRRRR.....",
            "...RRRRRRRRRR....",
            "..RRRDDRRRRRRR...",
            "WWRRD..DRRRRRR...",
            "WWWR...DRRRRRRR..",
            ".WW....DDRRRRRR..",
            "........DRRRRRR..",
            ".....WWWWWWWWWWWW",
            ".....WWWWWWWWWWWW",
            ".....SSSSSSSSSSSS",
    };

    private static Image logoAvecBonnet;

    private ThemeNoel() {
    }

    public static boolean actif() {
        return ACTIF;
    }

    /** Du 1er décembre au 5 janvier inclus, chaque année. En 2026, dès novembre. */
    static boolean estActif(LocalDate jour) {
        int mois = jour.getMonthValue();
        if (mois == 12) return true;
        if (mois == 1) return jour.getDayOfMonth() < 6;
        return mois == 11 && jour.getYear() == 2026;
    }

    /** Le logo, coiffé d'un bonnet pendant les fêtes. Hors saison : l'image d'origine. */
    public static Image logo(Image logo) {
        if (!ACTIF || logo == null || logo.getPixelReader() == null || logo.getWidth() < 34) {
            return logo;
        }
        if (logoAvecBonnet == null) {
            int largeur = (int) logo.getWidth();
            int hauteur = (int) logo.getHeight();
            WritableImage image = new WritableImage(largeur, hauteur);
            PixelWriter pixels = image.getPixelWriter();
            PixelReader source = logo.getPixelReader();
            pixels.setPixels(0, 0, largeur, hauteur, source, 0, 0);
            poser(pixels, BONNET, (int) Math.round(largeur * 0.03), (int) Math.round(hauteur * 0.005),
                    Math.max(1, (int) Math.round(largeur * 0.027)), largeur, hauteur);
            logoAvecBonnet = image;
        }
        return logoAvecBonnet;
    }

    /** Sapin en pixels aux branches enneigées. */
    static WritableImage sapin(int etages, boolean etoile, int echelle) {
        int base = etages + 3;
        int largeur = base * 2;
        int hauteur = 6 + (etages - 1) * 3 + (etoile ? 2 : 0);
        char[][] grille = new char[hauteur][largeur];
        for (char[] ligne : grille) java.util.Arrays.fill(ligne, '.');
        int centre = base - 1;

        grille[hauteur - 1][centre] = 'B';
        grille[hauteur - 1][centre + 1] = 'B';
        grille[hauteur - 2][centre] = 'B';
        grille[hauteur - 2][centre + 1] = 'B';

        int y = hauteur - 3;
        int sommet = y;
        for (int i = 0; i < etages; i++) {
            int largeurEtage = etages - i + 3;
            for (int j = 0; j < 4; j++) {
                int demi = largeurEtage - j;
                int ligne = y - j;
                for (int x = centre - demi + 1; x <= centre + demi; x++) {
                    boolean bord = x == centre - demi + 1 || x == centre + demi;
                    boolean neige = (j >= 1 && bord) || (i == etages - 1 && j == 3);
                    grille[ligne][x] = neige ? 'W' : x <= centre ? 'G' : 'g';
                }
                sommet = ligne;
            }
            y -= 3;
        }
        if (etoile) {
            for (int ligne = sommet - 2; ligne < sommet; ligne++) {
                grille[ligne][centre] = 'Y';
                grille[ligne][centre + 1] = 'Y';
            }
        }

        String[] lignes = new String[hauteur];
        for (int i = 0; i < hauteur; i++) lignes[i] = new String(grille[i]);
        WritableImage image = new WritableImage(largeur * echelle, hauteur * echelle);
        poser(image.getPixelWriter(), lignes, 0, 0, echelle, largeur * echelle, hauteur * echelle);
        return image;
    }

    /** Couche de neige en blocs qui pend du haut d'une carte aux coins arrondis. */
    public static ImageView neigeSurCarte(int largeur, double arrondi) {
        Random hasard = new Random(11L);
        int cellule = 4;
        WritableImage image = new WritableImage(largeur, cellule * 4);
        PixelWriter pixels = image.getPixelWriter();
        int cellules = 2;
        for (int x = 0; x < largeur; x += cellule) {
            int tirage = hasard.nextInt(10);
            if (tirage < 3) cellules = Math.max(1, cellules - 1);
            else if (tirage > 6) cellules = Math.min(4, cellules + 1);
            colonneNeige(pixels, x, 0, cellule, cellules, largeur, cellule * 4, BLANC, OMBRE);
        }
        ImageView vue = new ImageView(image);
        vue.setSmooth(false);
        Rectangle coins = new Rectangle(largeur, cellule * 4 + arrondi);
        coins.setArcWidth(arrondi * 2);
        coins.setArcHeight(arrondi * 2);
        vue.setClip(coins);
        return vue;
    }

    /** Colonne de neige : blanche, avec une dernière rangée dans l'ombre. */
    static void colonneNeige(PixelWriter pixels, int x, int haut, int cellule, int cellules,
                             int largeurMax, int hauteurMax, Color neige, Color ombre) {
        for (int c = 0; c < cellules; c++) {
            Color couleur = c == cellules - 1 ? ombre : neige;
            for (int py = haut + c * cellule; py < haut + (c + 1) * cellule && py < hauteurMax; py++) {
                if (py < 0) continue;
                for (int px = x; px < x + cellule && px < largeurMax; px++) {
                    pixels.setColor(px, py, couleur);
                }
            }
        }
    }

    private static void poser(PixelWriter pixels, String[] lignes, int origineX, int origineY, int echelle,
                              int largeurMax, int hauteurMax) {
        for (int y = 0; y < lignes.length; y++) {
            for (int x = 0; x < lignes[y].length(); x++) {
                Color couleur = couleur(lignes[y].charAt(x));
                if (couleur == null) continue;
                for (int py = origineY + y * echelle; py < origineY + (y + 1) * echelle && py < hauteurMax; py++) {
                    for (int px = origineX + x * echelle; px < origineX + (x + 1) * echelle && px < largeurMax; px++) {
                        pixels.setColor(px, py, couleur);
                    }
                }
            }
        }
    }

    private static Color couleur(char code) {
        switch (code) {
            case 'W': return BLANC;
            case 'S': return OMBRE;
            case 'R': return Color.web("#d62828");
            case 'D': return Color.web("#a51d1d");
            case 'G': return Color.web("#1f6b43");
            case 'g': return Color.web("#17523a");
            case 'B': return Color.web("#5a3a22");
            case 'Y': return Color.web("#ffc83d");
            default: return null;
        }
    }
}
