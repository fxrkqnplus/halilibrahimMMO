package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.gui.GuiManager;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
import com.halilibrahim.halilibrahimmmo.gui.JournalMenu;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * Opens the adventure journal via {@code /gunluk}.
 *
 * <p>Takes no arguments and prints nothing to chat: every one of this
 * plugin's player-facing screens lives in the GUI, and a command is only ever
 * a shortcut to open one.</p>
 */
public final class JournalCommand implements BasicCommand {

    private final GuiServices services;

    public JournalCommand(GuiServices services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            services.messages().send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        GuiManager.open(player, new JournalMenu(services));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return List.of();
    }
}
