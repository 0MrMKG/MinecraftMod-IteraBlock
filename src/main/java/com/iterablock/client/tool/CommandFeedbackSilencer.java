package com.iterablock.client.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

public final class CommandFeedbackSilencer {
    private static final CommandFeedbackSilencer INSTANCE = new CommandFeedbackSilencer();
    private static final long SILENCE_WINDOW_MILLIS = 15_000L;

    private int remainingMessages;
    private long silenceUntilMillis;

    private CommandFeedbackSilencer() {
    }

    public static CommandFeedbackSilencer getInstance() {
        return INSTANCE;
    }

    public void expectPlacementFeedback(int commandCount) {
        if (commandCount <= 0) {
            return;
        }

        this.remainingMessages += commandCount;
        this.silenceUntilMillis = System.currentTimeMillis() + SILENCE_WINDOW_MILLIS;
    }

    public void clear() {
        this.remainingMessages = 0;
        this.silenceUntilMillis = 0L;
    }

    @SubscribeEvent
    public void onClientSystemMessage(ClientChatReceivedEvent.System event) {
        if (this.remainingMessages <= 0 || System.currentTimeMillis() > this.silenceUntilMillis) {
            this.clear();
            return;
        }

        if (!isPlacementCommandFeedback(event.getMessage())) {
            return;
        }

        this.remainingMessages--;
        event.setCanceled(true);
    }

    private static boolean isPlacementCommandFeedback(Component message) {
        if (message.getContents() instanceof TranslatableContents contents) {
            return switch (contents.getKey()) {
                case "commands.setblock.success",
                     "commands.setblock.failed",
                     "commands.execute.conditional.fail",
                     "commands.execute.conditional.fail_count" -> true;
                default -> false;
            };
        }

        return false;
    }
}
