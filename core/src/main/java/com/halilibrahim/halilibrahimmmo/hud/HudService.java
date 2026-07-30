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
     * @param totalMillis     how long this activation was given, so the client can
     *                        draw progress rather than only a number
     */
    void showAbility(Player player, AbilityDefinition definition, long expiresAtMillis, long totalMillis);

    /** Announces that whatever was running has stopped. Safe to call when nothing was. */
    void clearAbility(Player player);
}
