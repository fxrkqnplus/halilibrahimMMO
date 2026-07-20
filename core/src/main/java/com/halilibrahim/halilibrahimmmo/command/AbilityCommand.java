package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Activates one configured player ability through {@code /kabiliyet <id>}. */
public final class AbilityCommand implements BasicCommand {

    private final AbilityService abilities;
    private final EnergyService energy;
    private final MessageService messages;

    public AbilityCommand(AbilityService abilities, EnergyService energy, MessageService messages) {
        this.abilities = abilities;
        this.energy = energy;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (args.length != 1) {
            String available = abilities.definitions().stream().map(AbilityDefinition::id).sorted().reduce((left, right) -> left + ", " + right).orElse("-");
            messages.send(player, MessageKey.ABILITY_LIST, Map.of("kabiliyetler", available));
            return;
        }
        abilities.activate(player, args[0].toLowerCase(Locale.ROOT), energy);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String partial = args[0].toLowerCase(Locale.ROOT);
        return abilities.definitions().stream().map(AbilityDefinition::id)
                .filter(id -> id.startsWith(partial)).sorted().toList();
    }
}
