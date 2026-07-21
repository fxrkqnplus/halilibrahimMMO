package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Owns YAML ability definitions, activation state and bounded gameplay effects. */
public interface AbilityService extends PluginService {

    AbilitySettings settings();

    Collection<AbilityDefinition> definitions();

    void activate(Player player, String abilityId, EnergyService energy);

    void handleBlockBreak(BlockBreakEvent event);

    /**
     * Applies the {@link AbilityEffectType#DAMAGE_BUFF} multiplier for a valid
     * PvE hit, if the attacker has one active. For {@link AbilityActivationMode#ENERJI}
     * abilities, also debits {@code energyPerHit} from {@code energy} and ends
     * the buff early once energy is insufficient - {@code energy} is only ever
     * touched for that mode.
     */
    void handleEntityDamage(EntityDamageByEntityEvent event, EnergyService energy);

    boolean isSecondaryBreak(BlockBreakEvent event);

    /**
     * Marks {@code abilityId} as the player's menu-selected ability, ready to
     * be right-click activated with the matching {@link AbilityToolTag}. Does
     * not itself activate anything.
     */
    void select(Player player, String abilityId);

    /**
     * Clears a player's menu-selected ability without touching any armed/buffed
     * game state - called when the player's class changes, since a
     * previously-selected ability may no longer belong to their new class.
     */
    void clearSelection(UUID playerId);

    /** The player's currently menu-selected ability id, if any. In-memory only. */
    Optional<String> selected(UUID playerId);

    /**
     * The player's currently armed/buffed ability and when it expires, if any -
     * used to drive action-bar countdown feedback. In-memory only.
     */
    Optional<ActiveAbilityStatus> activeStatus(UUID playerId);

    /**
     * Clears a player's activation visual feedback (glint on their held item,
     * action-bar countdown) without touching the underlying armed/buff game
     * state. Safe to call even if no feedback is currently showing.
     */
    void clearActivationFeedback(Player player);
}
