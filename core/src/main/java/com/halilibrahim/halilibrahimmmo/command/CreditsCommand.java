package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.gui.CreditsMenu;
import com.halilibrahim.halilibrahimmmo.gui.GuiManager;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * Opens the Emeği Geçenler screen directly.
 *
 * <p>Three labels for one screen, the same way {@code /yardım} has three: a
 * player who wants to know who made the server types whichever of
 * {@code /credits}, {@code /yapımcı} or {@code /halilibrahimmmo} first occurs to
 * them, and none of the three is a wrong guess. The screen itself was already
 * reachable from {@code /menü}; this only removes the two clicks.</p>
 *
 * <p>Opened with {@code fromAdmin = false}, so the back button returns to the
 * player hub - which is where somebody who typed the command rather than
 * arriving from the admin panel expects to be put down.</p>
 */
public final class CreditsCommand implements BasicCommand {

    private final GuiServices services;

    public CreditsCommand(GuiServices services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            services.messages().send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        GuiManager.open(player, new CreditsMenu(services, false));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return List.of();
    }
}
