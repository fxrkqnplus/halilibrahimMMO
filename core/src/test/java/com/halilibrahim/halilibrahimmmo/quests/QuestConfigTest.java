package com.halilibrahim.halilibrahimmmo.quests;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QuestConfigTest {

    private static final Set<String> SUPPORTED_TYPES = Set.of("PERMANENT", "DAILY", "WEEKLY");
    private static final Set<String> SUPPORTED_OBJECTIVES = Set.of("BLOCK_BREAK", "MOB_KILL");

    // Material#isBlock() needs a live RegistryAccess unavailable outside a running
    // Paper server, so - like SkillMaterialConfigTest - block-ness is asserted
    // against a known-block allow list rather than calling it from this test.
    private static final Set<String> SUPPORTED_BLOCK_MATERIALS = Set.of(
            "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG", "ACACIA_LOG", "DARK_OAK_LOG", "MANGROVE_LOG", "CHERRY_LOG", "PALE_OAK_LOG",
            "DEEPSLATE_DIAMOND_ORE", "DEEPSLATE_EMERALD_ORE", "ANCIENT_DEBRIS", "EMERALD_ORE", "DIAMOND_ORE",
            "COAL_ORE", "DEEPSLATE_COAL_ORE", "IRON_ORE", "DEEPSLATE_IRON_ORE", "COPPER_ORE", "DEEPSLATE_COPPER_ORE",
            "GOLD_ORE", "DEEPSLATE_GOLD_ORE", "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE", "REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE",
            "DIRT", "COARSE_DIRT", "GRASS_BLOCK", "GRAVEL", "SAND", "RED_SAND", "CLAY", "PODZOL", "MYCELIUM", "ROOTED_DIRT",
            "NETHER_GOLD_ORE", "NETHER_QUARTZ_ORE", "CRIMSON_STEM"
    );

    @Test
    void bundledQuestsDeclareValidTypesObjectivesAndPositiveRewards() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("quests.yml"), StandardCharsets.UTF_8));
        assertTrue(yaml.getInt("gunluk-atanan-sayisi") > 0);
        assertTrue(yaml.getInt("haftalik-atanan-sayisi") > 0);
        ConfigurationSection quests = yaml.getConfigurationSection("gorevler");
        assertNotNull(quests);
        assertTrue(quests.getKeys(false).size() >= 1);
        for (String id : quests.getKeys(false)) {
            assertNotNull(quests.getString(id + ".gorunen-ad"), id + " is missing gorunen-ad");
            assertNotNull(quests.getString(id + ".aciklama"), id + " is missing aciklama");
            assertNotNull(Material.matchMaterial(quests.getString(id + ".simge", "")), id + " has an unknown simge material");
            assertTrue(SUPPORTED_TYPES.contains(quests.getString(id + ".tur")), id);
            String objective = quests.getString(id + ".hedef.tur");
            assertTrue(SUPPORTED_OBJECTIVES.contains(objective), id);
            assertTrue(quests.getInt(id + ".sayi") > 0, id);
            assertTrue(quests.getLong(id + ".xp") > 0, id);
            if ("BLOCK_BREAK".equals(objective)) {
                for (String materialName : quests.getStringList(id + ".hedef.malzemeler")) {
                    assertNotNull(Material.matchMaterial(materialName), id + " -> " + materialName);
                    assertTrue(SUPPORTED_BLOCK_MATERIALS.contains(materialName), id + " -> " + materialName);
                }
            } else {
                for (String entityName : quests.getStringList(id + ".hedef.varliklar")) {
                    assertDoesNotThrow(() -> EntityType.valueOf(entityName), id + " -> " + entityName);
                }
            }
        }
    }
}
