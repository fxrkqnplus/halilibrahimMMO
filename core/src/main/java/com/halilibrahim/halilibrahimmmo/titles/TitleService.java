package com.halilibrahim.halilibrahimmmo.titles;

import com.halilibrahim.halilibrahimmmo.core.PluginService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Asynchronous persistent title unlock/equip contract. */
public interface TitleService extends PluginService {

    TitleSettings settings();

    Collection<TitleDefinition> definitions();

    CompletableFuture<Set<String>> unlockedIds(UUID playerId);

    CompletableFuture<Optional<TitleDefinition>> findEquipped(UUID playerId);

    CompletableFuture<TitleEquipResult> equip(UUID playerId, String playerName, String titleId);

    /**
     * Re-checks every not-yet-unlocked title's condition against the
     * player's current cross-system state, persists any newly satisfied
     * ones, and returns just the newly-unlocked definitions.
     */
    CompletableFuture<List<TitleDefinition>> evaluate(UUID playerId, String playerName);

    /** Synchronous, in-memory only - for the chat renderer, which cannot afford an async DB round-trip per message. */
    Optional<String> equippedDisplayName(UUID playerId);

    /** Synchronous, in-memory only - the equipped title's chat/sidebar tag style, alongside {@link #equippedDisplayName}. */
    Optional<TitleChatStyle> equippedStyle(UUID playerId);

    void evict(UUID playerId);
}
