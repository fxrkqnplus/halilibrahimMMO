package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.collections.CollectionDefinition;
import com.halilibrahim.halilibrahimmmo.collections.CollectionEntryDefinition;
import com.halilibrahim.halilibrahimmmo.collections.CollectionProgress;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Per-entry obtained/missing checklist for one configured collection, opened from {@link CollectionsMenu}. */
public final class CollectionDetailMenu implements GuiView {

    private static final int BACK_SLOT = 26;

    private final GuiServices services;
    private final String collectionId;

    public CollectionDetailMenu(GuiServices services, String collectionId) {
        this.services = services;
        this.collectionId = collectionId;
    }

    @Override
    public Component title() {
        CollectionDefinition definition = services.collections().settings().definitions().get(collectionId);
        String name = definition == null ? collectionId : definition.displayName();
        return services.messages().render(MessageKey.GUI_COLLECTION_DETAIL_TITLE, Map.of("koleksiyon", name));
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        CollectionDefinition definition = services.collections().settings().definitions().get(collectionId);
        if (definition == null) {
            return;
        }
        services.collections().find(player.getUniqueId(), collectionId).thenAccept(found ->
                player.getScheduler().run(services.plugin(), task -> {
                    CollectionProgress progress = found.orElseThrow();
                    int slot = 0;
                    for (CollectionEntryDefinition entry : definition.entries().values()) {
                        boolean obtained = progress.obtainedEntryIds().contains(entry.id());
                        inventory.setItem(slot++, GuiItems.of(
                                entry.item(),
                                services.messages().render(MessageKey.GUI_COLLECTION_ENTRY_ITEM, Map.of("oge", entry.displayName())),
                                List.of(services.messages().render(obtained ? MessageKey.GUI_COLLECTION_ENTRY_LORE_OBTAINED : MessageKey.GUI_COLLECTION_ENTRY_LORE_MISSING)),
                                obtained
                        ));
                    }
                }, null));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new CollectionsMenu(services));
        }
    }
}
