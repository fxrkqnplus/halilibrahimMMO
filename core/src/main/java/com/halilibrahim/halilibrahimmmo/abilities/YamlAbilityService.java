package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.classes.ClassModifiers;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Function;

/** YAML-backed ability state machine with only bounded, server-safe effect types. */
public final class YamlAbilityService implements AbilityService {

    private static final String FILE_NAME = "abilities.yml";

    private final JavaPlugin plugin;
    private final SkillService skills;
    private final MessageService messages;
    private final Function<UUID, ClassModifiers> classModifiers;
    private final AtomicReference<AbilitySettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<UUID, ArmedAbility> armed = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ArmedAbility> damageBuffs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CooldownKey, Long> cooldowns = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<Block>> secondaryBreaks = ThreadLocal.withInitial(HashSet::new);

    public YamlAbilityService(JavaPlugin plugin, SkillService skills, MessageService messages, Function<UUID, ClassModifiers> classModifiers) {
        this.plugin = plugin;
        this.skills = skills;
        this.messages = messages;
        this.classModifiers = classModifiers;
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void disable() {
        armed.clear();
        damageBuffs.clear();
        cooldowns.clear();
    }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        try {
            settings.set(parse(YamlConfiguration.loadConfiguration(file)));
            armed.clear();
            damageBuffs.clear();
        } catch (RuntimeException exception) {
            throw new LifecycleException("abilities.yml could not be loaded.", exception);
        }
    }

    @Override
    public AbilitySettings settings() {
        AbilitySettings current = settings.get();
        if (current == null) {
            throw new IllegalStateException("AbilityService.enable() has not run yet.");
        }
        return current;
    }

    @Override
    public Collection<AbilityDefinition> definitions() {
        return settings().definitions().values();
    }

    @Override
    public void activate(Player player, String abilityId, EnergyService energy) {
        AbilityDefinition definition = settings().definitions().get(abilityId.toLowerCase(Locale.ROOT));
        if (definition == null) {
            messages.send(player, MessageKey.ABILITY_UNKNOWN, Map.of("kabiliyet", abilityId));
            return;
        }
        skills.load(player.getUniqueId(), definition.skillId()).thenAccept(progress ->
                player.getScheduler().run(plugin, task -> activateOnPlayerRegion(player, definition, progress.level(), energy), null));
    }

    @Override
    public void handleBlockBreak(BlockBreakEvent event) {
        if (isSecondaryBreak(event)) {
            return;
        }
        Player player = event.getPlayer();
        ArmedAbility current = armed.get(player.getUniqueId());
        if (current == null || current.expiresAtMillis() < System.currentTimeMillis()) {
            armed.remove(player.getUniqueId(), current);
            return;
        }
        AbilityDefinition definition = settings().definitions().get(current.abilityId());
        if (definition == null || !isEligibleBlock(definition, event.getBlock())) {
            return;
        }
        armed.remove(player.getUniqueId(), current);
        switch (definition.effectType()) {
            case EXCAVATE -> breakSecondaryBlocks(player, event.getBlock(), excavationArea(event.getBlock(),
                    candidate -> isEligibleBlock(definition, candidate)), current.limit());
            case TREE_FELL, VEIN_MINE -> breakSecondaryBlocks(player, event.getBlock(),
                    connectedBlocks(event.getBlock(), block -> block.getType() == event.getBlock().getType(), current.limit()),
                    current.limit());
            case DAMAGE_BUFF -> throw new IllegalStateException("Damage ability cannot be armed by a block break.");
        }
        messages.send(player, MessageKey.ABILITY_TRIGGERED, Map.of("kabiliyet", definition.displayName()));
    }

