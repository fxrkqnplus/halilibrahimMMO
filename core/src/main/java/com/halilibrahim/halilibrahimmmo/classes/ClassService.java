package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.api.classes.PlayerClassSnapshot;
import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Owns configured classes, shared talent budgets, selections and active modifiers. */
public interface ClassService extends PluginService {
    ClassSettings settings();
    Collection<PlayerClassDefinition> definitions();
    String displayName(String classId);
    CompletableFuture<PlayerClassProgress> load(UUID playerId, String playerName);
    CompletableFuture<ClassSelectionResult> select(UUID playerId, String playerName, String classId);
    CompletableFuture<TalentSpendResult> spend(UUID playerId, String playerName, String talentId);
    CompletableFuture<Optional<PlayerClassSnapshot>> findSnapshot(UUID playerId);
    ClassModifiers modifiers(UUID playerId);
    void applyPlayerEffects(Player player);
    void evict(UUID playerId);
}
