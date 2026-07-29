package com.halilibrahim.halilibrahimmmo.boss;

import com.halilibrahim.halilibrahimmmo.classes.ClassAwardHandler;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.wealth.WealthAwardHandler;
import com.halilibrahim.halilibrahimmmo.wealth.WealthService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Everything a dead world boss pays out.
 *
 * <p>Reads the rarity straight off the corpse's {@code PersistentDataContainer}
 * rather than from any in-memory registry, which is what lets a boss that
 * outlived a server restart still reward properly. Ordinary skill XP, quest
 * progress and Zenginlik Puanı for the underlying creature are unaffected and
 * still flow through {@code SkillListener} - everything here is <em>on top</em>
 * of what killing that mob would normally be worth.</p>
 */
public final class BossListener implements Listener {

    private final JavaPlugin plugin;
    private final YamlBossService bosses;
    private final ProfileService profiles;
    private final ClassService classes;
    private final ClassAwardHandler classAwards;
    private final WealthService wealth;
    private final WealthAwardHandler wealthAwards;
    private final FeedbackService feedback;
    private final MessageService messages;

    public BossListener(
            JavaPlugin plugin, YamlBossService bosses, ProfileService profiles, ClassService classes,
            ClassAwardHandler classAwards, WealthService wealth, WealthAwardHandler wealthAwards,
            FeedbackService feedback, MessageService messages
    ) {
        this.plugin = plugin;
        this.bosses = bosses;
        this.profiles = profiles;
        this.classes = classes;
        this.classAwards = classAwards;
        this.wealth = wealth;
        this.wealthAwards = wealthAwards;
        this.feedback = feedback;
        this.messages = messages;
    }

    /**
     * The moment a boss stops being scenery and becomes somebody's fight.
     *
     * <p>Only a player's own blow counts. A boss that took splash damage from a
     * creeper or a fall never had anybody go to it, and clearing it away when
     * the next one spawns is exactly right; a boss somebody actually swung at
     * must survive that sweep however long the fight runs.</p>
     */
    @EventHandler(ignoreCancelled = true)
    public void onBossDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim) || !(resolveAttacker(event) instanceof Player)) {
            return;
        }
        if (bosses.isBoss(victim)) {
            bosses.markEngaged(victim);
        }
    }

    /** Unwraps an arrow/trident/potion so a ranged kill counts as engagement too. */
    private Entity resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Entity shooter) {
            return shooter;
        }
        return event.getDamager();
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Optional<BossRarity> rarity = bosses.rarityOf(entity);
        if (rarity.isEmpty()) {
            return;
        }
        BossRaritySettings tier = bosses.settings().rarities().get(rarity.get());
        String name = bosses.nameOf(entity).orElse("");
        if (tier == null || name.isEmpty()) {
            return;
        }
        bosses.retire(entity.getUniqueId());
        event.setDroppedExp(tier.vanillaExperience());
        rollLoot(event, tier);
        bosses.headOf(entity).ifPresent(head -> event.getDrops().add(headItem(head, rarity.get(), name)));

        Player killer = entity.getKiller();
        if (killer == null) {
            // Died to lava, a fall or another mob: the drops are still on the
            // ground for whoever finds them, but there is nobody to credit.
            return;
        }
        messages.broadcast(rarity.get().deathBroadcastKey(), Map.of("isim", name, "oyuncu", killer.getName()));
        Bukkit.getOnlinePlayers().forEach(online ->
                online.getScheduler().run(plugin, task -> feedback.celebrateBossDefeated(online), null));
        celebrateAtCorpse(entity);
        awardClassExperience(killer, tier);
        wealth.award(killer.getUniqueId(), tier.wealthReward())
                .thenAccept(award -> wealthAwards.handle(killer, award));
    }

    /**
     * The visible half of a boss falling. The death cue is a sound every online
     * player hears, which tells the server <em>that</em> it happened; this marks
     * <em>where</em>, for the people who actually went and fought it. Spawned at
     * the corpse rather than on the killer, so a whole raid party sees the same
     * thing in the same place.
     */
    private void celebrateAtCorpse(LivingEntity entity) {
        Location at = entity.getLocation().add(0, 1, 0);
        at.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, at, 3, 1.0D, 0.6D, 1.0D, 0.0D);
        at.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, at, 120, 1.4D, 1.0D, 1.4D, 0.4D);
        at.getWorld().spawnParticle(Particle.END_ROD, at, 60, 0.8D, 1.2D, 0.8D, 0.15D);
        at.getWorld().playSound(at, Sound.ENTITY_WITHER_DEATH, 1.0F, 1.2F);
    }

    /** Goes through the ordinary class-XP path, so a boss kill levels a class exactly like anything else does. */
    private void awardClassExperience(Player killer, BossRaritySettings tier) {
        String classId = profiles.cached(killer.getUniqueId()).map(profile -> profile.selectedClass()).orElse("");
        if (classId.isEmpty()) {
            return;
        }
        classes.awardClassExperience(killer.getUniqueId(), killer.getName(), classId, tier.classExperience())
                .thenAccept(result -> classAwards.handle(killer, classId, tier.classExperience(), result));
    }

    private void rollLoot(EntityDeathEvent event, BossRaritySettings tier) {
        for (BossLootEntry entry : tier.loot()) {
            if (ThreadLocalRandom.current().nextDouble() > entry.chance()) {
                continue;
            }
            int amount = entry.minimum() == entry.maximum()
                    ? entry.minimum()
                    : ThreadLocalRandom.current().nextInt(entry.minimum(), entry.maximum() + 1);
            event.getDrops().add(new ItemStack(entry.material(), amount));
        }
    }

    /** The trophy: this specific boss's own name, in its rarity's own colour. */
    private ItemStack headItem(Material material, BossRarity rarity, String name) {
        ItemStack head = new ItemStack(material);
        ItemMeta meta = head.getItemMeta();
        meta.displayName(messages.render(rarity.headKey(), Map.of("isim", name)));
        head.setItemMeta(meta);
        return head;
    }
}
