package fr.majestycraft;

import club.minnced.discord.rpc.*;

public class Discord {

    private static final String APPLICATION_ID = "805862518567469077";
    private static final String STEAM_ID = "";
    private static final String LARGE_IMAGE_KEY = "image";
    private DiscordRPC rpc;

    public Discord() {
        rpc = DiscordRPC.INSTANCE;
    }

    public void start() {
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        rpc.Discord_Initialize(APPLICATION_ID, handlers, true, STEAM_ID);

        DiscordRichPresence presence = new DiscordRichPresence();
        presence.startTimestamp = System.currentTimeMillis() / 1000;
        presence.largeImageKey = LARGE_IMAGE_KEY;
        presence.largeImageText = Main.bundle.getString("LARGE_IMAGE_TEXT");
        presence.details = Main.bundle.getString("DETAILS");
        presence.state = Main.bundle.getString("STATE");

        rpc.Discord_UpdatePresence(presence);
    }

    public void stop() {
        rpc.Discord_Shutdown();
    }
}
