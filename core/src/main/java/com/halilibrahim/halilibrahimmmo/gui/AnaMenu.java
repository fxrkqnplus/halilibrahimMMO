package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Central hub: a personal summary head across the top, the four category
 * icons - İlerleme ({@link ProgressionMenu}), Kabiliyetlerim
 * ({@link AbilitiesMenu}, kept top-level as this codebase's most involved
 * system), Görevler ({@link TasksMenu}) and Topluluk
 * ({@link CommunityMenu}) - each with its own "what is in here" lore line,
 * and a {@link CreditsMenu} link at the bottom.
 *
 * <p>The summary head reads {@code ProfileService}'s already-warmed in-memory
 * cache rather than issuing an asynchronous load, so this screen stays a
 * single synchronous render like every other hub menu. A player who somehow
 * has no cached profile simply gets the plain hub without the header.</p>
 */
public final class AnaMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int PROGRESSION_SLOT = 19;
    private static final int ABILITIES_SLOT = 21;
    private static final int TASKS_SLOT = 23;
    private static final int COMMUNITY_SLOT = 25;
    private static final int CREDITS_SLOT = 40;

    private final GuiServices services;

    public AnaMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_MAIN_TITLE);
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        ItemStack border = GuiItems.border();
        ItemStack filler = GuiItems.filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            boolean edge = slot < 9 || slot >= inventory.getSize() - 9 || slot % 9 == 0 || slot % 9 == 8;
            inventory.setItem(slot, edge ? border : filler);
        }

        services.profiles().cached(player.getUniqueId())
                .ifPresent(profile -> inventory.setItem(HEADER_SLOT, headerItem(player, profile)));

        inventory.setItem(PROGRESSION_SLOT, GuiItems.of(
                Material.EXPERIENCE_BOTTLE,
                services.messages().render(MessageKey.GUI_MAIN_CATEGORY_PROGRESSION),
                List.of(services.messages().render(MessageKey.GUI_MAIN_CATEGORY_PROGRESSION_LORE))
        ));
        inventory.setItem(ABILITIES_SLOT, GuiItems.of(
                Material.BLAZE_POWDER,
                services.messages().render(MessageKey.GUI_MAIN_ABILITIES),
                List.of(services.messages().render(MessageKey.GUI_MAIN_ABILITIES_LORE))
        ));
        inventory.setItem(TASKS_SLOT, GuiItems.of(
                Material.WRITTEN_BOOK,
                services.messages().render(MessageKey.GUI_MAIN_CATEGORY_TASKS),
                List.of(services.messages().render(MessageKey.GUI_MAIN_CATEGORY_TASKS_LORE))
        ));
        inventory.setItem(COMMUNITY_SLOT, GuiItems.of(
                Material.BEACON,
                services.messages().render(MessageKey.GUI_MAIN_CATEGORY_COMMUNITY),
                List.of(services.messages().render(MessageKey.GUI_MAIN_CATEGORY_COMMUNITY_LORE))
        ));
        inventory.setItem(CREDITS_SLOT, GuiItems.of(
                Material.NETHER_STAR,
                services.messages().render(MessageKey.GUI_MAIN_CREDITS),
                List.of(services.messages().render(MessageKey.GUI_MAIN_CREDITS_LORE))
        ));
    }

    private ItemStack headerItem(Player player, PlayerProfile profile) {
        String guc = Integer.toString(services.progression().cachedGlobalLevel(player.getUniqueId()));
        Component summary = profile.selectedClass().isEmpty()
                ? services.messages().render(MessageKey.GUI_MAIN_HEADER_LORE_NO_CLASS, Map.of(
                        "seviye", guc,
                        "prestij", Integer.toString(profile.prestige())
                ))
                : services.messages().render(MessageKey.GUI_MAIN_HEADER_LORE, Map.of(
                        "seviye", guc,
                        "prestij", Integer.toString(profile.prestige()),
                        "sinif", services.classes().displayName(profile.selectedClass())
                ));
        return GuiItems.playerHead(
                player,
                services.messages().render(MessageKey.GUI_MAIN_HEADER_ITEM, Map.of("oyuncu", profile.playerName())),
                List.of(summary)
        );
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        switch (slot) {
            case PROGRESSION_SLOT -> GuiManager.open(player, new ProgressionMenu(services));
            case ABILITIES_SLOT -> GuiManager.open(player, new AbilitiesMenu(services));
            case TASKS_SLOT -> GuiManager.open(player, new TasksMenu(services));
            case COMMUNITY_SLOT -> GuiManager.open(player, new CommunityMenu(services));
            case CREDITS_SLOT -> GuiManager.open(player, new CreditsMenu(services, false));
            default -> {
            }
        }
    }
}
