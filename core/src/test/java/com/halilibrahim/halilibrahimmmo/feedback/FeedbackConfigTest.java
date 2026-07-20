package com.halilibrahim.halilibrahimmmo.feedback;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class FeedbackConfigTest {

    @Test
    void bundledDurationIsPositive() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("feedback.yml"), StandardCharsets.UTF_8));
        assertTrue(yaml.getLong("sure-saniye") > 0);
    }
}
