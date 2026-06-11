package com.iterablock.client.gui;

import com.iterablock.client.gui.settings.GuiBuilderHelperSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public class HudVisibilityHandler {
    private static final HudVisibilityHandler INSTANCE = new HudVisibilityHandler();

    private HudVisibilityHandler() {
    }

    public static HudVisibilityHandler getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (this.shouldHideLayer(event.getName(), Minecraft.getInstance().screen)) {
            event.setCanceled(true);
        }
    }

    private boolean shouldHideLayer(ResourceLocation layer, Screen screen) {
        if (!this.isBuilderHelperScreen(screen)) {
            return false;
        }

        return VanillaGuiLayers.CROSSHAIR.equals(layer)
                || VanillaGuiLayers.HOTBAR.equals(layer)
                || VanillaGuiLayers.SELECTED_ITEM_NAME.equals(layer)
                || VanillaGuiLayers.EXPERIENCE_BAR.equals(layer)
                || VanillaGuiLayers.EXPERIENCE_LEVEL.equals(layer);
    }

    private boolean isBuilderHelperScreen(Screen screen) {
        return screen instanceof GuiRadialMenu
                || screen instanceof GuiIteraBlockMainMenu
                || screen instanceof GuiLoadedLitematicList
                || screen instanceof GuiBuilderHelperMainMenu
                || screen instanceof GuiBuilderHelperSettings;
    }
}
