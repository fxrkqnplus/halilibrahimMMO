package com.halilibrahim.halilibrahimmmo.resourcepack;

import java.net.URI;
import java.util.Locale;

/**
 * Immutable parsed {@code kaynak-paketi.yml} snapshot, published atomically on
 * every (re)load exactly like {@code ModVerificationSettings}.
 *
 * <p><strong>The validation here never throws for a merely unusable pack.</strong>
 * Elsewhere in the plugin a malformed config is a {@link
 * com.halilibrahim.halilibrahimmmo.core.LifecycleException}; here it must not
 * be, and the asymmetry is deliberate. Every other config failure degrades one
 * feature. This one decides whether players are allowed through the door, so a
 * blank address or a mistyped digest resolves to {@link #disabled()} - a line
 * in the log - rather than to a server nobody can enter.</p>
 *
 * @param enabled          master switch; ships {@code false} because the
 *                         address is server-specific and ship-and-copy (§5.3)
 *                         means the bundled file is what a fresh server keeps
 * @param url              absolute {@code http}/{@code https} address of the zip
 * @param sha1             40-character digest the client checks the bytes against
 * @param required         when true the client's "decline" button is disabled
 *                         and a refusal is a kick; when false the pack is still
 *                         offered but a refusal is forgiven
 * @param timeoutSeconds   how long the download may make <em>no progress</em>
 *                         before the watchdog gives up; the clock is reset by
 *                         every progress report, so a slow line is not punished
 * @param delayTicks       ticks after join before the pack is offered
 * @param bypassPermission holders are never kicked over the pack
 * @param guiBackgrounds   draw custom menu backgrounds into GUI titles
 * @param actionBarBars    draw pixel energy/cooldown bars in the action bar
 */
public record ResourcePackSettings(
        boolean enabled,
        String url,
        String sha1,
        boolean required,
        long timeoutSeconds,
        long delayTicks,
        String bypassPermission,
        boolean guiBackgrounds,
        boolean actionBarBars
) {

    /** Default bypass permission; mirrors {@code hmmo.mod.bypass}, including its {@code default: op}. */
    public static final String DEFAULT_BYPASS_PERMISSION = "hmmo.paket.bypass";

    public ResourcePackSettings {
        if (timeoutSeconds < 1) {
            throw new IllegalArgumentException("Resource pack timeout must be at least one second.");
        }
        if (delayTicks < 0) {
            throw new IllegalArgumentException("Resource pack delay must not be negative.");
        }
        if (bypassPermission == null || bypassPermission.isBlank()) {
            throw new IllegalArgumentException("Bypass permission must not be blank.");
        }
        url = url == null ? "" : url.trim();
        sha1 = sha1 == null ? "" : sha1.trim().toLowerCase(Locale.ROOT);
    }

    /** @return a settings object that sends nothing and kicks nobody. */
    public static ResourcePackSettings disabled() {
        return new ResourcePackSettings(
                false, "", "", false, 60L, 20L, DEFAULT_BYPASS_PERMISSION, false, false);
    }

    /**
     * @return why this configuration cannot be used, or {@code null} when it is
     *         usable. Returning the reason rather than a boolean is what lets
     *         the loader put something actionable in the console instead of
     *         "resource pack disabled".
     */
    public String unusableReason() {
        if (!enabled) {
            return null;
        }
        if (url.isBlank()) {
            return "'adres' bos";
        }
        if (!isAbsoluteHttpUrl(url)) {
            return "'adres' gecerli bir http/https adresi degil: " + url;
        }
        if (sha1.isBlank()) {
            return "'sha1' bos";
        }
        if (!PackChecksum.isValid(sha1)) {
            return "'sha1' 40 haneli onaltilik bir deger degil";
        }
        return null;
    }

    /** @return true when the pack may actually be offered to a joining player. */
    public boolean usable() {
        return unusableReason() == null && enabled;
    }

    /** @return the host part of the address, for an admin screen that must not print the whole URL. */
    public String host() {
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host;
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    public long timeoutMillis() {
        return timeoutSeconds * 1000L;
    }

    private static boolean isAbsoluteHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return uri.isAbsolute()
                    && uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
