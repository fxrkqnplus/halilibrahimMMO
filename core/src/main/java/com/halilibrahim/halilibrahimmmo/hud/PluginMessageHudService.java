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
    public void showAbility(
            Player player, AbilityDefinition definition, long expiresAtMillis,
            String comboLine, int comboTier
    ) {
        long remaining = expiresAtMillis <= 0 ? -1L : expiresAtMillis - System.currentTimeMillis();
        player.sendPluginMessage(plugin, HudProtocol.CHANNEL, HudProtocol.ability(
                definition.id(),
                definition.displayName(),
                definition.icon().name(),
                definition.visual().color().effectColor().asRGB(),
                remaining < 0 ? HudProtocol.NO_EXPIRY : (int) Math.min(Integer.MAX_VALUE, remaining),
                intensityOf(definition),
                flagsOf(definition),
                comboLine,
                comboTier
        ));
    }

    /**
     * Which of the client's optional presentations this ability turns on.
     *
     * <p>The pulse and the combo ladder ride on
     * {@link AbilityEffectType#isFrenzy()} rather than on a list of ability ids,
     * so an owner authoring a third ability of that kind gets both by choosing
     * the effect type and nothing else.</p>
     *
     * <p><strong>Who glows is the one thing the two frenzies do not share.</strong>
     * Asil Kurt is the predator the world runs from: every ordinary creature and
     * every other player is lit up, and the monsters - which the ability has
     * already made blind to the player - are not, because a thing that cannot
     * see you is not what you are watching for. Kan Yemini is the opposite
     * reading, a hunter of monsters: everything that can be killed glows and no
     * player does, so the oath never turns into a wallhack pointed at people.
     * Naming the two effect types here is deliberate - this is a statement about
     * these two abilities, not a property a third could inherit by accident.</p>
     */
    private int flagsOf(AbilityDefinition definition) {
        return switch (definition.effectType()) {
            case NOBLE_WOLF -> HudProtocol.FLAG_PULSE | HudProtocol.FLAG_PROXIMITY_GLOW
                    | HudProtocol.FLAG_GLOW_CREATURES | HudProtocol.FLAG_GLOW_PLAYERS;
            case LIFE_STEAL -> HudProtocol.FLAG_PULSE | HudProtocol.FLAG_PROXIMITY_GLOW
                    | HudProtocol.FLAG_GLOW_CREATURES | HudProtocol.FLAG_GLOW_MONSTERS;
            default -> 0;
        };
    }

    @Override
    public void clearAbility(Player player) {
        player.sendPluginMessage(plugin, HudProtocol.CHANNEL, HudProtocol.clear());
    }

    /**
     * Zero for an ability that does not tint the screen at all, and the client
     * reads zero as "draw nothing" - so the whole of §24's narrowing is carried
     * by one number that was already in the protocol.
     *
     * <p>Deliberately not a new flag. The badge, the countdown and the combo
     * line are all still sent for these abilities; the only thing switched off
     * is the border, and intensity is precisely the field that says how strong
     * the border is.</p>
     */
    private int intensityOf(AbilityDefinition definition) {
        if (!definition.effectType().tintsScreen()) {
            return 0;
        }
        for (int index = RUNG_LEVELS.length - 1; index >= 0; index--) {
            if (definition.requiredLevel() >= RUNG_LEVELS[index]) {
                return RUNG_INTENSITY[index];
            }
        }
        return RUNG_INTENSITY[0];
    }
}
