package com.iterablock.client.template;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.iterablock.client.litematica.LitematicaSchematicInfo;
import com.iterablock.client.tool.ClientToolState;
import com.iterablock.client.tool.SchematicProjectionRenderer;
import com.iterablock.client.tool.SchematicPlacementState;

public final class LoadedLitematicManager {
    public static final int MAX_LOADED = 6;
    private static final List<Entry> ENTRIES = new ArrayList<>();
    public static Entry selectedEntry;

    private LoadedLitematicManager() {
    }

    public static List<Entry> getEntries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static Entry load(Path path, LitematicaSchematicInfo info) {
        if (path == null || info == null) {
            return null;
        }

        Path normalizedPath = path.toAbsolutePath().normalize();

        for (Entry entry : ENTRIES) {
            if (entry.path().equals(normalizedPath)) {
                select(entry);
                return entry;
            }
        }

        if (ENTRIES.size() >= MAX_LOADED) {
            return null;
        }

        Entry entry = new Entry(normalizedPath, info);
        ENTRIES.add(entry);
        select(entry);
        return entry;
    }

    public static void select(Entry entry) {
        selectedEntry = ENTRIES.contains(entry) ? entry : null;
        ClientToolState.setCurrentLitematic(selectedEntry);
    }

    public static void unload(Entry entry) {
        if (entry == null) {
            return;
        }

        int index = ENTRIES.indexOf(entry);

        if (index < 0) {
            return;
        }

        boolean wasSelected = entry == selectedEntry;
        ENTRIES.remove(index);
        TemplateSelection.clearIfPath(entry.path());
        SchematicPlacementState.clearIfEntry(entry);
        SchematicProjectionRenderer.getInstance().clearCache();

        if (ENTRIES.isEmpty()) {
            selectedEntry = null;
        } else if (wasSelected) {
            selectedEntry = ENTRIES.get(Math.min(index, ENTRIES.size() - 1));
        }

        if (wasSelected || selectedEntry == null) {
            ClientToolState.setCurrentLitematic(selectedEntry);
        }
    }

    public static void clearAll() {
        ENTRIES.clear();
        selectedEntry = null;
        TemplateSelection.clear();
        SchematicPlacementState.clear();
        SchematicProjectionRenderer.getInstance().clearCache();
        ClientToolState.setCurrentLitematic(null);
    }

    public record Entry(Path path, LitematicaSchematicInfo info) {
        public String displayName() {
            String metadataName = this.info.metadata().name();

            if (metadataName != null && !metadataName.isBlank() && !metadataName.equalsIgnoreCase("Unnamed")) {
                return stripTemplateSuffix(metadataName);
            }

            return stripTemplateSuffix(this.path.getFileName().toString());
        }

        private static String stripTemplateSuffix(String name) {
            String lowerName = name.toLowerCase();

            if (lowerName.endsWith(".litematica")) {
                return name.substring(0, name.length() - ".litematica".length());
            }

            if (lowerName.endsWith(".litematic")) {
                return name.substring(0, name.length() - ".litematic".length());
            }

            if (lowerName.endsWith(".nbt")) {
                return name.substring(0, name.length() - ".nbt".length());
            }

            return name;
        }
    }
}
