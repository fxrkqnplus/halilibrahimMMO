package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeRewardService;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Applies selected-class passives on player lifecycle and valid PvE damage.
 *
 * <p>Also the one registered listener for {@link EntityDamageByEntityEvent}
 * against a valid non-player living entity, so it forwards to
 * {@link AbilityService#handleEntityDamage} for the {@code DAMAGE_BUFF}
 * ability effect (e.g. Avcı Odağı) - that effect type existed since Phase 3
 * but had no event source wired to it until this phase.</p>
 */
public final class ClassListener implements Listener {
    private final JavaPlugin plugin; private final ClassService classes; private final AbilityService abilities; private final EnergyService energy; private final SkillService skills;
    private final PrestigeRewardService prestigeRewards;
    public ClassListener(JavaPlugin plugin, ClassService classes, AbilityService abilities, EnergyService energy, SkillService skills, PrestigeRewardService prestigeRewards) {
        this.plugin = plugin; this.classes = classes; this.abilities = abilities; this.energy = energy; this.skills = skills; this.prestigeRewards = prestigeRewards;
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        classes.load(player.getUniqueId(), player.getName()).thenAccept(ignored -> player.getScheduler().run(plugin, task -> {
            classes.applyPlayerEffects(player);
            prestigeRewards.applyPlayerEffects(player);
        }, null));
    }
    /**
     * The selected class's MAX_HEALTH modifier is lost on death together with
     * the skill milestone bonuses (see {@code SkillListener#onRespawn}), and is
     * reapplied the same way: one tick after the respawn, once the server has
     * finished resetting the player's own attributes. Energy is reconciled in
     * the same pass so a class/milestone maximum-energy bonus is not left
     * clamped to the base value.
     */
    @EventHandler public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().runDelayed(plugin, task -> {
            classes.applyPlayerEffects(player);
            prestigeRewards.applyPlayerEffects(player);
            energy.reconcile(player);
        }, null, 1L);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { classes.evict(event.getPlayer().getUniqueId()); }
    @EventHandler(ignoreCancelled = true) public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof Player) return;
        double pveDamageBonus = classes.modifiers(player.getUniqueId()).pveDamageBonus()
                + skills.milestoneModifiers(player.getUniqueId()).pveDamageBonus()
                + prestigeRewards.damageBonus(player.getUniqueId());
        event.setDamage(event.getDamage() * (1.0D + pveDamageBonus));
        abilities.handleEntityDamage(event, energy);
    }
}
