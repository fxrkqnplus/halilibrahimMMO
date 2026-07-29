package com.halilibrahim.halilibrahimmmo.server;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;

/**
 * Brings morning as soon as a single player has slept through the night.
 *
 * <p><strong>{@link PlayerDeepSleepEvent} is the right hook, not
 * {@code PlayerBedEnterEvent}.</strong> Paper fires it once a player has
 * actually stayed in bed long enough for vanilla to consider the night slept -
 * exactly the moment the game would already have skipped the night if enough
 * players were in bed. Hooking bed entry instead would have skipped the night
 * the instant somebody touched a bed, including the click that immediately
 * follows setting a spawn point.</p>
 *
 * <p>The vanilla {@code playersSleepingPercentage} gamerule is deliberately
 * <em>not</em> touched. A gamerule write is a permanent change to the world
 * itself that outlives the plugin being removed; doing the skip here keeps the
 * behaviour something {@code sunucu.yml} can switch off again.</p>
 */
public final class SleepListener implements Listener {

    /** Ticks past dawn to land on - vanilla's own wake-up time, not literal 0. */
    private static final long MORNING_TICKS = 1_000L;

    private final ServerInfoService serverInfo;
    private final MessageService messages;

    public SleepListener(ServerInfoService serverInfo, MessageService messages) {
        this.serverInfo = serverInfo;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeepSleep(PlayerDeepSleepEvent event) {
        ServerSettings settings = serverInfo.settings();
        if (!settings.singleSleeperSkipsNight()) {
            return;
        }
        Player sleeper = event.getPlayer();
        World world = sleeper.getWorld();
        // Only the overworld has a night to skip; the Nether and the End have no
        // day cycle a bed could advance, and a bed there explodes anyway.
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        long dayStart = world.getFullTime() - world.getFullTime() % 24_000L;
        world.setFullTime(dayStart + 24_000L + MORNING_TICKS);
        if (settings.sleepClearsWeather()) {
            world.setStorm(false);
            world.setThundering(false);
        }
        for (Player online : world.getPlayers()) {
            online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6F, 1.4F);
            if (settings.announceSleep()) {
                messages.send(online, MessageKey.SLEEP_NIGHT_SKIPPED, Map.of("oyuncu", sleeper.getName()));
            }
        }
    }
}
