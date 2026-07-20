package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.quests.QuestDefinition;
import com.halilibrahim.halilibrahimmmo.quests.QuestProgress;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Read-only display of every configured quest and the invoking player's progress. */
public final class QuestsMenu implements GuiView {

    private static final int BACK_SLOT = 26;

    private final GuiServices services;

    public QuestsMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_QUESTS_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.quests().loadAll(player.getUniqueId()).thenAccept(progressById ->
                player.getScheduler().run(services.plugin(), task -> {
                    List<QuestDefinition> definitions = services.quests().definitions().stream()
                            .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
                    int slot = 0;
                    for (QuestDefinition definition : definitions) {
                        QuestProgress progress = progressById.get(definition.id());
                        inventory.setItem(slot++, GuiItems.of(
                                materialFor(definition),
                                services.messages().render(MessageKey.GUI_QUEST_ITEM, Map.of("gorev", definition.displayName())),
                                List.of(services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE, Map.of(
                                        "ilerleme", progress.progress() + "/" + definition.target()
                                )))
                        ));
                    }
                }, null));
    }

    private Material materialFor(QuestDefinition definition) {
        return switch (definition.type()) {
            case PERMANENT -> Material.WRITTEN_BOOK;
            case DAILY -> Material.CLOCK;
            case WEEKLY -> Material.SUNFLOWER;
        };
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AnaMenu(services));
        }
    }
}
