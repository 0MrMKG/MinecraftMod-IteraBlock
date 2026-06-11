package com.iterablock.client;

import net.minecraft.client.resources.language.I18n;

public final class Lang {
    private Lang() {
    }

    public static String tr(String key, Object... args) {
        return I18n.get(key, args);
    }
}
