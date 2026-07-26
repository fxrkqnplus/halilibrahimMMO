package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import com.halilibrahim.halilibrahimmmo.skills.SkillMilestoneEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Static explanation of what levels 25/50/75/100 grant for one specific skill, opened from {@link SkillDetailMenu}. */
public final class SkillMilestoneInfoMenu implements GuiView {

    private static final int[] TIER_SLOTS = {2, 3, 4, 5};
    private static final int[] TIER_LEVELS = {25, 50, 75, 100};
    private static final int BACK_SLOT = 8;

    private final GuiServices services;
    private final String skillId;

    public SkillMilestoneInfoMenu(GuiServices services, String skillId) {
        this.services = services;
        this.skillId = skillId;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_SKILL_MILESTONE_TITLE);
    }

    @Override
    public int size() {
        return 9;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        SkillDefinition definition = services.skills().settings().definitions().get(skillId);
        if (definition == null) {
            return;
        }
        for (int i = 0; i < TIER_LEVELS.length; i++) {
            SkillMilestoneEffect effect = definition.milestones().get(TIER_LEVELS[i]);
            inventory.setItem(TIER_SLOTS[i], GuiItems.of(effectIcon(effect),
                    services.messages().render(MessageKey.GUI_SKILL_MILESTONE_TIER_ITEM, Map.of("seviye", Integer.toString(TIER_LEVELS[i]))),
                    List.of(effectLore(effect))
            ));
        }
    }

    /**
     * One icon per {@link SkillMilestoneEffectType} instead of a single golden
     * apple for every tier, so a player can tell at a glance which boost a tier
     * grants without reading its lore line.
     */
    private Material effectIcon(SkillMilestoneEffect effect) {
        return switch (effect.type()) {
            case MAX_HEALTH -> Material.GOLDEN_APPLE;
            case PVE_DAMAGE_MULTIPLIER -> Material.DIAMOND_SWORD;
            case MAX_ENERGY -> Material.GLOWSTONE_DUST;
            case HASTE -> Material.GOLDEN_PICKAXE;
            case SPEED -> Material.SUGAR;
            case TREE_FELLING -> Material.IRON_AXE;
            case BREED_REFUND -> Material.WHEAT;
        };
    }

    private Component effectLore(SkillMilestoneEffect effect) {
        return switch (effect.type()) {
            case MAX_HEALTH -> services.messages().render(MessageKey.GUI_SKILL_MILESTONE_TIER_LORE_HEALTH, Map.of("can", formatNumber(effect.value())));
            case PVE_DAMAGE_MULTIPLIER -> services.messages().render(MessageKey.GUI_SKILL_MILESTONE_TIER_LORE_DAMAGE, Map.of("yuzde", formatNumber(effect.value() * 100)));
            case MAX_ENERGY -> services.messages().render(MessageKey.GUI_SKILL_MILESTONE_TIER_LORE_ENERGY, Map.of("enerji", formatNumber(effect.value())));
            case HASTE -> services.messages().render(MessageKey.GUI_SKILL_MILESTONE_TIER_LORE_HASTE, Map.of("kademe", Integer.toString((int) effect.value() + 1)));
            case SPEED -> services.messages().render(MessageKey.GUI_SKILL_MILESTONE_TIER_LORE_SPEED, Map.of("kademe", Integer.toString((int) effect.value() + 1)));
            case TREE_FELLING -> services.messages().render(MessageKey.GUI_SKILL_MILESTONE_TIER_LORE_TREE_FELLING, Map.of("deger", formatNumber(effect.value())));
            case BREED_REFUND -> services.messages().render(MessageKey.GUI_SKILL_MILESTONE_TIER_LORE_BREED_REFUND, Map.of("yuzde", formatNumber(effect.value() * 100)));
        };
    }

    private String formatNumber(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new SkillDetailMenu(services, skillId));
        }
    }
}
