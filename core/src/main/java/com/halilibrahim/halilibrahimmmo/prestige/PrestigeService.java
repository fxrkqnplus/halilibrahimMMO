package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Owns prestige eligibility validation and profile/skill/class reset behaviour. */
public interface PrestigeService extends PluginService {
    PrestigeSettings settings();
    CompletableFuture<PrestigeResult> prestige(UUID playerId, String playerName);

    /** Read-only eligibility checklist for status displays (e.g. {@code /prestij}, {@code PrestigeMenu}) - does not mutate anything. */
    CompletableFuture<PrestigeEligibilityReport> eligibility(UUID playerId, String playerName);
}
