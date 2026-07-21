package com.halilibrahim.halilibrahimmmo.titles;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * A bounded set of chat-tag colors/weights a title can be configured with
 * ({@code titles.yml}'s {@code sohbet-stili} key) - the same "YAML names a
 * bounded enum constant, Java owns the real value" pattern
 * {@code kaynak.tur}/{@code etki.tur}/{@code arac.tur} already use elsewhere
 * in this codebase, so a title's tag color is genuinely per-title configured
 * data rather than Java-authored text.
 */
public enum TitleChatStyle {
    ALTIN(NamedTextColor.GOLD, true),
    MOR_KALIN(NamedTextColor.DARK_PURPLE, true),
    KIRMIZI_KALIN(NamedTextColor.RED, true),
    YESIL_KALIN(NamedTextColor.DARK_GREEN, true),
    MAVI(NamedTextColor.AQUA, false),
    BEYAZ_KALIN(NamedTextColor.WHITE, true);

    private final TextColor color;
    private final boolean bold;

    TitleChatStyle(TextColor color, boolean bold) {
        this.color = color;
        this.bold = bold;
    }

    public TextColor color() {
        return color;
    }

    public boolean bold() {
        return bold;
    }
}
