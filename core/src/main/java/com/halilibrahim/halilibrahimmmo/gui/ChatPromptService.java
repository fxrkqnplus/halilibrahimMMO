package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Collects one line of free text from a player through chat, for the cases a
 * click cannot express.
 *
 * <p><strong>Why this exists at all.</strong> Since Phase 14 the whole
 * player-facing surface is GUI-only, and the admin panel has been GUI-only
 * since Phase 10 - which works right up to the moment staff need to type an
 * arbitrary number. Click-increment rows can add ten at a time; they cannot set
 * a reputation total of 4 812 500. Rather than reintroducing an admin chat command
 * (deleted on purpose, and the thing that made permissions hard to reason
 * about), the panel asks a question and reads the answer back.</p>
 *
 * <p>The pending chat line is <strong>always consumed</strong> - the event is
 * cancelled whether the answer parses or not - so an admin's reply never
 * reaches public chat. A prompt lapses on its own after
 * {@link #PROMPT_TIMEOUT_MILLIS}, and is dropped when its player leaves, so
 * nothing here outlives the person who asked for it.</p>
 */
public final class ChatPromptService implements PluginService, Listener {

    /** How long an unanswered prompt keeps swallowing the player's chat. */
    private static final long PROMPT_TIMEOUT_MILLIS = 60_000L;
    /** The one word that abandons a prompt without answering it. */
    private static final String CANCEL_WORD = "iptal";

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final ConcurrentHashMap<UUID, PendingPrompt> prompts = new ConcurrentHashMap<>();

    public ChatPromptService(JavaPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {
        prompts.clear();
    }

    /**
     * Asks {@code player} for one line and closes whatever screen they are on,
     * since a chat box behind an open inventory is not typeable.
     *
     * @param answer run on the player's own region scheduler once they reply
     */
    public void await(Player player, MessageKey instruction, Consumer<String> answer) {
        prompts.put(player.getUniqueId(), new PendingPrompt(answer, System.currentTimeMillis() + PROMPT_TIMEOUT_MILLIS));
        player.closeInventory();
        messages.send(player, instruction);
        messages.send(player, MessageKey.CHAT_PROMPT_CANCEL_HINT);
    }

    /**
     * {@link EventPriority#LOWEST} - which in Bukkit runs <em>first</em> - and
     * deliberately not {@code ignoreCancelled}: this has to claim the line
     * before any chat-formatting plugin renders it, and an answer to a prompt is
     * never a chat message even if something else already cancelled it.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PendingPrompt pending = prompts.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        if (pending.expiresAtMillis() < System.currentTimeMillis()) {
            messages.send(player, MessageKey.CHAT_PROMPT_EXPIRED);
            return;
        }
        String reply = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (reply.toLowerCase(Locale.ROOT).equals(CANCEL_WORD)) {
            messages.send(player, MessageKey.CHAT_PROMPT_CANCELLED);
            return;
        }
        // Chat is delivered off the main thread; everything the answer touches
        // (inventories, services that expect the player's region) is not.
        player.getScheduler().run(plugin, task -> pending.answer().accept(reply), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        prompts.remove(event.getPlayer().getUniqueId());
    }

    private record PendingPrompt(Consumer<String> answer, long expiresAtMillis) {
    }
}
