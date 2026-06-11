package com.iterablock.client.tool;

import com.iterablock.client.Lang;

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

        if (minecraft.options.hideGui || minecraft.screen != null || minecraft.player == null || !ToolState.hasToolItem(minecraft.player)) {
            return;
        }

        Font font = minecraft.font;
        ToolMode mode = ToolState.getMode();
        String title = Lang.tr("iterablock.tool.title");
        String modeText = Lang.tr("iterablock.tool.mode", mode.getDisplayName());
        String litematicText = ClientToolState.currentLitematic == null ? Lang.tr("iterablock.tool.litematic_none") : Lang.tr("iterablock.tool.litematic", ClientToolState.currentLitematic.displayName());
        String actionText = ToolState.getLastAction();
        int x = Math.round(8 / HUD_SCALE);
        int y = Math.round(8 / HUD_SCALE);
        int width = Math.max(Math.max(font.width(title), font.width(modeText)), Math.max(font.width(litematicText), font.width(actionText))) + 14;
        int height = 52;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(HUD_SCALE, HUD_SCALE, 1.0F);
        guiGraphics.fill(x - 4, y - 4, x + width, y + height, 0x8A071018);
        guiGraphics.fill(x - 4, y - 4, x + width, y - 3, 0xB64EAFC5);
        guiGraphics.drawString(font, title, x, y, 0xD6F4FF, true);
        guiGraphics.drawString(font, modeText, x, y + 12, 0xFFFFFF, true);
        guiGraphics.drawString(font, litematicText, x, y + 24, 0xFFFFFF, true);
        guiGraphics.drawString(font, actionText, x, y + 36, 0xA7D9E6, true);
        guiGraphics.pose().popPose();
    }
}
