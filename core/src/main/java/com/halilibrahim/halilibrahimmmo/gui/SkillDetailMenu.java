package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import com.halilibrahim.halilibrahimmmo.skills.SkillProgress;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * One skill's detail screen, opened from {@code /madencilik}/{@code /odunculuk}/
 * {@code /kazicilik}/{@code /avcilik}: current level/XP, the class this skill
 * powers (reverse {@code sinerji-beceri} lookup), a milestone-info button and
 * a leaderboard button reusing the existing, unmodified {@link LeaderboardDetailMenu}.
 */
public final class SkillDetailMenu implements GuiView {

    private static final int INFO_SLOT = 4;
    private static final int MILESTONE_INFO_SLOT = 11;
    private static final int LEADERBOARD_SLOT = 13;
    private static final int BACK_SLOT = 17;

    private final GuiServices services;
    private final String skillId;

    public SkillDetailMenu(GuiServices services, String skillId) {
        this.services = services;
        this.skillId = skillId;
    }

    @Override
    public Component title() {
        SkillDefinition definition = services.skills().settings().definitions().get(skillId);
        return services.messages().render(MessageKey.GUI_SKILL_DETAIL_TITLE, Map.of(
                "beceri", definition == null ? skillId : definition.displayName()
        ));
    }

    @Override
    public int size() {
        return 18;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        SkillDefinition definition = services.skills().settings().definitions().get(skillId);
        if (definition == null) {
            return;
        }
        services.skills().load(player.getUniqueId(), skillId).thenAccept(progress ->
                player.getScheduler().run(services.plugin(), task -> render(inventory, definition, progress), null));
    }

    private void render(Inventory inventory, SkillDefinition definition, SkillProgress progress) {
        String className = services.classes().definitions().stream()
                .filter(classDefinition -> classDefinition.synergySkillId().equals(skillId))
                .findFirst()
                .map(PlayerClassDefinition::displayName)
                .orElse("-");
        inventory.setItem(INFO_SLOT, GuiItems.of(Material.EXPERIENCE_BOTTLE,
                services.messages().render(MessageKey.GUI_SKILL_DETAIL_ITEM, Map.of("beceri", definition.displayName())),
                List.of(services.messages().render(MessageKey.GUI_SKILL_DETAIL_LORE, Map.of(
                        "seviye", Integer.toString(progress.level()),
                        "xp", Long.toString(progress.experience()),
                        "sinif", className
                )))
        ));
        inventory.setItem(MILESTONE_INFO_SLOT, GuiItems.of(Material.NETHER_STAR,
                services.messages().render(MessageKey.GUI_SKILL_MILESTONE_ITEM)));
        inventory.setItem(LEADERBOARD_SLOT, GuiItems.of(Material.GOLD_INGOT,
                services.messages().render(MessageKey.GUI_SKILL_LEADERBOARD_ITEM)));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new SkillsMenu(services));
            return;
        }
        if (slot == MILESTONE_INFO_SLOT) {
            GuiManager.open(player, new SkillMilestoneInfoMenu(services, skillId));
            return;
        }
        if (slot == LEADERBOARD_SLOT) {
            SkillDefinition definition = services.skills().settings().definitions().get(skillId);
            if (definition != null) {
                GuiManager.open(player, new LeaderboardDetailMenu(services, skillId, definition.displayName()));
            }
        }
    }
}
