package com.halilibrahim.halilibrahimmmo.prestige;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * One crate of real items a prestige pays out, straight from
 * {@code prestige.yml}.
 *
 * <p>The permanent reward is a number that follows a player forever; this is the
 * other half - something they can hold, spend and lose. Both are chosen and both
 * are mandatory, because a prestige that only made a stat go up is a prestige
 * nobody feels.</p>
 *
 * <p><strong>Fully configuration-driven, unlike {@link PrestigeRewardType}.</strong>
 * A permanent reward has to be a bounded enum because code has to know what to
 * do with it - there is no generic "apply an arbitrary stat". A crate is only a
 * list of items, so a server owner can add, remove or retune one without a
 * single line of Java, and nothing in the plugin has to recognise it.</p>
 *
 * @param contents what the crate hands over, in the order it is given
 */
public record PrestigePhysicalReward(
        String id,
        String displayName,
        Material icon,
        List<Map.Entry<Material, Integer>> contents
) {
}
