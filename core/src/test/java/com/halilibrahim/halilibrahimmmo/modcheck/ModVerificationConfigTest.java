package com.halilibrahim.halilibrahimmmo.modcheck;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural validation of the bundled {@code modlar.yml}, in the same spirit
 * as {@code QuestConfigTest}/{@code TitleConfigTest}: a broken shipped default
 * would kick every player on the server, so it is worth failing the build over.
 */
final class ModVerificationConfigTest {

    private static YamlConfiguration bundled() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                ModVerificationConfigTest.class.getClassLoader().getResourceAsStream("modlar.yml"),
                StandardCharsets.UTF_8));
    }

    @Test
    void verificationSectionIsSane() {
        ConfigurationSection section = bundled().getConfigurationSection("dogrulama");
        assertNotNull(section);
        assertTrue(section.getLong("zaman-asimi-saniye") > 0);
        assertTrue(section.getLong("brand-gecikme-tik") >= 0);
        assertFalse(section.getString("bypass-izni", "").isBlank());
        // Fabric API alone reports ~100 jar-in-jar sub-modules, so a low
        // ceiling would reject a completely normal client.
        assertTrue(section.getInt("azami-mod-sayisi") >= 256);
    }

    @Test
    void everyRequiredModParsesAndHasUniqueId() {
        List<Map<?, ?>> entries = bundled().getMapList("zorunlu-modlar");
        assertFalse(entries.isEmpty());

        Set<String> ids = new HashSet<>();
        for (Map<?, ?> entry : entries) {
            RequiredMod mod = assertDoesNotThrow(() -> new RequiredMod(
                    String.valueOf(entry.get("id")),
                    String.valueOf(entry.get("gorunen-ad")),
                    String.valueOf(entry.get("minimum-versiyon"))
            ));
            assertTrue(ids.add(mod.id()), "Duplicate required mod id: " + mod.id());
        }
    }

    @Test
    void everyMinimumVersionIsComparable() {
        for (Map<?, ?> entry : bundled().getMapList("zorunlu-modlar")) {
            String version = String.valueOf(entry.get("minimum-versiyon"));
            assertDoesNotThrow(() -> ModVersions.compare(version, version),
                    "Minimum version is not comparable: " + entry.get("id") + " -> " + version);
        }
    }

    /** The companion mod and the loader are what make the whole system work; neither may be dropped. */
    @Test
    void companionModAndLoaderAreRequired() {
        Set<String> ids = new HashSet<>();
        for (Map<?, ?> entry : bundled().getMapList("zorunlu-modlar")) {
            ids.add(String.valueOf(entry.get("id")));
        }
        assertTrue(ids.contains("hmmo-client"));
        assertTrue(ids.contains("fabricloader"));
        assertTrue(ids.contains("fabric-api"));
    }

    @Test
    void forbiddenModsParseWhenPresent() {
        for (Map<?, ?> entry : bundled().getMapList("yasakli-modlar")) {
            assertDoesNotThrow(() -> new ForbiddenMod(
                    String.valueOf(entry.get("id")),
                    String.valueOf(entry.get("gorunen-ad"))
            ));
        }
    }

    /** No required mod may also be forbidden - that would kick literally everybody. */
    @Test
    void requiredAndForbiddenListsDoNotOverlap() {
        Set<String> required = new HashSet<>();
        for (Map<?, ?> entry : bundled().getMapList("zorunlu-modlar")) {
            required.add(String.valueOf(entry.get("id")));
        }
        for (Map<?, ?> entry : bundled().getMapList("yasakli-modlar")) {
            assertFalse(required.contains(String.valueOf(entry.get("id"))),
                    "Mod is both required and forbidden: " + entry.get("id"));
        }
    }

    @Test
    void bundledListMatchesTheApprovedPack() {
        // 24 pack mods + Fabric Loader + the companion mod.
        assertEquals(26, bundled().getMapList("zorunlu-modlar").size());
    }
}
