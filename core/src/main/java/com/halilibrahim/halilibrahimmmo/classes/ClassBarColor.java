package com.halilibrahim.halilibrahimmmo.classes;

import net.kyori.adventure.bossbar.BossBar;

/**
 * A bounded set of bossbar colors a class can be configured with
 * ({@code classes.yml}'s {@code bossbar-rengi} key), so each class's XP
 * bossbar (see {@code ClassSynergyBridge}) is visually distinct from every
 * other class's - the same "YAML names a bounded enum constant, Java owns
 * the real value" pattern {@code titles.TitleChatStyle} already uses.
 */
public enum ClassBarColor {
    SARI(BossBar.Color.YELLOW),
    KIRMIZI(BossBar.Color.RED),
    MAVI(BossBar.Color.BLUE),
    YESIL(BossBar.Color.GREEN);

    private final BossBar.Color color;

    ClassBarColor(BossBar.Color color) {
        this.color = color;
    }

    public BossBar.Color color() {
        return color;
    }
}
