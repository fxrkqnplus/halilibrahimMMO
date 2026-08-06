package com.halilibrahim.halilibrahimmmo.hud;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;

/**
 * Tells the {@code hmmo-client} mod what to draw over the player's own screen.
 *
 * <p>Everything this plugin shows a player has, until now, been something a
 * vanilla client already knows how to render: chat, a bossbar, a title, the
 * action bar, particles in the world. Two of Faz 22's requests are not -
 * tinting the edges of the screen while an ability runs, and putting the
 * running ability in the panel where potion effects are listed - and there is
 * no packet for either. The mod is already mandatory on this server, so the
 * honest way to do them is to say what is happening and let the client draw
 * it.</p>
 *
 * <p><strong>Nothing here is gameplay.</strong> A client that ignores these
 * messages entirely - a stale build, a player who somehow got past the mod
 * check - plays exactly the same game with a plainer screen. That is the line
 * this service is not allowed to cross: the server stays the only authority on
 * what an ability actually does.</p>
 */
public interface HudService extends PluginService {

    /**
     * Announces that {@code definition} is now running for {@code player}.
     *
     * @param expiresAtMillis when it ends, or {@code 0} when it does not
     *                        (Creative, where the ability stays until switched off)
     * @param comboLine       the frenzy line to draw at the bottom of the screen,
     *                        already rendered to plain text by the server, or an
     *                        empty string when this ability has none. Rendered
     *                        server-side on purpose: the words are Turkish and
     *                        belong in {@code messages.yml} like every other
     *                        string a player reads, not compiled into a mod
     *                        nobody can edit.
     * @param comboTier       which rung that line is on, so the client knows how
     *                        hard to shake it
     */
    void showAbility(
            Player player, AbilityDefinition definition, long expiresAtMillis,
            String comboLine, int comboTier
    );

    /** Announces that whatever was running has stopped. Safe to call when nothing was. */
    void clearAbility(Player player);

    /**
     * Tells {@code recipient} which players are currently in Asil Kurt's form,
     * so their client can draw those players as the wolf instead of as people.
     *
     * <p>The one message on this channel that is not about the recipient. A
     * transformation nobody else can see is not a transformation, so this is
     * broadcast to everybody online - including the transformed players
     * themselves, whose own body is drawn the same way to anybody watching
     * them.</p>
     *
     * <p>Carries the whole set every time, so it is safe to send on any change
     * and on join without tracking what each client already believes.</p>
     */
    void showWolfForms(Player recipient, java.util.Set<java.util.UUID> transformedPlayerIds);
}
