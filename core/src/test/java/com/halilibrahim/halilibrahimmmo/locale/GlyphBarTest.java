package com.halilibrahim.halilibrahimmmo.locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every edge of the pixel bar, in the {@code NumberFormatterTest} mould. */
final class GlyphBarTest {

    private static long count(String bar, char cell) {
        return bar.chars().filter(c -> c == cell).count();
    }

    @Test
    void isAlwaysCapsPlusExactlyTheRequestedCells() {
        String bar = GlyphBar.of(3, 10, 10);
        assertEquals(12, bar.length());
        assertEquals(GlyphBar.CAP_LEFT, bar.charAt(0));
        assertEquals(GlyphBar.CAP_RIGHT, bar.charAt(bar.length() - 1));
        assertEquals(10, count(bar, GlyphBar.CELL_FULL) + count(bar, GlyphBar.CELL_EMPTY));
    }

    @Test
    void emptyAndFullAreExact() {
        assertEquals(0, count(GlyphBar.of(0, 100, 10), GlyphBar.CELL_FULL));
        assertEquals(10, count(GlyphBar.of(100, 100, 10), GlyphBar.CELL_FULL));
    }

    @Test
    void halfIsHalf() {
        assertEquals(5, count(GlyphBar.of(50, 100, 10), GlyphBar.CELL_FULL));
    }

    /** Clamped rather than rejected: a live value should never need a guard at the call site. */
    @Test
    void outOfRangeValuesClamp() {
        assertEquals(10, count(GlyphBar.of(250, 100, 10), GlyphBar.CELL_FULL));
        assertEquals(0, count(GlyphBar.of(-40, 100, 10), GlyphBar.CELL_FULL));
    }

    @Test
    void nonPositiveMaximumDrawsAnEmptyBarRatherThanDividingByZero() {
        assertEquals(0, count(GlyphBar.of(5, 0, 10), GlyphBar.CELL_FULL));
        assertEquals(0, count(GlyphBar.of(5, -1, 10), GlyphBar.CELL_FULL));
    }

    @Test
    void nonPositiveCellCountDrawsNothingAtAll() {
        assertEquals("", GlyphBar.of(5, 10, 0));
        assertEquals("", GlyphBar.of(5, 10, -3));
    }

    /**
     * The one deliberate rounding exception: a player who is visibly
     * regenerating must not see a bar that reads completely empty, because the
     * numbers printed beside it would then contradict the picture.
     */
    @Test
    void anyProgressAtAllLightsOneCell() {
        assertEquals(1, count(GlyphBar.of(1, 1000, 10), GlyphBar.CELL_FULL));
        assertEquals(0, count(GlyphBar.of(0, 1000, 10), GlyphBar.CELL_FULL));
    }

    /** Only a genuinely full bar may show every cell lit. */
    @Test
    void justBelowFullIsNotFull() {
        assertTrue(count(GlyphBar.of(99, 100, 10), GlyphBar.CELL_FULL) < 10);
    }

    /**
     * {@code current * cells} exceeds {@code int} range long before either
     * operand does, so the multiplication is widened to {@code long}. Without
     * that it wraps negative and the bar reads empty at exactly the moment it
     * should read full.
     *
     * <p>Four, not five: {@code (MAX_VALUE/2) * 10 / MAX_VALUE} is 4.999…, and
     * the bar floors. Only a genuinely full value lights every cell.</p>
     */
    @Test
    void largeValuesDoNotOverflow() {
        assertEquals(4, count(GlyphBar.of(Integer.MAX_VALUE / 2, Integer.MAX_VALUE, 10), GlyphBar.CELL_FULL));
        assertEquals(10, count(GlyphBar.of(Integer.MAX_VALUE, Integer.MAX_VALUE, 10), GlyphBar.CELL_FULL));
    }

    @Test
    void everyGlyphIsInThePrivateUseArea() {
        for (char c : GlyphBar.of(4, 10, 10).toCharArray()) {
            assertTrue(c >= 0xE000 && c <= 0xF8FF, "Not a Private Use Area codepoint: U+%04X".formatted((int) c));
        }
    }
}
