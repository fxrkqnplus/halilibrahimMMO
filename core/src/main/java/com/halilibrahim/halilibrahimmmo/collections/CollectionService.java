package com.halilibrahim.halilibrahimmmo.collections;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.Material;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Asynchronous persistent collection progression contract. */
public interface CollectionService extends PluginService {

    CollectionSettings settings();

    Collection<CollectionDefinition> definitions();

    CompletableFuture<Optional<CollectionProgress>> find(UUID playerId, String collectionId);

    CompletableFuture<Map<String, CollectionProgress>> loadAll(UUID playerId);

    /**
     * Awards every configured collection entry matching {@code material} that the
     * player has not already obtained. Several collections may share the same item.
     *
     * @return the entries newly obtained as a result of this call, each flagging
     * whether it also completed its collection.
     */
    CompletableFuture<List<CollectionEntryAward>> awardPickup(UUID playerId, Material material);

    void evict(UUID playerId);
}
