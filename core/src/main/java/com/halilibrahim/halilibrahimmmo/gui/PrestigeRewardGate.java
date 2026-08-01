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
        if (requested instanceof PrestigeRewardMenu || requested instanceof PrestigePhysicalRewardMenu) {
            return false;
        }
        if (!hasPendingChoice(player)) {
            return false;
        }
        send(player);
        return true;
    }

    /**
     * Also used by the commands that open a raw inventory instead of a
     * {@link GuiView}.
     *
     * <p>A prestige owes the player <em>two</em> choices, and either one being
     * outstanding holds the door: the permanent bonus, and the crate of items.
     * Both are derived from the rank rather than stored, so neither can be lost
     * to a crash or a closed window.</p>
     */
    public boolean hasPendingChoice(Player player) {
        return services.profiles().cached(player.getUniqueId())
                .map(profile -> services.prestigeRewards().pendingChoices(player.getUniqueId(), profile.prestige()) > 0
                        || services.prestigeRewards().pendingPhysicalChoices(player.getUniqueId(), profile.prestige()) > 0)
                .orElse(false);
    }

    /**
     * Opens whichever half is still owed, permanent first.
     *
     * <p>Order matters: the permanent bonus is the decision worth thinking
     * about and the crate is the celebration, so the thinking comes first and
     * the payout closes the sequence.</p>
     */
    public void send(Player player) {
        int prestige = services.profiles().cached(player.getUniqueId()).map(profile -> profile.prestige()).orElse(0);
        if (services.prestigeRewards().pendingChoices(player.getUniqueId(), prestige) > 0) {
            services.messages().send(player, MessageKey.PRESTIGE_REWARD_PENDING);
            GuiManager.open(player, new PrestigeRewardMenu(services));
            return;
        }
        services.messages().send(player, MessageKey.PRESTIGE_PHYSICAL_PENDING);
        GuiManager.open(player, new PrestigePhysicalRewardMenu(services));
    }
}
