package com.halilibrahim.halilibrahimmmo.titles;

import com.halilibrahim.halilibrahimmmo.unlock.UnlockCondition;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockConditionParser;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockConditionType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TitleConfigTest {

    @Test
    void bundledTitlesDeclareValidIconsAndConditions() {
        YamlConfiguration yaml = load("titles.yml");
        YamlConfiguration quests = load("quests.yml");
        YamlConfiguration collections = load("collections.yml");
        ConfigurationSection titles = yaml.getConfigurationSection("unvanlar");
        assertNotNull(titles);
        assertTrue(titles.getKeys(false).size() >= 1);
        for (String id : titles.getKeys(false)) {
            String displayName = titles.getString(id + ".gorunen-ad");
            assertNotNull(displayName, id);
            assertFalse(displayName.isBlank(), id);

            Material icon = Material.matchMaterial(titles.getString(id + ".esya", ""));
            assertNotNull(icon, id);

            TitleChatStyle style = assertDoesNotThrow(
                    () -> TitleChatStyle.valueOf(titles.getString(id + ".sohbet-stili", "")), id);
            assertNotNull(style, id);

            ConfigurationSection kosul = titles.getConfigurationSection(id + ".kosul");
            assertNotNull(kosul, id);
            UnlockCondition condition = assertDoesNotThrow(() -> UnlockConditionParser.parse(kosul, id), id);

            if (condition.type() == UnlockConditionType.QUEST_COMPLETED) {
                assertTrue(quests.contains("gorevler." + condition.referenceId()), id + " references an unknown quest");
            }
            if (condition.type() == UnlockConditionType.COLLECTION_COMPLETED) {
                assertTrue(collections.contains("koleksiyonlar." + condition.referenceId()), id + " references an unknown collection");
            }
        }
    }

    private YamlConfiguration load(String resource) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(resource), StandardCharsets.UTF_8));
    }
}
