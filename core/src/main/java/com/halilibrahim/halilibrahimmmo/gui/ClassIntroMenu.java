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

    /**
     * Three evenly-spaced pedestals in the middle row of a 45-slot board, with
     * the class's own frame colour under each. Widened from a 27-slot strip in
     * Faz 21: the entries had grown a blurb, a synergy line, four abilities and
     * a prestige note, and three of those stacked side by side in a cramped
     * screen read as one wall of grey text rather than as three choices.
     */
    private static final int[] CLASS_SLOTS = {20, 22, 24};
    /** Directly under each pedestal, in that class's own {@code cerceve} colour. */
    private static final int[] CLASS_BASE_SLOTS = {29, 31, 33};
    private static final int HEADER_SLOT = 4;
    private static final int STATUS_SLOT = 40;
    private static final int BACK_SLOT = 36;

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
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.border(), GuiItems.filler());
        inventory.setItem(HEADER_SLOT, GuiItems.of(
                Material.ENCHANTED_BOOK,
                services.messages().render(MessageKey.GUI_CLASS_INTRO_HEADER),
                List.of(
                        services.messages().render(MessageKey.GUI_CLASS_INTRO_HEADER_LORE),
                        services.messages().render(MessageKey.GUI_CLASS_INTRO_HEADER_LORE_TWO)
                )
        ));
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
            int position = index++;
            int slot = CLASS_SLOTS[position];
            boolean current = definition.id().equals(profile.selectedClass());
            slotToClassId.put(slot, definition.id());
            List<Component> lore = new ArrayList<>();
            MessageKey blurb = introLoreKey(definition.id());
            if (blurb != null) {
                lore.add(services.messages().render(blurb));
            }
            lore.add(Component.empty());
            // The synergy skills' own display names, joined into one unparsed
            // placeholder rather than nested components - the same "plain data
            // fragment into one placeholder" shape every list in this codebase
            // uses, so no Turkish is ever assembled in Java.
            lore.add(services.messages().render(MessageKey.GUI_CLASS_INTRO_SYNERGY_LINE,
                    Map.of("beceriler", synergySkillNames(definition))));
            MessageKey prestige = prestigeLoreKey(definition.id());
            if (prestige != null) {
                lore.add(services.messages().render(prestige));
            }
            lore.add(Component.empty());
            lore.add(services.messages().render(MessageKey.GUI_CLASS_INTRO_ABILITY_HEADER));
            services.abilities().definitions().stream()
                    .filter(ability -> ability.classId().equals(definition.id()))
                    .sorted((left, right) -> Integer.compare(left.requiredLevel(), right.requiredLevel()))
                    // The unlock level belongs on this line now that every class
                    // has four abilities on fixed rungs: the list doubles as
                    // "how far in do I get this", which is most of what a player
                    // is comparing when they pick a class.
                    .forEach(ability -> lore.add(services.messages().render(MessageKey.GUI_CLASS_INTRO_ABILITY_LINE,
                            Map.of("kabiliyet", ability.displayName(), "seviye", Integer.toString(ability.requiredLevel())))));
            lore.add(Component.empty());
            lore.add(services.messages().render(current
                    ? MessageKey.GUI_CLASS_INTRO_CURRENT_MARKER : MessageKey.GUI_CLASS_INTRO_PICK_HINT));
            inventory.setItem(slot, GuiItems.of(
                    definition.icon(),
                    services.messages().render(MessageKey.GUI_CLASS_ITEM, Map.of("sinif", definition.displayName())),
                    lore,
                    current
            ));
            // The pedestal under each icon is that class's own frame colour, so
            // the three choices are told apart by colour before a word is read.
            inventory.setItem(CLASS_BASE_SLOTS[position], GuiItems.pane(definition.frame()));
        }
    }

    private String synergySkillNames(PlayerClassDefinition definition) {
        return definition.synergySkillIds().stream()
                .map(skillId -> services.skills().settings().definitions().get(skillId))
                .filter(java.util.Objects::nonNull)
                .map(com.halilibrahim.halilibrahimmmo.skills.SkillDefinition::displayName)
                .collect(java.util.stream.Collectors.joining(", "));
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
            case "avci" -> MessageKey.GUI_CLASS_INTRO_LORE_AVCI;
            case "madenci" -> MessageKey.GUI_CLASS_INTRO_LORE_MADENCI;
            case "tabiat-varisi" -> MessageKey.GUI_CLASS_INTRO_LORE_TABIAT_VARISI;
            default -> null;
        };
    }

    /**
     * One fully-authored line per class rather than a template fed the effect
     * type's Turkish name - the prestige bonus is three fixed sentences, and
     * naming a {@code ClassEffectType} in Java is exactly what this codebase
     * does not do. A class added to configuration simply shows no line.
     */
    private MessageKey prestigeLoreKey(String classId) {
        return switch (classId) {
            case "avci" -> MessageKey.GUI_CLASS_INTRO_PRESTIGE_AVCI;
            case "madenci" -> MessageKey.GUI_CLASS_INTRO_PRESTIGE_MADENCI;
            case "tabiat-varisi" -> MessageKey.GUI_CLASS_INTRO_PRESTIGE_TABIAT_VARISI;
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
