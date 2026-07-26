package com.halilibrahim.halilibrahimmmo.skills;

/**
 * Gameplay event category that can award a configured skill passively.
 *
 * <p>Only one skill may claim a given block material and only one skill may
 * be the {@link #MOB} skill - {@code CachedSkillService#awardMatching}
 * resolves a source event to exactly one definition. {@link #ACTION} exists
 * for skills with no passive block/mob source at all: they are awarded solely
 * through explicit {@link SkillAction}s, so they can never collide with the
 * block or mob resolution path.</p>
 */
public enum SkillSource {
    BLOCK,
    MOB,
    ACTION
}
