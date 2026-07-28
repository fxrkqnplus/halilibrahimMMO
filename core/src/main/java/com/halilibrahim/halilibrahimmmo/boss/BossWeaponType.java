package com.halilibrahim.halilibrahimmmo.boss;

/**
 * What kind of weapon an archetype fights with.
 *
 * <p>The archetype picks the <em>kind</em> and the rarity picks the actual
 * item, which is what lets "a mystic boss carries a far better weapon" be one
 * line of configuration per rarity instead of a weapon list per archetype per
 * rarity. A skeleton always shoots and a vindicator always swings an axe; only
 * how good the bow or axe is changes.</p>
 */
public enum BossWeaponType {
    KILIC,
    BALTA,
    YAY,
    /** Witches throw their own potions and are given nothing to hold. */
    YOK
}
