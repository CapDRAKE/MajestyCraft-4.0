package fr.majestycraft;

import fr.trxyy.alternative.alternative_api.GameEngine;
import fr.trxyy.alternative.alternative_api.GameStyle;
import fr.trxyy.alternative.alternative_api.utils.config.EnumConfig;
import fr.trxyy.alternative.alternative_api.utils.config.LauncherConfig;

public class Utils {

    public enum ModloaderType {
        VANILLA,
        FORGE,
        FABRIC,
        QUILT,
        NEOFORGE,
        OPTIFINE
    }

    public static void regGameStyle(GameEngine engine, LauncherConfig config) {
        String version = config == null ? null : stringValue(config.getValue(EnumConfig.VERSION));
        engine.setGameStyle(determineGameStyle(version, resolveSelectedModloader(config)));
    }

    public static ModloaderType resolveSelectedModloader(LauncherConfig config) {
        if (config == null) {
            return ModloaderType.VANILLA;
        }

        return resolveSelectedModloader(
                booleanValue(config.getValue(EnumConfig.USE_FORGE)),
                booleanValue(config.getValue(EnumConfig.USE_FABRIC)),
                booleanValue(config.getValue(EnumConfig.USE_QUILT)),
                booleanValue(config.getValue(EnumConfig.USE_NEOFORGE)),
                booleanValue(config.getValue(EnumConfig.USE_OPTIFINE))
        );
    }

    public static ModloaderType resolveSelectedModloader(
            boolean useForge,
            boolean useFabric,
            boolean useQuilt,
            boolean useNeoForge,
            boolean useOptifine
    ) {
        if (useOptifine) {
            return ModloaderType.OPTIFINE;
        }
        if (useNeoForge) {
            return ModloaderType.NEOFORGE;
        }
        if (useQuilt) {
            return ModloaderType.QUILT;
        }
        if (useFabric) {
            return ModloaderType.FABRIC;
        }
        if (useForge) {
            return ModloaderType.FORGE;
        }
        return ModloaderType.VANILLA;
    }

    public static boolean usesCustomServerJson(ModloaderType modloaderType) {
        return modloaderType == ModloaderType.FABRIC
                || modloaderType == ModloaderType.QUILT
                || modloaderType == ModloaderType.NEOFORGE;
    }

    public static String resolveServerPath(String version, ModloaderType modloaderType) {
        String suffix = "/";
        if (modloaderType == ModloaderType.FORGE) {
            suffix = "/forge/";
        } else if (modloaderType == ModloaderType.FABRIC) {
            suffix = "/fabric/";
        } else if (modloaderType == ModloaderType.QUILT) {
            suffix = "/quilt/";
        } else if (modloaderType == ModloaderType.NEOFORGE) {
            suffix = "/neoforge/";
        }
        return "/" + version + suffix;
    }

    private static GameStyle determineGameStyle(String version, ModloaderType modloaderType) {
        if (modloaderType == ModloaderType.FORGE) {
            if (version != null && version.matches("1\\.(8|9|10\\.2|11\\.2|12\\.2)")) {
                return GameStyle.FORGE_1_8_TO_1_12_2;
            } else if (version != null && version.matches("1\\.(13\\.2|14\\.4|15\\.2|16\\.(\\d+)?)")) {
                return GameStyle.FORGE_1_13_HIGHER;
            } else if (version != null && version.matches("1\\.(17|17\\.1|18|18\\.1|18\\.2)")) {
                return GameStyle.FORGE_1_17_HIGHER;
            } else if (version != null && version.matches("1\\.(19|20|21)(\\.\\d+)?")) {
                return GameStyle.FORGE_1_19_HIGHER;
            }
        } else if (modloaderType == ModloaderType.FABRIC) {
            return GameStyle.FABRIC;
        } else if (modloaderType == ModloaderType.QUILT) {
            return GameStyle.QUILT;
        } else if (modloaderType == ModloaderType.NEOFORGE) {
            return GameStyle.NEOFORGE;
        } else if (modloaderType == ModloaderType.OPTIFINE) {
            return GameStyle.OPTIFINE;
        } else if (version != null && version.matches("1\\.(19|20|21)(\\.\\d+)?")) {
            return GameStyle.VANILLA_1_19_HIGHER;
        }

        return GameStyle.VANILLA;
    }

    public static boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean(((String) value).trim());
        }
        return false;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
