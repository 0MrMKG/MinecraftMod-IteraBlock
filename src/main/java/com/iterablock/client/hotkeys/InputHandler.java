package com.iterablock.client.hotkeys;

import com.iterablock.IteraBlock;

import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;

public class InputHandler implements IKeybindProvider {
    private static final InputHandler INSTANCE = new InputHandler();
    private final KeyCallbacks callbacks = new KeyCallbacks();

    public static InputHandler getInstance() {
        return INSTANCE;
    }

    public void init() {
        Hotkeys.OPEN_MAIN_MENU.getKeybind().setCallback(this.callbacks);
        Hotkeys.OPEN_RADIAL_MENU.getKeybind().setCallback(this.callbacks);
        Hotkeys.OPEN_SETTINGS_MENU.getKeybind().setCallback(this.callbacks);
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (IHotkey hotkey : Hotkeys.HOTKEY_LIST) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(IteraBlock.MOD_NAME, "iterablock.hotkeys", Hotkeys.HOTKEY_LIST);
    }
}
