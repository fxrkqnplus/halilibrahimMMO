package com.halilibrahim.halilibrahimmmo.classes;

import org.bukkit.Material;

import java.util.Map;
import java.util.Set;

/**
 * Immutable configured class with its rankable talent branches and the set of
 * skills ({@code synergySkillIds}) whose XP this class both boosts (see
 * {@code sinerji-xp-bonusu} in {@code classes.yml}) and mirrors into this
 * class's own level track while it is the player's selected class.
 *
 * <p>A set rather than a single id since Phase 15: Tabiat Vârisi covers four
 * skills at once (odunculuk, kazıcılık, tarımcılık, hayvancılık), while the
 * other two classes still name exactly one. Everything reading it - the
 * synergy multiplier, the class-XP bridge, prestige eligibility and the
 * per-skill detail screen - treats one and four alike.</p>
 *
 * <p>{@code prestigeEffectType}/{@code prestigeEffectValuePerRank} is a
 * permanent, stacking-per-prestige-rank modifier applied whenever this class
 * is selected (see {@code prestij-etkisi} in {@code classes.yml}) - reuses
 * the same bounded {@link ClassEffectType} talents already use, so prestige
 * boosts recompute through the exact same {@code modifiers(...)} summing
 * path.</p>
 *
 * <p>{@code icon} is the {@link Material} every class-picking screen renders
 * this class as ({@code simge} in {@code classes.yml}), so a class is
 * recognisable by its own tool/theme instead of every option sharing one
 * generic sword. {@code frame} is the stained-glass pane
 * ({@code cerceve} in {@code classes.yml}) that borders the screens belonging
 * to the player's own class - their ability list and their talent tree - so
 * those screens are visibly *theirs* the moment they open, in the same colour
 * as the class's XP bar.</p>
 */
public record PlayerClassDefinition(
        String id, String displayName, Set<String> synergySkillIds, ClassBarColor barColor, Material icon,
        Material frame, Map<String, TalentNodeDefinition> talents,
        ClassEffectType prestigeEffectType, double prestigeEffectValuePerRank
) {
    public boolean hasSynergyWith(String skillId) {
        return synergySkillIds.contains(skillId);
    }
}
