package com.iterablock.client.hotkeys;

import java.util.List;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkey;

public class Hotkeys {
    public static final ConfigHotkey OPEN_MAIN_MENU = new ConfigHotkey("openMainMenu", "I", "Open IteraBlock main menu");
    public static final ConfigHotkey OPEN_RADIAL_MENU = new ConfigHotkey("openRadialMenu", "U", "Open IteraBlock radial menu");
    public static final ConfigHotkey OPEN_SETTINGS_MENU = new ConfigHotkey("openSettingsMenu", "O", "Open BuilderHelper main menu");
    public static final List<IHotkey> HOTKEY_LIST = List.of(OPEN_MAIN_MENU, OPEN_RADIAL_MENU, OPEN_SETTINGS_MENU);

    private Hotkeys() {
    }
}
