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
 * @param physicalRewards the crates of real items a prestige also pays out, one
 *                      of which must be taken. Fully configuration-driven,
 *                      unlike {@code rewardValues} - see
 *                      {@link PrestigePhysicalReward}.
 */
public record PrestigeSettings(
        int maximumPrestige,
        int minimumSkillLevel,
        long reputationReward,
        Map<PrestigeRewardType, Double> rewardValues,
        Map<String, PrestigePhysicalReward> physicalRewards
) {
}
