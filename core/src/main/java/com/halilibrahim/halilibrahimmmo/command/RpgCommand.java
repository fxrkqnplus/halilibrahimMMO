package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The {@code /rpg} umbrella command - HalilIbrahimMMO's general-purpose
 * administrative and diagnostic entry point. Player-facing gameplay commands
 * ({@code /profil}, {@code /yetenekler}, ...) are registered as their own root
 * commands in later phases; {@code /rpg} stays reserved for cross-cutting
 * plugin-management actions.
 */
public final class RpgCommand implements BasicCommand {

    private final Map<String, SubCommand> subCommandsByName;
    private final MessageService messageService;

    public RpgCommand(List<SubCommand> subCommands, MessageService messageService) {
        Map<String, SubCommand> ordered = new LinkedHashMap<>();
        for (SubCommand subCommand : subCommands) {
            ordered.put(subCommand.name(), subCommand);
        }
        this.subCommandsByName = Map.copyOf(ordered);
        this.messageService = messageService;
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        if (args.length == 0) {
            messageService.send(commandSourceStack.getSender(), MessageKey.UNKNOWN_SUBCOMMAND);
            return;
        }

        SubCommand subCommand = subCommandsByName.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            messageService.send(commandSourceStack.getSender(), MessageKey.UNKNOWN_SUBCOMMAND);
            return;
        }

        if (!commandSourceStack.getSender().hasPermission(subCommand.permission())) {
            messageService.send(commandSourceStack.getSender(), MessageKey.NO_PERMISSION);
            return;
        }

        subCommand.execute(commandSourceStack, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String partial = args[0].toLowerCase(Locale.ROOT);
        return subCommandsByName.keySet().stream()
                .filter(name -> name.startsWith(partial))
                .toList();
    }

    @Override
    public String permission() {
        return "halilibrahimmmo.admin";
    }
}
