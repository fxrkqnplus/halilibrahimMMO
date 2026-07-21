package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.classes.ClassModifiers;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Function;

/** YAML-backed ability state machine with only bounded, server-safe effect types. */
public final class YamlAbilityService implements AbilityService {

    private static final String FILE_NAME = "abilities.yml";

    private final JavaPlugin plugin;
    private final SkillService skills;
    private final MessageService messages;
    private final ProfileService profiles;
    private final FeedbackService feedback;
    private final Function<UUID, ClassModifiers> classModifiers;
    private final AtomicReference<AbilitySettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<UUID, ArmedAbility> armed = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ArmedAbility> damageBuffs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ArmedAbility> dropBuffs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ArmedAbility> activeEffects = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CooldownKey, Long> cooldowns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> selectedAbility = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ScheduledTask> activationFeedbackTasks = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<Block>> secondaryBreaks = ThreadLocal.withInitial(HashSet::new);

    public YamlAbilityService(
            JavaPlugin plugin, SkillService skills, MessageService messages, ProfileService profiles,
            FeedbackService feedback, Function<UUID, ClassModifiers> classModifiers
    ) {
        this.plugin = plugin;
        this.skills = skills;
        this.messages = messages;
        this.profiles = profiles;
        this.feedback = feedback;
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
        dropBuffs.clear();
        activeEffects.clear();
        cooldowns.clear();
        selectedAbility.clear();
        activationFeedbackTasks.values().forEach(ScheduledTask::cancel);
        activationFeedbackTasks.clear();
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
            dropBuffs.clear();
            activeEffects.clear();
            selectedAbility.clear();
            activationFeedbackTasks.values().forEach(ScheduledTask::cancel);
            activationFeedbackTasks.clear();
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
        String selectedClass = profiles.cached(player.getUniqueId()).map(profile -> profile.selectedClass()).orElse("");
        if (!definition.classId().equals(selectedClass)) {
            messages.send(player, MessageKey.ABILITY_WRONG_CLASS, Map.of("kabiliyet", definition.displayName()));
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
        applyDropBuff(player, event);
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
        clearActivationFeedback(player);
        switch (definition.effectType()) {
            case EXCAVATE -> breakSecondaryBlocks(player, event.getBlock(), excavationArea(event.getBlock(),
                    candidate -> isEligibleBlock(definition, candidate)), current.limit());
            case TREE_FELL, VEIN_MINE -> breakSecondaryBlocks(player, event.getBlock(),
                    connectedBlocks(event.getBlock(), block -> block.getType() == event.getBlock().getType(), current.limit()),
                    current.limit());
            default -> throw new IllegalStateException("Ability effect cannot be armed by a block break: " + definition.effectType());
        }
        messages.send(player, MessageKey.ABILITY_TRIGGERED, Map.of("kabiliyet", definition.displayName()));
    }

    private void applyDropBuff(Player player, BlockBreakEvent event) {
        ArmedAbility buff = dropBuffs.get(player.getUniqueId());
        if (buff == null) {
            return;
        }
        if (buff.expiresAtMillis() < System.currentTimeMillis()) {
            dropBuffs.remove(player.getUniqueId(), buff);
            return;
        }
        AbilityDefinition definition = settings().definitions().get(buff.abilityId());
        if (definition == null || definition.effectType() != AbilityEffectType.EXTRA_DROP_CHANCE) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= definition.damageMultiplier()) {
            return;
        }
        Block block = event.getBlock();
        for (ItemStack drop : block.getDrops(player.getInventory().getItemInMainHand())) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }
    }

    @Override
    public void handleEntityDamage(EntityDamageByEntityEvent event, EnergyService energy) {
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
        if (definition == null || definition.effectType() != AbilityEffectType.DAMAGE_BUFF) {
            return;
        }
        event.setDamage(event.getDamage() * definition.damageMultiplier());
        if (definition.mode() != AbilityActivationMode.ENERJI) {
            return;
        }
        if (!energy.tryConsume(player, definition.energyPerHit())) {
            damageBuffs.remove(player.getUniqueId(), active);
            clearActivationFeedback(player);
        }
    }

    @Override
    public boolean isSecondaryBreak(BlockBreakEvent event) {
        return secondaryBreaks.get().contains(event.getBlock());
    }

    @Override
    public void select(Player player, String abilityId) {
        AbilityDefinition definition = settings().definitions().get(abilityId.toLowerCase(Locale.ROOT));
        if (definition == null) {
            messages.send(player, MessageKey.ABILITY_UNKNOWN, Map.of("kabiliyet", abilityId));
            return;
        }
        selectedAbility.put(player.getUniqueId(), definition.id());
        messages.send(player, MessageKey.ABILITY_SELECTED, Map.of("kabiliyet", definition.displayName()));
    }

    @Override
    public void clearSelection(UUID playerId) {
        selectedAbility.remove(playerId);
    }

    @Override
    public Optional<String> selected(UUID playerId) {
        return Optional.ofNullable(selectedAbility.get(playerId));
    }

    @Override
    public Optional<ActiveAbilityStatus> activeStatus(UUID playerId) {
        long now = System.currentTimeMillis();
        ArmedAbility armedState = armed.get(playerId);
        if (armedState != null && armedState.expiresAtMillis() > now) {
            return Optional.of(new ActiveAbilityStatus(armedState.abilityId(), armedState.expiresAtMillis()));
        }
        ArmedAbility buffState = damageBuffs.get(playerId);
        if (buffState != null && buffState.expiresAtMillis() > now) {
            return Optional.of(new ActiveAbilityStatus(buffState.abilityId(), buffState.expiresAtMillis()));
        }
        ArmedAbility dropState = dropBuffs.get(playerId);
        if (dropState != null && dropState.expiresAtMillis() > now) {
            return Optional.of(new ActiveAbilityStatus(dropState.abilityId(), dropState.expiresAtMillis()));
        }
        ArmedAbility instantState = activeEffects.get(playerId);
        if (instantState != null && instantState.expiresAtMillis() > now) {
            return Optional.of(new ActiveAbilityStatus(instantState.abilityId(), instantState.expiresAtMillis()));
        }
        return Optional.empty();
    }

    @Override
    public void clearActivationFeedback(Player player) {
        boolean wasActive = activationFeedbackTasks.containsKey(player.getUniqueId());
        cancelFeedbackTask(player.getUniqueId());
        applyGlint(player, null);
        if (wasActive) {
            feedback.celebrateAbilityEnded(player);
        }
    }

    /**
     * Starts the per-second action-bar countdown and applies a purely cosmetic
     * glint to the player's held item for as long as {@code definition} stays
     * armed/buffed. Only ever called from {@link #activateOnPlayerRegion}, which
     * already runs on the player's own region scheduler - see the class-level
     * note on why that thread confinement matters here.
     */
    private void startActivationFeedback(Player player, AbilityDefinition definition, long expiresAtMillis, EnergyService energy) {
        applyGlint(player, Boolean.TRUE);
        cancelFeedbackTask(player.getUniqueId());
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, self -> {
            long remainingMillis = expiresAtMillis - System.currentTimeMillis();
            if (remainingMillis <= 0 || !isStillActive(player.getUniqueId(), definition.id())) {
                clearActivationFeedback(player);
                return;
            }
            if (definition.mode() == AbilityActivationMode.ENERJI) {
                player.sendActionBar(messages.render(MessageKey.ABILITY_COUNTDOWN_ACTIONBAR_ENERGY, Map.of(
                        "kabiliyet", definition.displayName(),
                        "enerji", Integer.toString(energy.currentEnergy(player))
                )));
                return;
            }
            long remainingSeconds = Math.max(1L, (remainingMillis + 999L) / 1000L);
            player.sendActionBar(messages.render(MessageKey.ABILITY_COUNTDOWN_ACTIONBAR, Map.of(
                    "kabiliyet", definition.displayName(),
                    "kalan", Long.toString(remainingSeconds)
            )));
        }, null, 1L, 20L);
        activationFeedbackTasks.put(player.getUniqueId(), task);
    }

    private boolean isStillActive(UUID playerId, String abilityId) {
        long now = System.currentTimeMillis();
        ArmedAbility armedState = armed.get(playerId);
        if (armedState != null && armedState.abilityId().equals(abilityId) && armedState.expiresAtMillis() > now) {
            return true;
        }
        ArmedAbility buffState = damageBuffs.get(playerId);
        if (buffState != null && buffState.abilityId().equals(abilityId) && buffState.expiresAtMillis() > now) {
            return true;
        }
        ArmedAbility dropState = dropBuffs.get(playerId);
        if (dropState != null && dropState.abilityId().equals(abilityId) && dropState.expiresAtMillis() > now) {
            return true;
        }
        ArmedAbility instantState = activeEffects.get(playerId);
        return instantState != null && instantState.abilityId().equals(abilityId) && instantState.expiresAtMillis() > now;
    }

    private void cancelFeedbackTask(UUID playerId) {
        ScheduledTask task = activationFeedbackTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    /** {@code glowing == null} clears any override instead of forcing glint off. */
    private void applyGlint(Player player, Boolean glowing) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(glowing);
        item.setItemMeta(meta);
        player.getInventory().setItemInMainHand(item);
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
        switch (definition.effectType()) {
            case EXCAVATE, TREE_FELL, VEIN_MINE -> {
                armed.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ARMED, Map.of(
                        "kabiliyet", definition.displayName(),
                        "sure", Long.toString(Math.max(1L, definition.activeMillis() / 1000L))
                ));
            }
            case DAMAGE_BUFF -> {
                damageBuffs.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ACTIVE, Map.of(
                        "kabiliyet", definition.displayName(),
                        "sure", Long.toString(Math.max(1L, definition.activeMillis() / 1000L))
                ));
            }
            case EXTRA_DROP_CHANCE -> {
                dropBuffs.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ACTIVE_DROP, Map.of(
                        "kabiliyet", definition.displayName(),
                        "sure", Long.toString(Math.max(1L, definition.activeMillis() / 1000L))
                ));
            }
            default -> {
                applyInstantEffect(player, definition);
                activeEffects.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ACTIVE_GENERIC, Map.of(
                        "kabiliyet", definition.displayName(),
                        "sure", Long.toString(Math.max(1L, definition.activeMillis() / 1000L))
                ));
            }
        }
        feedback.celebrateAbilityActivated(player);
        startActivationFeedback(player, definition, active.expiresAtMillis(), energy);
    }

    private void applyInstantEffect(Player player, AbilityDefinition definition) {
        int durationTicks = (int) Math.max(1L, definition.activeMillis() / 50L);
        switch (definition.effectType()) {
            case BRIEF_INVULNERABILITY -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 4));
            case AREA_HEAL -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 1));
            case SPEED_BURST -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 1));
            case NIGHT_VISION -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, durationTicks, 0));
            case KNOCKBACK_SLAM -> knockbackSlam(player, definition);
            default -> throw new IllegalStateException("Not an instant ability effect: " + definition.effectType());
        }
    }

    private void knockbackSlam(Player player, AbilityDefinition definition) {
        double radius = definition.limit();
        for (Entity nearby : player.getNearbyEntities(radius, radius, radius)) {
            if (!(nearby instanceof LivingEntity target) || target instanceof Player) {
                continue;
            }
            Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector());
            if (push.lengthSquared() == 0) {
                push = player.getLocation().getDirection();
            }
            target.setVelocity(push.normalize().multiply(1.4D).setY(0.4D));
            target.damage(definition.damageMultiplier(), player);
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
            String description = section.getString("aciklama");
            String skillId = section.getString("beceri", "").toLowerCase(Locale.ROOT);
            if (displayName == null || displayName.isBlank() || description == null || description.isBlank()
                    || !skills.settings().definitions().containsKey(skillId)) {
                throw new IllegalArgumentException("Ability has a missing display name/description or unknown skill: " + id);
            }
            String classId = section.getString("sinif", "").toLowerCase(Locale.ROOT);
            if (classId.isBlank()) {
                throw new IllegalArgumentException("Ability must declare its owning class: " + id);
            }
            int requiredLevel = section.getInt("gerekli-seviye", 1);
            int energyCost = section.getInt("enerji", 0);
            long cooldownMillis = secondsToMillis(section.getLong("bekleme-suresi-saniye", 0));
            long activeMillis = secondsToMillis(section.getLong("etki-suresi-saniye", 0));
            AbilityEffectType effect = AbilityEffectType.valueOf(section.getString("etki.tur", "").toUpperCase(Locale.ROOT));
            int limit = section.getInt("etki.limit", 1);
            double multiplier = section.getDouble("etki.hasar-carpani", 1.0D);
            double chance = section.getDouble("etki.sans", -1.0D);
            int radius = section.getInt("etki.yaricap", -1);
            AbilityToolTag tool = AbilityToolTag.valueOf(section.getString("arac.tur", "").toUpperCase(Locale.ROOT));
            AbilityActivationMode mode = AbilityActivationMode.valueOf(section.getString("sure-modu", "SURE").toUpperCase(Locale.ROOT));
            int energyPerHit = section.getInt("enerji-vurus-basi", 0);
            if (requiredLevel < 1 || energyCost < 0 || cooldownMillis < 0 || activeMillis <= 0) {
                throw new IllegalArgumentException("Invalid ability settings: " + id);
            }
            if (mode == AbilityActivationMode.ENERJI && (effect != AbilityEffectType.DAMAGE_BUFF || energyPerHit <= 0)) {
                throw new IllegalArgumentException("ENERJI mode requires a DAMAGE_BUFF effect and a positive enerji-vurus-basi: " + id);
            }
            switch (effect) {
                case EXCAVATE, TREE_FELL, VEIN_MINE -> {
                    if (limit < 2) throw new IllegalArgumentException("Block abilities require a limit of at least two: " + id);
                }
                case DAMAGE_BUFF -> {
                    if (multiplier < 1.0D) throw new IllegalArgumentException("Damage buff multiplier must be at least 1.0: " + id);
                }
                case EXTRA_DROP_CHANCE -> {
                    if (chance <= 0 || chance > 1.0D) throw new IllegalArgumentException("Extra-drop chance must be in (0, 1]: " + id);
                    multiplier = chance;
                }
                case KNOCKBACK_SLAM -> {
                    if (radius < 1) throw new IllegalArgumentException("Knockback ability requires a positive radius: " + id);
                    if (multiplier < 0) throw new IllegalArgumentException("Knockback damage must not be negative: " + id);
                    limit = radius;
                }
                case BRIEF_INVULNERABILITY, AREA_HEAL, SPEED_BURST, NIGHT_VISION -> {
                    // No extra numeric fields required beyond duration/energy/cooldown.
                }
            }
            definitions.put(id, new AbilityDefinition(id, displayName, description, skillId, requiredLevel, energyCost,
                    cooldownMillis, activeMillis, effect, limit, multiplier, tool, classId, mode, energyPerHit));
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
