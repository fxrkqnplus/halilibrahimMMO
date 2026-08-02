package com.halilibrahim.halilibrahimmmo.boss;

/**
 * How much of a world boss's behaviour is actually thought rather than reflex.
 *
 * <p>Every capability the boss brain can exercise is declared here rather than
 * as its own {@code bosslar.yml} switch, for the same reason
 * {@code TalentPointCategory} is derived from {@code ClassEffectType} rather
 * than declared: nine independent booleans per rarity is nine chances for a
 * server owner to configure a boss that breaks a village's doors but cannot
 * open a fence gate, and no combination of that shape is one anybody wants. One
 * named level says how clever the creature is, and everything else follows.</p>
 *
 * <p><strong>The ladder is deliberately monotone</strong> - a higher level can
 * do everything a lower one can and more, which is what makes "a stronger boss
 * is a smarter boss" true by construction instead of by careful editing.
 * {@link #atLeast(BossIntelligence)} is how every capability is answered, so
 * adding a level in the middle later cannot silently drop a capability from the
 * tiers above it.</p>
 *
 * <p>What separates the top two levels is not a capability but a number:
 * {@code bosslar.yml} gives them faster recovery and more escapes. A capability
 * that only the very rarest boss has would be a capability almost nobody ever
 * sees.</p>
 */
public enum BossIntelligence {

    /** Reflex only: walks, swings, and wanders. What a very strong ordinary mob is. */
    SERSEM,
    /** Notices the world: climbs, opens what opens, and picks fights with the livestock. */
    UYANIK,
    /** Reads terrain and doors: avoids caves and ledges, breaks through, and keeps its distance when armed with a bow. */
    KURNAZ,
    /** Plays the map: everything above, plus it goes for villages. */
    USTA,
    /** Same repertoire as {@link #USTA}, executed with the numbers {@code bosslar.yml} gives the rarest tiers. */
    DEHA;

    /** Whether this level is {@code floor} or anything above it. */
    public boolean atLeast(BossIntelligence floor) {
        return ordinal() >= floor.ordinal();
    }

    /** Wanders the map on its own instead of standing where it spawned. Every boss does this. */
    public boolean roams() {
        return true;
    }

    /** Jumps a block, climbs a ladder or a vine, and steps up onto what a player could step onto. */
    public boolean climbs() {
        return atLeast(UYANIK);
    }

    /** Opens trapdoors and fence gates in its way rather than standing in front of them. */
    public boolean opensGates() {
        return atLeast(UYANIK);
    }

    /** Picks on the harmless while it has nothing else to do - livestock, pets and their young. */
    public boolean hunts() {
        return atLeast(UYANIK);
    }

    /** Stays out of caves and off cliff edges when choosing where to wander. */
    public boolean avoidsHazards() {
        return atLeast(KURNAZ);
    }

    /** Breaks a door it cannot walk around. */
    public boolean breaksDoors() {
        return atLeast(KURNAZ);
    }

    /** Backs away to shooting range and takes aimed shots instead of closing to melee with a bow in hand. */
    public boolean kites() {
        return atLeast(KURNAZ);
    }

    /** Runs at low health, mends itself somewhere quiet, and comes back. */
    public boolean recovers() {
        return atLeast(KURNAZ);
    }

    /** Goes for a village it can see: the golem first, then the doors, the crops and the people. */
    public boolean raidsVillages() {
        return atLeast(USTA);
    }

    /**
     * The level a rarity gets when {@code bosslar.yml} does not name one.
     *
     * <p>Deliberately derived from the rarity rather than defaulting to
     * {@link #SERSEM}: a server whose configuration predates this system would
     * otherwise silently get five tiers of identical, thoughtless bosses and no
     * indication why.</p>
     */
    public static BossIntelligence forRarity(BossRarity rarity) {
        return values()[Math.min(values().length - 1, rarity.ordinal())];
    }
}
