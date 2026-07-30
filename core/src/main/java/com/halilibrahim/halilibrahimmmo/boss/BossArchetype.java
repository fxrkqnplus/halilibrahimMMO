package com.halilibrahim.halilibrahimmmo.boss;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;

/**
 * One kind of creature a world boss can be, plus the pool of names that fit it.
 *
 * <p>Names are per archetype rather than one shared list because a name is
 * supposed to tell you what you are looking at before you are close enough to
 * read the rarity: "Nişancı Edward" is a skeleton, "Baltacı John" is a
 * vindicator, and swapping them would make the naming pure decoration.</p>
 *
 * @param headMaterial what this archetype's severed head looks like - the trophy
 *                     the boss leaves behind, carrying its own name
 * @param speedLevel   the permanent Speed level this archetype carries, or
 *                     {@code 0} for none. Per archetype rather than per rarity
 *                     because it is about how the creature <em>moves</em>: a
 *                     shambling zombie or a witch who never closes the distance
 *                     is a boss the player simply walks away from, while the
 *                     rarity multipliers already say how hard it hits.
 */
public record BossArchetype(
        String id,
        EntityType entityType,
        Material headMaterial,
        BossWeaponType weaponType,
        int speedLevel,
        List<String> names
) {
}
