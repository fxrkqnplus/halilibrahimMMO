package com.halilibrahim.halilibrahimmmo;

import com.halilibrahim.halilibrahimmmo.api.CachedPlayerSnapshot;
import com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI;
import com.halilibrahim.halilibrahimmmo.api.profile.PlayerProfileSnapshot;
import com.halilibrahim.halilibrahimmmo.api.skills.SkillDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.skills.SkillSnapshot;
import com.halilibrahim.halilibrahimmmo.api.classes.ClassDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.classes.PlayerClassSnapshot;
import com.halilibrahim.halilibrahimmmo.api.collections.CollectionDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.collections.CollectionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.cosmetics.CosmeticDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.cosmetics.CosmeticSnapshot;
import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardCategory;
import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardEntrySnapshot;
import com.halilibrahim.halilibrahimmmo.api.quests.QuestDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.quests.QuestSnapshot;
import com.halilibrahim.halilibrahimmmo.api.titles.TitleDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.titles.TitleSnapshot;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.collections.CollectionDefinition;
import com.halilibrahim.halilibrahimmmo.collections.CollectionService;
import com.halilibrahim.halilibrahimmmo.boss.BossService;
import com.halilibrahim.halilibrahimmmo.config.ConfigService;
import com.halilibrahim.halilibrahimmmo.wealth.WealthService;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.leaderboards.LeaderboardEntry;
import com.halilibrahim.halilibrahimmmo.leaderboards.LeaderboardService;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import com.halilibrahim.halilibrahimmmo.quests.QuestDefinition;
import com.halilibrahim.halilibrahimmmo.quests.QuestProgress;
import com.halilibrahim.halilibrahimmmo.quests.QuestService;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.classes.TalentNodeDefinition;
import com.halilibrahim.halilibrahimmmo.api.classes.TalentNodeSnapshot;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of the public API facade, registered with Bukkit's
 * {@code ServicesManager} once {@link HalilIbrahimMMOPlugin} finishes enabling.
 */
public final class HalilIbrahimMMOApiImpl implements HalilIbrahimMMOAPI {

    private final HalilIbrahimMMOPlugin plugin;
    private final ConfigService configService;
    private final MessageService messageService;
    private final PluginLogger logger;
    private final ProfileService profiles;
    private final ProgressionService progression;
    private final SkillService skills;
    private final AbilityService abilities;
    private final ClassService classes;
    private final PrestigeService prestige;
    private final QuestService quests;
    private final CollectionService collections;
    private final LeaderboardService leaderboards;
    private final TitleService titles;
    private final CosmeticService cosmetics;
    private final FeedbackService feedback;
    private final WealthService wealth;
    private final BossService bosses;

    public HalilIbrahimMMOApiImpl(
            HalilIbrahimMMOPlugin plugin,
            ConfigService configService,
            MessageService messageService,
            PluginLogger logger,
            ProfileService profiles,
            ProgressionService progression,
            SkillService skills,
            AbilityService abilities,
            ClassService classes,
            PrestigeService prestige,
            QuestService quests,
            CollectionService collections,
            LeaderboardService leaderboards,
            TitleService titles,
            CosmeticService cosmetics,
            FeedbackService feedback,
            WealthService wealth,
            BossService bosses
    ) {
        this.plugin = plugin;
        this.configService = configService;
        this.messageService = messageService;
        this.logger = logger;
        this.profiles = profiles;
        this.progression = progression;
        this.skills = skills;
        this.abilities = abilities;
        this.classes = classes;
        this.prestige = prestige;
        this.quests = quests;
        this.collections = collections;
        this.leaderboards = leaderboards;
        this.titles = titles;
        this.cosmetics = cosmetics;
        this.feedback = feedback;
        this.wealth = wealth;
        this.bosses = bosses;
    }

    @Override
    public String getPluginVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public String getSupportedMinecraftVersion() {
        return plugin.getPluginMeta().getAPIVersion();
    }

    @Override
    public boolean isDebugModeEnabled() {
        return configService.coreConfig().debugMode();
    }

    @Override
    public boolean reload() {
        try {
            configService.reload();
            messageService.reload();
            progression.reload();
            skills.reload();
            abilities.reload();
            classes.reload();
            prestige.reload();
            quests.reload();
            collections.reload();
            titles.reload();
            cosmetics.reload();
            feedback.reload();
            wealth.reload();
            bosses.reload();
            logger.info("Yapilandirma ve mesajlar basariyla yeniden yuklendi.");
            return true;
        } catch (Exception exception) {
            logger.error("Yeniden yukleme sirasinda bir hata olustu.", exception);
            return false;
        }
    }

    @Override
    public CompletableFuture<Optional<PlayerProfileSnapshot>> findProfile(UUID playerId) {
        return profiles.find(playerId).thenApply(profile -> profile.map(value -> value.snapshot()));
    }

    @Override
    public Collection<SkillDefinitionSnapshot> getSkills() {
        return skills.definitions().stream().map(this::snapshot).toList();
    }

