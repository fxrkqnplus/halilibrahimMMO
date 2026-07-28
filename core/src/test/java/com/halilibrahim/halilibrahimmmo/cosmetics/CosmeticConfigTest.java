package com.halilibrahim.halilibrahimmmo.cosmetics;

import com.halilibrahim.halilibrahimmmo.unlock.UnlockCondition;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockConditionParser;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockConditionType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CosmeticConfigTest {

    /**
     * {@code Particle#valueOf()} needs a live registry that does not exist
     * outside a running Paper server - on this version merely initialising the
     * class throws. The constant <em>names</em> can still be read reflectively
     * without initialising it, which gives the exact, always-current valid set
     * and replaces the hand-maintained allow list this test used to carry (and
     * which had to be edited every time a cosmetic gained a new effect).
     */
    private static final Set<String> SUPPORTED_PARTICLES = particleNames();

    private static Set<String> particleNames() {
        try {
            Class<?> type = Class.forName("org.bukkit.Particle", false, CosmeticConfigTest.class.getClassLoader());
            return Arrays.stream(type.getDeclaredFields())
                    .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == type)
                    .map(Field::getName)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Missing Bukkit class on the test classpath: org.bukkit.Particle", exception);
        }
    }

    @Test
    void bundledCosmeticsDeclareValidIconsEffectsAndConditions() {
        YamlConfiguration yaml = load("cosmetics.yml");
        YamlConfiguration quests = load("quests.yml");
        YamlConfiguration collections = load("collections.yml");
        ConfigurationSection cosmetics = yaml.getConfigurationSection("kozmetikler");
        assertNotNull(cosmetics);
        assertTrue(cosmetics.getKeys(false).size() >= 1);
        for (String id : cosmetics.getKeys(false)) {
            String displayName = cosmetics.getString(id + ".gorunen-ad");
            assertNotNull(displayName, id);
            assertFalse(displayName.isBlank(), id);

            Material icon = Material.matchMaterial(cosmetics.getString(id + ".esya", ""));
            assertNotNull(icon, id);

            String effect = cosmetics.getString(id + ".efekt", "");
            assertTrue(SUPPORTED_PARTICLES.contains(effect), id + " -> " + effect);

            ConfigurationSection kosul = cosmetics.getConfigurationSection(id + ".kosul");
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
