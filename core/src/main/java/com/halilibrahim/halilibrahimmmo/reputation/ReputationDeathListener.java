package com.halilibrahim.halilibrahimmmo.reputation;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.locale.NumberFormatter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * What dying costs, and telling the player about it.
 *
 * <p><strong>The cause is decided from the player's own last damage event, not
 * from the death event.</strong> {@link PlayerDeathEvent} only exposes a killer
 * when it was a player's own final blow, so a player shot off a cliff by a
 * skeleton, finished by a wolf somebody set on them, or knocked into lava would
 * all have read as "did it to themselves" - and the cheapest band of the table
 * is exactly the one somebody would arrange to land in. The last entity to
 * damage this player is remembered, projectiles are unwrapped to their shooter,
 * and a tamed animal counts as its owner.</p>
 *
 * <p>The chat line goes only to the player who lost the points. A public
 * announcement of every death would be both noise and an invitation; the
 * server-wide half of this system is the trend announcer, which speaks once
 * every half hour or so about somebody genuinely worth mentioning.</p>
 */
public final class ReputationDeathListener implements Listener {

    private final JavaPlugin plugin;
    private final ReputationService reputation;
    private final ReputationTrendAnnouncer trends;
    private final MessageService messages;

    public ReputationDeathListener(
            JavaPlugin plugin, ReputationService reputation, ReputationTrendAnnouncer trends, MessageService messages
    ) {
        this.plugin = plugin;
        this.reputation = reputation;
        this.trends = trends;
        this.messages = messages;
    }

    /**
     * Remembers what last hit this player, so the death event can name a cause
     * the death event itself does not carry.
     *
     * <p>{@code MONITOR} with {@code ignoreCancelled}: a blow some other plugin
     * refused never happened and must not be blamed for the fall that follows.</p>
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        trends.rememberAttacker(victim.getUniqueId(), classify(resolveAttacker(event)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ReputationDeathCause cause = player.getKiller() != null
                ? ReputationDeathCause.OYUNCU
                : trends.recentCause(player.getUniqueId());
        long loss = ReputationPenalty.lossFor(reputation.cached(player.getUniqueId()), cause);
        if (loss <= 0) {
            return;
        }
        reputation.deduct(player.getUniqueId(), loss).thenAccept(taken -> {
            if (taken <= 0) {
                return;
            }
            trends.recordLoss(player.getUniqueId(), taken);
            // Back onto the player's own region before touching them: the
            // deduction resolved on a database thread.
            player.getScheduler().run(plugin, task -> messages.send(player, causeMessage(cause), Map.of(
                    "kayip", NumberFormatter.compact(taken),
                    "sayginlik", NumberFormatter.compact(reputation.cached(player.getUniqueId()))
            )), null);
        });
    }

    /**
     * One fully-authored line per cause rather than one template fed a
     * Java-built noun - the same discipline every per-rarity boss line and
     * per-band TAB footer already follows, and the only way an owner can reword
     * "a creature got you" without touching code.
     */
    private MessageKey causeMessage(ReputationDeathCause cause) {
        return switch (cause) {
            case KENDI -> MessageKey.REPUTATION_LOSS_SELF;
            case YARATIK -> MessageKey.REPUTATION_LOSS_CREATURE;
            case OYUNCU -> MessageKey.REPUTATION_LOSS_PLAYER;
        };
    }

    /** Unwraps an arrow or a trident so a sniped player is not recorded as having tripped. */
    private Entity resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return shooter;
        }
        return event.getDamager();
    }

    /**
     * A tamed animal answers for its owner. Setting a wolf pack on somebody is
     * an attack by the person who tamed it, and the table's most expensive band
     * is exactly the one that should apply.
     */
    private ReputationDeathCause classify(Entity attacker) {
        if (attacker instanceof Player) {
            return ReputationDeathCause.OYUNCU;
        }
        if (attacker instanceof Tameable tameable && tameable.isTamed() && tameable.getOwner() != null) {
            return ReputationDeathCause.OYUNCU;
        }
        return ReputationDeathCause.YARATIK;
    }
}
