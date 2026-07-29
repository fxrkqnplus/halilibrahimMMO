package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.collections.CollectionDefinition;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.NumberFormatter;
import com.halilibrahim.halilibrahimmmo.quests.QuestDefinition;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockCondition;
import net.kyori.adventure.text.Component;

import java.util.Map;

/**
 * Renders "what would unlock this" for a locked title or cosmetic.
 *
 * <p>A locked entry deliberately shows no name at all - a title is something to
 * be earned and then worn, and reading the whole catalogue off a menu spends
 * the surprise for nothing. What it does show is the requirement, because a
 * locked row with no explanation is not a goal, it is just a gap.</p>
 *
 * <p>Shared by both screens rather than duplicated, unlike most of this
 * codebase's command/GUI pairs: titles and cosmetics are structurally the same
 * system over the same {@code unlock} condition model, so the two copies could
 * only ever drift from each other, never usefully differ.</p>
 */
final class UnlockRequirementLore {

    private UnlockRequirementLore() {
        throw new UnsupportedOperationException("UnlockRequirementLore is a static utility class");
    }

    static Component of(GuiServices services, UnlockCondition condition) {
        return switch (condition.type()) {
            case LEVEL -> services.messages().render(MessageKey.GUI_UNLOCK_REQUIREMENT_LEVEL, Map.of(
                    "seviye", Integer.toString(condition.level())
            ));
            case PRESTIGE -> services.messages().render(MessageKey.GUI_UNLOCK_REQUIREMENT_PRESTIGE, Map.of(
                    "prestij", Integer.toString(condition.level())
            ));
            case QUEST_COMPLETED -> services.messages().render(MessageKey.GUI_UNLOCK_REQUIREMENT_QUEST, Map.of(
                    "gorev", questName(services, condition.referenceId())
            ));
            case COLLECTION_COMPLETED -> services.messages().render(MessageKey.GUI_UNLOCK_REQUIREMENT_COLLECTION, Map.of(
                    "koleksiyon", collectionName(services, condition.referenceId())
            ));
            case SKILL_LEVEL -> services.messages().render(MessageKey.GUI_UNLOCK_REQUIREMENT_SKILL, Map.of(
                    "beceri", skillName(services, condition.referenceId()),
                    "seviye", Integer.toString(condition.level())
            ));
            case WEALTH -> services.messages().render(MessageKey.GUI_UNLOCK_REQUIREMENT_WEALTH, Map.of(
                    "zenginlik", NumberFormatter.compact(condition.amount())
            ));
        };
    }

    /** Falls back to the raw id if the referenced entry was removed from configuration after the condition was written. */
    private static String questName(GuiServices services, String questId) {
        QuestDefinition definition = services.quests().settings().definitions().get(questId);
        return definition == null ? questId : definition.displayName();
    }

    private static String collectionName(GuiServices services, String collectionId) {
        CollectionDefinition definition = services.collections().settings().definitions().get(collectionId);
        return definition == null ? collectionId : definition.displayName();
    }

    private static String skillName(GuiServices services, String skillId) {
        SkillDefinition definition = services.skills().settings().definitions().get(skillId);
        return definition == null ? skillId : definition.displayName();
    }
}
