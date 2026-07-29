package com.halilibrahim.halilibrahimmmo.backpack;

import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.FeatureUnlockRules;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<Inventory> open(Player player) {
        UUID playerId = player.getUniqueId();
        return repository.find(playerId).thenApply(stored -> {
            BackpackHolder holder = new BackpackHolder(playerId);
            Inventory inventory = Bukkit.createInventory(holder, SIZE, messages.render(MessageKey.GUI_BACKPACK_TITLE));
            holder.bind(inventory);
            stored.ifPresent(encoded -> inventory.setContents(decode(encoded)));
            return inventory;
        });
    }

    @Override
    public CompletableFuture<Void> save(UUID playerId, Inventory inventory) {
        return repository.save(playerId, encode(inventory.getContents()));
    }

    private String encode(ItemStack[] contents) {
        // Trimmed to the backpack's own size so a mis-sized inventory can never
        // grow the stored array, and empty slots are written as air rather than
        // as null, which the serialiser will not accept.
        ItemStack[] fixed = new ItemStack[SIZE];
        Arrays.setAll(fixed, slot -> slot < contents.length && contents[slot] != null
                ? contents[slot]
                : new ItemStack(org.bukkit.Material.AIR));
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
            return new ItemStack[SIZE];
        }
    }
}
