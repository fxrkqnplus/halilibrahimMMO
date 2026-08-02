package com.halilibrahim.halilibrahimmmo.reputation;

import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;

/**
 * Everything that happens <em>after</em> Saygınlık Puanı lands: the sidebar
 * refresh, and - only when the award crossed a milestone - the personal cue and
 * the server-wide announcement.
 *
 * <p>Extracted for the same reason {@code SkillAwardHandler} and
 * {@code ClassAwardHandler} were. Reputation is awarded from four independent
 * places (block breaks, kills, quest completions, world bosses) and a milestone
 * has to look identical whichever one produced it - otherwise crossing a
 * million points quietly because it happened to come from a boss rather than an
 * ore would read as a bug.</p>
 */
public final class ReputationAwardHandler {

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final FeedbackService feedback;
    private final SidebarService sidebar;

    public ReputationAwardHandler(JavaPlugin plugin, MessageService messages, FeedbackService feedback, SidebarService sidebar) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.sidebar = sidebar;
    }

    /**
     * Safe to call from any thread and with an empty award: the database future
     * that produced it resolved wherever it liked, and the overwhelming majority
     * of block breaks are worth nothing at all.
     */
    public void handle(Player player, Optional<ReputationAward> award) {
        if (award.isEmpty()) {
            return;
        }
        ReputationAward result = award.get();
        player.getScheduler().run(plugin, task -> {
            sidebar.refresh(player);
            if (!result.reachedMilestone()) {
                return;
            }
            feedback.celebrateReputationMilestone(player, result.newTotal());
            messages.broadcast(MessageKey.REPUTATION_MILESTONE_BROADCAST, Map.of(
                    "oyuncu", player.getName(),
                    "esik", Long.toString(result.crossedMilestone())
            ));
        }, null);
    }
}
