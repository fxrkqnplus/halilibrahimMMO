package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.gui.GuiManager;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
import com.halilibrahim.halilibrahimmmo.gui.PrestigeMenu;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * Opens the prestige screen via {@code /prestij}.
 *
 * <p>Takes no arguments and prints nothing to chat: every one of this
 * plugin's player-facing screens lives in the GUI, and a command is only ever
 * a shortcut to open one.</p>
 */
public final class PrestigeCommand implements BasicCommand {

    private final GuiServices services;

    public PrestigeCommand(GuiServices services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            services.messages().send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        GuiManager.open(player, new PrestigeMenu(services));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return List.of();
    }
}
