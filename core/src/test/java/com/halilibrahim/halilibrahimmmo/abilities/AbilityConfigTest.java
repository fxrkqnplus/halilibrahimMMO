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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private static final Set<String> SUPPORTED_TOOL_TAGS =
            Set.of("PICKAXE", "AXE", "SHOVEL", "SWORD", "MELEE", "HOE", "ANY");

    /** The four rungs every class's ability line is built on, weakest to strongest. */
    private static final List<Integer> EXPECTED_UNLOCK_LEVELS = List.of(10, 20, 35, 50);

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
                double ratio = ability.getDouble("etki.acik-alan-orani", -1);
                assertTrue(ratio > 0 && ratio < 1, id + " must declare etki.acik-alan-orani in (0, 1)");
            }
            if (effect == AbilityEffectType.ORE_REVEAL || effect == AbilityEffectType.KNOCKBACK_SLAM
                    || effect == AbilityEffectType.GROWTH_SURGE || effect == AbilityEffectType.LOVE_CALL
                    || effect == AbilityEffectType.BLAST_MINE || effect == AbilityEffectType.BULLDOZER
                    || effect == AbilityEffectType.MINE_BOMB) {
                assertTrue(ability.getInt("etki.yaricap", 0) > 0, id + " must declare a positive etki.yaricap");
            }
            if (effect == AbilityEffectType.LIFE_STEAL) {
                double ratio = ability.getDouble("etki.can-orani", -1);
                assertTrue(ratio > 0 && ratio <= 1, id + " must declare etki.can-orani in (0, 1]");
                int speed = ability.getInt("etki.hiz-seviyesi", 0);
                assertTrue(speed >= 1 && speed <= 5, id + " must declare etki.hiz-seviyesi in [1, 5]");
            }
            // A cube sweep is bounded to a 3x3x3 or a 5x5x5 on purpose: the
            // radius is a cube, so a third step would be 343 blocks a swing.
            if (effect == AbilityEffectType.BULLDOZER || effect == AbilityEffectType.BLAST_MINE) {
                int radius = ability.getInt("etki.yaricap", 0);
                assertTrue(radius <= 2, id + " cube sweep yaricap must be 1 or 2");
            }
            if (effect == AbilityEffectType.TOOL_EFFICIENCY) {
                int levels = ability.getInt("etki.seviye", 0);
                assertTrue(levels >= 1 && levels <= 10, id + " must declare etki.seviye in [1, 10]");
            }
            if (effect == AbilityEffectType.GUARDIAN_CALL) {
                assertTrue(ability.getInt("etki.hayvan-sayisi", 0) > 0, id + " must declare a positive etki.hayvan-sayisi");
                assertTrue(ability.getDouble("etki.hasar", 0) > 0, id + " must declare a positive etki.hasar");
            }
            // Only the two sustained sweeps and the two cube sweeps charge per
            // block; anywhere else the key would be silently ignored.
            if (ability.getInt("enerji-blok-basi", 0) > 0) {
                assertTrue(effect == AbilityEffectType.TREE_FELL || effect == AbilityEffectType.VEIN_MINE
                                || effect == AbilityEffectType.EXCAVATE || effect == AbilityEffectType.BLAST_MINE
                                || effect == AbilityEffectType.BULLDOZER,
                        id + " declares enerji-blok-basi but is not a block sweep");
            }
            assertTrue(ability.getInt("enerji-blok-basi", 0) >= 0, id + " has a negative enerji-blok-basi");
            assertVisual(ability.getConfigurationSection("gorsel"), id);
        }
    }

    /**
     * The shape every class's ability line is designed around: exactly four
     * abilities, unlocking at 10/20/35/50.
     *
     * <p>{@code AbilitiesMenu} lays out four fixed pedestals and four rung
     * markers, so a fifth ability would simply never be shown and a class with
     * three would leave a hole. Both failures are invisible until somebody opens
     * the screen - which is exactly the kind of thing a config test is for.</p>
     */
    @Test
    void everyClassHasFourAbilitiesOnTheSameRungs() {
        YamlConfiguration abilities = load("abilities.yml");
        YamlConfiguration classes = load("classes.yml");
        ConfigurationSection definitions = abilities.getConfigurationSection("kabiliyetler");
        ConfigurationSection classSection = classes.getConfigurationSection("siniflar");
        assertNotNull(classSection);
        for (String classId : classSection.getKeys(false)) {
            List<Integer> levels = definitions.getKeys(false).stream()
                    .filter(id -> classId.equals(definitions.getString(id + ".sinif")))
                    .map(id -> definitions.getInt(id + ".gerekli-seviye"))
                    .sorted()
                    .toList();
            assertEquals(EXPECTED_UNLOCK_LEVELS, levels, classId + " must own exactly four abilities at 10/20/35/50");
        }
    }

    /**
     * Abilities unlock by class level, but their {@code beceri} still gates
     * which blocks they fire on - an ability whose skill is not one its own
     * class powers would be unlockable by a player who never touches that
     * skill, which is the invariant Phase 10.3 confirmed and nothing since has
     * been enforcing.
     */
    @Test
    void everyAbilitysSkillIsOneItsOwnClassPowers() {
        YamlConfiguration abilities = load("abilities.yml");
        YamlConfiguration classes = load("classes.yml");
        ConfigurationSection definitions = abilities.getConfigurationSection("kabiliyetler");
        for (String id : definitions.getKeys(false)) {
            String classId = definitions.getString(id + ".sinif", "");
            String skillId = definitions.getString(id + ".beceri", "");
            assertTrue(classes.getStringList("siniflar." + classId + ".sinerji-beceriler").contains(skillId),
                    id + " uses skill '" + skillId + "', which class '" + classId + "' does not power");
        }
    }

    private void assertVisual(ConfigurationSection visual, String id) {
        assertNotNull(visual, id + " is missing its gorsel section");
        assertDoesNotThrow(() -> AbilityColor.valueOf(visual.getString("renk", "")), id + " has an unknown gorsel.renk");
        assertTrue(SOUND_NAMES.contains(visual.getString("ses", "")), id + " has an unknown gorsel.ses");
        String echo = visual.getString("yanki-ses", "");
        assertTrue(echo.isEmpty() || SOUND_NAMES.contains(echo), id + " has an unknown gorsel.yanki-ses");
        assertDoesNotThrow(() -> AbilityVisualShape.valueOf(visual.getString("sekil", "PATLAMA")),
                id + " has an unknown gorsel.sekil");
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
