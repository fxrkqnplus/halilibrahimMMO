package com.halilibrahim.halilibrahimmmo.progression;

import com.halilibrahim.halilibrahimmmo.api.progression.ProgressionCause;

import java.util.Map;

/** Immutable parsed settings from {@code progression.yml}. */
public record ProgressionSettings(ExperienceCurve curve, Map<ProgressionCause, Long> rewards) {
    public long rewardFor(ProgressionCause cause) {
        return rewards.getOrDefault(cause, 0L);
    }
}
