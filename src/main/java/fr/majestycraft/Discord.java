package fr.majestycraft;

import club.minnced.discord.rpc.*;

import java.util.logging.Logger;

public class Discord {

    private static final Logger LOGGER = Logger.getLogger(Discord.class.getName());
    private static final String APPLICATION_ID = "805862518567469077";
    private static final String STEAM_ID = "";
    private static final String LARGE_IMAGE_KEY = "image";
    private DiscordRPC rpc;

    /** Horodatage de départ (stable tant que la présence tourne, pour le compteur Discord). */
    private long startTimestamp;
    private boolean running = false;

    /** Derniers textes affichés (réappliqués si la présence est (re)démarrée). */
    private String currentDetails;
    private String currentState;

    public Discord() {
        try {
            rpc = DiscordRPC.INSTANCE;
        } catch (Exception | UnsatisfiedLinkError e) {
            LOGGER.warning("Discord RPC non disponible : " + e.getMessage());
            rpc = null;
        }
    }

    public void start() {
        if (rpc == null) return;
        try {
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            rpc.Discord_Initialize(APPLICATION_ID, handlers, true, STEAM_ID);

            this.startTimestamp = System.currentTimeMillis() / 1000;
            this.running = true;

            if (currentDetails == null) currentDetails = Main.bundle.getString("DETAILS");
            if (currentState == null) currentState = Main.bundle.getString("STATE");

            applyPresence();
        } catch (Exception | UnsatisfiedLinkError e) {
            LOGGER.warning("Erreur Discord RPC start : " + e.getMessage());
        }
    }

    /**
     * Met à jour la présence Discord (par ex. selon le profil actif ou l'état en jeu).
     * Sans effet si Discord n'est pas activé/disponible.
     */
    public void updatePresence(String details, String state) {
        this.currentDetails = details;
        this.currentState = state;
        applyPresence();
    }

    private void applyPresence() {
        if (rpc == null || !running) return;
        try {
            DiscordRichPresence presence = new DiscordRichPresence();
            presence.startTimestamp = this.startTimestamp;
            presence.largeImageKey = LARGE_IMAGE_KEY;
            presence.largeImageText = Main.bundle.getString("LARGE_IMAGE_TEXT");
            presence.details = currentDetails;
            presence.state = currentState;
            rpc.Discord_UpdatePresence(presence);
        } catch (Exception | UnsatisfiedLinkError e) {
            LOGGER.warning("Erreur Discord RPC update : " + e.getMessage());
        }
    }

    public void stop() {
        if (rpc == null) return;
        try {
            this.running = false;
            rpc.Discord_Shutdown();
        } catch (Exception | UnsatisfiedLinkError e) {
            LOGGER.warning("Erreur Discord RPC stop : " + e.getMessage());
        }
    }
}
