package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.gui.AbilitiesMenu;
import com.halilibrahim.halilibrahimmmo.gui.GuiManager;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * {@code /kabiliyet} opens the ability-selection menu directly when typed
 * bare; {@code /kabiliyet <id>} still activates that ability immediately,
 * unchanged from before.
 */
public final class AbilityCommand implements BasicCommand {

    private final AbilityService abilities;
    private final EnergyService energy;
    private final MessageService messages;
    private final GuiServices guiServices;

    public AbilityCommand(AbilityService abilities, EnergyService energy, MessageService messages, GuiServices guiServices) {
        this.abilities = abilities;
        this.energy = energy;
        this.messages = messages;
        this.guiServices = guiServices;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (args.length != 1) {
            GuiManager.open(player, new AbilitiesMenu(guiServices));
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
