package com.iterablock.client.litematica;

import java.nio.file.Path;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record LitematicaSchematicInfo(
        Path file,
        int version,
        int subVersion,
        int minecraftDataVersion,
        Metadata metadata,
        List<Region> regions
) {
    public record Metadata(
            String name,
            String author,
            String description,
            BlockPos enclosingSize,
            long timeCreated,
            long timeModified,
            int regionCount,
            int totalVolume,
            int totalBlocks
    ) {
    }

    public record Region(
            String name,
            BlockPos position,
            BlockPos size,
            BlockPos absoluteSize,
            int paletteSize,
            int blockStateLongCount,
            int tileEntityCount,
            int entityCount,
            int pendingBlockTickCount,
            int pendingFluidTickCount,
            int decodedVolume,
            int decodedNonAirBlocks,
            int invalidPaletteIds,
            List<String> palettePreview,
            List<BlockSample> blocks
    ) {
    }

    public record BlockSample(
            BlockPos pos,
            BlockState state
    ) {
    }
}
