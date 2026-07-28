package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.exploit.BlockBreakRateLimiter;
import com.halilibrahim.halilibrahimmmo.exploit.PlacedBlockTracker;
import com.halilibrahim.halilibrahimmmo.wealth.WealthAwardHandler;
import com.halilibrahim.halilibrahimmmo.wealth.WealthService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bridges valid player actions into persistent skills and ephemeral energy
 * state.
 *
 * <p>Zenginlik Puanı is awarded from here too rather than from a listener of
 * its own, deliberately: wealth answers to exactly the same four gates skill XP
 * does - the MMO must be activated, the gamemode must be a real one, the block
 * must be world-generated rather than player-placed, and the break rate must be
 * plausible - and a second listener would have had to reproduce all four and
 * then keep them in step forever. What differs between the two is only how much
 * each block or creature is worth, which is a lookup in a different YAML file.</p>
 */
public final class SkillListener implements Listener {

    private final JavaPlugin plugin;
    private final SkillService skills;
    private final AbilityService abilities;
    private final EnergyService energy;
    private final ActivationService activation;
    private final BlockBreakRateLimiter breakRateLimiter;
    private final PlacedBlockTracker placedBlocks;
    private final SkillAwardHandler awards;
    private final WealthService wealth;
    private final WealthAwardHandler wealthAwards;

    public SkillListener(
            JavaPlugin plugin, SkillService skills, AbilityService abilities, EnergyService energy,
            ActivationService activation, BlockBreakRateLimiter breakRateLimiter,
            PlacedBlockTracker placedBlocks, SkillAwardHandler awards,
            WealthService wealth, WealthAwardHandler wealthAwards
    ) {
        this.plugin = plugin;
        this.skills = skills;
        this.abilities = abilities;
        this.energy = energy;
        this.activation = activation;
        this.breakRateLimiter = breakRateLimiter;
        this.placedBlocks = placedBlocks;
        this.awards = awards;
        this.wealth = wealth;
        this.wealthAwards = wealthAwards;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        skills.loadAll(player.getUniqueId())
                .thenAccept(ignored -> player.getScheduler().run(plugin, task -> skills.applyMilestoneEffects(player), null));
        energy.initialize(player);
    }

    /**
     * Death strips the milestone bonuses: the HASTE/SPEED potion effects are
     * cleared outright and the MAX_HEALTH attribute modifier does not survive
     * the respawn either, so a player used to permanently lose every
     * 25/50/75/100 boost until they relogged. Reapplied one tick after the
     * respawn rather than inside the event, because the server finishes
     * resetting the player's attributes and effects after this event returns -
     * anything applied during it would be wiped again immediately.
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().runDelayed(plugin, task -> skills.applyMilestoneEffects(player), null, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        skills.evict(event.getPlayer().getUniqueId());
        energy.remove(event.getPlayer());
        breakRateLimiter.evict(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        abilities.handleBlockBreak(event);
        Player player = event.getPlayer();
        if (activation.cached(player.getUniqueId()) && !abilities.isSecondaryBreak(event)
                && SkillAwardHandler.isGameplayGameMode(player) && placedBlocks.isEligibleTerrain(event.getBlock())
                && breakRateLimiter.allow(player.getUniqueId())) {
            fellTreeIfUnlocked(player, event);
            skills.awardBlock(player.getUniqueId(), player.getName(), event.getBlock().getType())
                    .thenAccept(result -> awards.handle(player, result));
            wealth.awardBlock(player.getUniqueId(), event.getBlock().getType())
                    .thenAccept(award -> wealthAwards.handle(player, award));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && !(event.getEntity() instanceof Player) && activation.cached(killer.getUniqueId())
                && SkillAwardHandler.isGameplayGameMode(killer)) {
            skills.awardMob(killer.getUniqueId(), killer.getName(), event.getEntity().getType())
                    .thenAccept(result -> awards.handle(killer, result));
            wealth.awardEntity(killer.getUniqueId(), event.getEntity().getType())
                    .thenAccept(award -> wealthAwards.handle(killer, award));
        }
    }

    /**
     * The odunculuk level-25 milestone ({@code TREE_FELLING}): breaking the log
     * a tree actually stands on brings the whole trunk down.
     *
     * <p>Whether this <em>is</em> a tree is {@link TreeShape}'s decision, not a
     * bare "log with nothing under it" test. That test alone also described a
     * village house's corner pillar, which the world generates rather than a
     * player, so nothing else in the pipeline was rejecting it - felling one
     * took the timber out from under a generated building. Reuses the ability
     * layer's secondary-break machinery, so the extra logs are marked exactly
     * like {@code agac-kirici}'s are and never generate a second round of XP or
     * quest progress.</p>
     */
    private void fellTreeIfUnlocked(Player player, BlockBreakEvent event) {
        int limit = skills.milestoneModifiers(player.getUniqueId()).treeFellingLimit();
        if (limit <= 0 || !TreeShape.isNaturalTreeRoot(event.getBlock())) {
            return;
        }
        abilities.fellTree(event, limit);
    }
}
