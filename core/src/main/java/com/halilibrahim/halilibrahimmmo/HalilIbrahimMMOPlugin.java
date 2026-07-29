package com.halilibrahim.halilibrahimmmo;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.activation.ActivationRepository;
import com.halilibrahim.halilibrahimmmo.activation.CachedActivationService;
import com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI;
import com.halilibrahim.halilibrahimmmo.boss.BossListener;
import com.halilibrahim.halilibrahimmmo.boss.BossService;
import com.halilibrahim.halilibrahimmmo.boss.YamlBossService;
import com.halilibrahim.halilibrahimmmo.command.AdminCommand;
import com.halilibrahim.halilibrahimmmo.command.MmoCommand;
import com.halilibrahim.halilibrahimmmo.command.ProfileCommand;
import com.halilibrahim.halilibrahimmmo.command.AbilityCommand;
import com.halilibrahim.halilibrahimmmo.command.ClassCommand;
import com.halilibrahim.halilibrahimmmo.command.CollectionCommand;
import com.halilibrahim.halilibrahimmmo.command.CosmeticCommand;
import com.halilibrahim.halilibrahimmmo.command.JournalCommand;
import com.halilibrahim.halilibrahimmmo.command.LeaderboardCommand;
import com.halilibrahim.halilibrahimmmo.command.MenuCommand;
import com.halilibrahim.halilibrahimmmo.command.PanelCommand;
import com.halilibrahim.halilibrahimmmo.command.PrestigeCommand;
import com.halilibrahim.halilibrahimmmo.command.QuestCommand;
import com.halilibrahim.halilibrahimmmo.command.RandomTeleportCommand;
import com.halilibrahim.halilibrahimmmo.command.HomeCommand;
import com.halilibrahim.halilibrahimmmo.command.MapCommand;
import com.halilibrahim.halilibrahimmmo.command.SkillDetailCommand;
import com.halilibrahim.halilibrahimmmo.command.SkillsCommand;
import com.halilibrahim.halilibrahimmmo.command.TitleCommand;
import com.halilibrahim.halilibrahimmmo.command.TradeCommand;
import com.halilibrahim.halilibrahimmmo.classes.ClassAwardHandler;
import com.halilibrahim.halilibrahimmmo.classes.ClassListener;
import com.halilibrahim.halilibrahimmmo.classes.ClassModifiers;
import com.halilibrahim.halilibrahimmmo.classes.ClassRepository;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.classes.ClassSettings;
import com.halilibrahim.halilibrahimmmo.classes.ClassSynergyBridge;
import com.halilibrahim.halilibrahimmmo.classes.YamlClassService;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityActivationListener;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityExplosionListener;
import com.halilibrahim.halilibrahimmmo.backpack.BackpackListener;
import com.halilibrahim.halilibrahimmmo.backpack.BackpackRepository;
import com.halilibrahim.halilibrahimmmo.backpack.BackpackService;
import com.halilibrahim.halilibrahimmmo.backpack.DatabaseBackpackService;
import com.halilibrahim.halilibrahimmmo.command.BackpackCommand;
import com.halilibrahim.halilibrahimmmo.command.GuideCommand;
import com.halilibrahim.halilibrahimmmo.database.migration.BackpackSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.TalentGrantSchemaMigration;
import com.halilibrahim.halilibrahimmmo.guide.GuideService;
import com.halilibrahim.halilibrahimmmo.guide.YamlGuideService;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.abilities.YamlAbilityService;
import com.halilibrahim.halilibrahimmmo.collections.CachedCollectionService;
import com.halilibrahim.halilibrahimmmo.collections.CollectionListener;
import com.halilibrahim.halilibrahimmmo.collections.CollectionRepository;
import com.halilibrahim.halilibrahimmmo.collections.CollectionService;
import com.halilibrahim.halilibrahimmmo.config.ConfigService;
import com.halilibrahim.halilibrahimmmo.config.YamlConfigService;
import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.core.PluginService;
import com.halilibrahim.halilibrahimmmo.core.ServiceContainer;
import com.halilibrahim.halilibrahimmmo.database.DatabaseService;
import com.halilibrahim.halilibrahimmmo.database.HikariDatabaseService;
import com.halilibrahim.halilibrahimmmo.database.migration.CoreSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.Migration;
import com.halilibrahim.halilibrahimmmo.database.migration.MigrationRunner;
import com.halilibrahim.halilibrahimmmo.database.migration.PlayerProfileSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.QuestSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.SkillSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.ClassSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.ClassProgressSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.OnboardingSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.PlayerActivationSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.QuestAssignmentSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.QuestPinSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.TitleCosmeticSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.PlayerHomeSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.PrestigeBankedPowerSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.WealthSchemaMigration;
import com.halilibrahim.halilibrahimmmo.cosmetics.CachedCosmeticService;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticListener;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticRepository;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.exploit.BlockBreakRateLimiter;
import com.halilibrahim.halilibrahimmmo.exploit.PlacedBlockTracker;
import com.halilibrahim.halilibrahimmmo.energy.PlayerEnergyService;
import com.halilibrahim.halilibrahimmmo.feedback.BossBarFeedbackService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.gui.ChatPromptService;
import com.halilibrahim.halilibrahimmmo.gui.GuiListener;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
import com.halilibrahim.halilibrahimmmo.gui.TradeScreenBridge;
import com.halilibrahim.halilibrahimmmo.home.CachedHomeService;
import com.halilibrahim.halilibrahimmmo.home.HomeRepository;
import com.halilibrahim.halilibrahimmmo.home.HomeService;
import com.halilibrahim.halilibrahimmmo.journal.DatabaseJournalService;
import com.halilibrahim.halilibrahimmmo.journal.JournalRepository;
import com.halilibrahim.halilibrahimmmo.journal.JournalService;
import com.halilibrahim.halilibrahimmmo.leaderboards.CachedLeaderboardService;
import com.halilibrahim.halilibrahimmmo.leaderboards.LeaderboardRepository;
import com.halilibrahim.halilibrahimmmo.leaderboards.LeaderboardService;
import com.halilibrahim.halilibrahimmmo.luckperms.LuckPermsHook;
import com.halilibrahim.halilibrahimmmo.placeholder.PlaceholderApiHook;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.locale.MiniMessageService;
import com.halilibrahim.halilibrahimmmo.modcheck.ModCheckListener;
import com.halilibrahim.halilibrahimmmo.modcheck.ModCheckProtocol;
import com.halilibrahim.halilibrahimmmo.modcheck.ModVerificationService;
import com.halilibrahim.halilibrahimmmo.modcheck.PluginMessageModVerificationService;
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import com.halilibrahim.halilibrahimmmo.onboarding.CachedOnboardingService;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingRepository;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingRewarder;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingService;
import com.halilibrahim.halilibrahimmmo.profile.CachedProfileService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileRepository;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionListener;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import com.halilibrahim.halilibrahimmmo.progression.YamlProgressionService;
import com.halilibrahim.halilibrahimmmo.quests.CachedQuestService;
import com.halilibrahim.halilibrahimmmo.quests.QuestCompletionHandler;
import com.halilibrahim.halilibrahimmmo.quests.QuestDeedListener;
import com.halilibrahim.halilibrahimmmo.quests.QuestDiscoveryListener;
import com.halilibrahim.halilibrahimmmo.quests.QuestListener;
import com.halilibrahim.halilibrahimmmo.quests.QuestRepository;
import com.halilibrahim.halilibrahimmmo.quests.QuestService;
import com.halilibrahim.halilibrahimmmo.server.ServerInfoService;
import com.halilibrahim.halilibrahimmmo.server.SleepListener;
import com.halilibrahim.halilibrahimmmo.server.TabListService;
import com.halilibrahim.halilibrahimmmo.server.YamlServerInfoService;
import com.halilibrahim.halilibrahimmmo.scoreboard.ScoreboardSidebarService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarListener;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import com.halilibrahim.halilibrahimmmo.skills.CachedSkillService;
import com.halilibrahim.halilibrahimmmo.skills.FarmingListener;
import com.halilibrahim.halilibrahimmmo.skills.HusbandryListener;
import com.halilibrahim.halilibrahimmmo.skills.SkillAwardHandler;
import com.halilibrahim.halilibrahimmmo.skills.SkillListener;
import com.halilibrahim.halilibrahimmmo.skills.SkillRepository;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import com.halilibrahim.halilibrahimmmo.prestige.BankedPowerRepository;
import com.halilibrahim.halilibrahimmmo.prestige.BankedPowerService;
import com.halilibrahim.halilibrahimmmo.prestige.CachedBankedPowerService;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeService;
import com.halilibrahim.halilibrahimmmo.prestige.YamlPrestigeService;
import com.halilibrahim.halilibrahimmmo.titles.CachedTitleService;
import com.halilibrahim.halilibrahimmmo.titles.TitleChatListener;
import com.halilibrahim.halilibrahimmmo.titles.TitleListener;
import com.halilibrahim.halilibrahimmmo.titles.TitleRepository;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import com.halilibrahim.halilibrahimmmo.trade.InMemoryTradeService;
import com.halilibrahim.halilibrahimmmo.trade.TradeListener;
import com.halilibrahim.halilibrahimmmo.trade.TradeService;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockContextLoader;
import com.halilibrahim.halilibrahimmmo.wealth.CachedWealthService;
import com.halilibrahim.halilibrahimmmo.wealth.WealthAwardHandler;
import com.halilibrahim.halilibrahimmmo.wealth.WealthRankAnnouncer;
import com.halilibrahim.halilibrahimmmo.wealth.WealthRepository;
import com.halilibrahim.halilibrahimmmo.wealth.WealthService;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HalilIbrahimMMO's Paper entry point.
 *
 * <p>Responsible only for bootstrap wiring: constructing every {@link PluginService}
 * in dependency order, registering them with the {@link ServiceContainer}, running
 * pending database migrations exactly once, publishing the public
 * {@link HalilIbrahimMMOAPI} through Bukkit's {@code ServicesManager}, and
 * registering the {@code /haliladmin} command. No gameplay or business logic lives here -
 * that is the entire point of splitting every system into its own service class.</p>
 */
