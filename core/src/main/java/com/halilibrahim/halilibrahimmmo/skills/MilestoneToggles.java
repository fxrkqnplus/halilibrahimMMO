package com.halilibrahim.halilibrahimmmo.skills;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which unlocked milestone features a player has chosen to switch off.
 *
 * <p>Every one of these is a <em>convenience</em> that occasionally is not:
 * whole-tree felling is exactly wrong when clearing a single sapling, a 3x3
 * excavation is exactly wrong when laying a one-block path, and auto-replant is
 * exactly wrong when converting a field to something else. Earning a milestone
 * should not mean losing the ability to do the ordinary thing, so each of them
 * can be turned off and on again from the skill's own milestone screen.</p>
 *
 * <p><strong>Stored in the player's own {@code PersistentDataContainer}, not in
 * a table.</strong> The server already saves that container with the player
 * file, so the preference survives a restart with no schema, no migration and
 * no retention question - and unlike a plugin table, it is deleted along with
 * the player if their data ever is. The same reasoning §14.1 used for keeping
 * placed-block positions in the chunk's own container rather than in a
 * {@code hmmo_} table.</p>
 *
 * <p><strong>Stored as the disabled set rather than the enabled set</strong>, so
 * a player who has never touched the screen - and a milestone type added in a
 * later phase - both default to on. Nobody has to opt in to something they
 * already earned.</p>
 */
public final class MilestoneToggles {

    private final NamespacedKey key;

    public MilestoneToggles(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "kapali-kilometre-taslari");
    }

    /** Whether {@code type} should currently fire for this player. */
    public boolean isEnabled(Player player, SkillMilestoneEffectType type) {
        return !type.isToggleable() || !disabled(player).contains(type.name());
    }

    /** Flips one feature, and answers with its new state. */
    public boolean toggle(Player player, SkillMilestoneEffectType type) {
        if (!type.isToggleable()) {
            return true;
        }
        Set<String> current = new LinkedHashSet<>(disabled(player));
        boolean nowEnabled = current.remove(type.name());
        if (!nowEnabled) {
            current.add(type.name());
        }
        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, String.join(",", current));
        return nowEnabled;
    }

    private Set<String> disabled(Player player) {
        String raw = player.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        // A name that no longer matches any type - one removed in a later phase -
        // is simply carried along and ignored, which costs nothing and keeps the
        // rest of the player's choices intact.
        return Arrays.stream(raw.split(","))
                .map(name -> name.strip().toUpperCase(Locale.ROOT))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
