package com.halilibrahim.halilibrahimmmo.abilities;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural validation of the bundled {@code abilities.yml}, in the same
 * spirit as {@code ClassConfigTest}/{@code SkillMaterialConfigTest}. Phase 13
 * gave every ability its own {@code gorsel} sound/particle/colour, and a
 * misspelled {@code Sound}/{@code Particle} constant would otherwise only
 * surface as a plugin-enable failure on a live server.
 */
final class AbilityConfigTest {

    /**
     * Checked against hardcoded names rather than {@code AbilityToolTag.valueOf(...)}:
     * that enum's constants each wrap a {@code Tag<Material>} constant, whose class
     * initialiser needs a live Paper {@code RegistryAccess} that does not exist in a
     * plain JUnit run - the same standalone-registry limitation §16 already recorded
     * for {@code Material#isBlock()}.
     */
    private static final Set<String> SUPPORTED_TOOL_TAGS = Set.of("PICKAXE", "AXE", "SHOVEL", "SWORD", "HOE", "ANY");

    private static final Set<String> SOUND_NAMES = constantNames("org.bukkit.Sound");
    private static final Set<String> PARTICLE_NAMES = constantNames("org.bukkit.Particle");

    @Test
    void bundledAbilitiesDeclareValidEffectsToolsAndVisuals() {
        YamlConfiguration abilities = load("abilities.yml");
        YamlConfiguration classes = load("classes.yml");
        ConfigurationSection definitions = abilities.getConfigurationSection("kabiliyetler");
        assertNotNull(definitions);
        for (String id : definitions.getKeys(false)) {
            ConfigurationSection ability = definitions.getConfigurationSection(id);
            assertNotNull(ability, id);
            assertNotNull(ability.getString("gorunen-ad"), id + " is missing gorunen-ad");
            assertNotNull(ability.getString("aciklama"), id + " is missing aciklama");
            assertNotNull(Material.matchMaterial(ability.getString("simge", "")), id + " has an unknown simge material");
            assertTrue(classes.contains("siniflar." + ability.getString("sinif", "")), id + " references an unknown class");
            assertTrue(SUPPORTED_TOOL_TAGS.contains(ability.getString("arac.tur", "")), id + " has an unknown arac.tur");
            AbilityEffectType effect = assertDoesNotThrow(
                    () -> AbilityEffectType.valueOf(ability.getString("etki.tur", "")), id + " has an unknown etki.tur");
            if (effect == AbilityEffectType.ORE_REVEAL) {
                assertTrue(ability.getInt("etki.yaricap", 0) > 0, id + " must declare a positive etki.yaricap");
                double ratio = ability.getDouble("etki.acik-alan-orani", -1);
                assertTrue(ratio > 0 && ratio < 1, id + " must declare etki.acik-alan-orani in (0, 1)");
            }
            assertVisual(ability.getConfigurationSection("gorsel"), id);
        }
    }

    private void assertVisual(ConfigurationSection visual, String id) {
        assertNotNull(visual, id + " is missing its gorsel section");
        assertDoesNotThrow(() -> AbilityColor.valueOf(visual.getString("renk", "")), id + " has an unknown gorsel.renk");
        assertTrue(SOUND_NAMES.contains(visual.getString("ses", "")), id + " has an unknown gorsel.ses");
        assertTrue(PARTICLE_NAMES.contains(visual.getString("partikul", "")), id + " has an unknown gorsel.partikul");
        assertTrue(visual.getInt("partikul-sayisi", -1) >= 0, id + " has a negative gorsel.partikul-sayisi");
        assertTrue(visual.getDouble("ses-seviyesi", 0) > 0, id + " has a non-positive gorsel.ses-seviyesi");
        assertTrue(visual.getDouble("ton", 0) > 0, id + " has a non-positive gorsel.ton");
    }

    /**
     * Reads a Bukkit constant holder's field names <em>without</em> initialising
     * the class ({@code Class.forName(..., false, ...)}). On Paper 26.2 both
     * {@code Sound} and {@code Particle} are registry-backed, so merely touching
     * {@code Sound.valueOf(...)} from a test throws
     * {@code IllegalStateException: No RegistryAccess implementation found} -
     * reflection over the declared fields gives the exact, always-current valid
     * name set with none of that, and needs no hand-maintained allow-list.
     */
    private static Set<String> constantNames(String className) {
        try {
            Class<?> type = Class.forName(className, false, AbilityConfigTest.class.getClassLoader());
            return Arrays.stream(type.getDeclaredFields())
                    .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == type)
                    .map(Field::getName)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Missing Bukkit class on the test classpath: " + className, exception);
        }
    }

    private YamlConfiguration load(String resource) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(resource), StandardCharsets.UTF_8));
    }
}
