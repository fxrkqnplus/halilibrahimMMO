package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Asynchronous persistent permanent/daily/weekly quest progression contract. */
public interface QuestService extends PluginService {

    QuestSettings settings();

    Collection<QuestDefinition> definitions();

    CompletableFuture<Optional<QuestProgress>> find(UUID playerId, String questId);

    CompletableFuture<Map<String, QuestProgress>> loadAll(UUID playerId);

    /**
     * Awards progress toward every configured {@link QuestObjectiveType#BLOCK_BREAK}
     * quest matching {@code material}, expiring and resetting any repeating quest
     * whose period has ended. Several quests may match the same event.
     *
     * @return the quests that transitioned to completed as a result of this call.
     */
    CompletableFuture<List<QuestProgress>> awardBlock(UUID playerId, Material material);

    /** Same as {@link #awardBlock(UUID, Material)} for {@link QuestObjectiveType#MOB_KILL} quests. */
    CompletableFuture<List<QuestProgress>> awardMob(UUID playerId, EntityType entityType);

    void evict(UUID playerId);
}
