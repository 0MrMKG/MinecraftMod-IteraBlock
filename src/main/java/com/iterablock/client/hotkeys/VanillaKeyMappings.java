package com.iterablock.client.hotkeys;

import com.iterablock.client.config.BuilderHelperClientConfig;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class VanillaKeyMappings {
    public static final String CATEGORY = "key.categories.iterablock";

    public static final KeyMapping OPEN_FILES = new KeyMapping("key.iterablock.open_files", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, CATEGORY);
    public static final KeyMapping OPEN_RADIAL = new KeyMapping("key.iterablock.open_radial", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY);
    public static final KeyMapping OPEN_MAIN_MENU = new KeyMapping("key.iterablock.open_main_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY);
    public static final KeyMapping PLACE_PROJECTION = new KeyMapping("key.iterablock.place_projection", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY);
    public static final KeyMapping ROTATE_PROJECTION = new KeyMapping("key.iterablock.rotate_projection", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping MIRROR_PROJECTION = new KeyMapping("key.iterablock.mirror_projection", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    private VanillaKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_FILES);
        event.register(OPEN_RADIAL);
        event.register(OPEN_MAIN_MENU);
        event.register(PLACE_PROJECTION);
        event.register(ROTATE_PROJECTION);
        event.register(MIRROR_PROJECTION);
    }

    public static boolean matchesOpenFiles(int keyCode, int scanCode) {
        return OPEN_FILES.matches(keyCode, scanCode);
    }

    public static boolean matchesOpenRadial(int keyCode, int scanCode) {
        return OPEN_RADIAL.matches(keyCode, scanCode);
    }

    public static boolean matchesOpenMainMenu(int keyCode, int scanCode) {
        return OPEN_MAIN_MENU.matches(keyCode, scanCode);
    }

    public static boolean matchesPlaceProjection(int keyCode, int scanCode) {
        return PLACE_PROJECTION.matches(keyCode, scanCode);
    }

    public static boolean matchesRotateProjection(int keyCode, int scanCode) {
        return ROTATE_PROJECTION.matches(keyCode, scanCode);
    }

    public static boolean matchesMirrorProjection(int keyCode, int scanCode) {
        return MIRROR_PROJECTION.matches(keyCode, scanCode);
    }

    public static boolean isOpenRadialDown() {
        return OPEN_RADIAL.isDown();
    }

    public static String getKeyName(String key, String fallback) {
        KeyMapping mapping = getMapping(key);
        return mapping == null ? fallback : BuilderHelperClientConfig.keyNameFromCode(mapping.getKey().getValue());
    }

    public static void setKey(String key, int keyCode) {
        KeyMapping mapping = getMapping(key);
        if (mapping == null) {
            return;
        }

        mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    public static void resetKey(String key) {
        switch (key) {
            case "openFilesKey" -> setKey(key, GLFW.GLFW_KEY_I);
            case "openRadialKey" -> setKey(key, GLFW.GLFW_KEY_U);
            case "openMainMenuKey" -> setKey(key, GLFW.GLFW_KEY_O);
            case "placeProjectionKey" -> setKey(key, GLFW.GLFW_KEY_Y);
            case "rotateProjectionKey" -> setKey(key, GLFW.GLFW_KEY_R);
            case "mirrorProjectionKey" -> setKey(key, GLFW.GLFW_KEY_G);
            default -> {
            }
        }
    }

    private static KeyMapping getMapping(String key) {
        return switch (key) {
            case "openFilesKey" -> OPEN_FILES;
            case "openRadialKey" -> OPEN_RADIAL;
            case "openMainMenuKey" -> OPEN_MAIN_MENU;
            case "placeProjectionKey" -> PLACE_PROJECTION;
            case "rotateProjectionKey" -> ROTATE_PROJECTION;
            case "mirrorProjectionKey" -> MIRROR_PROJECTION;
            default -> null;
        };
    }
}
