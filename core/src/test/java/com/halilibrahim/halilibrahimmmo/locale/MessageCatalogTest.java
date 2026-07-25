package com.halilibrahim.halilibrahimmmo.locale;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps {@link MessageKey} and the bundled {@code messages.yml} in exact 1:1
 * agreement, and every {@code <simge:...>} reference pointing at a real
 * {@link IconPack} entry.
 *
 * <p>Both checks used to be a manual pass done once per phase - the Phase 11
 * review found five genuinely dead keys that way. A missing key only shows up
 * as a console warning plus a raw dotted path on screen, and an unknown icon
 * name renders as literal {@code <simge:xxx>} text, so neither fails loudly
 * enough on its own to be noticed in testing.</p>
 */
final class MessageCatalogTest {

    private static final Pattern ICON_TAG = Pattern.compile("<simge:([a-z0-9_-]+)>");

    @Test
    void everyMessageKeyHasExactlyOneMatchingEntry() {
        YamlConfiguration yaml = load();
        Set<String> declared = Stream.of(MessageKey.values()).map(MessageKey::path)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> present = yaml.getKeys(true).stream().filter(yaml::isString)
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(declared, present, "MessageKey and messages.yml must match exactly - no missing, no orphaned entries");
    }

    @Test
    void everyIconTagReferencesAKnownIcon() {
        YamlConfiguration yaml = load();
        List<String> unknown = yaml.getKeys(true).stream()
                .filter(yaml::isString)
                .flatMap(path -> iconNames(yaml.getString(path, "")).stream().map(name -> path + " -> " + name))
                .filter(entry -> IconPack.byId(entry.substring(entry.lastIndexOf(' ') + 1)).isEmpty())
                .toList();
        assertTrue(unknown.isEmpty(), "messages.yml references unknown icon names: " + unknown);
    }

    private List<String> iconNames(String template) {
        Matcher matcher = ICON_TAG.matcher(template);
        List<String> names = new java.util.ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private YamlConfiguration load() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("messages.yml"), StandardCharsets.UTF_8));
    }
}
