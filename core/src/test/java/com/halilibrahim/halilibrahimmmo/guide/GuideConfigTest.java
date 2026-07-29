package com.halilibrahim.halilibrahimmmo.guide;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural validation of the bundled {@code rehber.yml}.
 *
 * <p>The guide is the one screen whose whole job is being read, so its failure
 * modes are all quiet: an unknown icon material takes the plugin down at
 * enable, a category with more entries than the board has slots simply loses
 * the last page, and a line long enough to be truncated is only discovered by
 * a player hovering it. All three are cheaper to catch here.</p>
 */
final class GuideConfigTest {

    /**
     * How long a lore row can be before a client truncates it. Not an exact
     * pixel measure - it cannot be, the font is proportional - but a limit
     * comfortably inside where truncation starts for Turkish prose, applied to
     * the visible text with MiniMessage markup stripped.
     */
    private static final int MAXIMUM_VISIBLE_LINE_LENGTH = 78;

    @Test
    void everyCategoryDeclaresAHeadingAnIconAndAtLeastOneEntry() {
        ConfigurationSection categories = load().getConfigurationSection("kategoriler");
        assertNotNull(categories, "rehber.yml must declare a kategoriler section");
        assertFalse(categories.getKeys(false).isEmpty(), "rehber.yml must declare at least one category");
        for (String id : categories.getKeys(false)) {
            ConfigurationSection category = categories.getConfigurationSection(id);
            assertNotNull(category, id);
            assertTrue(id.matches("[a-z0-9_-]+"), id + " must be a lower-case ASCII id");
            assertFalse(category.getString("gorunen-ad", "").isBlank(), id + " is missing gorunen-ad");
            assertFalse(category.getString("ozet", "").isBlank(), id + " is missing ozet");
            assertNotNull(Material.matchMaterial(category.getString("simge", "")), id + " has an unknown simge material");
            List<Map<?, ?>> entries = category.getMapList("konular");
            assertFalse(entries.isEmpty(), id + " has no konular");
            assertTrue(entries.size() <= YamlGuideService.MAXIMUM_ENTRIES_PER_CATEGORY,
                    id + " has more konular than the category screen can show");
        }
    }

    @Test
    void everyEntryHasATitleAKnownIconAndReadableLines() {
        ConfigurationSection categories = load().getConfigurationSection("kategoriler");
        for (String id : categories.getKeys(false)) {
            for (Map<?, ?> entry : categories.getConfigurationSection(id).getMapList("konular")) {
                String title = String.valueOf(entry.get("baslik"));
                assertFalse(title.isBlank(), id + " has an entry with no baslik");
                assertNotNull(Material.matchMaterial(String.valueOf(entry.get("simge"))),
                        id + "/" + title + " has an unknown simge material");
                assertTrue(entry.get("satirlar") instanceof List<?> lines && !lines.isEmpty(),
                        id + "/" + title + " has no satirlar");
                for (Object line : (List<?>) entry.get("satirlar")) {
                    String visible = stripMarkup(String.valueOf(line));
                    assertTrue(visible.length() <= MAXIMUM_VISIBLE_LINE_LENGTH,
                            id + "/" + title + " has a line that will be truncated: " + visible);
                }
            }
        }
    }

    /** Everything a client actually draws - MiniMessage tags occupy no width. */
    private String stripMarkup(String raw) {
        return raw.replaceAll("<[^<>]+>", "");
    }

    private YamlConfiguration load() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("rehber.yml"), StandardCharsets.UTF_8));
    }
}
