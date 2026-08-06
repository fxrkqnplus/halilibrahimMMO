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

    /**
     * Ends whatever is running because the player just died.
     *
     * <p>Separate from {@link #deactivate} on both halves: this one keeps the
     * player's <em>selection</em>, because dying is not choosing to put an
     * ability down, and it says so with its own message rather than Creative's
     * "switched off" line. A no-op when nothing was running.</p>
     */
    void endOnDeath(Player player);

    /**
     * Resolves whatever a block break means for the ability layer: expiring ore
     * outlines, extra-drop buffs, and firing an armed sweep. {@code energy} is
     * needed because a sweep ability now pays per block as it runs, and stops
     * when the bar empties.
     */
    void handleBlockBreak(BlockBreakEvent event, EnergyService energy);

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
     * Clears the 3x3 plane around the block just broken, restricted to blocks
     * the same configured skill claims, up to {@code limit}. The
     * {@code EXCAVATION} milestone's counterpart to {@link #fellTree}, sharing
     * the same secondary-break bookkeeping for the same reason.
     *
     * @param skillId which skill's material set decides what an eligible neighbour is
     */
    void excavate(BlockBreakEvent event, String skillId, int limit);

    /**
     * Marks {@code abilityId} as the player's menu-selected ability, ready to
     * be right-click activated with the matching {@link AbilityToolTag}. Does
     * not itself activate anything.
     */
    void select(Player player, String abilityId);

    /**
     * Clears a player's menu-selected ability, along with anything that
     * selection left armed, buffed or running - called when the player's class
     * changes (by their own choice or an admin's), since neither the selection
     * nor its effects belong to the new class. Safe for an offline target;
     * per-ability cooldowns are deliberately preserved.
     */
    void clearSelection(UUID playerId);

    /**
     * The player taking their own ability off from the ability screen. Ends
     * everything the selection left running, but - unlike {@link #clearSelection}
     * - keeps the switch-cooldown bookkeeping, so unequipping is not a free way
     * around the wait between two different abilities.
     *
     * @return {@code false} when nothing was equipped in the first place
     */
    boolean unequip(Player player);

    /** The player's currently menu-selected ability id, if any. In-memory only. */
    Optional<String> selected(UUID playerId);

    /**
     * The player's currently armed/buffed ability and when it expires, if any -
     * used to drive action-bar countdown feedback. In-memory only.
     */
    Optional<ActiveAbilityStatus> activeStatus(UUID playerId);

    /**
     * How much of this ability's cooldown is left for this player, {@code 0}
     * when it is ready. Read by the activation listener so a right click during
     * a cooldown never reaches the two-click confirm prompt - prompting for
     * something that will only be refused is worse than saying nothing.
     */
    long cooldownRemainingMillis(UUID playerId, String abilityId);

    /**
     * Clears a player's activation visual feedback (glint on their held item,
     * action-bar countdown) without touching the underlying armed/buff game
     * state. Safe to call even if no feedback is currently showing.
     */
    void clearActivationFeedback(Player player);

    /**
     * What the two frenzy abilities do when something they were fighting dies.
     *
     * <p>{@link AbilityEffectType#LIFE_STEAL} plays its soul flourish, which is
     * purely audiovisual - the healing itself happened blow by blow in
     * {@link #handleEntityDamage}. {@link AbilityEffectType#NOBLE_WOLF} eats the
     * kill, which is not: an animal that drops meat feeds the player where it
     * fell. A caller with neither running is a no-op.</p>
     *
     * @param at         where the creature fell, which is where the soul is torn
     *                   from and where the eating is heard
     * @param victimType what died, which is the only thing that decides whether
     *                   there was anything on it worth eating
     */
    void handleCreatureDeath(Player killer, org.bukkit.Location at, org.bukkit.entity.EntityType victimType);

    /**
     * Cancels fall damage for a player currently in Asil Kurt's form.
     *
     * <p>Separate from {@link #handleEntityDamage}, which only ever sees one
     * entity hitting another: a fall has no attacker, so it arrives on the
     * broader event and would never reach that method.</p>
     */
    void handleFallDamage(org.bukkit.event.entity.EntityDamageEvent event);

    /**
     * A player starting or stopping a sneak, which is how Asil Kurt's ground
     * slam is aimed.
     *
     * <p>Sneak is the one gesture a falling player has free - there is no key to
     * bind and nothing to learn - and it only means anything here in mid-air
     * while transformed. Every other case returns immediately.</p>
     */
    void handleSneak(Player player, boolean sneaking);

    /** Whether this player is currently wearing Asil Kurt's body. In-memory only. */
    boolean isTransformed(UUID playerId);

    /** Whether this stack is a pair of Asil Kurt's claws, by the tag on the item itself. */
    boolean isWolfClaw(org.bukkit.inventory.ItemStack item);

    /**
     * Hands back a hotbar that Asil Kurt's claws were holding, if this player
     * still has one stored.
     *
     * <p>The safety net for an unclean shutdown. The saved hotbar rides on the
     * player's own persistent data, so it survives a crash the plugin never got
     * to react to; this is what notices on the next login and undoes the
     * swap.</p>
     */
    void restoreHotbarIfStranded(Player player);

    /**
     * Sends a joining player the set of players currently in Asil Kurt's form.
     *
     * <p>Needed because the set is only broadcast when it <em>changes</em>: a
     * player who logs in halfway through somebody else's transformation would
     * otherwise see a person where everybody else sees a wolf.</p>
     */
    void sendWolfForms(Player recipient);

    /**
     * Takes a leaving player out of the wolf-form set and tells everyone left.
     *
     * <p>Without this the transformation would outlive the connection: the
     * upkeep task stops on its own, but every other client would keep drawing a
     * wolf where a player used to be until the next change to the set.</p>
     */
    void handlePlayerQuit(Player player);

    /**
     * Puts back the Efficiency level of every pickaxe in this player's
     * inventory that {@link AbilityEffectType#TOOL_EFFICIENCY} temporarily
     * raised. The original level rides on the item's own persistent data, so
     * this is correct however the item moved in the meantime, and is a no-op
     * for a player holding nothing marked.
     */
    void restoreEfficiency(Player player);

    /**
     * Runs a mine bomb's clearing sweep in place of the vanilla explosion its
     * listener suppressed. Bounded and spread across ticks; see the
     * implementation for why both of those are required rather than tuning.
     *
     * @param radius how far the charge reaches, as tagged on the charge itself
     *               so a live edit to {@code abilities.yml} cannot change what
     *               an already-lit bomb does
     */
    void detonateMineBomb(org.bukkit.Location at, UUID ownerId, String abilityId, int radius);
}
