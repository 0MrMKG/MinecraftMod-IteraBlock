package com.iterablock.client.tool;

import com.iterablock.client.Lang;

public enum ToolMode {
    AREA_COPY_PASTE("iterablock.tool.mode.area_copy_paste", "iterablock.tool.mode.area_copy_paste.description", false),
    SCHEMATIC_PLACEMENT("iterablock.tool.mode.schematic_placement", "iterablock.tool.mode.schematic_placement.description", true),
    LINEAR_ARRAY("iterablock.tool.mode.linear_array", "iterablock.tool.mode.linear_array.description", true),
    VOLUME_ARRAY("iterablock.tool.mode.volume_array", "iterablock.tool.mode.volume_array.description", true),
    RANDOM_SCHEMATIC_PLACEMENT("iterablock.tool.mode.random_schematic_placement", "iterablock.tool.mode.random_schematic_placement.description", true),
    BEZIER_CURVE_GENERATION("iterablock.tool.mode.bezier_curve_generation", "iterablock.tool.mode.bezier_curve_generation.description", false);

    private final String displayNameKey;
    private final String descriptionKey;
    private final boolean usesSchematic;

    ToolMode(String displayNameKey, String descriptionKey, boolean usesSchematic) {
        this.displayNameKey = displayNameKey;
        this.descriptionKey = descriptionKey;
        this.usesSchematic = usesSchematic;
    }

    public String getDisplayName() {
        return Lang.tr(this.displayNameKey);
    }

    public String getDescription() {
        return Lang.tr(this.descriptionKey);
    }

    public boolean usesSchematic() {
        return this.usesSchematic;
    }

    public ToolMode cycle(boolean forward) {
        ToolMode[] modes = values();
        int offset = forward ? 1 : -1;
        int index = (this.ordinal() + offset + modes.length) % modes.length;
        return modes[index];
    }
}
