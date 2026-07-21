package com.halilibrahim.halilibrahimmmo.classes;

/** Result of a class-level XP award: the resulting level and whether it just increased. */
public record ClassExperienceAwardResult(int newLevel, boolean leveledUp) {
}
