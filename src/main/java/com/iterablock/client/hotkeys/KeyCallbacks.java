package com.iterablock.client.hotkeys;

import com.iterablock.client.gui.GuiBuilderHelperMainMenu;
import com.iterablock.client.gui.GuiIteraBlockMainMenu;
import com.iterablock.client.gui.GuiRadialMenu;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.Minecraft;

public class KeyCallbacks implements IHotkeyCallback {
    @Override
    public boolean onKeyAction(KeyAction action, IKeybind keybind) {
        if (action == KeyAction.PRESS && keybind == Hotkeys.OPEN_MAIN_MENU.getKeybind()) {
            GuiBase.openGui(new GuiIteraBlockMainMenu());
            return true;
        }

        if (action == KeyAction.PRESS && keybind == Hotkeys.OPEN_RADIAL_MENU.getKeybind()) {
            if (!(Minecraft.getInstance().screen instanceof GuiRadialMenu)) {
                GuiBase.openGui(new GuiRadialMenu());
            }

            return true;
        }

        if (action == KeyAction.PRESS && keybind == Hotkeys.OPEN_SETTINGS_MENU.getKeybind()) {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.screen instanceof GuiBuilderHelperMainMenu) {
                minecraft.setScreen(null);
            } else {
                GuiBase.openGui(new GuiBuilderHelperMainMenu());
            }

            return true;
        }

        return false;
    }
}
