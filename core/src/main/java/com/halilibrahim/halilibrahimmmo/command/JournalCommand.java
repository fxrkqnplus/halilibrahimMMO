package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.journal.JournalEntry;
import com.halilibrahim.halilibrahimmmo.journal.JournalService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Shows the invoking player's most recent quest and collection milestones via {@code /gunluk}. */
public final class JournalCommand implements BasicCommand {

    private static final int RECENT_LIMIT = 10;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final JavaPlugin plugin;
    private final JournalService journal;
    private final MessageService messages;

    public JournalCommand(JavaPlugin plugin, JournalService journal, MessageService messages) {
        this.plugin = plugin;
        this.journal = journal;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        journal.recent(player.getUniqueId(), RECENT_LIMIT).thenAccept(entries -> player.getScheduler().run(plugin, task -> {
            if (entries.isEmpty()) {
                messages.send(player, MessageKey.JOURNAL_EMPTY);
                return;
            }
            String lines = entries.stream().map(this::line).reduce((left, right) -> left + "\n" + right).orElse("-");
            messages.send(player, MessageKey.JOURNAL_LIST, Map.of("girdiler", lines));
        }, null));
    }

    private String line(JournalEntry entry) {
        String timestamp = TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(entry.createdAtMillis()));
        return "[" + timestamp + "] " + entry.referenceName() + " (+" + entry.experienceAwarded() + " XP)";
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return List.of();
    }
}
