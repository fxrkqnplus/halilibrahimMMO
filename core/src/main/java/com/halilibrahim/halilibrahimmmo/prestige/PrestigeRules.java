package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure prestige eligibility and transition rules. Eligibility (Phase 12)
 * requires every configured skill to be at least {@code minimumSkillLevel},
 * the selected class's class-level to be at its cap, and that class's
 * synergy skill to be at its cap - replacing the earlier flat Güç threshold.
 */
public final class PrestigeRules {
    private PrestigeRules() { throw new UnsupportedOperationException("PrestigeRules is a static utility class"); }

    public static PrestigeEligibility eligibility(PrestigeEligibilityInput input) {
        PrestigeEligibilityReport report = report(input);
        if (input.currentPrestige() >= input.maximumPrestige()) {
            return new PrestigeEligibility(PrestigeResult.Status.MAXIMUM_REACHED, report);
        }
        PrestigeResult.Status status = report.allMet() ? PrestigeResult.Status.PRESTIGED : PrestigeResult.Status.REQUIREMENTS_NOT_MET;
        return new PrestigeEligibility(status, report);
    }

    private static PrestigeEligibilityReport report(PrestigeEligibilityInput input) {
        Map<String, Boolean> skillsMet = new LinkedHashMap<>();
        input.skillLevels().forEach((skillId, level) -> skillsMet.put(skillId, level >= input.minimumSkillLevel()));
        boolean classLevelMet = input.classSelected() && input.classLevel() >= input.classLevelCap();
        boolean synergySkillMet = input.classSelected() && input.synergySkillLevel() >= input.synergySkillCap();
        return new PrestigeEligibilityReport(input.classSelected(), Map.copyOf(skillsMet), classLevelMet, synergySkillMet);
    }

    /**
     * Only increments the prestige count - {@link PlayerProfile#globalLevel()}/
     * {@link PlayerProfile#globalExperience()} are dead fields since Güç became
     * derived (Phase 11) and are left untouched. The actual reset (selected
     * class's class-level, all 4 skills) and the Güç-banking write happen in
     * {@code YamlPrestigeService#prestige(...)}, which has access to
     * {@code SkillService}/{@code ClassService}/{@code BankedPowerService}
     * that this pure function does not.
     */
    public static PlayerProfile apply(PlayerProfile profile) {
        return new PlayerProfile(profile.playerId(), profile.playerName(), profile.globalLevel(), profile.globalExperience(),
                profile.prestige() + 1, profile.selectedClass());
    }
}
