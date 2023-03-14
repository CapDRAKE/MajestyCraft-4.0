package fr.majestycraft;

import club.minnced.discord.rpc.*;

public class Discord {

    private DiscordRPC rpc;

    public Discord() {
        rpc = DiscordRPC.INSTANCE;
    }

    public void start(){

        String applicationId = "805862518567469077";
        String steamId = "";

        DiscordEventHandlers handlers = new DiscordEventHandlers();
        rpc.Discord_Initialize(applicationId, handlers, true, steamId);

        DiscordRichPresence presence = new DiscordRichPresence();
        presence.startTimestamp = System.currentTimeMillis() / 1000;
        presence.largeImageKey = "image";
        presence.largeImageText = "MajestyLauncher, Launcher Gratuit Crack/Premium";
        presence.details = "Launcher MajestyLauncher";
        presence.state = "Version : 1.8 => 1.19.4";

        rpc.Discord_UpdatePresence(presence);
    }

    public void stop(){
        rpc.Discord_Shutdown();
    }
}
