package com.iterablock.client.tool;

import java.util.List;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.litematica.LitematicaSchematicInfo;
import com.iterablock.client.template.LoadedLitematicManager;
import com.iterablock.common.PlacementReplaceMode;
import com.iterablock.network.IteraBlockNetwork.PlaceSchematicPayload;
import com.iterablock.network.IteraBlockNetwork.PlacedBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ToolState {
    private static final int MAX_PLACED_BLOCKS = 8192;
    private static ToolMode mode = ToolMode.SCHEMATIC_PLACEMENT;
    private static String lastAction = "";

    private ToolState() {
    }

    public static ToolMode getMode() {
        return mode;
    }

    public static String getLastAction() {
        return lastAction.isEmpty() ? Lang.tr("iterablock.tool.action.ready") : lastAction;
    }

    public static void setMode(ToolMode newMode) {
        if (newMode == null || mode == newMode) {
            return;
        }

        mode = newMode;
        setLastAction(Lang.tr("iterablock.tool.action.mode_changed", mode.getDisplayName()));
    }

    public static void cycleMode(boolean forward) {
        setMode(mode.cycle(forward));
    }

    public static void adjustLinearArray(Minecraft minecraft, int amount) {
        if (mode != ToolMode.LINEAR_ARRAY || minecraft.player == null || amount == 0) {
            return;
        }

        SchematicPlacementState.adjustLinearArray(minecraft.player.getLookAngle(), amount);
        setLastAction(Lang.tr("iterablock.tool.action.linear_array_count", SchematicPlacementState.getLinearArrayAxisName(), SchematicPlacementState.getLinearArrayCount()));
    }

    public static boolean hasToolItem(LocalPlayer player) {
        return player != null && (isToolItem(player.getMainHandItem()) || isToolItem(player.getOffhandItem()));
    }

    public static boolean isToolItem(ItemStack stack) {
        return stack.is(Items.BLAZE_ROD);
    }

    public static boolean handlePrimaryAction(Minecraft minecraft) {
        if (!hasToolItem(minecraft.player)) {
            return false;
        }

        setLastAction(withCurrentLitematic(Lang.tr("iterablock.tool.action.mode_primary", mode.getDisplayName())));
        return true;
    }

    public static boolean handleSecondaryAction(Minecraft minecraft) {
        if (!hasToolItem(minecraft.player)) {
            return false;
        }

        switch (mode) {
            case SCHEMATIC_PLACEMENT -> handleSchematicPlacementSecondary(minecraft);
            default -> setLastAction(withCurrentLitematic(Lang.tr("iterablock.tool.action.mode_secondary", mode.getDisplayName())));
        }

        return true;
    }

    public static boolean placeCurrentProjection(Minecraft minecraft) {
        if (!hasToolItem(minecraft.player)) {
            return false;
        }

        if (!SchematicPlacementState.hasPlacement() || minecraft.level == null || minecraft.getConnection() == null) {
            setLastAction(Lang.tr("iterablock.tool.action.no_projection"));
            return true;
        }

        LoadedLitematicManager.Entry entry = SchematicPlacementState.getEntry();
        BlockPos origin = SchematicPlacementState.getOrigin();

        if (entry == null || origin == null) {
            setLastAction(Lang.tr("iterablock.tool.action.no_projection"));
            return true;
        }

        List<PlacedBlock> blocks = collectBlocks(origin, entry.info());
        PlacementReplaceMode replaceMode = BuilderHelperClientConfig.getPlacementReplaceMode();
        PacketDistributor.sendToServer(new PlaceSchematicPayload(blocks, replaceMode.id()));
        setLastAction(Lang.tr("iterablock.tool.action.placed_projection", blocks.size()));
        return true;
    }

    public static boolean rotateCurrentProjection() {
        if (!SchematicPlacementState.hasPlacement()) {
            setLastAction(Lang.tr("iterablock.tool.action.no_projection"));
            return true;
        }

        SchematicPlacementState.rotateClockwise();
        setLastAction(Lang.tr("iterablock.tool.action.rotated_projection", SchematicPlacementState.getRotationSteps() * 90));
        return true;
    }

    private static void handleSchematicPlacementSecondary(Minecraft minecraft) {
        if (ClientToolState.currentLitematic == null) {
            setLastAction(Lang.tr("iterablock.tool.action.no_litematic"));
            return;
        }

        BlockPos origin = getPlacementOrigin(minecraft);
        SchematicPlacementState.place(ClientToolState.currentLitematic, origin);
        setLastAction(withCurrentLitematic(Lang.tr("iterablock.tool.action.placement_projected")));
    }

    private static BlockPos getPlacementOrigin(Minecraft minecraft) {
        int range = BuilderHelperClientConfig.getPlacementRange();
        HitResult result = minecraft.player.pick(range, 0.0F, false);

        if (result instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos().relative(hitResult.getDirection());
        }

        return BlockPos.containing(result.getLocation());
    }

    private static List<PlacedBlock> collectBlocks(BlockPos origin, LitematicaSchematicInfo info) {
        List<PlacedBlock> placed = new java.util.ArrayList<>();
        int copies = mode == ToolMode.LINEAR_ARRAY ? SchematicPlacementState.getLinearArrayCopyCount() : 1;
        BlockPos arrayStep = SchematicPlacementState.getLinearArrayStep(info);

        for (int copy = 0; copy < copies; copy++) {
            BlockPos copyOrigin = origin.offset(SchematicPlacementState.getLinearArrayOffset(copy, arrayStep));
            collectBlocksForOrigin(copyOrigin, info, placed);

            if (placed.size() >= MAX_PLACED_BLOCKS) {
                return placed;
            }
        }

        return placed;
    }

    private static void collectBlocksForOrigin(BlockPos origin, LitematicaSchematicInfo info, List<PlacedBlock> placed) {
        for (LitematicaSchematicInfo.Region region : info.regions()) {
            List<LitematicaSchematicInfo.BlockSample> blocks = region.blocks();

            for (LitematicaSchematicInfo.BlockSample block : blocks) {
                if (placed.size() >= MAX_PLACED_BLOCKS) {
                    return;
                }

                BlockState state = SchematicPlacementState.transformState(block.state());

                if (state.isAir()) {
                    continue;
                }

                BlockPos pos = origin.offset(SchematicPlacementState.transformBlockOffset(region.position(), block.pos(), region.size()));
                placed.add(new PlacedBlock(pos, Block.getId(state)));
            }
        }
    }

    private static String withCurrentLitematic(String action) {
        if (ClientToolState.currentLitematic == null) {
            return action + " - " + Lang.tr("iterablock.tool.litematic_none");
        }

        return action + " - " + Lang.tr("iterablock.tool.litematic", ClientToolState.currentLitematic.displayName());
    }

    private static void setLastAction(String text) {
        lastAction = text;
        showActionBar(text);
    }

    private static void showActionBar(String text) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player != null) {
            player.displayClientMessage(Component.literal(Lang.tr("iterablock.tool.actionbar", text)), true);
        }
    }
}
