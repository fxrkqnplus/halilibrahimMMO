package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.quests.QuestDefinition;
import com.halilibrahim.halilibrahimmmo.quests.QuestProgress;
import com.halilibrahim.halilibrahimmmo.quests.QuestService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Lists every configured quest and the invoking player's progress toward it. */
public final class QuestCommand implements BasicCommand {

    private final JavaPlugin plugin;
    private final QuestService quests;
    private final MessageService messages;

    public QuestCommand(JavaPlugin plugin, QuestService quests, MessageService messages) {
        this.plugin = plugin;
        this.quests = quests;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        quests.loadAll(player.getUniqueId()).thenAccept(progressById -> player.getScheduler().run(plugin, task -> {
            String summary = quests.definitions().stream()
                    .filter(definition -> progressById.containsKey(definition.id()))
                    .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName()))
                    .map(definition -> summarize(definition, progressById.get(definition.id())))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("-");
            messages.send(player, MessageKey.QUEST_LIST, Map.of("gorevler", summary));
        }, null));
    }

    private String summarize(QuestDefinition definition, QuestProgress progress) {
        return definition.displayName() + " " + progress.progress() + "/" + definition.target();
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return List.of();
    }
}
