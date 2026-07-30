package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.home.HomeActionResult;
import com.halilibrahim.halilibrahimmmo.home.HomeService;
import com.halilibrahim.halilibrahimmmo.locale.DurationFormatter;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.progression.FeatureUnlockRules;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * {@code /ev} (alias {@code /home}): teleports to the player's home, or with
 * {@code ayarla <isim>} sets one where they stand, or with {@code sil} removes it.
 *
 * <p><strong>One of only two commands in this plugin that still takes
 * arguments</strong> - the rest of the surface has been GUI-only since Phase
 * 14. A home carries a player-chosen name, and free text is precisely the one
 * thing an inventory menu cannot collect: there is no click that spells
 * "Dağ Evi". The bare form still needs no argument at all, so the common case
 * (going home) stays a single word.</p>
 */
public final class HomeCommand implements BasicCommand {

    private static final String SET_ACTION = "ayarla";
    private static final String DELETE_ACTION = "sil";

    private final JavaPlugin plugin;
    private final HomeService homes;
    private final MessageService messages;
    private final SidebarService sidebar;
    private final FeedbackService feedback;

    public HomeCommand(JavaPlugin plugin, HomeService homes, MessageService messages, SidebarService sidebar, FeedbackService feedback) {
        this.plugin = plugin;
        this.homes = homes;
        this.messages = messages;
        this.sidebar = sidebar;
        this.feedback = feedback;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        // The level check comes first, before the arguments are even looked at.
        // A player who has not unlocked homes used to be told how to set and
        // delete one - the usage line is help for a feature, and reading it out
        // to somebody who cannot use the feature is noise that also looks like
        // the command half-worked.
        if (!homes.isUnlocked(player.getUniqueId())) {
            sendLocked(player);
            return;
        }
        if (args.length == 0) {
            onPlayerRegion(player, homes.teleportHome(player), result -> reportTeleport(player, result));
            return;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals(DELETE_ACTION)) {
            onPlayerRegion(player, homes.deleteHome(player), result -> reportDelete(player, result));
            return;
        }
        if (!action.equals(SET_ACTION)) {
            messages.send(player, MessageKey.HOME_USAGE);
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (name.isEmpty()) {
            messages.send(player, MessageKey.HOME_USAGE);
            return;
        }
        onPlayerRegion(player, homes.setHome(player, name), result -> reportSet(player, name, result));
    }

    /** Every reply is composed on the player's own region, since it also refreshes their sidebar. */
    private void onPlayerRegion(Player player, CompletableFuture<HomeActionResult> future, Consumer<HomeActionResult> report) {
        future.thenAccept(result -> player.getScheduler().run(plugin, task -> {
            report.accept(result);
            sidebar.refresh(player);
        }, null));
    }

    private void reportTeleport(Player player, HomeActionResult result) {
        switch (result) {
            case SUCCESS -> homes.cached(player.getUniqueId()).ifPresent(home -> {
                messages.send(player, MessageKey.HOME_TELEPORTED, Map.of("ev", home.name()));
                feedback.celebrateHomeTeleport(player);
            });
            case LOCKED -> sendLocked(player);
            case NOT_SET -> messages.send(player, MessageKey.HOME_NOT_SET);
            case FORBIDDEN_WORLD -> messages.send(player, MessageKey.HOME_FORBIDDEN_WORLD);
            case WORLD_MISSING -> messages.send(player, MessageKey.HOME_WORLD_MISSING);
            case INVALID_NAME -> messages.send(player, MessageKey.HOME_INVALID_NAME);
            case COOLDOWN -> messages.send(player, MessageKey.HOME_COOLDOWN, Map.of(
                    "sure", DurationFormatter.ofMillis(messages, homes.teleportCooldownRemaining(player.getUniqueId()))
            ));
        }
    }

    private void reportSet(Player player, String name, HomeActionResult result) {
        switch (result) {
            case SUCCESS -> {
                messages.send(player, MessageKey.HOME_SET, Map.of("ev", name));
                feedback.celebrateHomeSet(player);
            }
            case LOCKED -> sendLocked(player);
            case FORBIDDEN_WORLD -> messages.send(player, MessageKey.HOME_FORBIDDEN_WORLD);
            case INVALID_NAME -> messages.send(player, MessageKey.HOME_INVALID_NAME);
            // Setting a home is never rate limited, so COOLDOWN cannot reach
            // here; it shares the catch-all with the other unreachable results.
            case NOT_SET, WORLD_MISSING, COOLDOWN -> messages.send(player, MessageKey.HOME_USAGE);
        }
    }

    private void reportDelete(Player player, HomeActionResult result) {
        if (result == HomeActionResult.SUCCESS) {
            messages.send(player, MessageKey.HOME_DELETED);
            return;
        }
        messages.send(player, MessageKey.HOME_NOT_SET);
    }

    private void sendLocked(Player player) {
        messages.send(player, MessageKey.HOME_LOCKED, Map.of(
                "seviye", Integer.toString(FeatureUnlockRules.HOME_CLASS_LEVEL)
        ));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length > 1 || !(stack.getSender() instanceof Player player) || !homes.isUnlocked(player.getUniqueId())) {
            return List.of();
        }
        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        return List.of(SET_ACTION, DELETE_ACTION).stream().filter(action -> action.startsWith(prefix)).toList();
    }
}
