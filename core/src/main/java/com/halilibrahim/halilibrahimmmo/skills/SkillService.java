package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Asynchronous persistent skill progression contract. */
public interface SkillService extends PluginService {

    SkillSettings settings();

    Collection<SkillDefinition> definitions();

    CompletableFuture<Optional<SkillProgress>> find(UUID playerId, String skillId);

    CompletableFuture<SkillProgress> load(UUID playerId, String skillId);

    CompletableFuture<Void> loadAll(UUID playerId);

    CompletableFuture<Optional<SkillAwardResult>> awardBlock(UUID playerId, String playerName, Material material);

    CompletableFuture<Optional<SkillAwardResult>> awardMob(UUID playerId, String playerName, EntityType entityType);

    /** Synchronous, in-memory only - for callers that cannot await a future (e.g. PlaceholderAPI). */
    Optional<SkillProgress> cached(UUID playerId, String skillId);

    void evict(UUID playerId);
}
