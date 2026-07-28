package com.halilibrahim.halilibrahimmmo.abilities;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Set;

/**
 * Which blocks already answer a right click with something of their own.
 *
 * <p>Abilities are triggered by right clicking, and a player right clicks
 * constantly for reasons that have nothing to do with abilities: opening a
 * door, flipping a lever, putting an item in a furnace. Every one of those was
 * also starting the confirm-and-activate flow, so a trip through a base spent
 * energy and cooldowns on doors. This is the list that stops it.</p>
 *
 * <p>Deliberately an explicit, bounded list rather than
 * {@code Material#isInteractable()}: that method is documented as not
 * comprehensive, and a hand-written list is the same discipline every other
 * bounded value in this project follows - a new Minecraft block changes what
 * this plugin does only when someone decides it should.</p>
 *
 * <p><strong>Sneaking is the escape hatch.</strong> Vanilla already suppresses
 * a block's own right-click behaviour while the player is sneaking, so a player
 * standing at a door who genuinely wants their ability holds shift - the same
 * gesture they already use to place a block against a chest.</p>
 */
public final class InteractableBlocks {

    /**
     * Blocks with no {@link Tag} family of their own. Containers, workstations
     * and the handful of one-off blocks that open a screen or change state on
     * click.
     */
    private static final Set<Material> SINGLES = Set.of(
            Material.LEVER, Material.CRAFTING_TABLE, Material.CRAFTER, Material.FURNACE, Material.BLAST_FURNACE,
            Material.SMOKER, Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.BARREL,
            Material.HOPPER, Material.DROPPER, Material.DISPENSER, Material.BREWING_STAND, Material.ENCHANTING_TABLE,
            Material.GRINDSTONE, Material.STONECUTTER, Material.LOOM, Material.CARTOGRAPHY_TABLE,
            Material.SMITHING_TABLE, Material.FLETCHING_TABLE, Material.LECTERN, Material.BEACON, Material.JUKEBOX,
            Material.NOTE_BLOCK, Material.COMPOSTER, Material.BELL, Material.RESPAWN_ANCHOR, Material.BEEHIVE,
            Material.BEE_NEST, Material.DECORATED_POT, Material.CHISELED_BOOKSHELF, Material.VAULT,
            Material.DAYLIGHT_DETECTOR, Material.REPEATER, Material.COMPARATOR, Material.CAKE, Material.LODESTONE,
            Material.DRAGON_EGG, Material.SWEET_BERRY_BUSH, Material.CAVE_VINES, Material.CAVE_VINES_PLANT
    );

    private InteractableBlocks() {
        throw new UnsupportedOperationException("InteractableBlocks is a static utility class");
    }

    public static boolean isInteractable(Material material) {
        return SINGLES.contains(material)
                || Tag.DOORS.isTagged(material)
                || Tag.TRAPDOORS.isTagged(material)
                || Tag.FENCE_GATES.isTagged(material)
                || Tag.BUTTONS.isTagged(material)
                || Tag.BEDS.isTagged(material)
                || Tag.SHULKER_BOXES.isTagged(material)
                || Tag.ANVIL.isTagged(material)
                || Tag.CAULDRONS.isTagged(material)
                || Tag.CANDLES.isTagged(material)
                || Tag.CANDLE_CAKES.isTagged(material)
                || Tag.FLOWER_POTS.isTagged(material)
                || Tag.CAMPFIRES.isTagged(material)
                || Tag.ALL_SIGNS.isTagged(material);
    }
}
