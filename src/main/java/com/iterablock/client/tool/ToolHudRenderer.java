package com.iterablock.client.tool;

import java.util.ArrayList;
import java.util.List;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.template.LoadedLitematicManager;

import fi.dy.masa.malilib.interfaces.IRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

public class ToolHudRenderer implements IRenderer {
    private static final ToolHudRenderer INSTANCE = new ToolHudRenderer();
    private static final float HUD_SCALE = 0.75F;
    private HudCacheKey cachedHudKey;
    private HudLayout cachedHudLayout;
    private BezierSampleCacheKey cachedBezierSampleKey;
    private String cachedBezierSampleText = "";
    private int cachedBezierSampleWidth;

    private ToolHudRenderer() {
    }

    public static ToolHudRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void onRenderGameOverlayPost(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();

        ToolMode mode = ToolState.getMode();

        if (minecraft.options.hideGui || minecraft.screen != null || minecraft.player == null || (!ToolState.hasToolItem(minecraft.player) && mode != ToolMode.BEZIER_CURVE_GENERATION && mode != ToolMode.SYMMETRY_PLACEMENT)) {
            return;
        }

        Font font = minecraft.font;
        HudLayout hud = this.getHudLayout(minecraft, font, mode);
        int x = Math.round(8 / HUD_SCALE);
        int y = Math.round(8 / HUD_SCALE);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(HUD_SCALE, HUD_SCALE, 1.0F);
        guiGraphics.fill(x - 4, y - 4, x + hud.width(), y + hud.height(), 0x8A071018);
        guiGraphics.fill(x - 4, y - 4, x + hud.width(), y - 3, 0xB64EAFC5);

        for (int i = 0; i < hud.lines().size(); i++) {
            HudLine line = hud.lines().get(i);
            guiGraphics.drawString(font, line.text(), x, y + i * 12, line.color(), true);
        }

        guiGraphics.pose().popPose();

        if (mode == ToolMode.BEZIER_CURVE_GENERATION) {
            this.drawBezierSampleCount(guiGraphics, font);
        }
    }

    private HudLayout getHudLayout(Minecraft minecraft, Font font, ToolMode mode) {
        HudCacheKey key = createHudCacheKey(minecraft, mode);

        if (key.equals(this.cachedHudKey) && this.cachedHudLayout != null) {
            return this.cachedHudLayout;
        }

        String title = Lang.tr("iterablock.tool.title");
        String plainModeText = Lang.tr("iterablock.tool.mode", mode.getDisplayName());
        String modeText = Lang.tr("iterablock.tool.mode_numbered", formatModeNumber(mode.ordinal() + 1), mode.getDisplayName());
        String litematicText = ClientToolState.currentLitematic == null ? Lang.tr("iterablock.tool.litematic_none") : Lang.tr("iterablock.tool.litematic", ClientToolState.currentLitematic.displayName());
        String bezierPlacementModeText = getBezierPlacementModeText(mode);
        String arrayText = getArrayText(minecraft, mode);
        String overlapText = getOverlapText(minecraft, mode);
        String actionText = ToolState.getLastAction();
        boolean showAction = actionText != null
                && !actionText.isBlank()
                && !actionText.equals(modeText)
                && !actionText.equals(plainModeText)
                && !actionText.equals(arrayText);
        List<HudLine> lines = new ArrayList<>();
        lines.add(new HudLine(title, 0xD6F4FF));
        lines.add(new HudLine(modeText, getModeTextColor(mode)));
        lines.add(new HudLine(litematicText, 0xFFFFFF));

        if (!bezierPlacementModeText.isEmpty()) {
            lines.add(new HudLine(bezierPlacementModeText, 0xFFF3EECF));
        }

        if (!arrayText.isEmpty()) {
            lines.add(new HudLine(arrayText, 0xD6F4FF));
        }

        if (!overlapText.isEmpty()) {
            lines.add(new HudLine(overlapText, 0xE8F6FF));
        }

        if (showAction) {
            lines.add(new HudLine(actionText, 0xA7D9E6));
        }

        int width = 0;
        for (HudLine line : lines) {
            width = Math.max(width, font.width(line.text()));
        }

        this.cachedHudKey = key;
        this.cachedHudLayout = new HudLayout(List.copyOf(lines), width + 14, 4 + lines.size() * 12);
        return this.cachedHudLayout;
    }

