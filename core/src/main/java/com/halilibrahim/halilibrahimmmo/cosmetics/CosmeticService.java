package com.halilibrahim.halilibrahimmmo.cosmetics;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Asynchronous persistent cosmetic unlock/equip contract. */
public interface CosmeticService extends PluginService {

    CosmeticSettings settings();

    Collection<CosmeticDefinition> definitions();

    CompletableFuture<Set<String>> unlockedIds(UUID playerId);

    CompletableFuture<Optional<CosmeticDefinition>> findEquipped(UUID playerId);

    CompletableFuture<CosmeticEquipResult> equip(UUID playerId, String playerName, String cosmeticId);

    /**
     * Re-checks every not-yet-unlocked cosmetic's condition against the
     * player's current cross-system state, persists any newly satisfied
     * ones, and returns just the newly-unlocked definitions.
     */
    CompletableFuture<List<CosmeticDefinition>> evaluate(UUID playerId, String playerName);

    /** Fires the player's equipped cosmetic's particle effect, if any - a no-op otherwise. */
    void celebrate(Player player, CelebrationMoment moment);

    /** Synchronous, in-memory only - for callers that cannot await a future (e.g. PlaceholderAPI). */
    Optional<String> equippedDisplayName(UUID playerId);

    void evict(UUID playerId);
}
