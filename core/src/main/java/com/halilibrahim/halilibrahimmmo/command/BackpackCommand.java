package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.backpack.BackpackService;
import com.halilibrahim.halilibrahimmmo.gui.PrestigeRewardGate;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.progression.FeatureUnlockRules;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** {@code /çanta} - opens the player's cloud chest. */
public final class BackpackCommand implements BasicCommand {

    private final JavaPlugin plugin;
    private final BackpackService backpacks;
    private final MessageService messages;
    private final PrestigeRewardGate prestigeGate;

    public BackpackCommand(JavaPlugin plugin, BackpackService backpacks, MessageService messages, PrestigeRewardGate prestigeGate) {
        this.plugin = plugin;
        this.backpacks = backpacks;
        this.messages = messages;
        this.prestigeGate = prestigeGate;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            messages.send(sender, MessageKey.PLAYER_ONLY);
            return;
        }
        // The backpack opens a real inventory rather than a GuiView, so it does
        // not pass through GuiManager's guard and has to ask for itself.
        if (prestigeGate.hasPendingChoice(player)) {
            prestigeGate.send(player);
            return;
        }
        if (!backpacks.isUnlocked(player.getUniqueId())) {
            messages.send(player, MessageKey.BACKPACK_LOCKED, Map.of(
                    "seviye", Integer.toString(FeatureUnlockRules.BACKPACK_CLASS_LEVEL)
            ));
            return;
        }
        // The read is a database round trip, so the window is opened back on the
        // player's own region once it resolves - the same hop every async
        // command in this codebase makes before touching Bukkit API.
        backpacks.open(player).thenAccept(inventory -> player.getScheduler().run(plugin, task -> {
            player.openInventory(inventory);
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.8F, 1.2F);
            // Personal chat confirmation, matching what /ev and /takas already
            // do: the window opening is the obvious part, but the chat line is
            // what stays on screen after it is closed again.
            messages.send(player, MessageKey.BACKPACK_OPENED);
        }, null));
    }
}
