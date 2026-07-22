package com.halilibrahim.halilibrahimmmo.api;

import com.halilibrahim.halilibrahimmmo.api.profile.PlayerProfileSnapshot;
import com.halilibrahim.halilibrahimmmo.api.classes.ClassDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.classes.PlayerClassSnapshot;
import com.halilibrahim.halilibrahimmmo.api.collections.CollectionDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.collections.CollectionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.cosmetics.CosmeticDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.cosmetics.CosmeticSnapshot;
import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardCategory;
import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardEntrySnapshot;
import com.halilibrahim.halilibrahimmmo.api.quests.QuestDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.quests.QuestSnapshot;
import com.halilibrahim.halilibrahimmmo.api.skills.SkillDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.skills.SkillSnapshot;
import com.halilibrahim.halilibrahimmmo.api.titles.TitleDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.titles.TitleSnapshot;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Root facade of the HalilIbrahimMMO developer API.
 *
 * <p>An instance of this interface is registered with Bukkit's {@code ServicesManager}
 * once HalilIbrahimMMO finishes enabling, and can be retrieved through
 * {@link HalilIbrahimMMOProvider#get()}.</p>
 *
 * <p>This interface will grow every phase as new subsystems (player profiles, skills,
 * abilities, quests, collections, ...) are implemented. Phase 1 only exposes plugin
 * metadata and lifecycle control, since no player-facing subsystem exists yet.</p>
 */
public interface HalilIbrahimMMOAPI {

    /**
     * @return the running plugin version, e.g. {@code "1.0.0-SNAPSHOT"}.
     */
    String getPluginVersion();

    /**
     * @return the Minecraft/Paper version this build targets, e.g. {@code "26.2"}.
     */
    String getSupportedMinecraftVersion();

    /**
     * @return {@code true} if verbose debug logging is currently enabled via {@code config.yml}
     * or toggled at runtime through {@code /haliladmin}'s debug toggle.
     */
    boolean isDebugModeEnabled();

    /**
     * Reloads every configuration file ({@code config.yml}, {@code messages.yml}, ...) and
     * re-initializes dependent services from disk. Does not reconnect the database unless
     * the database section actually changed.
     *
     * @return {@code true} if the reload completed without errors, {@code false} otherwise.
     * Failures are always logged to console regardless of the return value.
     */
    boolean reload();

    /**
     * Loads the latest persisted profile for a player without exposing mutable
     * implementation state.
     */
    CompletableFuture<Optional<PlayerProfileSnapshot>> findProfile(UUID playerId);

    /**
     * Returns the current configured skill catalogue. The returned objects are
     * immutable snapshots and cannot alter the running plugin configuration.
     */
    Collection<SkillDefinitionSnapshot> getSkills();

    /**
     * Loads one player's persisted progression for a configured skill.
     *
     * @return an empty result when {@code skillId} is not configured.
     */
    CompletableFuture<Optional<SkillSnapshot>> findSkill(UUID playerId, String skillId);

    /** Returns immutable definitions for every configured player class. */
    Collection<ClassDefinitionSnapshot> getClasses();

    /** Loads the player's selected class and persisted talent-rank allocation. */
    CompletableFuture<Optional<PlayerClassSnapshot>> findClassProgress(UUID playerId);

    /** Returns the current configured quest catalogue (permanent, daily and weekly). */
    Collection<QuestDefinitionSnapshot> getQuests();

    /**
     * Loads one player's persisted progress toward a configured quest.
     *
     * @return an empty result when {@code questId} is not configured.
     */
    CompletableFuture<Optional<QuestSnapshot>> findQuest(UUID playerId, String questId);

    /** Returns the current configured collection catalogue. */
    Collection<CollectionDefinitionSnapshot> getCollections();

    /**
     * Loads one player's persisted progress toward a configured collection.
     *
     * @return an empty result when {@code collectionId} is not configured.
     */
    CompletableFuture<Optional<CollectionSnapshot>> findCollection(UUID playerId, String collectionId);

    /** Loads the current top-ranked players for a fixed leaderboard category. */
    CompletableFuture<List<LeaderboardEntrySnapshot>> getLeaderboard(LeaderboardCategory category);

    /**
     * Loads the current top-ranked players for a configured skill.
     *
     * @return an empty list when {@code skillId} is not configured.
     */
    CompletableFuture<List<LeaderboardEntrySnapshot>> getSkillLeaderboard(String skillId);

    /** Returns the current configured title catalogue. */
    Collection<TitleDefinitionSnapshot> getTitles();

    /** Loads the player's currently equipped title, if any. */
    CompletableFuture<Optional<TitleSnapshot>> findEquippedTitle(UUID playerId);

    /** Returns the current configured cosmetic catalogue. */
    Collection<CosmeticDefinitionSnapshot> getCosmetics();

    /** Loads the player's currently equipped cosmetic, if any. */
    CompletableFuture<Optional<CosmeticSnapshot>> findEquippedCosmetic(UUID playerId);

    /**
     * Synchronous, in-memory-only snapshot of a player's most recently loaded
     * progression state - for callers that cannot await a
     * {@link CompletableFuture} (PlaceholderAPI, LuckPerms context
     * calculators). Empty until the player has been loaded into memory this
     * session (normally moments after they join); does not itself trigger a
     * database read.
     */
    Optional<CachedPlayerSnapshot> findCachedPlayer(UUID playerId);

    /**
     * Synchronous equivalent of {@link #findSkill(UUID, String)} - see
     * {@link #findCachedPlayer(UUID)} for the same in-memory-only caveats.
     */
    Optional<SkillSnapshot> findCachedSkill(UUID playerId, String skillId);
}
