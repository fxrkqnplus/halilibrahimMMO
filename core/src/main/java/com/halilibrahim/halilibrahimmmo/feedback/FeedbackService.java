package com.halilibrahim.halilibrahimmmo.feedback;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;

/**
 * Universal (not equip-based) bossbar/on-screen-title polish for the four
 * moments every player already gets a chat message for. Every method is
 * synchronous - callers must already be on the player's own region
 * scheduler, exactly like the {@code messages.send(...)} calls they sit
 * alongside.
 */
public interface FeedbackService extends PluginService {

    FeedbackSettings settings();

    void celebrateLevelUp(Player player, int newLevel);

    void celebrateQuestCompleted(Player player, String questDisplayName);

    void celebrateCollectionCompleted(Player player, String collectionDisplayName);

    void celebratePrestige(Player player, int newPrestige);
}
