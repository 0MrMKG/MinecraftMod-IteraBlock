package com.iterablock.client.template;

import java.nio.file.Path;

import com.iterablock.client.litematica.LitematicaSchematicInfo;

public final class TemplateSelection {
    private static Path loadedPath;
    private static LitematicaSchematicInfo litematicaInfo;
    private static String parseError;

    private TemplateSelection() {
    }

    public static void load(Path path, LitematicaSchematicInfo info, String error) {
        loadedPath = path;
        litematicaInfo = info;
        parseError = error;
    }

    public static Path getLoadedPath() {
        return loadedPath;
    }

    public static LitematicaSchematicInfo getLitematicaInfo() {
        return litematicaInfo;
    }

    public static String getParseError() {
        return parseError;
    }

    public static void clearIfPath(Path path) {
        if (path != null && loadedPath != null && loadedPath.toAbsolutePath().normalize().equals(path.toAbsolutePath().normalize())) {
            clear();
        }
    }

    public static void clear() {
        loadedPath = null;
        litematicaInfo = null;
        parseError = null;
    }
}
