package com.halilibrahim.halilibrahimmmo.unlock;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/** Shared {@code kosul:} YAML parsing for titles and cosmetics catalogues. */
public final class UnlockConditionParser {

    private UnlockConditionParser() {
        throw new UnsupportedOperationException("UnlockConditionParser is a static utility class");
    }

    public static UnlockCondition parse(ConfigurationSection kosul, String contextId) {
        UnlockConditionType type = UnlockConditionType.valueOf(kosul.getString("tur", "").toUpperCase(Locale.ROOT));
        return switch (type) {
            case LEVEL -> {
                int level = kosul.getInt("seviye", 0);
                if (level <= 0) {
                    throw new IllegalArgumentException("LEVEL condition requires a positive seviye: " + contextId);
                }
                yield new UnlockCondition(type, level, "", 0L);
            }
            case PRESTIGE -> {
                int prestige = kosul.getInt("prestij", 0);
                if (prestige <= 0) {
                    throw new IllegalArgumentException("PRESTIGE condition requires a positive prestij: " + contextId);
                }
                yield new UnlockCondition(type, prestige, "", 0L);
            }
            case QUEST_COMPLETED -> {
                String questId = kosul.getString("gorev-id", "");
                if (questId.isBlank()) {
                    throw new IllegalArgumentException("QUEST_COMPLETED condition requires gorev-id: " + contextId);
                }
                yield new UnlockCondition(type, 0, questId, 0L);
            }
            case COLLECTION_COMPLETED -> {
                String collectionId = kosul.getString("koleksiyon-id", "");
                if (collectionId.isBlank()) {
                    throw new IllegalArgumentException("COLLECTION_COMPLETED condition requires koleksiyon-id: " + contextId);
                }
                yield new UnlockCondition(type, 0, collectionId, 0L);
            }
            case SKILL_LEVEL -> {
                String skillId = kosul.getString("beceri-id", "");
                int level = kosul.getInt("seviye", 0);
                if (skillId.isBlank() || level <= 0) {
                    throw new IllegalArgumentException("SKILL_LEVEL condition requires beceri-id and a positive seviye: " + contextId);
                }
                yield new UnlockCondition(type, level, skillId, 0L);
            }
            case REPUTATION -> {
                long reputation = kosul.getLong("sayginlik", 0L);
                if (reputation <= 0) {
                    throw new IllegalArgumentException("REPUTATION condition requires a positive sayginlik: " + contextId);
                }
                yield new UnlockCondition(type, 0, "", reputation);
            }
        };
    }
}
