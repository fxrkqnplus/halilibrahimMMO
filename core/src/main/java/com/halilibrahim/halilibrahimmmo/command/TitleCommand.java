package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
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
import java.util.Set;

/** Lists every configured title (locked/unlocked) and equips one by id. */
public final class TitleCommand implements BasicCommand {

    private final JavaPlugin plugin;
    private final TitleService titles;
    private final MessageService messages;

    public TitleCommand(JavaPlugin plugin, TitleService titles, MessageService messages) {
        this.plugin = plugin;
        this.titles = titles;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sec")) {
            equip(player, args[1]);
            return;
        }
        list(player);
    }

    private void list(Player player) {
        titles.unlockedIds(player.getUniqueId()).thenAccept(unlocked ->
                player.getScheduler().run(plugin, task -> {
                    List<TitleDefinition> sorted = titles.definitions().stream()
                            .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
                    messages.send(player, MessageKey.TITLE_LIST, Map.of(
                            "acik", joined(sorted, unlocked, true),
                            "kilitli", joined(sorted, unlocked, false)
                    ));
                }, null));
    }

    private String joined(List<TitleDefinition> sorted, Set<String> unlocked, boolean wantUnlocked) {
        return sorted.stream()
                .filter(definition -> unlocked.contains(definition.id()) == wantUnlocked)
                .map(TitleDefinition::displayName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private void equip(Player player, String titleId) {
        titles.equip(player.getUniqueId(), player.getName(), titleId).thenAccept(result ->
                player.getScheduler().run(plugin, task -> {
                    MessageKey key = switch (result.status()) {
                        case EQUIPPED -> MessageKey.TITLE_EQUIPPED;
                        case ALREADY_EQUIPPED -> MessageKey.TITLE_ALREADY_EQUIPPED;
                        case NOT_UNLOCKED -> MessageKey.TITLE_NOT_UNLOCKED;
                        case UNKNOWN_TITLE -> MessageKey.TITLE_UNKNOWN;
                    };
                    messages.send(player, key, Map.of("unvan", displayNameOrId(result.titleId())));
                }, null));
    }

    private String displayNameOrId(String titleId) {
        TitleDefinition definition = titles.settings().definitions().get(titleId);
        return definition == null ? titleId : definition.displayName();
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length == 1) {
            return List.of("sec").stream().filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sec")) {
            return titles.definitions().stream().map(TitleDefinition::id)
                    .filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
