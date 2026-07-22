package com.halilibrahim.halilibrahimmmo.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkillMaterialConfigTest {

    private static final Set<String> SUPPORTED_MILESTONE_TIERS = Set.of("25", "50", "75", "100");
    private static final Set<String> SUPPORTED_MILESTONE_EFFECT_TYPES = Set.of(
            "MAX_HEALTH", "PVE_DAMAGE_MULTIPLIER", "MAX_ENERGY", "HASTE", "SPEED"
    );

    private static final Set<String> SUPPORTED_BLOCK_MATERIALS = Set.of(
            "DIRT", "GRASS_BLOCK", "COARSE_DIRT", "ROOTED_DIRT", "PODZOL", "MYCELIUM", "SAND", "RED_SAND", "GRAVEL", "CLAY", "SOUL_SAND", "SOUL_SOIL",
            "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG", "ACACIA_LOG", "DARK_OAK_LOG", "MANGROVE_LOG", "CHERRY_LOG", "PALE_OAK_LOG", "CRIMSON_STEM", "WARPED_STEM",
            "COAL_ORE", "DEEPSLATE_COAL_ORE", "COPPER_ORE", "DEEPSLATE_COPPER_ORE", "IRON_ORE", "DEEPSLATE_IRON_ORE", "GOLD_ORE", "DEEPSLATE_GOLD_ORE", "REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE", "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE", "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE", "EMERALD_ORE", "DEEPSLATE_EMERALD_ORE", "NETHER_GOLD_ORE", "NETHER_QUARTZ_ORE", "ANCIENT_DEBRIS"
    );

    @Test
    void bundledBlockSkillsReferenceOnlyValidBlockMaterials() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("skills.yml"), StandardCharsets.UTF_8));
        ConfigurationSection skills = yaml.getConfigurationSection("beceriler");
        assertNotNull(skills);
        for (String id : skills.getKeys(false)) {
            ConfigurationSection milestones = skills.getConfigurationSection(id + ".kilometre-taslari");
            assertNotNull(milestones, () -> "Missing kilometre-taslari for skill " + id);
            assertEquals(SUPPORTED_MILESTONE_TIERS, milestones.getKeys(false), () -> "Skill " + id + " must define exactly tiers 25/50/75/100");
            for (String tier : milestones.getKeys(false)) {
                String type = milestones.getString(tier + ".tur");
                assertTrue(SUPPORTED_MILESTONE_EFFECT_TYPES.contains(type), () -> "Unknown milestone effect type in skill " + id + " tier " + tier + ": " + type);
                assertTrue(milestones.getDouble(tier + ".deger", -1) >= 0, () -> "Negative milestone effect value in skill " + id + " tier " + tier);
            }
            if (!"BLOCK".equals(skills.getString(id + ".kaynak.tur"))) {
                continue;
            }
            ConfigurationSection materials = skills.getConfigurationSection(id + ".kaynak.malzemeler");
            assertNotNull(materials, () -> "Block skill is missing kaynak.malzemeler: " + id);
            for (String materialName : materials.getKeys(false)) {
                assertTrue(SUPPORTED_BLOCK_MATERIALS.contains(materialName), () -> "Unknown or non-block material in skills.yml: " + materialName);
                long xp = materials.getLong(materialName + ".xp", -1);
                String displayName = materials.getString(materialName + ".gorunen-ad");
                assertTrue(xp > 0, () -> "Non-positive XP for material " + materialName + " in skill " + id);
                assertNotNull(displayName, () -> "Missing display name for material " + materialName + " in skill " + id);
            }
        }
    }
}
