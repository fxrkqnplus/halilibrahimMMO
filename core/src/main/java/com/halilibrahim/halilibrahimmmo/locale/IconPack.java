package com.halilibrahim.halilibrahimmmo.locale;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The server's own icon set: one named, bounded glyph per gameplay concept,
 * written into {@code messages.yml} as a {@code <simge:...>} MiniMessage tag
 * (resolved by {@link MiniMessageService}) rather than pasted as a raw
 * character. Naming them centrally is what makes this a *pack*: swapping the
 * glyph a concept uses is a one-line change here and every sidebar line, GUI
 * item, bossbar and chat message that references it follows automatically.
 *
 * <p>Every glyph is deliberately restricted to the Unicode Basic Multilingual
 * Plane, which Minecraft's bundled Unifont fallback covers - astral-plane
 * emoji render as a missing-glyph box on a vanilla client and must never be
 * added here. The glyph itself carries no colour; the surrounding
 * {@code messages.yml} markup owns that, so the same icon can be gold on the
 * sidebar and gray in a GUI lore line.</p>
 */
public enum IconPack {

    /** Global power (Güç). */
    GUC("guc", "✦"),
    /** Prestige rank. */
    PRESTIJ("prestij", "★"),
    /** Player class. */
    SINIF("sinif", "⚔"),
    /** A level number of any track. */
    SEVIYE("seviye", "❖"),
    /** Experience. */
    XP("xp", "✧"),
    /** An active ability. */
    KABILIYET("kabiliyet", "✸"),
    /** Energy. */
    ENERJI("enerji", "⚡"),
    /** Health. */
    CAN("can", "❤"),
    /** Quests. */
    GOREV("gorev", "⚑"),
    /** Collections. */
    KOLEKSIYON("koleksiyon", "◆"),
    /** The adventure journal. */
    GUNLUK("gunluk", "✎"),
    /** Leaderboards. */
    LIDERLIK("liderlik", "♛"),
    /** Titles. */
    UNVAN("unvan", "✤"),
    /** Cosmetics. */
    KOZMETIK("kozmetik", "✿"),
    /** The madencilik skill. */
    MADENCILIK("madencilik", "⛏"),
    /** The odunculuk skill. */
    ODUNCULUK("odunculuk", "♣"),
    /** The kazicilik skill. */
    KAZICILIK("kazicilik", "⚒"),
    /** The avcilik skill. */
    AVCILIK("avcilik", "☠"),
    /** The tarimcilik skill. */
    TARIMCILIK("tarimcilik", "❀"),
    /** The hayvancilik skill. */
    HAYVANCILIK("hayvancilik", "❦"),
    /** A satisfied requirement / obtained entry. */
    TAMAM("tamam", "✔"),
    /** An unsatisfied requirement / missing entry. */
    EKSIK("eksik", "✘"),
    /** Navigation pointer. */
    OK("ok", "»"),
    /** Warnings and irreversible actions. */
    UYARI("uyari", "⚠"),
    /** Informational/administrative screens. */
    BILGI("bilgi", "⚙"),
    /** Celebration/fanfare accent. */
    KUTLAMA("kutlama", "✹"),
    /** Zenginlik Puanı. */
    ZENGINLIK("zenginlik", "❈"),
    /** A world boss. */
    BOSS("boss", "☬");

    private static final Map<String, IconPack> BY_ID = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(IconPack::id, Function.identity()));

    private final String id;
    private final String glyph;

    IconPack(String id, String glyph) {
        this.id = id;
        this.glyph = glyph;
    }

    /** The name this icon is written as inside a {@code <simge:...>} tag. */
    public String id() {
        return id;
    }

    public String glyph() {
        return glyph;
    }

    public static Optional<IconPack> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id.toLowerCase(Locale.ROOT)));
    }
}
