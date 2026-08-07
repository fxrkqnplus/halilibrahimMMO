package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One online player's inventory, exactly as they are carrying it, with their
 * armour, their off hand, and how they are doing.
 *
 * <p><strong>A copy that is written through, not a live window.</strong>
 * Bukkit can hand a second player somebody's real {@link PlayerInventory} to
 * open, and doing that is how most plugins implement this - but it makes every
 * vanilla inventory rule apply at once: shift-clicking pulls stacks through
 * armour slots, the target can move an item out from under the staff member
 * mid-click, and closing the window while a stack is on the cursor drops it into
 * whichever of the two players happens to be holding it. This screen instead
 * paints a snapshot and performs each transfer itself, which is the same shape
 * the trade screen has used since Faz 16 and for the same reason: one place
 * decides where an item ends up.</p>
 *
 * <p><strong>Both directions, one gesture.</strong> Clicking an item in the
 * upper board takes it - it leaves the target and arrives in the staff member's
 * own inventory. Clicking an item in the staff member's own inventory gives it -
 * it leaves them and arrives in the target's. Whole stacks either way: half
 * stacks are a shopkeeper's problem, and an admin confiscating something wants
 * all of it.</p>
 *
 * <p><strong>Nothing is ever destroyed.</strong> Every transfer checks that the
 * receiving side has room <em>before</em> the giving side loses anything, and
 * refuses with a message when it does not. A target who logs out mid-screen
 * leaves the board showing what they had and every click answered with "not
 * online" - the screen never writes into a stale inventory.</p>
 */
public final class AdminInventoryMenu implements GuiView {

    /** Rows 0-2 hold the target's storage (their slots 9-35), row 3 their hotbar (0-8). */
    private static final int STORAGE_SLOTS = 27;
    private static final int HOTBAR_ROW_START = 27;
    /** Row 4: armour, off hand, and the three status readings. */
    private static final int HELMET_SLOT = 36;
    private static final int CHESTPLATE_SLOT = 37;
    private static final int LEGGINGS_SLOT = 38;
    private static final int BOOTS_SLOT = 39;
    private static final int OFFHAND_SLOT = 40;
    private static final int STATUS_SLOT = 42;
    private static final int HINT_SLOT = 43;
    private static final int REFRESH_SLOT = 44;
    private static final int BACK_SLOT = 49;

    private final GuiServices services;
    private final UUID targetId;
    private final String targetName;
    private final boolean fromOnlineList;

