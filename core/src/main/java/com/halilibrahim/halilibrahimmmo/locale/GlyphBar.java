package com.halilibrahim.halilibrahimmmo.locale;

/**
 * Turns a current/maximum pair into a run of Private Use Area characters that
 * the resource pack's {@code hmmo:pano} font draws as a pixel bar.
 *
 * <p>Pure and dependency-free, alongside {@link NumberFormatter} and
 * {@link DurationFormatter}: no Bukkit, no Adventure, no I/O, integer maths
 * only, so every edge is reachable from a plain unit test.</p>
 *
 * <p><strong>The bar is always additive, never the only carrier.</strong> Every
 * template that shows one keeps its {@code <mevcut>/<maksimum>} numbers next to
 * it. That is a rule rather than a nicety: a client whose pack failed to apply
 * renders these codepoints as empty boxes, and a reading that lives only in the
 * glyphs would simply be gone. The bypass permission holder - the server owner -
 * is exactly the player who can be in that state and not be kicked for it.</p>
 *
 * <p>The codepoints sit in the Private Use Area on purpose. They are wide,
 * multi-cell glyphs, so under the pack's one-cell rule they must live in
 * {@code hmmo:pano} rather than in the default font, and may only appear where
 * nothing measures width - the action bar, never item lore.</p>
 */
public final class GlyphBar {

    /** Left cap, drawn once at the start of the bar. */
    public static final char CAP_LEFT = '\uF212';

    /** Right cap, drawn once at the end. */
    public static final char CAP_RIGHT = '\uF213';

    /** One filled cell. */
    public static final char CELL_FULL = '\uF211';

    /** One empty cell. */
    public static final char CELL_EMPTY = '\uF210';

    /** What every caller uses unless it has a reason not to. */
    public static final int DEFAULT_CELLS = 10;

    private GlyphBar() {
        throw new UnsupportedOperationException("GlyphBar is a static utility class");
    }

    /**
     * @param current how much is filled; values outside {@code [0, maximum]} are
     *                clamped rather than rejected, because a caller reading a
     *                live value should never have to guard a cosmetic bar.
     * @param maximum the full value; a non-positive maximum yields an empty bar
     *                rather than a division by zero.
     * @param cells   how many cells wide; a non-positive count yields {@code ""}
     *                so a misconfigured bar disappears instead of misdrawing.
     */
    public static String of(int current, int maximum, int cells) {
        if (cells <= 0) {
            return "";
        }
        int filled = filledCells(current, maximum, cells);

        StringBuilder bar = new StringBuilder(cells + 2);
        bar.append(CAP_LEFT);
        bar.append(String.valueOf(CELL_FULL).repeat(filled));
        bar.append(String.valueOf(CELL_EMPTY).repeat(cells - filled));
        bar.append(CAP_RIGHT);
        return bar.toString();
    }

    public static String of(int current, int maximum) {
        return of(current, maximum, DEFAULT_CELLS);
    }

    /**
     * Rounds down, with one deliberate exception: any non-zero progress lights
     * at least one cell. A bar that reads completely empty while the player is
     * visibly regenerating looks broken, and the numbers beside it would then
     * contradict the picture.
     */
    static int filledCells(int current, int maximum, int cells) {
        if (maximum <= 0 || current <= 0) {
            return 0;
        }
        if (current >= maximum) {
            return cells;
        }
        int filled = (int) ((long) current * cells / maximum);
        return Math.max(1, filled);
    }
}
