package com.halilibrahim.halilibrahimmmo.unlock;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UnlockConditionsTest {

    private static final UnlockContext CONTEXT = new UnlockContext(
            25, 1, Set.of("odun-ustasi"), Set.of("degerli-madenler"), Map.of("madencilik", 50)
    );

    @Test
    void levelConditionComparesGlobalLevel() {
        assertTrue(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.LEVEL, 25, ""), CONTEXT));
        assertTrue(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.LEVEL, 10, ""), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.LEVEL, 26, ""), CONTEXT));
    }

    @Test
    void prestigeConditionComparesPrestige() {
        assertTrue(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.PRESTIGE, 1, ""), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.PRESTIGE, 2, ""), CONTEXT));
    }

    @Test
    void questCompletedConditionChecksMembership() {
        assertTrue(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.QUEST_COMPLETED, 0, "odun-ustasi"), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.QUEST_COMPLETED, 0, "gunluk-avlanma"), CONTEXT));
    }

    @Test
    void collectionCompletedConditionChecksMembership() {
        assertTrue(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.COLLECTION_COMPLETED, 0, "degerli-madenler"), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.COLLECTION_COMPLETED, 0, "deniz-hazineleri"), CONTEXT));
    }

    @Test
    void skillLevelConditionComparesConfiguredSkillLevel() {
        assertTrue(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.SKILL_LEVEL, 50, "madencilik"), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.SKILL_LEVEL, 51, "madencilik"), CONTEXT));
        assertFalse(UnlockConditions.isSatisfied(new UnlockCondition(UnlockConditionType.SKILL_LEVEL, 1, "avcilik"), CONTEXT));
    }
}
