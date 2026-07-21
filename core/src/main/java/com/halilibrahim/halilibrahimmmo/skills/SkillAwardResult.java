package com.halilibrahim.halilibrahimmmo.skills;

/**
 * Result of a skill XP award: the resulting progress, the level it was at
 * just before this award (to detect multi-level jumps precisely), the actual
 * XP amount granted (post class-synergy bonus, if any), whether it just
 * leveled up, and the specific block/mob's Turkish display name that earned
 * it (for bossbar feedback, e.g. "Madencilik +14xp (Elmas Cevheri)").
 */
public record SkillAwardResult(SkillProgress progress, int previousLevel, long grantedAmount, boolean leveledUp, String sourceDisplayName) {
}
