package com.halilibrahim.halilibrahimmmo;

import com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI;
import com.halilibrahim.halilibrahimmmo.command.RpgCommand;
import com.halilibrahim.halilibrahimmmo.command.ProfileCommand;
import com.halilibrahim.halilibrahimmmo.command.AbilityCommand;
import com.halilibrahim.halilibrahimmmo.command.ClassCommand;
import com.halilibrahim.halilibrahimmmo.command.CollectionCommand;
import com.halilibrahim.halilibrahimmmo.command.CosmeticCommand;
import com.halilibrahim.halilibrahimmmo.command.JournalCommand;
import com.halilibrahim.halilibrahimmmo.command.LeaderboardCommand;
import com.halilibrahim.halilibrahimmmo.command.MenuCommand;
import com.halilibrahim.halilibrahimmmo.command.PrestigeCommand;
import com.halilibrahim.halilibrahimmmo.command.QuestCommand;
import com.halilibrahim.halilibrahimmmo.command.TitleCommand;
import com.halilibrahim.halilibrahimmmo.classes.ClassListener;
import com.halilibrahim.halilibrahimmmo.classes.ClassModifiers;
import com.halilibrahim.halilibrahimmmo.classes.ClassRepository;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.classes.YamlClassService;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.abilities.YamlAbilityService;
import com.halilibrahim.halilibrahimmmo.collections.CachedCollectionService;
import com.halilibrahim.halilibrahimmmo.collections.CollectionListener;
import com.halilibrahim.halilibrahimmmo.collections.CollectionRepository;
import com.halilibrahim.halilibrahimmmo.collections.CollectionService;
import com.halilibrahim.halilibrahimmmo.command.SubCommand;
import com.halilibrahim.halilibrahimmmo.command.subcommand.DebugToggleSubCommand;
import com.halilibrahim.halilibrahimmmo.command.subcommand.ReloadSubCommand;
import com.halilibrahim.halilibrahimmmo.command.subcommand.VersionSubCommand;
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
import com.halilibrahim.halilibrahimmmo.database.migration.TitleCosmeticSchemaMigration;
import com.halilibrahim.halilibrahimmmo.cosmetics.CachedCosmeticService;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticListener;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticRepository;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.exploit.BlockBreakRateLimiter;
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
import com.halilibrahim.halilibrahimmmo.skills.CachedSkillService;
import com.halilibrahim.halilibrahimmmo.skills.SkillListener;
import com.halilibrahim.halilibrahimmmo.skills.SkillRepository;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
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
 * registering the {@code /rpg} command. No gameplay or business logic lives here -
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
            ProgressionService progressionService = startService(
                    new YamlProgressionService(this, profileService), ProgressionService.class
            );
            SkillService skillService = startService(
                    new CachedSkillService(this, new SkillRepository(databaseService)), SkillService.class
            );
            AtomicReference<ClassService> classServiceReference = new AtomicReference<>();
            AbilityService abilityService = startService(
                    new YamlAbilityService(this, skillService, messageService, playerId -> {
                        ClassService classService = classServiceReference.get();
                        return classService == null ? ClassModifiers.none() : classService.modifiers(playerId);
                    }), AbilityService.class
            );
            ClassService classService = startService(
                    new YamlClassService(this, profileService, new ClassRepository(databaseService), abilityService), ClassService.class
            );
            classServiceReference.set(classService);
            EnergyService energyService = startService(
                    new PlayerEnergyService(this, messageService, abilityService::settings,
                            player -> classService.modifiers(player.getUniqueId()).maximumEnergyBonus()), EnergyService.class
            );
            PrestigeService prestigeService = startService(
                    new YamlPrestigeService(this, profileService, progressionService), PrestigeService.class
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
                    new CachedLeaderboardService(this, new LeaderboardRepository(databaseService)), LeaderboardService.class
            );
            UnlockContextLoader unlockContextLoader = new UnlockContextLoader(profileService, skillService, questService, collectionService);
            TitleService titleService = startService(
                    new CachedTitleService(this, new TitleRepository(databaseService), unlockContextLoader), TitleService.class
            );
            CosmeticService cosmeticService = startService(
                    new CachedCosmeticService(this, new CosmeticRepository(databaseService), unlockContextLoader), CosmeticService.class
            );
            FeedbackService feedbackService = startService(
                    new BossBarFeedbackService(this, messageService), FeedbackService.class
            );
            BlockBreakRateLimiter breakRateLimiter = new BlockBreakRateLimiter();
            this.progressionListener = new ProgressionListener(
                    this, profileService, progressionService, abilityService, titleService, cosmeticService, feedbackService, messageService, breakRateLimiter
            );
            getServer().getPluginManager().registerEvents(progressionListener, this);
            getServer().getPluginManager().registerEvents(new SkillListener(skillService, abilityService, energyService, breakRateLimiter), this);
            getServer().getPluginManager().registerEvents(new ClassListener(this, classService), this);
            getServer().getPluginManager().registerEvents(
                    new QuestListener(this, questService, abilityService, progressionService, titleService, cosmeticService, feedbackService, messageService, journalService, breakRateLimiter), this
            );
            getServer().getPluginManager().registerEvents(
                    new CollectionListener(this, collectionService, progressionService, titleService, cosmeticService, feedbackService, messageService, journalService), this
            );
            getServer().getPluginManager().registerEvents(new TitleListener(titleService), this);
            getServer().getPluginManager().registerEvents(new TitleChatListener(titleService, messageService), this);
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
                    titleService, cosmeticService, feedbackService
            );

            registerCommands(configService, messageService, profileService, progressionService, abilityService, classService,
                    energyService, prestigeService, questService, collectionService, journalService, leaderboardService, skillService,
                    titleService, cosmeticService, feedbackService, guiServices);

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
        List<Migration> migrations = List.of(new CoreSchemaMigration(), new PlayerProfileSchemaMigration(), new SkillSchemaMigration(), new ClassSchemaMigration(), new QuestSchemaMigration(), new TitleCosmeticSchemaMigration());
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

    private void registerCommands(
            ConfigService configService,
            MessageService messageService,
            ProfileService profileService,
            ProgressionService progressionService,
            AbilityService abilityService,
            ClassService classService,
            EnergyService energyService,
            PrestigeService prestigeService,
            QuestService questService,
            CollectionService collectionService,
            JournalService journalService,
            LeaderboardService leaderboardService,
            SkillService skillService,
            TitleService titleService,
            CosmeticService cosmeticService,
            FeedbackService feedbackService,
            GuiServices guiServices
    ) {
        List<SubCommand> subCommands = List.of(
                new ReloadSubCommand(api, messageService),
                new VersionSubCommand(api, messageService),
                new DebugToggleSubCommand(configService, messageService)
        );

        registerCommand(
                "rpg",
                "HalilIbrahimMMO yonetici ve teshis komutu",
                List.of(),
                new RpgCommand(subCommands, messageService)
        );
        registerCommand("profil", "MMO profilinizi gosterir", List.of(), new ProfileCommand(this, profileService, messageService, classService));
        registerCommand("kabiliyet", "Bir MMO kabiliyetini kullanir", List.of(), new AbilityCommand(abilityService, energyService, messageService));
        registerCommand("sinif", "MMO sinifinizi ve yetenek agacinizi yonetir", List.of(), new ClassCommand(this, classService, profileService, energyService, messageService));
        registerCommand("prestij", "Maksimum seviyede prestij yapar", List.of(), new PrestigeCommand(this, prestigeService, profileService, progressionService, titleService, cosmeticService, feedbackService, messageService));
        registerCommand("gorev", "Gorevlerinizi ve ilerlemenizi gosterir", List.of(), new QuestCommand(this, questService, messageService));
        registerCommand("koleksiyon", "Koleksiyonlarinizi ve ilerlemenizi gosterir", List.of(), new CollectionCommand(this, collectionService, messageService));
        registerCommand("gunluk", "Macera gunluginuzu gosterir", List.of(), new JournalCommand(this, journalService, messageService));
        registerCommand("liderlik", "Liderlik tablolarini gosterir", List.of(), new LeaderboardCommand(this, leaderboardService, skillService, messageService));
        registerCommand("unvan", "Unvanlarinizi gosterir ve kusanir", List.of(), new TitleCommand(this, titleService, messageService));
        registerCommand("kozmetik", "Kozmetiklerinizi gosterir ve kusanir", List.of(), new CosmeticCommand(this, cosmeticService, messageService));
        registerCommand("menu", "Ana MMO menusunu acar", List.of(), new MenuCommand(guiServices));
    }

    private boolean isDebugModeCurrentlyEnabled() {
        return services.isRegistered(ConfigService.class) && services.resolve(ConfigService.class).coreConfig().debugMode();
    }
}
