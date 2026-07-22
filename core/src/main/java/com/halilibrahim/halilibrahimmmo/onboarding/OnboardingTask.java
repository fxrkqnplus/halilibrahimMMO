package com.halilibrahim.halilibrahimmmo.onboarding;

/**
 * A fixed, small set of one-time tutorial nudges - deliberately not
 * YAML-configurable (unlike quests) since this is a tiny, stable list, not a
 * gameplay-tunable catalogue.
 */
public enum OnboardingTask {
    ILK_MENU_ACMA("ilk-menu-acma", 50),
    BECERILERI_INCELEME("becerileri-inceleme", 50),
    KABILIYETLERI_INCELEME("kabiliyetleri-inceleme", 50),
    BECERI_KOMUTU_KULLANMA("beceri-komutu-kullanma", 50);

    private final String id;
    private final long rewardExperience;

    OnboardingTask(String id, long rewardExperience) {
        this.id = id;
        this.rewardExperience = rewardExperience;
    }

    public String id() {
        return id;
    }

    public long rewardExperience() {
        return rewardExperience;
    }
}
