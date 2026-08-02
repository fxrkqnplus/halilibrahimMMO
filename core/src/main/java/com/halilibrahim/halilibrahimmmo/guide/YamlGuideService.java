package com.halilibrahim.halilibrahimmmo.guide;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.resourcepack.ItemModels;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** YAML-backed {@link GuideService}, published atomically like every other catalogue here. */
public final class YamlGuideService implements GuideService {

    private static final String FILE_NAME = "rehber.yml";
    /** How many entries one category screen has room for, and therefore how many it may declare. */
    static final int MAXIMUM_ENTRIES_PER_CATEGORY = 28;

    private final JavaPlugin plugin;
    private final AtomicReference<Map<String, GuideCategory>> categories = new AtomicReference<>();

    public YamlGuideService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void disable() {
        // Nothing to release - the catalogue is an in-memory snapshot.
    }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        try {
            categories.set(parse(YamlConfiguration.loadConfiguration(file)));
        } catch (RuntimeException exception) {
            throw new LifecycleException("rehber.yml could not be loaded.", exception);
        }
    }

    @Override
    public List<GuideCategory> categories() {
        return List.copyOf(current().values());
    }

    @Override
    public Optional<GuideCategory> find(String id) {
        return Optional.ofNullable(current().get(id.toLowerCase(Locale.ROOT)));
    }

    private Map<String, GuideCategory> current() {
        Map<String, GuideCategory> value = categories.get();
        if (value == null) {
            throw new IllegalStateException("GuideService.enable() has not run yet.");
        }
        return value;
    }

    private Map<String, GuideCategory> parse(YamlConfiguration yaml) {
        ConfigurationSection root = yaml.getConfigurationSection("kategoriler");
        if (root == null) {
            throw new IllegalArgumentException("rehber.yml must declare a kategoriler section.");
        }
        Map<String, GuideCategory> parsed = new LinkedHashMap<>();
        for (String rawId : root.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+") || parsed.containsKey(id)) {
                throw new IllegalArgumentException("Invalid or duplicate guide category id: " + rawId);
            }
            ConfigurationSection section = requireSection(root, rawId);
            String title = requireText(section, "gorunen-ad", id);
            String summary = requireText(section, "ozet", id);
            Material icon = Material.matchMaterial(section.getString("simge", ""));
            if (icon == null) {
                throw new IllegalArgumentException("Guide category must declare a known simge material: " + id);
            }
            // Optional: names a texture the server resource pack ships.
            // Absent leaves the icon exactly as it was, which is what lets a
            // server keep its unregenerated YAML and still run this version.
            NamespacedKey model = ItemModels.parse(section);
            parsed.put(id, new GuideCategory(id, title, icon, model, summary, parseEntries(section, id)));
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("rehber.yml must define at least one category.");
        }
        return Map.copyOf(parsed);
    }

    private List<GuideEntry> parseEntries(ConfigurationSection category, String categoryId) {
        List<GuideEntry> entries = new ArrayList<>();
        for (Map<?, ?> raw : category.getMapList("konular")) {
            String title = text(raw.get("baslik"));
            Material icon = Material.matchMaterial(text(raw.get("simge")));
            if (title.isBlank() || icon == null) {
                throw new IllegalArgumentException("Guide entry needs a baslik and a known simge: " + categoryId);
            }
            List<String> lines = new ArrayList<>();
            if (raw.get("satirlar") instanceof List<?> rawLines) {
                rawLines.forEach(line -> lines.add(text(line)));
            }
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("Guide entry has no satirlar: " + categoryId + "/" + title);
            }
            entries.add(new GuideEntry(title, icon, lines));
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Guide category has no konular: " + categoryId);
        }
        // Bounded at parse time rather than silently truncated at render time -
        // an entry that never appears is the kind of fault nobody notices until
        // a player asks why the guide is missing a page.
        if (entries.size() > MAXIMUM_ENTRIES_PER_CATEGORY) {
            throw new IllegalArgumentException("Guide category has more than "
                    + MAXIMUM_ENTRIES_PER_CATEGORY + " konular: " + categoryId);
        }
        return entries;
    }

    private String requireText(ConfigurationSection section, String path, String id) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Guide category is missing '" + path + "': " + id);
        }
        return value;
    }

    private ConfigurationSection requireSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing YAML section: " + path);
        }
        return section;
    }

    private String text(Object raw) {
        return raw == null ? "" : String.valueOf(raw);
    }
}
