package com.halilibrahim.halilibrahimmmo.energy;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;

/** Ephemeral shared energy pool used by active abilities. */
public interface EnergyService extends PluginService {

    void initialize(Player player);

    void remove(Player player);

    boolean tryConsume(Player player, int amount);

    int currentEnergy(Player player);

    /**
     * The player's own energy ceiling - the configured base plus every class
     * and skill-milestone bonus. Public because every place that shows energy
     * shows it as "current/maximum", never as a bare number.
     */
    int maximumEnergy(Player player);

    void reconcile(Player player);
}
