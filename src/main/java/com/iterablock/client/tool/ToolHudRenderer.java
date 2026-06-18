package com.iterablock.client.tool;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;

import fi.dy.masa.malilib.interfaces.IRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ToolHudRenderer implements IRenderer {
    private static final ToolHudRenderer INSTANCE = new ToolHudRenderer();
    private static final float HUD_SCALE = 0.75F;

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
        String title = Lang.tr("iterablock.tool.title");
        String plainModeText = Lang.tr("iterablock.tool.mode", mode.getDisplayName());
        String modeText = Lang.tr("iterablock.tool.mode_numbered", formatModeNumber(mode.ordinal() + 1), mode.getDisplayName());
        String litematicText = ClientToolState.currentLitematic == null ? Lang.tr("iterablock.tool.litematic_none") : Lang.tr("iterablock.tool.litematic", ClientToolState.currentLitematic.displayName());
        String bezierPlacementModeText = getBezierPlacementModeText(mode);
        String arrayText = getArrayText(minecraft, mode);
        String overlapText = getOverlapText(minecraft, mode);
        String actionText = ToolState.getLastAction();
        boolean showBezierPlacementMode = !bezierPlacementModeText.isEmpty();
        boolean showArray = !arrayText.isEmpty();
        boolean showOverlap = !overlapText.isEmpty();
        boolean showAction = actionText != null && !actionText.isBlank() && !actionText.equals(modeText) && !actionText.equals(plainModeText) && !actionText.equals(arrayText);
        int x = Math.round(8 / HUD_SCALE);
        int y = Math.round(8 / HUD_SCALE);
        int bezierPlacementModeWidth = showBezierPlacementMode ? font.width(bezierPlacementModeText) : 0;
        int actionWidth = showAction ? font.width(actionText) : 0;
        int arrayWidth = showArray ? font.width(arrayText) : 0;
        int overlapWidth = showOverlap ? font.width(overlapText) : 0;
        int width = Math.max(Math.max(Math.max(font.width(title), font.width(modeText)), font.width(litematicText)), Math.max(Math.max(Math.max(actionWidth, arrayWidth), overlapWidth), bezierPlacementModeWidth)) + 14;
        int height = 40 + (showBezierPlacementMode ? 12 : 0) + (showArray ? 12 : 0) + (showOverlap ? 12 : 0) + (showAction ? 12 : 0);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(HUD_SCALE, HUD_SCALE, 1.0F);
        guiGraphics.fill(x - 4, y - 4, x + width, y + height, 0x8A071018);
        guiGraphics.fill(x - 4, y - 4, x + width, y - 3, 0xB64EAFC5);
        guiGraphics.drawString(font, title, x, y, 0xD6F4FF, true);
        guiGraphics.drawString(font, modeText, x, y + 12, getModeTextColor(mode), true);
        guiGraphics.drawString(font, litematicText, x, y + 24, 0xFFFFFF, true);

        int nextLineY = y + 36;
        if (showBezierPlacementMode) {
            guiGraphics.drawString(font, bezierPlacementModeText, x, nextLineY, 0xFFF3EECF, true);
            nextLineY += 12;
        }

        if (showArray) {
            guiGraphics.drawString(font, arrayText, x, nextLineY, 0xD6F4FF, true);
            nextLineY += 12;
        }

        if (showOverlap) {
            guiGraphics.drawString(font, overlapText, x, nextLineY, 0xE8F6FF, true);
            nextLineY += 12;
        }

        if (showAction) {
            guiGraphics.drawString(font, actionText, x, nextLineY, 0xA7D9E6, true);
        }

        guiGraphics.pose().popPose();

        if (mode == ToolMode.BEZIER_CURVE_GENERATION) {
            drawBezierSampleCount(guiGraphics, font);
        }
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

    private static void drawBezierSampleCount(GuiGraphics guiGraphics, Font font) {
        String text = Lang.tr("iterablock.tool.bezier.sample_points", BezierCurveState.getSamplePointCount());
        int scaledWidth = Math.round(guiGraphics.guiWidth() / HUD_SCALE);
        int x = scaledWidth - font.width(text) - Math.round(8 / HUD_SCALE);
        int y = Math.round(8 / HUD_SCALE);
        int width = font.width(text) + 10;

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
                    center.getX(),
                    center.getY(),
                    center.getZ(),
                    SymmetryPlacementState.getRadius(),
                    SymmetryPlacementState.getHeight(),
                    Lang.tr(SymmetryPlacementState.isLocked() ? "iterablock.tool.symmetry.locked" : "iterablock.tool.symmetry.editing"));
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
}
