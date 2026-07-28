package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.classes.ClassAwardHandler;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.journal.JournalService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import com.halilibrahim.halilibrahimmmo.skills.SkillAwardHandler;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import com.halilibrahim.halilibrahimmmo.wealth.WealthAwardHandler;
import com.halilibrahim.halilibrahimmmo.wealth.WealthService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/**
 * Everything a finished quest pays out, in one place: the personal chat line
 * and completion fanfare, the server-wide announcement for a daily/weekly, the
 * XP itself, the journal entry and the title/cosmetic unlock re-evaluation.
 *
 * <p>Extracted because quests are now completed from two independent listeners
 * (block breaks and mob kills on one side, structure discoveries and item
 * pickups on the other) and both have to reward identically down to the sound
 * cue.</p>
 *
 * <h2>Where the XP goes</h2>
 * <p>A quest's reward is paid into the <strong>skill it trains</strong> - the
 * {@code beceri} it declares in {@code quests.yml} - through the ordinary
 * {@code SkillService} award path. That is what makes hunting quests advance
 * avcılık and digging quests advance kazıcılık, and it is also what makes the
 * class track advance: the skill award notifies the class-synergy bridge, which
 * mirrors it into the player's class whenever that class draws synergy from
 * that skill. So an Amansız Savaşçı finishing a zombie hunt gets both avcılık
 * and Amansız Savaşçı XP out of a single award, with no double-crediting.</p>
 *
 * <p>When the quest's skill is <em>not</em> one the player's class synergises
 * with, the bridge stays silent, and the class is credited directly here
 * instead - so no quest is ever worth zero class XP just because it happens to
 * be off-speciality. Either way the credit goes through
 * {@link ClassAwardHandler}, which is why a quest level-up now produces exactly
 * the same chat message, sound and bossbar as any other level-up; it used to
 * assemble a partial copy of that feedback locally.</p>
 */
public final class QuestCompletionHandler {

    private final JavaPlugin plugin;
    private final QuestService quests;
    private final ProfileService profiles;
    private final ClassService classes;
    private final SkillService skills;
    private final TitleService titles;
    private final CosmeticService cosmetics;
    private final FeedbackService feedback;
    private final MessageService messages;
    private final JournalService journal;
    private final SidebarService sidebar;
    private final SkillAwardHandler skillAwards;
    private final ClassAwardHandler classAwards;
    private final WealthService wealth;
    private final WealthAwardHandler wealthAwards;

    public QuestCompletionHandler(
            JavaPlugin plugin, QuestService quests, ProfileService profiles, ClassService classes, SkillService skills,
            TitleService titles, CosmeticService cosmetics, FeedbackService feedback, MessageService messages,
            JournalService journal, SidebarService sidebar, SkillAwardHandler skillAwards, ClassAwardHandler classAwards,
            WealthService wealth, WealthAwardHandler wealthAwards
    ) {
        this.plugin = plugin;
        this.quests = quests;
        this.profiles = profiles;
        this.classes = classes;
        this.skills = skills;
        this.titles = titles;
        this.cosmetics = cosmetics;
        this.feedback = feedback;
        this.messages = messages;
        this.journal = journal;
        this.sidebar = sidebar;
        this.skillAwards = skillAwards;
        this.classAwards = classAwards;
        this.wealth = wealth;
        this.wealthAwards = wealthAwards;
    }

    public void notifyCompleted(Player player, List<QuestProgress> completed) {
        completed.forEach(progress -> {
            QuestDefinition definition = quests.settings().definitions().get(progress.questId());
            if (definition == null) {
                return;
            }
            player.getScheduler().run(plugin, task -> {
                messages.send(player, MessageKey.QUEST_COMPLETED, Map.of(
                        "gorev", definition.displayName(),
                        "xp", Long.toString(definition.rewardExperience())
                ));
                feedback.celebrateQuestCompleted(player, definition.displayName());
                sidebar.refresh(player);
            }, null);
            if (definition.type() == QuestType.DAILY || definition.type() == QuestType.WEEKLY) {
                messages.broadcast(MessageKey.QUEST_COMPLETION_BROADCAST, Map.of(
                        "oyuncu", player.getName(), "gorev", definition.displayName()
                ));
            }
            awardExperience(player, definition);
            // Discovering somewhere valuable pays Zenginlik Puanı here rather
            // than from a "you entered a structure" hook, because a quest
            // completes exactly once and a structure can be walked into all day.
            wealth.awardQuest(player.getUniqueId(), definition.id())
                    .thenAccept(award -> wealthAwards.handle(player, award));
            journal.recordQuestCompleted(player.getUniqueId(), definition.id(), definition.displayName(), definition.rewardExperience());
            titles.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyTitles(player, unlocked));
            cosmetics.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyCosmetics(player, unlocked));
        });
    }

    private void awardExperience(Player player, QuestDefinition definition) {
        skills.awardExternal(player.getUniqueId(), player.getName(), definition.skillId(),
                        definition.rewardExperience(), definition.displayName())
                .thenAccept(result -> skillAwards.handle(player, result));

        String classId = profiles.cached(player.getUniqueId()).map(profile -> profile.selectedClass()).orElse("");
        if (classId.isEmpty() || synergises(classId, definition.skillId())) {
            return;
        }
        classes.awardClassExperience(player.getUniqueId(), player.getName(), classId, definition.rewardExperience())
                .thenAccept(result -> classAwards.handle(player, classId, definition.rewardExperience(), result));
    }

    /** True when the synergy bridge will already mirror this skill's award into the class track. */
    private boolean synergises(String classId, String skillId) {
        PlayerClassDefinition definition = classes.settings().definitions().get(classId);
        return definition != null && definition.hasSynergyWith(skillId);
    }

    private void notifyTitles(Player player, List<TitleDefinition> unlocked) {
        if (unlocked.isEmpty()) {
            return;
        }
        player.getScheduler().run(plugin, task -> unlocked.forEach(definition ->
                messages.send(player, MessageKey.TITLE_UNLOCKED, Map.of("unvan", definition.displayName()))), null);
    }

    private void notifyCosmetics(Player player, List<CosmeticDefinition> unlocked) {
        if (unlocked.isEmpty()) {
            return;
        }
        player.getScheduler().run(plugin, task -> unlocked.forEach(definition ->
                messages.send(player, MessageKey.COSMETIC_UNLOCKED, Map.of("kozmetik", definition.displayName()))), null);
    }
}
