package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.Material;

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

    CompletableFuture<Optional<SkillProgress>> awardBlock(UUID playerId, Material material);

    CompletableFuture<Optional<SkillProgress>> awardMob(UUID playerId);

    /** Synchronous, in-memory only - for callers that cannot await a future (e.g. PlaceholderAPI). */
    Optional<SkillProgress> cached(UUID playerId, String skillId);

    void evict(UUID playerId);
}
