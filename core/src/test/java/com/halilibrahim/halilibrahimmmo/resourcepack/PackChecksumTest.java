package com.halilibrahim.halilibrahimmmo.resourcepack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every branch of the digest parser, in the {@code ModVersionsTest} mould.
 *
 * <p>Worth being exhaustive about: this parser decides whether a server owner's
 * copy-pasted digest is accepted, and a digest that parses but is wrong makes
 * every client reject the pack.</p>
 */
final class PackChecksumTest {

    private static final String VALID = "3f9c1a2b4d5e6f708192a3b4c5d6e7f809a1b2c3";

    @Test
    void parsesFortyHexCharactersIntoTwentyBytes() {
        byte[] digest = PackChecksum.parse(VALID);
        assertEquals(20, digest.length);
        assertEquals((byte) 0x3f, digest[0]);
        assertEquals((byte) 0xc3, digest[19]);
    }

    @Test
    void caseDoesNotMatter() {
        assertArrayEquals(PackChecksum.parse(VALID), PackChecksum.parse(VALID.toUpperCase()));
    }

    /** Both survive a copy-paste out of a terminal, so neither may be a failure. */
    @Test
    void surroundingWhitespaceIsTrimmed() {
        assertArrayEquals(PackChecksum.parse(VALID), PackChecksum.parse("  " + VALID + "\n"));
    }

    @Test
    void roundTripsThroughFormat() {
        assertEquals(VALID, PackChecksum.format(PackChecksum.parse(VALID)));
    }

    @Test
    void formatIsLowerCase() {
        assertEquals(VALID, PackChecksum.format(PackChecksum.parse(VALID.toUpperCase())));
    }

    @Test
    void rejectsWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> PackChecksum.parse(VALID.substring(0, 39)));
        assertThrows(IllegalArgumentException.class, () -> PackChecksum.parse(VALID + "0"));
    }

    @Test
    void rejectsNonHexadecimal() {
        assertThrows(IllegalArgumentException.class,
                () -> PackChecksum.parse("zzzz1a2b4d5e6f708192a3b4c5d6e7f809a1b2c3"));
    }

    /** A {@code 0x} prefix keeps the length wrong, which is the failure we want. */
    @Test
    void rejectsHexPrefix() {
        assertThrows(IllegalArgumentException.class, () -> PackChecksum.parse("0x" + VALID));
    }

    @Test
    void rejectsBlankAndNull() {
        assertThrows(IllegalArgumentException.class, () -> PackChecksum.parse(""));
        assertThrows(IllegalArgumentException.class, () -> PackChecksum.parse("   "));
        assertThrows(IllegalArgumentException.class, () -> PackChecksum.parse(null));
    }

    @Test
    void isValidMirrorsParse() {
        assertTrue(PackChecksum.isValid(VALID));
        assertFalse(PackChecksum.isValid(""));
        assertFalse(PackChecksum.isValid(null));
        assertFalse(PackChecksum.isValid("nope"));
    }

    @Test
    void formatRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> PackChecksum.format(null));
    }

    @Test
    void abbreviateTakesThePrefixAndNeverPads() {
        assertEquals("3f9c1a2b", PackChecksum.abbreviate(VALID, 8));
        assertEquals("", PackChecksum.abbreviate(VALID, 0));
        assertEquals("", PackChecksum.abbreviate(VALID, -1));
        assertEquals("abc", PackChecksum.abbreviate("abc", 8));
        assertEquals("", PackChecksum.abbreviate(null, 8));
    }
}
