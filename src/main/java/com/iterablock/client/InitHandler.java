package com.iterablock.client;

import com.iterablock.client.gui.HudVisibilityHandler;
import com.iterablock.client.hotkeys.InputHandler;
import com.iterablock.client.tool.CommandFeedbackSilencer;
import com.iterablock.client.tool.SchematicProjectionRenderer;
import com.iterablock.client.tool.SymmetryPlacementHandler;
import com.iterablock.client.tool.ToolHudRenderer;
import com.iterablock.client.tool.ToolInputHandler;

import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.neoforged.neoforge.common.NeoForge;

public class InitHandler implements IInitializationHandler {
    public static void register() {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }

    @Override
    public void registerModHandlers() {
        InputHandler.getInstance().init();
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(ToolInputHandler.getInstance());
        InputEventHandler.getInputManager().registerMouseInputHandler(ToolInputHandler.getInstance());
        RenderEventHandler.getInstance().registerGameOverlayRenderer(ToolHudRenderer.getInstance());
        NeoForge.EVENT_BUS.register(WorldSessionHandler.getInstance());
        NeoForge.EVENT_BUS.register(HudVisibilityHandler.getInstance());
        NeoForge.EVENT_BUS.register(SchematicProjectionRenderer.getInstance());
        NeoForge.EVENT_BUS.register(SymmetryPlacementHandler.getInstance());
        NeoForge.EVENT_BUS.register(CommandFeedbackSilencer.getInstance());
    }
}
