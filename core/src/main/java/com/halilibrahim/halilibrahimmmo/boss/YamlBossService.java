package com.halilibrahim.halilibrahimmmo.boss;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * YAML-backed world boss spawner and behaviour loop.
 *
 * <p>Bosses are deliberately spawned <strong>near a randomly chosen online
 * player</strong> rather than at a genuinely uniform point on the map. A boss
 * nobody can reach is a boss nobody fights: at a few hundred blocks it is
 * somewhere the player has to travel to, in terrain they have probably never
 * seen, but it is still a place they can get to before the next one spawns. The
 * announcement carries the coordinates, so the whole server can converge on
 * it.</p>
 */
public final class YamlBossService implements BossService {

    private static final String FILE_NAME = "bosslar.yml";
    /** How often each living boss re-evaluates whether to heal, blink or run. */
    private static final long BEHAVIOUR_PERIOD_TICKS = 40L;
    private static final double FLEE_HEALTH_RATIO = 0.25D;
    private static final double HEAL_HEALTH_RATIO = 0.50D;
    private static final long HEAL_INTERVAL_MILLIS = 30_000L;
    private static final long TELEPORT_INTERVAL_MILLIS = 12_000L;
    /** How far a fleeing boss throws itself, and how far a blinking one repositions. */
    private static final int FLEE_DISTANCE = 16;
    private static final int BLINK_DISTANCE = 6;
    private static final double TARGET_SEARCH_RADIUS = 32.0D;
    /** Attempts made to find standing room before giving up on this spawn cycle. */
    private static final int SPAWN_ATTEMPTS = 12;

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final FeedbackService feedback;
    private final NamespacedKey rarityKey;
    private final NamespacedKey nameKey;
    private final NamespacedKey headKey;
    private final AtomicReference<BossSettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<UUID, BossState> activeBosses = new ConcurrentHashMap<>();
    private ScheduledTask spawnTask;

