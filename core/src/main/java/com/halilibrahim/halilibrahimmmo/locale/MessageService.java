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
     * Renders an admin-authored MiniMessage string that is <em>content</em>
     * rather than a template - a guide page's body line, today the only caller.
     *
     * <p>Deliberately narrow. Plugin-authored text must always go through a
     * {@link MessageKey}, and a value the code produced must always be
     * substituted as an unparsed placeholder so a player's own data can never
     * inject markup. This exists for the opposite case: a paragraph an owner
     * wrote in {@code rehber.yml}, where deciding that a word is bold is the
     * whole point of writing it there. The server's own {@code <simge:...>}
     * icons resolve here exactly as they do in every template.</p>
     */
    Component renderText(String template);

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
