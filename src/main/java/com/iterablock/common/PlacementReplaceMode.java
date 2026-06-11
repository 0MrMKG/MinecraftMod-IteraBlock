package com.iterablock.common;

public enum PlacementReplaceMode {
    REPLACE_ALL,
    ONLY_REPLACE_BLOCKS,
    ONLY_REPLACE_AIR;

    public static PlacementReplaceMode byId(int id) {
        PlacementReplaceMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : REPLACE_ALL;
    }

    public static PlacementReplaceMode fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return REPLACE_ALL;
        }

        try {
            return PlacementReplaceMode.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return REPLACE_ALL;
        }
    }

    public int id() {
        return this.ordinal();
    }
}
