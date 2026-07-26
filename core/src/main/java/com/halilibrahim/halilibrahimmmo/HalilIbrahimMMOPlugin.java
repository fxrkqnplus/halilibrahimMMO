package com.halilibrahim.halilibrahimmmo;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.activation.ActivationRepository;
import com.halilibrahim.halilibrahimmmo.activation.CachedActivationService;
import com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI;
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
import com.halilibrahim.halilibrahimmmo.command.SkillDetailCommand;
import com.halilibrahim.halilibrahimmmo.command.TitleCommand;
import com.halilibrahim.halilibrahimmmo.classes.ClassListener;
import com.halilibrahim.halilibrahimmmo.classes.ClassModifiers;
import com.halilibrahim.halilibrahimmmo.classes.ClassRepository;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.classes.ClassSettings;
import com.halilibrahim.halilibrahimmmo.classes.ClassSynergyBridge;
import com.halilibrahim.halilibrahimmmo.classes.YamlClassService;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityActivationListener;
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
import com.halilibrahim.halilibrahimmmo.database.migration.TitleCosmeticSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.PrestigeBankedPowerSchemaMigration;
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
import com.halilibrahim.halilibrahimmmo.gui.GuiListener;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
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
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import com.halilibrahim.halilibrahimmmo.onboarding.CachedOnboardingService;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingRepository;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingService;
import com.halilibrahim.halilibrahimmmo.profile.CachedProfileService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileRepository;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionListener;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import com.halilibrahim.halilibrahimmmo.progression.YamlProgressionService;
import com.halilibrahim.halilibrahimmmo.quests.CachedQuestService;
import com.halilibrahim.halilibrahimmmo.quests.QuestListener;
import com.halilibrahim.halilibrahimmmo.quests.QuestRepository;
import com.halilibrahim.halilibrahimmmo.quests.QuestService;
import com.halilibrahim.halilibrahimmmo.scoreboard.ScoreboardSidebarService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarListener;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import com.halilibrahim.halilibrahimmmo.skills.CachedSkillService;
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
import com.halilibrahim.halilibrahimmmo.unlock.UnlockContextLoader;
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
            FeedbackService feedbackService = startService(
                    new BossBarFeedbackService(this, messageService), FeedbackService.class
            );
            AtomicReference<ClassService> classServiceReference = new AtomicReference<>();
            AtomicReference<AbilityService> abilityServiceReference = new AtomicReference<>();
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
                                double synergyMultiplier = definition != null && definition.synergySkillId().equals(skillId)
                                        ? 1.0 + classSettings.synergyExperienceBonus()
                                        : 1.0;
                                return synergyMultiplier * prestigeMultiplier;
                            }).orElse(1.0),
                            new ClassSynergyBridge(this, profileService, classServiceReference, abilityServiceReference, feedbackService)
                    ), SkillService.class
            );
            ProgressionService progressionService = startService(
                    new YamlProgressionService(skillService, bankedPowerService), ProgressionService.class
            );
            AbilityService abilityService = startService(
                    new YamlAbilityService(this, skillService, messageService, profileService, feedbackService, placedBlockTracker, playerId -> {
                        ClassService classService = classServiceReference.get();
                        return classService == null ? ClassModifiers.none() : classService.modifiers(playerId);
                    }, (playerId, classId) -> {
                        ClassService classService = classServiceReference.get();
                        return classService == null ? 1 : classService.classProgress(playerId, classId).level();
                    }), AbilityService.class
            );
            abilityServiceReference.set(abilityService);
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
                    new CachedQuestService(this, new QuestRepository(databaseService)), QuestService.class
            );
            CollectionService collectionService = startService(
                    new CachedCollectionService(this, new CollectionRepository(databaseService)), CollectionService.class
            );
            LeaderboardService leaderboardService = startService(
                    new CachedLeaderboardService(this, new LeaderboardRepository(databaseService, skillService)), LeaderboardService.class
            );
            UnlockContextLoader unlockContextLoader = new UnlockContextLoader(profileService, skillService, questService, collectionService);
            TitleService titleService = startService(
                    new CachedTitleService(this, new TitleRepository(databaseService), unlockContextLoader), TitleService.class
            );
            CosmeticService cosmeticService = startService(
                    new CachedCosmeticService(this, new CosmeticRepository(databaseService), unlockContextLoader), CosmeticService.class
            );
            SidebarService sidebarService = startService(
                    new ScoreboardSidebarService(this, messageService, profileService, classService, progressionService,
                            activationService, abilityService, questService, titleService, onboardingService), SidebarService.class
            );
            BlockBreakRateLimiter breakRateLimiter = new BlockBreakRateLimiter();
            getServer().getPluginManager().registerEvents(placedBlockTracker, this);
            this.progressionListener = new ProgressionListener(profileService, feedbackService, breakRateLimiter, activationService, bankedPowerService);
            getServer().getPluginManager().registerEvents(progressionListener, this);
            getServer().getPluginManager().registerEvents(new SkillListener(this, skillService, abilityService, energyService, feedbackService, activationService, breakRateLimiter, placedBlockTracker, progressionService, messageService), this);
            getServer().getPluginManager().registerEvents(new ClassListener(this, classService, abilityService, energyService, skillService), this);
            getServer().getPluginManager().registerEvents(new AbilityActivationListener(abilityService, energyService), this);
            getServer().getPluginManager().registerEvents(new SidebarListener(this, sidebarService, profileService, classService, activationService, onboardingService), this);
            getServer().getPluginManager().registerEvents(
                    new QuestListener(this, questService, abilityService, profileService, classService, titleService, cosmeticService, feedbackService, messageService, journalService, breakRateLimiter, placedBlockTracker, sidebarService, activationService), this
            );
            getServer().getPluginManager().registerEvents(
                    new CollectionListener(this, collectionService, profileService, classService, abilityService, titleService, cosmeticService, feedbackService, messageService, journalService, sidebarService, activationService), this
            );
            getServer().getPluginManager().registerEvents(new TitleListener(titleService), this);
            getServer().getPluginManager().registerEvents(new TitleChatListener(titleService), this);
            getServer().getPluginManager().registerEvents(new CosmeticListener(this, cosmeticService), this);
            getServer().getPluginManager().registerEvents(new GuiListener(), this);

            this.api = new HalilIbrahimMMOApiImpl(
                    this, configService, messageService, pluginLogger, profileService, progressionService, skillService,
                    abilityService, classService, prestigeService, questService, collectionService, leaderboardService,
                    titleService, cosmeticService, feedbackService
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
                    pluginLogger, onboardingService
            );

            registerCommands(messageService, sidebarService, guiServices, activationService);

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

    private <T extends PluginService> T startService(T service, Class<T> type) {
        service.enable();
        services.register(type, service);
        enabledServices.add(service);
        return service;
    }

    private void runPendingMigrations(DatabaseService databaseService) {
        List<Migration> migrations = List.of(new CoreSchemaMigration(), new PlayerProfileSchemaMigration(), new SkillSchemaMigration(), new ClassSchemaMigration(), new QuestSchemaMigration(), new TitleCosmeticSchemaMigration(), new ClassProgressSchemaMigration(), new PlayerActivationSchemaMigration(), new QuestAssignmentSchemaMigration(), new OnboardingSchemaMigration(), new PrestigeBankedPowerSchemaMigration());
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
     */
    private void registerCommands(
            MessageService messageService,
            SidebarService sidebarService,
            GuiServices guiServices,
            ActivationService activationService
    ) {
        registerCommand("haliladmin", "HalilIbrahimMMO yonetici paneli", List.of(), new AdminCommand(guiServices));
        registerCommand("profil", "MMO profil ekranini acar", List.of(), new ProfileCommand(guiServices));
        registerCommand("kabiliyet", "Kabiliyet menusunu acar", List.of(), new AbilityCommand(guiServices));
        registerCommand("sinif", "MMO sinif menusunu acar", List.of(), new ClassCommand(guiServices));
        registerCommand("prestij", "Prestij ekranini acar", List.of(), new PrestigeCommand(guiServices));
        registerCommand("gorev", "Gorev ekranini acar", List.of(), new QuestCommand(guiServices));
        registerCommand("koleksiyon", "Koleksiyon ekranini acar", List.of(), new CollectionCommand(guiServices));
        registerCommand("gunluk", "Macera gunlugu ekranini acar", List.of(), new JournalCommand(guiServices));
        registerCommand("liderlik", "Liderlik tablolari ekranini acar", List.of(), new LeaderboardCommand(guiServices));
        registerCommand("unvan", "Unvan ekranini acar", List.of(), new TitleCommand(guiServices));
        registerCommand("kozmetik", "Kozmetik ekranini acar", List.of(), new CosmeticCommand(guiServices));
        registerCommand("menu", "Ana MMO menusunu acar", List.of(), new MenuCommand(guiServices));
        registerCommand("panel", "Sag taraftaki bilgi ekranini acar/kapatir", List.of(), new PanelCommand(sidebarService, messageService));
        registerCommand("mmo", "MMO sistemini aktiflestirir", List.of(), new MmoCommand(this, activationService, guiServices, messageService));
        registerCommand("madencilik", "Madencilik beceri detayini acar", List.of(), new SkillDetailCommand(guiServices, "madencilik"));
        registerCommand("odunculuk", "Odunculuk beceri detayini acar", List.of(), new SkillDetailCommand(guiServices, "odunculuk"));
        registerCommand("kazicilik", "Kazicilik beceri detayini acar", List.of(), new SkillDetailCommand(guiServices, "kazicilik"));
        registerCommand("avcilik", "Avcilik beceri detayini acar", List.of(), new SkillDetailCommand(guiServices, "avcilik"));
    }

    private boolean isDebugModeCurrentlyEnabled() {
        return services.isRegistered(ConfigService.class) && services.resolve(ConfigService.class).coreConfig().debugMode();
    }
}
