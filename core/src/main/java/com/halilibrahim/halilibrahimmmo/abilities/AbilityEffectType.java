package com.halilibrahim.halilibrahimmmo.abilities;

/** Built-in, bounded gameplay effects available to YAML ability definitions. */
public enum AbilityEffectType {
    EXCAVATE,
    TREE_FELL,
    VEIN_MINE,
    DAMAGE_BUFF,
    /** Armed for a window: chance (in {@code damageMultiplier}) to drop an extra copy of a normally broken block's drop. */
    EXTRA_DROP_CHANCE,
    /** Instant: near-total damage reduction ({@link org.bukkit.potion.PotionEffectType#RESISTANCE}) for the ability's duration. */
    BRIEF_INVULNERABILITY,
    /** Instant: self-only {@link org.bukkit.potion.PotionEffectType#REGENERATION} for the ability's duration. */
    AREA_HEAL,
    /** Instant: {@link org.bukkit.potion.PotionEffectType#SPEED} for the ability's duration. */
    SPEED_BURST,
    /** Instant: pushes and lightly damages nearby entities within {@code limit} blocks. */
    KNOCKBACK_SLAM,
    /** Instant: {@link org.bukkit.potion.PotionEffectType#NIGHT_VISION} for the ability's duration. */
    NIGHT_VISION,
    /**
     * Instant: outlines every block of the ability's own skill's material set
     * within {@code limit} blocks with a glowing, see-through-walls
     * {@link org.bukkit.entity.BlockDisplay} for the ability's duration.
     * Usable only inside a wide, roofed mine - see {@code damageMultiplier}
     * on {@link AbilityDefinition} for the open-air ratio that gate reads.
     */
    ORE_REVEAL,
    /**
     * Instant: brings every immature crop within {@code limit} blocks straight
     * to harvest, like a wave of bone meal that costs nothing but energy.
     */
    GROWTH_SURGE,
    /**
     * Instant: puts every breedable adult animal within {@code limit} blocks
     * into love mode for the ability's duration, pairing up a whole herd at
     * once without spending a single breeding item.
     */
    LOVE_CALL,
    /**
     * Armed for a window: the next eligible block break clears a solid cube of
     * {@code limit} blocks around it, restricted to the ability's own skill's
     * materials. Distinct from {@link #EXCAVATE} (a flat 3x3 plane) and
     * {@link #VEIN_MINE} (only blocks of the one broken type) - this is the
     * mining line's heaviest tool and takes a cube out of a wall in one swing.
     */
    BLAST_MINE,
    /**
     * Buffed for a window: a fraction ({@code damageMultiplier}) of the damage
     * dealt to a creature is returned to the attacker as health. The combat
     * line's last ability - it changes how long a fight can be sustained rather
     * than how hard one hit lands, which is what separates it from
     * {@link #DAMAGE_BUFF}.
     */
    LIFE_STEAL;

    /**
     * Whether the whole effect has already happened by the time activation
     * returns, so nothing is left for the duration to hold open.
     *
     * <p>This is what stops Creative mode from parking a one-shot ability for
     * six hours. Creative deliberately stretches an ability's duration so a
     * sustained buff stays up until it is switched off - but a shockwave has
     * already thrown everything nearby, and a growth surge has already grown
     * every crop, before the timer even starts. Holding those open only made
     * them unusable again until the player remembered to toggle them off, when
     * what they wanted was to press it again. Their configured duration is used
     * unchanged instead, which for Yıkıcı Darbe is the two seconds it takes to
     * watch the dust settle.</p>
     */
    public boolean resolvesImmediately() {
        return this == KNOCKBACK_SLAM || this == GROWTH_SURGE || this == LOVE_CALL;
    }
}
