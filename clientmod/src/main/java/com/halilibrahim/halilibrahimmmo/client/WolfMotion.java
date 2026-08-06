package com.halilibrahim.halilibrahimmmo.client;

/**
 * What one transformed player's body is doing right now, as five numbers the
 * animation can blend between.
 *
 * <p><strong>Smoothed values rather than booleans, and that is the whole point
 * of the record existing.</strong> "Airborne" is a flag that flips in one tick,
 * and a pose driven straight off a flag snaps: a wolf that is standing one frame
 * and mid-leap the next reads as a glitch, not as a jump. Every field here is a
 * 0..1 (or -1..1) weight that {@link WolfFormTicker} eases toward its target
 * twenty times a second, so {@link WerewolfModel} can simply mix poses by weight
 * and never has to know a transition happened.</p>
 *
 * <p>It lives outside the model for a second reason: one {@link WerewolfModel}
 * instance draws <em>every</em> transformed player, so it cannot hold per-player
 * state of its own. Anything that has to remember what a particular wolf was
 * doing last tick has to be keyed by entity id somewhere else, and that
 * somewhere is {@link WolfFormState}.</p>
 *
 * @param air    1 while off the ground, 0 while standing on it
 * @param rise   +1 climbing at full speed, -1 falling at terminal speed, 0 level
 * @param slam   1 while the ground-slam dive is being held (crouch, in mid-air)
 * @param sprint 1 at a full run, 0 at a walk or standing still
 * @param crouch 1 while sneaking on the ground
 */
public record WolfMotion(float air, float rise, float slam, float sprint, float crouch) {

    /** A wolf standing still on the ground - what an unknown entity is assumed to be doing. */
    public static final WolfMotion RESTING = new WolfMotion(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    /**
     * Eases every field toward {@code target}.
     *
     * <p>Two rates, not one. Rising into a pose is fast because a leap has to
     * look like it was thrown; settling back out of one is slower because a
     * landing is a body absorbing something. One rate for both made every
     * landing look like the animation had been switched off.</p>
     */
    public WolfMotion easeToward(WolfMotion target, float rise, float fall) {
        return new WolfMotion(
                ease(air, target.air, rise, fall),
                ease(this.rise, target.rise, rise, fall),
                ease(slam, target.slam, rise, fall),
                ease(sprint, target.sprint, rise, fall),
                ease(crouch, target.crouch, rise, fall)
        );
    }

    private static float ease(float current, float target, float upRate, float downRate) {
        float rate = Math.abs(target) > Math.abs(current) ? upRate : downRate;
        return current + (target - current) * rate;
    }
}
