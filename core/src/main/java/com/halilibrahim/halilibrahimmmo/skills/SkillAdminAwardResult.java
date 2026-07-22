package com.halilibrahim.halilibrahimmmo.skills;

/** Result of an admin-granted skill level/XP mutation: the level just before it, the resulting level, and whether it just increased. */
public record SkillAdminAwardResult(int previousLevel, int newLevel, boolean leveledUp) {
}
