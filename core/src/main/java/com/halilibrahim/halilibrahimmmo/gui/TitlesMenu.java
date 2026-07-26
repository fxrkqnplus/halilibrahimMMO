package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Title unlock status and equip action - the only way to equip one, opened by {@code /unvan}. */
public final class TitlesMenu implements GuiView {

    private static final int FIRST_SLOT = 10;
    private static final int BACK_SLOT = 26;

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
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.titles().unlockedIds(player.getUniqueId())
                .thenCompose(unlocked -> services.titles().findEquipped(player.getUniqueId())
                        .thenAccept(equipped -> player.getScheduler().run(services.plugin(), task ->
                                render(inventory, unlocked, equipped), null)));
    }

    private void render(Inventory inventory, Set<String> unlocked, Optional<TitleDefinition> equipped) {
        List<TitleDefinition> definitions = services.titles().definitions().stream()
                .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
        String equippedId = equipped.map(TitleDefinition::id).orElse("");
        int slot = FIRST_SLOT;
        for (TitleDefinition definition : definitions) {
            boolean isUnlocked = unlocked.contains(definition.id());
            boolean isEquipped = definition.id().equals(equippedId);
            slotToTitleId.put(slot, definition.id());
            inventory.setItem(slot++, GuiItems.of(
                    definition.icon(),
                    services.messages().render(MessageKey.GUI_TITLE_ITEM, Map.of("unvan", definition.displayName())),
                    List.of(services.messages().render(isUnlocked ? MessageKey.GUI_TITLE_ITEM_LORE_UNLOCKED : MessageKey.GUI_TITLE_ITEM_LORE_LOCKED)),
                    isEquipped
            ));
        }
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
