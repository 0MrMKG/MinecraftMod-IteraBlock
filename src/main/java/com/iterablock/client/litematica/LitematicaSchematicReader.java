package com.iterablock.client.litematica;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class LitematicaSchematicReader {
    private static final int MAX_SUPPORTED_VERSION = 7;
    private static final int PALETTE_PREVIEW_LIMIT = 6;
    private static final int MAX_RENDER_BLOCKS = 8192;

    private LitematicaSchematicReader() {
    }

    public static LitematicaSchematicInfo read(Path file) throws IOException {
        CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());

        if (!root.contains("Version", Tag.TAG_INT)) {
            throw new IOException("Missing Litematica Version tag");
        }

        int version = root.getInt("Version");

        if (version < 1 || version > MAX_SUPPORTED_VERSION) {
            throw new IOException("Unsupported Litematica version: " + version);
        }

        int subVersion = root.contains("SubVersion", Tag.TAG_INT) ? root.getInt("SubVersion") : 0;
        int minecraftDataVersion = root.contains("MinecraftDataVersion", Tag.TAG_INT) ? root.getInt("MinecraftDataVersion") : 0;
        LitematicaSchematicInfo.Metadata metadata = readMetadata(root.getCompound("Metadata"));
        List<LitematicaSchematicInfo.Region> regions = readRegions(root.getCompound("Regions"), version);

        return new LitematicaSchematicInfo(file, version, subVersion, minecraftDataVersion, metadata, regions);
    }

    private static LitematicaSchematicInfo.Metadata readMetadata(CompoundTag tag) {
        return new LitematicaSchematicInfo.Metadata(
                tag.getString("Name"),
                tag.getString("Author"),
                tag.getString("Description"),
                readBlockPos(tag.getCompound("EnclosingSize")),
                tag.getLong("TimeCreated"),
                tag.getLong("TimeModified"),
                tag.getInt("RegionCount"),
                tag.getInt("TotalVolume"),
                tag.getInt("TotalBlocks")
        );
    }

    private static List<LitematicaSchematicInfo.Region> readRegions(CompoundTag regionsTag, int version) {
        List<LitematicaSchematicInfo.Region> regions = new ArrayList<>();

        for (String regionName : regionsTag.getAllKeys()) {
            if (regionsTag.getTagType(regionName) == Tag.TAG_COMPOUND) {
                regions.add(readRegion(regionName, regionsTag.getCompound(regionName), version));
            }
        }

        return regions;
    }

    private static LitematicaSchematicInfo.Region readRegion(String regionName, CompoundTag tag, int version) {
        BlockPos position = readBlockPos(tag.getCompound("Position"));
        BlockPos size = readBlockPos(tag.getCompound("Size"));
        BlockPos absoluteSize = new BlockPos(Math.abs(size.getX()), Math.abs(size.getY()), Math.abs(size.getZ()));
        ListTag palette = tag.getList("BlockStatePalette", Tag.TAG_COMPOUND);
        long[] blockStates = readBlockStates(tag);
        int volume = absoluteSize.getX() * absoluteSize.getY() * absoluteSize.getZ();
        DecodeStats stats = decodeBlockStates(palette, blockStates, volume);
        List<LitematicaSchematicInfo.BlockSample> blocks = readBlockSamples(palette, blockStates, absoluteSize, volume);

        return new LitematicaSchematicInfo.Region(
                regionName,
                position,
                size,
                absoluteSize,
                palette.size(),
                blockStates.length,
                tag.getList("TileEntities", Tag.TAG_COMPOUND).size(),
                tag.getList("Entities", Tag.TAG_COMPOUND).size(),
                version >= 3 ? tag.getList("PendingBlockTicks", Tag.TAG_COMPOUND).size() : 0,
                version >= 5 ? tag.getList("PendingFluidTicks", Tag.TAG_COMPOUND).size() : 0,
                volume,
                stats.nonAirBlocks(),
                stats.invalidPaletteIds(),
                readPalettePreview(palette),
                blocks
        );
    }

    private static long[] readBlockStates(CompoundTag tag) {
        if (tag.getTagType("BlockStates") == Tag.TAG_LONG_ARRAY) {
            return ((LongArrayTag) tag.get("BlockStates")).getAsLongArray();
        }

        return new long[0];
    }

    private static DecodeStats decodeBlockStates(ListTag palette, long[] blockStates, int volume) {
        if (palette.isEmpty() || blockStates.length == 0 || volume <= 0) {
            return new DecodeStats(0, 0);
        }

        int bits = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(palette.size() - 1));
        int nonAirBlocks = 0;
        int invalidPaletteIds = 0;

        for (int index = 0; index < volume; index++) {
            int paletteId = getPackedValue(blockStates, index, bits);

            if (paletteId < 0 || paletteId >= palette.size()) {
                invalidPaletteIds++;
            } else if (!isAirPaletteEntry(palette.getCompound(paletteId))) {
                nonAirBlocks++;
            }
        }

        return new DecodeStats(nonAirBlocks, invalidPaletteIds);
    }

    private static List<LitematicaSchematicInfo.BlockSample> readBlockSamples(ListTag palette, long[] blockStates, BlockPos size, int volume) {
        List<LitematicaSchematicInfo.BlockSample> blocks = new ArrayList<>();

        if (palette.isEmpty() || blockStates.length == 0 || volume <= 0 || size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
            return blocks;
        }

        int bits = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(palette.size() - 1));

        for (int index = 0; index < volume && blocks.size() < MAX_RENDER_BLOCKS; index++) {
            int paletteId = getPackedValue(blockStates, index, bits);

            if (paletteId < 0 || paletteId >= palette.size()) {
                continue;
            }

            CompoundTag paletteEntry = palette.getCompound(paletteId);

            if (isAirPaletteEntry(paletteEntry)) {
                continue;
            }

            int x = index % size.getX();
            int z = index / size.getX() % size.getZ();
            int y = index / (size.getX() * size.getZ());
            blocks.add(new LitematicaSchematicInfo.BlockSample(new BlockPos(x, y, z), readBlockState(paletteEntry)));
        }

        return blocks;
    }

    private static BlockState readBlockState(CompoundTag paletteEntry) {
        ResourceLocation id = ResourceLocation.tryParse(paletteEntry.getString("Name"));
        Block block = id == null ? Blocks.AIR : BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR);
        BlockState state = block.defaultBlockState();

        if (paletteEntry.contains("Properties", Tag.TAG_COMPOUND)) {
            CompoundTag properties = paletteEntry.getCompound("Properties");

            for (String key : properties.getAllKeys()) {
                state = applyProperty(state, key, properties.getString(key));
            }
        }

        return state;
    }

    private static BlockState applyProperty(BlockState state, String key, String value) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(key)) {
                return applyPropertyValue(state, property, value);
            }
        }

        return state;
    }

    private static <T extends Comparable<T>> BlockState applyPropertyValue(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.map(propertyValue -> state.setValue(property, propertyValue)).orElse(state);
    }

    private static int getPackedValue(long[] data, int index, int bits) {
        long bitIndex = (long) index * bits;
        int startLong = (int) (bitIndex >>> 6);
        int startOffset = (int) (bitIndex & 63L);

        if (startLong >= data.length) {
            return -1;
        }

        long value = data[startLong] >>> startOffset;
        int endOffset = startOffset + bits;

        if (endOffset > 64) {
            int nextLong = startLong + 1;

            if (nextLong >= data.length) {
                return -1;
            }

            value |= data[nextLong] << (64 - startOffset);
        }

        long mask = (1L << bits) - 1L;
        return (int) (value & mask);
    }

    private static boolean isAirPaletteEntry(CompoundTag tag) {
        return "minecraft:air".equals(tag.getString("Name"));
    }

    private static List<String> readPalettePreview(ListTag palette) {
        List<String> preview = new ArrayList<>();
        int count = Math.min(PALETTE_PREVIEW_LIMIT, palette.size());

        for (int i = 0; i < count; i++) {
            CompoundTag tag = palette.getCompound(i);
            preview.add(tag.getString("Name"));
        }

        return preview;
    }

    private static BlockPos readBlockPos(CompoundTag tag) {
        return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    private record DecodeStats(int nonAirBlocks, int invalidPaletteIds) {
    }
}
