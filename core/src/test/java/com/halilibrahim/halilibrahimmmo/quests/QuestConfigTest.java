package com.halilibrahim.halilibrahimmmo.quests;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QuestConfigTest {

    private static final Set<String> SUPPORTED_TYPES = Set.of("PERMANENT", "DAILY", "WEEKLY");
    /** Roughly what fits on a scoreboard line alongside its progress fraction and XP tag. */
    private static final int MAXIMUM_SHORT_NAME_LENGTH = 20;
    private static final Set<String> SUPPORTED_OBJECTIVES =
            Set.of("BLOCK_BREAK", "MOB_KILL", "STRUCTURE_DISCOVER", "ITEM_OBTAIN", "DEED");
    private static final Set<String> SUPPORTED_DIMENSIONS = Set.of("NORMAL", "NETHER", "THE_END");

    // Material#isBlock() needs a live RegistryAccess unavailable outside a running
    // Paper server, so - like SkillMaterialConfigTest - block-ness is asserted
    // against a known-block allow list rather than calling it from this test.
    private static final Set<String> SUPPORTED_BLOCK_MATERIALS = Set.of(
            "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG", "ACACIA_LOG", "DARK_OAK_LOG", "MANGROVE_LOG", "CHERRY_LOG", "PALE_OAK_LOG",
            "DEEPSLATE_DIAMOND_ORE", "DEEPSLATE_EMERALD_ORE", "ANCIENT_DEBRIS", "EMERALD_ORE", "DIAMOND_ORE",
            "COAL_ORE", "DEEPSLATE_COAL_ORE", "IRON_ORE", "DEEPSLATE_IRON_ORE", "COPPER_ORE", "DEEPSLATE_COPPER_ORE",
            "GOLD_ORE", "DEEPSLATE_GOLD_ORE", "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE", "REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE",
            "DIRT", "COARSE_DIRT", "GRASS_BLOCK", "GRAVEL", "SAND", "RED_SAND", "CLAY", "PODZOL", "MYCELIUM", "ROOTED_DIRT",
            "NETHER_GOLD_ORE", "NETHER_QUARTZ_ORE", "CRIMSON_STEM", "GOLD_BLOCK",
            "WHEAT", "CARROTS", "POTATOES", "BEETROOTS", "NETHER_WART"
    );

    /**
     * {@code org.bukkit.generator.structure.Structure} is registry-backed, so
     * merely initialising the class outside a running server throws. Its
     * constant <em>names</em> are read reflectively without initialising it -
     * the technique §13 established for {@code Sound}/{@code Particle}, and
     * strictly better than a hand-maintained allow list because it can never
     * drift from the API.
     */
    private static Set<String> structureNames() throws ClassNotFoundException {
        Class<?> structureClass = Class.forName(
                "org.bukkit.generator.structure.Structure", false, QuestConfigTest.class.getClassLoader());
        return Arrays.stream(structureClass.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.getType().equals(structureClass))
                .map(Field::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> configuredSkillIds() {
        YamlConfiguration skills = YamlConfiguration.loadConfiguration(new InputStreamReader(
                QuestConfigTest.class.getClassLoader().getResourceAsStream("skills.yml"), StandardCharsets.UTF_8));
        ConfigurationSection section = skills.getConfigurationSection("beceriler");
        assertNotNull(section, "skills.yml is missing its beceriler section");
        return section.getKeys(false);
    }

    @Test
    void bundledQuestsDeclareValidTypesObjectivesAndPositiveRewards() throws ClassNotFoundException {
        Set<String> structures = structureNames();
        Set<String> skillIds = configuredSkillIds();
        ConfigurationSection quests = quests();

        for (String id : quests.getKeys(false)) {
            assertNotNull(quests.getString(id + ".gorunen-ad"), id + " is missing gorunen-ad");
            assertNotNull(quests.getString(id + ".aciklama"), id + " is missing aciklama");
            // The sidebar truncates rather than wraps, so a long name there is
            // silently cut off mid-word - this is the only place that catches it.
            String shortName = quests.getString(id + ".kisa-ad");
            assertNotNull(shortName, id + " is missing kisa-ad");
            assertFalse(shortName.isBlank(), id + " has a blank kisa-ad");
            assertTrue(shortName.length() <= MAXIMUM_SHORT_NAME_LENGTH,
                    id + "'s kisa-ad is too long for a sidebar line: " + shortName);
            assertNotNull(Material.matchMaterial(quests.getString(id + ".simge", "")), id + " has an unknown simge material");
            assertTrue(SUPPORTED_TYPES.contains(quests.getString(id + ".tur")), id);
            assertTrue(skillIds.contains(quests.getString(id + ".beceri", "")), id + " must name a configured beceri");
            assertTrue(quests.getInt(id + ".sayi") > 0, id);
            assertTrue(quests.getLong(id + ".xp") > 0, id);

            String objective = quests.getString(id + ".hedef.tur");
            assertTrue(SUPPORTED_OBJECTIVES.contains(objective), id);
            switch (objective) {
                case "BLOCK_BREAK" -> {
                    assertFalse(quests.getStringList(id + ".hedef.malzemeler").isEmpty(), id + " has no malzemeler");
                    for (String materialName : quests.getStringList(id + ".hedef.malzemeler")) {
                        assertNotNull(Material.matchMaterial(materialName), id + " -> " + materialName);
                        assertTrue(SUPPORTED_BLOCK_MATERIALS.contains(materialName), id + " -> " + materialName);
                    }
                }
                case "MOB_KILL" -> {
                    for (String entityName : quests.getStringList(id + ".hedef.varliklar")) {
                        assertDoesNotThrow(() -> EntityType.valueOf(entityName), id + " -> " + entityName);
                    }
                }
                case "STRUCTURE_DISCOVER" -> {
                    assertFalse(quests.getStringList(id + ".hedef.yapilar").isEmpty(), id + " has no yapilar");
                    for (String structureName : quests.getStringList(id + ".hedef.yapilar")) {
                        assertTrue(structures.contains(structureName.toUpperCase(Locale.ROOT)),
                                id + " -> unknown structure " + structureName);
                    }
                }
                case "DEED" -> {
                    String deed = quests.getString(id + ".hedef.eylem", "");
                    assertTrue(QuestDeed.byId(deed).isPresent(), id + " -> unknown deed " + deed);
                }
                default -> {
                    assertFalse(quests.getStringList(id + ".hedef.esyalar").isEmpty(), id + " has no esyalar");
                    for (String itemName : quests.getStringList(id + ".hedef.esyalar")) {
                        assertNotNull(Material.matchMaterial(itemName), id + " -> " + itemName);
                    }
                }
            }
            String dimension = quests.getString(id + ".hedef.boyut", "");
            assertTrue(dimension.isEmpty() || SUPPORTED_DIMENSIONS.contains(dimension),
                    id + " -> unknown hedef.boyut " + dimension);
        }
    }

    /**
     * Every deed must be asked for by something. A {@link QuestDeed} nothing
     * uses is a listener firing for nobody, which is exactly the kind of dead
     * weight that only ever gets noticed once it breaks.
     */
    @Test
    void everyDeedIsUsedByAtLeastOneQuest() {
        ConfigurationSection quests = quests();
        Set<String> used = quests.getKeys(false).stream()
                .map(id -> quests.getString(id + ".hedef.eylem", ""))
                .collect(Collectors.toUnmodifiableSet());
        for (QuestDeed deed : QuestDeed.values()) {
            assertTrue(used.contains(deed.id()), "no quest asks for the " + deed.id() + " deed");
        }
    }

    /**
     * Every configured skill should have somewhere to earn quest XP, now that a
     * quest's reward lands in the skill it names. A skill nothing points at
     * would silently be the one skill quests never train.
     */
    @Test
    void everyConfiguredSkillIsTrainedByAtLeastOneQuest() {
        ConfigurationSection quests = quests();
        Set<String> trained = quests.getKeys(false).stream()
                .map(id -> quests.getString(id + ".beceri", ""))
                .collect(Collectors.toUnmodifiableSet());
        for (String skillId : configuredSkillIds()) {
            assertTrue(trained.contains(skillId), "no quest awards the " + skillId + " skill");
        }
    }

    @Test
    void dailyAndWeeklyPoolsAreLargerThanTheirAssignmentCounts() {
        YamlConfiguration yaml = yaml();
        ConfigurationSection quests = quests();
        long daily = quests.getKeys(false).stream().filter(id -> "DAILY".equals(quests.getString(id + ".tur"))).count();
        long weekly = quests.getKeys(false).stream().filter(id -> "WEEKLY".equals(quests.getString(id + ".tur"))).count();
        assertTrue(yaml.getInt("gunluk-atanan-sayisi") > 0);
        assertTrue(yaml.getInt("haftalik-atanan-sayisi") > 0);
        assertTrue(daily >= yaml.getInt("gunluk-atanan-sayisi"), "daily pool is smaller than the assignment count");
        assertTrue(weekly >= yaml.getInt("haftalik-atanan-sayisi"), "weekly pool is smaller than the assignment count");
    }

    private static YamlConfiguration yaml() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                QuestConfigTest.class.getClassLoader().getResourceAsStream("quests.yml"), StandardCharsets.UTF_8));
    }

    private static ConfigurationSection quests() {
        ConfigurationSection quests = yaml().getConfigurationSection("gorevler");
        assertNotNull(quests);
        assertTrue(quests.getKeys(false).size() >= 1);
        return quests;
    }
}
