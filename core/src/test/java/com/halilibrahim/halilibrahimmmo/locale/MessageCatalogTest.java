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
    private static final Pattern FONT_TAG = Pattern.compile("<font:([a-z0-9_.-]+:[a-z0-9_./-]+)>");

    /** The one font the resource pack defines for wide, offset and multi-cell glyphs. */
    private static final String PACK_FONT = "hmmo:pano";

    /**
     * Keys allowed to carry Private Use Area codepoints. Everything here is a
     * GUI title or an action bar line - places the pack may draw freely because
     * nothing measures their width. See {@link #privateUseGlyphsStayOutOfLore()}.
     */
    private static final Set<String> PRIVATE_USE_ALLOWED = Set.of(
            // The four menu backgrounds. Container titles are the one surface
            // where a multi-cell glyph is safe: nothing measures their width.
            "gui.arkaplan.satir-3",
            "gui.arkaplan.satir-4",
            "gui.arkaplan.satir-5",
            "gui.arkaplan.satir-6"
    );

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

    /**
     * The mechanical half of the resource pack's "one cell rule".
     *
     * <p>{@code LoreWrapper} wraps item lore at 42 <em>characters</em> and its
     * own javadoc admits it cannot know the client's font. The pack changes that
     * font. The rule that keeps 42 as valid as it is today: icons keep their
     * existing BMP codepoints at one cell each, and every wide, negative-space
     * or multi-cell glyph lives at a Private Use Area codepoint in the separate
     * {@code hmmo:pano} font, usable only where nothing measures width - a GUI
     * title or the action bar.</p>
     *
     * <p>Without this test the rule is a paragraph in PROJECT_MEMORY that the
     * next person to add a fancy glyph will not have read.</p>
     */
    @Test
    void privateUseGlyphsStayOutOfLore() {
        YamlConfiguration yaml = load();
        List<String> offenders = yaml.getKeys(true).stream()
                .filter(yaml::isString)
                .filter(path -> !PRIVATE_USE_ALLOWED.contains(path))
                .filter(path -> containsPrivateUse(yaml.getString(path, "")))
                .toList();
        assertTrue(offenders.isEmpty(),
                "These messages.yml entries use Private Use Area codepoints but are not on the "
                        + "title/action-bar allow-list, so they can reach GuiItems.of(...) and silently "
                        + "invalidate LoreWrapper's 42-character budget: " + offenders);
    }

    /** Every allow-listed key must still exist, or the list is quietly rotting. */
    @Test
    void privateUseAllowListHasNoDeadEntries() {
        YamlConfiguration yaml = load();
        List<String> dead = PRIVATE_USE_ALLOWED.stream().filter(path -> !yaml.isString(path)).toList();
        assertTrue(dead.isEmpty(), "Allow-listed keys that no longer exist in messages.yml: " + dead);
    }

    /**
     * A {@code <font:...>} tag may only name the pack's own font. Pointing one
     * at {@code minecraft:default} would be a no-op, and pointing it at a font
     * the pack does not ship renders every character in it as a white box.
     */
    @Test
    void everyFontTagNamesThePackFont() {
        YamlConfiguration yaml = load();
        List<String> wrong = yaml.getKeys(true).stream()
                .filter(yaml::isString)
                .flatMap(path -> fontNames(yaml.getString(path, "")).stream().map(name -> path + " -> " + name))
                .filter(entry -> !entry.endsWith(" -> " + PACK_FONT))
                .toList();
        assertTrue(wrong.isEmpty(), "messages.yml uses a font other than " + PACK_FONT + ": " + wrong);
    }

    private static boolean containsPrivateUse(String template) {
        return template.codePoints().anyMatch(cp -> cp >= 0xE000 && cp <= 0xF8FF);
    }

    private List<String> fontNames(String template) {
        Matcher matcher = FONT_TAG.matcher(template);
        List<String> names = new java.util.ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
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
