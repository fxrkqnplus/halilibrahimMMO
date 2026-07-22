package com.halilibrahim.halilibrahimmmo.prestige;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrestigeConfigTest {
    @Test
    void bundledPrestigeSettingsArePositive() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("prestige.yml"), StandardCharsets.UTF_8));
        assertTrue(yaml.getInt("maksimum-prestij") > 0);
        assertTrue(yaml.getInt("gerekli-beceri-seviyesi") > 0);
    }
}
