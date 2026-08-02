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
    LIFE_STEAL,
    /**
     * Armed for a window and <em>not</em> consumed by the first break: every
     * eligible break while it lasts clears a cube of {@code limit} blocks
     * around itself, of whatever the pickaxe can dig rather than of the
     * ability's own skill's materials. The mining line's tunnelling tool -
     * where {@link #BLAST_MINE} was one enormous swing, this is a shape the
     * player keeps digging with until the energy bar says stop.
     */
    BULLDOZER,
    /**
     * Instant: places a harmless charge that clears filler stone in a large
     * sphere without touching a single ore, a creature or a player, then leaves
     * the exposed ores glowing briefly. {@code limit} is the radius.
     */
    MINE_BOMB,
    /**
     * Instant: raises the held pickaxe's Efficiency by {@code limit} levels for
     * the ability's duration, stacking on whatever it already had, and puts it
     * back exactly as it was afterwards.
     */
    TOOL_EFFICIENCY,
    /**
     * Instant: for the ability's duration nothing hostile can see the player -
     * every creature currently hunting them loses its target and cannot
     * re-acquire it - and they get Invisibility and Speed to leave with.
     */
    PHANTOM_STEP,
    /**
     * Instant: the wild and farmed animals around the player turn on whatever
     * is hunting them, charging it, shoving it back and worrying at it for the
     * ability's duration, while the player keeps them alive out of their own
     * energy. {@code limit} is how many answer the call,
     * {@code damageMultiplier} is what one of their hits is worth and
     * {@code energyPerHit} is what restoring one of their hearts costs. Tamed
     * animals never answer - see {@code nearbyGuardians} for why.
     */
    GUARDIAN_CALL,
    /**
     * Instant, and only after dark: the player becomes the thing in the woods.
     *
     * <p>For the ability's duration they wear no armour, run faster, hit harder
     * and take less; nothing hostile will come near them; the wolves and dogs
     * around them tear into whatever they are fighting; and every ordinary
     * animal that can see them runs. {@code limit} is how far all of that
     * reaches.</p>
     *
     * <p>It shares Kan Yemini's whole presentation - the pulsing red screen, the
     * heartbeat, the creatures glowing through it and the combo ladder - because
     * the two are the same idea told twice: one is a hunter who has gone too
     * far, the other is what is hunting him.</p>
     */
    NOBLE_WOLF;

    /**
     * Whether this ability drives the client's blood-red presentation: the
     * pulsing screen edge, the heartbeat, the creatures glowing brighter as they
     * close, and the combo ladder counting what the player has torn through.
     *
     * <p>Answered here rather than by naming ability ids anywhere, so a server
     * owner who authors a third ability of this kind gets the whole presentation
     * by choosing the effect type and nothing else.</p>
     */
    public boolean isFrenzy() {
        return this == LIFE_STEAL || this == NOBLE_WOLF;
    }

    /**
     * Whether this ability keeps working after its first block break, rather
     * than being spent by it.
     *
     * <p>A one-shot sweep is a single decision: point it at the right block and
     * it is over. These two are a <em>mode</em> - the player keeps felling, keeps
     * tunnelling, and what ends it is the energy bar or the clock, which is
     * exactly what makes the per-block energy cost the interesting number rather
     * than a formality.</p>
     */
    public boolean isSustainedSweep() {
        return this == TREE_FELL || this == BULLDOZER;
    }

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
        return this == KNOCKBACK_SLAM || this == LOVE_CALL || this == MINE_BOMB;
    }
}
