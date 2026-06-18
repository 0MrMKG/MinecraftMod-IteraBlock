package com.iterablock.client.tool;

import com.iterablock.client.gui.GuiBuilderHelperMainMenu;
import com.iterablock.client.gui.GuiIteraBlockMainMenu;
import com.iterablock.client.gui.GuiRadialMenu;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.hotkeys.VanillaKeyMappings;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class ToolInputHandler implements IKeyboardInputHandler, IMouseInputHandler {
    private static final ToolInputHandler INSTANCE = new ToolInputHandler();

    private ToolInputHandler() {
    }

    public static ToolInputHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean onKeyInput(int keyCode, int scanCode, int modifiers, boolean eventKeyState) {
        if (!eventKeyState) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (VanillaKeyMappings.matchesOpenFiles(keyCode, scanCode)) {
            if (minecraft.screen instanceof GuiIteraBlockMainMenu) {
                minecraft.setScreen(null);
                return true;
            } else if (minecraft.screen == null) {
                GuiBase.openGui(new GuiIteraBlockMainMenu());
                return true;
            }
        }

        if (VanillaKeyMappings.matchesOpenRadial(keyCode, scanCode) || BuilderHelperClientConfig.matchesOpenRadialKey(keyCode)) {
            if (minecraft.screen == null) {
                GuiBase.openGui(new GuiRadialMenu());
                return true;
            }
        }

        if (VanillaKeyMappings.matchesOpenMainMenu(keyCode, scanCode)) {
            if (minecraft.screen instanceof GuiBuilderHelperMainMenu) {
                minecraft.setScreen(null);
                return true;
            } else if (minecraft.screen == null) {
                GuiBase.openGui(new GuiBuilderHelperMainMenu());
                return true;
            }
        }

        if (ToolState.getMode() == ToolMode.BEZIER_CURVE_GENERATION
                && VanillaKeyMappings.matchesPlaceProjection(keyCode, scanCode)
                && canHandleBezierPlacementInput(minecraft)) {
            return ToolState.placeBezierCurve(minecraft);
        }

        if (!canHandleToolInput(minecraft)) {
            return false;
        }

        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_C && ToolState.getMode() == ToolMode.SYMMETRY_PLACEMENT) {
            return ToolState.toggleSymmetryKind();
        }

        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_V && ToolState.getMode() == ToolMode.SYMMETRY_PLACEMENT) {
            return ToolState.toggleSymmetryParity();
        }

        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_C && ToolState.getMode() == ToolMode.AREA_COPY_PASTE) {
            return ToolState.copyAreaSelectionToLoaded(minecraft);
        }

        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_V && ToolState.getMode() == ToolMode.SCHEMATIC_PLACEMENT) {
            return ToolState.toggleSchematicPlacementExecutionMode();
        }

        if (minecraft.options.keyAttack.matches(keyCode, scanCode)) {
            return ToolState.handlePrimaryAction(minecraft);
        }

        if (minecraft.options.keyUse.matches(keyCode, scanCode)) {
            return ToolState.handleSecondaryAction(minecraft);
        }

        if (VanillaKeyMappings.matchesPlaceProjection(keyCode, scanCode)) {
            return ToolState.placeCurrentProjection(minecraft);
        }

        if (VanillaKeyMappings.matchesRotateProjection(keyCode, scanCode)) {
            return ToolState.rotateCurrentProjection();
        }

        if (VanillaKeyMappings.matchesMirrorProjection(keyCode, scanCode) || BuilderHelperClientConfig.matchesMirrorProjectionKey(keyCode)) {
            return ToolState.mirrorCurrentProjection(minecraft);
        }

        return false;
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int mouseButton, boolean eventButtonState) {
        if (!eventButtonState) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (!canHandleToolInput(minecraft)) {
            return false;
        }

        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && ToolState.getMode() == ToolMode.AREA_COPY_PASTE) {
            return ToolState.toggleAreaSelectionReference();
        }

        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && ToolState.getMode() == ToolMode.SYMMETRY_PLACEMENT) {
            return ToolState.toggleSymmetryLock();
        }

        if (minecraft.options.keyAttack.matchesMouse(mouseButton)) {
            return ToolState.handlePrimaryAction(minecraft);
        }

        if (minecraft.options.keyUse.matchesMouse(mouseButton)) {
            return ToolState.handleSecondaryAction(minecraft);
        }

        return false;
    }

    @Override
    public boolean onMouseScroll(int mouseX, int mouseY, double amount) {
        Minecraft minecraft = Minecraft.getInstance();

        if (amount == 0.0 || !canHandleToolInput(minecraft)) {
            return false;
        }

        if (Screen.hasControlDown()) {
            ToolState.cycleMode(amount > 0.0);
            return true;
        }

        if (ToolState.getMode() == ToolMode.AREA_COPY_PASTE) {
            return ToolState.adjustAreaSelection(minecraft, amount > 0.0 ? 1 : -1);
        }

        if (ToolState.getMode() == ToolMode.LINEAR_ARRAY) {
            if (!SchematicPlacementState.hasPlacement()) {
                return false;
            }

            ToolState.adjustLinearArray(minecraft, amount > 0.0 ? 1 : -1);
            return true;
        }

        if (ToolState.getMode() == ToolMode.VOLUME_ARRAY) {
            if (!SchematicPlacementState.hasPlacement()) {
                return false;
            }

            ToolState.adjustVolumeArray(minecraft, amount > 0.0 ? 1 : -1);
            return true;
        }

        if (ToolState.getMode() == ToolMode.SYMMETRY_PLACEMENT) {
            return ToolState.adjustSymmetryArea(minecraft, amount > 0.0 ? 1 : -1);
        }

        return false;
    }

    private static boolean canHandleToolInput(Minecraft minecraft) {
        return minecraft.screen == null
                && minecraft.level != null
                && minecraft.player != null
                && ToolState.hasToolItem(minecraft.player);
    }

    private static boolean canHandleBezierPlacementInput(Minecraft minecraft) {
        return minecraft.screen == null
                && minecraft.level != null
                && minecraft.player != null;
    }
}
