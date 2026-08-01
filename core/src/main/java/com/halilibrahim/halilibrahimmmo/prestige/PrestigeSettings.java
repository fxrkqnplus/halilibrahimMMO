package com.halilibrahim.halilibrahimmmo.prestige;

import java.util.Map;

/**
 * Immutable parsed {@code prestige.yml} snapshot.
 *
 * @param reputationReward  Saygınlık Puanı granted by each prestige. Prestige never
 *                      resets reputation - the score counts what a player has taken
 *                      out of the world and that does not stop being true - so
 *                      this is a straight addition on top.
 * @param rewardValues  what one pick of each permanent reward is worth
 */
public record PrestigeSettings(
        int maximumPrestige,
        int minimumSkillLevel,
        long reputationReward,
        Map<PrestigeRewardType, Double> rewardValues
) {
}
