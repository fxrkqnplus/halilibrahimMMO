package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.gui.ClassIntroMenu;
import com.halilibrahim.halilibrahimmmo.gui.GuiManager;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * {@code /mmo aktif} - the one-time onboarding gate. A brand-new player's
 * progression stays completely inert until this runs; success immediately
 * opens the class-introduction menu.
 */
public final class MmoCommand implements BasicCommand {

    private final JavaPlugin plugin;
    private final ActivationService activation;
    private final GuiServices guiServices;
    private final MessageService messages;

    public MmoCommand(JavaPlugin plugin, ActivationService activation, GuiServices guiServices, MessageService messages) {
        this.plugin = plugin;
        this.activation = activation;
        this.guiServices = guiServices;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (args.length != 1 || !args[0].equalsIgnoreCase("aktif")) {
            messages.send(player, MessageKey.MMO_USAGE);
            return;
        }
        if (activation.cached(player.getUniqueId())) {
            messages.send(player, MessageKey.MMO_ALREADY_ACTIVE);
            return;
        }
        activation.activate(player.getUniqueId()).thenAccept(ignored ->
                player.getScheduler().run(plugin, task -> {
                    messages.send(player, MessageKey.MMO_ACTIVATED);
                    GuiManager.open(player, new ClassIntroMenu(guiServices));
                }, null));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length == 1) {
            return List.of("aktif").stream().filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
