package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Read-only display of the invoking player's progress in every configured skill. */
public final class SkillsMenu implements GuiView {

    private static final int FIRST_SLOT = 10;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;

    public SkillsMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_SKILLS_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        List<SkillDefinition> definitions = services.skills().definitions().stream()
                .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
        int slot = FIRST_SLOT;
        for (SkillDefinition definition : definitions) {
            int itemSlot = slot++;
            services.skills().load(player.getUniqueId(), definition.id()).thenAccept(progress ->
                    player.getScheduler().run(services.plugin(), task -> inventory.setItem(itemSlot, GuiItems.of(
                            Material.EXPERIENCE_BOTTLE,
                            services.messages().render(MessageKey.GUI_SKILL_ITEM, Map.of("yetenek", definition.displayName())),
                            List.of(services.messages().render(MessageKey.GUI_SKILL_ITEM_LORE, Map.of(
                                    "seviye", Integer.toString(progress.level()),
                                    "xp", Long.toString(progress.experience())
                            )))
                    )), null));
        }
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new ProgressionMenu(services));
        }
    }
}
