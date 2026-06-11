package com.iterablock.client.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Locale;

import com.iterablock.common.PlacementReplaceMode;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class BuilderHelperClientConfig {
    public static final int DEFAULT_PLACEMENT_RANGE = 100;
    public static final int DEFAULT_LINEAR_ARRAY_RENDER_LIMIT = 5;
    private static final int MIN_PLACEMENT_RANGE = 1;
    private static final int MAX_PLACEMENT_RANGE = 512;
    private static final int MIN_LINEAR_ARRAY_RENDER_LIMIT = 1;
    private static final int MAX_LINEAR_ARRAY_RENDER_LIMIT = 32;
    private static final String PLACEMENT_RANGE_KEY = "placementRange";
    private static final String LINEAR_ARRAY_RENDER_LIMIT_KEY = "linearArrayRenderLimit";
    private static final String PLACE_PROJECTION_KEY = "placeProjectionKey";
    private static final String ROTATE_PROJECTION_KEY = "rotateProjectionKey";
    private static final String PLACEMENT_REPLACE_MODE_KEY = "placementReplaceMode";

    private BuilderHelperClientConfig() {
    }

    public static int getPlacementRange() {
        Properties properties = loadProperties();
        return clamp(parseInt(properties.getProperty(PLACEMENT_RANGE_KEY), DEFAULT_PLACEMENT_RANGE), MIN_PLACEMENT_RANGE, MAX_PLACEMENT_RANGE);
    }

    public static int getLinearArrayRenderLimit() {
        Properties properties = loadProperties();
        return clamp(parseInt(properties.getProperty(LINEAR_ARRAY_RENDER_LIMIT_KEY), DEFAULT_LINEAR_ARRAY_RENDER_LIMIT), MIN_LINEAR_ARRAY_RENDER_LIMIT, MAX_LINEAR_ARRAY_RENDER_LIMIT);
    }

    public static boolean matchesPlaceProjectionKey(int keyCode) {
        return keyCode == getKeyCode(PLACE_PROJECTION_KEY, GLFW.GLFW_KEY_Y);
    }

    public static boolean matchesRotateProjectionKey(int keyCode) {
        return keyCode == getKeyCode(ROTATE_PROJECTION_KEY, GLFW.GLFW_KEY_R);
    }

    public static PlacementReplaceMode getPlacementReplaceMode() {
        return PlacementReplaceMode.fromConfig(loadProperties().getProperty(PLACEMENT_REPLACE_MODE_KEY));
    }

    public static void setPlacementReplaceMode(PlacementReplaceMode mode) {
        Properties properties = loadProperties();
        properties.setProperty(PLACEMENT_REPLACE_MODE_KEY, mode.name());
        saveProperties(properties);
    }

    private static int getKeyCode(String key, int fallback) {
        return parseKeyCode(loadProperties().getProperty(key), fallback);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        Path path = getConfigPath();

        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException ignored) {
            }
        }

        return properties;
    }

    private static Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("builderhelper-client.properties");
    }

    private static void saveProperties(Properties properties) {
        Path path = getConfigPath();

        try {
            Files.createDirectories(path.getParent());
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "BuilderHelper client settings");
            }
        } catch (IOException ignored) {
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseKeyCode(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String key = value.trim().toUpperCase(Locale.ROOT);

        if (key.length() == 1) {
            char c = key.charAt(0);

            if (c >= 'A' && c <= 'Z') {
                return GLFW.GLFW_KEY_A + c - 'A';
            }

            if (c >= '0' && c <= '9') {
                return GLFW.GLFW_KEY_0 + c - '0';
            }
        }

        if (key.startsWith("F")) {
            int functionKey = parseInt(key.substring(1), -1);

            if (functionKey >= 1 && functionKey <= 12) {
                return GLFW.GLFW_KEY_F1 + functionKey - 1;
            }
        }

        return switch (key) {
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "LEFT_SHIFT", "SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "LEFT_CONTROL", "CTRL", "CONTROL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "LEFT_ALT", "ALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "DELETE" -> GLFW.GLFW_KEY_DELETE;
            case "INSERT" -> GLFW.GLFW_KEY_INSERT;
            case "HOME" -> GLFW.GLFW_KEY_HOME;
            case "END" -> GLFW.GLFW_KEY_END;
            case "PAGE_UP" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PAGE_DOWN" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            default -> fallback;
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
