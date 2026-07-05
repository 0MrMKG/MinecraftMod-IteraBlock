package com.iterablock.client.tool;

import com.iterablock.client.Lang;

public enum ToolMode {
    AREA_COPY_PASTE("iterablock.tool.mode.area_copy_paste", "iterablock.tool.mode.area_copy_paste.description", false),
    SCHEMATIC_PLACEMENT("iterablock.tool.mode.schematic_placement", "iterablock.tool.mode.schematic_placement.description", true),
    ARRAY_PLACEMENT("iterablock.tool.mode.array_placement", "iterablock.tool.mode.array_placement.description", true),
    RANDOM_SCHEMATIC_PLACEMENT("iterablock.tool.mode.random_schematic_placement", "iterablock.tool.mode.random_schematic_placement.description", true),
    BEZIER_CURVE_GENERATION("iterablock.tool.mode.bezier_curve_generation", "iterablock.tool.mode.bezier_curve_generation.description", false),
    RING_PLACEMENT("iterablock.tool.mode.ring_placement", "iterablock.tool.mode.ring_placement.description", true, "\u5706\u73af\u653e\u7f6e\u6a21\u5f0f", "\u56f4\u7ed5\u5706\u5fc3\u5e73\u5747\u5206\u914d\u653e\u7f6e\u5f53\u524d\u539f\u7406\u56fe"),
    SYMMETRY_PLACEMENT("iterablock.tool.mode.symmetry_placement", "iterablock.tool.mode.symmetry_placement.description", false);

    private final String displayNameKey;
    private final String descriptionKey;
    private final boolean usesSchematic;
    private final String fallbackDisplayName;
    private final String fallbackDescription;

    ToolMode(String displayNameKey, String descriptionKey, boolean usesSchematic) {
        this(displayNameKey, descriptionKey, usesSchematic, null, null);
    }

    ToolMode(String displayNameKey, String descriptionKey, boolean usesSchematic, String fallbackDisplayName, String fallbackDescription) {
        this.displayNameKey = displayNameKey;
        this.descriptionKey = descriptionKey;
        this.usesSchematic = usesSchematic;
        this.fallbackDisplayName = fallbackDisplayName;
        this.fallbackDescription = fallbackDescription;
    }

    public String getDisplayName() {
        String text = Lang.tr(this.displayNameKey);
        return this.fallbackDisplayName != null && text.equals(this.displayNameKey) ? this.fallbackDisplayName : text;
    }

    public String getDescription() {
        String text = Lang.tr(this.descriptionKey);
        return this.fallbackDescription != null && text.equals(this.descriptionKey) ? this.fallbackDescription : text;
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
