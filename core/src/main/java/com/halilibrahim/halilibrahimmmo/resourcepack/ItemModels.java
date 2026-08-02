package com.halilibrahim.halilibrahimmmo.resourcepack;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.Map;

/**
 * Turns the optional {@code model:} line in a definition's YAML into the
 * {@link NamespacedKey} of an item model the resource pack ships.
 *
 * <p>Pure and dependency-free apart from Bukkit's own {@code NamespacedKey}, in
 * the {@code ModVersions}/{@code PackChecksum} mould. One place decides how a
 * short, owner-friendly name like {@code madencilik} becomes
 * {@code hmmo:simge/madencilik}, so the seven parsers that read this key each
 * stay a single line and cannot drift apart.</p>
 *
 * <p><strong>Absent means "unchanged", never "broken".</strong> Every caller
 * gets {@code null} for a missing or blank value, and {@code GuiItems} then
 * builds exactly the item it built before this existed. That is what lets a
 * live server keep its config files unregenerated (§5.3) and still run this
 * version.</p>
 */
public final class ItemModels {

    /** The pack's own namespace; the same one {@code assets/hmmo/...} uses. */
    public static final String NAMESPACE = "hmmo";

    /** All GUI icon models live under one path prefix, so they cannot collide with anything else the pack adds. */
    public static final String PREFIX = "simge/";

    private static final String KEY = "model";

    private ItemModels() {
        throw new UnsupportedOperationException("ItemModels is a static utility class");
    }

    /** @return the model key declared by this section, or {@code null} if it declares none. */
    public static NamespacedKey parse(ConfigurationSection section) {
        return section == null ? null : of(section.getString(KEY));
    }

    /** @return the model key declared by this map-list entry, or {@code null} if it declares none. */
    public static NamespacedKey parse(Map<?, ?> raw) {
        if (raw == null) {
            return null;
        }
        Object value = raw.get(KEY);
        return value == null ? null : of(value.toString());
    }

    /**
     * @param name short model name from YAML, e.g. {@code madencilik}.
     * @return {@code hmmo:simge/<name>}, or {@code null} when blank.
     * @throws IllegalArgumentException if the name is not a legal key path -
     *         a typo here shows the missing-model cube in game and logs
     *         nothing, so it is worth refusing loudly at load time.
     */
    public static NamespacedKey of(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String path = PREFIX + name.trim().toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(NAMESPACE + ":" + path);
        if (key == null) {
            throw new IllegalArgumentException("Gecersiz kaynak paketi model adi: '" + name
                    + "'. Yalnizca kucuk harf, rakam, alt cizgi, tire ve nokta kullanilabilir.");
        }
        return key;
    }
}
