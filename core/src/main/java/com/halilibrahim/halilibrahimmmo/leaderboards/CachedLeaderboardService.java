package com.halilibrahim.halilibrahimmmo.leaderboards;

import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardCategory;
import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Direct-query leaderboard service backed by a per-key time-to-live cache,
 * not the per-player write-through cache other services use - the source
 * tables span every player, not just currently loaded ones, so there is no
 * meaningful "cache" without periodically re-querying the whole table.
 */
public final class CachedLeaderboardService implements LeaderboardService {

    private static final String FILE_NAME = "leaderboards.yml";

    private final JavaPlugin plugin;
    private final LeaderboardRepository repository;
    private final AtomicReference<LeaderboardSettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<LeaderboardCategory, CachedBoard> categoryCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CachedBoard> skillCache = new ConcurrentHashMap<>();

    private record CachedBoard(List<LeaderboardEntry> entries, long fetchedAtMillis) {
    }

    public CachedLeaderboardService(JavaPlugin plugin, LeaderboardRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void disable() {
        categoryCache.clear();
        skillCache.clear();
    }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            int entryLimit = yaml.getInt("giris-sayisi", 10);
            if (entryLimit < 1) {
                throw new IllegalArgumentException("Leaderboard entry limit must be positive.");
            }
            long refreshSeconds = yaml.getLong("yenileme-araligi-saniye", 300);
            if (refreshSeconds < 1) {
                throw new IllegalArgumentException("Leaderboard refresh interval must be positive.");
            }
            settings.set(new LeaderboardSettings(entryLimit, refreshSeconds * 1000L));
            categoryCache.clear();
            skillCache.clear();
        } catch (RuntimeException exception) {
            throw new LifecycleException("leaderboards.yml could not be loaded.", exception);
        }
    }

    @Override
    public LeaderboardSettings settings() {
        LeaderboardSettings value = settings.get();
        if (value == null) {
            throw new IllegalStateException("LeaderboardService.enable() has not run yet.");
        }
        return value;
    }

    @Override
    public CompletableFuture<List<LeaderboardEntry>> top(LeaderboardCategory category) {
        return resolve(categoryCache, category, () -> repository.top(category, settings().entryLimit()));
    }

    @Override
    public CompletableFuture<List<LeaderboardEntry>> topSkill(String skillId) {
        return resolve(skillCache, skillId, () -> repository.topSkill(skillId, settings().entryLimit()));
    }

    @Override
    public CompletableFuture<Integer> powerRank(int prestige, long power) {
        return repository.rankOfPower(prestige, power);
    }

    @Override
    public CompletableFuture<Integer> wealthRank(long wealth) {
        return repository.rankOfWealth(wealth);
    }

    @Override
    public CompletableFuture<Integer> skillRank(String skillId, int level, long experience) {
        return repository.rankOfSkill(skillId, level, experience);
    }

    private <K> CompletableFuture<List<LeaderboardEntry>> resolve(
            Map<K, CachedBoard> cache, K key, Supplier<CompletableFuture<List<LeaderboardEntry>>> loader
    ) {
        CachedBoard cached = cache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.fetchedAtMillis() < settings().refreshIntervalMillis()) {
            return CompletableFuture.completedFuture(cached.entries());
        }
        return loader.get().thenApply(entries -> {
            cache.put(key, new CachedBoard(entries, now));
            return entries;
        });
    }
}
