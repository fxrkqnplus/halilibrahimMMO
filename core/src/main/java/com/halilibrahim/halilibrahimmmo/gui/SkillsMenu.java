package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingTask;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Becerilerim": every configured skill's level and XP, each drawn with its own
 * icon, and each one a door into that skill's detail screen.
 *
 * <p>Two things changed here in Phase 16 and both are the same idea. The screen
 * used to be called "Yeteneklerim", which is what the talent tree is called -
 * two different systems sharing one word is a reliable way to make players open
 * the wrong screen. And every skill used to be an identical experience bottle;
 * now each carries the {@code simge} it declares in {@code skills.yml}, so a
 * pickaxe means mining without reading anything, and a skill the player's class
 * draws synergy from is marked as such rather than looking like all the
 * rest.</p>
 */
public final class SkillsMenu implements GuiView {

    private static final int[] SKILL_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int BACK_SLOT = 31;

    private final GuiServices services;
    private final Map<Integer, String> slotToSkillId = new LinkedHashMap<>();

    public SkillsMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_SKILLS_TITLE);
    }

    @Override
    public int size() {
        return 36;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.border(), GuiItems.filler());
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.onboardingRewards().complete(player, OnboardingTask.BECERILERI_INCELEME, MessageKey.ONBOARDING_SKILLS_COMPLETED);

        String classId = services.profiles().cached(player.getUniqueId())
                .map(profile -> profile.selectedClass()).orElse("");
        PlayerClassDefinition selectedClass = classId.isEmpty()
                ? null
                : services.classes().settings().definitions().get(classId);

        List<SkillDefinition> definitions = services.skills().definitions().stream()
                .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
        int index = 0;
        for (SkillDefinition definition : definitions) {
            if (index >= SKILL_SLOTS.length) {
                break;
            }
            int slot = SKILL_SLOTS[index++];
            slotToSkillId.put(slot, definition.id());
            boolean synergy = selectedClass != null && selectedClass.hasSynergyWith(definition.id());
            services.skills().load(player.getUniqueId(), definition.id()).thenAccept(progress ->
                    player.getScheduler().run(services.plugin(), task ->
                            inventory.setItem(slot, skillItem(definition, progress.level(), progress.experience(), synergy, selectedClass)), null));
        }
    }

    private ItemStack skillItem(SkillDefinition definition, int level, long experience, boolean synergy, PlayerClassDefinition selectedClass) {
        List<Component> lore = new ArrayList<>();
        lore.add(services.messages().render(MessageKey.GUI_SKILL_ITEM_LORE, Map.of(
                "seviye", Integer.toString(level),
                "xp", Long.toString(experience)
        )));
        if (synergy) {
            lore.add(services.messages().render(MessageKey.GUI_SKILL_ITEM_SYNERGY, Map.of(
                    "sinif", selectedClass.displayName()
            )));
        }
        lore.add(services.messages().render(MessageKey.GUI_SKILL_ITEM_OPEN_HINT));
        return GuiItems.of(
                definition.icon(),
                services.messages().render(MessageKey.GUI_SKILL_ITEM, Map.of("yetenek", definition.displayName())),
                lore,
                synergy,
                definition.model()
        );
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new ProgressionMenu(services));
            return;
        }
        String skillId = slotToSkillId.get(slot);
        if (skillId != null) {
            GuiManager.open(player, new SkillDetailMenu(services, skillId));
        }
    }
}
