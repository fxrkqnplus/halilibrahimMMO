package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;

import java.util.Collection;

/** Owns YAML ability definitions, activation state and bounded gameplay effects. */
public interface AbilityService extends PluginService {

    AbilitySettings settings();

    Collection<AbilityDefinition> definitions();

    void activate(Player player, String abilityId, EnergyService energy);

    void handleBlockBreak(BlockBreakEvent event);

    void handleEntityDamage(EntityDamageByEntityEvent event);

    boolean isSecondaryBreak(BlockBreakEvent event);
}
