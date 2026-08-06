package com.halilibrahim.halilibrahimmmo.prestige;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Faz 25, madde 10: the prestige screen offers all four permanent rewards
 * again, and every one of them still resolves by name.
 *
 * <p>Faz 24 had narrowed the screen to two on the reading that four small
 * trickles were a menu rather than a decision. What the request had actually
 * wanted narrowed was the physical chest; taking a direction away from a build
 * steering towards energy or towards skill XP was a side effect nobody
 * asked for.</p>
 */
class PrestigeRewardSelectionTest {

    @Test
    @DisplayName("The screen offers all four rewards")
    void everyRewardIsSelectable() {
        assertEquals(4, PrestigeRewardType.SELECTABLE_COUNT);
        assertEquals(4, PrestigeRewardType.selectable().size());
    }

    @Test
    @DisplayName("They are offered in declaration order")
    void selectableFollowDeclarationOrder() {
        assertEquals(
                List.of(
                        PrestigeRewardType.CAN,
                        PrestigeRewardType.HASAR,
                        PrestigeRewardType.ENERJI,
                        PrestigeRewardType.BILGELIK
                ),
                PrestigeRewardType.selectable());
    }

    /**
     * The rule §15.4 set for talent ranks whose configuration moved under them,
     * applied here: whatever the screen currently offers, a reward a player has
     * already banked has to keep resolving, because
     * {@link PrestigeRewardType#byName} answers an unknown name with nothing and
     * the stored rows would quietly stop counting.
     */
    @Test
    @DisplayName("Every banked reward name still resolves")
    void everyBankedRewardResolves() {
        for (PrestigeRewardType type : PrestigeRewardType.values()) {
            assertTrue(PrestigeRewardType.byName(type.name()).isPresent(), type.name());
        }
        assertEquals(4, PrestigeRewardType.values().length);
    }
}
