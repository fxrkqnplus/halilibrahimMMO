package com.halilibrahim.halilibrahimmmo.skills;

import org.bukkit.Material;

import java.util.Set;

/**
 * One configured {@link SkillAction} reward. Deliberately separate from
 * {@link SkillRewardEntry} rather than reusing it, because an action can also
 * be gated on the item that caused it: {@code materials} is the set of
 * qualifying {@link Material}s (the cooked foods that count as "meat", the
 * crafted item that counts as bread, ...). An empty set means the action is
 * not material-gated at all and the listener's own event is the whole gate.
 */
public record SkillActionReward(long xp, String displayName, Set<Material> materials) {

    public boolean accepts(Material material) {
        return materials.isEmpty() || (material != null && materials.contains(material));
    }
}
