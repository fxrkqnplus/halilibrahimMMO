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

    /**
     * Ends whatever this player currently has running, clearing both the
     * internal armed/buffed state and any vanilla potion effect an instant
     * ability applied.
     *
     * <p>Only reached from Creative mode, where an ability has no cooldown, no
     * energy cost and an effectively unlimited duration - which means it has no
     * natural way to end and needs an explicit off switch.</p>
     */
    void deactivate(Player player);

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
     * Brings down every log connected to the one just broken, up to
     * {@code limit}. Lives here rather than in {@code skills} because the
     * secondary-break bookkeeping it needs - re-firing a {@link BlockBreakEvent}
     * per extra block for protection-plugin compatibility, and marking each one
     * so it never earns a second round of XP or quest progress - is this
     * service's machinery, already used by the {@code agac-kirici} ability.
     * Called by the odunculuk {@code TREE_FELLING} milestone, which owns the
     * decision of <em>when</em> a tree should fall.
     */
    void fellTree(BlockBreakEvent event, int limit);

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
