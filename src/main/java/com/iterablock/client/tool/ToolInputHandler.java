package com.iterablock.client.tool;

import com.iterablock.client.config.BuilderHelperClientConfig;

import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

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

        if (!canHandleToolInput(minecraft)) {
            return false;
        }

        if (minecraft.options.keyAttack.matches(keyCode, scanCode)) {
            return ToolState.handlePrimaryAction(minecraft);
        }

        if (minecraft.options.keyUse.matches(keyCode, scanCode)) {
            return ToolState.handleSecondaryAction(minecraft);
        }

        if (BuilderHelperClientConfig.matchesPlaceProjectionKey(keyCode)) {
            return ToolState.placeCurrentProjection(minecraft);
        }

        if (BuilderHelperClientConfig.matchesRotateProjectionKey(keyCode)) {
            return ToolState.rotateCurrentProjection();
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

        if (ToolState.getMode() == ToolMode.LINEAR_ARRAY) {
            ToolState.adjustLinearArray(minecraft, amount > 0.0 ? 1 : -1);
            return true;
        }

        return false;
    }

    private static boolean canHandleToolInput(Minecraft minecraft) {
        return minecraft.screen == null
                && minecraft.level != null
                && minecraft.player != null
                && ToolState.hasToolItem(minecraft.player);
    }
}
