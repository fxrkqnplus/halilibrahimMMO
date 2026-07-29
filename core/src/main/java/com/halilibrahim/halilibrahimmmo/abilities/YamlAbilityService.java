package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.classes.ClassModifiers;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.exploit.PlacedBlockTracker;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.DurationFormatter;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import com.halilibrahim.halilibrahimmmo.skills.TreeShape;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
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
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
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
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Function;

/** YAML-backed ability state machine with only bounded, server-safe effect types. */
public final class YamlAbilityService implements AbilityService {

    private static final String FILE_NAME = "abilities.yml";
    /**
     * Hard ceiling on ORE_REVEAL's display entities per activation, independent
     * of the configured radius - raised alongside the 12 to 36 block radius so
     * a genuinely large cave still lights up, while the client is never asked
     * to render an unbounded number of glowing entities at once.
     */
    private static final int MAX_REVEALED_ORES = 200;
    /** Half-edge of the cube ORE_REVEAL's "wide mine" gate samples for open space. */
    private static final int OPEN_SPACE_PROBE_RADIUS = 4;
    /** Hard ceiling on GROWTH_SURGE's crops per activation, independent of the configured radius. */
    private static final int MAX_SURGED_CROPS = 256;
    /** How far an activation particle burst scatters around the caster, in blocks. */
    private static final double BURST_SPREAD = 0.6D;
    /** How many frames every shaped activation animation runs for. */
    private static final int ANIMATION_STEPS = 5;
    /** Ticks between those frames - fast enough to read as one gesture. */
    private static final long ANIMATION_INTERVAL_TICKS = 2L;
    /** How far behind the main cue an ability's echo lands, and how much higher it is pitched. */
    private static final long ECHO_DELAY_TICKS = 4L;
    private static final float ECHO_PITCH_FACTOR = 1.3F;
    /**
     * How long a <em>sustained</em> ability stays up in Creative mode. Long
     * enough to be effectively "until switched off" for any realistic session,
     * but still a real expiry, so nothing can be left armed forever by a player
     * who logs out mid-test. Abilities that resolve the instant they fire
     * ({@link AbilityEffectType#resolvesImmediately()}) keep their configured
     * duration even here - see that method for why.
     */
    private static final long CREATIVE_DURATION_MILLIS = 6L * 60L * 60L * 1_000L;
    /**
     * How long LOVE_CALL leaves an animal in love mode - vanilla's own breeding
     * window, not the ability's duration. See {@code callHerd} for why the two
     * are deliberately independent.
     */
    private static final int LOVE_MODE_TICKS = 600;

    private final JavaPlugin plugin;
    private final SkillService skills;
    private final MessageService messages;
    private final ProfileService profiles;
    private final FeedbackService feedback;
    private final PlacedBlockTracker placedBlocks;
    private final Function<UUID, ClassModifiers> classModifiers;
    private final BiFunction<UUID, String, Integer> classLevelLookup;
    private final AtomicReference<AbilitySettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<UUID, ArmedAbility> armed = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ArmedAbility> damageBuffs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ArmedAbility> dropBuffs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ArmedAbility> activeEffects = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CooldownKey, Long> cooldowns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> selectedAbility = new ConcurrentHashMap<>();
    /**
     * The last ability this player deliberately chose, which survives them
     * unequipping it. {@code selectedAbility} answers "what is equipped now";
     * this answers "what were they last on", which is what the switch cooldown
     * has to compare against.
     */
    private final ConcurrentHashMap<UUID, String> lastSelectedAbility = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastSwitchMillis = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ScheduledTask> activationFeedbackTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> glintedSlots = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<RevealedBlock, RevealedOre> revealedOres = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<Block>> secondaryBreaks = ThreadLocal.withInitial(HashSet::new);

    public YamlAbilityService(
            JavaPlugin plugin, SkillService skills, MessageService messages, ProfileService profiles,
            FeedbackService feedback, PlacedBlockTracker placedBlocks, Function<UUID, ClassModifiers> classModifiers,
            BiFunction<UUID, String, Integer> classLevelLookup
    ) {
        this.plugin = plugin;
        this.skills = skills;
        this.messages = messages;
        this.profiles = profiles;
        this.feedback = feedback;
        this.placedBlocks = placedBlocks;
        this.classModifiers = classModifiers;
        this.classLevelLookup = classLevelLookup;
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
        lastSelectedAbility.clear();
        activationFeedbackTasks.values().forEach(ScheduledTask::cancel);
        activationFeedbackTasks.clear();
        glintedSlots.clear();
        // Removed directly rather than through each entity's own scheduler: no
        // new task may be scheduled during plugin shutdown, and this project
        // targets Paper, where disable runs on the thread owning these entities.
        revealedOres.values().forEach(ore -> ore.display().remove());
        revealedOres.clear();
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
        int classLevel = classLevelLookup.apply(player.getUniqueId(), definition.classId());
        player.getScheduler().run(plugin, task -> activateOnPlayerRegion(player, definition, classLevel, energy), null);
    }

