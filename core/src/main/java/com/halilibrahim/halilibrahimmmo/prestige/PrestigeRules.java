package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;

/** Pure prestige eligibility and transition rules. */
public final class PrestigeRules {
    private PrestigeRules() { throw new UnsupportedOperationException("PrestigeRules is a static utility class"); }
    public static PrestigeResult.Status eligibility(PlayerProfile profile, int maximumLevel, int maximumPrestige) {
        if (profile.prestige() >= maximumPrestige) return PrestigeResult.Status.MAXIMUM_REACHED;
        return profile.globalLevel() >= maximumLevel ? PrestigeResult.Status.PRESTIGED : PrestigeResult.Status.LEVEL_REQUIRED;
    }
    public static PlayerProfile apply(PlayerProfile profile) {
        return new PlayerProfile(profile.playerId(), profile.playerName(), 1, 0, profile.prestige() + 1, profile.selectedClass());
    }
}
