package com.halilibrahim.halilibrahimmmo.leaderboards;

import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardCategory;
import com.halilibrahim.halilibrahimmmo.core.PluginService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Read-only, cached top-N ranking over the fixed categories and the configured skill catalogue. */
public interface LeaderboardService extends PluginService {

    LeaderboardSettings settings();

    CompletableFuture<List<LeaderboardEntry>> top(LeaderboardCategory category);

    CompletableFuture<List<LeaderboardEntry>> topSkill(String skillId);
}