    public YamlBossService(JavaPlugin plugin, MessageService messages, FeedbackService feedback) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.rarityKey = new NamespacedKey(plugin, "boss-nadirlik");
        this.nameKey = new NamespacedKey(plugin, "boss-isim");
        this.headKey = new NamespacedKey(plugin, "boss-kafa");
    }

    @Override
    public void enable() {
        reload();
        long periodTicks = Math.max(20L, settings().spawnIntervalMillis() / 50L);
        spawnTask = plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, task -> spawnNow(), periodTicks, periodTicks);
    }

    @Override
    public void disable() {
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        activeBosses.clear();
    }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        try {
            settings.set(parse(YamlConfiguration.loadConfiguration(file)));
        } catch (RuntimeException exception) {
            throw new LifecycleException("bosslar.yml could not be loaded.", exception);
        }
    }

    @Override
    public BossSettings settings() {
        BossSettings value = settings.get();
        if (value == null) {
            throw new IllegalStateException("BossService.enable() has not run yet.");
        }
        return value;
    }

    @Override
    public boolean isBoss(LivingEntity entity) {
        return entity.getPersistentDataContainer().has(rarityKey, PersistentDataType.STRING);
    }

    @Override
    public Optional<BossRarity> rarityOf(LivingEntity entity) {
        String raw = entity.getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(BossRarity.valueOf(raw));
        } catch (IllegalArgumentException exception) {
            // A rarity removed from configuration after a boss carrying it was
            // saved: the creature stays, it simply stops being a boss.
            return Optional.empty();
        }
    }

    /** The name this boss was given, for the death announcement and its head. */
    public Optional<String> nameOf(LivingEntity entity) {
        return Optional.ofNullable(entity.getPersistentDataContainer().get(nameKey, PersistentDataType.STRING));
    }

    /** The head material this boss should leave, resolved from the tag rather than from live configuration. */
    public Optional<Material> headOf(LivingEntity entity) {
        String raw = entity.getPersistentDataContainer().get(headKey, PersistentDataType.STRING);
        return raw == null ? Optional.empty() : Optional.ofNullable(Material.matchMaterial(raw));
    }

    @Override
    public Optional<String> spawnNow() {
        List<Player> candidates = Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getWorld().getEnvironment() == World.Environment.NORMAL)
                .map(player -> (Player) player)
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        Player anchor = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        BossArchetype archetype = pickArchetype();
        BossRarity rarity = pickRarity();
        String name = archetype.names().get(ThreadLocalRandom.current().nextInt(archetype.names().size()));
        // The anchor's own region owns the chunks around them, so the search and
        // the spawn both have to happen from there.
        anchor.getScheduler().run(plugin, task -> spawnNear(anchor, archetype, rarity, name), null);
        return Optional.of(name);
    }

    private void spawnNear(Player anchor, BossArchetype archetype, BossRarity rarity, String name) {
        Location location = findSpawn(anchor);
        if (location == null) {
            return;
        }
        BossRaritySettings tier = settings().rarities().get(rarity);
        if (!(location.getWorld().spawnEntity(location, archetype.entityType()) instanceof Mob mob)) {
            return;
        }
        applyStats(mob, archetype, rarity, tier, name);
        activeBosses.put(mob.getUniqueId(), new BossState(rarity));
        mob.getScheduler().runAtFixedRate(plugin, task -> behave(mob), () -> activeBosses.remove(mob.getUniqueId()),
                BEHAVIOUR_PERIOD_TICKS, BEHAVIOUR_PERIOD_TICKS);
        messages.broadcast(rarity.spawnBroadcastKey(), Map.of(
                "isim", name,
                "x", Integer.toString(location.getBlockX()),
                "z", Integer.toString(location.getBlockZ())
        ));
        Bukkit.getOnlinePlayers().forEach(online ->
                online.getScheduler().run(plugin, task -> feedback.celebrateBossSpawned(online), null));
    }

    /**
     * Standing room within the configured ring around {@code anchor}. Only
     * already-loaded chunks are considered - a boss is announced with its
     * coordinates, and generating terrain nobody has visited just to put one
     * there would stall the region for everyone nearby.
     */
    private Location findSpawn(Player anchor) {
        World world = anchor.getWorld();
        BossSettings current = settings();
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            int x = anchor.getLocation().getBlockX() + signedOffset(current);
            int z = anchor.getLocation().getBlockZ() + signedOffset(current);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int y = world.getHighestBlockYAt(x, z);
            if (y <= world.getMinHeight()) {
                continue;
            }
            if (!world.getBlockAt(x, y, z).getType().isSolid()
                    || !world.getBlockAt(x, y + 1, z).getType().isAir()
                    || !world.getBlockAt(x, y + 2, z).getType().isAir()) {
                continue;
            }
            return new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
        }
        return null;
    }

    private int signedOffset(BossSettings current) {
        int distance = ThreadLocalRandom.current().nextInt(current.minimumDistance(), current.maximumDistance() + 1);
        return ThreadLocalRandom.current().nextBoolean() ? distance : -distance;
    }

    private BossArchetype pickArchetype() {
        List<BossArchetype> archetypes = settings().archetypeList();
        return archetypes.get(ThreadLocalRandom.current().nextInt(archetypes.size()));
    }

    /**
     * Weighted pick over every rarity's configured share. Bounded by the total
     * weight rather than by 100, so the shipped 49/30/20/10/1 split is used as
     * the ratio it describes and can never quietly stop summing to a whole.
     */
    private BossRarity pickRarity() {
        BossSettings current = settings();
        int roll = ThreadLocalRandom.current().nextInt(current.totalWeight());
        int cumulative = 0;
        for (Map.Entry<BossRarity, BossRaritySettings> entry : current.rarities().entrySet()) {
            cumulative += entry.getValue().weight();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }
        return BossRarity.SIRADAN;
    }

    private void applyStats(Mob mob, BossArchetype archetype, BossRarity rarity, BossRaritySettings tier, String name) {
        mob.customName(messages.render(rarity.nameplateKey(), Map.of("isim", name)));
        mob.setCustomNameVisible(true);
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
        mob.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, rarity.name());
        mob.getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, name);
        mob.getPersistentDataContainer().set(headKey, PersistentDataType.STRING, archetype.headMaterial().name());

        scale(mob, Attribute.MAX_HEALTH, tier.healthMultiplier());
        AttributeInstance maximumHealth = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maximumHealth != null) {
            mob.setHealth(maximumHealth.getValue());
        }
        scale(mob, Attribute.ATTACK_DAMAGE, tier.damageMultiplier());
        scale(mob, Attribute.MOVEMENT_SPEED, tier.speedMultiplier());
        scale(mob, Attribute.FOLLOW_RANGE, 2.0D);

        EntityEquipment equipment = mob.getEquipment();
        if (equipment != null) {
            Material weapon = tier.weapons().get(archetype.weaponType());
            if (weapon != null) {
                equipment.setItemInMainHand(new ItemStack(weapon));
            }
            equipment.setItemInMainHandDropChance(0f);
            applyArmour(equipment, tier.armour());
        }
        // The boss's own permanent buffs. Duration is effectively unbounded
        // because these are what the rarity *is*, not a temporary state.
        if (tier.damageMultiplier() >= 2.0D) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 0, false, false));
        }
        if (tier.healthMultiplier() >= 6.0D) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 1, false, false));
        }
        mob.getWorld().spawnParticle(Particle.FLASH, mob.getLocation(), 1);
    }

    private void applyArmour(EntityEquipment equipment, List<Material> armour) {
        equipment.setHelmetDropChance(0f);
        equipment.setChestplateDropChance(0f);
        equipment.setLeggingsDropChance(0f);
        equipment.setBootsDropChance(0f);
        for (int slot = 0; slot < armour.size(); slot++) {
            ItemStack piece = new ItemStack(armour.get(slot));
            switch (slot) {
                case 0 -> equipment.setHelmet(piece);
                case 1 -> equipment.setChestplate(piece);
                case 2 -> equipment.setLeggings(piece);
                case 3 -> equipment.setBoots(piece);
                default -> {
                    // A fifth entry would have nowhere to go; the parser caps the list at four.
                }
            }
        }
    }

    /** Multiplies an attribute the mob may not even have - skeletons carry no attack-damage attribute. */
    private void scale(Mob mob, Attribute attribute, double multiplier) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(instance.getBaseValue() * multiplier);
        }
    }

    /**
     * The per-boss behaviour tick: heal, blink, or run. Only ever runs on the
     * boss's own entity scheduler, so touching its health, position and effects
     * from here is safe.
     */
    private void behave(Mob mob) {
        BossState state = activeBosses.get(mob.getUniqueId());
        if (state == null || mob.isDead()) {
            return;
        }
        BossRaritySettings tier = settings().rarities().get(state.rarity());
        if (tier == null) {
            return;
        }
        AttributeInstance maximum = mob.getAttribute(Attribute.MAX_HEALTH);
        double ratio = maximum == null ? 1.0D : mob.getHealth() / maximum.getValue();
        long now = System.currentTimeMillis();

        if (tier.drinksPotions() && ratio <= HEAL_HEALTH_RATIO && now - state.lastHealMillis() >= HEAL_INTERVAL_MILLIS) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 2));
            mob.getWorld().spawnParticle(Particle.HEART, mob.getLocation().add(0, 1, 0), 8, 0.4D, 0.4D, 0.4D, 0.0D);
            state.markHealed(now);
        }
        // Running takes priority over repositioning: a boss at a quarter health
        // is trying to get away, not to close the distance.
        if (tier.flees() && ratio <= FLEE_HEALTH_RATIO && !state.hasFled()) {
            blink(mob, FLEE_DISTANCE, true);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2));
            state.markFled();
            return;
        }
        if (tier.teleports() && now - state.lastTeleportMillis() >= TELEPORT_INTERVAL_MILLIS) {
            blink(mob, BLINK_DISTANCE, false);
            state.markTeleported(now);
        }
    }

    /**
     * Moves the boss {@code distance} blocks away from - or toward - the nearest
     * player, landing on solid ground. A blink that finds nowhere to land simply
     * does not happen, which is much better than dropping a boss into a wall.
     */
    private void blink(Mob mob, int distance, boolean away) {
        Player nearest = nearestPlayer(mob);
        if (nearest == null) {
            return;
        }
        Vector direction = nearest.getLocation().toVector().subtract(mob.getLocation().toVector());
        if (direction.lengthSquared() == 0) {
            return;
        }
        direction = direction.normalize().multiply(away ? -distance : distance);
        World world = mob.getWorld();
        int x = mob.getLocation().getBlockX() + direction.getBlockX();
        int z = mob.getLocation().getBlockZ() + direction.getBlockZ();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        int y = world.getHighestBlockYAt(x, z);
        if (y <= world.getMinHeight() || !world.getBlockAt(x, y + 1, z).getType().isAir()) {
            return;
        }
        Location destination = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
        world.spawnParticle(Particle.PORTAL, mob.getLocation().add(0, 1, 0), 30, 0.4D, 0.8D, 0.4D, 0.4D);
        mob.teleportAsync(destination);
    }

    private Player nearestPlayer(Mob mob) {
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player player : mob.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(mob.getLocation());
            if (distance < best && distance <= TARGET_SEARCH_RADIUS * TARGET_SEARCH_RADIUS) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private BossSettings parse(YamlConfiguration yaml) {
        long intervalMinutes = yaml.getLong("spawn-araligi-dakika", 30);
        int minimumDistance = yaml.getInt("oyuncuya-uzaklik.en-az", 120);
        int maximumDistance = yaml.getInt("oyuncuya-uzaklik.en-fazla", 320);
        if (intervalMinutes < 1 || minimumDistance < 1 || maximumDistance <= minimumDistance) {
            throw new IllegalArgumentException("Invalid boss spawn interval or distance range.");
        }
        Map<BossRarity, BossRaritySettings> rarities = new EnumMap<>(BossRarity.class);
        ConfigurationSection raritySection = requireSection(yaml, "nadirlikler");
        for (BossRarity rarity : BossRarity.values()) {
            ConfigurationSection section = raritySection.getConfigurationSection(rarity.name());
            if (section == null) {
                throw new IllegalArgumentException("bosslar.yml is missing rarity: " + rarity.name());
            }
            rarities.put(rarity, parseRarity(section, rarity));
        }
        Map<String, BossArchetype> archetypes = new LinkedHashMap<>();
        ConfigurationSection archetypeSection = requireSection(yaml, "bosslar");
        for (String rawId : archetypeSection.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+") || archetypes.containsKey(id)) {
                throw new IllegalArgumentException("Invalid or duplicate boss archetype id: " + rawId);
            }
            archetypes.put(id, parseArchetype(requireSection(archetypeSection, rawId), id));
        }
        if (archetypes.isEmpty()) {
            throw new IllegalArgumentException("bosslar.yml must define at least one boss archetype.");
        }
        return new BossSettings(intervalMinutes * 60_000L, minimumDistance, maximumDistance,
                Map.copyOf(rarities), Map.copyOf(archetypes));
    }

    private BossRaritySettings parseRarity(ConfigurationSection section, BossRarity rarity) {
        int weight = section.getInt("agirlik", 0);
        double health = section.getDouble("can-carpani", 0);
        double damage = section.getDouble("hasar-carpani", 0);
        double speed = section.getDouble("hiz-carpani", 0);
        long classExperience = section.getLong("sinif-xp", 0);
        int vanillaExperience = section.getInt("oyun-xp", 0);
        long wealth = section.getLong("zenginlik", 0);
        if (weight <= 0 || health < 1 || damage < 1 || speed < 1 || classExperience <= 0 || vanillaExperience <= 0 || wealth <= 0) {
            throw new IllegalArgumentException("Invalid numeric settings for boss rarity: " + rarity.name());
        }
        Map<BossWeaponType, Material> weapons = new EnumMap<>(BossWeaponType.class);
        ConfigurationSection weaponSection = section.getConfigurationSection("silahlar");
        if (weaponSection != null) {
            for (String rawType : weaponSection.getKeys(false)) {
                BossWeaponType type = BossWeaponType.valueOf(rawType.toUpperCase(Locale.ROOT));
                Material material = Material.matchMaterial(weaponSection.getString(rawType, ""));
                if (material == null) {
                    throw new IllegalArgumentException("Unknown weapon material for " + rarity.name() + ": " + rawType);
                }
                weapons.put(type, material);
            }
        }
        List<Material> armour = new ArrayList<>();
        for (String rawName : section.getStringList("zirh")) {
            Material material = Material.matchMaterial(rawName);
            if (material == null) {
                throw new IllegalArgumentException("Unknown armour material for " + rarity.name() + ": " + rawName);
            }
            armour.add(material);
        }
        if (armour.size() > 4) {
            throw new IllegalArgumentException("A boss can wear at most four armour pieces: " + rarity.name());
        }
        List<BossLootEntry> loot = new ArrayList<>();
        for (Map<?, ?> raw : section.getMapList("ganimet")) {
            loot.add(parseLoot(raw, rarity));
        }
        if (loot.isEmpty()) {
            throw new IllegalArgumentException("Every boss rarity must drop something: " + rarity.name());
        }
        return new BossRaritySettings(weight, health, damage, speed, classExperience, vanillaExperience, wealth,
                section.getBoolean("kacar", false), section.getBoolean("isinlanir", false),
                section.getBoolean("iksir-icer", false), Map.copyOf(weapons), List.copyOf(armour), List.copyOf(loot));
    }

    private BossLootEntry parseLoot(Map<?, ?> raw, BossRarity rarity) {
        Material material = Material.matchMaterial(String.valueOf(raw.get("esya")));
        if (material == null) {
            throw new IllegalArgumentException("Unknown loot material for " + rarity.name() + ": " + raw.get("esya"));
        }
        int minimum = number(raw.get("en-az"), 1).intValue();
        int maximum = number(raw.get("en-fazla"), minimum).intValue();
        double chance = number(raw.get("sans"), 1.0D).doubleValue();
        if (minimum < 1 || maximum < minimum || chance <= 0 || chance > 1.0D) {
            throw new IllegalArgumentException("Invalid loot entry for " + rarity.name() + ": " + material);
        }
        return new BossLootEntry(material, minimum, maximum, chance);
    }

    /** Bukkit hands map-list values back as a wildcard-typed map, so the fallback has to be supplied separately. */
    private Number number(Object value, Number fallback) {
        return value instanceof Number parsed ? parsed : fallback;
    }

    private BossArchetype parseArchetype(ConfigurationSection section, String id) {
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(section.getString("varlik", "").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown entity type for boss archetype: " + id, exception);
        }
        Material head = Material.matchMaterial(section.getString("kafa", ""));
        if (head == null) {
            throw new IllegalArgumentException("Boss archetype must declare a known kafa material: " + id);
        }
        BossWeaponType weaponType = BossWeaponType.valueOf(section.getString("silah-turu", "YOK").toUpperCase(Locale.ROOT));
        List<String> names = section.getStringList("isimler");
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Boss archetype must declare at least one name: " + id);
        }
        return new BossArchetype(id, entityType, head, weaponType, List.copyOf(names));
    }

    private ConfigurationSection requireSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing YAML section: " + path);
        }
        return section;
    }

    /** Transient per-boss behaviour state; lost on restart along with the behaviour loop itself. */
    private static final class BossState {

        private final BossRarity rarity;
        private volatile long lastHealMillis;
        private volatile long lastTeleportMillis;
        private volatile boolean fled;

        private BossState(BossRarity rarity) {
            this.rarity = rarity;
        }

        BossRarity rarity() {
            return rarity;
        }

        long lastHealMillis() {
            return lastHealMillis;
        }

        long lastTeleportMillis() {
            return lastTeleportMillis;
        }

        boolean hasFled() {
            return fled;
        }

        void markHealed(long now) {
            lastHealMillis = now;
        }

        void markTeleported(long now) {
            lastTeleportMillis = now;
        }

        void markFled() {
            fled = true;
        }
    }
}
