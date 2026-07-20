package com.halilibrahim.halilibrahimmmo.command.subcommand;

import com.halilibrahim.halilibrahimmmo.command.SubCommand;
import com.halilibrahim.halilibrahimmmo.config.ConfigService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * {@code /rpg hataayiklama} - flips verbose debug logging for the current session
 * without touching {@code config.yml} on disk. The override is cleared the next
 * time the config is reloaded from disk.
 */
public final class DebugToggleSubCommand implements SubCommand {

    private final ConfigService configService;
    private final MessageService messageService;

    public DebugToggleSubCommand(ConfigService configService, MessageService messageService) {
        this.configService = configService;
        this.messageService = messageService;
    }

    @Override
    public String name() {
        return "hataayiklama";
    }

    @Override
    public String permission() {
        return "halilibrahimmmo.admin";
    }

    @Override
    public void execute(CommandSourceStack sourceStack, String[] args) {
        boolean newState = !configService.coreConfig().debugMode();
        configService.setDebugModeOverride(newState);
        messageService.send(sourceStack.getSender(), newState ? MessageKey.DEBUG_ENABLED : MessageKey.DEBUG_DISABLED);
    }
}
