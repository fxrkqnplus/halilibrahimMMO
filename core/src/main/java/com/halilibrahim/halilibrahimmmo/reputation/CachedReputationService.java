package com.halilibrahim.halilibrahimmmo.reputation;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * YAML-backed catalogue plus a write-through, join-warmed per-player cache -
 * the same shape every other persistent service in this codebase uses, with the
 * per-player serial update chain that keeps two awards in the same tick from
 * overwriting each other.
 */
public final class CachedReputationService implements ReputationService {

    private static final String FILE_NAME = "sayginlik.yml";

    private final JavaPlugin plugin;
    private final ReputationRepository repository;
    private final AtomicReference<ReputationSettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<UUID, Long> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Optional<ReputationAward>>> updateChains = new ConcurrentHashMap<>();

    public CachedReputationService(JavaPlugin plugin, ReputationRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void disable() {
        cache.clear();
        updateChains.clear();
    }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        try {
            settings.set(parse(YamlConfiguration.loadConfiguration(file)));
        } catch (RuntimeException exception) {
            throw new LifecycleException("sayginlik.yml could not be loaded.", exception);
        }
    }

    @Override
    public ReputationSettings settings() {
        ReputationSettings value = settings.get();
        if (value == null) {
            throw new IllegalStateException("ReputationService.enable() has not run yet.");
        }
        return value;
    }

    @Override
    public long cached(UUID playerId) {
        return cache.getOrDefault(playerId, 0L);
    }

    @Override
    public CompletableFuture<Long> load(UUID playerId) {
        Long cached = cache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.find(playerId).thenApply(total -> {
            cache.put(playerId, total);
            return total;
        });
    }

    @Override
    public CompletableFuture<Optional<ReputationAward>> award(UUID playerId, long amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        synchronized (updateChains) {
            CompletableFuture<Optional<ReputationAward>> previous =
                    updateChains.getOrDefault(playerId, CompletableFuture.completedFuture(Optional.empty()));
            CompletableFuture<Optional<ReputationAward>> next = previous
                    .handle((ignoredValue, ignoredThrowable) -> null)
                    .thenCompose(ignored -> awardNow(playerId, amount));
            updateChains.put(playerId, next);
            next.whenComplete((ignored, throwable) -> updateChains.remove(playerId, next));
            return next;
        }
    }