    public AdminInventoryMenu(GuiServices services, UUID targetId, String targetName, boolean fromOnlineList) {
        this.services = services;
        this.targetId = targetId;
        this.targetName = targetName;
        this.fromOnlineList = fromOnlineList;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ADMIN_INVENTORY_TITLE, Map.of("oyuncu", targetName));
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            renderOffline(inventory);
            return;
        }
        PlayerInventory carried = target.getInventory();
        // The target's storage first, then their hotbar underneath it - the same
        // order they see it in, so a staff member and the player they are looking
        // at are describing the same square when they talk about "the third slot".
        for (int index = 0; index < STORAGE_SLOTS; index++) {
            inventory.setItem(index, describe(carried.getItem(index + 9)));
        }
        for (int index = 0; index < 9; index++) {
            inventory.setItem(HOTBAR_ROW_START + index, describe(carried.getItem(index)));
        }
        inventory.setItem(HELMET_SLOT, equipment(carried.getHelmet(), MessageKey.GUI_ADMIN_INVENTORY_SLOT_HELMET));
        inventory.setItem(CHESTPLATE_SLOT, equipment(carried.getChestplate(), MessageKey.GUI_ADMIN_INVENTORY_SLOT_CHESTPLATE));
        inventory.setItem(LEGGINGS_SLOT, equipment(carried.getLeggings(), MessageKey.GUI_ADMIN_INVENTORY_SLOT_LEGGINGS));
        inventory.setItem(BOOTS_SLOT, equipment(carried.getBoots(), MessageKey.GUI_ADMIN_INVENTORY_SLOT_BOOTS));
        inventory.setItem(OFFHAND_SLOT, equipment(carried.getItemInOffHand(), MessageKey.GUI_ADMIN_INVENTORY_SLOT_OFFHAND));

        inventory.setItem(STATUS_SLOT, GuiItems.playerHead(target,
                services.messages().render(MessageKey.GUI_ADMIN_INVENTORY_STATUS, Map.of("oyuncu", target.getName())),
                statusLore(target)));
        inventory.setItem(HINT_SLOT, GuiItems.of(Material.BOOK,
                services.messages().render(MessageKey.GUI_ADMIN_INVENTORY_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_INVENTORY_HINT))));
        inventory.setItem(REFRESH_SLOT, GuiItems.of(Material.LIME_DYE,
                services.messages().render(MessageKey.GUI_ADMIN_INVENTORY_REFRESH)));
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
    }

    private void renderOffline(Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.pane(Material.RED_STAINED_GLASS_PANE), GuiItems.filler());
        inventory.setItem(STATUS_SLOT, GuiItems.of(Material.BARRIER,
                services.messages().render(MessageKey.GUI_ADMIN_INVENTORY_OFFLINE, Map.of("oyuncu", targetName))));
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
    }

    /**
     * The player's own item, untouched.
     *
     * <p>Deliberately <em>not</em> re-labelled or re-loremmed: staff are looking
     * at this screen to see what somebody is actually carrying, and an item whose
     * name this screen rewrote would be a different item from the one in the
     * report. An empty slot stays empty rather than being filled with a pane -
     * a board of grey glass hides the shape of an inventory, which is half of
     * what is being read here.</p>
     */
    private static ItemStack describe(ItemStack carried) {
        return carried == null || carried.getType().isAir() ? null : carried.clone();
    }

    /**
     * One equipment slot: the item if there is one, an empty marker if not, and
     * the durability underneath either way.
     */
    private ItemStack equipment(ItemStack worn, MessageKey slotName) {
        if (worn == null || worn.getType().isAir()) {
            return GuiItems.of(Material.GRAY_STAINED_GLASS_PANE, services.messages().render(slotName));
        }
        ItemStack shown = worn.clone();
        ItemMeta meta = shown.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(services.messages().render(slotName));
            durabilityLine(worn).ifPresent(lore::add);
            meta.lore(lore);
            shown.setItemMeta(meta);
        }
        return shown;
    }

    /**
     * How much life is left in a tool or a piece of armour.
     *
     * <p>Read off the item's own damage rather than from any cached number, and
     * absent entirely for something that cannot break - a stack of blocks has no
     * durability and inventing "∞" for it would be one more line to read past on
     * every screen.</p>
     */
    private java.util.Optional<Component> durabilityLine(ItemStack item) {
        int maximum = item.getType().getMaxDurability();
        if (maximum <= 0 || !(item.getItemMeta() instanceof Damageable damageable)) {
            return java.util.Optional.empty();
        }
        int remaining = Math.max(0, maximum - damageable.getDamage());
        return java.util.Optional.of(services.messages().render(MessageKey.GUI_ADMIN_INVENTORY_DURABILITY, Map.of(
                "kalan", Integer.toString(remaining),
                "toplam", Integer.toString(maximum),
                "yuzde", Integer.toString((int) Math.round(remaining * 100.0D / maximum))
        )));
    }

    private List<Component> statusLore(Player target) {
        AttributeInstance maximum = target.getAttribute(Attribute.MAX_HEALTH);
        double maximumHealth = maximum == null ? 20.0D : maximum.getValue();
        return List.of(
                services.messages().render(MessageKey.GUI_ADMIN_INVENTORY_STATUS_HEALTH, Map.of(
                        "can", Integer.toString((int) Math.ceil(target.getHealth())),
                        "maksimum", Integer.toString((int) Math.ceil(maximumHealth))
                )),
                services.messages().render(MessageKey.GUI_ADMIN_INVENTORY_STATUS_HUNGER, Map.of(
                        "aclik", Integer.toString(target.getFoodLevel()),
                        "doyum", Integer.toString((int) target.getSaturation())
                )),
                services.messages().render(MessageKey.GUI_ADMIN_INVENTORY_STATUS_ARMOR, Map.of(
                        "zirh", Integer.toString(armorPoints(target))
                )),
                services.messages().render(MessageKey.GUI_ADMIN_INVENTORY_STATUS_LOCATION, Map.of(
                        "dunya", target.getWorld().getName(),
                        "x", Integer.toString(target.getLocation().getBlockX()),
                        "y", Integer.toString(target.getLocation().getBlockY()),
                        "z", Integer.toString(target.getLocation().getBlockZ())
                ))
        );
    }

    /** Armour points as the HUD shows them, read off the attribute so enchantments and modifiers count. */
    private static int armorPoints(Player target) {
        AttributeInstance armor = target.getAttribute(Attribute.ARMOR);
        return armor == null ? 0 : (int) Math.round(armor.getValue());
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName, fromOnlineList));
            return;
        }
        if (slot == REFRESH_SLOT) {
            GuiManager.open(player, reopened());
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            services.messages().send(player, MessageKey.GUI_ADMIN_INVENTORY_OFFLINE, Map.of("oyuncu", targetName));
            return;
        }
        int carriedSlot = toInventorySlot(slot);
        if (carriedSlot < 0) {
            return;
        }
        take(player, target, carriedSlot);
    }

    /**
     * Which of the target's own inventory slots this board square stands for, or
     * {@code -1} for a square that is not one of theirs.
     *
     * <p>Bukkit numbers a player inventory 0-8 hotbar, 9-35 storage, 36-39 boots
     * to helmet, 40 off hand. The board is laid out storage-then-hotbar because
     * that is how a player sees it, so this is the one place the two orders are
     * reconciled.</p>
     */
    private static int toInventorySlot(int boardSlot) {
        if (boardSlot >= 0 && boardSlot < STORAGE_SLOTS) {
            return boardSlot + 9;
        }
        if (boardSlot >= HOTBAR_ROW_START && boardSlot < HOTBAR_ROW_START + 9) {
            return boardSlot - HOTBAR_ROW_START;
        }
        return switch (boardSlot) {
            case BOOTS_SLOT -> 36;
            case LEGGINGS_SLOT -> 37;
            case CHESTPLATE_SLOT -> 38;
            case HELMET_SLOT -> 39;
            case OFFHAND_SLOT -> 40;
            default -> -1;
        };
    }

    /** Target → staff. Refuses before taking anything if there is nowhere to put it. */
    private void take(Player player, Player target, int carriedSlot) {
        ItemStack held = target.getInventory().getItem(carriedSlot);
        if (held == null || held.getType().isAir()) {
            return;
        }
        ItemStack moved = held.clone();
        if (!player.getInventory().addItem(moved).isEmpty()) {
            services.messages().send(player, MessageKey.GUI_ADMIN_INVENTORY_SELF_FULL);
            // Whatever did fit has already been added, so the target loses
            // exactly that much and keeps the rest - counted rather than assumed.
            int leftBehind = countOf(player, moved);
            reduce(target, carriedSlot, held.getAmount() - leftBehind);
            GuiManager.open(player, reopened());
            return;
        }
        target.getInventory().setItem(carriedSlot, null);
        target.updateInventory();
        announce(player, MessageKey.GUI_ADMIN_INVENTORY_TAKEN, target.getName(), moved);
        GuiManager.open(player, reopened());
    }

    /** Staff → target. Same refusal rule, in the other direction. */
    @Override
    public void handleOwnInventoryClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            services.messages().send(player, MessageKey.GUI_ADMIN_INVENTORY_OFFLINE, Map.of("oyuncu", targetName));
            return;
        }
        ItemStack moved = clicked.clone();
        Map<Integer, ItemStack> refused = target.getInventory().addItem(moved);
        if (!refused.isEmpty()) {
            services.messages().send(player, MessageKey.GUI_ADMIN_INVENTORY_TARGET_FULL);
            // Give back whatever would not fit, so nothing is lost between the
            // two inventories even when the target is nearly full.
            refused.values().forEach(left -> player.getInventory().addItem(left));
            GuiManager.open(player, reopened());
            return;
        }
        player.getInventory().setItem(slot, null);
        target.updateInventory();
        announce(player, MessageKey.GUI_ADMIN_INVENTORY_GIVEN, target.getName(), moved);
        GuiManager.open(player, reopened());
    }

    /** How many of this stack's type the player is now carrying, used to work out a partial transfer. */
    private static int countOf(Player player, ItemStack sample) {
        int total = 0;
        for (ItemStack candidate : player.getInventory().getContents()) {
            if (candidate != null && candidate.isSimilar(sample)) {
                total += candidate.getAmount();
            }
        }
        return total;
    }

    private static void reduce(Player target, int carriedSlot, int taken) {
        ItemStack held = target.getInventory().getItem(carriedSlot);
        if (held == null || taken <= 0) {
            return;
        }
        int left = held.getAmount() - taken;
        target.getInventory().setItem(carriedSlot, left > 0 ? held.asQuantity(left) : null);
        target.updateInventory();
    }

    private void announce(Player player, MessageKey key, String targetName, ItemStack moved) {
        services.messages().send(player, key, Map.of(
                "esya", moved.getType().name(),
                "adet", Integer.toString(moved.getAmount()),
                "oyuncu", targetName
        ));
    }

    private AdminInventoryMenu reopened() {
        return new AdminInventoryMenu(services, targetId, targetName, fromOnlineList);
    }
}
