package com.iterablock.client;

import com.iterablock.client.template.LoadedLitematicManager;
import com.iterablock.client.tool.AreaSelectionState;
import com.iterablock.client.tool.BezierCurveState;
import com.iterablock.client.tool.CommandFeedbackSilencer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public class WorldSessionHandler {
    private static final WorldSessionHandler INSTANCE = new WorldSessionHandler();

    private WorldSessionHandler() {
    }

    public static WorldSessionHandler getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LoadedLitematicManager.clearAll();
        AreaSelectionState.clear();
        BezierCurveState.clear();
        CommandFeedbackSilencer.getInstance().clear();
    }
}
