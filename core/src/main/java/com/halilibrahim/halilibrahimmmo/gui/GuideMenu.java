package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.guide.GuideCategory;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The handbook's front page: one icon per {@code rehber.yml} category.
 *
 * <p>Laid out on a fixed grid rather than packed left-to-right, so a category's
 * position is stable between opens and a player learns where "Prestij" is
 * instead of re-reading the whole board every time. Content order comes from
 * the file; only how many fit is decided here.</p>
 */
public final class GuideMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int BACK_SLOT = 49;
    /**
     * The three interior rows, edges excluded. Twenty-one places for a file
     * that ships ten categories - room to grow without the screen ever needing
     * a second page.
     */
    private static final int[] CATEGORY_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final GuiServices services;
    private final boolean fromMenu;
    private final List<GuideCategory> categories = new ArrayList<>();

    /**
     * @param fromMenu whether this was reached from {@code /menü} - the back
     *                 button only exists when there is somewhere to go back to,
     *                 since {@code /yardım} is also a top-level command.
     */
    public GuideMenu(GuiServices services, boolean fromMenu) {
        this.services = services;
        this.fromMenu = fromMenu;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_GUIDE_TITLE);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.pane(Material.BLUE_STAINED_GLASS_PANE), GuiItems.filler());
        inventory.setItem(HEADER_SLOT, GuiItems.of(
                Material.ENCHANTED_BOOK,
                services.messages().render(MessageKey.GUI_GUIDE_HEADER),
                List.of(
                        services.messages().render(MessageKey.GUI_GUIDE_HEADER_LORE),
                        services.messages().render(MessageKey.GUI_GUIDE_HEADER_HINT)
                )
        ));
        categories.clear();
        categories.addAll(services.guide().categories());
        for (int index = 0; index < categories.size() && index < CATEGORY_SLOTS.length; index++) {
            GuideCategory category = categories.get(index);
            inventory.setItem(CATEGORY_SLOTS[index], GuiItems.of(
                    category.icon(),
                    services.messages().renderText(category.title()),
                    List.of(
                            services.messages().renderText(category.summary()),
                            Component.empty(),
                            services.messages().render(MessageKey.GUI_GUIDE_CATEGORY_COUNT,
                                    Map.of("sayi", Integer.toString(category.entries().size())))
                    ),
                    category.model()
            ));
        }
        if (fromMenu) {
            inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        }
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (fromMenu && slot == BACK_SLOT) {
            GuiManager.open(player, new AnaMenu(services));
            return;
        }
        for (int index = 0; index < CATEGORY_SLOTS.length && index < categories.size(); index++) {
            if (CATEGORY_SLOTS[index] == slot) {
                GuiManager.open(player, new GuideCategoryMenu(services, categories.get(index), fromMenu));
                return;
            }
        }
    }
}
