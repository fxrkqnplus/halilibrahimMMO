package com.halilibrahim.halilibrahimmmo.classes;

/** Persistent per (player, class) level/XP - independent of any other class's own track. */
public record ClassLevelProgress(String classId, int level, long experience) {
}
