package com.iterablock.client.gui;

import com.iterablock.IteraBlock;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class GuiTextures {
    private static final int ICON_TEXTURE_SIZE = 16;

    private GuiTextures() {
    }

    public static void drawIcon(GuiGraphics guiGraphics, String name, int x, int y, int size) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(IteraBlock.MODID, "textures/gui/icons/" + name + ".png");
        guiGraphics.blit(texture, x, y, size, size, 0.0F, 0.0F, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
    }
}