    private static HudCacheKey createHudCacheKey(Minecraft minecraft, ToolMode mode) {
        SchematicPlacementState.Axis lookAxis = minecraft.player == null ? null : SchematicPlacementState.getLookAxis(minecraft.player.getLookAngle());
        BlockPos symmetryCenter = SymmetryPlacementState.getCenter();
        return new HudCacheKey(
                mode,
                ClientToolState.currentLitematic,
                ToolState.getLastActionCacheToken(),
                lookAxis,
                BuilderHelperClientConfig.isBezierPlaceNbtMode(),
                BezierCurveState.getPointCount(),
                BezierCurveState.getRequiredPointCount(),
                List.copyOf(BezierCurveState.getControlPoints()),
                BuilderHelperClientConfig.getRandomPlacementRadius(),
                BuilderHelperClientConfig.getRandomPlacementHeightMin(),
                BuilderHelperClientConfig.getRandomPlacementHeightMax(),
                BuilderHelperClientConfig.getRandomPlacementCount(),
                BuilderHelperClientConfig.getRandomPlacementRotationChance(),
                AreaSelectionState.getFirstCorner(),
                AreaSelectionState.getSecondCorner(),
                AreaSelectionState.getActiveCorner(),
                SchematicPlacementState.hasPlacement(),
                SchematicPlacementState.getLinearArrayAxisName(),
                SchematicPlacementState.getLinearArrayCount(),
                SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.X),
                SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.Y),
                SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.Z),
                SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.X),
                SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Y),
                SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Z),
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

    private static String getBezierPlacementModeText(ToolMode mode) {
        if (mode != ToolMode.BEZIER_CURVE_GENERATION) {
            return "";
        }

        String modeKey = BuilderHelperClientConfig.isBezierPlaceNbtMode()
                ? "iterablock.tool.bezier.placement_mode.nbt"
                : "iterablock.tool.bezier.placement_mode.block";
        return Lang.tr("iterablock.tool.bezier.placement_mode", Lang.tr(modeKey));
    }

    private static int getModeTextColor(ToolMode mode) {
        if (mode == ToolMode.SCHEMATIC_PLACEMENT) {
            return 0x9AF5B0;
        }

        if (mode == ToolMode.LINEAR_ARRAY
                || mode == ToolMode.VOLUME_ARRAY
                || mode == ToolMode.RANDOM_SCHEMATIC_PLACEMENT
                || mode == ToolMode.BEZIER_CURVE_GENERATION
                || mode == ToolMode.SYMMETRY_PLACEMENT) {
            return 0xFFE680;
        }

        return 0xFFFFFF;
    }

    private void drawBezierSampleCount(GuiGraphics guiGraphics, Font font) {
        BezierSampleCacheKey key = new BezierSampleCacheKey(
                List.copyOf(BezierCurveState.getControlPoints()),
                BezierCurveState.getRequiredPointCount(),
                BuilderHelperClientConfig.getBezierPlacementPrecision()
        );

        if (!key.equals(this.cachedBezierSampleKey)) {
            this.cachedBezierSampleKey = key;
            this.cachedBezierSampleText = Lang.tr("iterablock.tool.bezier.sample_points", BezierCurveState.getSamplePointCount());
            this.cachedBezierSampleWidth = font.width(this.cachedBezierSampleText);
        }

        String text = this.cachedBezierSampleText;
        int scaledWidth = Math.round(guiGraphics.guiWidth() / HUD_SCALE);
        int x = scaledWidth - this.cachedBezierSampleWidth - Math.round(8 / HUD_SCALE);
        int y = Math.round(8 / HUD_SCALE);
        int width = this.cachedBezierSampleWidth + 10;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(HUD_SCALE, HUD_SCALE, 1.0F);
        guiGraphics.fill(x - 4, y - 4, x + width, y + 12, 0x8A071018);
        guiGraphics.fill(x - 4, y - 4, x + width, y - 3, 0xB6F3C6D3);
        guiGraphics.drawString(font, text, x, y, 0xFFF3EECF, true);
        guiGraphics.pose().popPose();
    }

    private static String getArrayText(Minecraft minecraft, ToolMode mode) {
        if (minecraft.player == null) {
            return "";
        }

        if (mode == ToolMode.AREA_COPY_PASTE) {
            if (!AreaSelectionState.hasFirstCorner() && !AreaSelectionState.hasSecondCorner()) {
                return Lang.tr("iterablock.tool.area.empty");
            }

            if (!AreaSelectionState.hasSelection()) {
                return Lang.tr("iterablock.tool.area.partial", formatPos(AreaSelectionState.getFirstCorner()), formatPos(AreaSelectionState.getSecondCorner()));
            }

            return Lang.tr("iterablock.tool.area.selection", formatPos(AreaSelectionState.getFirstCorner()), formatPos(AreaSelectionState.getSecondCorner()), AreaSelectionState.getSizeX(), AreaSelectionState.getSizeY(), AreaSelectionState.getSizeZ());
        }

        if (mode == ToolMode.BEZIER_CURVE_GENERATION) {
            return Lang.tr("iterablock.tool.bezier.points", BezierCurveState.getPointCount(), BezierCurveState.getRequiredPointCount());
        }

        if (mode == ToolMode.RANDOM_SCHEMATIC_PLACEMENT) {
            return Lang.tr("iterablock.tool.random.params",
                    BuilderHelperClientConfig.getRandomPlacementRadius(),
                    BuilderHelperClientConfig.getRandomPlacementHeightMin(),
                    BuilderHelperClientConfig.getRandomPlacementHeightMax(),
                    BuilderHelperClientConfig.getRandomPlacementCount(),
                    BuilderHelperClientConfig.getRandomPlacementRotationChance());
        }

        if (mode == ToolMode.SYMMETRY_PLACEMENT) {
            if (!SymmetryPlacementState.hasCenter()) {
                return Lang.tr("iterablock.tool.symmetry.empty");
            }

            net.minecraft.core.BlockPos center = SymmetryPlacementState.getCenter();
            return Lang.tr("iterablock.tool.symmetry.params",
                    Lang.tr(SymmetryPlacementState.getKind().translationKey()),
                    Lang.tr(SymmetryPlacementState.getParity().translationKey()),
                    center.getX(),
                    center.getY(),
                    center.getZ(),
                    SymmetryPlacementState.getRadius(),
                    SymmetryPlacementState.getHeight(),
                    Lang.tr(SymmetryPlacementState.isLocked() ? "iterablock.tool.symmetry.locked" : "iterablock.tool.symmetry.editing"),
                    Lang.tr(SymmetryPlacementState.isEnabled() ? "iterablock.tool.symmetry.enabled" : "iterablock.tool.symmetry.paused"));
        }

        if (!SchematicPlacementState.hasPlacement()) {
            return "";
        }

        SchematicPlacementState.Axis lookAxis = SchematicPlacementState.getLookAxis(minecraft.player.getLookAngle());

        if (mode == ToolMode.LINEAR_ARRAY) {
            String axis = SchematicPlacementState.getLinearArrayAxisName();
            int count = SchematicPlacementState.getLinearArrayCount();
            return Lang.tr("iterablock.tool.array.linear", formatAxisName(axis, lookAxis), count);
        }

        if (mode == ToolMode.VOLUME_ARRAY) {
            return Lang.tr("iterablock.tool.array.volume",
                    formatArrayValue(SchematicPlacementState.Axis.X, lookAxis),
                    formatArrayValue(SchematicPlacementState.Axis.Y, lookAxis),
                    formatArrayValue(SchematicPlacementState.Axis.Z, lookAxis));
        }

        return "";
    }

    private static String getOverlapText(Minecraft minecraft, ToolMode mode) {
        if (minecraft.player == null || !SchematicPlacementState.hasPlacement()) {
            return "";
        }

        if (mode == ToolMode.LINEAR_ARRAY) {
            SchematicPlacementState.Axis axis = SchematicPlacementState.getLookAxis(minecraft.player.getLookAngle());
            return Lang.tr("iterablock.tool.overlap.single", axis.name(), SchematicPlacementState.getOverlap(axis));
        }

        if (mode == ToolMode.VOLUME_ARRAY) {
            SchematicPlacementState.Axis lookAxis = SchematicPlacementState.getLookAxis(minecraft.player.getLookAngle());
            return Lang.tr("iterablock.tool.overlap.all",
                    formatAxisValue(SchematicPlacementState.Axis.X, lookAxis),
                    formatAxisValue(SchematicPlacementState.Axis.Y, lookAxis),
                    formatAxisValue(SchematicPlacementState.Axis.Z, lookAxis));
        }

        return "";
    }

    private static String formatAxisValue(SchematicPlacementState.Axis axis, SchematicPlacementState.Axis lookAxis) {
        return formatAxisName(axis.name(), lookAxis) + ": " + SchematicPlacementState.getOverlap(axis);
    }

    private static String formatArrayValue(SchematicPlacementState.Axis axis, SchematicPlacementState.Axis lookAxis) {
        return formatAxisName(axis.name(), lookAxis) + ": " + SchematicPlacementState.getVolumeArrayCount(axis);
    }

    private static String formatAxisName(String axis, SchematicPlacementState.Axis lookAxis) {
        String name = switch (axis) {
            case "X" -> "X\u8f74";
            case "Y" -> "Y\u8f74";
            case "Z" -> "Z\u8f74";
            default -> axis;
        };

        return axis.equals(lookAxis.name()) ? "\u00A7a" + name + "\u00A7f" : name;
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

    private record HudLine(String text, int color) {
    }

    private record HudLayout(List<HudLine> lines, int width, int height) {
    }

    private record BezierSampleCacheKey(List<BlockPos> controlPoints, int requiredPoints, int precision) {
    }

    private record HudCacheKey(
            ToolMode mode,
            LoadedLitematicManager.Entry litematic,
            String lastAction,
            SchematicPlacementState.Axis lookAxis,
            boolean bezierPlaceNbtMode,
            int bezierPointCount,
            int bezierRequiredPointCount,
            List<BlockPos> bezierControlPoints,
            int randomRadius,
            int randomHeightMin,
            int randomHeightMax,
            int randomCount,
            int randomRotationChance,
            BlockPos areaFirstCorner,
            BlockPos areaSecondCorner,
            AreaSelectionState.Corner areaActiveCorner,
            boolean hasPlacement,
            String linearAxis,
            int linearCount,
            int volumeX,
            int volumeY,
            int volumeZ,
            int overlapX,
            int overlapY,
            int overlapZ,
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
