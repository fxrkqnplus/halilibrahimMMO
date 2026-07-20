package com.halilibrahim.halilibrahimmmo.command.subcommand;

import com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI;
import com.halilibrahim.halilibrahimmmo.command.SubCommand;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * {@code /rpg yenile} - reloads {@code config.yml} and {@code messages.yml}.
 * Deliberately calls through the same {@link HalilIbrahimMMOAPI#reload()} that
 * third-party plugins would use, so this command is always guaranteed to behave
 * identically to the public API.
 */
public final class ReloadSubCommand implements SubCommand {

    private final HalilIbrahimMMOAPI api;
    private final MessageService messageService;

    public ReloadSubCommand(HalilIbrahimMMOAPI api, MessageService messageService) {
        this.api = api;
        this.messageService = messageService;
    }

    @Override
    public String name() {
        return "yenile";
    }

    @Override
    public String permission() {
        return "halilibrahimmmo.admin";
    }

    @Override
    public void execute(CommandSourceStack sourceStack, String[] args) {
        boolean success = api.reload();
        messageService.send(sourceStack.getSender(), success ? MessageKey.RELOAD_SUCCESS : MessageKey.RELOAD_FAILURE);
    }
}
