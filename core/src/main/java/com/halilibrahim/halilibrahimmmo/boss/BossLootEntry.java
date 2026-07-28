package com.halilibrahim.halilibrahimmmo.boss;

import org.bukkit.Material;

/**
 * One line of a rarity's drop table.
 *
 * @param material  what drops
 * @param minimum   fewest of it, inclusive
 * @param maximum   most of it, inclusive
 * @param chance    probability in {@code (0, 1]} that this line drops at all
 */
public record BossLootEntry(Material material, int minimum, int maximum, double chance) {
}
