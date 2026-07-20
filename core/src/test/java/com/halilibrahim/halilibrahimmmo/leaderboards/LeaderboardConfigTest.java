package com.halilibrahim.halilibrahimmmo.leaderboards;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LeaderboardConfigTest {

    @Test
    void bundledSettingsArePositive() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("leaderboards.yml"), StandardCharsets.UTF_8));
        assertTrue(yaml.getInt("giris-sayisi") > 0);
        assertTrue(yaml.getLong("yenileme-araligi-saniye") > 0);
    }
}
