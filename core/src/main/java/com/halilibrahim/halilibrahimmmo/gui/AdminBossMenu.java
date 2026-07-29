package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.boss.BossRarity;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The world-boss summoning screen.
 *
 * <p>The old panel had a single "summon" button that rolled everything itself.
 * That is the right default - and it is still what the first slot does - but it
 * is the wrong <em>only</em> option for staff running an event, who need to
 * promise a Mistik boss and then deliver one. So each rarity gets its own
 * button, and a name typed into chat can replace the one drawn from the
 * archetype's pool.</p>
 *
 * <p>A chosen name is kept only until the next summon, in memory, on this
 * screen. It is not a setting: naming a boss is something staff do for one
 * boss, and a name that outlived the button would surprise whoever pressed it
 * next.</p>
 */
public final class AdminBossMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int RANDOM_SLOT = 20;
    private static final int NAME_SLOT = 24;
    private static final int FIRST_RARITY_SLOT = 29;
    private static final int BACK_SLOT = 40;

    private final GuiServices services;
    private final Map<Integer, BossRarity> slotToRarity = new LinkedHashMap<>();
    private final String chosenName;

    public AdminBossMenu(GuiServices services) {
        this(services, null);
    }

    private AdminBossMenu(GuiServices services, String chosenName) {
        this.services = services;
        this.chosenName = chosenName;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ADMIN_BOSS_TITLE);
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.pane(Material.RED_STAINED_GLASS_PANE), GuiItems.filler());
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        inventory.setItem(HEADER_SLOT, GuiItems.of(Material.WITHER_SKELETON_SKULL,
                services.messages().render(MessageKey.GUI_ADMIN_BOSS_HEADER),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_BOSS_HEADER_LORE))));
        inventory.setItem(RANDOM_SLOT, GuiItems.of(Material.NETHER_STAR,
                services.messages().render(MessageKey.GUI_ADMIN_BOSS_RANDOM_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_BOSS_RANDOM_LORE))));
        inventory.setItem(NAME_SLOT, GuiItems.of(Material.NAME_TAG,
                services.messages().render(MessageKey.GUI_ADMIN_BOSS_NAME_ITEM),
                List.of(
                        services.messages().render(chosenName == null
                                        ? MessageKey.GUI_ADMIN_BOSS_NAME_RANDOM : MessageKey.GUI_ADMIN_BOSS_NAME_CHOSEN,
                                Map.of("isim", chosenName == null ? "" : chosenName)),
                        services.messages().render(MessageKey.GUI_ADMIN_BOSS_NAME_LORE)
                ), chosenName != null));

        int slot = FIRST_RARITY_SLOT;
        for (BossRarity rarity : BossRarity.values()) {
            slotToRarity.put(slot, rarity);
            List<Component> lore = new ArrayList<>();
            lore.add(services.messages().render(MessageKey.GUI_ADMIN_BOSS_RARITY_LORE));
            // The rarity's own nameplate template is what tells the eye which
            // tier this button is - the same colours the spawn broadcast uses.
            inventory.setItem(slot++, GuiItems.of(rarityIcon(rarity),
                    services.messages().render(rarity.nameplateKey(), Map.of("isim", "")), lore));
        }
    }

    private Material rarityIcon(BossRarity rarity) {
        return switch (rarity) {
            case SIRADAN -> Material.IRON_SWORD;
            case NADIR -> Material.EMERALD;
            case EPIK -> Material.AMETHYST_SHARD;
            case EFSANEVI -> Material.GOLD_INGOT;
            case MISTIK -> Material.NETHER_STAR;
        };
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AdminMenu(services));
            return;
        }
        if (slot == NAME_SLOT) {
            promptForName(player);
            return;
        }
        if (slot == RANDOM_SLOT) {
            summon(player, null);
            return;
        }
        BossRarity rarity = slotToRarity.get(slot);
        if (rarity != null) {
            summon(player, rarity);
        }
    }

    /**
     * A name is free text, which a menu cannot collect - the same reason
     * {@code /ev} and the admin wealth field take theirs from chat.
     */
    private void promptForName(Player player) {
        services.chatPrompts().await(player, MessageKey.ADMIN_BOSS_NAME_PROMPT, reply -> {
            String name = reply.strip();
            if (name.isEmpty() || name.length() > 32) {
                services.messages().send(player, MessageKey.ADMIN_BOSS_NAME_INVALID);
                GuiManager.open(player, new AdminBossMenu(services, chosenName));
                return;
            }
            services.messages().send(player, MessageKey.ADMIN_BOSS_NAME_SET, Map.of("isim", name));
            GuiManager.open(player, new AdminBossMenu(services, name));
        });
    }

    /**
     * The confirmation waits for the spawn to actually happen rather than for a
     * name to have been drawn: the search for somewhere to stand can fail, and
     * an admin told "summoned" who then sees no announcement, no bar and no
     * sound has been told something untrue.
     */
    private void summon(Player player, BossRarity rarity) {
        player.closeInventory();
        services.bosses().spawnNow(rarity, chosenName).thenAccept(name ->
                player.getScheduler().run(services.plugin(), task -> name.ifPresentOrElse(
                        spawned -> {
                            services.messages().send(player, MessageKey.ADMIN_BOSS_SPAWNED, Map.of("isim", spawned));
                            services.logger().info("Yonetici " + player.getName() + " elle bir dunya bossu cagirdi: "
                                    + spawned + " (" + (rarity == null ? "rastgele nadirlik" : rarity.name()) + ").");
                        },
                        () -> services.messages().send(player, MessageKey.ADMIN_BOSS_NO_ANCHOR)
                ), null));
    }
}