    @Override
    public CompletableFuture<Optional<SkillSnapshot>> findSkill(UUID playerId, String skillId) {
        return skills.find(playerId, skillId).thenApply(progress -> progress.map(value -> value.snapshot()));
    }

    @Override
    public Collection<ClassDefinitionSnapshot> getClasses() {
        return classes.definitions().stream().map(this::snapshot).toList();
    }

    @Override
    public CompletableFuture<Optional<PlayerClassSnapshot>> findClassProgress(UUID playerId) {
        return classes.findSnapshot(playerId);
    }

    @Override
    public Collection<QuestDefinitionSnapshot> getQuests() {
        return quests.definitions().stream().map(this::snapshot).toList();
    }

    @Override
    public CompletableFuture<Optional<QuestSnapshot>> findQuest(UUID playerId, String questId) {
        return quests.find(playerId, questId).thenApply(progress -> progress.map(QuestProgress::snapshot));
    }

    @Override
    public Collection<CollectionDefinitionSnapshot> getCollections() {
        return collections.definitions().stream().map(this::snapshot).toList();
    }

    @Override
    public CompletableFuture<Optional<CollectionSnapshot>> findCollection(UUID playerId, String collectionId) {
        CollectionDefinition definition = collections.settings().definitions().get(collectionId);
        if (definition == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return collections.find(playerId, collectionId).thenApply(progress -> progress.map(value -> value.snapshot(definition)));
    }

    @Override
    public CompletableFuture<List<LeaderboardEntrySnapshot>> getLeaderboard(LeaderboardCategory category) {
        return leaderboards.top(category).thenApply(entries -> entries.stream().map(this::snapshot).toList());
    }

    @Override
    public CompletableFuture<List<LeaderboardEntrySnapshot>> getSkillLeaderboard(String skillId) {
        return leaderboards.topSkill(skillId).thenApply(entries -> entries.stream().map(this::snapshot).toList());
    }

    private LeaderboardEntrySnapshot snapshot(LeaderboardEntry entry) {
        return new LeaderboardEntrySnapshot(entry.rank(), entry.playerName(), entry.primaryValue(), entry.secondaryValue());
    }

    @Override
    public Collection<TitleDefinitionSnapshot> getTitles() {
        return titles.definitions().stream().map(this::snapshot).toList();
    }

    @Override
    public CompletableFuture<Optional<TitleSnapshot>> findEquippedTitle(UUID playerId) {
        return titles.findEquipped(playerId).thenApply(found -> found.map(definition ->
                new TitleSnapshot(playerId, definition.id(), definition.displayName())));
    }

    @Override
    public Collection<CosmeticDefinitionSnapshot> getCosmetics() {
        return cosmetics.definitions().stream().map(this::snapshot).toList();
    }

    @Override
    public CompletableFuture<Optional<CosmeticSnapshot>> findEquippedCosmetic(UUID playerId) {
        return cosmetics.findEquipped(playerId).thenApply(found -> found.map(definition ->
                new CosmeticSnapshot(playerId, definition.id(), definition.displayName())));
    }

    @Override
    public Optional<CachedPlayerSnapshot> findCachedPlayer(UUID playerId) {
        return profiles.cached(playerId).map(profile -> new CachedPlayerSnapshot(
                playerId,
                progression.cachedGlobalLevel(playerId),
                0L,
                0L,
                profile.prestige(),
                profile.selectedClass(),
                titles.equippedDisplayName(playerId).orElse(""),
                cosmetics.equippedDisplayName(playerId).orElse("")
        ));
    }

    @Override
    public Optional<SkillSnapshot> findCachedSkill(UUID playerId, String skillId) {
        return skills.cached(playerId, skillId).map(progress -> progress.snapshot());
    }

    private TitleDefinitionSnapshot snapshot(TitleDefinition definition) {
        return new TitleDefinitionSnapshot(definition.id(), definition.displayName());
    }

    private CosmeticDefinitionSnapshot snapshot(CosmeticDefinition definition) {
        return new CosmeticDefinitionSnapshot(definition.id(), definition.displayName());
    }

    private QuestDefinitionSnapshot snapshot(QuestDefinition definition) {
        return new QuestDefinitionSnapshot(definition.id(), definition.displayName(), definition.target());
    }

    private CollectionDefinitionSnapshot snapshot(CollectionDefinition definition) {
        return new CollectionDefinitionSnapshot(definition.id(), definition.displayName(), definition.entries().size());
    }

    private SkillDefinitionSnapshot snapshot(SkillDefinition definition) {
        return new SkillDefinitionSnapshot(definition.id(), definition.displayName(), skills.settings().curve().maximumLevel());
    }

    private ClassDefinitionSnapshot snapshot(PlayerClassDefinition definition) {
        return new ClassDefinitionSnapshot(definition.id(), definition.displayName(), definition.talents().values().stream()
                .map(this::snapshot).toList());
    }

    private TalentNodeSnapshot snapshot(TalentNodeDefinition definition) {
        return new TalentNodeSnapshot(definition.id(), definition.displayName(), definition.maximumRank());
    }
}
