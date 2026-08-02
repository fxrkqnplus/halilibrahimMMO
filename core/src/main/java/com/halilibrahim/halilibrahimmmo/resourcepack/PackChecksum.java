package com.halilibrahim.halilibrahimmmo.resourcepack;

import java.util.Locale;

/**
 * Parses and formats the SHA-1 digest a resource pack is pinned to.
 *
 * <p>Pure, dependency-free and total, in the {@code ModVersions} /
 * {@code ReputationPenalty} mould: no Bukkit, no I/O, no logging, so every
 * branch is reachable from a plain unit test.</p>
 *
 * <p>The parser is deliberately strict. A digest that is one character short,
 * or that carries a stray {@code 0x} prefix, is not "nearly right" - the client
 * compares the downloaded bytes against it and rejects the pack outright on any
 * mismatch. A lenient parser here would turn a typo in {@code kaynak-paketi.yml}
 * into every player being unable to join, with nothing in the log to say why.</p>
 */
public final class PackChecksum {

    /** A SHA-1 digest is 20 bytes, so exactly 40 hexadecimal characters. */
    public static final int HEX_LENGTH = 40;

    private static final int BYTE_LENGTH = HEX_LENGTH / 2;

    private PackChecksum() {
    }

    /**
     * @param hex a 40-character hexadecimal digest; case is irrelevant and
     *            surrounding whitespace is trimmed, because both survive a
     *            copy-paste out of a terminal.
     * @return the 20 raw bytes, freshly allocated on every call.
     * @throws IllegalArgumentException if the input is not exactly 40
     *                                  hexadecimal characters.
     */
    public static byte[] parse(String hex) {
        String trimmed = hex == null ? "" : hex.trim();
        if (trimmed.length() != HEX_LENGTH) {
            throw new IllegalArgumentException(
                    "SHA-1 digest must be exactly " + HEX_LENGTH + " hex characters, got " + trimmed.length() + ".");
        }

        byte[] digest = new byte[BYTE_LENGTH];
        for (int i = 0; i < BYTE_LENGTH; i++) {
            int high = digitValue(trimmed.charAt(i * 2));
            int low = digitValue(trimmed.charAt(i * 2 + 1));
            digest[i] = (byte) ((high << 4) | low);
        }
        return digest;
    }

    /**
     * @return {@code true} when {@link #parse(String)} would succeed. Lets the
     *         config loader report a bad digest and fall back to "disabled"
     *         instead of throwing, which is the difference between a warning in
     *         the log and an empty server.
     */
    public static boolean isValid(String hex) {
        try {
            parse(hex);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /** @return the lower-case hexadecimal form of a digest, for logs and screens. */
    public static String format(byte[] digest) {
        if (digest == null) {
            throw new IllegalArgumentException("Digest must not be null.");
        }
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            hex.append(Character.forDigit((value >> 4) & 0xF, 16));
            hex.append(Character.forDigit(value & 0xF, 16));
        }
        return hex.toString();
    }

    /**
     * @return the first {@code count} characters of a digest, for an admin
     *         screen that must identify a pack without printing all forty.
     *         Shorter inputs are returned whole rather than padded.
     */
    public static String abbreviate(String hex, int count) {
        String trimmed = hex == null ? "" : hex.trim().toLowerCase(Locale.ROOT);
        if (count <= 0) {
            return "";
        }
        return trimmed.length() <= count ? trimmed : trimmed.substring(0, count);
    }

    private static int digitValue(char character) {
        int value = Character.digit(character, 16);
        if (value < 0) {
            throw new IllegalArgumentException("SHA-1 digest contains a non-hexadecimal character: '" + character + "'.");
        }
        return value;
    }
}
