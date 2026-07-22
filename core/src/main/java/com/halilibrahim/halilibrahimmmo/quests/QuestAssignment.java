package com.halilibrahim.halilibrahimmmo.quests;

import java.util.Set;

/** One player's currently-assigned quest ids for a single DAILY/WEEKLY period, and when that period ends. */
record QuestAssignment(Set<String> questIds, long periodEndMillis) {
}
