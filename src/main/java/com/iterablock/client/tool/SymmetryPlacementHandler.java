package com.iterablock.client.tool;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;

public final class SymmetryPlacementHandler {
    private static final SymmetryPlacementHandler INSTANCE = new SymmetryPlacementHandler();

    private SymmetryPlacementHandler() {
    }

    public static SymmetryPlacementHandler getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (!event.getLevel().isClientSide()
                || minecraft.screen != null
                || minecraft.player == null
                || minecraft.getConnection() == null
                || event.getEntity() != minecraft.player
                || ToolState.getMode() != ToolMode.SYMMETRY_PLACEMENT
                || !SymmetryPlacementState.isActive()) {
            return;
        }

        BlockHitResult hitResult = event.getHitVec();
        BlockPos interactedPos = event.getPos();
        BlockState interactedState = event.getLevel().getBlockState(interactedPos);

        if (!minecraft.player.isShiftKeyDown() && interactedState.hasProperty(BlockStateProperties.OPEN)) {
            mirrorOpenStateInteraction(minecraft, interactedPos, interactedState);
            return;
        }

        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        BlockPos placedPos = event.getPos().relative(hitResult.getDirection());

        if (!SymmetryPlacementState.contains(placedPos)) {
            return;
        }

        BlockState state = blockItem.getBlock().getStateForPlacement(new BlockPlaceContext((LocalPlayer) minecraft.player, event.getHand(), stack, hitResult));

        if (state == null || state.isAir()) {
            return;
        }

        List<SymmetryPlacementState.MirrorPlacement> placements = SymmetryPlacementState.getMirrorPlacements(placedPos);

        if (placements.isEmpty()) {
            return;
        }

        CommandFeedbackSilencer.getInstance().expectPlacementFeedback(placements.size());

        for (SymmetryPlacementState.MirrorPlacement placement : placements) {
            minecraft.player.connection.sendCommand(toSetBlockCommand(placement.pos(), mirrorState(state, placement)));
        }
    }

    private static void mirrorOpenStateInteraction(Minecraft minecraft, BlockPos interactedPos, BlockState interactedState) {
        if (!SymmetryPlacementState.contains(interactedPos)) {
            return;
        }

        List<SymmetryPlacementState.MirrorPlacement> placements = SymmetryPlacementState.getMirrorPlacements(interactedPos);

        if (placements.isEmpty()) {
            return;
        }

        int mirroredCount = 0;

        for (SymmetryPlacementState.MirrorPlacement placement : placements) {
            BlockState targetState = minecraft.level.getBlockState(placement.pos());

            if (targetState.getBlock() == interactedState.getBlock()) {
                mirroredCount++;
            }
        }

        if (mirroredCount == 0) {
            return;
        }

        BlockState toggledState = interactedState.cycle(BlockStateProperties.OPEN);
        CommandFeedbackSilencer.getInstance().expectPlacementFeedback(mirroredCount);

        for (SymmetryPlacementState.MirrorPlacement placement : placements) {
            BlockState targetState = minecraft.level.getBlockState(placement.pos());

            if (targetState.getBlock() == interactedState.getBlock()) {
                minecraft.player.connection.sendCommand(toSetBlockCommand(placement.pos(), mirrorState(toggledState, placement)));
            }
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (!event.getLevel().isClientSide()
                || event.getAction() != Action.START
                || minecraft.screen != null
                || minecraft.player == null
                || minecraft.getConnection() == null
                || event.getEntity() != minecraft.player
                || ToolState.getMode() != ToolMode.SYMMETRY_PLACEMENT
                || !SymmetryPlacementState.isActive()) {
            return;
        }

        BlockPos brokenPos = event.getPos();

        if (!SymmetryPlacementState.contains(brokenPos)) {
            return;
        }

        List<SymmetryPlacementState.MirrorPlacement> placements = SymmetryPlacementState.getMirrorPlacements(brokenPos);

        if (placements.isEmpty()) {
            return;
        }

        CommandFeedbackSilencer.getInstance().expectPlacementFeedback(placements.size());

        for (SymmetryPlacementState.MirrorPlacement placement : placements) {
            minecraft.player.connection.sendCommand(toClearBlockCommand(placement.pos()));
        }
    }

    private static BlockState mirrorState(BlockState state, SymmetryPlacementState.MirrorPlacement placement) {
        BlockState mirrored = state;

        if (placement.mirrorX()) {
            mirrored = mirrored.mirror(Mirror.FRONT_BACK);
        }

        if (placement.mirrorZ()) {
            mirrored = mirrored.mirror(Mirror.LEFT_RIGHT);
        }

        return mirrored;
    }

    private static String toSetBlockCommand(BlockPos pos, BlockState state) {
        return "setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + BlockStateParser.serialize(state) + " replace";
    }

    private static String toClearBlockCommand(BlockPos pos) {
        return "setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " air replace";
    }
}
