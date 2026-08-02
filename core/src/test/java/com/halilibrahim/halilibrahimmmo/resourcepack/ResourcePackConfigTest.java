package com.halilibrahim.halilibrahimmmo.resourcepack;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural validation of the bundled {@code kaynak-paketi.yml}.
 *
 * <p>The load-bearing assertion is {@link #shippedDefaultIsDisabled()}. Because
 * config files are copied out once and never rewritten (§5.3), the bundled file
 * is what a fresh server keeps forever - and it cannot contain a working pack
 * address, because the address is specific to one server. Shipping
 * {@code etkin: true} would therefore turn every new installation into a server
 * that kicks its own first player.</p>
 */
final class ResourcePackConfigTest {

    private static YamlConfiguration bundled() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                ResourcePackConfigTest.class.getClassLoader().getResourceAsStream("kaynak-paketi.yml"),
                StandardCharsets.UTF_8));
    }

    private static ConfigurationSection section() {
        ConfigurationSection section = bundled().getConfigurationSection("kaynak-paketi");
        assertNotNull(section, "kaynak-paketi.yml is missing the 'kaynak-paketi' section.");
        return section;
    }

    @Test
    void shippedDefaultIsDisabled() {
        assertFalse(section().getBoolean("etkin", true),
                "The bundled kaynak-paketi.yml must ship disabled: it has no address, "
                        + "and ship-and-copy means a fresh server keeps this file forever.");
    }

    /** A shipped address or digest would be one server's, and wrong for every other. */
    @Test
    void shippedDefaultCarriesNoAddressOrDigest() {
        assertTrue(section().getString("adres", "x").isBlank());
        assertTrue(section().getString("sha1", "x").isBlank());
    }

    @Test
    void timingValuesAreSane() {
        assertTrue(section().getLong("zaman-asimi-saniye") > 0);
        assertTrue(section().getLong("gecikme-tik") >= 0);
    }

    @Test
    void bypassPermissionIsPresentAndDistinctFromTheModCheckOne() {
        String permission = section().getString("bypass-izni", "");
        assertFalse(permission.isBlank());
        // Two independent gates must be independently waivable; sharing one
        // permission would mean exempting a tester from the pack also exempts
        // them from the mod check.
        assertNotEquals("hmmo.mod.bypass", permission);
    }

    @Test
    void bypassPermissionMatchesTheRecordDefault() {
        assertTrue(ResourcePackSettings.DEFAULT_BYPASS_PERMISSION.equals(section().getString("bypass-izni", "")));
    }

    /**
     * The riskiest feature in the pack ships off, so a first boot cannot show a
     * broken menu even after an owner fills in the address.
     */
    @Test
    void guiBackgroundsShipDisabled() {
        assertFalse(section().getBoolean("arayuz-arkaplani", true));
    }

    /**
     * Parsing the shipped defaults must produce a settings object that reports
     * no complaint (because it is disabled) and offers nothing to anybody.
     */
    @Test
    void shippedDefaultsParseIntoAnInertSettingsObject() {
        ConfigurationSection section = section();
        ResourcePackSettings settings = new ResourcePackSettings(
                section.getBoolean("etkin", false),
                section.getString("adres", ""),
                section.getString("sha1", ""),
                section.getBoolean("zorunlu", true),
                section.getLong("zaman-asimi-saniye", 60L),
                section.getLong("gecikme-tik", 20L),
                section.getString("bypass-izni", ResourcePackSettings.DEFAULT_BYPASS_PERMISSION),
                section.getBoolean("arayuz-arkaplani", false),
                section.getBoolean("eylem-cubugu-barlari", true)
        );
        assertFalse(settings.usable());
        assertFalse(settings.enabled());
    }
}
