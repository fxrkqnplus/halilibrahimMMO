package com.halilibrahim.halilibrahimmmo.classes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClassConfigTest {
    @Test
    void bundledClassesUseBoundedEffectsAndKnownAbilities() {
        YamlConfiguration classes = load("classes.yml");
        YamlConfiguration abilities = load("abilities.yml");
        ConfigurationSection definitions = classes.getConfigurationSection("siniflar");
        assertNotNull(definitions);
        assertEquals(4, definitions.getKeys(false).size());
        assertEquals(7, classes.getLong("sinif-degistirme-bekleme-gun"));
        assertEquals(25, classes.getInt("seviye-basina-yetenek-puani"));
        for (String classId : definitions.getKeys(false)) {
            ConfigurationSection prestigeEffect = definitions.getConfigurationSection(classId + ".prestij-etkisi");
            assertNotNull(prestigeEffect, classId + " is missing prestij-etkisi");
            ClassEffectType prestigeType = ClassEffectType.valueOf(prestigeEffect.getString("tur"));
            assertTrue(prestigeType == ClassEffectType.MAX_HEALTH
                    || prestigeType == ClassEffectType.PVE_DAMAGE_MULTIPLIER
                    || prestigeType == ClassEffectType.MAX_ENERGY, classId + " has an unsupported prestij-etkisi tur");
            assertTrue(prestigeEffect.getDouble("deger-basina") > 0, classId + " prestij-etkisi deger-basina must be positive");
            ConfigurationSection talents = definitions.getConfigurationSection(classId + ".yetenekler");
            assertNotNull(talents);
            assertEquals(3, talents.getKeys(false).size());
            for (String talentId : talents.getKeys(false)) {
                String path = classId + ".yetenekler." + talentId;
                assertEquals(4, talents.getInt(talentId + ".maksimum-rank"));
                ClassEffectType type = ClassEffectType.valueOf(talents.getString(talentId + ".etki.tur"));
                String abilityId = talents.getString(talentId + ".etki.kabiliyet", "");
                boolean abilityEffect = switch (type) {
                    case ABILITY_ENERGY_COST_REDUCTION, ABILITY_COOLDOWN_REDUCTION, ABILITY_LIMIT_BONUS -> true;
                    default -> false;
                };
                assertEquals(abilityEffect, !abilityId.isBlank(), path);
                if (abilityEffect) assertTrue(abilities.contains("kabiliyetler." + abilityId), path + " references an unknown ability");
            }
        }
    }

    private YamlConfiguration load(String resource) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(resource), StandardCharsets.UTF_8));
    }
}
