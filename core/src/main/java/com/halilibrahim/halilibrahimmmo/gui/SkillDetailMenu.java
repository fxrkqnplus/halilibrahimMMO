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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One skill's detail screen, opened from the skills list or from the skill's
 * own command ({@code /madencilik}, {@code /tarımcılık}, ...): current level and
 * XP under the skill's own icon, the class this skill powers, a milestone-info
 * button and a leaderboard button reusing the existing, unmodified
 * {@link LeaderboardDetailMenu}.
 *
 * <p>The screen is framed in the powering class's own colour, so opening
 * {@code /odunculuk} looks like Tabiat Vârisi's screen and {@code /madencilik}
 * looks like Mağara Adamı's - the connection between a skill and its class is
 * the single most useful thing this screen has to say, and colour says it
 * before any text does.</p>
 */
public final class SkillDetailMenu implements GuiView {

    private static final int INFO_SLOT = 13;
    private static final int PROGRESS_SLOT = 20;
    private static final int MILESTONE_INFO_SLOT = 22;
    private static final int LEADERBOARD_SLOT = 24;
    private static final int BACK_SLOT = 31;

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
        return 36;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        SkillDefinition definition = services.skills().settings().definitions().get(skillId);
        Optional<PlayerClassDefinition> owningClass = services.classes().definitions().stream()
                .filter(classDefinition -> classDefinition.hasSynergyWith(skillId))
                .findFirst();
        GuiItems.frame(inventory,
                owningClass.map(classDefinition -> GuiItems.pane(classDefinition.frame())).orElseGet(GuiItems::border),
                GuiItems.filler());
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        if (definition == null) {
            return;
        }
        services.skills().load(player.getUniqueId(), skillId).thenAccept(progress ->
                player.getScheduler().run(services.plugin(), task -> render(inventory, definition, progress, owningClass), null));
    }

    private void render(
            Inventory inventory, SkillDefinition definition, SkillProgress progress, Optional<PlayerClassDefinition> owningClass
    ) {
        String className = owningClass.map(PlayerClassDefinition::displayName).orElse("-");
        inventory.setItem(INFO_SLOT, GuiItems.of(definition.icon(),
                services.messages().render(MessageKey.GUI_SKILL_DETAIL_ITEM, Map.of("beceri", definition.displayName())),
                List.of(services.messages().render(MessageKey.GUI_SKILL_DETAIL_LORE, Map.of(
                        "seviye", Integer.toString(progress.level()),
                        "xp", Long.toString(progress.experience()),
                        "sinif", className
                ))),
                definition.model()
        ));
        inventory.setItem(PROGRESS_SLOT, GuiItems.of(Material.EXPERIENCE_BOTTLE,
                services.messages().render(MessageKey.GUI_SKILL_DETAIL_PROGRESS_ITEM),
                List.of(services.messages().render(MessageKey.GUI_SKILL_DETAIL_PROGRESS_LORE, Map.of(
                        "xp", Long.toString(progress.experience()),
                        "gereken", Long.toString(requiredForLevel(progress.level()))
                )))
        ));
        inventory.setItem(MILESTONE_INFO_SLOT, GuiItems.of(Material.NETHER_STAR,
                services.messages().render(MessageKey.GUI_SKILL_MILESTONE_ITEM), nextMilestoneLore(definition, progress.level())));
        inventory.setItem(LEADERBOARD_SLOT, GuiItems.of(Material.GOLD_INGOT,
                services.messages().render(MessageKey.GUI_SKILL_LEADERBOARD_ITEM)));
    }

    private long requiredForLevel(int level) {
        return level >= services.skills().settings().curve().maximumLevel()
                ? 0L
                : services.skills().settings().curve().requiredForCurrentLevel(level);
    }

    /** Points at the tier the player is actually working toward, rather than repeating the full 25/50/75/100 table the info screen already lists. */
    private List<Component> nextMilestoneLore(SkillDefinition definition, int level) {
        List<Component> lore = new ArrayList<>();
        definition.milestones().entrySet().stream()
                .filter(entry -> entry.getKey() > level)
                .min(Map.Entry.comparingByKey())
                .ifPresent(entry -> lore.add(services.messages().render(MessageKey.GUI_SKILL_MILESTONE_NEXT, Map.of(
                        "seviye", Integer.toString(entry.getKey()),
                        "kalan", Integer.toString(entry.getKey() - level)
                ))));
        lore.add(services.messages().render(MessageKey.GUI_SKILL_MILESTONE_OPEN_HINT));
        return lore;
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        switch (slot) {
            case BACK_SLOT -> GuiManager.open(player, new SkillsMenu(services));
            case MILESTONE_INFO_SLOT -> GuiManager.open(player, new SkillMilestoneInfoMenu(services, skillId));
            case LEADERBOARD_SLOT -> {
                SkillDefinition definition = services.skills().settings().definitions().get(skillId);
                if (definition != null) {
                    GuiManager.open(player, new LeaderboardDetailMenu(services, skillId, definition.displayName()));
                }
            }
            default -> {
            }
        }
    }
}
