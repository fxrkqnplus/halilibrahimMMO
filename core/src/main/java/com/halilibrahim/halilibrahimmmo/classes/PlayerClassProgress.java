package com.halilibrahim.halilibrahimmmo.classes;

import java.util.Map;
import java.util.UUID;

/**
 * Persistent player talent allocation and per-class level track, kept
 * independently from the currently selected class - switching classes never
 * discards either a class's spent talent ranks or its accumulated level/XP.
 */
public record PlayerClassProgress(
        UUID playerId,
        Map<String, Map<String, Integer>> talentRanks,
        long classChangeReadyAtMillis,
        Map<String, ClassLevelProgress> classLevels
) {
    public int spentPoints() {
        return talentRanks.values().stream().flatMap(nodes -> nodes.values().stream()).mapToInt(Integer::intValue).sum();
    }

    /**
     * Ranks spent out of one specific point pool, across every class - the two
     * pools are budgeted and consumed independently. {@code definitions} is
     * needed because only the catalogue knows a talent's category; a rank whose
     * talent no longer exists in configuration counts against nothing, so
     * removing a talent from {@code classes.yml} silently refunds it rather
     * than leaving the player permanently short.
     */
    public int spentPoints(Map<String, PlayerClassDefinition> definitions, TalentPointCategory category) {
        int spent = 0;
        for (Map.Entry<String, Map<String, Integer>> byClass : talentRanks.entrySet()) {
            PlayerClassDefinition definition = definitions.get(byClass.getKey());
            if (definition == null) {
                continue;
            }
            for (Map.Entry<String, Integer> rank : byClass.getValue().entrySet()) {
                TalentNodeDefinition talent = definition.talents().get(rank.getKey());
                if (talent != null && talent.effectType().category() == category) {
                    spent += rank.getValue();
                }
            }
        }
        return spent;
    }

    public ClassLevelProgress levelProgress(String classId) {
        return classLevels.getOrDefault(classId, new ClassLevelProgress(classId, 1, 0L));
    }
}
