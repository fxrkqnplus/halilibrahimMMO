package com.halilibrahim.halilibrahimmmo.progression;

import com.halilibrahim.halilibrahimmmo.api.progression.ProgressionCause;
import com.halilibrahim.halilibrahimmmo.core.PluginService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Central authority for global level configuration and XP awards. */
public interface ProgressionService extends PluginService {

    ProgressionSettings settings();

    CompletableFuture<ProgressionAwardResult> award(UUID playerId, String playerName, long amount, ProgressionCause cause);

    CompletableFuture<ProgressionAwardResult> awardConfigured(UUID playerId, String playerName, ProgressionCause cause);
}
