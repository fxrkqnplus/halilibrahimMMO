package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.gui.GuiManager;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
import com.halilibrahim.halilibrahimmmo.gui.GuideMenu;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** {@code /yardım} (also {@code /bilgi}, {@code /rehber}) - opens the in-game handbook. */
public final class GuideCommand implements BasicCommand {

    private final GuiServices services;

    public GuideCommand(GuiServices services) {
        this.services = services;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            services.messages().send(sender, MessageKey.PLAYER_ONLY);
            return;
        }
        GuiManager.open(player, new GuideMenu(services, false));
    }
}
