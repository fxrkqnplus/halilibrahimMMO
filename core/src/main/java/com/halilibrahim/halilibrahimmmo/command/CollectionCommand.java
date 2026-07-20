package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.collections.CollectionDefinition;
import com.halilibrahim.halilibrahimmmo.collections.CollectionProgress;
import com.halilibrahim.halilibrahimmmo.collections.CollectionService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lists configured collections, or one collection's entry checklist via {@code /koleksiyon <id>}. */
public final class CollectionCommand implements BasicCommand {

    private final JavaPlugin plugin;
    private final CollectionService collections;
    private final MessageService messages;

    public CollectionCommand(JavaPlugin plugin, CollectionService collections, MessageService messages) {
        this.plugin = plugin;
        this.collections = collections;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (args.length == 1) {
            detail(player, args[0].toLowerCase(Locale.ROOT));
            return;
        }
        list(player);
    }

    private void list(Player player) {
        collections.loadAll(player.getUniqueId()).thenAccept(progressById -> player.getScheduler().run(plugin, task -> {
            String summary = collections.definitions().stream()
                    .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName()))
                    .map(definition -> definition.displayName() + " " + progressById.get(definition.id()).obtainedCount() + "/" + definition.entries().size())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("-");
            messages.send(player, MessageKey.COLLECTION_LIST, Map.of("koleksiyonlar", summary));
        }, null));
    }

    private void detail(Player player, String collectionId) {
        CollectionDefinition definition = collections.settings().definitions().get(collectionId);
        if (definition == null) {
            messages.send(player, MessageKey.COLLECTION_UNKNOWN, Map.of("koleksiyon", collectionId));
            return;
        }
        collections.find(player.getUniqueId(), collectionId).thenAccept(found -> player.getScheduler().run(plugin, task -> {
            CollectionProgress progress = found.orElseThrow();
            String entries = definition.entries().values().stream()
                    .map(entry -> (progress.obtainedEntryIds().contains(entry.id()) ? "✔ " : "✘ ") + entry.displayName())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("-");
            messages.send(player, MessageKey.COLLECTION_DETAIL, Map.of(
                    "koleksiyon", definition.displayName(),
                    "ilerleme", progress.obtainedCount() + "/" + definition.entries().size(),
                    "ogeler", entries
            ));
        }, null));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String partial = args[0].toLowerCase(Locale.ROOT);
        return collections.definitions().stream().map(CollectionDefinition::id).filter(id -> id.startsWith(partial)).toList();
    }
}
