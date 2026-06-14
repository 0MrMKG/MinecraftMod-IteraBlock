package com.iterablock.common;

public enum PlacementReplaceMode {
    REPLACE_ALL,
    ONLY_REPLACE_AIR;

    public static PlacementReplaceMode byId(int id) {
        PlacementReplaceMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : ONLY_REPLACE_AIR;
    }

    public static PlacementReplaceMode fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return ONLY_REPLACE_AIR;
        }

        if ("ONLY_REPLACE_BLOCKS".equalsIgnoreCase(value.trim())) {
            return ONLY_REPLACE_AIR;
        }

        try {
            return PlacementReplaceMode.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ONLY_REPLACE_AIR;
        }
    }

    public PlacementReplaceMode next() {
        return switch (this) {
            case ONLY_REPLACE_AIR -> REPLACE_ALL;
            case REPLACE_ALL -> ONLY_REPLACE_AIR;
        };
    }

    public int id() {
        return this.ordinal();
    }
}
