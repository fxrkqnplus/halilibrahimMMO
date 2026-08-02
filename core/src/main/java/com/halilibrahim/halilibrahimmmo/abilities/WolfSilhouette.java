package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.resourcepack.ItemModels;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws the Asil Kurt transformation as a set of {@link ItemDisplay} entities
 * carried by the player.
 *
 * <p><strong>Why this and not a model.</strong> §23.4 recorded that a werewolf
 * model could not be built, and a resource pack alone does not change that: a
 * player's geometry lives in the client's own {@code PlayerModel} /
 * {@code PlayerRenderer} code, and 26.2 reads no
 * {@code assets/minecraft/models/entity/**}. Entity models are not data-driven.
 * Repainting {@code textures/entity/wolf/wolf.png} would change every wolf in
 * the world for everybody, which is useless here. So the pack supplies three
 * silhouette <em>item</em> models and the server carries them around the
 * player: a head, a snout and a tail. Everyone on the server sees it, no
 * client mod is involved, and it reuses the {@code setItemModel} pipeline
 * this phase already built.</p>
 *
 * <p><strong>Every display must be accounted for.</strong> An {@link ItemDisplay}
 * left behind is permanent world litter that no player can remove. Four things
 * therefore remove them - the ability ending, the player quitting, the player
 * dying, and the plugin disabling - and because none of those survives a crash,
 * every display is also stamped with {@link #MARKER_KEY} so {@link #sweep(JavaPlugin)}
 * can clear orphans from a previous run at startup.</p>
 */
public final class WolfSilhouette {

    /** Stamped on every display so a crashed server's leftovers are still identifiable. */
    private static final String MARKER_KEY = "asil-kurt-siluet";

    /** Parts, in the order they are spawned; each names a model the pack ships. */
    private static final List<Part> PARTS = List.of(
            new Part("kurt-kafa", 0.0F, 2.35F, 0.0F, 0.85F),
            new Part("kurt-burun", 0.0F, 2.15F, 0.42F, 0.55F),
            new Part("kurt-kuyruk", 0.0F, 1.10F, -0.55F, 0.70F)
    );

    private final JavaPlugin plugin;
    private final NamespacedKey marker;
    private final Map<UUID, List<UUID>> displays = new ConcurrentHashMap<>();

    public WolfSilhouette(JavaPlugin plugin) {
        this.plugin = plugin;
        this.marker = new NamespacedKey(plugin, MARKER_KEY);
    }

    /** @param yOffset height above the player's feet; {@code zOffset} is forward. */
    private record Part(String model, float xOffset, float yOffset, float zOffset, float scale) {
    }

    /**
     * Spawns the silhouette, replacing any the player already has. Idempotent on
     * purpose: an ability re-armed before its previous run finished must not
     * leave a second set behind.
     */
    public void show(Player player) {
        hide(player);

        List<UUID> spawned = new ArrayList<>(PARTS.size());
        for (Part part : PARTS) {
            ItemDisplay display = player.getWorld().spawn(anchor(player, part), ItemDisplay.class, entity -> {
                entity.setItemStack(itemFor(part));
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 0, 1),
                        new Vector3f(part.scale(), part.scale(), part.scale()),
                        new AxisAngle4f(0, 0, 0, 1)
                ));
                // Short interpolation so the parts glide with the player instead
                // of snapping once per upkeep tick.
                entity.setInterpolationDuration(4);
                entity.setTeleportDuration(4);
                entity.setPersistent(false);
                entity.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
            });
            spawned.add(display.getUniqueId());
        }
        displays.put(player.getUniqueId(), spawned);
    }

    /** Keeps the parts with the player; called from the ability's existing upkeep tick. */
    public void follow(Player player) {
        List<UUID> ids = displays.get(player.getUniqueId());
        if (ids == null) {
            return;
        }
        for (int index = 0; index < ids.size() && index < PARTS.size(); index++) {
            Entity entity = plugin.getServer().getEntity(ids.get(index));
            if (entity instanceof ItemDisplay display && display.isValid()) {
                display.teleport(anchor(player, PARTS.get(index)));
            }
        }
    }

    /** Removes this player's silhouette. Safe to call when there is none. */
    public void hide(Player player) {
        hide(player.getUniqueId());
    }

    public void hide(UUID playerId) {
        List<UUID> ids = displays.remove(playerId);
        if (ids == null) {
            return;
        }
        for (UUID id : ids) {
            Entity entity = plugin.getServer().getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    /** Removes every silhouette this instance owns; called on plugin shutdown. */
    public void clear() {
        for (UUID playerId : List.copyOf(displays.keySet())) {
            hide(playerId);
        }
    }

    /**
     * Removes marked displays left over from a previous run.
     *
     * <p>The four removal paths all require the server to still be alive. A
     * crash or a hard kill bypasses every one of them, and the leftovers would
     * then be invisible litter forever - so startup sweeps for the marker. The
     * displays are also spawned non-persistent, which handles the common case;
     * this catches the rest.</p>
     */
    public void sweep(JavaPlugin owner) {
        int removed = 0;
        for (World world : owner.getServer().getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                if (display.getPersistentDataContainer().has(marker, PersistentDataType.BYTE)) {
                    display.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            owner.getSLF4JLogger().info("Onceki oturumdan kalan {} Asil Kurt siluet parcasi temizlendi.", removed);
        }
    }

    private Location anchor(Player player, Part part) {
        Location base = player.getLocation();
        Location location = base.clone().add(
                base.getDirection().normalize().multiply(part.zOffset()));
        location.add(part.xOffset(), part.yOffset(), 0);
        location.setYaw(base.getYaw());
        location.setPitch(0);
        return location;
    }

    /**
     * The item is only a carrier: what the client actually draws is the model the
     * pack supplies. A player without the pack sees the fallback material, which
     * is why it is a bone rather than a barrier - wrong, but not nonsense.
     */
    private ItemStack itemFor(Part part) {
        ItemStack item = new ItemStack(Material.BONE);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(ItemModels.of(part.model()));
        item.setItemMeta(meta);
        return item;
    }
}