    /**
     * The mirror image of {@link #award}, down to the chain it rides.
     *
     * <p>Written as its own method rather than as a negative award because the
     * two are genuinely different: an award produces a {@link ReputationAward}
     * carrying a milestone that may have been crossed, and a loss can only ever
     * cross one downward - which is not an event this server announces. What it
     * returns instead is what was actually taken, so the player can be told a
     * number that matches their own total rather than the one the table asked
     * for.</p>
     */
    @Override
    public CompletableFuture<Long> deduct(UUID playerId, long amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(0L);
        }
        synchronized (updateChains) {
            CompletableFuture<Optional<ReputationAward>> previous =
                    updateChains.getOrDefault(playerId, CompletableFuture.completedFuture(Optional.empty()));
            CompletableFuture<Long> taken = new CompletableFuture<>();
            CompletableFuture<Optional<ReputationAward>> next = previous
                    .handle((ignoredValue, ignoredThrowable) -> null)
                    .thenCompose(ignored -> load(playerId).thenCompose(currentTotal -> {
                        long removed = Math.min(currentTotal, amount);
                        long newTotal = currentTotal - removed;
                        return repository.save(playerId, newTotal).thenApply(saved -> {
                            cache.put(playerId, newTotal);
                            taken.complete(removed);
                            return Optional.<ReputationAward>empty();
                        });
                    }));
            updateChains.put(playerId, next);
            next.whenComplete((ignored, throwable) -> {
                updateChains.remove(playerId, next);
                if (throwable != null) {
                    taken.completeExceptionally(throwable);
                }
            });
            return taken;
        }
    }

    /**
     * Rides the same serial chain {@link #award} uses rather than writing
     * straight through: an admin correction landing in the same tick as an
     * organic award must not be silently undone by it, in either order.
     */
    @Override
    public CompletableFuture<Long> adminSetTotal(UUID playerId, long total) {
        long clamped = Math.max(0L, total);
        synchronized (updateChains) {
            CompletableFuture<Optional<ReputationAward>> previous =
                    updateChains.getOrDefault(playerId, CompletableFuture.completedFuture(Optional.empty()));
            CompletableFuture<Optional<ReputationAward>> next = previous
                    .handle((ignoredValue, ignoredThrowable) -> null)
                    .thenCompose(ignored -> repository.save(playerId, clamped).thenApply(saved -> {
                        cache.put(playerId, clamped);
                        return Optional.empty();
                    }));
            updateChains.put(playerId, next);
            next.whenComplete((ignored, throwable) -> updateChains.remove(playerId, next));
            return next.thenApply(ignored -> clamped);
        }
    }

    private CompletableFuture<Optional<ReputationAward>> awardNow(UUID playerId, long amount) {
        return load(playerId).thenCompose(previousTotal -> {
            long newTotal = previousTotal + amount;
            return repository.save(playerId, newTotal).thenApply(ignored -> {
                cache.put(playerId, newTotal);
                return Optional.of(new ReputationAward(
                        amount, newTotal, settings().crossedMilestone(previousTotal, newTotal)));
            });
        });
    }

    @Override
    public CompletableFuture<Optional<ReputationAward>> awardBlock(UUID playerId, Material material) {
        return award(playerId, settings().blockValue(material));
    }

    @Override
    public CompletableFuture<Optional<ReputationAward>> awardEntity(UUID playerId, EntityType entityType) {
        return award(playerId, settings().entityValue(entityType));
    }

    @Override
    public CompletableFuture<Optional<ReputationAward>> awardQuest(UUID playerId, String questId) {
        return award(playerId, settings().questValue(questId));
    }

    @Override
    public void evict(UUID playerId) {
        cache.remove(playerId);
    }

    private ReputationSettings parse(YamlConfiguration yaml) {
        Map<Material, Long> blockValues = new LinkedHashMap<>();
        ConfigurationSection blocks = yaml.getConfigurationSection("bloklar");
        if (blocks != null) {
            for (String rawName : blocks.getKeys(false)) {
                Material material = Material.matchMaterial(rawName);
                if (material == null) {
                    throw new IllegalArgumentException("Invalid block material in sayginlik.yml: " + rawName);
                }
                blockValues.put(material, requirePositive(blocks.getLong(rawName), rawName));
            }
        }
        Map<EntityType, Long> entityValues = new LinkedHashMap<>();
        ConfigurationSection entities = yaml.getConfigurationSection("varliklar");
        if (entities != null) {
            for (String rawName : entities.getKeys(false)) {
                entityValues.put(parseEntityType(rawName), requirePositive(entities.getLong(rawName), rawName));
            }
        }
        Map<String, Long> questValues = new LinkedHashMap<>();
        ConfigurationSection quests = yaml.getConfigurationSection("gorevler");
        if (quests != null) {
            for (String questId : quests.getKeys(false)) {
                questValues.put(questId.toLowerCase(Locale.ROOT), requirePositive(quests.getLong(questId), questId));
            }
        }
        long defaultEntityValue = yaml.getLong("varsayilan-varlik", 0);
        long defaultQuestValue = yaml.getLong("varsayilan-gorev", 0);
        if (defaultEntityValue < 0 || defaultQuestValue < 0) {
            throw new IllegalArgumentException("Default reputation values cannot be negative.");
        }
        List<Long> milestones = new ArrayList<>(yaml.getLongList("kilometre-taslari"));
        if (milestones.isEmpty()) {
            throw new IllegalArgumentException("sayginlik.yml must define at least one kilometre-taslari entry.");
        }
        for (long milestone : milestones) {
            requirePositive(milestone, "kilometre-taslari");
        }
        Collections.sort(milestones);
        long rankAnnouncementMinutes = yaml.getLong("siralama-duyuru-araligi-dakika", 25);
        if (rankAnnouncementMinutes < 1) {
            throw new IllegalArgumentException("siralama-duyuru-araligi-dakika must be positive.");
        }
        long trendMinimum = yaml.getLong("egilim-duyuru-en-az-dakika", 35);
        long trendMaximum = yaml.getLong("egilim-duyuru-en-fazla-dakika", 40);
        if (trendMinimum < 1 || trendMaximum < trendMinimum) {
            throw new IllegalArgumentException("egilim-duyuru minute band is invalid.");
        }
        return new ReputationSettings(Map.copyOf(blockValues), Map.copyOf(entityValues), defaultEntityValue,
                Map.copyOf(questValues), defaultQuestValue, List.copyOf(milestones),
                rankAnnouncementMinutes * 60_000L, trendMinimum, trendMaximum);
    }

    private long requirePositive(long value, String context) {
        if (value <= 0) {
            throw new IllegalArgumentException("Reputation value must be positive: " + context);
        }
        return value;
    }

    private EntityType parseEntityType(String raw) {
        try {
            return EntityType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid entity type in sayginlik.yml: " + raw, exception);
        }
    }
}
