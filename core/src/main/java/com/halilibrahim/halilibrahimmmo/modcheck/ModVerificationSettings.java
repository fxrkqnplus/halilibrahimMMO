package com.halilibrahim.halilibrahimmmo.modcheck;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable parsed {@code modlar.yml} snapshot, published atomically on every
 * (re)load exactly like {@code CoreConfig} - readers on the plugin-message
 * thread never observe a half-updated list.
 */
public record ModVerificationSettings(
        boolean enabled,
        long timeoutSeconds,
        boolean brandCheckEnabled,
        long brandDelayTicks,
        int maximumModCount,
        String bypassPermission,
        List<RequiredMod> requiredMods,
        List<ForbiddenMod> forbiddenMods
) {

    public ModVerificationSettings {
        if (timeoutSeconds < 1) {
            throw new IllegalArgumentException("Verification timeout must be at least one second.");
        }
        if (brandDelayTicks < 0) {
            throw new IllegalArgumentException("Brand check delay must not be negative.");
        }
        if (maximumModCount < 1) {
            throw new IllegalArgumentException("Maximum mod count must be positive.");
        }
        if (bypassPermission == null || bypassPermission.isBlank()) {
            throw new IllegalArgumentException("Bypass permission must not be blank.");
        }
        requiredMods = List.copyOf(requiredMods);
        forbiddenMods = List.copyOf(forbiddenMods);
    }

    /** @return forbidden mods keyed by id, for a single-pass lookup against a reported list. */
    public Map<String, ForbiddenMod> forbiddenById() {
        Map<String, ForbiddenMod> byId = new LinkedHashMap<>();
        for (ForbiddenMod mod : forbiddenMods) {
            byId.put(mod.id(), mod);
        }
        return byId;
    }

    public long timeoutTicks() {
        return timeoutSeconds * 20L;
    }
}
