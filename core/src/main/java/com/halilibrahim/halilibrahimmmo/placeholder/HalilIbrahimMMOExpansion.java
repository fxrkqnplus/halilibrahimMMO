package com.halilibrahim.halilibrahimmmo.placeholder;

import com.halilibrahim.halilibrahimmmo.api.CachedPlayerSnapshot;
import com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI;
import com.halilibrahim.halilibrahimmmo.api.classes.ClassDefinitionSnapshot;
import com.halilibrahim.halilibrahimmmo.api.skills.SkillSnapshot;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Optional;

/**
 * PlaceholderAPI expansion exposing this plugin's progression data, e.g.
 * {@code %halilibrahimmmo_level%}, {@code %halilibrahimmmo_skill_madencilik_level%}.
 * Reads exclusively through {@link HalilIbrahimMMOAPI}'s synchronous
 * cached-snapshot methods, the same way any third-party integration would -
 * PlaceholderAPI calls {@link #onPlaceholderRequest(Player, String)}
 * synchronously and cannot await a future.
 */
public final class HalilIbrahimMMOExpansion extends PlaceholderExpansion {

    private static final String SKILL_PREFIX = "skill_";
    private static final String LEVEL_SUFFIX = "_level";
    private static final String XP_SUFFIX = "_xp";

    private final JavaPlugin plugin;
    private final HalilIbrahimMMOAPI api;

    public HalilIbrahimMMOExpansion(JavaPlugin plugin, HalilIbrahimMMOAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public String getIdentifier() {
        return "halilibrahimmmo";
    }

    @Override
    public String getAuthor() {
        return "HalilIbrahim Studio";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        String key = params.toLowerCase(Locale.ROOT);
        if (key.startsWith(SKILL_PREFIX)) {
            return skillPlaceholder(player, key);
        }
        Optional<CachedPlayerSnapshot> cached = api.findCachedPlayer(player.getUniqueId());
        if (cached.isEmpty()) {
            return "";
        }
        CachedPlayerSnapshot snapshot = cached.get();
        return switch (key) {
            case "level" -> Integer.toString(snapshot.globalLevel());
            case "xp" -> Long.toString(snapshot.globalExperience());
            case "xp_to_next_level" -> Long.toString(snapshot.experienceToNextLevel());
            case "prestige" -> Integer.toString(snapshot.prestige());
            case "class" -> classDisplayName(snapshot.selectedClassId());
            case "title" -> snapshot.equippedTitle();
            case "cosmetic" -> snapshot.equippedCosmetic();
            default -> null;
        };
    }

    private String classDisplayName(String classId) {
        if (classId.isEmpty()) {
            return "";
        }
        return api.getClasses().stream()
                .filter(definition -> definition.id().equals(classId))
                .map(ClassDefinitionSnapshot::displayName)
                .findFirst()
                .orElse("");
    }

    private String skillPlaceholder(Player player, String key) {
        String remainder = key.substring(SKILL_PREFIX.length());
        boolean wantsXp;
        String skillId;
        if (remainder.endsWith(LEVEL_SUFFIX)) {
            wantsXp = false;
            skillId = remainder.substring(0, remainder.length() - LEVEL_SUFFIX.length());
        } else if (remainder.endsWith(XP_SUFFIX)) {
            wantsXp = true;
            skillId = remainder.substring(0, remainder.length() - XP_SUFFIX.length());
        } else {
            return null;
        }
        Optional<SkillSnapshot> skill = api.findCachedSkill(player.getUniqueId(), skillId);
        if (skill.isEmpty()) {
            return "0";
        }
        return wantsXp ? Long.toString(skill.get().experience()) : Integer.toString(skill.get().level());
    }
}
