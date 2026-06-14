package com.iterablock.client.hotkeys;

import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;

public class KeyCallbacks implements IHotkeyCallback {
    @Override
    public boolean onKeyAction(KeyAction action, IKeybind keybind) {
        return false;
    }
}