    @Override
    public void handleBlockBreak(BlockBreakEvent event, EnergyService energy) {
        // A block the player put down themselves is not terrain: no ability may
        // fire off it, exactly as it earns no skill/class XP or quest progress.
        clearRevealedOre(event.getBlock());
        if (isSecondaryBreak(event) || placedBlocks.isPlayerPlaced(event.getBlock())) {
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
        // Ağaç Kırıcı stays armed until it finds an actual tree: a village
        // house's timber is a world-generated log with nothing under it just
        // like a trunk is, and chopping a building down is never what the
        // player asked for. Every other effect fires on the block it was
        // pointed at.
        if (definition.effectType() == AbilityEffectType.TREE_FELL && !TreeShape.isNaturalTreeRoot(event.getBlock())) {
            messages.send(player, MessageKey.ABILITY_REQUIRES_TREE, Map.of("kabiliyet", definition.displayName()));
            return;
        }
        armed.remove(player.getUniqueId(), current);
        clearActivationFeedback(player);
        // Creative lifts every budget an ability answers to (§16.5), energy
        // included, so a sweep there is never cut short by an empty bar.
        EnergyDrain drain = isCreative(player)
                ? EnergyDrain.none()
                : new EnergyDrain(energy, definition.energyPerBlock());
        int broken = switch (definition.effectType()) {
            case EXCAVATE -> breakSecondaryBlocks(player, event.getBlock(), excavationArea(event.getBlock(),
                    candidate -> isEligibleBlock(definition, candidate)), current.limit(), drain);
            case BLAST_MINE -> {
                List<Block> cube = cubeArea(event.getBlock(), current.limit(),
                        candidate -> isEligibleBlock(definition, candidate));
                yield breakSecondaryBlocks(player, event.getBlock(), cube, cube.size(), drain);
            }
            case TREE_FELL, VEIN_MINE -> breakSecondaryBlocks(player, event.getBlock(),
                    connectedBlocks(event.getBlock(), block -> block.getType() == event.getBlock().getType(), current.limit()),
                    current.limit(), drain);
            default -> throw new IllegalStateException("Ability effect cannot be armed by a block break: " + definition.effectType());
        };
        messages.send(player, MessageKey.ABILITY_TRIGGERED, Map.of("kabiliyet", definition.displayName()));
        if (drain.exhausted()) {
            messages.send(player, MessageKey.ABILITY_ENERGY_DEPLETED, Map.of(
                    "kabiliyet", definition.displayName(), "sayi", Integer.toString(broken)
            ));
        }
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
        double chance = classModifiers.apply(player.getUniqueId())
                .adjustedEffectValue(definition.id(), definition.damageMultiplier());
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
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
        if (definition == null) {
            return;
        }
        double effectValue = classModifiers.apply(player.getUniqueId())
                .adjustedEffectValue(definition.id(), definition.damageMultiplier());
        switch (definition.effectType()) {
            case DAMAGE_BUFF -> event.setDamage(event.getDamage() * effectValue);
            // Healed from the damage the hit is about to deal, so a bigger swing
            // returns more - and capped at the player's own maximum so it can
            // never overheal. Scheduled on the victim's own region via the
            // attacker's scheduler, since the damage event resolves there.
            case LIFE_STEAL -> healFromDamage(player, event.getDamage() * effectValue);
            default -> {
                return;
            }
        }
        if (definition.mode() != AbilityActivationMode.ENERJI || isCreative(player)) {
            return;
        }
        if (!energy.tryConsume(player, definition.energyPerHit())) {
            damageBuffs.remove(player.getUniqueId(), active);
            clearActivationFeedback(player);
        }
    }

    /** {@link AbilityEffectType#LIFE_STEAL}'s payout, clamped to the attacker's own maximum health. */
    private void healFromDamage(Player player, double amount) {
        if (amount <= 0) {
            return;
        }
        AttributeInstance maximum = player.getAttribute(Attribute.MAX_HEALTH);
        double cap = maximum == null ? 20.0D : maximum.getValue();
        double healed = Math.min(cap, player.getHealth() + amount);
        if (healed <= player.getHealth()) {
            return;
        }
        player.setHealth(healed);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.2D, 0), 3, 0.3D, 0.3D, 0.3D, 0.0D);
    }

    @Override
    public boolean isSecondaryBreak(BlockBreakEvent event) {
        return secondaryBreaks.get().contains(event.getBlock());
    }

    /**
     * Unlike {@code agac-kirici}, which only ever fires on a log the ability's
     * own skill lists, this accepts any block matching the broken one's own
     * type - the caller has already decided the block is a tree's root log.
     */
    @Override
    public void fellTree(BlockBreakEvent event, int limit) {
        Block origin = event.getBlock();
        breakSecondaryBlocks(event.getPlayer(), origin,
                connectedBlocks(origin, block -> block.getType() == origin.getType(), limit), limit, EnergyDrain.none());
    }

    /**
     * Restricted to the skill's own configured materials rather than the broken
     * block's exact type: a shovel sweep through mixed dirt, gravel and sand is
     * one job to the player, and refusing every neighbour that is not literally
     * the same block would make the milestone feel broken on the terrain it
     * exists for.
     */
    @Override
    public void excavate(BlockBreakEvent event, String skillId, int limit) {
        SkillDefinition skill = skills.settings().definitions().get(skillId);
        if (skill == null) {
            return;
        }
        Block origin = event.getBlock();
        breakSecondaryBlocks(event.getPlayer(), origin,
                excavationArea(origin, candidate -> skill.materials().contains(candidate.getType())), limit, EnergyDrain.none());
    }

    @Override
    public void select(Player player, String abilityId) {
        AbilityDefinition definition = settings().definitions().get(abilityId.toLowerCase(Locale.ROOT));
        if (definition == null) {
            messages.send(player, MessageKey.ABILITY_UNKNOWN, Map.of("kabiliyet", abilityId));
            return;
        }
        UUID playerId = player.getUniqueId();
        // Deliberately the *last* ability chosen rather than the currently
        // equipped one: unequipping must not be a free way around the switch
        // cooldown, and it would be if the check could only see an empty slot.
        String previouslySelected = lastSelectedAbility.get(playerId);
        // Creative is for trying things out: making a builder or an admin wait
        // two minutes to look at a different ability serves nobody.
        if (previouslySelected != null && !previouslySelected.equals(definition.id()) && !isCreative(player)) {
            long now = System.currentTimeMillis();
            long readyAt = lastSwitchMillis.getOrDefault(playerId, 0L) + settings().selectionCooldownMillis();
            if (readyAt > now) {
                messages.send(player, MessageKey.ABILITY_SELECTION_COOLDOWN, Map.of(
                        "sure", DurationFormatter.ofMillis(messages, readyAt - now)
                ));
                return;
            }
        }
        selectedAbility.put(playerId, definition.id());
        lastSelectedAbility.put(playerId, definition.id());
        lastSwitchMillis.put(playerId, System.currentTimeMillis());
        messages.send(player, MessageKey.ABILITY_SELECTED, Map.of("kabiliyet", definition.displayName()));
        feedback.celebrateAbilitySelected(player);
    }

    /**
     * The player taking their own ability off, from the ability screen.
     *
     * <p>Everything the selection left running ends with it, exactly as on a
     * class change - but unlike a class change this deliberately keeps the
     * switch-cooldown bookkeeping, so unequipping and re-equipping is not a way
     * around the two-minute wait between different abilities. Per-ability
     * cooldowns are kept for the same reason they are on a class change.</p>
     *
     * @return {@code false} when nothing was equipped, so the caller can say so
     */
    @Override
    public boolean unequip(Player player) {
        UUID playerId = player.getUniqueId();
        if (selectedAbility.remove(playerId) == null) {
            return false;
        }
        ArmedAbility instant = activeEffects.remove(playerId);
        armed.remove(playerId);
        damageBuffs.remove(playerId);
        dropBuffs.remove(playerId);
        clearRevealedOres(playerId);
        if (instant != null) {
            AbilityDefinition definition = settings().definitions().get(instant.abilityId());
            if (definition != null) {
                clearInstantEffect(player, definition);
            }
        }
        clearActivationFeedback(player);
        return true;
    }

    /**
     * Clears the selection <em>and</em> everything it left running. A class
     * change is the only caller, and after one the player no longer owns the
     * ability at all - leaving a buff from the abandoned class ticking away, or
     * a block ability still armed, would let it fire for a class its owner has
     * left. Cooldowns are deliberately kept: they are per (player, ability) and
     * survive so a change cannot be used to wipe one.
     */
    @Override
    public void clearSelection(UUID playerId) {
        selectedAbility.remove(playerId);
        // A class change is a clean slate: the ability they were last on
        // belongs to a class they no longer play, so it must not hold the new
        // class's first choice behind a switch cooldown.
        lastSelectedAbility.remove(playerId);
        lastSwitchMillis.remove(playerId);
        ArmedAbility instant = activeEffects.remove(playerId);
        armed.remove(playerId);
        damageBuffs.remove(playerId);
        dropBuffs.remove(playerId);
        clearRevealedOres(playerId);
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) {
            cancelFeedbackTask(playerId);
            glintedSlots.remove(playerId);
            return;
        }
        // An admin force-set resolves on a database thread; the potion effect,
        // the held item's glint and the action bar may only be touched from the
        // region that owns the player.
        player.getScheduler().run(plugin, task -> {
            if (instant != null) {
                AbilityDefinition definition = settings().definitions().get(instant.abilityId());
                if (definition != null) {
                    clearInstantEffect(player, definition);
                }
            }
            clearActivationFeedback(player);
        }, null);
    }

    @Override
    public Optional<String> selected(UUID playerId) {
        return Optional.ofNullable(selectedAbility.get(playerId));
    }

    @Override
    public long cooldownRemainingMillis(UUID playerId, String abilityId) {
        long readyAt = cooldowns.getOrDefault(new CooldownKey(playerId, abilityId), 0L);
        return Math.max(0L, readyAt - System.currentTimeMillis());
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

    /**
     * Tears down every kind of active state this player could be holding, plus
     * whatever vanilla effect an instant ability applied. Only reachable from
     * Creative mode, where an ability that never expires on its own has to have
     * some way back off - see {@code AbilityActivationListener}.
     */
    @Override
    public void deactivate(Player player) {
        UUID playerId = player.getUniqueId();
        ArmedAbility instant = activeEffects.get(playerId);
        armed.remove(playerId);
        damageBuffs.remove(playerId);
        dropBuffs.remove(playerId);
        activeEffects.remove(playerId);
        clearRevealedOres(playerId);
        if (instant != null) {
            AbilityDefinition definition = settings().definitions().get(instant.abilityId());
            if (definition != null) {
                clearInstantEffect(player, definition);
            }
        }
        clearActivationFeedback(player);
        messages.send(player, MessageKey.ABILITY_DEACTIVATED);
    }

    /** The mirror of {@link #applyInstantEffect}: only the potion-backed effects leave anything to remove. */
    private void clearInstantEffect(Player player, AbilityDefinition definition) {
        switch (definition.effectType()) {
            case BRIEF_INVULNERABILITY -> player.removePotionEffect(PotionEffectType.RESISTANCE);
            case AREA_HEAL -> player.removePotionEffect(PotionEffectType.REGENERATION);
            case SPEED_BURST -> player.removePotionEffect(PotionEffectType.SPEED);
            case NIGHT_VISION -> player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            default -> {
                // Every other effect either already resolved (a one-shot area
                // sweep) or has its own expiry (the ore outlines).
            }
        }
    }

    private static boolean isCreative(Player player) {
        return player.getGameMode() == GameMode.CREATIVE;
    }

    @Override
    public void clearActivationFeedback(Player player) {
        boolean wasActive = activationFeedbackTasks.containsKey(player.getUniqueId());
        cancelFeedbackTask(player.getUniqueId());
        clearGlint(player);
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
        // A second activation while a first is still glinting would otherwise
        // strand the glint override on the earlier slot forever.
        clearGlint(player);
        applyGlint(player, player.getInventory().getHeldItemSlot(), Boolean.TRUE);
        cancelFeedbackTask(player.getUniqueId());
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, self -> {
            long remainingMillis = expiresAtMillis - System.currentTimeMillis();
            if (remainingMillis <= 0 || !isStillActive(player.getUniqueId(), definition.id())) {
                clearActivationFeedback(player);
                return;
            }
            // In Creative there is no remaining time and no energy worth
            // reading - what the player needs to know is how to turn it off.
            if (isCreative(player)) {
                player.sendActionBar(messages.render(MessageKey.ABILITY_COUNTDOWN_ACTIONBAR_CREATIVE, Map.of(
                        "kabiliyet", definition.displayName()
                )));
                return;
            }
            // Energy is part of both variants: while an ability is running the
            // action bar is the only place it can be read, and the plain energy
            // status line is deliberately suppressed for the whole duration.
            if (definition.mode() == AbilityActivationMode.ENERJI) {
                player.sendActionBar(messages.render(MessageKey.ABILITY_COUNTDOWN_ACTIONBAR_ENERGY, Map.of(
                        "kabiliyet", definition.displayName(),
                        "mevcut", Integer.toString(energy.currentEnergy(player)),
                        "maksimum", Integer.toString(energy.maximumEnergy(player))
                )));
                return;
            }
            player.sendActionBar(messages.render(MessageKey.ABILITY_COUNTDOWN_ACTIONBAR, Map.of(
                    "kabiliyet", definition.displayName(),
                    "kalan", DurationFormatter.ofMillis(messages, remainingMillis),
                    "mevcut", Integer.toString(energy.currentEnergy(player)),
                    "maksimum", Integer.toString(energy.maximumEnergy(player))
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

    /**
     * Applies the cosmetic glint to one specific hotbar {@code slot} and
     * remembers it, rather than to whatever happens to be in the main hand at
     * the time it is cleared. A player may freely switch hotbar slots while an
     * ability runs (see {@code AbilityActivationListener}), so "the held item"
     * is not a stable identity across an ability's lifetime - clearing the
     * override on the main hand would have both left the real ability tool
     * glowing forever and stripped an unrelated item's glint.
     *
     * <p>{@code glowing == null} clears any override instead of forcing glint off.</p>
     */
    private void applyGlint(Player player, int slot, Boolean glowing) {
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(glowing);
        item.setItemMeta(meta);
        player.getInventory().setItem(slot, item);
        if (glowing == null) {
            glintedSlots.remove(player.getUniqueId());
        } else {
            glintedSlots.put(player.getUniqueId(), slot);
        }
    }

    private void clearGlint(Player player) {
        Integer slot = glintedSlots.remove(player.getUniqueId());
        if (slot != null) {
            applyGlint(player, slot, null);
        }
    }

    /**
     * The cooldown check-then-write and the {@link EnergyState} check-then-consume
     * below are only safe because this method is always invoked from
     * {@code player.getScheduler()}, which serializes per-entity - there is no
     * explicit lock guarding {@code cooldowns}/energy here, unlike the
     * per-key serial chains every persistent cached service uses. Calling
     * this from any other thread would reintroduce a double-activation race.
     */
    private void activateOnPlayerRegion(Player player, AbilityDefinition definition, int classLevel, EnergyService energy) {
        if (classLevel < definition.requiredLevel()) {
            messages.send(player, MessageKey.ABILITY_LOCKED, Map.of(
                    "kabiliyet", definition.displayName(),
                    "seviye", Integer.toString(definition.requiredLevel())
            ));
            return;
        }
        // Environment precondition, deliberately checked before any energy is
        // spent or any cooldown is written: ORE_REVEAL is the one ability that
        // is only meant to be usable somewhere it is actually useful.
        if (definition.effectType() == AbilityEffectType.ORE_REVEAL && !isInsideWideMine(player, definition)) {
            messages.send(player, MessageKey.ABILITY_REQUIRES_MINE, Map.of("kabiliyet", definition.displayName()));
            return;
        }
        // Creative mode lifts every budget an ability normally answers to: no
        // cooldown, no energy, and a duration that outlasts any session, so it
        // stays on until it is explicitly switched off again. The point of
        // Creative is testing what an ability does, not rationing it.
        boolean creative = isCreative(player);
        ClassModifiers modifiers = classModifiers.apply(player.getUniqueId());
        int energyCost = creative ? 0 : modifiers.adjustedEnergyCost(definition.id(), definition.energyCost());
        long cooldownMillis = creative ? 0L : modifiers.adjustedCooldownMillis(definition.id(), definition.cooldownMillis());
        int limit = modifiers.adjustedLimit(definition.id(), definition.limit());
        long configuredMillis = modifiers.adjustedDurationMillis(definition.id(), definition.activeMillis());
        long activeMillis = creative && !definition.effectType().resolvesImmediately()
                ? CREATIVE_DURATION_MILLIS
                : configuredMillis;
        CooldownKey cooldownKey = new CooldownKey(player.getUniqueId(), definition.id());
        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(cooldownKey, 0L);
        if (!creative && readyAt > now) {
            messages.send(player, MessageKey.ABILITY_COOLDOWN, Map.of(
                    "kabiliyet", definition.displayName(),
                    "sure", DurationFormatter.ofMillis(messages, readyAt - now)
            ));
            return;
        }
        if (!creative) {
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
        }
        ArmedAbility active = new ArmedAbility(definition.id(), now + activeMillis, limit);
        String duration = DurationFormatter.ofMillis(messages, activeMillis);
        switch (definition.effectType()) {
            case EXCAVATE, TREE_FELL, VEIN_MINE, BLAST_MINE -> {
                armed.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ARMED, Map.of(
                        "kabiliyet", definition.displayName(), "sure", duration
                ));
            }
            case DAMAGE_BUFF -> {
                damageBuffs.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ACTIVE, Map.of(
                        "kabiliyet", definition.displayName(), "sure", duration
                ));
            }
            // Shares damageBuffs with DAMAGE_BUFF rather than owning a fifth
            // state map: both are "buffed while attacking", only one ability can
            // be selected at a time, and handleEntityDamage already has the
            // definition in hand to tell them apart.
            case LIFE_STEAL -> {
                damageBuffs.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ACTIVE_LIFE_STEAL, Map.of(
                        "kabiliyet", definition.displayName(), "sure", duration,
                        "yuzde", Integer.toString((int) Math.round(definition.damageMultiplier() * 100))
                ));
            }
            case EXTRA_DROP_CHANCE -> {
                dropBuffs.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ACTIVE_DROP, Map.of(
                        "kabiliyet", definition.displayName(), "sure", duration
                ));
            }
            case ORE_REVEAL -> {
                int revealed = revealOres(player, definition, activeMillis);
                activeEffects.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ACTIVE_ORE_REVEAL, Map.of(
                        "kabiliyet", definition.displayName(),
                        "sayi", Integer.toString(revealed),
                        "sure", duration
                ));
            }
            case GROWTH_SURGE -> {
                int grown = surgeGrowth(player, limit);
                activeEffects.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ACTIVE_GROWTH, Map.of(
                        "kabiliyet", definition.displayName(), "sayi", Integer.toString(grown)
                ));
            }
            case LOVE_CALL -> {
                int called = callHerd(player, limit);
                activeEffects.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ACTIVE_LOVE_CALL, Map.of(
                        "kabiliyet", definition.displayName(), "sayi", Integer.toString(called)
                ));
            }
            default -> {
                applyInstantEffect(player, definition, activeMillis, modifiers);
                activeEffects.put(player.getUniqueId(), active);
                messages.send(player, MessageKey.ABILITY_ACTIVE_GENERIC, Map.of(
                        "kabiliyet", definition.displayName(), "sure", duration
                ));
            }
        }
        playActivationVisual(player, definition);
        startActivationFeedback(player, definition, active.expiresAtMillis(), energy);
    }

    /**
     * The ability's own sound cue and particle burst, replacing the single
     * shared {@code kabiliyet-baslangic} cue that used to fire identically for
     * all twelve abilities. An ability with no configured sound still falls
     * back to that shared cue, so {@code feedback.yml} stays meaningful.
     */
    private void playActivationVisual(Player player, AbilityDefinition definition) {
        AbilityVisual visual = definition.visual();
        if (visual.sound().isPresent()) {
            player.getWorld().playSound(player.getLocation(), visual.sound().get(), visual.volume(), visual.pitch());
        } else {
            feedback.celebrateAbilityActivated(player);
        }
        // A second cue a few ticks behind the first is what turns one sample
        // into a moment: the ear reads the pair as a single, larger event.
        visual.echoSound().ifPresent(echo -> player.getScheduler().runDelayed(plugin, task ->
                player.getWorld().playSound(player.getLocation(), echo, visual.volume(), visual.pitch() * ECHO_PITCH_FACTOR),
                null, ECHO_DELAY_TICKS));
        playShape(player, visual);
    }

    /**
     * The activation's particle choreography. Every shape is a short, bounded
     * animation that cancels itself - an ability's flourish can never outlive
     * the activation that started it, however long the ability itself runs.
     */
    private void playShape(Player player, AbilityVisual visual) {
        if (visual.particle().isEmpty() || visual.particleCount() <= 0) {
            return;
        }
        switch (visual.shape()) {
            case PATLAMA -> spawnBurst(player.getLocation().add(0, 1, 0), visual);
            case HALKA -> animate(player, visual, ANIMATION_STEPS, (location, step) ->
                    spawnRing(location, visual, 0.8D + step * 0.7D, 0.15D));
            case SARMAL -> animate(player, visual, ANIMATION_STEPS, (location, step) ->
                    spawnHelix(location, visual, step));
            case KUBBE -> animate(player, visual, ANIMATION_STEPS, (location, step) ->
                    spawnDome(location, visual, 0.6D + step * 0.5D));
        }
    }

    /**
     * Runs {@code frame} once per {@link #ANIMATION_INTERVAL_TICKS} for
     * {@code steps} steps on the player's own region scheduler, re-reading their
     * location each time so the effect follows them, then stops. Written as a
     * self-cancelling counter rather than a stored task because these live for
     * well under a second and nothing else ever needs to cancel one.
     */
    private void animate(Player player, AbilityVisual visual, int steps, AnimationFrame frame) {
        int[] step = {0};
        player.getScheduler().runAtFixedRate(plugin, task -> {
            if (step[0] >= steps || !player.isOnline()) {
                task.cancel();
                return;
            }
            frame.render(player.getLocation(), step[0]++);
        }, null, 1L, ANIMATION_INTERVAL_TICKS);
    }

    @FunctionalInterface
    private interface AnimationFrame {
        void render(Location center, int step);
    }

    /** Tinted for {@link Particle#DUST}, which is the only particle whose colour is caller-controlled. */
    private void spawnBurst(Location center, AbilityVisual visual) {
        if (visual.particle().isEmpty() || visual.particleCount() <= 0) {
            return;
        }
        Particle particle = visual.particle().get();
        if (particle == Particle.DUST) {
            center.getWorld().spawnParticle(particle, center, visual.particleCount(), BURST_SPREAD, BURST_SPREAD, BURST_SPREAD,
                    0.0D, new Particle.DustOptions(visual.color().effectColor(), 1.6F));
            return;
        }
        center.getWorld().spawnParticle(particle, center, visual.particleCount(), BURST_SPREAD, BURST_SPREAD, BURST_SPREAD, 0.05D);
    }

    /** One particle at a time around a circle, so the ring reads as a ring rather than as a cloud. */
    private void spawnRing(Location center, AbilityVisual visual, double radius, double height) {
        int points = Math.max(8, Math.min(visual.particleCount(), 32));
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            spawnSingle(center.clone().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius), visual);
        }
    }

    private void spawnHelix(Location center, AbilityVisual visual, int step) {
        int points = Math.max(3, Math.min(visual.particleCount() / 6, 8));
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points + step * 0.9D;
            double height = 0.2D + step * 0.4D;
            spawnSingle(center.clone().add(Math.cos(angle) * 0.9D, height, Math.sin(angle) * 0.9D), visual);
        }
    }

    private void spawnDome(Location center, AbilityVisual visual, double radius) {
        int points = Math.max(8, Math.min(visual.particleCount(), 28));
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double lift = Math.sin(Math.PI * i / points) * radius;
            spawnSingle(center.clone().add(Math.cos(angle) * radius, 0.4D + lift, Math.sin(angle) * radius), visual);
        }
    }

    private void spawnSingle(Location at, AbilityVisual visual) {
        Particle particle = visual.particle().orElseThrow();
        if (particle == Particle.DUST) {
            at.getWorld().spawnParticle(particle, at, 1, 0, 0, 0, 0.0D,
                    new Particle.DustOptions(visual.color().effectColor(), 1.4F));
            return;
        }
        at.getWorld().spawnParticle(particle, at, 1, 0, 0, 0, 0.0D);
    }

    private void applyInstantEffect(Player player, AbilityDefinition definition, long activeMillis, ClassModifiers modifiers) {
        int durationTicks = (int) Math.max(1L, activeMillis / 50L);
        switch (definition.effectType()) {
            case BRIEF_INVULNERABILITY -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 4));
            case AREA_HEAL -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 1));
            case SPEED_BURST -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 1));
            case NIGHT_VISION -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, durationTicks, 0));
            case KNOCKBACK_SLAM -> knockbackSlam(player, definition,
                    modifiers.adjustedEffectValue(definition.id(), definition.damageMultiplier()));
            default -> throw new IllegalStateException("Not an instant ability effect: " + definition.effectType());
        }
    }

    /**
     * A shockwave rather than a shove: the blast plays an explosion cue and
     * emitter at the caster's feet, throws every nearby creature roughly twice
     * as far and twice as high as the original implementation did, and leaves
     * an explosion puff on each thrown target. The configured
     * {@code hasar-carpani} is raw damage, not a multiplier, for this effect.
     */
    private void knockbackSlam(Player player, AbilityDefinition definition, double damage) {
        Location center = player.getLocation();
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.4F, 0.9F);
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center.clone().add(0, 1, 0), 12, 1.2D, 0.6D, 1.2D, 0.0D);
        double radius = definition.limit();
        for (Entity nearby : player.getNearbyEntities(radius, radius, radius)) {
            if (!(nearby instanceof LivingEntity target) || target instanceof Player) {
                continue;
            }
            Vector push = target.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() == 0) {
                push = center.getDirection();
            }
            target.setVelocity(push.normalize().multiply(2.6D).setY(0.85D));
            target.damage(damage, player);
            target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 0.8D, 0), 3, 0.2D, 0.2D, 0.2D, 0.0D);
        }
    }

    /**
     * The "wide mine" gate for {@link AbilityEffectType#ORE_REVEAL}: the
     * player must be genuinely underground (something solid overhead, so an
     * open field never qualifies) and standing in an actually open chamber
     * rather than a one-block strip-mining tunnel. Openness is measured as the
     * fraction of air in the surrounding cube against the ability's configured
     * {@code acik-alan-orani}.
     */
    private boolean isInsideWideMine(Player player, AbilityDefinition definition) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world.getHighestBlockYAt(location) <= location.getBlockY() + 2) {
            return false;
        }
        int originX = location.getBlockX();
        int originY = location.getBlockY();
        int originZ = location.getBlockZ();
        int total = 0;
        int air = 0;
        for (int x = -OPEN_SPACE_PROBE_RADIUS; x <= OPEN_SPACE_PROBE_RADIUS; x++) {
            for (int y = -OPEN_SPACE_PROBE_RADIUS; y <= OPEN_SPACE_PROBE_RADIUS; y++) {
                for (int z = -OPEN_SPACE_PROBE_RADIUS; z <= OPEN_SPACE_PROBE_RADIUS; z++) {
                    total++;
                    if (world.getBlockAt(originX + x, originY + y, originZ + z).getType().isAir()) {
                        air++;
                    }
                }
            }
        }
        return (double) air / total >= definition.damageMultiplier();
    }

    /**
     * Outlines every eligible ore within the ability's radius with a glowing,
     * see-through-walls {@link BlockDisplay} clone of the block itself, tinted
     * in the ability's own colour and removed again once the ability expires.
     * A display entity is the only way to make a *block* glow - the glow flag
     * exists on entities only - and it is scaled a hair larger than the real
     * block so the outline surrounds it instead of z-fighting with it.
     *
     * <p>Which blocks count is not a second hand-maintained ore list: it is
     * the ability's own skill's configured material set, so
     * {@code skills.yml} stays the single source of truth for what an ore
     * is.</p>
     *
     * <p><strong>The scan walks outward from the player, one cube shell at a
     * time, and that is load-bearing rather than a micro-optimisation.</strong>
     * The previous column-major sweep began at the western corner of the search
     * box, so once {@link #MAX_REVEALED_ORES} was reached - which at radius 36 a
     * real cave hits easily - the ores that got lit were whichever ones happened
     * to lie west of the player, and the exposed coal seam they were standing in
     * front of was never reached at all. That was the whole "only the ones
     * behind walls glow" report: nothing was wrong with the outline, the nearby
     * ores were simply never in the first two hundred. Shell order also means
     * the loop stops as soon as the budget is spent, instead of sweeping a
     * 73-block cube whenever ores are scarce.</p>
     *
     * @return how many ores were actually lit up, for the player's feedback message.
     */
    private int revealOres(Player player, AbilityDefinition definition, long activeMillis) {
        SkillDefinition skill = skills.settings().definitions().get(definition.skillId());
        if (skill == null) {
            return 0;
        }
        Set<Material> eligible = skill.materials();
        World world = player.getWorld();
        Location origin = player.getLocation();
        int radius = definition.limit();
        long expiryTicks = Math.max(1L, activeMillis / 50L);
        int originX = origin.getBlockX();
        int originY = origin.getBlockY();
        int originZ = origin.getBlockZ();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int revealed = 0;
        for (int shell = 0; shell <= radius && revealed < MAX_REVEALED_ORES; shell++) {
            for (int[] offset : shellOffsets(shell)) {
                if (revealed >= MAX_REVEALED_ORES) {
                    break;
                }
                int y = originY + offset[1];
                if (y < minY || y > maxY) {
                    continue;
                }
                int x = originX + offset[0];
                int z = originZ + offset[2];
                // Never generate a chunk the player cannot even see just to
                // look for ore in it.
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                Block block = world.getBlockAt(x, y, z);
                if (!eligible.contains(block.getType())) {
                    continue;
                }
                highlight(block, definition, expiryTicks, player.getUniqueId());
                revealed++;
            }
        }
        return revealed;
    }

    /**
     * Every offset whose Chebyshev distance from the origin is exactly
     * {@code shell} - the surface of one cube - enumerated directly rather than
     * by filtering the whole cube, so walking outward stays proportional to the
     * volume actually visited.
     */
    private static List<int[]> shellOffsets(int shell) {
        List<int[]> offsets = new ArrayList<>();
        if (shell == 0) {
            offsets.add(new int[]{0, 0, 0});
            return offsets;
        }
        for (int x = -shell; x <= shell; x++) {
            for (int z = -shell; z <= shell; z++) {
                offsets.add(new int[]{x, -shell, z});
                offsets.add(new int[]{x, shell, z});
            }
        }
        for (int y = -shell + 1; y <= shell - 1; y++) {
            for (int x = -shell; x <= shell; x++) {
                offsets.add(new int[]{x, y, -shell});
                offsets.add(new int[]{x, y, shell});
            }
            for (int z = -shell + 1; z <= shell - 1; z++) {
                offsets.add(new int[]{-shell, y, z});
                offsets.add(new int[]{shell, y, z});
            }
        }
        return offsets;
    }

    /**
     * {@link AbilityEffectType#GROWTH_SURGE}: brings every immature crop within
     * {@code radius} straight to harvest. Deliberately only touches
     * {@link Ageable} blocks that are not already at maximum age, so the count
     * the player is shown is the number of plants that actually changed, and
     * saplings/leaves/vines are left alone.
     *
     * @return how many crops were advanced, for the player's feedback message.
     */
    private int surgeGrowth(Player player, int radius) {
        World world = player.getWorld();
        Location origin = player.getLocation();
        int originX = origin.getBlockX();
        int originY = origin.getBlockY();
        int originZ = origin.getBlockZ();
        int minY = Math.max(world.getMinHeight(), originY - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, originY + radius);
        int grown = 0;
        for (int x = originX - radius; x <= originX + radius && grown < MAX_SURGED_CROPS; x++) {
            for (int z = originZ - radius; z <= originZ + radius && grown < MAX_SURGED_CROPS; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = minY; y <= maxY && grown < MAX_SURGED_CROPS; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!(block.getBlockData() instanceof Ageable crop) || crop.getAge() >= crop.getMaximumAge()) {
                        continue;
                    }
                    crop.setAge(crop.getMaximumAge());
                    block.setBlockData(crop);
                    world.spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5D, 0.5D, 0.5D), 6, 0.3D, 0.3D, 0.3D, 0.0D);
                    grown++;
                }
            }
        }
        return grown;
    }

    /**
     * {@link AbilityEffectType#LOVE_CALL}: puts every breedable adult animal
     * within {@code radius} into love mode, so a whole herd pairs up at once
     * without spending a single item on them. Babies and animals already in
     * love are skipped, which is also what keeps re-casting it on the same herd
     * from doing anything.
     *
     * <p>The animals' love window is {@link #LOVE_MODE_TICKS}, deliberately
     * <em>not</em> the ability's own duration. The two answer different
     * questions: the ability is over the instant the call goes out (which is
     * why it is configured at two seconds), while the herd still needs vanilla's
     * usual half-minute to find each other and pair up. Tying the two together
     * meant shortening the ability also quietly stopped it working.</p>
     *
     *
     * @return how many animals answered, for the player's feedback message.
     */
    private int callHerd(Player player, int radius) {
        int called = 0;
        for (Entity nearby : player.getNearbyEntities(radius, radius, radius)) {
            if (!(nearby instanceof Animals animal) || !animal.isAdult() || animal.isLoveMode()) {
                continue;
            }
            animal.setLoveModeTicks(LOVE_MODE_TICKS);
            animal.getWorld().spawnParticle(Particle.HEART, animal.getLocation().add(0, 1, 0), 3, 0.3D, 0.3D, 0.3D, 0.0D);
            called++;
        }
        return called;
    }

    /**
     * The outline is scaled a clear 8% larger than the real block rather than
     * the 2% it started at. At 2% an ore the player could already see plainly
     * had its halo hidden inside its own faces, so only ores buried in rock -
     * where the glow renders through the wall - looked lit at all.
     */
    private void highlight(Block block, AbilityDefinition definition, long expiryTicks, UUID ownerId) {
        RevealedBlock position = RevealedBlock.of(block);
        BlockDisplay display = block.getWorld().spawn(block.getLocation(), BlockDisplay.class, entity -> {
            entity.setBlock(block.getBlockData());
            entity.setGlowing(true);
            entity.setGlowColorOverride(definition.visual().color().effectColor());
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setPersistent(false);
            entity.setTransformation(new Transformation(
                    new Vector3f(-0.04F, -0.04F, -0.04F), new AxisAngle4f(),
                    new Vector3f(1.08F, 1.08F, 1.08F), new AxisAngle4f()
            ));
        });
        // A second activation can legitimately re-light a block whose previous
        // outline is still up; the old display is dropped rather than orphaned.
        RevealedOre previous = revealedOres.put(position, new RevealedOre(ownerId, display));
        if (previous != null) {
            previous.display().remove();
        }
        display.getScheduler().runDelayed(plugin, task -> {
            revealedOres.remove(position, new RevealedOre(ownerId, display));
            display.remove();
        }, null, expiryTicks);
    }

    /**
     * Takes down every outline this player put up. In Creative the ability has
     * no expiry worth waiting for, so switching it back off is the only thing
     * that ends it - and an ability that is off must not leave the cave still
     * glowing. Another player's outlines on the same blocks are untouched.
     */
    private void clearRevealedOres(UUID ownerId) {
        revealedOres.entrySet().removeIf(entry -> {
            if (!entry.getValue().ownerId().equals(ownerId)) {
                return false;
            }
            BlockDisplay display = entry.getValue().display();
            display.getScheduler().run(plugin, task -> display.remove(), null);
            return true;
        });
    }

    /**
     * Takes the outline down the moment its ore is actually mined, instead of
     * leaving a glowing ghost of a block that is no longer there for the rest of
     * the ability's duration. Called from the very top of
     * {@link #handleBlockBreak}, before the secondary-break and player-placed
     * guards, because a vein-mined or excavated block is broken for real too and
     * must stop glowing exactly the same way.
     */
    private void clearRevealedOre(Block block) {
        RevealedOre ore = revealedOres.remove(RevealedBlock.of(block));
        if (ore != null) {
            ore.display().remove();
        }
    }

    private boolean isEligibleBlock(AbilityDefinition definition, Block block) {
        SkillDefinition skill = skills.settings().definitions().get(definition.skillId());
        return skill != null && skill.materials().contains(block.getType());
    }

    /**
     * Every accepted block in the solid cube of half-edge {@code radius} around
     * {@code origin} - {@link AbilityEffectType#BLAST_MINE}'s shape. Unlike
     * {@code connectedBlocks} this does not require the blocks to touch each
     * other, which is the point: a blast takes out the stone between two seams
     * as readily as the seams themselves.
     */
    private List<Block> cubeArea(Block origin, int radius, Predicate<Block> accepted) {
        List<Block> blocks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block candidate = origin.getRelative(x, y, z);
                    if (accepted.test(candidate)) {
                        blocks.add(candidate);
                    }
                }
            }
        }
        return blocks;
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

    /**
     * Breaks every eligible candidate around {@code origin}, and is the single
     * place two rules that apply to <em>every</em> sweep live.
     *
     * <p><strong>The tool takes one point of damage per block actually
     * broken.</strong> {@link Block#breakNaturally(ItemStack, boolean)} uses the
     * item only to decide what drops - it never touches durability - so without
     * this a sweep was strictly cheaper than breaking the same blocks by hand.
     * Charging exactly what the manual work would have cost is what keeps
     * felling a whole tree a real decision, and {@code damage(...)} still
     * honours Unbreaking and still fires the item-break handling.</p>
     *
     * <p><strong>{@code drain} stops the sweep the moment the player's energy
     * runs out</strong>, so an ability's reach is bounded by the energy bar
     * rather than by its configured block limit alone. A no-op drain (Creative,
     * or an ability with no per-block cost) leaves the older behaviour
     * untouched.</p>
     *
     * @return how many blocks were actually broken
     */
    private int breakSecondaryBlocks(Player player, Block origin, List<Block> candidates, int limit, EnergyDrain drain) {
        int processed = 0;
        int broken = 0;
        for (Block candidate : candidates) {
            if (candidate.equals(origin) || processed++ >= limit || candidate.getType().isAir()
                    || placedBlocks.isPlayerPlaced(candidate)) {
                continue;
            }
            if (!drain.tryConsume(player)) {
                break;
            }
            Set<Block> marked = secondaryBreaks.get();
            marked.add(candidate);
            try {
                BlockBreakEvent secondary = new BlockBreakEvent(candidate, player);
                Bukkit.getPluginManager().callEvent(secondary);
                if (!secondary.isCancelled()) {
                    candidate.breakNaturally(player.getInventory().getItemInMainHand(), true);
                    broken++;
                }
            } finally {
                marked.remove(candidate);
            }
        }
        damageTool(player, broken);
        return broken;
    }

    private void damageTool(Player player, int amount) {
        if (amount <= 0 || isCreative(player)) {
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) {
            return;
        }
        // The returned stack is what survives - an item that broke comes back
        // empty - so it has to be written back rather than assumed to be the
        // same object the inventory is holding.
        player.getInventory().setItemInMainHand(tool.damage(amount, player));
    }

    /**
     * Per-block energy budget for one sweep. Constructed once per sweep so the
     * "did we run out?" answer survives for the caller's message, rather than
     * being recomputed from a bar that is already empty.
     */
    private static final class EnergyDrain {

        private final EnergyService energy;
        private final int perBlock;
        private boolean exhausted;

        private EnergyDrain(EnergyService energy, int perBlock) {
            this.energy = energy;
            this.perBlock = perBlock;
        }

        static EnergyDrain none() {
            return new EnergyDrain(null, 0);
        }

        boolean tryConsume(Player player) {
            if (energy == null || perBlock <= 0) {
                return true;
            }
            if (energy.tryConsume(player, perBlock)) {
                return true;
            }
            exhausted = true;
            return false;
        }

        boolean exhausted() {
            return exhausted;
        }
    }

    private AbilitySettings parse(YamlConfiguration yaml) {
        int maximumEnergy = yaml.getInt("enerji.maksimum", 100);
        int regeneration = yaml.getInt("enerji.yenilenme-saniye-basina", 4);
        if (maximumEnergy <= 0 || regeneration < 0) {
            throw new IllegalArgumentException("Energy settings must be non-negative and maximum positive.");
        }
        long selectionCooldownMillis = secondsToMillis(yaml.getLong("secim-bekleme-saniye", 120));
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
            Material icon = Material.matchMaterial(section.getString("simge", ""));
            if (icon == null) {
                throw new IllegalArgumentException("Ability must declare a known simge material: " + id);
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
            int energyPerBlock = section.getInt("enerji-blok-basi", 0);
            AbilityVisual visual = parseVisual(section.getConfigurationSection("gorsel"), id);
            double openAirRatio = section.getDouble("etki.acik-alan-orani", -1.0D);
            if (requiredLevel < 1 || energyCost < 0 || cooldownMillis < 0 || activeMillis <= 0 || energyPerBlock < 0) {
                throw new IllegalArgumentException("Invalid ability settings: " + id);
            }
            boolean sweep = effect == AbilityEffectType.EXCAVATE || effect == AbilityEffectType.TREE_FELL
                    || effect == AbilityEffectType.VEIN_MINE || effect == AbilityEffectType.BLAST_MINE;
            if (energyPerBlock > 0 && !sweep) {
                throw new IllegalArgumentException("enerji-blok-basi only applies to a block-sweep ability: " + id);
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
                case LIFE_STEAL -> {
                    // A fraction of the damage dealt, so unlike DAMAGE_BUFF it
                    // is a ratio and must stay inside (0, 1].
                    double ratio = section.getDouble("etki.can-orani", -1.0D);
                    if (ratio <= 0 || ratio > 1.0D) throw new IllegalArgumentException("Life steal can-orani must be in (0, 1]: " + id);
                    multiplier = ratio;
                }
                case BLAST_MINE -> {
                    if (radius < 1 || radius > 2) throw new IllegalArgumentException("Blast mine yaricap must be 1 or 2: " + id);
                    limit = radius;
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
                case ORE_REVEAL -> {
                    if (radius < 1) throw new IllegalArgumentException("Ore reveal ability requires a positive radius: " + id);
                    if (openAirRatio <= 0 || openAirRatio >= 1.0D) {
                        throw new IllegalArgumentException("Ore reveal acik-alan-orani must be in (0, 1): " + id);
                    }
                    limit = radius;
                    multiplier = openAirRatio;
                }
                case GROWTH_SURGE, LOVE_CALL -> {
                    if (radius < 1) throw new IllegalArgumentException("Area ability requires a positive radius: " + id);
                    limit = radius;
                }
                case BRIEF_INVULNERABILITY, AREA_HEAL, SPEED_BURST, NIGHT_VISION -> {
                    // No extra numeric fields required beyond duration/energy/cooldown.
                }
            }
            definitions.put(id, new AbilityDefinition(id, displayName, description, icon, skillId, requiredLevel, energyCost,
                    cooldownMillis, activeMillis, effect, limit, multiplier, tool, classId, mode, energyPerHit, energyPerBlock, visual));
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("abilities.yml must define at least one ability.");
        }
        return new AbilitySettings(maximumEnergy, regeneration, selectionCooldownMillis, Map.copyOf(definitions));
    }

    /**
     * {@code gorsel} is optional so an admin-authored ability without one
     * still loads; it then falls back to the single shared
     * {@code kabiliyet-baslangic} sound cue and spawns no particles, which is
     * exactly how every ability behaved before per-ability visuals existed.
     */
    private AbilityVisual parseVisual(ConfigurationSection section, String abilityId) {
        if (section == null) {
            return AbilityVisual.defaults();
        }
        AbilityColor color = AbilityColor.valueOf(section.getString("renk", "BEYAZ").toUpperCase(Locale.ROOT));
        String soundName = section.getString("ses", "");
        String echoName = section.getString("yanki-ses", "");
        String particleName = section.getString("partikul", "");
        float volume = (float) section.getDouble("ses-seviyesi", 1.0D);
        float pitch = (float) section.getDouble("ton", 1.0D);
        int particleCount = section.getInt("partikul-sayisi", 30);
        if (volume <= 0 || pitch <= 0 || particleCount < 0) {
            throw new IllegalArgumentException("Ability sound volume/pitch must be positive and particle count non-negative: " + abilityId);
        }
        AbilityVisualShape shape = AbilityVisualShape.valueOf(section.getString("sekil", "PATLAMA").toUpperCase(Locale.ROOT));
        Optional<Sound> sound = soundName.isBlank()
                ? Optional.empty()
                : Optional.of(Sound.valueOf(soundName.toUpperCase(Locale.ROOT)));
        Optional<Sound> echoSound = echoName.isBlank()
                ? Optional.empty()
                : Optional.of(Sound.valueOf(echoName.toUpperCase(Locale.ROOT)));
        Optional<Particle> particle = particleName.isBlank()
                ? Optional.empty()
                : Optional.of(Particle.valueOf(particleName.toUpperCase(Locale.ROOT)));
        return new AbilityVisual(color, sound, volume, pitch, particle, particleCount, shape, echoSound);
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

    /** Identity of one ORE_REVEAL outline, so the right display is torn down when that exact block breaks. */
    private record RevealedBlock(UUID worldId, int x, int y, int z) {

        static RevealedBlock of(Block block) {
            return new RevealedBlock(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }

    /** One outline plus who lit it, so switching the ability off only takes down that player's own. */
    private record RevealedOre(UUID ownerId, BlockDisplay display) {
    }

    private record CooldownKey(UUID playerId, String abilityId) {
    }
}
