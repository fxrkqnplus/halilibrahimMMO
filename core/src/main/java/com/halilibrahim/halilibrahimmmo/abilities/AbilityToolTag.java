package com.halilibrahim.halilibrahimmmo.abilities;

import org.bukkit.Material;
import org.bukkit.Tag;

/**
 * The vanilla tool tag a player must hold in their main hand to right-click
 * activate a configured ability - e.g. any pickaxe for a mining ability. Uses
 * Paper's built-in item tags so every wood/stone/iron/golden/diamond/netherite
 * variant is covered without listing materials by hand in {@code abilities.yml}.
 */
public enum AbilityToolTag {
    PICKAXE(Tag.ITEMS_PICKAXES),
    AXE(Tag.ITEMS_AXES),
    SHOVEL(Tag.ITEMS_SHOVELS),
    SWORD(Tag.ITEMS_SWORDS),
    HOE(Tag.ITEMS_HOES),
    /** Tool-agnostic - matches any material, including an empty main hand. */
    ANY(null);

    private final Tag<Material> materials;

    AbilityToolTag(Tag<Material> materials) {
        this.materials = materials;
    }

    public boolean matches(Material material) {
        return materials == null || materials.isTagged(material);
    }
}
