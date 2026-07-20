package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeResult;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Explicit two-step prestige command to avoid accidental global XP resets. */
public final class PrestigeCommand implements BasicCommand {

    private final JavaPlugin plugin;
    private final PrestigeService prestige;
    private final ProfileService profiles;
    private final ProgressionService progression;
    private final TitleService titles;
    private final CosmeticService cosmetics;
    private final FeedbackService feedback;
    private final MessageService messages;

    public PrestigeCommand(
            JavaPlugin plugin,
            PrestigeService prestige,
            ProfileService profiles,
            ProgressionService progression,
            TitleService titles,
            CosmeticService cosmetics,
            FeedbackService feedback,
            MessageService messages
    ) {
        this.plugin = plugin;
        this.prestige = prestige;
        this.profiles = profiles;
        this.progression = progression;
        this.titles = titles;
        this.cosmetics = cosmetics;
        this.feedback = feedback;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("onayla")) {
            confirm(player);
            return;
        }
        profiles.load(player.getUniqueId(), player.getName()).thenAccept(profile ->
                player.getScheduler().run(plugin, task -> messages.send(player, MessageKey.PRESTIGE_STATUS, Map.of(
                        "seviye", Integer.toString(profile.globalLevel()),
                        "gereken", Integer.toString(progression.settings().curve().maximumLevel()),
                        "prestij", Integer.toString(profile.prestige()),
                        "maksimum", Integer.toString(prestige.settings().maximumPrestige())
                )), null));
    }

    private void confirm(Player player) {
        prestige.prestige(player.getUniqueId(), player.getName()).thenAccept(result ->
                player.getScheduler().run(plugin, task -> {
                    MessageKey key = switch (result.status()) {
                        case PRESTIGED -> MessageKey.PRESTIGE_SUCCESS;
                        case LEVEL_REQUIRED -> MessageKey.PRESTIGE_LEVEL_REQUIRED;
                        case MAXIMUM_REACHED -> MessageKey.PRESTIGE_MAXIMUM;
                    };
                    messages.send(player, key, Map.of(
                            "prestij", Integer.toString(result.prestige()),
                            "maksimum", Integer.toString(prestige.settings().maximumPrestige())
                    ));
                    if (result.status() == PrestigeResult.Status.PRESTIGED) {
                        feedback.celebratePrestige(player, result.prestige());
                        titles.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyTitles(player, unlocked));
                        cosmetics.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyCosmetics(player, unlocked));
                    }
                }, null));
    }

    private void notifyTitles(Player player, List<TitleDefinition> unlocked) {
        if (unlocked.isEmpty()) {
            return;
        }
        player.getScheduler().run(plugin, task -> unlocked.forEach(definition ->
                messages.send(player, MessageKey.TITLE_UNLOCKED, Map.of("unvan", definition.displayName()))), null);
    }

    private void notifyCosmetics(Player player, List<CosmeticDefinition> unlocked) {
        if (unlocked.isEmpty()) {
            return;
        }
        player.getScheduler().run(plugin, task -> unlocked.forEach(definition ->
                messages.send(player, MessageKey.COSMETIC_UNLOCKED, Map.of("kozmetik", definition.displayName()))), null);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return args.length == 1
                ? List.of("onayla").stream().filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList()
                : List.of();
    }
}
