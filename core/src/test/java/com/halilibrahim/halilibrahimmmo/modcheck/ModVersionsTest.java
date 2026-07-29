package com.halilibrahim.halilibrahimmmo.modcheck;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every case here uses a version string that actually appears in the bundled
 * modpack, so a regression fails against reality rather than against invented
 * examples.
 */
final class ModVersionsTest {

    @Test
    void identicalVersionsPass() {
        assertTrue(ModVersions.isAtLeast("0.154.2+26.2", "0.154.2+26.2"));
        assertTrue(ModVersions.isAtLeast("26.2.155", "26.2.155"));
    }

    @Test
    void buildMetadataIsIgnored() {
        assertTrue(ModVersions.isAtLeast("0.154.2", "0.154.2+26.2"));
        assertTrue(ModVersions.isAtLeast("1.11.2+mc26.2", "1.11.2"));
        assertTrue(ModVersions.isAtLeast("26.2.8+fabric", "26.2.8+something-else"));
    }

    @Test
    void newerVersionsPass() {
        assertTrue(ModVersions.isAtLeast("0.154.3+26.2", "0.154.2+26.2"));
        assertTrue(ModVersions.isAtLeast("0.155.0+26.2", "0.154.2+26.2"));
        assertTrue(ModVersions.isAtLeast("1.0.0", "0.9.9"));
    }

    @Test
    void olderVersionsFail() {
        assertFalse(ModVersions.isAtLeast("0.154.1+26.2", "0.154.2+26.2"));
        assertFalse(ModVersions.isAtLeast("0.9.0+mc26.2", "0.9.1+mc26.2"));
    }

    /** Collective ships 8.39; a naive string compare would rank 8.40 below it. */
    @Test
    void numericPartsCompareNumericallyNotLexically() {
        assertTrue(ModVersions.isAtLeast("8.40", "8.39"));
        assertTrue(ModVersions.isAtLeast("8.100", "8.99"));
        assertFalse(ModVersions.isAtLeast("8.9", "8.39"));
    }

    /** Distant Horizons ships "3.2.0-b": a pre-release is older than the plain release. */
    @Test
    void preReleaseIsOlderThanRelease() {
        assertTrue(ModVersions.isAtLeast("3.2.0", "3.2.0-b"));
        assertTrue(ModVersions.isAtLeast("3.2.0-c", "3.2.0-b"));
        assertFalse(ModVersions.isAtLeast("3.2.0-a", "3.2.0-b"));
        assertFalse(ModVersions.isAtLeast("3.2.0-b", "3.2.0"));
    }

    /** Placeholder API ships "3.1.0-beta.1+26.2". */
    @Test
    void multiPartPreReleaseCompares() {
        assertTrue(ModVersions.isAtLeast("3.1.0", "3.1.0-beta.1+26.2"));
        assertTrue(ModVersions.isAtLeast("3.1.0-beta.2+26.2", "3.1.0-beta.1+26.2"));
        assertFalse(ModVersions.isAtLeast("3.1.0-alpha.9", "3.1.0-beta.1"));
    }

    @Test
    void extraNumericPartIsNewer() {
        assertTrue(ModVersions.isAtLeast("26.2.0.1", "26.2.0.0"));
        assertTrue(ModVersions.isAtLeast("3.1.0.1", "3.1.0"));
        assertFalse(ModVersions.isAtLeast("3.1.0", "3.1.0.1"));
    }

    @Test
    void leadingZerosDoNotBreakNumericComparison() {
        assertTrue(ModVersions.isAtLeast("1.010", "1.9"));
        assertTrue(ModVersions.isAtLeast("1.007", "1.7"));
    }

    /** Fabric Language Kotlin ships "1.13.12+kotlin.2.4.0" - metadata must not leak into the comparison. */
    @Test
    void kotlinStyleMetadataIsDropped() {
        assertTrue(ModVersions.isAtLeast("1.13.12+kotlin.2.4.0", "1.13.12"));
        assertTrue(ModVersions.isAtLeast("1.13.13+kotlin.1.0.0", "1.13.12+kotlin.9.9.9"));
    }

    @Test
    void separatorsAreEquivalent() {
        assertTrue(ModVersions.isAtLeast("3.9.5", "3.9.5"));
        assertTrue(ModVersions.isAtLeast("26.2-2.6.28", "26.2-2.6.27"));
        assertFalse(ModVersions.isAtLeast("26.2-2.6.26", "26.2-2.6.27"));
    }

    @Test
    void unparseableVersionsThrowRatherThanSilentlyPassing() {
        assertThrows(IllegalArgumentException.class, () -> ModVersions.isAtLeast(null, "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> ModVersions.isAtLeast("", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> ModVersions.isAtLeast("   ", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> ModVersions.isAtLeast("+26.2", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> ModVersions.isAtLeast("1.0.0", "..--.."));
    }
}
