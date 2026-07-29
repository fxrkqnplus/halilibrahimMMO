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

    /**
     * Non-block items whose right click already does something of its own.
     * Bounded and hand-written for the same reason {@link #SINGLES} is: a new
     * Minecraft item changes what this plugin does only when someone decides
     * it should.
     */
    private static final Set<Material> USE_ITEMS = Set.of(
            Material.BUCKET, Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.POWDER_SNOW_BUCKET,
            Material.MILK_BUCKET, Material.COD_BUCKET, Material.SALMON_BUCKET, Material.TROPICAL_FISH_BUCKET,
            Material.PUFFERFISH_BUCKET, Material.AXOLOTL_BUCKET, Material.TADPOLE_BUCKET,
            Material.FLINT_AND_STEEL, Material.FIRE_CHARGE, Material.ENDER_EYE, Material.BONE_MEAL,
            Material.ARMOR_STAND, Material.ITEM_FRAME, Material.GLOW_ITEM_FRAME, Material.PAINTING,
            Material.LEAD, Material.NAME_TAG, Material.FIREWORK_ROCKET, Material.END_CRYSTAL,
            Material.MINECART, Material.CHEST_MINECART, Material.FURNACE_MINECART, Material.HOPPER_MINECART,
            Material.TNT_MINECART, Material.COMMAND_BLOCK_MINECART, Material.GLOW_INK_SAC, Material.INK_SAC,
            Material.HONEYCOMB, Material.SHEARS
    );

    private InteractableBlocks() {
        throw new UnsupportedOperationException("InteractableBlocks is a static utility class");
    }

    /**
     * Whether a right click holding {@code material} is the player trying to
     * <em>use the item</em> rather than trigger an ability.
     *
     * <p>Every block is a placement attempt, which is by far the most common
     * right click anyone performs - and the ability that suffers most is the one
     * whose tool is {@code ANY}, because there is no held item that rules it
     * out. Building a wall was spending Sürü Çağrısı's energy and starting its
     * cooldown once per two blocks laid.</p>
     *
     * <p>Unlike {@link #isInteractable}, sneaking is <strong>not</strong> an
     * escape hatch here: sneaking is exactly the gesture used to place a block
     * against a chest or a door, so honouring it would reopen the hole at the
     * one moment it is most likely to be hit. An ability held in a hand full of
     * blocks is still started by right-clicking open air, which places
     * nothing.</p>
     */
    public static boolean isUseItem(Material material) {
        if (material.isAir()) {
            // An empty hand places nothing and uses nothing; it is also the only
            // way to start a tool-agnostic ability while standing at a block.
            return false;
        }
        String name = material.name();
        return USE_ITEMS.contains(material)
                || name.endsWith("_SPAWN_EGG")
                || name.endsWith("_BOAT")
                || name.endsWith("_RAFT")
                || material.isBlock();
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
