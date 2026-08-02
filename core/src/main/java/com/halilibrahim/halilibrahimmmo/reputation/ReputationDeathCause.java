package com.halilibrahim.halilibrahimmmo.reputation;

/**
 * Who is answerable for a death, as far as Saygınlık is concerned.
 *
 * <p>Three causes rather than Minecraft's dozen damage types, because the
 * penalty table only ever distinguishes three things: whether somebody killed
 * you, whether something killed you, or whether you managed it on your own. Lava
 * and a hundred-block fall are the same mistake.</p>
 */
public enum ReputationDeathCause {

    /** Fell, drowned, burned, starved - nobody else involved. */
    KENDI,
    /** A hostile creature, or anything a creature caused. */
    YARATIK,
    /** Another player, whether by hand or by arrow. */
    OYUNCU
}
