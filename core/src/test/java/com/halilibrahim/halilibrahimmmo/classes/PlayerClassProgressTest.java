package com.halilibrahim.halilibrahimmmo.classes;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure-logic coverage of the per-pool spend counting the two-category talent
 * budget rests on - the same convention {@code PrestigeRulesTest}/
 * {@code UnlockConditionsTest} follow for the rest of this codebase's
 * dependency-free rules.
 */
final class PlayerClassProgressTest {

    private static final UUID PLAYER = UUID.randomUUID();

    private static final Map<String, PlayerClassDefinition> DEFINITIONS = Map.of(
            "avci", definition("avci", Map.of(
                    "canlilik", talent("canlilik", ClassEffectType.MAX_HEALTH, ""),
                    "odak-ritmi", talent("odak-ritmi", ClassEffectType.ABILITY_COOLDOWN_REDUCTION, "avci-odagi"),
                    "odak-suresi", talent("odak-suresi", ClassEffectType.ABILITY_DURATION_BONUS, "avci-odagi")
            )),
            "madenci", definition("madenci", Map.of(
                    "dayaniklilik", talent("dayaniklilik", ClassEffectType.MAX_ENERGY, "")
            ))
    );

    @Test
    void countsEachPoolSeparatelyAcrossEveryClass() {
        PlayerClassProgress progress = progressWith(Map.of(
                "avci", Map.of("canlilik", 3, "odak-ritmi", 2, "odak-suresi", 1),
                "madenci", Map.of("dayaniklilik", 4)
        ));
        assertEquals(7, progress.spentPoints(DEFINITIONS, TalentPointCategory.OZELLIK));
        assertEquals(3, progress.spentPoints(DEFINITIONS, TalentPointCategory.KABILIYET));
        assertEquals(10, progress.spentPoints());
    }

    /**
     * Ranks whose talent or class was removed from configuration count against
     * neither pool, so deleting an entry from {@code classes.yml} refunds it
     * rather than leaving the player permanently short of points.
     */
    @Test
    void ignoresRanksWhoseTalentOrClassNoLongerExists() {
        PlayerClassProgress progress = progressWith(Map.of(
                "avci", Map.of("canlilik", 2, "kaldirilmis-yetenek", 4),
                "kaldirilmis-sinif", Map.of("canlilik", 4)
        ));
        assertEquals(2, progress.spentPoints(DEFINITIONS, TalentPointCategory.OZELLIK));
        assertEquals(0, progress.spentPoints(DEFINITIONS, TalentPointCategory.KABILIYET));
    }

    @Test
    void reportsNothingSpentForAFreshPlayer() {
        PlayerClassProgress progress = progressWith(Map.of());
        assertEquals(0, progress.spentPoints(DEFINITIONS, TalentPointCategory.OZELLIK));
        assertEquals(0, progress.spentPoints(DEFINITIONS, TalentPointCategory.KABILIYET));
    }

    private static PlayerClassProgress progressWith(Map<String, Map<String, Integer>> ranks) {
        return new PlayerClassProgress(PLAYER, ranks, 0L, Map.of());
    }

    private static PlayerClassDefinition definition(String id, Map<String, TalentNodeDefinition> talents) {
        return new PlayerClassDefinition(id, id, Set.of(), ClassBarColor.YESIL, null, null, talents, ClassEffectType.MAX_HEALTH, 1);
    }

    private static TalentNodeDefinition talent(String id, ClassEffectType type, String abilityId) {
        return new TalentNodeDefinition(id, id, 4, type, abilityId, 1);
    }
}
