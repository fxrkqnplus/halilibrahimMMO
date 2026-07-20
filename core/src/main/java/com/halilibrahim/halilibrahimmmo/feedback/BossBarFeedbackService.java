package com.halilibrahim.halilibrahimmmo.feedback;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Adventure {@link BossBar}/{@link Title}-backed implementation of {@link FeedbackService}. */
public final class BossBarFeedbackService implements FeedbackService {

    private static final String FILE_NAME = "feedback.yml";
    private static final Duration TITLE_FADE_IN = Duration.ofMillis(500);
    private static final Duration TITLE_FADE_OUT = Duration.ofMillis(500);
    private static final long TICKS_PER_SECOND = 20L;

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final AtomicReference<FeedbackSettings> settings = new AtomicReference<>();

    public BossBarFeedbackService(JavaPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        try {
            long seconds = YamlConfiguration.loadConfiguration(file).getLong("sure-saniye", 5);
            if (seconds < 1) {
                throw new IllegalArgumentException("Feedback duration must be positive.");
            }
            settings.set(new FeedbackSettings(seconds * 1000L));
        } catch (RuntimeException exception) {
            throw new LifecycleException("feedback.yml could not be loaded.", exception);
        }
    }

    @Override
    public FeedbackSettings settings() {
        FeedbackSettings value = settings.get();
        if (value == null) {
            throw new IllegalStateException("FeedbackService.enable() has not run yet.");
        }
        return value;
    }

    @Override
    public void celebrateLevelUp(Player player, int newLevel) {
        show(player, MessageKey.FEEDBACK_LEVEL_UP_BOSSBAR, MessageKey.FEEDBACK_LEVEL_UP_TITLE, MessageKey.FEEDBACK_LEVEL_UP_SUBTITLE,
                Map.of("seviye", Integer.toString(newLevel)), BossBar.Color.GREEN);
    }

    @Override
    public void celebrateQuestCompleted(Player player, String questDisplayName) {
        show(player, MessageKey.FEEDBACK_QUEST_BOSSBAR, MessageKey.FEEDBACK_QUEST_TITLE, MessageKey.FEEDBACK_QUEST_SUBTITLE,
                Map.of("gorev", questDisplayName), BossBar.Color.BLUE);
    }

    @Override
    public void celebrateCollectionCompleted(Player player, String collectionDisplayName) {
        show(player, MessageKey.FEEDBACK_COLLECTION_BOSSBAR, MessageKey.FEEDBACK_COLLECTION_TITLE, MessageKey.FEEDBACK_COLLECTION_SUBTITLE,
                Map.of("koleksiyon", collectionDisplayName), BossBar.Color.PURPLE);
    }

    @Override
    public void celebratePrestige(Player player, int newPrestige) {
        show(player, MessageKey.FEEDBACK_PRESTIGE_BOSSBAR, MessageKey.FEEDBACK_PRESTIGE_TITLE, MessageKey.FEEDBACK_PRESTIGE_SUBTITLE,
                Map.of("prestij", Integer.toString(newPrestige)), BossBar.Color.YELLOW);
    }

    private void show(
            Player player, MessageKey bossBarKey, MessageKey titleKey, MessageKey subtitleKey,
            Map<String, String> placeholders, BossBar.Color color
    ) {
        BossBar bar = BossBar.bossBar(messages.render(bossBarKey, placeholders), 1f, color, BossBar.Overlay.PROGRESS);
        player.showBossBar(bar);
        long durationTicks = settings().durationMillis() / 1000L * TICKS_PER_SECOND;
        player.getScheduler().runDelayed(plugin, task -> player.hideBossBar(bar), null, durationTicks);

        Component title = messages.render(titleKey, placeholders);
        Component subtitle = messages.render(subtitleKey, placeholders);
        Duration stay = Duration.ofMillis(settings().durationMillis());
        player.showTitle(Title.title(title, subtitle, Title.Times.times(TITLE_FADE_IN, stay, TITLE_FADE_OUT)));
    }
}
