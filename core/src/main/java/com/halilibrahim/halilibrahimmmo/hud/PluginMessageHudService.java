package com.halilibrahim.halilibrahimmmo.hud;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityEffectType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin-message implementation of {@link HudService}.
 *
 * <p>Stateless by design: every message carries the whole picture, so a client
 * that joined late, reconnected, or dropped a packet is corrected by the next
 * one rather than having to be resynchronised. The activation feedback task
 * already re-sends once a second while an ability runs, which is what makes
 * that true in practice.</p>
 */
public final class PluginMessageHudService implements HudService {

    /**
     * How strongly the screen edge is tinted, by which rung of its class the
     * ability sits on.
     *
     * <p>"Gücüne ve etkisine göre" has to be answered by something the server
     * already knows, and the unlock level is exactly that: every class's four
     * abilities sit at 10/20/35/50 and get stronger up the ladder, so the rung
     * <em>is</em> the ability's weight. Tying the tint to, say, the damage
     * number instead would have made a mining tool and a combat buff
     * incomparable.</p>
     */
    private static final int[] RUNG_LEVELS = {10, 20, 35, 50};
    private static final int[] RUNG_INTENSITY = {25, 45, 70, 100};

    private final JavaPlugin plugin;

    public PluginMessageHudService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, HudProtocol.CHANNEL);
    }

    @Override
    public void disable() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, HudProtocol.CHANNEL);
    }

    @Override
    public void showAbility(Player player, AbilityDefinition definition, long expiresAtMillis, long totalMillis) {
        long remaining = expiresAtMillis <= 0 ? -1L : expiresAtMillis - System.currentTimeMillis();
        int flags = definition.effectType() == AbilityEffectType.LIFE_STEAL ? HudProtocol.FLAG_HIGHLIGHT_HOSTILES : 0;
        player.sendPluginMessage(plugin, HudProtocol.CHANNEL, HudProtocol.ability(
                definition.id(),
                definition.displayName(),
                definition.icon().name(),
                definition.visual().color().effectColor().asRGB(),
                remaining < 0 ? HudProtocol.NO_EXPIRY : (int) Math.min(Integer.MAX_VALUE, remaining),
                (int) Math.min(Integer.MAX_VALUE, Math.max(0L, totalMillis)),
                intensityOf(definition),
                flags
        ));
    }

    @Override
    public void clearAbility(Player player) {
        player.sendPluginMessage(plugin, HudProtocol.CHANNEL, HudProtocol.clear());
    }

    private int intensityOf(AbilityDefinition definition) {
        for (int index = RUNG_LEVELS.length - 1; index >= 0; index--) {
            if (definition.requiredLevel() >= RUNG_LEVELS[index]) {
                return RUNG_INTENSITY[index];
            }
        }
        return RUNG_INTENSITY[0];
    }
}
