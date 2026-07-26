package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassProgress;
import com.halilibrahim.halilibrahimmmo.locale.DurationFormatter;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
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

/**
 * The class picker, used for <em>both</em> the very first pick (opened straight
 * after {@code /mmo aktif}) and every later class change (opened from
 * {@link ClassMenu}) - deliberately the same screen, so a player changing class
 * re-reads the same blurbs and ability lists they chose from originally instead
 * of picking blind off a bare icon row.
 *
 * <p>The two entry points differ only in framing, carried by {@code fromMenu}:
 * the change flow adds a back button and a status item showing the current
 * class and whether the change cooldown has expired yet, and it passes that
 * flag down to {@link ClassConfirmMenu} so cancelling returns to the right
 * screen. Picking always routes through the confirmation screen either way.</p>
 */
public final class ClassIntroMenu implements GuiView {

    private static final int[] CLASS_SLOTS = {10, 12, 14, 16};
    private static final int STATUS_SLOT = 4;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;
    private final boolean fromMenu;
    private final Map<Integer, String> slotToClassId = new LinkedHashMap<>();

    public ClassIntroMenu(GuiServices services) {
        this(services, false);
    }

    public ClassIntroMenu(GuiServices services, boolean fromMenu) {
        this.services = services;
        this.fromMenu = fromMenu;
    }

    @Override
    public Component title() {
        return services.messages().render(fromMenu ? MessageKey.GUI_CLASS_INTRO_CHANGE_TITLE : MessageKey.GUI_CLASS_INTRO_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        if (fromMenu) {
            inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        }
        services.profiles().load(player.getUniqueId(), player.getName())
                .thenCompose(profile -> services.classes().load(player.getUniqueId(), player.getName())
                        .thenApply(progress -> new Object[]{profile, progress}))
                .thenAccept(values -> player.getScheduler().run(services.plugin(), task ->
                        render(inventory, (PlayerProfile) values[0], (PlayerClassProgress) values[1]), null));
    }

    private void render(Inventory inventory, PlayerProfile profile, PlayerClassProgress progress) {
        if (fromMenu) {
            inventory.setItem(STATUS_SLOT, statusItem(profile, progress));
        }
        List<PlayerClassDefinition> definitions = services.classes().definitions().stream()
                .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
        int index = 0;
        for (PlayerClassDefinition definition : definitions) {
            if (index >= CLASS_SLOTS.length) {
                break;
            }
            int slot = CLASS_SLOTS[index++];
            boolean current = definition.id().equals(profile.selectedClass());
            slotToClassId.put(slot, definition.id());
            List<Component> lore = new ArrayList<>();
            MessageKey blurb = introLoreKey(definition.id());
            if (blurb != null) {
                lore.add(services.messages().render(blurb));
            }
            services.abilities().definitions().stream()
                    .filter(ability -> ability.classId().equals(definition.id()))
                    .sorted((left, right) -> Integer.compare(left.requiredLevel(), right.requiredLevel()))
                    .forEach(ability -> lore.add(services.messages().render(MessageKey.GUI_CLASS_INTRO_ABILITY_LINE,
                            Map.of("kabiliyet", ability.displayName()))));
            if (current) {
                lore.add(services.messages().render(MessageKey.GUI_CLASS_INTRO_CURRENT_MARKER));
            }
            inventory.setItem(slot, GuiItems.of(
                    definition.icon(),
                    services.messages().render(MessageKey.GUI_CLASS_ITEM, Map.of("sinif", definition.displayName())),
                    lore,
                    current
            ));
        }
    }

    /** Answers "what am I now, and may I change yet?" before the player commits to a pick. */
    private ItemStack statusItem(PlayerProfile profile, PlayerClassProgress progress) {
        List<Component> lore = new ArrayList<>();
        lore.add(services.messages().render(MessageKey.GUI_CLASS_INTRO_STATUS_CURRENT, Map.of(
                "sinif", profile.selectedClass().isEmpty() ? "-" : services.classes().displayName(profile.selectedClass())
        )));
        long remaining = progress.classChangeReadyAtMillis() - System.currentTimeMillis();
        if (profile.selectedClass().isEmpty() || remaining <= 0) {
            lore.add(services.messages().render(MessageKey.GUI_CLASS_INTRO_STATUS_READY));
        } else {
            lore.add(services.messages().render(MessageKey.GUI_CLASS_INTRO_STATUS_COOLDOWN, Map.of(
                    "sure", DurationFormatter.ofMillis(services.messages(), remaining)
            )));
        }
        return GuiItems.of(Material.CLOCK, services.messages().render(MessageKey.GUI_CLASS_INTRO_STATUS_ITEM), lore);
    }

    private MessageKey introLoreKey(String classId) {
        return switch (classId) {
            case "savasci" -> MessageKey.GUI_CLASS_INTRO_LORE_SAVASCI;
            case "avci" -> MessageKey.GUI_CLASS_INTRO_LORE_AVCI;
            case "madenci" -> MessageKey.GUI_CLASS_INTRO_LORE_MADENCI;
            case "oduncu" -> MessageKey.GUI_CLASS_INTRO_LORE_ODUNCU;
            default -> null;
        };
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (fromMenu && slot == BACK_SLOT) {
            GuiManager.open(player, new ClassMenu(services));
            return;
        }
        String classId = slotToClassId.get(slot);
        if (classId != null) {
            GuiManager.open(player, new ClassConfirmMenu(services, classId, fromMenu));
        }
    }
}
