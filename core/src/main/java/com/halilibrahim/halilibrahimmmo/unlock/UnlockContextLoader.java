package com.halilibrahim.halilibrahimmmo.unlock;

import com.halilibrahim.halilibrahimmmo.collections.CollectionDefinition;
import com.halilibrahim.halilibrahimmmo.collections.CollectionProgress;
import com.halilibrahim.halilibrahimmmo.collections.CollectionService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.quests.QuestProgress;
import com.halilibrahim.halilibrahimmmo.quests.QuestService;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Assembles one player's cross-system {@link UnlockContext} from every other
 * subsystem's current state. The one deliberate exception to this codebase's
 * "quest/collection services stay decoupled from profile/progression" rule -
 * title/cosmetic unlock criteria are inherently a read over everything else.
 */
public final class UnlockContextLoader {

    private final ProfileService profiles;
    private final SkillService skills;
    private final QuestService quests;
    private final CollectionService collections;

    public UnlockContextLoader(ProfileService profiles, SkillService skills, QuestService quests, CollectionService collections) {
        this.profiles = profiles;
        this.skills = skills;
        this.quests = quests;
        this.collections = collections;
    }

    public CompletableFuture<UnlockContext> load(UUID playerId, String playerName) {
        return profiles.load(playerId, playerName).thenCompose(profile ->
                quests.loadAll(playerId).thenCompose(questProgress ->
                        collections.loadAll(playerId).thenCompose(collectionProgress ->
                                loadSkillLevels(playerId).thenApply(skillLevels -> new UnlockContext(
                                        profile.globalLevel(),
                                        profile.prestige(),
                                        completedQuestIds(questProgress),
                                        completedCollectionIds(collectionProgress),
                                        skillLevels
                                )))));
    }

    private Set<String> completedQuestIds(Map<String, QuestProgress> progressById) {
        return progressById.values().stream()
                .filter(QuestProgress::completed)
                .map(QuestProgress::questId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> completedCollectionIds(Map<String, CollectionProgress> progressById) {
        Map<String, CollectionDefinition> definitions = collections.settings().definitions();
        return progressById.values().stream()
                .filter(progress -> {
                    CollectionDefinition definition = definitions.get(progress.collectionId());
                    return definition != null && progress.completed(definition);
                })
                .map(CollectionProgress::collectionId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private CompletableFuture<Map<String, Integer>> loadSkillLevels(UUID playerId) {
        Map<String, Integer> levels = new HashMap<>();
        CompletableFuture<?>[] futures = skills.definitions().stream()
                .map(definition -> skills.find(playerId, definition.id()).thenAccept(found ->
                        found.ifPresent(progress -> levels.put(definition.id(), progress.level()))))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures).thenApply(ignored -> Map.copyOf(levels));
    }
}
