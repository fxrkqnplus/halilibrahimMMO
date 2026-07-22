package com.halilibrahim.halilibrahimmmo.classes;

/** Result of a class-level XP award: the level just before the award, the resulting level, and whether it just increased. */
public record ClassExperienceAwardResult(int previousLevel, int newLevel, boolean leveledUp) {
}