public final class HalilIbrahimMMOPlugin extends JavaPlugin {

    private final ServiceContainer services = new ServiceContainer();
    private final List<PluginService> enabledServices = new ArrayList<>();

    private PluginLogger pluginLogger;
    private HalilIbrahimMMOAPI api;
    private ProgressionListener progressionListener;
    private HusbandryListener husbandryListener;
    private QuestDiscoveryListener questDiscoveryListener;
    private WealthRankAnnouncer wealthRankAnnouncer;
    private Runnable placeholderTeardown;
    private Runnable luckPermsTeardown;

    @Override
    public void onEnable() {
        this.pluginLogger = new PluginLogger(getSLF4JLogger(), this::isDebugModeCurrentlyEnabled);

        try {
            // Constructed before every service because AbilityService needs it; it owns no
            // lifecycle of its own, only a NamespacedKey and a per-loaded-chunk read cache.
            PlacedBlockTracker placedBlockTracker = new PlacedBlockTracker(this);
            ConfigService configService = startService(new YamlConfigService(this, pluginLogger), ConfigService.class);
            MessageService messageService = startService(new MiniMessageService(this, pluginLogger), MessageService.class);
            // Behaviour switches only (sleep, TAB list) and no dependency of its
            // own, so it can sit next to the two config services it belongs with.
            ServerInfoService serverInfoService = startService(new YamlServerInfoService(this), ServerInfoService.class);

            // Depends only on config + locale, and deliberately runs before the
            // database: a player who fails mod verification must be turned away
            // whether or not the rest of the plugin is healthy.
            ModVerificationService modVerificationService = startService(
                    new PluginMessageModVerificationService(this, messageService, pluginLogger),
                    ModVerificationService.class
            );
            registerModCheckChannel(modVerificationService);

            DatabaseService databaseService = startService(
                    new HikariDatabaseService(configService.coreConfig().databaseSettings(), getDataFolder().toPath(), pluginLogger),
                    DatabaseService.class
            );
            runPendingMigrations(databaseService);

            ProfileService profileService = startService(
                    new CachedProfileService(new ProfileRepository(databaseService)), ProfileService.class
            );
            ActivationService activationService = startService(
                    new CachedActivationService(new ActivationRepository(databaseService)), ActivationService.class
            );
            OnboardingService onboardingService = startService(
                    new CachedOnboardingService(new OnboardingRepository(databaseService)), OnboardingService.class
            );
            BankedPowerService bankedPowerService = startService(
                    new CachedBankedPowerService(new BankedPowerRepository(databaseService)), BankedPowerService.class
            );
            WealthService wealthService = startService(
                    new CachedWealthService(this, new WealthRepository(databaseService)), WealthService.class
            );
            FeedbackService feedbackService = startService(
                    new BossBarFeedbackService(this, messageService), FeedbackService.class
            );
            AtomicReference<ClassService> classServiceReference = new AtomicReference<>();
            AtomicReference<AbilityService> abilityServiceReference = new AtomicReference<>();
            // Owns every piece of feedback a class XP award produces, whichever
            // of the six sources it came from. Built here, before its first
            // consumer, and reads both services through the same forward
            // references the synergy bridge already needs.
            ClassAwardHandler classAwardHandler = new ClassAwardHandler(
                    this, classServiceReference, abilityServiceReference, feedbackService, messageService
            );
            SkillService skillService = startService(
                    new CachedSkillService(this, new SkillRepository(databaseService),
                            (playerId, skillId) -> profileService.cached(playerId).map(profile -> {
                                double prestigeMultiplier = 1.0 + 0.25 * profile.prestige();
                                ClassService cs = classServiceReference.get();
                                String classId = profile.selectedClass();
                                if (cs == null || classId.isEmpty()) {
                                    return prestigeMultiplier;
                                }
                                ClassSettings classSettings = cs.settings();
                                var definition = classSettings.definitions().get(classId);
                                double synergyMultiplier = definition != null && definition.hasSynergyWith(skillId)
                                        ? 1.0 + classSettings.synergyExperienceBonus()
                                        : 1.0;
                                return synergyMultiplier * prestigeMultiplier;
                            }).orElse(1.0),
                            new ClassSynergyBridge(profileService, classServiceReference, classAwardHandler)
                    ), SkillService.class
            );
            ProgressionService progressionService = startService(
                    new YamlProgressionService(skillService, bankedPowerService), ProgressionService.class
            );
            YamlAbilityService yamlAbilityService = new YamlAbilityService(
                    this, skillService, messageService, profileService, feedbackService, placedBlockTracker, playerId -> {
                        ClassService classService = classServiceReference.get();
                        return classService == null ? ClassModifiers.none() : classService.modifiers(playerId);
                    }, (playerId, classId) -> {
                        ClassService classService = classServiceReference.get();
                        return classService == null ? 1 : classService.classProgress(playerId, classId).level();
                    });
            AbilityService abilityService = startService(yamlAbilityService, AbilityService.class);
            abilityServiceReference.set(abilityService);
            // The concrete type is needed here and only here: the explosion
            // listener has to be told which persistent-data keys mark a charge
            // as ours, and those keys belong to the implementation that writes
            // them rather than to the service contract.
            getServer().getPluginManager().registerEvents(new AbilityExplosionListener(abilityService,
                    yamlAbilityService.mineBombKey(), yamlAbilityService.mineBombOwnerKey(),
                    yamlAbilityService.mineBombAbilityKey()), this);
            ClassService classService = startService(
                    new YamlClassService(this, profileService, new ClassRepository(databaseService), abilityService, skillService, progressionService), ClassService.class
            );
            classServiceReference.set(classService);
            EnergyService energyService = startService(
                    new PlayerEnergyService(this, messageService, abilityService::settings,
                            player -> classService.modifiers(player.getUniqueId()).maximumEnergyBonus()
                                    + skillService.milestoneModifiers(player.getUniqueId()).maxEnergyBonus(),
                            player -> abilityService.activeStatus(player.getUniqueId()).isPresent()), EnergyService.class
            );
            PrestigeService prestigeService = startService(
                    new YamlPrestigeService(this, profileService, progressionService, skillService, classService, bankedPowerService), PrestigeService.class
            );
            JournalService journalService = startService(
                    new DatabaseJournalService(new JournalRepository(databaseService)), JournalService.class
            );
            QuestService questService = startService(
                    new CachedQuestService(this, new QuestRepository(databaseService), skillService), QuestService.class
            );
            HomeService homeService = startService(
                    new CachedHomeService(this, new HomeRepository(databaseService), profileService, classService), HomeService.class
            );
            InMemoryTradeService tradeService = new InMemoryTradeService(
                    profileService, classService, activationService, messageService, feedbackService
            );
            startService(tradeService, TradeService.class);
            CollectionService collectionService = startService(
                    new CachedCollectionService(this, new CollectionRepository(databaseService)), CollectionService.class
            );
            LeaderboardService leaderboardService = startService(
                    new CachedLeaderboardService(this, new LeaderboardRepository(databaseService, skillService)), LeaderboardService.class
            );
            UnlockContextLoader unlockContextLoader = new UnlockContextLoader(profileService, skillService, questService, collectionService, wealthService);
            TitleService titleService = startService(
                    new CachedTitleService(this, new TitleRepository(databaseService), unlockContextLoader), TitleService.class
            );
            CosmeticService cosmeticService = startService(
                    new CachedCosmeticService(this, new CosmeticRepository(databaseService), unlockContextLoader), CosmeticService.class
            );
            SidebarService sidebarService = startService(
                    new ScoreboardSidebarService(this, messageService, profileService, classService, progressionService,
                            activationService, abilityService, questService, titleService, onboardingService,
                            homeService, tradeService, wealthService), SidebarService.class
            );
            // After TitleService (entry names carry the equipped title) and
            // ProgressionService (each entry shows its owner's Güç).
            TabListService tabListService = startService(
                    new TabListService(this, messageService, serverInfoService, titleService, progressionService), TabListService.class
            );
            OnboardingRewarder onboardingRewarder = new OnboardingRewarder(
                    this, onboardingService, profileService, classService, messageService, sidebarService, classAwardHandler
            );
            WealthAwardHandler wealthAwardHandler = new WealthAwardHandler(this, messageService, feedbackService, sidebarService);
            this.wealthRankAnnouncer = new WealthRankAnnouncer(this, messageService, leaderboardService, wealthService);
            wealthRankAnnouncer.start();
            YamlBossService bossService = new YamlBossService(this, messageService, feedbackService);
            startService(bossService, BossService.class);
            getServer().getPluginManager().registerEvents(new BossListener(this, bossService, profileService, classService,
                    classAwardHandler, wealthService, wealthAwardHandler, feedbackService, messageService), this);
            BlockBreakRateLimiter breakRateLimiter = new BlockBreakRateLimiter();
            getServer().getPluginManager().registerEvents(placedBlockTracker, this);
            this.progressionListener = new ProgressionListener(profileService, feedbackService, breakRateLimiter, activationService, bankedPowerService, wealthService);
            getServer().getPluginManager().registerEvents(progressionListener, this);
            SkillAwardHandler skillAwardHandler = new SkillAwardHandler(this, skillService, feedbackService, progressionService, messageService);
            getServer().getPluginManager().registerEvents(new SkillListener(this, skillService, abilityService, energyService, activationService, breakRateLimiter, placedBlockTracker, skillAwardHandler, wealthService, wealthAwardHandler), this);
            this.husbandryListener = new HusbandryListener(this, skillService, activationService, messageService, skillAwardHandler);
            getServer().getPluginManager().registerEvents(husbandryListener, this);
            husbandryListener.start();
            getServer().getPluginManager().registerEvents(new FarmingListener(this, skillService, activationService, skillAwardHandler), this);
            getServer().getPluginManager().registerEvents(new ClassListener(this, classService, abilityService, energyService, skillService), this);
            getServer().getPluginManager().registerEvents(new AbilityActivationListener(abilityService, energyService, messageService), this);
            getServer().getPluginManager().registerEvents(new SidebarListener(this, sidebarService, profileService, classService, activationService, onboardingService, homeService), this);
            QuestCompletionHandler questCompletionHandler = new QuestCompletionHandler(
                    this, questService, profileService, classService, skillService, titleService, cosmeticService,
                    feedbackService, messageService, journalService, sidebarService, skillAwardHandler, classAwardHandler,
                    wealthService, wealthAwardHandler
            );
            getServer().getPluginManager().registerEvents(
                    new QuestListener(questService, abilityService, breakRateLimiter, placedBlockTracker, activationService, questCompletionHandler), this
            );
            getServer().getPluginManager().registerEvents(
                    new QuestDeedListener(questService, activationService, questCompletionHandler), this
            );
            this.questDiscoveryListener = new QuestDiscoveryListener(this, questService, activationService, questCompletionHandler);
            getServer().getPluginManager().registerEvents(questDiscoveryListener, this);
            questDiscoveryListener.start();
            getServer().getPluginManager().registerEvents(new TradeListener(tradeService), this);
            getServer().getPluginManager().registerEvents(
                    new CollectionListener(this, collectionService, profileService, classService, titleService, cosmeticService, feedbackService, messageService, journalService, sidebarService, activationService, classAwardHandler), this
            );
            getServer().getPluginManager().registerEvents(new TitleListener(titleService), this);
            getServer().getPluginManager().registerEvents(new TitleChatListener(titleService), this);
            getServer().getPluginManager().registerEvents(new CosmeticListener(this, cosmeticService), this);
            getServer().getPluginManager().registerEvents(new GuiListener(), this);
            getServer().getPluginManager().registerEvents(new SleepListener(serverInfoService, messageService), this);
            // Both a service (so it clears its pending prompts on shutdown) and
            // a listener (so it can claim the answer out of chat).
            ChatPromptService chatPromptService = startService(new ChatPromptService(this, messageService), ChatPromptService.class);
            getServer().getPluginManager().registerEvents(chatPromptService, this);
            // The handbook depends on nothing but its own file, so it can be
            // started anywhere; it lives here next to the screens that open it.
            GuideService guideService = startService(new YamlGuideService(this), GuideService.class);
            BackpackService backpackService = startService(
                    new DatabaseBackpackService(this, new BackpackRepository(databaseService), profileService,
                            classService, messageService), BackpackService.class
            );
            getServer().getPluginManager().registerEvents(new BackpackListener(backpackService), this);

            this.api = new HalilIbrahimMMOApiImpl(
                    this, configService, messageService, pluginLogger, profileService, progressionService, skillService,
                    abilityService, classService, prestigeService, questService, collectionService, leaderboardService,
                    titleService, cosmeticService, feedbackService, wealthService, bossService, modVerificationService,
                    serverInfoService, tabListService
            );
            getServer().getServicesManager().register(HalilIbrahimMMOAPI.class, api, this, ServicePriority.Normal);

            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                this.placeholderTeardown = PlaceholderApiHook.register(this, api);
            }
            if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
                this.luckPermsTeardown = LuckPermsHook.register(api);
            }

