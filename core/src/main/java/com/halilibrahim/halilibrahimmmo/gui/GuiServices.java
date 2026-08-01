package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI;
import com.halilibrahim.halilibrahimmmo.boss.BossService;
import com.halilibrahim.halilibrahimmmo.reputation.ReputationService;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.collections.CollectionService;
import com.halilibrahim.halilibrahimmmo.config.ConfigService;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.backpack.BackpackService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.guide.GuideService;
import com.halilibrahim.halilibrahimmmo.journal.JournalService;
import com.halilibrahim.halilibrahimmmo.leaderboards.LeaderboardService;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.home.HomeService;
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import com.halilibrahim.halilibrahimmmo.modcheck.ModVerificationService;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingRewarder;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingService;
import com.halilibrahim.halilibrahimmmo.trade.TradeService;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeRewardService;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import com.halilibrahim.halilibrahimmmo.quests.QuestService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Every service a GUI view might need to render itself or link to another
 * view, bundled into one constructor parameter instead of a dozen. All 10 menus
 * are cross-linked from the main hub, so each one plausibly needs any service.
 */
public record GuiServices(
        JavaPlugin plugin,
        MessageService messages,
        ProfileService profiles,
        ProgressionService progression,
        SkillService skills,
        AbilityService abilities,
        EnergyService energy,
        ClassService classes,
        PrestigeService prestige,
        PrestigeRewardService prestigeRewards,
        QuestService quests,
        CollectionService collections,
        JournalService journal,
        LeaderboardService leaderboards,
        TitleService titles,
        CosmeticService cosmetics,
        FeedbackService feedback,
        SidebarService sidebar,
        ConfigService config,
        HalilIbrahimMMOAPI api,
        ActivationService activation,
        PluginLogger logger,
        OnboardingService onboarding,
        OnboardingRewarder onboardingRewards,
        HomeService homes,
        TradeService trades,
        ReputationService reputation,
        BossService bosses,
        ModVerificationService modVerification,
        ChatPromptService chatPrompts,
        GuideService guide,
        BackpackService backpacks
) {
}
