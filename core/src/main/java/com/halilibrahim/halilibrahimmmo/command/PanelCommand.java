package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/** {@code /panel} toggles the invoking player's right-hand profile sidebar. */
public final class PanelCommand implements BasicCommand {

    private final SidebarService sidebar;
    private final MessageService messages;

    public PanelCommand(SidebarService sidebar, MessageService messages) {
        this.sidebar = sidebar;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        boolean visible = sidebar.toggle(player);
        messages.send(player, visible ? MessageKey.PANEL_ENABLED : MessageKey.PANEL_DISABLED);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        return List.of();
    }
}
