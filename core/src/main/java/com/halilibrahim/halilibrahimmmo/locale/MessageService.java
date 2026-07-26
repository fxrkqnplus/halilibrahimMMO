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
     * Renders a message and strips every style, yielding the bare text. Exists
     * for the narrow case of composing an already-Turkish fragment that will
     * itself be substituted into another template as an unparsed placeholder
     * value (see {@link DurationFormatter}) - never for sending to a player,
     * which always goes through {@link #send} so styling survives.
     */
    String plain(MessageKey key, Map<String, String> placeholders);

    /**
     * Sends a rendered message to the given audience, automatically prefixed
     * with {@link MessageKey#PREFIX}.
     */
    void send(Audience audience, MessageKey key, Map<String, String> placeholders);

    void send(Audience audience, MessageKey key);

    /**
     * Sends a rendered, prefixed message to every currently online player -
     * for server-wide announcements (e.g. a prestige milestone), never used
     * for routine per-player feedback.
     */
    void broadcast(MessageKey key, Map<String, String> placeholders);

    /**
     * Prepends the configured {@link MessageKey#PREFIX} to an already-rendered message.
     */
    Component prefixed(Component message);
}
