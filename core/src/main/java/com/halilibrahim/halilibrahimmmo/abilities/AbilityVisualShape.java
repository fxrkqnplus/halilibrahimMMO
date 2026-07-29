package com.halilibrahim.halilibrahimmmo.abilities;

/**
 * How an ability's activation particles are laid out in space
 * ({@code abilities.yml}'s {@code gorsel.sekil}), on top of <em>which</em>
 * particle and colour it uses.
 *
 * <p>Shape is a separate axis from particle type on purpose: the same
 * {@code CRIT} particle reads as a blade flourish in a rising helix and as a
 * shockwave in expanding rings, so two combat abilities can share a particle
 * and still be told apart at a glance. Each shape is a bounded, short animation
 * measured in ticks - nothing here can outlive its own activation.</p>
 */
public enum AbilityVisualShape {
    /** A scatter around the caster - the original behaviour, and the default. */
    PATLAMA,
    /** Rings expanding outward at foot height, for anything that pushes or sweeps. */
    HALKA,
    /** A helix winding up the caster's body, for anything that buffs them. */
    SARMAL,
    /** A hemisphere shell overhead, for anything that reveals or blesses an area. */
    KUBBE
}
