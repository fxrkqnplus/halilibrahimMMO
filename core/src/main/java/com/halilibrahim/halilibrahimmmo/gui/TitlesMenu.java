package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
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
import java.util.Optional;
import java.util.Set;

/**
 * Title unlock status and equip action - the only way to equip one, opened by
 * {@code /ünvan}.
 *
 * <p>Rebuilt from a 27-slot grid that had stopped fitting its own catalogue.
 * Titles grew from five to nineteen and the entries were written from slot 10
 * onwards with no upper bound: the back button at slot 26 was painted over by a
 * title, and everything past slot 26 was written past the end of the inventory
 * altogether. Now it is a framed 54-slot board with a fixed 28-slot entry area
 * and the navigation kept out of it.</p>
 *
 * <p><strong>A locked title shows no name.</strong> It renders as a plain grey
 * placeholder carrying only what would unlock it - a title is worn, and reading
 * the whole list off a menu before earning any of it spends the reveal for
 * nothing.</p>
 */
public final class TitlesMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int BACK_SLOT = 49;
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final GuiServices services;
    private final Map<Integer, String> slotToTitleId = new LinkedHashMap<>();

    public TitlesMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_TITLES_TITLE);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.border(), GuiItems.filler());
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.titles().unlockedIds(player.getUniqueId())
                .thenCompose(unlocked -> services.titles().findEquipped(player.getUniqueId())
                        .thenAccept(equipped -> player.getScheduler().run(services.plugin(), task ->
                                render(player, inventory, unlocked, equipped), null)));
    }

    private void render(Player player, Inventory inventory, Set<String> unlocked, Optional<TitleDefinition> equipped) {
        List<TitleDefinition> definitions = services.titles().definitions().stream()
                .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
        String equippedId = equipped.map(TitleDefinition::id).orElse("");

        inventory.setItem(HEADER_SLOT, GuiItems.playerHead(
                player,
                services.messages().render(MessageKey.GUI_TITLES_HEADER_ITEM),
                List.of(services.messages().render(MessageKey.GUI_TITLES_HEADER_LORE, Map.of(
                        "acik", Integer.toString(unlocked.size()),
                        "toplam", Integer.toString(definitions.size())
                )))
        ));

        slotToTitleId.clear();
        for (int index = 0; index < ENTRY_SLOTS.length && index < definitions.size(); index++) {
            TitleDefinition definition = definitions.get(index);
            boolean isUnlocked = unlocked.contains(definition.id());
            if (isUnlocked) {
                // Only an unlocked title is clickable, so only it is remembered.
                slotToTitleId.put(ENTRY_SLOTS[index], definition.id());
            }
            inventory.setItem(ENTRY_SLOTS[index], entryItem(definition, isUnlocked, definition.id().equals(equippedId)));
        }
    }

    private ItemStack entryItem(TitleDefinition definition, boolean unlocked, boolean equippedNow) {
        if (!unlocked) {
            List<Component> lore = new ArrayList<>();
            lore.add(services.messages().render(MessageKey.GUI_TITLE_ITEM_LORE_LOCKED));
            lore.add(UnlockRequirementLore.of(services, definition.condition()));
            return GuiItems.of(Material.GRAY_DYE, services.messages().render(MessageKey.GUI_TITLE_ITEM_LOCKED), lore);
        }
        return GuiItems.of(
                definition.icon(),
                services.messages().render(MessageKey.GUI_TITLE_ITEM, Map.of("unvan", definition.displayName())),
                List.of(services.messages().render(equippedNow
                        ? MessageKey.GUI_TITLE_ITEM_LORE_EQUIPPED : MessageKey.GUI_TITLE_ITEM_LORE_UNLOCKED)),
                equippedNow,
                definition.model()
        );
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new CommunityMenu(services));
            return;
        }
        String titleId = slotToTitleId.get(slot);
        if (titleId == null) {
            return;
        }
        services.titles().equip(player.getUniqueId(), player.getName(), titleId).thenAccept(result ->
                player.getScheduler().run(services.plugin(), task -> {
                    MessageKey key = switch (result.status()) {
                        case EQUIPPED -> MessageKey.TITLE_EQUIPPED;
                        case ALREADY_EQUIPPED -> MessageKey.TITLE_ALREADY_EQUIPPED;
                        case NOT_UNLOCKED -> MessageKey.TITLE_NOT_UNLOCKED;
                        case UNKNOWN_TITLE -> MessageKey.TITLE_UNKNOWN;
                    };
                    services.messages().send(player, key, Map.of("unvan", displayNameOrId(result.titleId())));
                    GuiManager.open(player, new TitlesMenu(services));
                }, null));
    }

    private String displayNameOrId(String titleId) {
        TitleDefinition definition = services.titles().settings().definitions().get(titleId);
        return definition == null ? titleId : definition.displayName();
    }
}
