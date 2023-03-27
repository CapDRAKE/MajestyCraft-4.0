package fr.majestycraft;

import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.utils.config.*;

public class Utils {

    public static void regGameStyle(GameEngine engine, LauncherConfig config) {
        String version = (String) config.getValue(EnumConfig.VERSION);
        boolean useForge = (boolean) (config.getValue(EnumConfig.USE_FORGE));
        boolean useOptifine = (boolean) (config.getValue(EnumConfig.USE_OPTIFINE));

        engine.setGameStyle(determineGameStyle(version, useForge, useOptifine));
    }

    private static GameStyle determineGameStyle(String version, boolean useForge, boolean useOptifine) {
        if (useForge) {
            if (version.matches("1\\.(8|9|10\\.2|11\\.2|12\\.2)")) {
                return GameStyle.FORGE_1_8_TO_1_12_2;
            } else if (version.matches("1\\.(13\\.2|14\\.4|15\\.2|16\\.(2|3|4|5))")) {
                return GameStyle.FORGE_1_13_HIGHER;
            } else if (version.matches("1\\.(17|17\\.1|18|18\\.1|18\\.2)")) {
                return GameStyle.FORGE_1_17_HIGHER;
            } else if (version.matches("1\\.19(\\.\\d+)?")) {
                return GameStyle.FORGE_1_19_HIGHER;
            }
        } else if (useOptifine) {
            return GameStyle.OPTIFINE;
        } else if (version.matches("1\\.19(\\.\\d+)?")) {
            return GameStyle.VANILLA_1_19_HIGHER;
        }

        return GameStyle.VANILLA;
    }
}