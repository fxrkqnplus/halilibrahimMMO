package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.gui.AdminMenu;
import com.halilibrahim.halilibrahimmmo.gui.GuiManager;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * {@code /haliladmin} - the sole entry point into every administrative action
 * this plugin exposes, entirely GUI-driven (see {@code AdminMenu}). Replaces
 * the old {@code /rpg} chat-argument command; unlike {@code /menu}, this
 * command permission-checks itself before opening anything, since every
 * screen behind it is staff-only.
 */
public final class AdminCommand implements BasicCommand {

    private static final String PERMISSION = "halilibrahimmmo.admin";

    private final GuiServices services;

    public AdminCommand(GuiServices services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            services.messages().send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (!player.hasPermission(PERMISSION)) {
            services.messages().send(player, MessageKey.NO_PERMISSION);
            return;
        }
        GuiManager.open(player, new AdminMenu(services));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return List.of();
    }
}
