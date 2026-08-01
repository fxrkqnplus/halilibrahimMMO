package com.halilibrahim.halilibrahimmmo.server;

import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes one console line for every villager and every tamed horse, donkey,
 * mule, dog or cat that dies on this server: where, when, by whose hand, and
 * under what name.
 *
 * <p><strong>Why the console and not the database.</strong> This is a
 * moderation trail, and the question it answers is "who killed the trader
 * everyone was using" - asked once, hours later, by an owner reading a log
 * file. A table would mean a schema, a migration, a retention policy and a
 * screen to read it on, for something {@code grep} already does perfectly on
 * the file the server is writing anyway.</p>
 *
 * <p><strong>The killer is remembered separately from the death.</strong>
 * {@link EntityDeathEvent} exposes {@code getKiller()} only for a player, and
 * only when the player's own blow was the last one - a villager finished off by
 * a zombie, by fall damage after being knocked off a roof, or by a wolf a player
 * set on it would all be recorded as nobody. The last damage a creature took
 * from <em>any</em> entity is remembered at {@code MONITOR}, so the line can say
 * "a zombie" or "Vesen's wolf" instead of quietly saying nothing at all. That
 * memory is a plain map keyed by entity id and is cleaned up by the death that
 * reads it.</p>
 */
public final class ProtectedDeathLogListener implements Listener {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final PluginLogger logger;
    /** Entity id to a already-rendered description of whatever last hit it. */
    private final ConcurrentHashMap<UUID, String> lastAttacker = new ConcurrentHashMap<>();

    public ProtectedDeathLogListener(PluginLogger logger) {
        this.logger = logger;
    }

    /**
     * Remembers who struck, for any creature this listener would report on.
     *
     * <p>{@code MONITOR} with {@code ignoreCancelled}, so a hit some protection
     * plugin refused is never remembered as the cause of a death that happened
     * for another reason entirely.</p>
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim) || !isWatched(victim)) {
            return;
        }
        lastAttacker.put(victim.getUniqueId(), describe(resolveAttacker(event)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        String remembered = lastAttacker.remove(victim.getUniqueId());
        if (!isWatched(victim)) {
            return;
        }
        Player killer = victim.getKiller();
        String culprit = killer != null ? killer.getName() : remembered != null ? remembered : "bilinmiyor";
        Location at = victim.getLocation();
        logger.info(String.format(
                "[OLUM KAYDI] %s | %s | isim: %s | dunya: %s | konum: %d, %d, %d | olduren: %s",
                LocalDateTime.now().format(CLOCK),
                kindOf(victim),
                nameOf(victim),
                at.getWorld().getName(),
                at.getBlockX(), at.getBlockY(), at.getBlockZ(),
                culprit
        ));
    }

    /**
     * Villagers, and animals somebody has actually tamed.
     *
     * <p>An untamed wolf on a hillside is wildlife; the wolf sitting in
     * somebody's base is property, and that is the whole difference this
     * listener exists to record. Wandering traders count as villagers because
     * they are the same kind of loss - {@link AbstractVillager} covers both
     * without naming either.</p>
     */
    private boolean isWatched(LivingEntity entity) {
        return entity instanceof AbstractVillager
                || (entity instanceof Tameable tameable && tameable.isTamed());
    }

    /** The creature's own type name, which is what an owner reading the log is scanning for. */
    private String kindOf(LivingEntity entity) {
        return entity.getType().getKey().getKey();
    }

    /** A name tag if it has one, or a dash - never an empty column. */
    private String nameOf(LivingEntity entity) {
        Component custom = entity.customName();
        if (custom == null) {
            return "-";
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(custom).strip();
        return plain.isEmpty() ? "-" : plain;
    }

    /** Unwraps an arrow or a trident so a ranged kill names the shooter, not the projectile. */
    private Entity resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return shooter;
        }
        return event.getDamager();
    }

    /**
     * How the attacker reads in the log. A tamed animal is written as its
     * owner's, because "a wolf" is not the answer anybody is looking for when
     * somebody's wolf pack has emptied a village.
     */
    private String describe(Entity attacker) {
        if (attacker instanceof Player player) {
            return player.getName();
        }
        String kind = attacker.getType().getKey().getKey();
        if (attacker instanceof Tameable tameable && tameable.isTamed() && tameable.getOwner() != null) {
            return tameable.getOwner().getName() + " sahipli " + kind;
        }
        return kind;
    }
}
