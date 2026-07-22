package com.halilibrahim.halilibrahimmmo.prestige;

import java.util.Map;

/**
 * Pure inputs {@link PrestigeRules#eligibility(PrestigeEligibilityInput)}
 * needs to decide prestige eligibility - gathered by
 * {@code YamlPrestigeService} from {@code SkillService}/{@code ClassService}
 * since this record itself has no service dependencies.
 */
public record PrestigeEligibilityInput(
        Map<String, Integer> skillLevels,
        int minimumSkillLevel,
        boolean classSelected,
        int classLevel,
        int classLevelCap,
        int synergySkillLevel,
        int synergySkillCap,
        int currentPrestige,
        int maximumPrestige
) {
}
