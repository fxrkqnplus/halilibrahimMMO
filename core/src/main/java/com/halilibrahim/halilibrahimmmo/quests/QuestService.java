package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.structure.Structure;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Asynchronous persistent permanent/daily/weekly quest progression contract. */
public interface QuestService extends PluginService {

    /**
     * How many quests of one type a player may pin to their sidebar. The panel
     * renders fifteen lines at most, and pinning is meant to add focus to the
     * sections rather than to replace everything else on the screen with a
     * quest log.
     */
    int MAXIMUM_PINS_PER_TYPE = 3;

    QuestSettings settings();

    Collection<QuestDefinition> definitions();

    CompletableFuture<Optional<QuestProgress>> find(UUID playerId, String questId);

    CompletableFuture<Map<String, QuestProgress>> loadAll(UUID playerId);

    /** Synchronous, in-memory only - empty until {@link #loadAll} has warmed the cache for this player. */
    Map<String, QuestProgress> cachedAll(UUID playerId);

    /**
     * Awards progress toward every configured {@link QuestObjectiveType#BLOCK_BREAK}
     * quest matching {@code material} in {@code environment}, expiring and
     * resetting any repeating quest whose period has ended. Several quests may
     * match the same event.
     *
     * @return the quests that transitioned to completed as a result of this call.
     */
    CompletableFuture<List<QuestProgress>> awardBlock(UUID playerId, Material material, World.Environment environment);

    /** Same as {@link #awardBlock} for {@link QuestObjectiveType#MOB_KILL} quests. */
    CompletableFuture<List<QuestProgress>> awardMob(UUID playerId, EntityType entityType, World.Environment environment);

    /** Same as {@link #awardBlock} for {@link QuestObjectiveType#STRUCTURE_DISCOVER} quests. */
    CompletableFuture<List<QuestProgress>> awardStructure(UUID playerId, Structure structure);

    /** Same as {@link #awardBlock} for {@link QuestObjectiveType#ITEM_OBTAIN} quests. */
    CompletableFuture<List<QuestProgress>> awardItem(UUID playerId, Material material);

    /** Same as {@link #awardBlock} for {@link QuestObjectiveType#DEED} quests. */
    CompletableFuture<List<QuestProgress>> awardDeed(UUID playerId, QuestDeed deed, World.Environment environment);

    /**
     * Whether any configured quest is waiting on {@code material} at all.
     * Synchronous and catalogue-only, so the item listener can reject the
     * overwhelming majority of pickups without touching a future.
     */
    boolean tracksItem(Material material);

    /** Warms this player's sidebar pins on join; the sidebar reads them synchronously. */
    CompletableFuture<Void> loadPins(UUID playerId);

    /** Synchronous, in-memory only: the quest ids this player pinned to one sidebar section. */
    Set<String> cachedPins(UUID playerId, QuestType type);

    /** Pins an unpinned quest or unpins a pinned one, within {@link #MAXIMUM_PINS_PER_TYPE} per type. */
    CompletableFuture<QuestPinResult> togglePin(UUID playerId, String questId);

    void evict(UUID playerId);
}
