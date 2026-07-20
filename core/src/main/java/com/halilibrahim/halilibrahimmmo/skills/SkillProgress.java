package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.api.skills.SkillSnapshot;

import java.util.UUID;

/** Immutable internal skill state at the cache and repository boundary. */
public record SkillProgress(UUID playerId, String skillId, int level, long experience) {

    public SkillSnapshot snapshot() {
        return new SkillSnapshot(playerId, skillId, level, experience);
    }
}
