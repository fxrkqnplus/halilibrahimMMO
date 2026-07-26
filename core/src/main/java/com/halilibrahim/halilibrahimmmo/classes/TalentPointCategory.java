package com.halilibrahim.halilibrahimmmo.classes;

/**
 * The two separate pools a talent point can come from. A point of one kind can
 * never be spent on the other kind of talent, which is the whole point of the
 * split: a player who wants tougher abilities cannot get there by giving up
 * raw stats, and vice versa.
 *
 * <p>A talent's category is <em>derived</em> from its {@link ClassEffectType}
 * rather than declared separately in {@code classes.yml} - an ability-scoped
 * effect needs an ability id and a stat effect does not, so the two are
 * already structurally distinct and a redundant {@code kategori} key could
 * only ever disagree with reality.</p>
 */
public enum TalentPointCategory {
    /** Permanent character stats: health, PvE damage, energy capacity. */
    OZELLIK,
    /** Modifiers scoped to one named ability: energy cost, cooldown, limit, duration, strength. */
    KABILIYET
}
