package com.halilibrahim.halilibrahimmmo.abilities;

import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.Optional;

/**
 * One ability's own colour, activation sound cue and activation particle
 * burst ({@code abilities.yml}'s {@code gorsel} section), so two abilities of
 * the same {@link AbilityEffectType} still look and sound nothing alike.
 *
 * <p>{@code sound} and {@code particle} are both optional: an ability that
 * declares neither simply falls back to the single shared
 * {@code kabiliyet-baslangic} cue in {@code feedback.yml} and spawns nothing,
 * i.e. exactly the pre-Phase-13 behaviour. {@code colour} always has a value
 * ({@link AbilityColor#BEYAZ} by default) because it also tints the particle
 * burst and the ability's name in {@code AbilitiesMenu}.</p>
 */
public record AbilityVisual(
        AbilityColor color,
        Optional<Sound> sound,
        float volume,
        float pitch,
        Optional<Particle> particle,
        int particleCount
) {

    /** The neutral visual an ability with no {@code gorsel} section gets. */
    public static AbilityVisual defaults() {
        return new AbilityVisual(AbilityColor.BEYAZ, Optional.empty(), 1.0F, 1.0F, Optional.empty(), 0);
    }
}
