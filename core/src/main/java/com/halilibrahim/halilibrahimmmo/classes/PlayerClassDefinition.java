package com.halilibrahim.halilibrahimmmo.classes;

import java.util.Map;

/**
 * Immutable configured class with its rankable talent branches and the one
 * skill ({@code synergySkillId}) whose XP this class both boosts (see
 * {@code sinerji-xp-bonusu} in {@code classes.yml}) and mirrors into this
 * class's own level track while it is the player's selected class.
 *
 * <p>{@code prestigeEffectType}/{@code prestigeEffectValuePerRank} is a
 * permanent, stacking-per-prestige-rank modifier applied whenever this class
 * is selected (see {@code prestij-etkisi} in {@code classes.yml}) - reuses
 * the same bounded {@link ClassEffectType} talents already use, so prestige
 * boosts recompute through the exact same {@code modifiers(...)} summing
 * path.</p>
 */
public record PlayerClassDefinition(
        String id, String displayName, String synergySkillId, ClassBarColor barColor,
        Map<String, TalentNodeDefinition> talents,
        ClassEffectType prestigeEffectType, double prestigeEffectValuePerRank
) {
}
