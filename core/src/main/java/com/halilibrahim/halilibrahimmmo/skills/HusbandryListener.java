package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Awards the hayvancılık skill. Deliberately covers only ways of keeping an
 * animal <em>alive and productive</em> - breeding, taming, leading, shearing,
 * milking, and turning what they give into food. Killing an animal stays
 * avcılık's business, so the two skills never reward the same act.
 *
 * <p>The lead-carrying tick is the one source with no event of its own, so
 * this listener owns a repeating task. It is started and stopped explicitly
 * by {@code HalilIbrahimMMOPlugin}, mirroring {@code ProgressionListener}'s
 * existing {@code close()} discipline rather than turning a listener into a
 * {@code PluginService} just for one timer.</p>
 */
public final class HusbandryListener implements Listener {

    /** 20 seconds, as a tick count - the requested cadence for the lead-carrying reward. */
    private static final long LEAD_TICK_INTERVAL = 400L;
    private static final double LEAD_SCAN_RADIUS = 12.0D;
    private static final Set<EntityType> LEADABLE_LIVESTOCK = Set.of(EntityType.COW, EntityType.PIG, EntityType.SHEEP);
    private static final Set<EntityType> MILKABLE = Set.of(EntityType.COW, EntityType.MOOSHROOM, EntityType.GOAT);
    /** Both parents' food is refunded together, matching how it was spent. */
    private static final int BREED_REFUND_AMOUNT = 2;

    private final JavaPlugin plugin;
    private final SkillService skills;
    private final ActivationService activation;
    private final MessageService messages;
    private final SkillAwardHandler awards;
    private final MilestoneToggles toggles;
    private ScheduledTask leadTask;

    public HusbandryListener(
            JavaPlugin plugin, SkillService skills, ActivationService activation,
            MessageService messages, SkillAwardHandler awards, MilestoneToggles toggles
    ) {
        this.plugin = plugin;
        this.skills = skills;
        this.activation = activation;
        this.messages = messages;
        this.awards = awards;
        this.toggles = toggles;
    }

    /** Starts the lead-carrying tick; same shape as {@code PlayerEnergyService.enable()}'s regeneration task. */
    public void start() {
        leadTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task ->
                Bukkit.getOnlinePlayers().forEach(player ->
                        player.getScheduler().run(plugin, ignored -> awardLeadCarrying(player), null)),
                LEAD_TICK_INTERVAL, LEAD_TICK_INTERVAL);
    }

    public void close() {
        if (leadTask != null) {
            leadTask.cancel();
            leadTask = null;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player) || !isEligible(player)) {
            return;
        }
        award(player, SkillAction.HAYVAN_CIFTLESTIRME, null);
        refundBreedingFood(player, event.getBredWith());
        spawnTwinIfLucky(player, event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player) || !isEligible(player)) {
            return;
        }
        award(player, SkillAction.HAYVAN_EVCILLESTIRME, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (event.getEntity().getType() != EntityType.SHEEP || !isEligible(event.getPlayer())) {
            return;
        }
        award(event.getPlayer(), SkillAction.KOYUN_KIRPMA, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMilk(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !MILKABLE.contains(event.getRightClicked().getType())) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.BUCKET || !isEligible(player)) {
            return;
        }
        award(player, SkillAction.SUT_SAGMA, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !isEligible(player)) {
            return;
        }
        award(player, SkillAction.PASTA_URETIMI, event.getRecipe().getResult().getType());
    }

    /**
     * Awarded once per extraction rather than per item, so emptying a full
     * furnace pays the same as taking one steak out - the reward is for
     * cooking, not for how long the hopper was left running.
     */
    @EventHandler(ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        if (!isEligible(event.getPlayer())) {
            return;
        }
        award(event.getPlayer(), SkillAction.ET_PISIRME, event.getItemType());
    }

    /**
     * One award per tick regardless of herd size: leading ten cows is the same
     * job as leading one, and paying per animal would just reward stacking.
     */
    private void awardLeadCarrying(Player player) {
        if (!isEligible(player)) {
            return;
        }
        boolean leading = player.getNearbyEntities(LEAD_SCAN_RADIUS, LEAD_SCAN_RADIUS, LEAD_SCAN_RADIUS).stream()
                .anyMatch(entity -> isLedBy(entity, player));
        if (leading) {
            award(player, SkillAction.HAYVAN_TASIMA, null);
        }
    }

    private boolean isLedBy(Entity entity, Player player) {
        return entity instanceof LivingEntity animal && LEADABLE_LIVESTOCK.contains(animal.getType())
                && animal.isLeashed() && player.equals(animal.getLeashHolder());
    }

    /**
     * The hayvancılık level-25 milestone ({@code BREED_REFUND}). Rolled after
     * the baby is already born, so a failed roll costs the player nothing they
     * did not already expect to spend.
     */
    private void refundBreedingFood(Player player, ItemStack breedItem) {
        double chance = toggles.isEnabled(player, SkillMilestoneEffectType.BREED_REFUND)
                ? skills.milestoneModifiers(player.getUniqueId()).breedRefundChance()
                : 0.0D;
        if (chance <= 0 || breedItem == null || breedItem.getType().isAir()) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        ItemStack refund = breedItem.clone();
        refund.setAmount(BREED_REFUND_AMOUNT);
        player.getScheduler().run(plugin, task -> {
            player.getInventory().addItem(refund).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            messages.send(player, MessageKey.HUSBANDRY_BREED_REFUND, Map.of("miktar", Integer.toString(BREED_REFUND_AMOUNT)));
        }, null);
    }

    /**
     * The hayvancılık {@code BREED_TWIN} milestone: one breeding occasionally
     * produces a second baby.
     *
     * <p>Spawned as a copy of the baby that was actually born rather than of
     * either parent, so a mooshroom stays a mooshroom and a bred variant is not
     * silently normalised. It is deliberately <em>not</em> put into love mode or
     * given any head start - it is an extra animal, not a shortcut past the
     * growing-up timer.</p>
     */
    private void spawnTwinIfLucky(Player player, EntityBreedEvent event) {
        double chance = toggles.isEnabled(player, SkillMilestoneEffectType.BREED_TWIN)
                ? skills.milestoneModifiers(player.getUniqueId()).breedTwinChance()
                : 0.0D;
        Entity newborn = event.getEntity();
        if (chance <= 0 || !(newborn instanceof Ageable)) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        newborn.getScheduler().run(plugin, task -> {
            Entity twin = newborn.getWorld().spawnEntity(newborn.getLocation(), newborn.getType());
            if (twin instanceof Ageable twinAge) {
                twinAge.setBaby();
            }
            newborn.getWorld().spawnParticle(Particle.HEART, newborn.getLocation().add(0, 0.6D, 0), 8, 0.3D, 0.3D, 0.3D, 0.0D);
            messages.send(player, MessageKey.HUSBANDRY_BREED_TWIN);
        }, null);
    }

    private boolean isEligible(Player player) {
        return activation.cached(player.getUniqueId()) && SkillAwardHandler.isGameplayGameMode(player);
    }

    private void award(Player player, SkillAction action, Material context) {
        skills.awardAction(player.getUniqueId(), player.getName(), action, context)
                .thenAccept((Optional<SkillAwardResult> result) -> awards.handle(player, result));
    }
}
