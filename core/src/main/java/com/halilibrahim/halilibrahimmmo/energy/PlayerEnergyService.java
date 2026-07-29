package com.halilibrahim.halilibrahimmmo.energy;

import com.halilibrahim.halilibrahimmmo.abilities.AbilitySettings;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/** Player-local, non-persistent energy state with scheduler-safe regeneration. */
public final class PlayerEnergyService implements EnergyService {

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final Supplier<AbilitySettings> settings;
    private final ToIntFunction<Player> maximumEnergyBonus;
    private final Predicate<Player> abilityRunning;
    private final ConcurrentHashMap<UUID, EnergyState> energy = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastShownAt = new ConcurrentHashMap<>();
    private ScheduledTask regenerationTask;

    /**
     * @param abilityRunning returns {@code true} while this player has an
     *                       ability armed or active. Two things hang off it, and
     *                       they are deliberately the same condition rather than
     *                       two switches: the energy status line yields to the
     *                       ability's own action-bar countdown instead of the two
     *                       flickering on one line, and <strong>regeneration
     *                       stops entirely</strong>. Energy is a budget an ability
     *                       spends, so refilling it mid-ability would mean an
     *                       ability that drains per block could never actually run
     *                       out - which is the whole thing the drain exists to
     *                       decide.
     */
    public PlayerEnergyService(
            JavaPlugin plugin, MessageService messages, Supplier<AbilitySettings> settings,
            ToIntFunction<Player> maximumEnergyBonus, Predicate<Player> abilityRunning
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.settings = settings;
        this.maximumEnergyBonus = maximumEnergyBonus;
        this.abilityRunning = abilityRunning;
    }

    @Override
    public void enable() {
        regenerationTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task ->
                Bukkit.getOnlinePlayers().forEach(player -> player.getScheduler().run(plugin, ignored -> regenerate(player), null)),
                20L, 20L);
    }

    @Override
    public void disable() {
        if (regenerationTask != null) {
            regenerationTask.cancel();
            regenerationTask = null;
        }
        energy.clear();
        lastShownAt.clear();
    }

    @Override
    public void initialize(Player player) {
        int maximum = maximumEnergy(player);
        energy.put(player.getUniqueId(), new EnergyState(maximum));
        lastShownAt.remove(player.getUniqueId());
        show(player, maximum, false);
    }

    @Override
    public void remove(Player player) {
        energy.remove(player.getUniqueId());
        lastShownAt.remove(player.getUniqueId());
    }

    @Override
    public boolean tryConsume(Player player, int amount) {
        UUID playerId = player.getUniqueId();
        int maximum = maximumEnergy(player);
        EnergyState state = energy.computeIfAbsent(playerId, ignored -> new EnergyState(maximum));
        if (!state.tryConsume(amount)) {
            return false;
        }
        show(player, state.current(), false);
        return true;
    }

    @Override
    public int currentEnergy(Player player) {
        return energy.getOrDefault(player.getUniqueId(), new EnergyState(maximumEnergy(player))).current();
    }

    @Override
    public void reconcile(Player player) {
        EnergyState state = energy.get(player.getUniqueId());
        if (state != null && state.clamp(maximumEnergy(player))) show(player, state.current(), false);
    }

    private void regenerate(Player player) {
        if (abilityRunning.test(player)) {
            return;
        }
        int maximum = maximumEnergy(player);
        EnergyState state = energy.computeIfAbsent(player.getUniqueId(), ignored -> new EnergyState(maximum));
        if (state.regenerate(maximum, settings.get().energyRegenerationPerSecond())) {
            // A refill only reads as a refill while it is still happening. Once
            // the bar is full the ordinary status line is the honest one - and
            // it is the moment the player is waiting for, so it must be
            // distinguishable at a glance from the ticks before it.
            show(player, state.current(), state.current() < maximum);
        }
    }

    /**
     * @param regenerating whether to use the "filling up" line instead of the
     *                     plain status one. Since an ability stops regeneration
     *                     entirely, this line is the only place a player can
     *                     watch the bar come back after one ends - which is
     *                     precisely the wait that decides when they can go
     *                     again.
     */
    private void show(Player player, int current, boolean regenerating) {
        if (abilityRunning.test(player)) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = lastShownAt.putIfAbsent(player.getUniqueId(), now);
        if (previous != null && now - previous < 1_000L) {
            return;
        }
        lastShownAt.put(player.getUniqueId(), now);
        if (regenerating) {
            player.sendActionBar(messages.render(MessageKey.ENERGY_REGENERATING, Map.of(
                    "mevcut", Integer.toString(current),
                    "maksimum", Integer.toString(maximumEnergy(player)),
                    "hiz", Integer.toString(settings.get().energyRegenerationPerSecond())
            )));
            return;
        }
        player.sendActionBar(messages.render(MessageKey.ENERGY_STATUS, Map.of(
                "mevcut", Integer.toString(current),
                "maksimum", Integer.toString(maximumEnergy(player))
        )));
    }

    @Override
    public int maximumEnergy(Player player) {
        return Math.addExact(settings.get().maximumEnergy(), maximumEnergyBonus.applyAsInt(player));
    }
}
