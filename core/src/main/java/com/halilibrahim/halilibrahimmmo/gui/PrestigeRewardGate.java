package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import org.bukkit.entity.Player;

/**
 * Sends a player who owes the game a prestige reward back to the screen that
 * spends it, whatever they tried to open.
 *
 * <p>Bound into {@link GuiManager} once at bootstrap, so it covers every
 * command shortcut and every menu-to-menu link at the same time - there is
 * exactly one place a {@link GuiView} is ever shown from.</p>
 *
 * <p><strong>Reads the cached profile, never the database.</strong> The guard
 * runs on every screen open, so an asynchronous check would either block or
 * open the wrong screen and correct itself a tick later. A player whose profile
 * is not yet warm is let through untouched, which is the right failure: they
 * have just joined, the reward screen will meet them the next time they open
 * anything, and the choice is derived rather than stored so it cannot be
 * lost.</p>
 */
public final class PrestigeRewardGate implements GuiManager.OpenGuard {

    private final GuiServices services;

    public PrestigeRewardGate(GuiServices services) {
        this.services = services;
    }

    @Override
    public boolean redirect(Player player, GuiView requested) {
        if (requested instanceof PrestigeRewardMenu) {
            return false;
        }
        if (!hasPendingChoice(player)) {
            return false;
        }
        services.messages().send(player, MessageKey.PRESTIGE_REWARD_PENDING);
        GuiManager.open(player, new PrestigeRewardMenu(services));
        return true;
    }

    /** Also used by the two commands that open a raw inventory instead of a {@link GuiView}. */
    public boolean hasPendingChoice(Player player) {
        return services.profiles().cached(player.getUniqueId())
                .map(profile -> services.prestigeRewards().pendingChoices(player.getUniqueId(), profile.prestige()) > 0)
                .orElse(false);
    }

    /** Opens the reward screen and says why, for a caller that is not a {@link GuiView}. */
    public void send(Player player) {
        services.messages().send(player, MessageKey.PRESTIGE_REWARD_PENDING);
        GuiManager.open(player, new PrestigeRewardMenu(services));
    }
}
