package fr.majestycraft;

import club.minnced.discord.rpc.*;

import java.util.logging.Logger;

public class Discord {

    private static final Logger LOGGER = Logger.getLogger(Discord.class.getName());
    private static final String APPLICATION_ID = "805862518567469077";
    private static final String STEAM_ID = "";
    private static final String LARGE_IMAGE_KEY = "image";
    private DiscordRPC rpc;

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

            DiscordRichPresence presence = new DiscordRichPresence();
            presence.startTimestamp = System.currentTimeMillis() / 1000;
            presence.largeImageKey = LARGE_IMAGE_KEY;
            presence.largeImageText = Main.bundle.getString("LARGE_IMAGE_TEXT");
            presence.details = Main.bundle.getString("DETAILS");
            presence.state = Main.bundle.getString("STATE");

            rpc.Discord_UpdatePresence(presence);
        } catch (Exception | UnsatisfiedLinkError e) {
            LOGGER.warning("Erreur Discord RPC start : " + e.getMessage());
        }
    }

    public void stop() {
        if (rpc == null) return;
        try {
            rpc.Discord_Shutdown();
        } catch (Exception | UnsatisfiedLinkError e) {
            LOGGER.warning("Erreur Discord RPC stop : " + e.getMessage());
        }
    }
}
