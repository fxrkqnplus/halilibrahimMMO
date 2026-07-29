package com.halilibrahim.halilibrahimmmo.feedback;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FeedbackConfigTest {

    private YamlConfiguration bundled() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("feedback.yml"), StandardCharsets.UTF_8));
    }

    /**
     * {@code org.bukkit.Sound} is registry-backed, so merely initialising the
     * class outside a running server throws. Its constant <em>names</em> are
     * read reflectively without initialising it - the technique §13 established,
     * which cannot drift from the API the way a hand-written allow list would.
     */
    private static Set<String> soundNames() throws ClassNotFoundException {
        Class<?> soundClass = Class.forName("org.bukkit.Sound", false, FeedbackConfigTest.class.getClassLoader());
        return Arrays.stream(soundClass.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == soundClass)
                .map(Field::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Test
    void bundledDurationIsPositive() {
        assertTrue(bundled().getLong("sure-saniye") > 0);
    }

    @Test
    @DisplayName("Every named sound cue resolves to a real Sound constant")
    void namedCuesAreRealSounds() throws ClassNotFoundException {
        Set<String> known = soundNames();
        ConfigurationSection sounds = bundled().getConfigurationSection("sesler");
        assertNotNull(sounds, "feedback.yml is missing the sesler section");
        for (String key : sounds.getKeys(false)) {
            String name = sounds.getString(key + ".ses", "").toUpperCase(Locale.ROOT);
            assertTrue(known.contains(name), "sesler." + key + " names an unknown Sound: " + name);
        }
    }

    /**
     * The boss spawn cue is picked at random from this list, so a single wrong
     * name here would only fail on the roll that happened to select it - the
     * kind of defect a config test exists to catch before a live server does.
     */
    @Test
    @DisplayName("Every boss spawn cue resolves to a real Sound constant")
    void bossSpawnCuesAreRealSounds() throws ClassNotFoundException {
        Set<String> known = soundNames();
        var cues = bundled().getMapList("boss-dogus-sesleri");
        assertFalse(cues.isEmpty(), "feedback.yml should ship at least one boss spawn cue");
        for (Map<?, ?> cue : cues) {
            String name = String.valueOf(cue.get("ses")).toUpperCase(Locale.ROOT);
            assertTrue(known.contains(name), "boss-dogus-sesleri names an unknown Sound: " + name);
            assertTrue(((Number) cue.get("ses-seviyesi")).doubleValue() > 0, "Volume must be positive: " + name);
            assertTrue(((Number) cue.get("ton")).doubleValue() > 0, "Pitch must be positive: " + name);
        }
    }
}
