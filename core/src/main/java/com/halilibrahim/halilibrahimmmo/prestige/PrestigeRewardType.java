package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import org.bukkit.Material;

import java.util.Locale;
import java.util.Optional;

/**
 * The permanent rewards a player may pick from after a prestige, one per rank.
 *
 * <p>Bounded and enumerated here rather than configurable, for the same reason
 * {@code ClassEffectType} and {@code SkillMilestoneEffectType} are: each member
 * needs real code somewhere to have any effect at all, so a server owner adding
 * a fifth name to a YAML file would only be adding a button that does nothing.
 * <strong>How much each one gives is configuration</strong> and lives in
 * {@code prestige.yml} - that is the part an owner can genuinely retune.</p>
 *
 * <p>All four are deliberately small and permanent rather than large and
 * situational. A prestige already pays out a standing XP bonus and a per-class
 * bonus; this is the part the player chooses, so it has to be a direction they
 * are steering their character in over ten ranks, not a single decisive
 * pick.</p>
 */
public enum PrestigeRewardType {

    /** Extra maximum health, in half-heart points. Applied as its own attribute modifier. */
    CAN(Material.GOLDEN_APPLE, MessageKey.GUI_PRESTIGE_REWARD_HEALTH, MessageKey.GUI_PRESTIGE_REWARD_HEALTH_LORE),
    /** Extra damage to creatures, as a fraction. Summed into the same expression class and milestone bonuses use. */
    HASAR(Material.DIAMOND_SWORD, MessageKey.GUI_PRESTIGE_REWARD_DAMAGE, MessageKey.GUI_PRESTIGE_REWARD_DAMAGE_LORE),
    /** Extra maximum energy. Summed into the same bonus lambda the class and milestone bonuses feed. */
    ENERJI(Material.GLOWSTONE_DUST, MessageKey.GUI_PRESTIGE_REWARD_ENERGY, MessageKey.GUI_PRESTIGE_REWARD_ENERGY_LORE),
    /**
     * Extra skill XP, as a fraction.
     *
     * <p>Scoped to skill XP on purpose, and that is worth knowing before
     * picking it: it therefore also reaches the class track for a synergy
     * skill, since that XP is mirrored across at the amount the skill actually
     * granted - but a quest handed straight to a class is unaffected.</p>
     */
    BILGELIK(Material.ENCHANTED_BOOK, MessageKey.GUI_PRESTIGE_REWARD_WISDOM, MessageKey.GUI_PRESTIGE_REWARD_WISDOM_LORE);

    private final Material icon;
    private final MessageKey nameKey;
    private final MessageKey loreKey;

    PrestigeRewardType(Material icon, MessageKey nameKey, MessageKey loreKey) {
        this.icon = icon;
        this.nameKey = nameKey;
        this.loreKey = loreKey;
    }

    public Material icon() {
        return icon;
    }

    public MessageKey nameKey() {
        return nameKey;
    }

    public MessageKey loreKey() {
        return loreKey;
    }

    /** Tolerant lookup for a value read back out of the database. */
    public static Optional<PrestigeRewardType> byName(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            // A reward removed from the enum after a player had picked it: the
            // row stays, it simply stops counting for anything.
            return Optional.empty();
        }
    }
}
