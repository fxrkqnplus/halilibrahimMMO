package com.halilibrahim.halilibrahimmmo.abilities;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;

/**
 * A bounded set of colours an ability can be configured with
 * ({@code abilities.yml}'s {@code gorsel.renk} key) so every ability is
 * visually distinct from every other - the same "YAML names a bounded enum
 * constant, Java owns the real value" pattern {@code classes.ClassBarColor}
 * and {@code titles.TitleChatStyle} already use.
 *
 * <p>Each constant carries the value twice because two unrelated APIs need
 * it: a {@link NamedTextColor} for the ability's name wherever it is rendered
 * as text, and a Bukkit {@link Color} for the particle/glow tint of the
 * effects it spawns in-world.</p>
 */
public enum AbilityColor {

    KIRMIZI(NamedTextColor.RED, Color.fromRGB(0xFF5555)),
    TURUNCU(NamedTextColor.GOLD, Color.fromRGB(0xFFAA00)),
    SARI(NamedTextColor.YELLOW, Color.fromRGB(0xFFFF55)),
    YESIL(NamedTextColor.GREEN, Color.fromRGB(0x55FF55)),
    KOYU_YESIL(NamedTextColor.DARK_GREEN, Color.fromRGB(0x00AA00)),
    TURKUAZ(NamedTextColor.AQUA, Color.fromRGB(0x55FFFF)),
    MAVI(NamedTextColor.BLUE, Color.fromRGB(0x5555FF)),
    MOR(NamedTextColor.LIGHT_PURPLE, Color.fromRGB(0xFF55FF)),
    KOYU_MOR(NamedTextColor.DARK_PURPLE, Color.fromRGB(0xAA00AA)),
    BEYAZ(NamedTextColor.WHITE, Color.fromRGB(0xFFFFFF));

    private final NamedTextColor textColor;
    private final Color effectColor;

    AbilityColor(NamedTextColor textColor, Color effectColor) {
        this.textColor = textColor;
        this.effectColor = effectColor;
    }

    public NamedTextColor textColor() {
        return textColor;
    }

    public Color effectColor() {
        return effectColor;
    }
}
