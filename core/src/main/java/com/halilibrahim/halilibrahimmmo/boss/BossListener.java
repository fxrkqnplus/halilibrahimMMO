package com.halilibrahim.halilibrahimmmo.boss;

import com.halilibrahim.halilibrahimmmo.classes.ClassAwardHandler;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.wealth.WealthAwardHandler;
import com.halilibrahim.halilibrahimmmo.wealth.WealthService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        awardClassExperience(killer, tier);
        wealth.award(killer.getUniqueId(), tier.wealthReward())
                .thenAccept(award -> wealthAwards.handle(killer, award));
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
