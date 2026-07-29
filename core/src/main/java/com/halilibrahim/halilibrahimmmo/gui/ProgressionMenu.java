package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * "İlerleme" category sub-hub: profile, skills, class and prestige.
 *
 * <p>Framed and lore-carrying like every other hub in the plugin. It used to be
 * a bare 27-slot strip of four unlabelled icons, which meant the one screen
 * standing between a player and four different systems said nothing about any
 * of them.</p>
 */
public final class ProgressionMenu implements GuiView {

    private static final int PROFILE_SLOT = 20;
    private static final int SKILLS_SLOT = 22;
    private static final int CLASS_SLOT = 24;
    private static final int PRESTIGE_SLOT = 31;
    private static final int BACK_SLOT = 40;

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
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.pane(Material.ORANGE_STAINED_GLASS_PANE), GuiItems.filler());
        inventory.setItem(PROFILE_SLOT, GuiItems.playerHead(player,
                services.messages().render(MessageKey.GUI_MAIN_PROFILE),
                List.of(services.messages().render(MessageKey.GUI_MAIN_PROFILE_LORE))));
        inventory.setItem(SKILLS_SLOT, GuiItems.of(Material.EXPERIENCE_BOTTLE,
                services.messages().render(MessageKey.GUI_MAIN_SKILLS),
                List.of(services.messages().render(MessageKey.GUI_MAIN_SKILLS_LORE))));
        inventory.setItem(CLASS_SLOT, GuiItems.of(Material.DIAMOND_SWORD,
                services.messages().render(MessageKey.GUI_MAIN_CLASS),
                List.of(services.messages().render(MessageKey.GUI_MAIN_CLASS_LORE))));
        inventory.setItem(PRESTIGE_SLOT, GuiItems.of(Material.NETHER_STAR,
                services.messages().render(MessageKey.GUI_MAIN_PRESTIGE),
                List.of(services.messages().render(MessageKey.GUI_MAIN_PRESTIGE_LORE))));
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        switch (slot) {
            case PROFILE_SLOT -> GuiManager.open(player, new PlayerProfileMenu(services, player.getUniqueId(), player.getName()));
            case SKILLS_SLOT -> GuiManager.open(player, new SkillsMenu(services));
            case CLASS_SLOT -> GuiManager.open(player, new ClassMenu(services));
            case PRESTIGE_SLOT -> GuiManager.open(player, new PrestigeMenu(services));
            case BACK_SLOT -> GuiManager.open(player, new AnaMenu(services));
            default -> {
            }
        }
    }
}
