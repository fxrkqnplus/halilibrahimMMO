package com.halilibrahim.halilibrahimmmo.onboarding;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import org.bukkit.Material;

/**
 * A fixed, small set of one-time tutorial nudges - deliberately not
 * YAML-configurable (unlike quests) since this is a tiny, stable list, not a
 * gameplay-tunable catalogue.
 */
public enum OnboardingTask {
    ILK_MENU_ACMA("ilk-menu-acma", 150, MessageKey.GUI_QUESTS_ONBOARDING_MENU, Material.COMPASS),
    BECERILERI_INCELEME("becerileri-inceleme", 150, MessageKey.GUI_QUESTS_ONBOARDING_SKILLS, Material.IRON_PICKAXE),
    KABILIYETLERI_INCELEME("kabiliyetleri-inceleme", 150, MessageKey.GUI_QUESTS_ONBOARDING_ABILITIES, Material.BLAZE_POWDER),
    BECERI_KOMUTU_KULLANMA("beceri-komutu-kullanma", 150, MessageKey.GUI_QUESTS_ONBOARDING_SKILL_COMMAND, Material.PAPER);

    private final String id;
    private final long rewardExperience;
    private final MessageKey displayKey;
    private final Material icon;

    OnboardingTask(String id, long rewardExperience, MessageKey displayKey, Material icon) {
        this.id = id;
        this.rewardExperience = rewardExperience;
        this.displayKey = displayKey;
        this.icon = icon;
    }

    public String id() {
        return id;
    }

    public long rewardExperience() {
        return rewardExperience;
    }

    /**
     * What the quest board calls this task. A {@link MessageKey} rather than a
     * string, for the same reason everything else a player reads is one - and
     * it lives on the enum because the enum is already the one place that knows
     * this list is fixed and complete.
     */
    public MessageKey displayKey() {
        return displayKey;
    }

    public Material icon() {
        return icon;
    }

    /** Whether {@code completedIds} covers every task there is. */
    public static boolean allCompleted(java.util.Set<String> completedIds) {
        for (OnboardingTask task : values()) {
            if (!completedIds.contains(task.id())) {
                return false;
            }
        }
        return true;
    }
}
