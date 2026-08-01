package com.halilibrahim.halilibrahimmmo.backpack;

import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.FeatureUnlockRules;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database-backed {@link BackpackService}.
 *
 * <p>Contents are serialised with Paper's own
 * {@link ItemStack#serializeItemsAsBytes(ItemStack[])}, which round-trips every
 * component an item can carry - enchantments, custom names, written books,
 * shulker contents - rather than the handful of fields a hand-written schema
 * would have thought of. The bytes are Base64-wrapped so the column can be
 * plain text on all four supported engines.</p>
 *
 * <p><strong>Deliberately cache-free.</strong> Every other per-player service
 * here keeps a write-through cache, but those hold progression numbers where a
 * stale read costs a little XP. This holds items: the only acceptable answer to
 * "what is in my backpack" is what the database says, and reading it on open
 * and writing it on close is both simple and exactly right.</p>
 */
public final class DatabaseBackpackService implements BackpackService {

    private final JavaPlugin plugin;
    private final BackpackRepository repository;
    private final ProfileService profiles;
    private final ClassService classes;
    private final MessageService messages;
    /**
     * The one thing about a backpack that <em>is</em> cached: how wide it is.
     * Unlike the contents, a stale answer here cannot lose anything - and the
     * window has to be sized before the contents are even fetched.
     */
    private final ConcurrentHashMap<UUID, Integer> upgradeCache = new ConcurrentHashMap<>();

    public DatabaseBackpackService(
            JavaPlugin plugin, BackpackRepository repository, ProfileService profiles,
            ClassService classes, MessageService messages
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.profiles = profiles;
        this.classes = classes;
        this.messages = messages;
    }

    @Override
    public void enable() {
        // Nothing to warm: contents are read on open and written on close.
    }

    @Override
    public void disable() {
        // Every open backpack is saved by its own close event, which the server
        // raises for each player before the plugin is disabled.
    }

    @Override
    public boolean isUnlocked(UUID playerId) {
        PlayerProfile profile = profiles.cached(playerId).orElse(null);
        if (profile == null) {
            return false;
        }
        String classId = profile.selectedClass();
        int classLevel = classId.isBlank() ? 0 : classes.classProgress(playerId, classId).level();
        return FeatureUnlockRules.isUnlocked(classLevel, profile.prestige(), FeatureUnlockRules.BACKPACK_CLASS_LEVEL);
    }

    @Override
    public int upgrades(UUID playerId) {
        return upgradeCache.getOrDefault(playerId, 0);
    }

    @Override
    public int capacity(UUID playerId) {
        return BASE_SIZE + upgrades(playerId) * SLOTS_PER_UPGRADE;
    }

    @Override
    public CompletableFuture<Integer> loadUpgrades(UUID playerId) {
        Integer cached = upgradeCache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.findUpgrades(playerId).thenApply(count -> {
            int clamped = Math.max(0, Math.min(MAXIMUM_UPGRADES, count));
            upgradeCache.put(playerId, clamped);
            return clamped;
        });
    }

    @Override
    public CompletableFuture<Integer> purchaseUpgrade(UUID playerId) {
        return loadUpgrades(playerId).thenCompose(current -> {
            if (current >= MAXIMUM_UPGRADES) {
                return CompletableFuture.completedFuture(current);
            }
            int next = current + 1;
            return repository.saveUpgrades(playerId, next).thenApply(ignored -> {
                upgradeCache.put(playerId, next);
                return next;
            });
        });
    }

    @Override
    public void evict(UUID playerId) {
        upgradeCache.remove(playerId);
    }

    /**
     * Opens the window at this player's own size.
     *
     * <p>The inventory itself is always a whole number of rows, because
     * Minecraft cannot make a 25-slot chest; the slots past the capacity carry a
     * blocked pane and {@code BackpackListener} refuses clicks on them. That
     * pane is placed <em>after</em> the stored contents, so a player who somehow
     * has items in a slot they no longer own still sees them rather than having
     * them quietly covered - the slot is unusable, not emptied.</p>
     */
    @Override
    public CompletableFuture<Inventory> open(Player player) {
        UUID playerId = player.getUniqueId();
        return loadUpgrades(playerId).thenCompose(ignored -> repository.find(playerId)).thenApply(stored -> {
            int capacity = capacity(playerId);
            int windowSize = BackpackService.windowSize(capacity);
            BackpackHolder holder = new BackpackHolder(playerId, capacity);
            Inventory inventory = Bukkit.createInventory(holder, windowSize, messages.render(MessageKey.GUI_BACKPACK_TITLE));
            holder.bind(inventory);
            stored.ifPresent(encoded -> {
                ItemStack[] contents = decode(encoded);
                for (int slot = 0; slot < capacity && slot < contents.length; slot++) {
                    inventory.setItem(slot, contents[slot]);
                }
            });
            ItemStack blocked = blockedSlotItem();
            for (int slot = capacity; slot < windowSize; slot++) {
                inventory.setItem(slot, blocked);
            }
            return inventory;
        });
    }

    /**
     * What fills a slot the player has not bought yet: a red pane naming the
     * talent that opens it, so the empty half of the window explains itself.
     */
    private ItemStack blockedSlotItem() {
        ItemStack pane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(messages.render(MessageKey.GUI_BACKPACK_LOCKED_SLOT));
        meta.lore(List.of(messages.render(MessageKey.GUI_BACKPACK_LOCKED_SLOT_LORE, Map.of(
                "slot", Integer.toString(SLOTS_PER_UPGRADE)
        ))));
        pane.setItemMeta(meta);
        return pane;
    }

    @Override
    public CompletableFuture<Void> save(UUID playerId, Inventory inventory) {
        return repository.save(playerId, encode(playerId, inventory.getContents()));
    }

    private String encode(UUID playerId, ItemStack[] contents) {
        // Trimmed to the backpack's own capacity so the blocked panes are never
        // stored as belongings, and empty slots are written as air rather than
        // as null, which the serialiser will not accept.
        ItemStack[] fixed = new ItemStack[capacity(playerId)];
        Arrays.setAll(fixed, slot -> slot < contents.length && contents[slot] != null
                ? contents[slot]
                : new ItemStack(Material.AIR));
        return Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(fixed));
    }

    /**
     * A row this server can no longer read is left alone rather than replaced
     * with an empty backpack: handing the player an empty chest would look
     * exactly like their belongings being deleted, and the next close would
     * make that true. Logging it and opening empty-but-unsaved is the honest
     * failure - the row is still there for an admin to look at.
     */
    private ItemStack[] decode(String encoded) {
        try {
            return ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(encoded));
        } catch (RuntimeException exception) {
            plugin.getSLF4JLogger().warn("Bir oyuncunun canta icerigi okunamadi, bos acilacak.", exception);
            return new ItemStack[0];
        }
    }
}
