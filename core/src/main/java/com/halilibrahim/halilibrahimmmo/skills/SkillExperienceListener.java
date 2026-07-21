package com.halilibrahim.halilibrahimmmo.skills;

import java.util.UUID;

/**
 * Called by {@link CachedSkillService} after every skill XP award, so a
 * cross-cutting subsystem (the class-synergy bridge, wired in the plugin
 * bootstrap) can react without this package depending on that subsystem -
 * mirroring the {@code Function}/{@code AtomicReference} bridge already used
 * between {@code abilities} and {@code classes}.
 */
@FunctionalInterface
public interface SkillExperienceListener {
    void onSkillExperienceGained(UUID playerId, String playerName, String skillId, long grantedAmount);
}
