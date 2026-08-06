package com.halilibrahim.halilibrahimmmo.feedback;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
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
    private final AtomicReference<List<SoundCue>> bossSpawnSounds = new AtomicReference<>(List.of());
    private final ConcurrentHashMap<ExperienceBarKey, BossBar> experienceBars = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ExperienceBarKey, ScheduledTask> experienceBarHideTasks = new ConcurrentHashMap<>();
    private final NamespacedKey harmlessFireworkKey;

    public BossBarFeedbackService(JavaPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.harmlessFireworkKey = new NamespacedKey(plugin, "zararsiz-havai-fisek");
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
            bossSpawnSounds.set(parseSoundList(yaml, "boss-dogus-sesleri"));
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

    /**
     * A list of interchangeable cues rather than a single named one, so an
     * event that fires repeatedly does not sound identical every time. An
     * absent or empty section is not an error - the caller falls back to its
     * single named cue, which is exactly the pre-existing behaviour.
     */
    private List<SoundCue> parseSoundList(YamlConfiguration yaml, String path) {
        List<SoundCue> parsed = new ArrayList<>();
        for (Map<?, ?> raw : yaml.getMapList(path)) {
            Sound sound = Sound.valueOf(String.valueOf(raw.get("ses")).toUpperCase(Locale.ROOT));
            float volume = raw.get("ses-seviyesi") instanceof Number value ? value.floatValue() : 1.0f;
            float pitch = raw.get("ton") instanceof Number value ? value.floatValue() : 1.0f;
            if (volume <= 0 || pitch <= 0) {
                throw new IllegalArgumentException("Sound volume/pitch must be positive: " + path);
            }
            parsed.add(new SoundCue(sound, volume, pitch));
        }
        return List.copyOf(parsed);
    }

    private void playSound(Player player, String key) {
        SoundCue cue = sounds.get().get(key);
        if (cue != null) {
            player.playSound(player.getLocation(), cue.sound(), cue.volume(), cue.pitch());
        }
    }

    private void playRandom(Player player, List<SoundCue> cues, String fallbackKey) {
        if (cues.isEmpty()) {
            playSound(player, fallbackKey);
            return;
        }
        SoundCue cue = cues.get(ThreadLocalRandom.current().nextInt(cues.size()));
        player.playSound(player.getLocation(), cue.sound(), cue.volume(), cue.pitch());
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

    /**
     * Held twice as long as an ordinary celebration, for the same reason the
     * prestige screen is: this happens once per player and it is the sentence
     * that replaces a whole sidebar section as it disappears.
     */
    @Override
    public void celebrateOnboardingCompleted(Player player) {
        Duration stay = Duration.ofMillis(settings().durationMillis() * 2);
        BossBar bar = BossBar.bossBar(messages.render(MessageKey.FEEDBACK_ONBOARDING_COMPLETE_BOSSBAR),
                1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        player.showBossBar(bar);
        player.getScheduler().runDelayed(plugin, task -> player.hideBossBar(bar), null,
                stay.toMillis() / 1000L * TICKS_PER_SECOND);
        player.showTitle(Title.title(
                messages.render(MessageKey.FEEDBACK_ONBOARDING_COMPLETE_TITLE),
                messages.render(MessageKey.FEEDBACK_ONBOARDING_COMPLETE_SUBTITLE),
                Title.Times.times(TITLE_FADE_IN, stay, TITLE_FADE_OUT)
        ));
        playSound(player, "baslangic-tamamlandi");
        sparkle(player, Particle.HAPPY_VILLAGER, 40, 1.0D);
    }

    @Override
    public void celebrateClassChangedByAdmin(Player player, String classDisplayName) {
        Map<String, String> placeholders = Map.of("sinif", classDisplayName);
        show(player, MessageKey.FEEDBACK_CLASS_ADMIN_SET_BOSSBAR, MessageKey.FEEDBACK_CLASS_ADMIN_SET_TITLE,
                MessageKey.FEEDBACK_CLASS_ADMIN_SET_SUBTITLE, placeholders, BossBar.Color.PURPLE);
        messages.send(player, MessageKey.FEEDBACK_CLASS_ADMIN_SET_CHAT, placeholders);
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

    @Override
    public void celebrateAbilityUnequipped(Player player) {
        playSound(player, "kabiliyet-cikarildi");
    }

    @Override
    public void celebrateTalentUnlocked(Player player) {
        playSound(player, "yetenek-acildi");
        sparkle(player, Particle.ENCHANT, 30, 1.0D);
    }

    @Override
    public void celebrateHomeSet(Player player) {
        playSound(player, "ev-ayarlandi");
        sparkle(player, Particle.HAPPY_VILLAGER, 24, 0.8D);
    }

    @Override
    public void celebrateHomeTeleport(Player player) {
        playSound(player, "ev-isinlanma");
        sparkle(player, Particle.PORTAL, 40, 1.0D);
    }

    @Override
    public void celebrateTradeRequestSent(Player player) {
        playSound(player, "takas-istek-gonderildi");
    }

    @Override
    public void celebrateTradeRequestReceived(Player player) {
        playSound(player, "takas-istek-alindi");
        sparkle(player, Particle.END_ROD, 20, 0.6D);
    }

    /**
     * A small local burst around the player. Deliberately spawned in the world
     * rather than sent only to them: these are moments a bystander benefits
     * from noticing (somebody just arrived home, somebody just spent a point),
     * and none of them is frequent enough to become clutter.
     */
    private void sparkle(Player player, Particle particle, int count, double spread) {
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, spread, spread, spread, 0.02D);
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
        playSound(player, "gorev-tamamlandi");
        sparkle(player, Particle.TOTEM_OF_UNDYING, 40, 0.9D);
    }

    @Override
    public void celebrateCollectionCompleted(Player player, String collectionDisplayName) {
        show(player, MessageKey.FEEDBACK_COLLECTION_BOSSBAR, MessageKey.FEEDBACK_COLLECTION_TITLE, MessageKey.FEEDBACK_COLLECTION_SUBTITLE,
                Map.of("koleksiyon", collectionDisplayName), BossBar.Color.PURPLE);
    }

    /**
     * The prestige moment, which is the single biggest thing a player does on
     * this server and now looks like it.
     *
     * <p>Beyond the bossbar and title every celebration gets, this one holds
     * its title on screen for twice as long, spells the new XP bonus out in the
     * subtitle - the reward is a rate, and a rate is invisible unless somebody
     * says the number - and raises a column of totem and end-rod particles the
     * player can see from inside their own screen. All of it is local: the
     * server-wide announcement is a separate broadcast.</p>
     */
    @Override
    public void celebratePrestige(Player player, int newPrestige) {
        Map<String, String> placeholders = Map.of(
                "prestij", Integer.toString(newPrestige),
                "xp", Long.toString(Math.round(PlayerProfile.EXPERIENCE_BONUS_PER_PRESTIGE * newPrestige * 100))
        );
        BossBar bar = BossBar.bossBar(messages.render(MessageKey.FEEDBACK_PRESTIGE_BOSSBAR, placeholders),
                1f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_20);
        player.showBossBar(bar);
        Duration stay = Duration.ofMillis(settings().durationMillis() * 2);
        player.getScheduler().runDelayed(plugin, task -> player.hideBossBar(bar), null,
                stay.toMillis() / 1000L * TICKS_PER_SECOND);
        player.showTitle(Title.title(
                messages.render(MessageKey.FEEDBACK_PRESTIGE_TITLE, placeholders),
                messages.render(MessageKey.FEEDBACK_PRESTIGE_SUBTITLE, placeholders),
                Title.Times.times(TITLE_FADE_IN, stay, TITLE_FADE_OUT)
        ));
        playSound(player, "prestij");
        sparkle(player, Particle.TOTEM_OF_UNDYING, 120, 1.4D);
        sparkle(player, Particle.END_ROD, 60, 1.0D);
    }

    @Override
    public void celebrateWithFirework(Player player) {
        player.getWorld().spawn(player.getLocation(), Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().withColor(Color.YELLOW, Color.PURPLE)
                    .with(FireworkEffect.Type.BURST).trail(true).flicker(true).build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
            // The tag is the whole mechanism: FireworkDamageListener refuses
            // damage from anything carrying it, so a burst in a one-block
            // corridor congratulates the player instead of hurting them.
            firework.getPersistentDataContainer().set(harmlessFireworkKey, PersistentDataType.BYTE, (byte) 1);
        });
    }

    /** The key {@code FireworkDamageListener} reads to recognise one of ours. */
    public NamespacedKey harmlessFireworkKey() {
        return harmlessFireworkKey;
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

    /**
     * A boss spawns every half hour for the whole server's benefit, so this cue
     * is heard more often than any other in the plugin - one fixed sound would
     * turn the most dramatic moment on the map into a notification chime. One
     * of {@code boss-dogus-sesleri}' authored cues is picked at random instead.
     */
    @Override
    public void celebrateBossSpawned(Player player) {
        playRandom(player, bossSpawnSounds.get(), "boss-belirdi");
    }

    @Override
    public void announceBossArrival(Player player, MessageKey titleKey, MessageKey subtitleKey, Map<String, String> placeholders) {
        // Held twice as long as an ordinary celebration: this one carries
        // coordinates a player is meant to read and act on, not a number to
        // glance at.
        Duration stay = Duration.ofMillis(settings().durationMillis() * 2);
        player.showTitle(Title.title(
                messages.render(titleKey, placeholders),
                messages.render(subtitleKey, placeholders),
                Title.Times.times(TITLE_FADE_IN, stay, TITLE_FADE_OUT)
        ));
    }

    @Override
    public void celebrateMysticBossSpawned(Player player) {
        playSound(player, "boss-mistik");
        // Two cues, deliberately simultaneous rather than sequenced. The elder
        // guardian's curse alone is a thin, high sound that reads as a warning
        // ping; laying a deep growl under it in the same instant is what turns
        // the rarest spawn on the server into something a player stops for.
        playSound(player, "boss-mistik-ikincil");
        // ELDER_GUARDIAN is the one particle that draws on the receiving
        // player's screen rather than in the world, so it is spawned through
        // the player and its location is only there to satisfy the signature.
        player.spawnParticle(Particle.ELDER_GUARDIAN, player.getLocation(), 1);
    }

    @Override
    public void celebrateBossDefeated(Player player) {
        playSound(player, "boss-olduruldu");
    }

    @Override
    public void celebrateReputationMilestone(Player player, long newTotal) {
        messages.send(player, MessageKey.REPUTATION_MILESTONE_PERSONAL, Map.of("sayginlik", Long.toString(newTotal)));
        playSound(player, "sayginlik-kilometre-tasi");
    }

    @Override
    public void celebrateRandomTeleport(Player player, String x, String y, String z) {
        show(player, MessageKey.FEEDBACK_RANDOM_TELEPORT_BOSSBAR, MessageKey.FEEDBACK_RANDOM_TELEPORT_TITLE,
                MessageKey.FEEDBACK_RANDOM_TELEPORT_SUBTITLE, Map.of("x", x, "y", y, "z", z), BossBar.Color.GREEN);
        playSound(player, "rastgele-isinlanma");
        sparkle(player, Particle.REVERSE_PORTAL, 60, 1.0D);
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
