package com.iterablock.client.tool;

import java.util.List;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.template.LoadedLitematicManager;

import fi.dy.masa.malilib.interfaces.IRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class ToolHudRenderer implements IRenderer {
    private static final ToolHudRenderer INSTANCE = new ToolHudRenderer();
    private static final float HUD_SCALE = 0.75F;
    private static final int HOTBAR_AVAILABLE_THEME = 0xFF9AF5B0;
    private static final int HOTBAR_RESERVED_THEME = 0xFFD98A4E;
    private static final int HOTBAR_AVAILABLE_ACCENT = 0xB69AF5B0;
    private static final int HOTBAR_RESERVED_ACCENT = 0xB6D98A4E;
    private static final int[] MODE_RAINBOW_COLORS = {
            0xFFFF6B6B, 0xFFFFA24D, 0xFFFFE680, 0xFF9AF5B0, 0xFF7FE9FF, 0xFF8DB6FF
    };
    private HudCacheKey cachedHudKey;
    private HudLayout cachedHudLayout;

    private ToolHudRenderer() {
    }

    public static ToolHudRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void onRenderGameOverlayPost(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();

        ToolMode mode = ToolState.getMode();

        boolean persistentRandomArea = ToolState.isRandomAreaMode() && ToolState.isRandomAreaLocked();
        if (minecraft.options.hideGui
                || minecraft.screen != null
                || minecraft.player == null
                || (!ToolState.hasToolItem(minecraft.player)
                && mode != ToolMode.BEZIER_CURVE_GENERATION
                && mode != ToolMode.SYMMETRY_PLACEMENT
                && !persistentRandomArea)) {
            return;
        }

        Font font = minecraft.font;
        boolean canScrollHotbar = ToolInputHandler.getInstance().canScrollHotbar(minecraft);
        HudLayout hud = this.getHudLayout(minecraft, font, mode, canScrollHotbar);
        int x = Math.round(8 / HUD_SCALE);
        int y = Math.round(8 / HUD_SCALE);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(HUD_SCALE, HUD_SCALE, 1.0F);
        guiGraphics.fill(x - 4, y - 4, x + hud.width(), y + hud.height(), 0x8A071018);
        guiGraphics.fill(x - 4, y - 4, x + hud.width(), y - 3,
                canScrollHotbar ? HOTBAR_AVAILABLE_ACCENT : HOTBAR_RESERVED_ACCENT);

        for (int i = 0; i < hud.lines().size(); i++) {
            HudLine line = hud.lines().get(i);
            guiGraphics.drawString(font, line.text(), x, y + i * 12, line.color(), true);
        }

        guiGraphics.pose().popPose();
    }

    private HudLayout getHudLayout(Minecraft minecraft, Font font, ToolMode mode, boolean canScrollHotbar) {
        HudCacheKey key = createHudCacheKey(minecraft, mode, canScrollHotbar);

        if (key.equals(this.cachedHudKey) && this.cachedHudLayout != null) {
            return this.cachedHudLayout;
        }

        Component modeText = Component.translatable("iterablock.tool.mode_numbered",
                value(formatModeNumber(mode.ordinal() + 1)), value(mode.getDisplayName()));
        Component litematicText = Component.translatable("iterablock.tool.litematic",
                value(ClientToolState.currentLitematic == null
                        ? Lang.tr("iterablock.tool.litematic_none_value")
                        : ClientToolState.currentLitematic.displayName()));
        int themeColor = canScrollHotbar ? HOTBAR_AVAILABLE_THEME : HOTBAR_RESERVED_THEME;
        List<HudLine> lines = List.of(
                new HudLine(Component.literal("IteraBlock"), themeColor),
                new HudLine(modeText, getModeRainbowColor(mode)),
                new HudLine(litematicText, 0xFFFFFF),
                new HudLine(getInfoLine(minecraft, mode), 0xFFFFFF)
        );

        int width = 0;
        for (HudLine line : lines) {
            width = Math.max(width, font.width(line.text()));
        }

        this.cachedHudKey = key;
        this.cachedHudLayout = new HudLayout(List.copyOf(lines), width + 14, 4 + lines.size() * 12);
        return this.cachedHudLayout;
    }

    private static int getModeRainbowColor(ToolMode mode) {
        return MODE_RAINBOW_COLORS[mode.ordinal() % MODE_RAINBOW_COLORS.length];
    }

    private static HudCacheKey createHudCacheKey(Minecraft minecraft, ToolMode mode, boolean canScrollHotbar) {
        SchematicPlacementState.Axis lookAxis = minecraft.player == null ? null : SchematicPlacementState.getLookAxis(minecraft.player.getLookAngle());
        BlockPos symmetryCenter = SymmetryPlacementState.getCenter();
        return new HudCacheKey(
                mode,
                canScrollHotbar,
                SchematicPlacementState.getArrayMode(),
                ToolState.getRandomPlacementMode(),
                ToolState.isRandomAreaLocked(),
                ClientToolState.currentLitematic,
                lookAxis,
                BuilderHelperClientConfig.isBezierPlaceNbtMode(),
                BuilderHelperClientConfig.getBezierPlacementPrecision(),
                BuilderHelperClientConfig.getBezierPlacementWidth(),
                BezierCurveState.getPointCount(),
                BezierCurveState.getRequiredPointCount(),
                BezierCurveState.getRevision(),
                BuilderHelperClientConfig.getRandomPlacementRadius(),
                BuilderHelperClientConfig.getRandomPlacementHeightMin(),
                BuilderHelperClientConfig.getRandomPlacementHeightMax(),
                BuilderHelperClientConfig.getRandomPlacementCount(),
                BuilderHelperClientConfig.getRandomPlacementRotationChance(),
                AreaSelectionState.getFirstCorner(),
                AreaSelectionState.getSecondCorner(),
                AreaSelectionState.getActiveCorner(),
                SchematicPlacementState.hasPlacement(),
                SchematicPlacementState.getOrigin(),
                SchematicPlacementState.getLinearArrayAxisName(),
                SchematicPlacementState.getLinearArrayCount(),
                SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.X),
                SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.Y),
                SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.Z),
                SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.X),
                SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Y),
                SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Z),
                SchematicPlacementState.isPlacementPointAdjusting(),
                SchematicPlacementState.getPlacementOffset().getX(),
                SchematicPlacementState.getPlacementOffset().getY(),
                SchematicPlacementState.getPlacementOffset().getZ(),
                SymmetryPlacementState.hasCenter(),
                symmetryCenter,
                SymmetryPlacementState.getKind(),
                SymmetryPlacementState.getParity(),
                SymmetryPlacementState.getRadius(),
                SymmetryPlacementState.getHeight(),
                SymmetryPlacementState.isLocked(),
                SymmetryPlacementState.isEnabled()
        );
    }

    private static Component getInfoLine(Minecraft minecraft, ToolMode mode) {
        if (minecraft.player == null) {
            return Component.translatable("iterablock.tool.action.ready");
        }

        if (mode == ToolMode.AREA_COPY_PASTE) {
            if (!AreaSelectionState.hasFirstCorner() && !AreaSelectionState.hasSecondCorner()) {
                return Component.translatable("iterablock.tool.area.empty");
            }

            if (!AreaSelectionState.hasSelection()) {
                return Component.translatable("iterablock.tool.area.partial",
                        value(formatPos(AreaSelectionState.getFirstCorner())),
                        value(formatPos(AreaSelectionState.getSecondCorner())));
            }

            return Component.translatable("iterablock.tool.area.selection",
                    value(formatPos(AreaSelectionState.getFirstCorner())),
                    value(formatPos(AreaSelectionState.getSecondCorner())),
                    value(AreaSelectionState.getSizeX()),
                    value(AreaSelectionState.getSizeY()),
                    value(AreaSelectionState.getSizeZ()));
        }

        if (mode == ToolMode.BEZIER_CURVE_GENERATION) {
            String placementMode = Lang.tr(BuilderHelperClientConfig.isBezierPlaceNbtMode()
                    ? "iterablock.tool.bezier.placement_mode.nbt"
                    : "iterablock.tool.bezier.placement_mode.block");
            return Component.translatable("iterablock.tool.hud.bezier",
                    value(BezierCurveState.getPointCount()),
                    value(BezierCurveState.getRequiredPointCount()),
                    value(BezierCurveState.getSamplePointCount()),
                    value(BuilderHelperClientConfig.getBezierPlacementPrecision()),
                    value(BuilderHelperClientConfig.getBezierPlacementWidth()),
                    value(placementMode));
        }

        if (mode == ToolMode.RANDOM_SCHEMATIC_PLACEMENT) {
            if (ToolState.getRandomPlacementMode() == ToolState.RandomPlacementMode.AREA_BLOCK) {
                if (!AreaSelectionState.hasSelection()) {
                    return Component.translatable("iterablock.tool.random.area.empty");
                }

                return Component.translatable("iterablock.tool.random.area.selection",
                        value(AreaSelectionState.getSizeX()),
                        value(AreaSelectionState.getSizeY()),
                        value(AreaSelectionState.getSizeZ()),
                        value(Lang.tr(ToolState.isRandomAreaLocked()
                                ? "iterablock.tool.random.area.locked"
                                : "iterablock.tool.random.area.editing")));
            }

            return Component.translatable("iterablock.tool.random.params",
                    value(BuilderHelperClientConfig.getRandomPlacementRadius()),
                    value(BuilderHelperClientConfig.getRandomPlacementHeightMin()),
                    value(BuilderHelperClientConfig.getRandomPlacementHeightMax()),
                    value(BuilderHelperClientConfig.getRandomPlacementCount()),
                    value(BuilderHelperClientConfig.getRandomPlacementRotationChance()));
        }

        if (mode == ToolMode.SYMMETRY_PLACEMENT) {
            if (!SymmetryPlacementState.hasCenter()) {
                return Component.translatable("iterablock.tool.symmetry.empty");
            }

            BlockPos center = SymmetryPlacementState.getCenter();
            return Component.translatable("iterablock.tool.symmetry.params",
                    value(Lang.tr(SymmetryPlacementState.getKind().translationKey())),
                    value(Lang.tr(SymmetryPlacementState.getParity().translationKey())),
                    value(center.getX()),
                    value(center.getY()),
                    value(center.getZ()),
                    value(SymmetryPlacementState.getRadius()),
                    value(SymmetryPlacementState.getHeight()),
                    value(Lang.tr(SymmetryPlacementState.isLocked() ? "iterablock.tool.symmetry.locked" : "iterablock.tool.symmetry.editing")),
                    value(Lang.tr(SymmetryPlacementState.isEnabled() ? "iterablock.tool.symmetry.enabled" : "iterablock.tool.symmetry.paused")));
        }

        if (mode == ToolMode.SCHEMATIC_PLACEMENT) {
            if (!SchematicPlacementState.hasPlacement()) {
                return Component.translatable("iterablock.tool.schematic.empty");
            }

            BlockPos offset = SchematicPlacementState.getPlacementOffset();
            String state = Lang.tr(SchematicPlacementState.isPlacementPointAdjusting()
                    ? "iterablock.tool.schematic.adjusting"
                    : "iterablock.tool.schematic.normal");
            return Component.translatable("iterablock.tool.schematic.info",
                    value(state), value(offset.getX()), value(offset.getY()), value(offset.getZ()));
        }

        if (mode == ToolMode.ARRAY_PLACEMENT
                && SchematicPlacementState.getArrayMode() == SchematicPlacementState.ArrayMode.LINEAR) {
            if (!SchematicPlacementState.hasPlacement()) {
                return Component.translatable("iterablock.tool.hud.array.linear.empty");
            }

            SchematicPlacementState.Axis lookAxis = SchematicPlacementState.getLookAxis(minecraft.player.getLookAngle());
            String axisName = SchematicPlacementState.getLinearArrayAxisName();
            SchematicPlacementState.Axis axis = "-".equals(axisName)
                    ? lookAxis
                    : SchematicPlacementState.Axis.valueOf(axisName);
            int count = SchematicPlacementState.getLinearArrayCount();
            return Component.translatable("iterablock.tool.hud.array.linear",
                    axis(axis, lookAxis), value(count), value(SchematicPlacementState.getOverlap(axis)));
        }

        if (mode == ToolMode.ARRAY_PLACEMENT
                && SchematicPlacementState.getArrayMode() == SchematicPlacementState.ArrayMode.VOLUME) {
            if (!SchematicPlacementState.hasPlacement()) {
                return Component.translatable("iterablock.tool.hud.array.volume.empty");
            }

            SchematicPlacementState.Axis lookAxis = SchematicPlacementState.getLookAxis(minecraft.player.getLookAngle());
            return Component.translatable("iterablock.tool.hud.array.volume",
                    axis(SchematicPlacementState.Axis.X, lookAxis),
                    value(SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.X)),
                    value(SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.X)),
                    axis(SchematicPlacementState.Axis.Y, lookAxis),
                    value(SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.Y)),
                    value(SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Y)),
                    axis(SchematicPlacementState.Axis.Z, lookAxis),
                    value(SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.Z)),
                    value(SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Z)));
        }

        return Component.translatable("iterablock.tool.action.ready");
    }

    private static Component value(Object value) {
        return Component.literal(String.valueOf(value));
    }

    private static Component axis(SchematicPlacementState.Axis axis, SchematicPlacementState.Axis lookAxis) {
        Component result = Component.translatable("iterablock.tool.axis." + axis.name().toLowerCase());
        return axis == lookAxis ? result.copy().withStyle(ChatFormatting.GREEN) : result;
    }

    private static String formatModeNumber(int number) {
        return switch (number) {
            case 1 -> "\u4e00";
            case 2 -> "\u4e8c";
            case 3 -> "\u4e09";
            case 4 -> "\u56db";
            case 5 -> "\u4e94";
            case 6 -> "\u516d";
            case 7 -> "\u4e03";
            case 8 -> "\u516b";
            case 9 -> "\u4e5d";
            default -> Integer.toString(number);
        };
    }

    private static String formatPos(net.minecraft.core.BlockPos pos) {
        return pos == null ? "-" : pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private record HudLine(Component text, int color) {
    }

    private record HudLayout(List<HudLine> lines, int width, int height) {
    }

    private record HudCacheKey(
            ToolMode mode,
            boolean canScrollHotbar,
            SchematicPlacementState.ArrayMode arrayMode,
            ToolState.RandomPlacementMode randomPlacementMode,
            boolean randomAreaLocked,
            LoadedLitematicManager.Entry litematic,
            SchematicPlacementState.Axis lookAxis,
            boolean bezierPlaceNbtMode,
            int bezierPrecision,
            int bezierWidth,
            int bezierPointCount,
            int bezierRequiredPointCount,
            int bezierRevision,
            int randomRadius,
            int randomHeightMin,
            int randomHeightMax,
            int randomCount,
            int randomRotationChance,
            BlockPos areaFirstCorner,
            BlockPos areaSecondCorner,
            AreaSelectionState.Corner areaActiveCorner,
            boolean hasPlacement,
            BlockPos placementOrigin,
            String linearAxis,
            int linearCount,
            int volumeX,
            int volumeY,
            int volumeZ,
            int overlapX,
            int overlapY,
            int overlapZ,
            boolean placementPointAdjusting,
            int placementOffsetX,
            int placementOffsetY,
            int placementOffsetZ,
            boolean symmetryHasCenter,
            BlockPos symmetryCenter,
            SymmetryPlacementState.Kind symmetryKind,
            SymmetryPlacementState.Parity symmetryParity,
            int symmetryRadius,
            int symmetryHeight,
            boolean symmetryLocked,
            boolean symmetryEnabled) {
    }
}
