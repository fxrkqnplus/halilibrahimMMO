package com.halilibrahim.halilibrahimmmo.locale;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import java.util.Map;

/**
 * Renders and sends every Turkish, MiniMessage-formatted player/admin-facing
 * message defined in {@code messages.yml}, through the Adventure {@link Audience}
 * API - never legacy {@code ChatColor}/{@code §} formatting.
 */
public interface MessageService extends PluginService {

    /**
     * Renders a message with placeholder substitution. Placeholder values are
     * inserted unparsed (they cannot inject further MiniMessage tags).
     */
    Component render(MessageKey key, Map<String, String> placeholders);

    Component render(MessageKey key);

    /**
     * Sends a rendered message to the given audience, automatically prefixed
     * with {@link MessageKey#PREFIX}.
     */
    void send(Audience audience, MessageKey key, Map<String, String> placeholders);

    void send(Audience audience, MessageKey key);

    /**
     * Prepends the configured {@link MessageKey#PREFIX} to an already-rendered message.
     */
    Component prefixed(Component message);
}
