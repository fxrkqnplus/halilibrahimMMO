package com.halilibrahim.halilibrahimmmo.collections;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CollectionConfigTest {

    @Test
    void bundledCollectionsDeclarePositiveRewardsAndValidItems() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("collections.yml"), StandardCharsets.UTF_8));
        ConfigurationSection collections = yaml.getConfigurationSection("koleksiyonlar");
        assertNotNull(collections);
        assertTrue(collections.getKeys(false).size() >= 1);
        for (String id : collections.getKeys(false)) {
            assertTrue(collections.getLong(id + ".tamamlama-xp") > 0, id);
            ConfigurationSection entries = collections.getConfigurationSection(id + ".ogeler");
            assertNotNull(entries, id);
            assertTrue(entries.getKeys(false).size() >= 1, id);
            for (String entryId : entries.getKeys(false)) {
                String path = id + ".ogeler." + entryId;
                assertNotNull(Material.matchMaterial(collections.getString(path + ".esya", "")), path);
                assertTrue(collections.getLong(path + ".xp") > 0, path);
            }
        }
    }
}
