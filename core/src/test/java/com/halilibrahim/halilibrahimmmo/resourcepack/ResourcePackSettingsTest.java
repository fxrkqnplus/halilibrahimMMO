package com.halilibrahim.halilibrahimmmo.resourcepack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Record validation, and - the part that actually matters - the rule that an
 * enabled-but-unusable configuration reports a reason instead of throwing.
 *
 * <p>Every other config in this plugin raises {@code LifecycleException} on bad
 * input. This one must not: its failure mode is "nobody can join", so a typo
 * has to degrade to a log line. These tests are what stop a later refactor from
 * quietly restoring the throw.</p>
 */
final class ResourcePackSettingsTest {

    private static final String VALID_SHA1 = "3f9c1a2b4d5e6f708192a3b4c5d6e7f809a1b2c3";
    private static final String VALID_URL = "https://kaynak.fxrkqn.org/hmmo-paket-1.0.0.zip";

    private static ResourcePackSettings enabled(String url, String sha1) {
        return new ResourcePackSettings(true, url, sha1, true, 60L, 20L,
                ResourcePackSettings.DEFAULT_BYPASS_PERMISSION, false, true);
    }

    @Test
    void aFullyConfiguredPackIsUsable() {
        ResourcePackSettings settings = enabled(VALID_URL, VALID_SHA1);
        assertNull(settings.unusableReason());
        assertTrue(settings.usable());
        assertEquals("kaynak.fxrkqn.org", settings.host());
    }

    @Test
    void disabledIsNeverUsableAndReportsNoProblem() {
        ResourcePackSettings settings = ResourcePackSettings.disabled();
        assertFalse(settings.usable());
        // Not a complaint: being switched off is a valid state, not a mistake.
        assertNull(settings.unusableReason());
    }

    @Test
    void enabledWithBlankAddressIsUnusableButDoesNotThrow() {
        ResourcePackSettings settings = enabled("", VALID_SHA1);
        assertNotNull(settings.unusableReason());
        assertFalse(settings.usable());
    }

    @Test
    void enabledWithBlankDigestIsUnusable() {
        assertNotNull(enabled(VALID_URL, "").unusableReason());
    }

    @Test
    void enabledWithMalformedDigestIsUnusable() {
        assertNotNull(enabled(VALID_URL, "not-a-digest").unusableReason());
        assertNotNull(enabled(VALID_URL, VALID_SHA1.substring(0, 39)).unusableReason());
    }

    @Test
    void nonHttpSchemesAreRejected() {
        assertNotNull(enabled("ftp://kaynak.fxrkqn.org/p.zip", VALID_SHA1).unusableReason());
        assertNotNull(enabled("file:///C:/p.zip", VALID_SHA1).unusableReason());
        assertNotNull(enabled("kaynak.fxrkqn.org/p.zip", VALID_SHA1).unusableReason());
    }

    /** Plain http is how the local test loop works, so it must stay legal. */
    @Test
    void plainHttpIsAccepted() {
        assertNull(enabled("http://127.0.0.1:8080/hmmo-paket-1.0.0.zip", VALID_SHA1).unusableReason());
    }

    @Test
    void digestIsNormalisedToLowerCase() {
        assertEquals(VALID_SHA1, enabled(VALID_URL, VALID_SHA1.toUpperCase()).sha1());
    }

    @Test
    void addressAndDigestAreTrimmed() {
        ResourcePackSettings settings = enabled("  " + VALID_URL + " ", " " + VALID_SHA1 + "\n");
        assertEquals(VALID_URL, settings.url());
        assertEquals(VALID_SHA1, settings.sha1());
        assertNull(settings.unusableReason());
    }

    @Test
    void nullAddressAndDigestBecomeEmptyRatherThanExploding() {
        ResourcePackSettings settings = enabled(null, null);
        assertEquals("", settings.url());
        assertEquals("", settings.sha1());
        assertNotNull(settings.unusableReason());
    }

    @Test
    void hostIsEmptyRatherThanNullForAnUnparseableAddress() {
        assertEquals("", enabled("not a url", VALID_SHA1).host());
    }

    @Test
    void timeoutConvertsToMilliseconds() {
        assertEquals(60_000L, enabled(VALID_URL, VALID_SHA1).timeoutMillis());
    }

    /** These three are programming errors, not owner typos, so they still throw. */
    @Test
    void structurallyImpossibleValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ResourcePackSettings(
                true, VALID_URL, VALID_SHA1, true, 0L, 20L, "hmmo.paket.bypass", false, true));
        assertThrows(IllegalArgumentException.class, () -> new ResourcePackSettings(
                true, VALID_URL, VALID_SHA1, true, 60L, -1L, "hmmo.paket.bypass", false, true));
        assertThrows(IllegalArgumentException.class, () -> new ResourcePackSettings(
                true, VALID_URL, VALID_SHA1, true, 60L, 20L, "  ", false, true));
    }
}
