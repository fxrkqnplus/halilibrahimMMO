package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** "İlerleme" category sub-hub: profile, skills, class and prestige. */
public final class ProgressionMenu implements GuiView {

    private static final int PROFILE_SLOT = 10;
    private static final int SKILLS_SLOT = 12;
    private static final int CLASS_SLOT = 14;
    private static final int PRESTIGE_SLOT = 16;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;

    public ProgressionMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_PROGRESSION_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        ItemStack filler = GuiItems.filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(PROFILE_SLOT, GuiItems.of(Material.PLAYER_HEAD, services.messages().render(MessageKey.GUI_MAIN_PROFILE)));
        inventory.setItem(SKILLS_SLOT, GuiItems.of(Material.EXPERIENCE_BOTTLE, services.messages().render(MessageKey.GUI_MAIN_SKILLS)));
        inventory.setItem(CLASS_SLOT, GuiItems.of(Material.DIAMOND_SWORD, services.messages().render(MessageKey.GUI_MAIN_CLASS)));
        inventory.setItem(PRESTIGE_SLOT, GuiItems.of(Material.NETHER_STAR, services.messages().render(MessageKey.GUI_MAIN_PRESTIGE)));
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        switch (slot) {
            case PROFILE_SLOT -> GuiManager.open(player, new ProfileMenu(services));
            case SKILLS_SLOT -> GuiManager.open(player, new SkillsMenu(services));
            case CLASS_SLOT -> GuiManager.open(player, new ClassMenu(services));
            case PRESTIGE_SLOT -> GuiManager.open(player, new PrestigeMenu(services));
            case BACK_SLOT -> GuiManager.open(player, new AnaMenu(services));
            default -> {
            }
        }
    }
}
