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
    public static final int DEFAULT_VOLUME_ARRAY_RENDER_LIMIT = 3;
    public static final int DEFAULT_RANDOM_PLACEMENT_RADIUS = 16;
    public static final int DEFAULT_RANDOM_PLACEMENT_HEIGHT_MIN = 0;
    public static final int DEFAULT_RANDOM_PLACEMENT_HEIGHT_MAX = 0;
    public static final int DEFAULT_RANDOM_PLACEMENT_COUNT = 8;
    public static final int DEFAULT_RANDOM_PLACEMENT_ROTATION_CHANCE = 100;
    public static final int DEFAULT_BEZIER_PLACEMENT_PRECISION = 8;
    public static final int DEFAULT_BEZIER_PLACEMENT_WIDTH = 1;
    public static final boolean DEFAULT_BEZIER_PLACE_NBT_MODE = false;
    private static final int MIN_PLACEMENT_RANGE = 1;
    private static final int MAX_PLACEMENT_RANGE = 512;
    private static final int MIN_LINEAR_ARRAY_RENDER_LIMIT = 1;
    private static final int MAX_LINEAR_ARRAY_RENDER_LIMIT = 32;
    private static final int MIN_VOLUME_ARRAY_RENDER_LIMIT = 1;
    private static final int MAX_VOLUME_ARRAY_RENDER_LIMIT = 5;
    private static final int MIN_RANDOM_PLACEMENT_RADIUS = 1;
    private static final int MAX_RANDOM_PLACEMENT_RADIUS = 512;
    private static final int MIN_RANDOM_PLACEMENT_HEIGHT = -384;
    private static final int MAX_RANDOM_PLACEMENT_HEIGHT = 384;
    private static final int MIN_RANDOM_PLACEMENT_COUNT = 1;
    private static final int MAX_RANDOM_PLACEMENT_COUNT = 4096;
    private static final int MIN_RANDOM_PLACEMENT_ROTATION_CHANCE = 0;
    private static final int MAX_RANDOM_PLACEMENT_ROTATION_CHANCE = 100;
    private static final int MIN_BEZIER_PLACEMENT_PRECISION = 1;
    private static final int MAX_BEZIER_PLACEMENT_PRECISION = 128;
    private static final int MIN_BEZIER_PLACEMENT_WIDTH = 1;
    private static final int MAX_BEZIER_PLACEMENT_WIDTH = 32;
    private static final String PLACEMENT_RANGE_KEY = "placementRange";
    private static final String LINEAR_ARRAY_RENDER_LIMIT_KEY = "linearArrayRenderLimit";
    private static final String VOLUME_ARRAY_RENDER_LIMIT_KEY = "volumeArrayRenderLimit";
    private static final String LITEMATIC_PATH_KEY = "litematicPath";
    private static final String OPEN_FILES_KEY = "openFilesKey";
    private static final String OPEN_RADIAL_KEY = "openRadialKey";
    private static final String OPEN_MAIN_MENU_KEY = "openMainMenuKey";
    private static final String PLACE_PROJECTION_KEY = "placeProjectionKey";
    private static final String ROTATE_PROJECTION_KEY = "rotateProjectionKey";
    private static final String PLACEMENT_REPLACE_MODE_KEY = "placementReplaceMode";
    private static final String RANDOM_PLACEMENT_ROTATION_KEY = "randomPlacementRotation";
    private static final String RANDOM_PLACEMENT_RADIUS_KEY = "randomPlacementRadius";
    private static final String RANDOM_PLACEMENT_HEIGHT_MIN_KEY = "randomPlacementHeightMin";
    private static final String RANDOM_PLACEMENT_HEIGHT_MAX_KEY = "randomPlacementHeightMax";
    private static final String RANDOM_PLACEMENT_COUNT_KEY = "randomPlacementCount";
    private static final String RANDOM_PLACEMENT_ROTATION_CHANCE_KEY = "randomPlacementRotationChance";
    private static final String BEZIER_PLACEMENT_PRECISION_KEY = "bezierPlacementPrecision";
    private static final String BEZIER_PLACEMENT_WIDTH_KEY = "bezierPlacementWidth";
    private static final String BEZIER_PLACE_NBT_MODE_KEY = "bezierPlaceNbtMode";

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

    public static int getVolumeArrayRenderLimit() {
        Properties properties = loadProperties();
        return clamp(parseInt(properties.getProperty(VOLUME_ARRAY_RENDER_LIMIT_KEY), DEFAULT_VOLUME_ARRAY_RENDER_LIMIT), MIN_VOLUME_ARRAY_RENDER_LIMIT, MAX_VOLUME_ARRAY_RENDER_LIMIT);
    }

    public static int getRandomPlacementRadius() {
        return getIntValue(RANDOM_PLACEMENT_RADIUS_KEY, DEFAULT_RANDOM_PLACEMENT_RADIUS, MIN_RANDOM_PLACEMENT_RADIUS, MAX_RANDOM_PLACEMENT_RADIUS);
    }

    public static int getRandomPlacementHeightMin() {
        return getIntValue(RANDOM_PLACEMENT_HEIGHT_MIN_KEY, DEFAULT_RANDOM_PLACEMENT_HEIGHT_MIN, MIN_RANDOM_PLACEMENT_HEIGHT, MAX_RANDOM_PLACEMENT_HEIGHT);
    }

    public static int getRandomPlacementHeightMax() {
        return getIntValue(RANDOM_PLACEMENT_HEIGHT_MAX_KEY, DEFAULT_RANDOM_PLACEMENT_HEIGHT_MAX, MIN_RANDOM_PLACEMENT_HEIGHT, MAX_RANDOM_PLACEMENT_HEIGHT);
    }

    public static int getRandomPlacementCount() {
        return getIntValue(RANDOM_PLACEMENT_COUNT_KEY, DEFAULT_RANDOM_PLACEMENT_COUNT, MIN_RANDOM_PLACEMENT_COUNT, MAX_RANDOM_PLACEMENT_COUNT);
    }

    public static int getRandomPlacementRotationChance() {
        return getIntValue(RANDOM_PLACEMENT_ROTATION_CHANCE_KEY, DEFAULT_RANDOM_PLACEMENT_ROTATION_CHANCE, MIN_RANDOM_PLACEMENT_ROTATION_CHANCE, MAX_RANDOM_PLACEMENT_ROTATION_CHANCE);
    }

    public static int getBezierPlacementPrecision() {
        return getIntValue(BEZIER_PLACEMENT_PRECISION_KEY, DEFAULT_BEZIER_PLACEMENT_PRECISION, MIN_BEZIER_PLACEMENT_PRECISION, MAX_BEZIER_PLACEMENT_PRECISION);
    }

    public static int getBezierPlacementWidth() {
        return getIntValue(BEZIER_PLACEMENT_WIDTH_KEY, DEFAULT_BEZIER_PLACEMENT_WIDTH, MIN_BEZIER_PLACEMENT_WIDTH, MAX_BEZIER_PLACEMENT_WIDTH);
    }

    public static boolean isBezierPlaceNbtMode() {
        return Boolean.parseBoolean(loadProperties().getProperty(BEZIER_PLACE_NBT_MODE_KEY, Boolean.toString(DEFAULT_BEZIER_PLACE_NBT_MODE)));
    }

    public static void setRandomPlacementRadius(int value) {
        setIntValue(RANDOM_PLACEMENT_RADIUS_KEY, value, MIN_RANDOM_PLACEMENT_RADIUS, MAX_RANDOM_PLACEMENT_RADIUS);
    }

    public static void setRandomPlacementHeightMin(int value) {
        setIntValue(RANDOM_PLACEMENT_HEIGHT_MIN_KEY, value, MIN_RANDOM_PLACEMENT_HEIGHT, MAX_RANDOM_PLACEMENT_HEIGHT);
    }

    public static void setRandomPlacementHeightMax(int value) {
        setIntValue(RANDOM_PLACEMENT_HEIGHT_MAX_KEY, value, MIN_RANDOM_PLACEMENT_HEIGHT, MAX_RANDOM_PLACEMENT_HEIGHT);
    }

    public static void setRandomPlacementCount(int value) {
        setIntValue(RANDOM_PLACEMENT_COUNT_KEY, value, MIN_RANDOM_PLACEMENT_COUNT, MAX_RANDOM_PLACEMENT_COUNT);
    }

    public static void setRandomPlacementRotationChance(int value) {
        setIntValue(RANDOM_PLACEMENT_ROTATION_CHANCE_KEY, value, MIN_RANDOM_PLACEMENT_ROTATION_CHANCE, MAX_RANDOM_PLACEMENT_ROTATION_CHANCE);
    }

    public static void setBezierPlacementPrecision(int value) {
        setIntValue(BEZIER_PLACEMENT_PRECISION_KEY, value, MIN_BEZIER_PLACEMENT_PRECISION, MAX_BEZIER_PLACEMENT_PRECISION);
    }

    public static void setBezierPlacementWidth(int value) {
        setIntValue(BEZIER_PLACEMENT_WIDTH_KEY, value, MIN_BEZIER_PLACEMENT_WIDTH, MAX_BEZIER_PLACEMENT_WIDTH);
    }

    public static void setBezierPlaceNbtMode(boolean enabled) {
        Properties properties = loadProperties();
        properties.setProperty(BEZIER_PLACE_NBT_MODE_KEY, Boolean.toString(enabled));
        saveProperties(properties);
    }

    public static Path getDefaultLitematicPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("schematics").toAbsolutePath().normalize();
    }

    public static Path getLitematicPath() {
        String value = loadProperties().getProperty(LITEMATIC_PATH_KEY);

        if (value == null || value.isBlank()) {
            return getDefaultLitematicPath();
        }

        Path path = Path.of(value.trim());
        return (path.isAbsolute() ? path : getDefaultLitematicPath()).toAbsolutePath().normalize();
    }

    public static boolean matchesPlaceProjectionKey(int keyCode) {
        return keyCode == getKeyCode(PLACE_PROJECTION_KEY, GLFW.GLFW_KEY_Y);
    }

    public static boolean matchesRotateProjectionKey(int keyCode) {
        return keyCode == getKeyCode(ROTATE_PROJECTION_KEY, GLFW.GLFW_KEY_R);
    }

    public static boolean matchesOpenFilesKey(int keyCode) {
        return keyCode == getKeyCode(OPEN_FILES_KEY, GLFW.GLFW_KEY_I);
    }

    public static boolean matchesOpenRadialKey(int keyCode) {
        return keyCode == getKeyCode(OPEN_RADIAL_KEY, GLFW.GLFW_KEY_U);
    }

    public static boolean matchesOpenMainMenuKey(int keyCode) {
        return keyCode == getKeyCode(OPEN_MAIN_MENU_KEY, GLFW.GLFW_KEY_O);
    }

    public static String getOpenFilesKeyName() {
        return getKeyName(OPEN_FILES_KEY, "I");
    }

    public static String getOpenRadialKeyName() {
        return getKeyName(OPEN_RADIAL_KEY, "U");
    }

    public static String getOpenMainMenuKeyName() {
        return getKeyName(OPEN_MAIN_MENU_KEY, "O");
    }

    public static boolean isOpenRadialKeyDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, getKeyCode(OPEN_RADIAL_KEY, GLFW.GLFW_KEY_U)) == GLFW.GLFW_PRESS;
    }

    public static String getKeyName(String key, String fallback) {
        return normalizeKeyName(loadProperties().getProperty(key), fallback);
    }

    public static String keyNameFromCode(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return Character.toString((char) ('A' + keyCode - GLFW.GLFW_KEY_A));
        }

        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return Character.toString((char) ('0' + keyCode - GLFW.GLFW_KEY_0));
        }

        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F12) {
            return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        }

        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_KP_ENTER -> "KP_ENTER";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LEFT_SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RIGHT_SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LEFT_CONTROL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RIGHT_CONTROL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LEFT_ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RIGHT_ALT";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_DELETE -> "DELETE";
            case GLFW.GLFW_KEY_INSERT -> "INSERT";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PAGE_UP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PAGE_DOWN";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            default -> "KEY_" + keyCode;
        };
    }

    public static PlacementReplaceMode getPlacementReplaceMode() {
        return PlacementReplaceMode.fromConfig(loadProperties().getProperty(PLACEMENT_REPLACE_MODE_KEY));
    }

    public static void setPlacementReplaceMode(PlacementReplaceMode mode) {
        Properties properties = loadProperties();
        properties.setProperty(PLACEMENT_REPLACE_MODE_KEY, mode.name());
        saveProperties(properties);
    }

    public static boolean isRandomPlacementRotationEnabled() {
        return Boolean.parseBoolean(loadProperties().getProperty(RANDOM_PLACEMENT_ROTATION_KEY, "true"));
    }

    public static void toggleRandomPlacementRotation() {
        Properties properties = loadProperties();
        boolean enabled = Boolean.parseBoolean(properties.getProperty(RANDOM_PLACEMENT_ROTATION_KEY, "true"));
        properties.setProperty(RANDOM_PLACEMENT_ROTATION_KEY, Boolean.toString(!enabled));
        saveProperties(properties);
    }

    private static int getIntValue(String key, int fallback, int min, int max) {
        return clamp(parseInt(loadProperties().getProperty(key), fallback), min, max);
    }

    private static void setIntValue(String key, int value, int min, int max) {
        Properties properties = loadProperties();
        properties.setProperty(key, Integer.toString(clamp(value, min, max)));
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

        if (key.startsWith("KEY_")) {
            return parseInt(key.substring(4), fallback);
        }

        return switch (key) {
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "KP_ENTER" -> GLFW.GLFW_KEY_KP_ENTER;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "LEFT_SHIFT", "SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RIGHT_SHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LEFT_CONTROL", "CTRL", "CONTROL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RIGHT_CONTROL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LEFT_ALT", "ALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RIGHT_ALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
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

    private static String normalizeKeyName(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String key = value.trim().toUpperCase(Locale.ROOT);
        int keyCode = parseKeyCode(key, Integer.MIN_VALUE);
        return keyCode == Integer.MIN_VALUE ? fallback : keyNameFromCode(keyCode);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
