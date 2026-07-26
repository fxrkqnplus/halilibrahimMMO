package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.exploit.BlockBreakRateLimiter;
import com.halilibrahim.halilibrahimmmo.exploit.PlacedBlockTracker;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Bridges valid player actions into persistent skills and ephemeral energy state. */
public final class SkillListener implements Listener {

    private final JavaPlugin plugin;
    private final SkillService skills;
    private final AbilityService abilities;
    private final EnergyService energy;
    private final ActivationService activation;
    private final BlockBreakRateLimiter breakRateLimiter;
    private final PlacedBlockTracker placedBlocks;
    private final SkillAwardHandler awards;

    public SkillListener(
            JavaPlugin plugin, SkillService skills, AbilityService abilities, EnergyService energy,
            ActivationService activation, BlockBreakRateLimiter breakRateLimiter,
            PlacedBlockTracker placedBlocks, SkillAwardHandler awards
    ) {
        this.plugin = plugin;
        this.skills = skills;
        this.abilities = abilities;
        this.energy = energy;
        this.activation = activation;
        this.breakRateLimiter = breakRateLimiter;
        this.placedBlocks = placedBlocks;
        this.awards = awards;
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
                && SkillAwardHandler.isGameplayGameMode(player) && isEligibleTerrain(event.getBlock())
                && breakRateLimiter.allow(player.getUniqueId())) {
            fellTreeIfUnlocked(player, event);
            skills.awardBlock(player.getUniqueId(), player.getName(), event.getBlock().getType())
                    .thenAccept(result -> awards.handle(player, result));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && !(event.getEntity() instanceof Player) && activation.cached(killer.getUniqueId())
                && SkillAwardHandler.isGameplayGameMode(killer)) {
            skills.awardMob(killer.getUniqueId(), killer.getName(), event.getEntity().getType())
                    .thenAccept(result -> awards.handle(killer, result));
        }
    }

    /**
     * Player-placed blocks are not terrain and never award anything - with one
     * deliberate exception. Farming <em>is</em> planting and harvesting your
     * own crops, so an {@link Ageable} block is exempt from that gate; what
     * stops break-and-replant farming instead is that a crop only counts once
     * it is fully grown. Hay bales are not {@link Ageable}, so they stay
     * gated and only the world's own village bales count.
     */
    private boolean isEligibleTerrain(Block block) {
        if (block.getBlockData() instanceof Ageable crop) {
            return crop.getAge() >= crop.getMaximumAge();
        }
        return !placedBlocks.isPlayerPlaced(block);
    }

    /**
     * The odunculuk level-25 milestone ({@code TREE_FELLING}): breaking the log
     * a tree actually stands on brings the whole trunk down. Deliberately gated
     * on the <em>root</em> log - a log with another log directly beneath it is
     * mid-trunk, and felling from there would make every single log break
     * cascade. Reuses the ability layer's secondary-break machinery, so the
     * extra logs are marked exactly like {@code agac-kirici}'s are and never
     * generate a second round of XP or quest progress.
     */
    private void fellTreeIfUnlocked(Player player, BlockBreakEvent event) {
        int limit = skills.milestoneModifiers(player.getUniqueId()).treeFellingLimit();
        if (limit <= 0 || !Tag.LOGS.isTagged(event.getBlock().getType())) {
            return;
        }
        if (Tag.LOGS.isTagged(event.getBlock().getRelative(0, -1, 0).getType())) {
            return;
        }
        abilities.fellTree(event, limit);
    }
}
