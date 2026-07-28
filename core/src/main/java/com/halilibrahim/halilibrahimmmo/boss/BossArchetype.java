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
 */
public record BossArchetype(
        String id,
        EntityType entityType,
        Material headMaterial,
        BossWeaponType weaponType,
        List<String> names
) {
}
