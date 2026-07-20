package com.halilibrahim.halilibrahimmmo.titles;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;

/** Prepends the sender's equipped title to their chat messages, if any. */
public final class TitleChatListener implements Listener {

    private final TitleService titles;
    private final MessageService messages;

    public TitleChatListener(TitleService titles, MessageService messages) {
        this.titles = titles;
        this.messages = messages;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        titles.equippedDisplayName(player.getUniqueId()).ifPresent(titleName -> {
            Component prefix = messages.render(MessageKey.TITLE_CHAT_PREFIX, Map.of("unvan", titleName, "oyuncu", player.getName()));
            event.renderer((source, sourceDisplayName, message, viewer) -> prefix.append(message));
        });
    }
}
