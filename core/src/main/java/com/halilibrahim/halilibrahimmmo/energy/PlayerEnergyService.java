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
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/** Player-local, non-persistent energy state with scheduler-safe regeneration. */
public final class PlayerEnergyService implements EnergyService {

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final Supplier<AbilitySettings> settings;
    private final ToIntFunction<Player> maximumEnergyBonus;
    private final ConcurrentHashMap<UUID, EnergyState> energy = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastShownAt = new ConcurrentHashMap<>();
    private ScheduledTask regenerationTask;

    public PlayerEnergyService(JavaPlugin plugin, MessageService messages, Supplier<AbilitySettings> settings, ToIntFunction<Player> maximumEnergyBonus) {
        this.plugin = plugin;
        this.messages = messages;
        this.settings = settings;
        this.maximumEnergyBonus = maximumEnergyBonus;
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
        show(player, maximum);
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
        show(player, state.current());
        return true;
    }

    @Override
    public int currentEnergy(Player player) {
        return energy.getOrDefault(player.getUniqueId(), new EnergyState(maximumEnergy(player))).current();
    }

    @Override
    public void reconcile(Player player) {
        EnergyState state = energy.get(player.getUniqueId());
        if (state != null && state.clamp(maximumEnergy(player))) show(player, state.current());
    }

    private void regenerate(Player player) {
        int maximum = maximumEnergy(player);
        EnergyState state = energy.computeIfAbsent(player.getUniqueId(), ignored -> new EnergyState(maximum));
        if (state.regenerate(maximum, settings.get().energyRegenerationPerSecond())) {
            show(player, state.current());
        }
    }

    private void show(Player player, int current) {
        long now = System.currentTimeMillis();
        Long previous = lastShownAt.putIfAbsent(player.getUniqueId(), now);
        if (previous != null && now - previous < 1_000L) {
            return;
        }
        lastShownAt.put(player.getUniqueId(), now);
        player.sendActionBar(messages.render(MessageKey.ENERGY_STATUS, Map.of(
                "mevcut", Integer.toString(current),
                "maksimum", Integer.toString(maximumEnergy(player))
        )));
    }

    private int maximumEnergy(Player player) {
        return Math.addExact(settings.get().maximumEnergy(), maximumEnergyBonus.applyAsInt(player));
    }
}
