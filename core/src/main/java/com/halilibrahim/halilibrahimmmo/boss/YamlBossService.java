package com.halilibrahim.halilibrahimmmo.boss;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
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
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
    /**
     * How often each living boss re-evaluates whether to heal, blink or run,
     * and repaints its health bar. Half a second is chosen by the bar rather
     * than by the behaviour: a health bar that lags a fight by two seconds
     * reads as broken, while every behaviour decision is independently gated on
     * its own millisecond interval and does not care how often it is asked.
     */
    private static final long BEHAVIOUR_PERIOD_TICKS = 10L;
    /** How close a player must be to see a boss's health bar. */
    private static final double BAR_VIEW_RADIUS = 96.0D;
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
        // The scheduled spawn deliberately ignores its own future: nobody is
        // waiting on it, and a cycle that finds nowhere to stand simply waits
        // for the next one. Only the admin panel cares about the answer.
    }

    @Override
    public void disable() {
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        // Boss bars are shown to players, not owned by the entity, so they have
        // to be taken down explicitly - a reload would otherwise leave a frozen
        // bar on every screen that had one open.
        Set.copyOf(activeBosses.keySet()).forEach(this::retire);
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
    public CompletableFuture<Optional<String>> spawnNow() {
        return spawnNow(null, null);
    }

    @Override
    public CompletableFuture<Optional<String>> spawnNow(BossRarity chosenRarity, String chosenName) {
        List<Player> candidates = Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getWorld().getEnvironment() == World.Environment.NORMAL)
                .map(player -> (Player) player)
                .toList();
        if (candidates.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        Player anchor = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        BossArchetype archetype = pickArchetype();
        BossRarity rarity = chosenRarity == null ? pickRarity() : chosenRarity;
        String name = chosenName == null || chosenName.isBlank()
                ? archetype.names().get(ThreadLocalRandom.current().nextInt(archetype.names().size()))
                : chosenName.strip();
        CompletableFuture<Optional<String>> result = new CompletableFuture<>();
        findSpawn(anchor, 1, result, location ->
                anchor.getScheduler().run(plugin, task -> {
                    // Spawning has to happen from a region that owns the chunk;
                    // the anchor's own region is the one that just loaded it.
                    boolean spawned = spawnAt(location, archetype, rarity, name);
                    result.complete(spawned ? Optional.of(name) : Optional.empty());
                }, () -> result.complete(Optional.empty())));
        return result;
    }

    private boolean spawnAt(Location location, BossArchetype archetype, BossRarity rarity, String name) {
        BossRaritySettings tier = settings().rarities().get(rarity);
        if (tier == null || !(location.getWorld().spawnEntity(location, archetype.entityType()) instanceof Mob mob)) {
            return false;
        }
        // Cleared before the newcomer is announced, so the server never sees two
        // spawn broadcasts and then finds the older boss still standing.
        dismissUnengagedBosses();
        applyStats(mob, archetype, rarity, tier, name);
        BossState state = new BossState(rarity, name, createBossBar(mob, rarity, name));
        activeBosses.put(mob.getUniqueId(), state);
        mob.getScheduler().runAtFixedRate(plugin, task -> behave(mob), () -> retire(mob.getUniqueId()),
                BEHAVIOUR_PERIOD_TICKS, BEHAVIOUR_PERIOD_TICKS);
        messages.broadcast(rarity.spawnBroadcastKey(), Map.of(
                "isim", name,
                "x", Integer.toString(location.getBlockX()),
                "y", Integer.toString(location.getBlockY()),
                "z", Integer.toString(location.getBlockZ()),
                "dunya", location.getWorld().getName()
        ));
        shareWaypoint(location, rarity, name);
        Bukkit.getOnlinePlayers().forEach(online ->
                online.getScheduler().run(plugin, task -> feedback.celebrateBossSpawned(online), null));
        return true;
    }

    /**
     * Puts the boss on everyone's minimap as a Xaero's Minimap waypoint.
     *
     * <p>Xaero's reads waypoints out of ordinary chat messages, so the whole
     * integration is one line of text in a documented format - no API, no
     * dependency, and nothing to break when the mod updates. It is safe to send
     * to everybody because Xaero's Minimap is a <em>required</em> mod on this
     * server ({@code modlar.yml}), so no player is left staring at the raw
     * token. The template lives in {@code messages.yml} rather than in Java for
     * the ordinary reason every other line does, and because that is also the
     * one place an owner can blank it out if they ever drop the mod
     * requirement.</p>
     *
     * <p>What the waypoint can carry is genuinely limited by the format: a
     * short label, a colour index and coordinates. A boss therefore shows up as
     * its own initials in its rarity's colour - the closest thing to "its head
     * on the map" the mod actually supports. Xaero's draws the off-screen
     * direction arrow for a waypoint on its own, which is what covers a player
     * who is nowhere near it yet.</p>
     */
    private void shareWaypoint(Location location, BossRarity rarity, String name) {
        Map<String, String> placeholders = Map.of(
                "isim", name,
                "kisaltma", waypointInitials(name),
                "x", Integer.toString(location.getBlockX()),
                "y", Integer.toString(location.getBlockY()),
                "z", Integer.toString(location.getBlockZ()),
                "renk", Integer.toString(waypointColorIndex(rarity))
        );
        // render(...), not send(...): Xaero's matches the whole message against
        // its own format, so the chat prefix every other line carries would stop
        // the waypoint being recognised at all. Same reasoning §18.3 recorded
        // for the kick screen.
        Component waypoint = messages.render(MessageKey.BOSS_WAYPOINT, placeholders);
        Bukkit.getOnlinePlayers().forEach(online -> online.sendMessage(waypoint));
    }

    /** Xaero's waypoint label: at most two characters, since that is all it draws. */
    private String waypointInitials(String name) {
        String letters = name.replaceAll("[^\\p{L}\\p{N}]", "");
        if (letters.isEmpty()) {
            return "BS";
        }
        return letters.substring(0, Math.min(2, letters.length())).toUpperCase(Locale.ROOT);
    }

    /** Xaero's takes a colour as an index into the vanilla chat-colour table, not as RGB. */
    private int waypointColorIndex(BossRarity rarity) {
        return switch (rarity) {
            case SIRADAN -> 7;
            case NADIR -> 10;
            case EPIK -> 5;
            case EFSANEVI -> 14;
            case MISTIK -> 13;
        };
    }

    @Override
    public void markEngaged(LivingEntity entity) {
        BossState state = activeBosses.get(entity.getUniqueId());
        if (state != null) {
            state.markEngaged();
        }
    }

    /**
     * Removes every living boss nobody ever hit, called at the moment a
     * replacement arrives.
     *
     * <p>A boss that has taken a player's hit is deliberately left alone even
     * though it is now the older of two: somebody is fighting it, and having it
     * evaporate mid-swing would be far worse than briefly having two on the
     * map. Bosses that outlived a restart are not in {@code activeBosses} at all
     * and are therefore never touched by this - the same honest limitation the
     * behaviour loop already carries.</p>
     */
    private void dismissUnengagedBosses() {
        for (Map.Entry<UUID, BossState> entry : Map.copyOf(activeBosses).entrySet()) {
            if (entry.getValue().isEngaged()) {
                continue;
            }
            Entity existing = Bukkit.getEntity(entry.getKey());
            retire(entry.getKey());
            if (existing != null) {
                existing.getScheduler().run(plugin, task -> {
                    existing.getWorld().spawnParticle(Particle.SMOKE, existing.getLocation().add(0, 1, 0),
                            30, 0.4D, 0.8D, 0.4D, 0.02D);
                    existing.remove();
                }, null);
            }
        }
    }

    /**
     * Walks candidate points in the configured ring around {@code anchor} until
     * one has standing room, then hands it to {@code onFound}.
     *
     * <p><strong>Each candidate's chunk is loaded asynchronously first.</strong>
     * The previous version simply skipped any candidate that was not already
     * loaded - and at 120 to 320 blocks out, almost nothing is: a boss summoned
     * from the admin panel would burn all twelve attempts on unloaded chunks and
     * vanish without a single message, which is exactly the "nothing happens"
     * the panel was reporting. Asking for the highest block in an unloaded chunk
     * synchronously is not the alternative - that generates terrain on the spot
     * and stalls the region - so the chunk is loaded through
     * {@link World#getChunkAtAsync(int, int)} the same way {@code /rtp} already
     * loads its landing spot.</p>
     */
    private void findSpawn(Player anchor, int attempt, CompletableFuture<Optional<String>> result, Consumer<Location> onFound) {
        if (attempt > SPAWN_ATTEMPTS) {
            result.complete(Optional.empty());
            return;
        }
        World world = anchor.getWorld();
        BossSettings current = settings();
        int x = anchor.getLocation().getBlockX() + signedOffset(current);
        int z = anchor.getLocation().getBlockZ() + signedOffset(current);
        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            Location standing = standingRoomAt(world, x, z);
            if (standing == null) {
                findSpawn(anchor, attempt + 1, result, onFound);
                return;
            }
            onFound.accept(standing);
        });
    }

    /** The open spot on top of the highest block at {@code (x, z)}, or {@code null} if a boss cannot stand there. */
    private Location standingRoomAt(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        if (y <= world.getMinHeight()) {
            return null;
        }
        if (!world.getBlockAt(x, y, z).getType().isSolid()
                || !world.getBlockAt(x, y + 1, z).getType().isAir()
                || !world.getBlockAt(x, y + 2, z).getType().isAir()) {
            return null;
        }
        return new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
    }

    /**
     * The health bar at the top of the screen, carrying the boss's name, its
     * remaining health and its rarity.
     *
     * <p>Viewers are whoever is within {@link #BAR_VIEW_RADIUS} of the boss,
     * recomputed on every behaviour tick - a server-wide bar would leave every
     * player who never went to the fight staring at a stranger's health for as
     * long as it lasted, and a bar attached at spawn only would never reach the
     * people who travelled to the announced coordinates.</p>
     */
    private BossBar createBossBar(Mob mob, BossRarity rarity, String name) {
        return BossBar.bossBar(barText(mob, rarity, name), 1f, rarity.barColor(), BossBar.Overlay.NOTCHED_10);
    }

    private Component barText(Mob mob, BossRarity rarity, String name) {
        AttributeInstance maximum = mob.getAttribute(Attribute.MAX_HEALTH);
        long maximumHealth = maximum == null ? Math.round(mob.getHealth()) : Math.round(maximum.getValue());
        return messages.render(rarity.bossBarKey(), Map.of(
                "isim", name,
                "can", Long.toString(Math.max(0L, Math.round(mob.getHealth()))),
                "maksimum", Long.toString(Math.max(1L, maximumHealth))
        ));
    }

    /**
     * Repaints one boss's bar and hands it to whoever is now close enough to
     * care. Only ever called from the boss's own entity scheduler, like every
     * other read of its health and position - which on Paper is the main
     * thread, so showing and hiding the bar directly here is the same
     * already-established call shape {@code BossBarFeedbackService} uses for
     * every XP bar.
     */
    private void updateBossBar(Mob mob, BossState state) {
        BossBar bar = state.bossBar();
        AttributeInstance maximum = mob.getAttribute(Attribute.MAX_HEALTH);
        double maximumHealth = maximum == null ? mob.getHealth() : maximum.getValue();
        bar.progress((float) Math.max(0.0D, Math.min(1.0D, maximumHealth <= 0 ? 0 : mob.getHealth() / maximumHealth)));
        bar.name(barText(mob, state.rarity(), state.name()));
        Set<UUID> nearby = new HashSet<>();
        for (Player player : mob.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(mob.getLocation()) <= BAR_VIEW_RADIUS * BAR_VIEW_RADIUS) {
                nearby.add(player.getUniqueId());
                if (state.viewers().add(player.getUniqueId())) {
                    player.showBossBar(bar);
                }
            }
        }
        state.viewers().removeIf(viewerId -> {
            if (nearby.contains(viewerId)) {
                return false;
            }
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null) {
                viewer.hideBossBar(bar);
            }
            return true;
        });
    }

    /** Drops a boss's bar from every screen still showing it, then forgets the boss. */
    @Override
    public void retire(UUID bossId) {
        BossState state = activeBosses.remove(bossId);
        if (state == null) {
            return;
        }
        state.viewers().forEach(viewerId -> {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null) {
                viewer.hideBossBar(state.bossBar());
            }
        });
        state.viewers().clear();
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
        makeSunproof(mob);
        // FLASH is a coloured particle on this server version and throws
        // outright without a Color payload - which is what killed the whole
        // spawn, after the creature had already been created. See BossRarity.
        mob.getWorld().spawnParticle(Particle.FLASH, mob.getLocation().add(0, 1, 0), 1, rarity.auraColor());
    }

    /**
     * Stops the morning from doing what a whole server could not.
     *
     * <p>A boss is announced with its coordinates and is meant to be travelled
     * to; an undead one spawned before dawn would simply burn to death on its
     * own before anybody arrived, and the announcement would have been a lie.
     * The per-type switch is what actually stops the fire, and the infinite
     * fire resistance is the belt to its braces - it also covers a boss that
     * wanders into lava on the way, and it covers any future archetype whose
     * type is not one of the two handled here.</p>
     */
    private void makeSunproof(Mob mob) {
        if (mob instanceof Zombie zombie) {
            zombie.setShouldBurnInDay(false);
        } else if (mob instanceof AbstractSkeleton skeleton) {
            skeleton.setShouldBurnInDay(false);
        }
        mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
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
        if (state == null) {
            return;
        }
        if (mob.isDead()) {
            retire(mob.getUniqueId());
            return;
        }
        updateBossBar(mob, state);
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
        private final BossBar bossBar;
        private final String name;
        private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
        private volatile long lastHealMillis;
        private volatile long lastTeleportMillis;
        private volatile boolean fled;
        private volatile boolean engaged;

        private BossState(BossRarity rarity, String name, BossBar bossBar) {
            this.rarity = rarity;
            this.name = name;
            this.bossBar = bossBar;
        }

        BossRarity rarity() {
            return rarity;
        }

        BossBar bossBar() {
            return bossBar;
        }

        String name() {
            return name;
        }

        Set<UUID> viewers() {
            return viewers;
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

        boolean isEngaged() {
            return engaged;
        }

        void markEngaged() {
            engaged = true;
        }
    }
}
