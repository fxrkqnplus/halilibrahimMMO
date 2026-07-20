package com.halilibrahim.halilibrahimmmo.command.subcommand;

import com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI;
import com.halilibrahim.halilibrahimmmo.command.SubCommand;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Map;

/**
 * {@code /rpg versiyon} - prints the running plugin version and the Paper/Minecraft
 * API version it was built against.
 */
public final class VersionSubCommand implements SubCommand {

    private final HalilIbrahimMMOAPI api;
    private final MessageService messageService;

    public VersionSubCommand(HalilIbrahimMMOAPI api, MessageService messageService) {
        this.api = api;
        this.messageService = messageService;
    }

    @Override
    public String name() {
        return "versiyon";
    }

    @Override
    public String permission() {
        return "halilibrahimmmo.admin";
    }

    @Override
    public void execute(CommandSourceStack sourceStack, String[] args) {
        messageService.send(sourceStack.getSender(), MessageKey.VERSION_INFO, Map.of(
                "surum", api.getPluginVersion(),
                "paper_surumu", api.getSupportedMinecraftVersion()
        ));
    }
}
