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
    /**
     * Either weapon a melee build actually fights with.
     *
     * <p>Every Amansız Savaşçı ability used to demand a sword specifically,
     * which quietly told an axe player their whole class was off limits - an
     * axe is a legitimate melee weapon in this game (slower, harder-hitting),
     * and nothing about a hunting focus or a blood oath is about the blade's
     * shape. Modelled as its own tag rather than as a list of tags on
     * {@link AbilityDefinition} because "what a warrior swings" is one concept,
     * not two, and every one of that class's four abilities wants exactly
     * it.</p>
     */
    MELEE(null) {
        @Override
        public boolean matches(Material material) {
            return Tag.ITEMS_SWORDS.isTagged(material) || Tag.ITEMS_AXES.isTagged(material);
        }
    },
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
