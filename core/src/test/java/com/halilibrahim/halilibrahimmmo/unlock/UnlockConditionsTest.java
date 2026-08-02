package com.halilibrahim.halilibrahimmmo.unlock;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UnlockConditionsTest {

    private static final UnlockContext CONTEXT = new UnlockContext(
            25, 1, Set.of("odun-ustasi"), Set.of("degerli-madenler"), Map.of("madencilik", 50), 1_000_000L
    );

    private static UnlockCondition threshold(UnlockConditionType type, int level) {
        return new UnlockCondition(type, level, "", 0L);
    }

    private static UnlockCondition reference(UnlockConditionType type, int level, String referenceId) {
        return new UnlockCondition(type, level, referenceId, 0L);
    }

    @Test
    void levelConditionComparesGlobalLevel() {
        assertTrue(UnlockConditions.isSatisfied(threshold(UnlockConditionType.LEVEL, 25), CONTEXT));
        assertTrue(UnlockConditions.isSatisfied(threshold(UnlockConditionType.LEVEL, 10), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(threshold(UnlockConditionType.LEVEL, 26), CONTEXT));
    }

    @Test
    void prestigeConditionComparesPrestige() {
        assertTrue(UnlockConditions.isSatisfied(threshold(UnlockConditionType.PRESTIGE, 1), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(threshold(UnlockConditionType.PRESTIGE, 2), CONTEXT));
    }

    @Test
    void questCompletedConditionChecksMembership() {
        assertTrue(UnlockConditions.isSatisfied(reference(UnlockConditionType.QUEST_COMPLETED, 0, "odun-ustasi"), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(reference(UnlockConditionType.QUEST_COMPLETED, 0, "gunluk-avlanma"), CONTEXT));
    }

    @Test
    void collectionCompletedConditionChecksMembership() {
        assertTrue(UnlockConditions.isSatisfied(reference(UnlockConditionType.COLLECTION_COMPLETED, 0, "degerli-madenler"), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(reference(UnlockConditionType.COLLECTION_COMPLETED, 0, "deniz-hazineleri"), CONTEXT));
    }

    @Test
    void skillLevelConditionComparesConfiguredSkillLevel() {
        assertTrue(UnlockConditions.isSatisfied(reference(UnlockConditionType.SKILL_LEVEL, 50, "madencilik"), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(reference(UnlockConditionType.SKILL_LEVEL, 51, "madencilik"), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(reference(UnlockConditionType.SKILL_LEVEL, 1, "avcilik"), CONTEXT));
    }

    /** Saygınlık thresholds run past {@code int}, which is why the condition carries its own {@code long}. */
    @Test
    void reputationConditionComparesReputationTotal() {
        assertTrue(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.REPUTATION, 0, "", 1_000_000L), CONTEXT));
        assertTrue(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.REPUTATION, 0, "", 250_000L), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.REPUTATION, 0, "", 5_000_000_000L), CONTEXT));
    }
}
