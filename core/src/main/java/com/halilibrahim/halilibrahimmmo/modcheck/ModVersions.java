package com.halilibrahim.halilibrahimmmo.modcheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure, dependency-free comparison of the version strings Fabric mods actually
 * ship, in the same spirit as {@code PrestigeRules}/{@code QuestResetRules}:
 * no Bukkit types, no configuration, fully unit-testable in isolation.
 *
 * <p>Real examples this has to survive, taken from the bundled modpack:
 * {@code 0.154.2+26.2}, {@code 1.13.12+kotlin.2.4.0}, {@code 3.2.0-b},
 * {@code 3.1.0-beta.1+26.2}, {@code 3.9.5+26.2-fabric}, {@code 26.2.8+fabric}.</p>
 *
 * <p><strong>Why not a strict semver parser:</strong> almost none of those are
 * valid semver. What matters is not semantic correctness but <em>consistency
 * within one mod's own scheme</em> - the only comparison ever performed is
 * "this mod's reported version" against "this mod's configured minimum", so an
 * ordering that is monotonic within a scheme is sufficient. The one case this
 * cannot survive is a mod changing its versioning scheme outright; the fix
 * there is regenerating {@code modlar.yml} from the pack, not a smarter
 * comparator.</p>
 */
public final class ModVersions {

    private ModVersions() {
    }

    /**
     * @return {@code true} when {@code reported} is the same as or newer than
     *         {@code minimum}. Minimum versions are lower bounds, never exact
     *         matches: a player running a newer build always passes.
     */
    public static boolean isAtLeast(String reported, String minimum) {
        return compare(reported, minimum) >= 0;
    }

    /**
     * @throws IllegalArgumentException if either side is null, blank, or
     *         contains no comparable part at all. Callers treat this as a
     *         failed check rather than swallowing it, so an unparseable version
     *         can never silently pass verification.
     */
    public static int compare(String left, String right) {
        List<String> leftParts = split(left);
        List<String> rightParts = split(right);
        int length = Math.max(leftParts.size(), rightParts.size());

        for (int index = 0; index < length; index++) {
            if (index >= leftParts.size()) {
                // "3.1.0" vs "3.1.0.1" -> the extra numeric part makes the longer one newer.
                // "3.1.0" vs "3.1.0-beta" -> the extra non-numeric part is a pre-release, so older.
                return isNumeric(rightParts.get(index)) ? -1 : 1;
            }
            if (index >= rightParts.size()) {
                return isNumeric(leftParts.get(index)) ? 1 : -1;
            }
            int result = comparePart(leftParts.get(index), rightParts.get(index));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private static int comparePart(String left, String right) {
        boolean leftNumeric = isNumeric(left);
        boolean rightNumeric = isNumeric(right);
        if (leftNumeric && rightNumeric) {
            return compareNumeric(left, right);
        }
        if (leftNumeric) {
            return 1;
        }
        if (rightNumeric) {
            return -1;
        }
        return left.compareTo(right);
    }

    /**
     * Compares two digit-only strings without parsing them into a numeric type,
     * so a pathologically long version segment can never overflow.
     */
    private static int compareNumeric(String left, String right) {
        String leftDigits = stripLeadingZeros(left);
        String rightDigits = stripLeadingZeros(right);
        if (leftDigits.length() != rightDigits.length()) {
            return Integer.compare(leftDigits.length(), rightDigits.length());
        }
        return leftDigits.compareTo(rightDigits);
    }

    private static String stripLeadingZeros(String digits) {
        int index = 0;
        while (index < digits.length() - 1 && digits.charAt(index) == '0') {
            index++;
        }
        return digits.substring(index);
    }

    private static boolean isNumeric(String part) {
        for (int index = 0; index < part.length(); index++) {
            if (!Character.isDigit(part.charAt(index))) {
                return false;
            }
        }
        return !part.isEmpty();
    }

    /**
     * Drops build metadata (everything from the first {@code +}) and splits the
     * remainder on {@code .} and {@code -}. Lowercasing uses {@link Locale#ROOT}
     * deliberately: this plugin runs on Turkish servers, where the default
     * locale would turn {@code I} into {@code ı} and break comparisons.
     */
    private static List<String> split(String version) {
        if (version == null) {
            throw new IllegalArgumentException("Version must not be null.");
        }
        String trimmed = version.trim();
        int metadataIndex = trimmed.indexOf('+');
        String core = metadataIndex >= 0 ? trimmed.substring(0, metadataIndex).trim() : trimmed;

        List<String> parts = new ArrayList<>();
        for (String part : core.split("[.\\-]")) {
            if (!part.isEmpty()) {
                parts.add(part.toLowerCase(Locale.ROOT));
            }
        }
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Version has no comparable part: '" + version + "'");
        }
        return parts;
    }
}
