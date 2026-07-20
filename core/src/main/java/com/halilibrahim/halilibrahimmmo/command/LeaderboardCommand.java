package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardCategory;
import com.halilibrahim.halilibrahimmmo.leaderboards.LeaderboardEntry;
import com.halilibrahim.halilibrahimmmo.leaderboards.LeaderboardService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Lists the top-ranked players for one fixed category or a configured skill. */
public final class LeaderboardCommand implements BasicCommand {

    private final JavaPlugin plugin;
    private final LeaderboardService leaderboards;
    private final SkillService skills;
    private final MessageService messages;

    public LeaderboardCommand(JavaPlugin plugin, LeaderboardService leaderboards, SkillService skills, MessageService messages) {
        this.plugin = plugin;
        this.leaderboards = leaderboards;
        this.skills = skills;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (args.length == 0) {
            messages.send(player, MessageKey.LEADERBOARD_USAGE);
            return;
        }
        String argument = args[0].toLowerCase(Locale.ROOT);
        LeaderboardCategory category = fixedCategory(argument);
        if (category != null) {
            leaderboards.top(category).thenAccept(entries ->
                    player.getScheduler().run(plugin, task -> sendCategory(player, category, entries), null));
            return;
        }
        Optional<SkillDefinition> skill = skills.definitions().stream()
                .filter(definition -> definition.id().equalsIgnoreCase(argument))
                .findFirst();
        if (skill.isEmpty()) {
            messages.send(player, MessageKey.LEADERBOARD_UNKNOWN);
            return;
        }
        leaderboards.topSkill(skill.get().id()).thenAccept(entries ->
                player.getScheduler().run(plugin, task -> sendSkill(player, skill.get(), entries), null));
    }

    private LeaderboardCategory fixedCategory(String argument) {
        return switch (argument) {
            case "seviye" -> LeaderboardCategory.GLOBAL_LEVEL;
            case "prestij" -> LeaderboardCategory.PRESTIGE;
            case "gorev" -> LeaderboardCategory.QUEST_COMPLETIONS;
            case "koleksiyon" -> LeaderboardCategory.COLLECTION_ENTRIES;
            default -> null;
        };
    }

    private void sendCategory(Player player, LeaderboardCategory category, List<LeaderboardEntry> entries) {
        if (entries.isEmpty()) {
            messages.send(player, MessageKey.LEADERBOARD_EMPTY);
            return;
        }
        MessageKey key = switch (category) {
            case GLOBAL_LEVEL -> MessageKey.LEADERBOARD_LEVEL_LIST;
            case PRESTIGE -> MessageKey.LEADERBOARD_PRESTIGE_LIST;
            case QUEST_COMPLETIONS -> MessageKey.LEADERBOARD_QUEST_LIST;
            case COLLECTION_ENTRIES -> MessageKey.LEADERBOARD_COLLECTION_LIST;
        };
        messages.send(player, key, Map.of("liste", join(entries)));
    }

    private void sendSkill(Player player, SkillDefinition skill, List<LeaderboardEntry> entries) {
        if (entries.isEmpty()) {
            messages.send(player, MessageKey.LEADERBOARD_EMPTY);
            return;
        }
        messages.send(player, MessageKey.LEADERBOARD_SKILL_LIST, Map.of("yetenek", skill.displayName(), "liste", join(entries)));
    }

    private String join(List<LeaderboardEntry> entries) {
        return entries.stream()
                .map(entry -> entry.rank() + ". " + entry.playerName() + " (" + entry.primaryValue() + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> options = new ArrayList<>(List.of("seviye", "prestij", "gorev", "koleksiyon"));
        skills.definitions().forEach(definition -> options.add(definition.id()));
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
