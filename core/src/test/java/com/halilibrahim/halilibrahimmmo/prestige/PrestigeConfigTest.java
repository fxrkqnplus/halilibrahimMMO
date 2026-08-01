package com.halilibrahim.halilibrahimmmo.prestige;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrestigeConfigTest {
    @Test
    void bundledPrestigeSettingsArePositive() {
        YamlConfiguration yaml = load();
        assertTrue(yaml.getInt("maksimum-prestij") > 0);
        assertTrue(yaml.getInt("gerekli-beceri-seviyesi") > 0);
        assertTrue(yaml.getLong("prestij-sayginlik-odulu") >= 0);
    }

    /**
     * The reward screen offers all four unconditionally, so a missing or
     * zero-valued entry would be a button that lies about what it does. The
     * parser already refuses to load such a file; this catches it one step
     * earlier, where the config edit is actually made.
     */
    @Test
    void everyPermanentRewardHasAPositiveValue() {
        YamlConfiguration yaml = load();
        for (PrestigeRewardType type : PrestigeRewardType.values()) {
            assertTrue(yaml.getDouble("kalici-oduller." + type.name(), -1) > 0,
                    "prestige.yml must declare a positive kalici-oduller." + type.name());
        }
    }

    /**
     * The physical half. Two rules, both of which the parser also enforces:
     * fewer than two crates is not a choice, and a crate with nothing in it is
     * a button that lies. Asserted here as well because this is the file a
     * server owner actually edits, and finding out at boot is finding out too
     * late.
     */
    @Test
    void everyPhysicalRewardOffersAChoiceAndSomethingRealInside() {
        YamlConfiguration yaml = load();
        ConfigurationSection section = yaml.getConfigurationSection("fiziksel-oduller");
        assertNotNull(section, "prestige.yml must declare a fiziksel-oduller section");
        Set<String> ids = section.getKeys(false);
        assertTrue(ids.size() >= 2, "There must be at least two crates to choose between");
        for (String id : ids) {
            ConfigurationSection crate = section.getConfigurationSection(id);
            assertNotNull(crate, "Crate is not a section: " + id);
            assertFalse(crate.getString("gorunen-ad", "").isBlank(), "Crate needs a gorunen-ad: " + id);
            assertNotNull(Material.matchMaterial(crate.getString("simge", "")), "Crate needs a known simge: " + id);
            List<Map<?, ?>> items = crate.getMapList("esyalar");
            assertFalse(items.isEmpty(), "Crate must contain something: " + id);
            for (Map<?, ?> item : items) {
                assertNotNull(Material.matchMaterial(String.valueOf(item.get("esya"))),
                        "Unknown item in crate " + id + ": " + item.get("esya"));
                assertTrue(((Number) item.get("adet")).intValue() > 0, "Item amount must be positive in crate: " + id);
            }
        }
    }

    private YamlConfiguration load() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("prestige.yml"), StandardCharsets.UTF_8));
    }
}
