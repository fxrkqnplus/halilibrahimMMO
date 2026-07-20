package com.halilibrahim.halilibrahimmmo.energy;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;

/** Ephemeral shared energy pool used by active abilities. */
public interface EnergyService extends PluginService {

    void initialize(Player player);

    void remove(Player player);

    boolean tryConsume(Player player, int amount);

    int currentEnergy(Player player);

    void reconcile(Player player);
}
