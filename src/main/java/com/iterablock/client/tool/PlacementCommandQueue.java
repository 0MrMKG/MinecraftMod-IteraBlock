package com.iterablock.client.tool;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class PlacementCommandQueue {
    private static final PlacementCommandQueue INSTANCE = new PlacementCommandQueue();
    private static final int COMMANDS_PER_TICK = 128;

    private final Queue<String> commands = new ArrayDeque<>();

    private PlacementCommandQueue() {
    }

    public static PlacementCommandQueue getInstance() {
        return INSTANCE;
    }

    public void enqueue(List<String> newCommands) {
        if (newCommands.isEmpty()) {
            return;
        }

        this.commands.addAll(newCommands);
    }

    public void clear() {
        this.commands.clear();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (this.commands.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.getConnection() == null) {
            this.clear();
            return;
        }

        int sent = 0;

        while (sent < COMMANDS_PER_TICK && !this.commands.isEmpty()) {
            player.connection.sendCommand(this.commands.remove());
            sent++;
        }

        CommandFeedbackSilencer.getInstance().expectPlacementFeedback(sent);
    }
}
