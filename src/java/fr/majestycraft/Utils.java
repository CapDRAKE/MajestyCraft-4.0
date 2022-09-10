package fr.majestycraft;

import fr.trxyy.alternative.alternative_api.*;
import fr.trxyy.alternative.alternative_api.utils.config.*;

public class Utils {

    public static void regGameStyle(GameEngine engine, LauncherConfig config) {
        String version = (String) config.getValue(EnumConfig.VERSION);
        if ((boolean)(config.getValue(EnumConfig.USE_FORGE))) {
            switch (version) {
                case "1.8":
                case "1.9":
                case "1.10.2":
                case "1.11.2":
                case "1.12.2":
                    engine.setGameStyle(GameStyle.FORGE_1_8_TO_1_12_2);
                    break;
                case "1.13.2":
                case "1.14.4":
                case "1.15.2":
                case "1.16.2":
                case "1.16.3":
                case "1.16.4":
                case "1.16.5":
                    engine.setGameStyle(GameStyle.FORGE_1_13_HIGHER);
                    break;
                case "1.17":
                case "1.17.1":
                case "1.18":
                case "1.18.1":
                case "1.18.2":
                    engine.setGameStyle(GameStyle.FORGE_1_17_HIGHER);
                    break;
                case "1.19":
                case "1.19.1":
                case "1.19.2":
                    engine.setGameStyle(GameStyle.FORGE_1_19_HIGHER);
                    break;
            }
        } else if ((boolean)(config.getValue(EnumConfig.USE_OPTIFINE))) {
            engine.setGameStyle(GameStyle.OPTIFINE);
        } else {
            switch (version) {
                case "1.8":
                case "1.9":
                case "1.10.2":
                case "1.11.2":
                case "1.12.2":
                case "1.13.2":
                case "1.14.4":
                case "1.15.2":
                case "1.16.2":
                case "1.16.3":
                case "1.16.4":
                case "1.16.5":
                case "1.17":
                case "1.17.1":
                case "1.18":
                case "1.18.1":
                case "1.18.2":
                    engine.setGameStyle(GameStyle.VANILLA);
                    break;
                case "19":
                case "1.19.1":
                case "1.19.2":
                    engine.setGameStyle(GameStyle.VANILLA_1_19_HIGHER);
                    break;
            }
        }
    }
}
