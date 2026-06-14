package com.iterablock.network;

import java.util.ArrayList;
import java.util.List;

import com.iterablock.IteraBlock;
import com.iterablock.common.PlacementReplaceMode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class IteraBlockNetwork {
    private static final int MAX_BLOCKS_PER_PACKET = 8192;

    private IteraBlockNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(PlaceSchematicPayload.TYPE, PlaceSchematicPayload.STREAM_CODEC, IteraBlockNetwork::handlePlaceSchematic);
    }

    private static void handlePlaceSchematic(PlaceSchematicPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        int count = Math.min(payload.blocks().size(), MAX_BLOCKS_PER_PACKET);
        PlacementReplaceMode replaceMode = PlacementReplaceMode.byId(payload.replaceModeId());

        for (int i = 0; i < count; i++) {
            PlacedBlock block = payload.blocks().get(i);

            if (!level.isLoaded(block.pos())) {
                continue;
            }

            BlockState state = Block.stateById(block.stateId());

            if (!state.isAir() && canReplace(level.getBlockState(block.pos()), replaceMode)) {
                level.setBlock(block.pos(), state, 3);
            }
        }
    }

    private static boolean canReplace(BlockState currentState, PlacementReplaceMode replaceMode) {
        return switch (replaceMode) {
            case REPLACE_ALL -> true;
            case ONLY_REPLACE_AIR -> currentState.isAir();
        };
    }

    public record PlaceSchematicPayload(List<PlacedBlock> blocks, int replaceModeId) implements CustomPacketPayload {
        public static final Type<PlaceSchematicPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IteraBlock.MODID, "place_schematic"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlaceSchematicPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> payload.write(buffer),
                PlaceSchematicPayload::read
        );

        private static PlaceSchematicPayload read(RegistryFriendlyByteBuf buffer) {
            int count = Math.min(buffer.readVarInt(), MAX_BLOCKS_PER_PACKET);
            List<PlacedBlock> blocks = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                blocks.add(new PlacedBlock(buffer.readBlockPos(), buffer.readVarInt()));
            }

            return new PlaceSchematicPayload(blocks, buffer.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            int count = Math.min(this.blocks.size(), MAX_BLOCKS_PER_PACKET);
            buffer.writeVarInt(count);

            for (int i = 0; i < count; i++) {
                PlacedBlock block = this.blocks.get(i);
                buffer.writeBlockPos(block.pos());
                buffer.writeVarInt(block.stateId());
            }

            buffer.writeVarInt(this.replaceModeId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PlacedBlock(BlockPos pos, int stateId) {
    }
}
