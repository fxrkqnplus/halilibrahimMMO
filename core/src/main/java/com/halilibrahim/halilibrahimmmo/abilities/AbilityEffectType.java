package com.halilibrahim.halilibrahimmmo.abilities;

/** Built-in, bounded gameplay effects available to YAML ability definitions. */
public enum AbilityEffectType {
    EXCAVATE,
    TREE_FELL,
    VEIN_MINE,
    DAMAGE_BUFF,
    /** Armed for a window: chance (in {@code damageMultiplier}) to drop an extra copy of a normally broken block's drop. */
    EXTRA_DROP_CHANCE,
    /** Instant: near-total damage reduction ({@link org.bukkit.potion.PotionEffectType#RESISTANCE}) for the ability's duration. */
    BRIEF_INVULNERABILITY,
    /** Instant: self-only {@link org.bukkit.potion.PotionEffectType#REGENERATION} for the ability's duration. */
    AREA_HEAL,
    /** Instant: {@link org.bukkit.potion.PotionEffectType#SPEED} for the ability's duration. */
    SPEED_BURST,
    /** Instant: pushes and lightly damages nearby entities within {@code limit} blocks. */
    KNOCKBACK_SLAM,
    /** Instant: {@link org.bukkit.potion.PotionEffectType#NIGHT_VISION} for the ability's duration. */
    NIGHT_VISION
}
