package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.collections.CollectionDefinition;
import com.halilibrahim.halilibrahimmmo.collections.CollectionProgress;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only list of every configured collection; click one to open its checklist. */
public final class CollectionsMenu implements GuiView {

    private static final int BACK_SLOT = 26;

    private final GuiServices services;
    private final Map<Integer, String> slotToCollectionId = new LinkedHashMap<>();

    public CollectionsMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_COLLECTIONS_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.collections().loadAll(player.getUniqueId()).thenAccept(progressById ->
                player.getScheduler().run(services.plugin(), task -> {
                    List<CollectionDefinition> definitions = services.collections().definitions().stream()
                            .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
                    int slot = 0;
                    for (CollectionDefinition definition : definitions) {
                        CollectionProgress progress = progressById.get(definition.id());
                        slotToCollectionId.put(slot, definition.id());
                        inventory.setItem(slot++, GuiItems.of(
                                Material.CHEST,
                                services.messages().render(MessageKey.GUI_COLLECTION_ITEM, Map.of("koleksiyon", definition.displayName())),
                                List.of(services.messages().render(MessageKey.GUI_COLLECTION_ITEM_LORE, Map.of(
                                        "ilerleme", progress.obtainedCount() + "/" + definition.entries().size()
                                )))
                        ));
                    }
                }, null));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new TasksMenu(services));
            return;
        }
        String collectionId = slotToCollectionId.get(slot);
        if (collectionId != null) {
            GuiManager.open(player, new CollectionDetailMenu(services, collectionId));
        }
    }
}
