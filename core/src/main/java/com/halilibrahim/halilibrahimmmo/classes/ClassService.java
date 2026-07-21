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

    /**
     * Internal: awards class-track XP to one specific (player, class) pair
     * through the shared per-player update chain, independent of whether
     * that class is currently selected. Callers (the skill-synergy bridge)
     * are responsible for only calling this for the player's selected class.
     */
    CompletableFuture<ClassExperienceAwardResult> awardClassExperience(UUID playerId, String playerName, String classId, long amount);

    /** Synchronous, in-memory cached read of one (player, class) level+XP - level 1/XP 0 default if never loaded. */
    ClassLevelProgress classProgress(UUID playerId, String classId);
}