    @Override
    public void handleEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof Player) {
            return;
        }
        ArmedAbility active = damageBuffs.get(player.getUniqueId());
        if (active == null) {
            return;
        }
        if (active.expiresAtMillis() < System.currentTimeMillis()) {
            damageBuffs.remove(player.getUniqueId(), active);
            return;
        }
        AbilityDefinition definition = settings().definitions().get(active.abilityId());
        if (definition != null && definition.effectType() == AbilityEffectType.DAMAGE_BUFF) {
            event.setDamage(event.getDamage() * definition.damageMultiplier());
        }
    }

    @Override
    public boolean isSecondaryBreak(BlockBreakEvent event) {
        return secondaryBreaks.get().contains(event.getBlock());
    }

    /**
     * The cooldown check-then-write and the {@link EnergyState} check-then-consume
     * below are only safe because this method is always invoked from
     * {@code player.getScheduler()}, which serializes per-entity - there is no
     * explicit lock guarding {@code cooldowns}/energy here, unlike the
     * per-key serial chains every persistent cached service uses. Calling
     * this from any other thread would reintroduce a double-activation race.
     */
    private void activateOnPlayerRegion(Player player, AbilityDefinition definition, int skillLevel, EnergyService energy) {
        if (skillLevel < definition.requiredLevel()) {
            messages.send(player, MessageKey.ABILITY_LOCKED, Map.of(
                    "kabiliyet", definition.displayName(),
                    "seviye", Integer.toString(definition.requiredLevel())
            ));
            return;
        }
        ClassModifiers modifiers = classModifiers.apply(player.getUniqueId());
        int energyCost = modifiers.adjustedEnergyCost(definition.id(), definition.energyCost());
        long cooldownMillis = modifiers.adjustedCooldownMillis(definition.id(), definition.cooldownMillis());
        int limit = modifiers.adjustedLimit(definition.id(), definition.limit());
        CooldownKey cooldownKey = new CooldownKey(player.getUniqueId(), definition.id());
        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(cooldownKey, 0L);
        if (readyAt > now) {
            messages.send(player, MessageKey.ABILITY_COOLDOWN, Map.of(
                    "kabiliyet", definition.displayName(),
                    "sure", Long.toString(Math.max(1L, (readyAt - now + 999L) / 1000L))
            ));
            return;
        }
        if (energy.currentEnergy(player) < energyCost) {
            messages.send(player, MessageKey.ABILITY_ENERGY, Map.of(
                    "kabiliyet", definition.displayName(),
                    "gereken", Integer.toString(energyCost),
                    "mevcut", Integer.toString(energy.currentEnergy(player))
            ));
            return;
        }
        if (!energy.tryConsume(player, energyCost)) {
            return;
        }
        cooldowns.put(cooldownKey, now + cooldownMillis);
        ArmedAbility active = new ArmedAbility(definition.id(), now + definition.activeMillis(), limit);
        if (definition.effectType() == AbilityEffectType.DAMAGE_BUFF) {
            damageBuffs.put(player.getUniqueId(), active);
            messages.send(player, MessageKey.ABILITY_ACTIVE, Map.of(
                    "kabiliyet", definition.displayName(),
                    "sure", Long.toString(Math.max(1L, definition.activeMillis() / 1000L))
            ));
        } else {
            armed.put(player.getUniqueId(), active);
            messages.send(player, MessageKey.ABILITY_ARMED, Map.of(
                    "kabiliyet", definition.displayName(),
                    "sure", Long.toString(Math.max(1L, definition.activeMillis() / 1000L))
            ));
        }
    }

    private boolean isEligibleBlock(AbilityDefinition definition, Block block) {
        SkillDefinition skill = skills.settings().definitions().get(definition.skillId());
        return skill != null && skill.materials().contains(block.getType());
    }

    private List<Block> excavationArea(Block origin, Predicate<Block> accepted) {
        List<Block> blocks = new ArrayList<>(9);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block candidate = origin.getRelative(x, 0, z);
                if (accepted.test(candidate)) {
                    blocks.add(candidate);
                }
            }
        }
        return blocks;
    }

    private List<Block> connectedBlocks(Block origin, Predicate<Block> accepted, int limit) {
        List<Block> found = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        Deque<Block> pending = new ArrayDeque<>();
        pending.add(origin);
        while (!pending.isEmpty() && found.size() < limit) {
            Block block = pending.removeFirst();
            if (!visited.add(block) || !accepted.test(block)) {
                continue;
            }
            found.add(block);
            pending.add(block.getRelative(1, 0, 0));
            pending.add(block.getRelative(-1, 0, 0));
            pending.add(block.getRelative(0, 1, 0));
            pending.add(block.getRelative(0, -1, 0));
            pending.add(block.getRelative(0, 0, 1));
            pending.add(block.getRelative(0, 0, -1));
        }
        return found;
    }

    private void breakSecondaryBlocks(Player player, Block origin, List<Block> candidates, int limit) {
        int processed = 0;
        for (Block candidate : candidates) {
            if (candidate.equals(origin) || processed++ >= limit || candidate.getType().isAir()) {
                continue;
            }
            Set<Block> marked = secondaryBreaks.get();
            marked.add(candidate);
            try {
                BlockBreakEvent secondary = new BlockBreakEvent(candidate, player);
                Bukkit.getPluginManager().callEvent(secondary);
                if (!secondary.isCancelled()) {
                    candidate.breakNaturally(player.getInventory().getItemInMainHand(), true);
                }
            } finally {
                marked.remove(candidate);
            }
        }
    }

    private AbilitySettings parse(YamlConfiguration yaml) {
        int maximumEnergy = yaml.getInt("enerji.maksimum", 100);
        int regeneration = yaml.getInt("enerji.yenilenme-saniye-basina", 4);
        if (maximumEnergy <= 0 || regeneration < 0) {
            throw new IllegalArgumentException("Energy settings must be non-negative and maximum positive.");
        }
        ConfigurationSection abilities = requireSection(yaml, "kabiliyetler");
        Map<String, AbilityDefinition> definitions = new LinkedHashMap<>();
        for (String rawId : abilities.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+") || definitions.containsKey(id)) {
                throw new IllegalArgumentException("Invalid or duplicate ability id: " + rawId);
            }
            ConfigurationSection section = requireSection(abilities, rawId);
            String displayName = section.getString("gorunen-ad");
            String skillId = section.getString("beceri", "").toLowerCase(Locale.ROOT);
            if (displayName == null || displayName.isBlank() || !skills.settings().definitions().containsKey(skillId)) {
                throw new IllegalArgumentException("Ability has a missing display name or unknown skill: " + id);
            }
            int requiredLevel = section.getInt("gerekli-seviye", 1);
            int energyCost = section.getInt("enerji", 0);
            long cooldownMillis = secondsToMillis(section.getLong("bekleme-suresi-saniye", 0));
            long activeMillis = secondsToMillis(section.getLong("etki-suresi-saniye", 0));
            AbilityEffectType effect = AbilityEffectType.valueOf(section.getString("etki.tur", "").toUpperCase(Locale.ROOT));
            int limit = section.getInt("etki.limit", 1);
            double multiplier = section.getDouble("etki.hasar-carpani", 1.0D);
            if (requiredLevel < 1 || energyCost < 0 || cooldownMillis < 0 || activeMillis <= 0 || limit < 1 || multiplier < 1.0D) {
                throw new IllegalArgumentException("Invalid ability settings: " + id);
            }
            if (effect != AbilityEffectType.DAMAGE_BUFF && limit < 2) {
                throw new IllegalArgumentException("Block abilities require a limit of at least two: " + id);
            }
            definitions.put(id, new AbilityDefinition(id, displayName, skillId, requiredLevel, energyCost,
                    cooldownMillis, activeMillis, effect, limit, multiplier));
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("abilities.yml must define at least one ability.");
        }
        return new AbilitySettings(maximumEnergy, regeneration, Map.copyOf(definitions));
    }

    private ConfigurationSection requireSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing YAML section: " + path);
        }
        return section;
    }

    private long secondsToMillis(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Duration cannot be negative.");
        }
        return Math.multiplyExact(seconds, 1_000L);
    }

    private record ArmedAbility(String abilityId, long expiresAtMillis, int limit) {
    }

    private record CooldownKey(UUID playerId, String abilityId) {
    }
}
