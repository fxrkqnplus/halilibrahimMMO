package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingTask;
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
 * Click-to-select view over the player's own class's abilities only, mirroring
 * {@link com.halilibrahim.halilibrahimmmo.abilities.AbilityService#select}.
 * Activation itself now happens by right-clicking a matching tool in-world
 * (see {@code AbilityActivationListener}), not from this menu.
 */
public final class AbilitiesMenu implements GuiView {

    private static final int FIRST_SLOT = 10;
    private static final int NO_CLASS_SLOT = 13;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;
    private final Map<Integer, String> slotToAbilityId = new LinkedHashMap<>();

    public AbilitiesMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ABILITIES_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        completeOnboarding(player);
        services.profiles().load(player.getUniqueId(), player.getName())
                .thenAccept(profile -> player.getScheduler().run(services.plugin(), task ->
                        render(inventory, player, profile.selectedClass()), null));
    }

    private void completeOnboarding(Player player) {
        services.onboarding().complete(player.getUniqueId(), player.getName(), OnboardingTask.KABILIYETLERI_INCELEME).thenAccept(newlyCompleted -> {
            if (!newlyCompleted) {
                return;
            }
            String classId = services.profiles().cached(player.getUniqueId()).map(profile -> profile.selectedClass()).orElse("");
            if (!classId.isEmpty()) {
                services.classes().awardClassExperience(player.getUniqueId(), player.getName(), classId, OnboardingTask.KABILIYETLERI_INCELEME.rewardExperience());
            }
            player.getScheduler().run(services.plugin(), task -> {
                services.messages().send(player, MessageKey.ONBOARDING_ABILITIES_COMPLETED, Map.of("xp", Long.toString(OnboardingTask.KABILIYETLERI_INCELEME.rewardExperience())));
                services.sidebar().refresh(player);
            }, null);
        });
    }

    private void render(Inventory inventory, Player player, String selectedClassId) {
        if (selectedClassId.isEmpty()) {
            inventory.setItem(NO_CLASS_SLOT, GuiItems.of(Material.BARRIER, services.messages().render(MessageKey.GUI_ABILITIES_NO_CLASS)));
            return;
        }
        String selectedAbilityId = services.abilities().selected(player.getUniqueId()).orElse(null);
        List<AbilityDefinition> definitions = services.abilities().definitions().stream()
                .filter(definition -> definition.classId().equals(selectedClassId))
                .sorted((left, right) -> {
                    int byLevel = Integer.compare(left.requiredLevel(), right.requiredLevel());
                    return byLevel != 0 ? byLevel : left.displayName().compareToIgnoreCase(right.displayName());
                }).toList();
        int slot = FIRST_SLOT;
        for (AbilityDefinition definition : definitions) {
            int classLevel = services.classes().classProgress(player.getUniqueId(), definition.classId()).level();
            if (classLevel < definition.requiredLevel()) {
                inventory.setItem(slot++, GuiItems.of(
                        Material.BARRIER,
                        services.messages().render(MessageKey.GUI_ABILITY_LOCKED_ITEM, Map.of("kabiliyet", definition.displayName())),
                        List.of(services.messages().render(MessageKey.GUI_ABILITY_LOCKED_LORE, Map.of("seviye", Integer.toString(definition.requiredLevel()))))
                ));
                continue;
            }
            slotToAbilityId.put(slot, definition.id());
            boolean selected = definition.id().equals(selectedAbilityId);
            List<Component> lore = new ArrayList<>();
            lore.add(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_DESCRIPTION, Map.of("aciklama", definition.description())));
            lore.add(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE, Map.of(
                    "seviye", Integer.toString(definition.requiredLevel()),
                    "bekleme", Long.toString(definition.cooldownMillis() / 1000L)
            )));
            if (definition.energyCost() > 0) {
                lore.add(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_ENERGY_LINE, Map.of("enerji", Integer.toString(definition.energyCost()))));
            }
            if (selected) {
                lore.add(services.messages().render(MessageKey.GUI_ABILITY_SELECTED_MARKER));
            }
            inventory.setItem(slot++, GuiItems.of(
                    Material.BLAZE_POWDER,
                    abilityName(definition),
                    lore,
                    selected
            ));
        }
    }

    /**
     * Built directly as a coloured {@link Component} rather than through a
     * {@code MessageKey} template: an ability's colour is per-ability
     * configured data ({@code gorsel.renk}), and {@code MessageService} only
     * substitutes unparsed string placeholders, which cannot vary a
     * placeholder's own colour per invocation. This is the same documented
     * exception §10.6 established for per-title chat tags and §12.12 reused
     * for the sidebar name line.
     */
    private Component abilityName(AbilityDefinition definition) {
        return Component.text(definition.displayName(), definition.visual().color().textColor());
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AnaMenu(services));
            return;
        }
        String abilityId = slotToAbilityId.get(slot);
        if (abilityId != null) {
            services.abilities().select(player, abilityId);
            GuiManager.open(player, new AbilitiesMenu(services));
        }
    }
}
