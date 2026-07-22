package com.halilibrahim.halilibrahimmmo.prestige;

import java.util.Map;

/**
 * Per-condition breakdown of prestige eligibility, for a requirement
 * checklist display (e.g. {@code PrestigeMenu}/{@code PrestigeCommand}) -
 * not just a pass/fail {@code PrestigeResult.Status}.
 */
public record PrestigeEligibilityReport(
        boolean classSelected,
        Map<String, Boolean> skillsMet,
        boolean classLevelMet,
        boolean synergySkillMet
) {
    public boolean allMet() {
        return classSelected && classLevelMet && synergySkillMet && skillsMet.values().stream().allMatch(Boolean::booleanValue);
    }
}
