package com.iterablock.client.tool;

import com.iterablock.client.template.LoadedLitematicManager;

public final class ClientToolState {
    public static LoadedLitematicManager.Entry currentLitematic;

    private ClientToolState() {
    }

    public static void setCurrentLitematic(LoadedLitematicManager.Entry entry) {
        currentLitematic = entry;
    }
}
