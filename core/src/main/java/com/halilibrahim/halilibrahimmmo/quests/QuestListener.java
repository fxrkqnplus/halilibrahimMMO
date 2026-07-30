package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.exploit.BlockBreakRateLimiter;
import com.halilibrahim.halilibrahimmmo.exploit.PlacedBlockTracker;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Bridges block breaks and mob kills into quest progress. Everything that
 * happens once a quest actually finishes - XP, journal, announcements, unlock
 * re-evaluation - belongs to {@link QuestCompletionHandler}, which the
 * discovery-objective listener shares.
 */
public final class QuestListener implements Listener {

    private final QuestService quests;
    private final AbilityService abilities;
    private final BlockBreakRateLimiter breakRateLimiter;
    private final PlacedBlockTracker placedBlocks;
    private final ActivationService activation;
    private final QuestCompletionHandler completions;

    public QuestListener(
            QuestService quests, AbilityService abilities, BlockBreakRateLimiter breakRateLimiter,
            PlacedBlockTracker placedBlocks, ActivationService activation, QuestCompletionHandler completions
    ) {
        this.quests = quests;
        this.abilities = abilities;
        this.breakRateLimiter = breakRateLimiter;
        this.placedBlocks = placedBlocks;
        this.activation = activation;
        this.completions = completions;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        quests.loadAll(event.getPlayer().getUniqueId());
        quests.loadPins(event.getPlayer().getUniqueId());
        quests.loadDismissals(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        quests.evict(event.getPlayer().getUniqueId());
        breakRateLimiter.evict(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!activation.cached(player.getUniqueId()) || abilities.isSecondaryBreak(event) || !isGameplayGameMode(player)) {
            return;
        }
        // The same terrain test skill XP uses, crop exemption included - a
        // harvest quest must count exactly the wheat the tarımcılık skill does.
        if (!placedBlocks.isEligibleTerrain(event.getBlock()) || !breakRateLimiter.allow(player.getUniqueId())) {
            return;
        }
        quests.awardBlock(player.getUniqueId(), event.getBlock().getType(), player.getWorld().getEnvironment())
                .thenAccept(completed -> completions.notifyCompleted(player, completed));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || event.getEntity() instanceof Player || !activation.cached(killer.getUniqueId())
                || !isGameplayGameMode(killer)) {
            return;
        }
        quests.awardMob(killer.getUniqueId(), event.getEntity().getType(), event.getEntity().getWorld().getEnvironment())
                .thenAccept(completed -> completions.notifyCompleted(killer, completed));
    }

    /** Creative/Spectator players never progress quests - no risk, no reward, matches skill/class XP gating. */
    private boolean isGameplayGameMode(Player player) {
        return player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR;
    }
}