            GuiServices guiServices = new GuiServices(
                    this, messageService, profileService, progressionService, skillService, abilityService, energyService,
                    classService, prestigeService, questService, collectionService, journalService, leaderboardService,
                    titleService, cosmeticService, feedbackService, sidebarService, configService, api, activationService,
                    pluginLogger, onboardingService, onboardingRewarder, homeService, tradeService,
                    wealthService, bossService, modVerificationService, chatPromptService,
                    guideService, backpackService
            );
            // The trade screen lives in the GUI layer, which needs the trade
            // service to build - so the service is told how to draw itself only
            // once both halves exist.
            tradeService.bindRenderer(new TradeScreenBridge(guiServices));

            registerCommands(messageService, sidebarService, guiServices, activationService, homeService, tradeService,
                    feedbackService, backpackService);

            pluginLogger.info("HalilIbrahimMMO " + getPluginMeta().getVersion() + " basariyla etkinlestirildi.");
        } catch (Exception exception) {
            pluginLogger.error(
                    "HalilIbrahimMMO etkinlestirilirken kritik bir hata olustu, eklenti devre disi birakiliyor.",
                    exception
            );
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, ModCheckProtocol.CHANNEL);
        getServer().getMessenger().unregisterIncomingPluginChannel(this, ModCheckProtocol.CHANNEL);

        if (placeholderTeardown != null) {
            placeholderTeardown.run();
            placeholderTeardown = null;
        }
        if (luckPermsTeardown != null) {
            luckPermsTeardown.run();
            luckPermsTeardown = null;
        }

        if (progressionListener != null) {
            progressionListener.close();
            progressionListener = null;
        }
        if (husbandryListener != null) {
            husbandryListener.close();
            husbandryListener = null;
        }
        if (questDiscoveryListener != null) {
            questDiscoveryListener.close();
            questDiscoveryListener = null;
        }
        if (wealthRankAnnouncer != null) {
            wealthRankAnnouncer.close();
            wealthRankAnnouncer = null;
        }

        for (int i = enabledServices.size() - 1; i >= 0; i--) {
            PluginService service = enabledServices.get(i);
            try {
                service.disable();
            } catch (Exception exception) {
                getSLF4JLogger().error("Servis kapatilirken hata olustu: " + service.getClass().getSimpleName(), exception);
            }
        }

        enabledServices.clear();
        services.clear();
    }

    /**
     * Registers the {@code hmmo:modcheck} channel in both directions and wires
     * the join/quit/payload listener. Both registrations are required: outgoing
     * to send the challenge, incoming to receive the client's mod list.
     */
    private void registerModCheckChannel(ModVerificationService modVerificationService) {
        ModCheckListener listener = new ModCheckListener(modVerificationService);
        getServer().getMessenger().registerOutgoingPluginChannel(this, ModCheckProtocol.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, ModCheckProtocol.CHANNEL, listener);
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private <T extends PluginService> T startService(T service, Class<T> type) {
        service.enable();
        services.register(type, service);
        enabledServices.add(service);
        return service;
    }

    private void runPendingMigrations(DatabaseService databaseService) {
        List<Migration> migrations = List.of(new CoreSchemaMigration(), new PlayerProfileSchemaMigration(), new SkillSchemaMigration(), new ClassSchemaMigration(), new QuestSchemaMigration(), new TitleCosmeticSchemaMigration(), new ClassProgressSchemaMigration(), new PlayerActivationSchemaMigration(), new QuestAssignmentSchemaMigration(), new OnboardingSchemaMigration(), new PrestigeBankedPowerSchemaMigration(), new PlayerHomeSchemaMigration(), new WealthSchemaMigration(), new QuestPinSchemaMigration(), new BackpackSchemaMigration(), new TalentGrantSchemaMigration());
        MigrationRunner migrationRunner = new MigrationRunner(databaseService, pluginLogger, migrations);

        try {
            migrationRunner.runPendingMigrations().get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LifecycleException("Veritabani gocleri calistirilirken islem kesintiye ugradi.", exception);
        } catch (ExecutionException exception) {
            throw new LifecycleException("Veritabani gocleri calistirilamadi.", exception.getCause());
        }
    }

    /**
     * Every gameplay command is now a bare GUI shortcut, so this only needs the
     * three collaborators those shortcuts actually use: the shared
     * {@link GuiServices} bundle, the sidebar toggle behind {@code /panel} and
     * the activation gate behind {@code /mmo}.
     *
     * <p><strong>Player-facing commands are spelled with real Turkish
     * diacritics</strong> ({@code /menü}, {@code /görev}, {@code /sınıf}, ...),
     * deliberately reversing the earlier "command literals stay plain ASCII"
     * rule for this half of the surface: these are typed by players on Turkish
     * keyboards, where the diacritic is the natural spelling and its absence is
     * the thing that needs explaining. Every one of them keeps its old ASCII
     * spelling as an alias, so nothing a player already learned stops working
     * and a non-Turkish keyboard can still reach every screen. The admin
     * command ({@code /haliladmin}) stays ASCII-only - it is typed by staff
     * from a console as often as from a client.</p>
     */
    private void registerCommands(
            MessageService messageService,
            SidebarService sidebarService,
            GuiServices guiServices,
            ActivationService activationService,
            HomeService homeService,
            TradeService tradeService,
            FeedbackService feedbackService,
            BackpackService backpackService
    ) {
        registerCommand("haliladmin", "HalilIbrahimMMO yonetici paneli", List.of(), new AdminCommand(guiServices));
        // Three labels for one screen, because a player looking for help types
        // whichever of the three first occurs to them and none of them is wrong.
        registerCommand("yardım", "Oyun ici rehberi acar", List.of("yardim", "bilgi", "rehber"), new GuideCommand(guiServices));
        registerCommand("çanta", "Bulut sandigini acar", List.of("canta", "backpack"),
                new BackpackCommand(this, backpackService, messageService));
        registerCommand("profil", "MMO profil ekranini acar", List.of(), new ProfileCommand(guiServices));
        registerCommand("kabiliyet", "Kabiliyet menusunu acar", List.of(), new AbilityCommand(guiServices));
        registerCommand("sınıf", "MMO sinif menusunu acar", List.of("sinif"), new ClassCommand(guiServices));
        registerCommand("prestij", "Prestij ekranini acar", List.of(), new PrestigeCommand(guiServices));
        registerCommand("görev", "Gorev ekranini acar", List.of("gorev"), new QuestCommand(guiServices));
        registerCommand("koleksiyon", "Koleksiyon ekranini acar", List.of(), new CollectionCommand(guiServices));
        registerCommand("günlük", "Macera gunlugu ekranini acar", List.of("gunluk"), new JournalCommand(guiServices));
        registerCommand("liderlik", "Liderlik tablolari ekranini acar", List.of(), new LeaderboardCommand(guiServices));
        registerCommand("ünvan", "Unvan ekranini acar", List.of("unvan"), new TitleCommand(guiServices));
        registerCommand("kozmetik", "Kozmetik ekranini acar", List.of(), new CosmeticCommand(guiServices));
        registerCommand("menü", "Ana MMO menusunu acar", List.of("menu"), new MenuCommand(guiServices));
        registerCommand("panel", "Sag taraftaki bilgi ekranini acar/kapatir", List.of(), new PanelCommand(sidebarService, messageService));
        registerCommand("mmo", "MMO sistemini aktiflestirir", List.of(), new MmoCommand(this, activationService, guiServices, messageService));
        registerCommand("madencilik", "Madencilik beceri detayini acar", List.of(), new SkillDetailCommand(guiServices, "madencilik"));
        registerCommand("odunculuk", "Odunculuk beceri detayini acar", List.of(), new SkillDetailCommand(guiServices, "odunculuk"));
        registerCommand("kazıcılık", "Kazicilik beceri detayini acar", List.of("kazicilik"), new SkillDetailCommand(guiServices, "kazicilik"));
        registerCommand("avcılık", "Avcilik beceri detayini acar", List.of("avcilik"), new SkillDetailCommand(guiServices, "avcilik"));
        registerCommand("tarımcılık", "Tarimcilik beceri detayini acar", List.of("tarimcilik"), new SkillDetailCommand(guiServices, "tarimcilik"));
        registerCommand("hayvancılık", "Hayvancilik beceri detayini acar", List.of("hayvancilik"), new SkillDetailCommand(guiServices, "hayvancilik"));
        registerCommand("beceri", "Beceri ekranini acar", List.of("beceriler"), new SkillsCommand(guiServices));
        registerCommand("ev", "Evine isinlanir veya evini ayarlar", List.of("home"), new HomeCommand(this, homeService, messageService, sidebarService, feedbackService));
        registerCommand("takas", "Baska bir oyuncuyla takas baslatir", List.of("trade"), new TradeCommand(tradeService, messageService));
        registerCommand("rtp", "Haritada rastgele bir noktaya isinlanir", List.of("rastgele"), new RandomTeleportCommand(this, messageService, feedbackService));
        registerCommand("harita", "Canli oyunici harita adresini gosterir", List.of("map"), new MapCommand(messageService));
    }

    private boolean isDebugModeCurrentlyEnabled() {
        return services.isRegistered(ConfigService.class) && services.resolve(ConfigService.class).coreConfig().debugMode();
    }
}
