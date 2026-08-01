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

    /**
     * Where one player stands, rather than who the leaders are.
     *
     * <p>Deliberately <em>not</em> cached, unlike the boards above: a board is
     * one answer shared by everyone looking at it, while a rank is a different
     * question per player and is only ever asked when somebody opens a profile
     * screen. Each takes the player's own already-in-memory numbers, so the
     * query counts rather than recomputes.</p>
     */
    CompletableFuture<Integer> powerRank(int prestige, long power);

    CompletableFuture<Integer> reputationRank(long reputation);

    CompletableFuture<Integer> skillRank(String skillId, int level, long experience);
}
