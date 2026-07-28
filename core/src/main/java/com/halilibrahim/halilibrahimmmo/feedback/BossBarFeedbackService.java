package com.halilibrahim.halilibrahimmmo.feedback;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final AtomicReference<Map<String, SoundCue>> sounds = new AtomicReference<>();
    private final ConcurrentHashMap<ExperienceBarKey, BossBar> experienceBars = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ExperienceBarKey, ScheduledTask> experienceBarHideTasks = new ConcurrentHashMap<>();

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
        experienceBars.clear();
        experienceBarHideTasks.clear();
    }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            long seconds = yaml.getLong("sure-saniye", 5);
            long experienceBarSeconds = yaml.getLong("xp-cubugu-sure-saniye", 3);
            if (seconds < 1 || experienceBarSeconds < 1) {
                throw new IllegalArgumentException("Feedback durations must be positive.");
            }
            settings.set(new FeedbackSettings(seconds * 1000L, experienceBarSeconds * 1000L));
            sounds.set(parseSounds(yaml));
        } catch (RuntimeException exception) {
            throw new LifecycleException("feedback.yml could not be loaded.", exception);
        }
    }

    private Map<String, SoundCue> parseSounds(YamlConfiguration yaml) {
        ConfigurationSection section = yaml.getConfigurationSection("sesler");
        if (section == null) {
            throw new IllegalArgumentException("Missing YAML section: sesler");
        }
        Map<String, SoundCue> parsed = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection cue = section.getConfigurationSection(key);
            if (cue == null) {
                throw new IllegalArgumentException("Missing sound entry: " + key);
            }
            Sound sound = Sound.valueOf(cue.getString("ses", "").toUpperCase(Locale.ROOT));
            float volume = (float) cue.getDouble("ses-seviyesi", 1.0);
            float pitch = (float) cue.getDouble("ton", 1.0);
            if (volume <= 0 || pitch <= 0) {
                throw new IllegalArgumentException("Sound volume/pitch must be positive: " + key);
            }
            parsed.put(key, new SoundCue(sound, volume, pitch));
        }
        return Map.copyOf(parsed);
    }

    private void playSound(Player player, String key) {
        SoundCue cue = sounds.get().get(key);
        if (cue != null) {
            player.playSound(player.getLocation(), cue.sound(), cue.volume(), cue.pitch());
        }
    }

    @Override
    public void celebrateClassSelected(Player player) {
        playSound(player, "sinif-secildi");
    }

    @Override
    public void celebrateClassChanged(Player player, String classDisplayName) {
        Map<String, String> placeholders = Map.of("sinif", classDisplayName);
        show(player, MessageKey.FEEDBACK_CLASS_CHANGED_BOSSBAR, MessageKey.FEEDBACK_CLASS_CHANGED_TITLE,
                MessageKey.FEEDBACK_CLASS_CHANGED_SUBTITLE, placeholders, BossBar.Color.PURPLE);
        messages.send(player, MessageKey.FEEDBACK_CLASS_CHANGED_CHAT, placeholders);
        playSound(player, "sinif-degistirildi");
    }

    @Override
    public void celebrateAbilitySelected(Player player) {
        playSound(player, "kabiliyet-secildi");
    }

    @Override
    public void celebrateAbilityActivated(Player player) {
        playSound(player, "kabiliyet-baslangic");
    }

    @Override
    public void celebrateAbilityEnded(Player player) {
        playSound(player, "kabiliyet-bitis");
    }

    private record SoundCue(Sound sound, float volume, float pitch) {
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
    public void showExperienceProgress(
            Player player, MessageKey template, Map<String, String> placeholders,
            long currentExperience, long requiredExperience, String category, BossBar.Color color
    ) {
        Map<String, String> merged = new HashMap<>(placeholders);
        merged.put("kazanilan", merged.getOrDefault("kazanilan", "0"));
        merged.put("mevcut", Long.toString(currentExperience));
        merged.put("gereken", Long.toString(requiredExperience));

        float progress = requiredExperience <= 0
                ? 1f
                : Math.max(0f, Math.min(1f, (float) ((double) currentExperience / (double) requiredExperience)));

        Component text = messages.render(template, merged);
        ExperienceBarKey key = new ExperienceBarKey(player.getUniqueId(), category);
        BossBar bar = experienceBars.computeIfAbsent(key, ignored -> {
            BossBar created = BossBar.bossBar(text, progress, color, BossBar.Overlay.PROGRESS);
            player.showBossBar(created);
            return created;
        });
        bar.name(text);
        bar.progress(progress);
        bar.color(color);

        ScheduledTask previous = experienceBarHideTasks.remove(key);
        if (previous != null) {
            previous.cancel();
        }
        long durationTicks = settings().experienceBarDurationMillis() / 1000L * TICKS_PER_SECOND;
        ScheduledTask hideTask = player.getScheduler().runDelayed(plugin, task -> {
            BossBar removed = experienceBars.remove(key);
            if (removed != null) {
                player.hideBossBar(removed);
            }
            experienceBarHideTasks.remove(key);
        }, null, durationTicks);
        experienceBarHideTasks.put(key, hideTask);
    }

    @Override
    public void clearExperienceProgress(Player player) {
        UUID playerId = player.getUniqueId();
        experienceBars.keySet().removeIf(key -> key.playerId().equals(playerId));
        experienceBarHideTasks.keySet().removeIf(key -> {
            if (!key.playerId().equals(playerId)) {
                return false;
            }
            ScheduledTask task = experienceBarHideTasks.get(key);
            if (task != null) {
                task.cancel();
            }
            return true;
        });
    }

    private record ExperienceBarKey(UUID playerId, String category) {
    }

    @Override
    public void celebrateLevelUp(Player player, int newLevel) {
        messages.send(player, MessageKey.FEEDBACK_LEVEL_UP_CHAT, Map.of("seviye", Integer.toString(newLevel)));
        playSound(player, "seviye-atlama");
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

    @Override
    public void celebrateAbilityUnlocked(Player player, String abilityDisplayName) {
        show(player, MessageKey.FEEDBACK_ABILITY_UNLOCKED_BOSSBAR, MessageKey.FEEDBACK_ABILITY_UNLOCKED_TITLE, MessageKey.FEEDBACK_ABILITY_UNLOCKED_SUBTITLE,
                Map.of("kabiliyet", abilityDisplayName), BossBar.Color.WHITE);
        playSound(player, "kabiliyet-acildi");
    }

    @Override
    public void celebrateClassLevelUp(Player player, String classDisplayName, int newLevel) {
        messages.send(player, MessageKey.FEEDBACK_CLASS_LEVEL_UP_CHAT, Map.of(
                "sinif", classDisplayName, "seviye", Integer.toString(newLevel)
        ));
        playSound(player, "sinif-seviye-atlama");
    }

    @Override
    public void celebrateSkillLevelUp(Player player, String skillDisplayName, int newLevel) {
        messages.send(player, MessageKey.FEEDBACK_SKILL_LEVEL_UP_CHAT, Map.of(
                "beceri", skillDisplayName, "seviye", Integer.toString(newLevel)
        ));
        playSound(player, "beceri-seviye-atlama");
    }

    @Override
    public void celebrateGucMilestone(Player player, int newGuc) {
        show(player, MessageKey.FEEDBACK_GUC_BOSSBAR, MessageKey.FEEDBACK_GUC_TITLE, MessageKey.FEEDBACK_GUC_SUBTITLE,
                Map.of("guc", Integer.toString(newGuc)), BossBar.Color.GREEN);
    }

    @Override
    public void celebrateBossSpawned(Player player) {
        playSound(player, "boss-belirdi");
    }

    @Override
    public void celebrateBossDefeated(Player player) {
        playSound(player, "boss-olduruldu");
    }

    @Override
    public void celebrateWealthMilestone(Player player, long newTotal) {
        messages.send(player, MessageKey.WEALTH_MILESTONE_PERSONAL, Map.of("zenginlik", Long.toString(newTotal)));
        playSound(player, "zenginlik-kilometre-tasi");
    }

    @Override
    public void celebrateRandomTeleport(Player player, String x, String y, String z) {
        show(player, MessageKey.FEEDBACK_RANDOM_TELEPORT_BOSSBAR, MessageKey.FEEDBACK_RANDOM_TELEPORT_TITLE,
                MessageKey.FEEDBACK_RANDOM_TELEPORT_SUBTITLE, Map.of("x", x, "y", y, "z", z), BossBar.Color.GREEN);
        playSound(player, "rastgele-isinlanma");
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
