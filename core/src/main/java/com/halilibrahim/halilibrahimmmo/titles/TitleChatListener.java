package com.halilibrahim.halilibrahimmmo.titles;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Prepends the sender's equipped title to their chat messages, if any, in
 * that title's own configured color/weight ({@link TitleChatStyle}) - the
 * one deliberate exception in this codebase to always rendering
 * player-facing text through a {@code MessageKey}/{@code messages.yml}
 * template: a title's tag color is genuinely per-title configured data
 * ({@code titles.yml}), not Java-authored text, and {@code MessageService}
 * only ever substitutes unparsed string placeholders - it cannot vary a
 * placeholder's own color per invocation.
 */
public final class TitleChatListener implements Listener {

    private final TitleService titles;

    public TitleChatListener(TitleService titles) {
        this.titles = titles;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        titles.equippedDisplayName(player.getUniqueId()).ifPresent(titleName -> {
            TitleChatStyle style = titles.equippedStyle(player.getUniqueId()).orElse(TitleChatStyle.MAVI);
            Component prefix = Component.text("[", NamedTextColor.GRAY)
                    .append(Component.text(titleName).color(style.color()).decoration(TextDecoration.BOLD, style.bold()))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(Component.text(player.getName(), NamedTextColor.WHITE))
                    .append(Component.text(": ", NamedTextColor.GRAY));
            event.renderer((source, sourceDisplayName, message, viewer) -> prefix.append(message));
        });
    }
}
