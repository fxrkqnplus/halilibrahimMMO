package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Owns maximum-level prestige validation and profile reset behaviour. */
public interface PrestigeService extends PluginService {
    PrestigeSettings settings();
    CompletableFuture<PrestigeResult> prestige(UUID playerId, String playerName);
}
