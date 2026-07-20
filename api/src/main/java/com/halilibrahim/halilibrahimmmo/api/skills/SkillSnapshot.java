package com.halilibrahim.halilibrahimmmo.api.skills;

import java.util.UUID;

/** Immutable public view of one player's persistent skill progression. */
public record SkillSnapshot(UUID playerId, String skillId, int level, long experience) {
}
