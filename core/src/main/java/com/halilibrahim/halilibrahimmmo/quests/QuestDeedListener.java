package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The named-deed objectives ({@link QuestDeed}).
 *
 * <p>These are the objectives that could not be expressed as "break N of this"
 * or "kill N of that". Most are a single event handler; the ghast one is not,
 * and is the reason this listener has state: Minecraft never tells you "a ghast
 * died to its own fireball", so the damage event has to notice the reflection
 * first and remember which ghast it happened to, and the death event then asks
 * whether that ghast is the one dying.</p>
 */
public final class QuestDeedListener implements Listener {

    private final QuestService quests;
    private final ActivationService activation;
    private final QuestCompletionHandler completions;
    /** Ghasts last hit by their own fireball, remembered only long enough for the death event to read it. */
    private final Set<UUID> ghastsHitByOwnFireball = ConcurrentHashMap.newKeySet();
    /** Who reflected it, so the credit goes to the player and not to the ghast. */
    private final ConcurrentHashMap<UUID, UUID> ghastReflectors = new ConcurrentHashMap<>();

    public QuestDeedListener(QuestService quests, ActivationService activation, QuestCompletionHandler completions) {
        this.quests = quests;
        this.activation = activation;
        this.completions = completions;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPiglinBarter(PiglinBarterEvent event) {
        Player nearest = nearestEligiblePlayer(event.getEntity().getLocation().getWorld(), event.getEntity().getUniqueId());
        if (nearest != null) {
            award(nearest, QuestDeed.PIGLIN_TAKASI);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVillagerTrade(PlayerTradeEvent event) {
        award(event.getPlayer(), QuestDeed.KOYLU_TAKASI);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        award(event.getEnchanter(), QuestDeed.BUYULEME);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            award(event.getPlayer(), QuestDeed.BALIK_TUTMA);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player player) {
            award(player, QuestDeed.EVCILLESTIRME);
        }
    }

    /**
     * A ghast hit by a fireball whose shooter is that same ghast - which is what
     * a reflected fireball looks like from the server's side. The player who
     * deflected it is not part of this event at all, so the nearest eligible
     * player is credited; a ghast fight is not a crowd scene.
     */
    @EventHandler(ignoreCancelled = true)
    public void onGhastDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Ghast ghast) || !(event.getDamager() instanceof Fireball fireball)) {
            return;
        }
        if (!(fireball.getShooter() instanceof Ghast shooter) || !shooter.getUniqueId().equals(ghast.getUniqueId())) {
            return;
        }
        Player reflector = nearestEligiblePlayer(ghast.getWorld(), ghast.getUniqueId());
        if (reflector == null) {
            return;
        }
        ghastsHitByOwnFireball.add(ghast.getUniqueId());
        ghastReflectors.put(ghast.getUniqueId(), reflector.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onGhastDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Ghast ghast) || !ghastsHitByOwnFireball.remove(ghast.getUniqueId())) {
            return;
        }
        UUID reflectorId = ghastReflectors.remove(ghast.getUniqueId());
        Player reflector = reflectorId == null ? null : ghast.getServer().getPlayer(reflectorId);
        if (reflector != null) {
            award(reflector, QuestDeed.GHAST_ALEV_TOPU);
        }
    }

    /**
     * The closest activated, playing player in the world - for the two deeds
     * whose events carry a creature rather than a player. Deliberately
     * unbounded in range beyond the world itself: both events only fire while
     * someone is interacting with the creature, so "closest" is "the one doing
     * it" in every realistic case.
     */
    private Player nearestEligiblePlayer(org.bukkit.World world, UUID anchorId) {
        var anchor = world.getEntity(anchorId);
        if (anchor == null) {
            return null;
        }
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player player : new HashSet<>(world.getPlayers())) {
            if (!isEligible(player)) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(anchor.getLocation());
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private void award(Player player, QuestDeed deed) {
        if (!isEligible(player)) {
            return;
        }
        quests.awardDeed(player.getUniqueId(), deed, player.getWorld().getEnvironment())
                .thenAccept(completed -> completions.notifyCompleted(player, completed));
    }

    /** The same activation and gamemode gate every other quest path applies. */
    private boolean isEligible(Player player) {
        return activation.cached(player.getUniqueId())
                && player.getGameMode() != GameMode.CREATIVE
                && player.getGameMode() != GameMode.SPECTATOR;
    }
}
