package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.exploit.BlockBreakRateLimiter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Bridges valid player actions into persistent skills and ephemeral energy state. */
public final class SkillListener implements Listener {

    private final SkillService skills;
    private final AbilityService abilities;
    private final EnergyService energy;
    private final BlockBreakRateLimiter breakRateLimiter;

    public SkillListener(SkillService skills, AbilityService abilities, EnergyService energy, BlockBreakRateLimiter breakRateLimiter) {
        this.skills = skills;
        this.abilities = abilities;
        this.energy = energy;
        this.breakRateLimiter = breakRateLimiter;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        skills.loadAll(player.getUniqueId());
        energy.initialize(player);
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
        if (!abilities.isSecondaryBreak(event) && breakRateLimiter.allow(event.getPlayer().getUniqueId())) {
            skills.awardBlock(event.getPlayer().getUniqueId(), event.getBlock().getType());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && !(event.getEntity() instanceof Player)) {
            skills.awardMob(killer.getUniqueId());
        }
    }
}
