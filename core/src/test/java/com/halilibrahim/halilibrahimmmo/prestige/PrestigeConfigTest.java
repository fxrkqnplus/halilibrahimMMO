package com.halilibrahim.halilibrahimmmo.prestige;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrestigeConfigTest {
    @Test
    void bundledPrestigeSettingsArePositive() {
        YamlConfiguration yaml = load();
        assertTrue(yaml.getInt("maksimum-prestij") > 0);
        assertTrue(yaml.getInt("gerekli-beceri-seviyesi") > 0);
        assertTrue(yaml.getLong("prestij-zenginlik-odulu") >= 0);
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

    private YamlConfiguration load() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("prestige.yml"), StandardCharsets.UTF_8));
    }
}
