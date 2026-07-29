package com.halilibrahim.halilibrahimmmo.classes;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClassConfigTest {

    @Test
    void bundledClassesUseBoundedEffectsAndKnownAbilities() {
        YamlConfiguration abilities = load("abilities.yml");
        ConfigurationSection definitions = definitions();
        assertEquals(Set.of("avci", "madenci", "tabiat-varisi"), definitions.getKeys(false));
        for (String classId : definitions.getKeys(false)) {
            String icon = definitions.getString(classId + ".simge", "");
            assertNotNull(Material.matchMaterial(icon), classId + " has an unknown simge material: " + icon);
            String frame = definitions.getString(classId + ".cerceve", "");
            assertNotNull(Material.matchMaterial(frame), classId + " has an unknown cerceve material: " + frame);
            assertTrue(frame.endsWith("_STAINED_GLASS_PANE"), classId + " cerceve should be a stained glass pane: " + frame);
            ConfigurationSection prestigeEffect = definitions.getConfigurationSection(classId + ".prestij-etkisi");
            assertNotNull(prestigeEffect, classId + " is missing prestij-etkisi");
            ClassEffectType prestigeType = ClassEffectType.valueOf(prestigeEffect.getString("tur"));
            assertTrue(prestigeType == ClassEffectType.MAX_HEALTH
                    || prestigeType == ClassEffectType.PVE_DAMAGE_MULTIPLIER
                    || prestigeType == ClassEffectType.MAX_ENERGY, classId + " has an unsupported prestij-etkisi tur");
            assertTrue(prestigeEffect.getDouble("deger-basina") > 0, classId + " prestij-etkisi deger-basina must be positive");
            ConfigurationSection talents = definitions.getConfigurationSection(classId + ".yetenekler");
            assertNotNull(talents);
            assertFalse(talents.getKeys(false).isEmpty(), classId + " must define talents");
            for (String talentId : talents.getKeys(false)) {
                String path = classId + ".yetenekler." + talentId;
                // Four is the house style and most talents use it, but a talent
                // whose rank buys a whole step - an Efficiency level, a Speed
                // level, one energy off every swept block - has to stop sooner
                // or a fully-ranked player gets something the game cannot mean.
                int maximumRank = talents.getInt(talentId + ".maksimum-rank");
                assertTrue(maximumRank >= 1 && maximumRank <= 4, path + " maksimum-rank must be 1-4");
                ClassEffectType type = ClassEffectType.valueOf(talents.getString(talentId + ".etki.tur"));
                String abilityId = talents.getString(talentId + ".etki.kabiliyet", "");
                assertEquals(type.requiresAbility(), !abilityId.isBlank(), path);
                if (type.requiresAbility()) {
                    assertTrue(abilities.contains("kabiliyetler." + abilityId), path + " references an unknown ability");
                }
            }
        }
    }

    /** Both point pools must be reachable in every class, or one of them is dead weight there. */
    @Test
    void everyClassOffersBothTalentPointCategories() {
        ConfigurationSection definitions = definitions();
        for (String classId : definitions.getKeys(false)) {
            ConfigurationSection talents = definitions.getConfigurationSection(classId + ".yetenekler");
            Set<TalentPointCategory> categories = new HashSet<>();
            for (String talentId : talents.getKeys(false)) {
                categories.add(ClassEffectType.valueOf(talents.getString(talentId + ".etki.tur")).category());
            }
            assertEquals(Set.of(TalentPointCategory.OZELLIK, TalentPointCategory.KABILIYET), categories,
                    classId + " must offer both a feature and an ability talent");
        }
    }

    @Test
    void talentPointCadencesAreConfigured() {
        YamlConfiguration classes = load("classes.yml");
        assertEquals(7, classes.getLong("sinif-degistirme-bekleme-gun"));
        ConfigurationSection points = classes.getConfigurationSection("yetenek-puani");
        assertNotNull(points, "classes.yml must define the yetenek-puani section");
        assertTrue(points.getInt("ozellik-seviye-basina") >= 1);
        assertTrue(points.getInt("kabiliyet-seviye-basina") >= 1);
        assertTrue(points.getInt("prestij-basina-ozellik", -1) >= 0);
        assertTrue(points.getInt("prestij-basina-kabiliyet", -1) >= 0);
    }

    /**
     * Every skill must be powered by exactly one class, and every synergy entry
     * must name a real skill - otherwise a skill silently earns no class XP and
     * no synergy bonus for anyone.
     */
    @Test
    void everySkillIsPoweredByExactlyOneClass() {
        Set<String> skillIds = load("skills.yml").getConfigurationSection("beceriler").getKeys(false);
        ConfigurationSection definitions = definitions();
        Set<String> covered = new HashSet<>();
        for (String classId : definitions.getKeys(false)) {
            List<String> synergies = definitions.getStringList(classId + ".sinerji-beceriler");
            assertFalse(synergies.isEmpty(), classId + " must declare at least one sinerji-beceriler entry");
            for (String skillId : synergies) {
                assertTrue(skillIds.contains(skillId), classId + " references an unknown skill: " + skillId);
                assertTrue(covered.add(skillId), "Skill is claimed by more than one class: " + skillId);
            }
        }
        assertEquals(skillIds, covered, "Every configured skill must power exactly one class");
    }

    private ConfigurationSection definitions() {
        ConfigurationSection definitions = load("classes.yml").getConfigurationSection("siniflar");
        assertNotNull(definitions);
        return definitions;
    }

    private YamlConfiguration load(String resource) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(resource), StandardCharsets.UTF_8));
    }
}
