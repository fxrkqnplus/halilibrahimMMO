package com.halilibrahim.halilibrahimmmo.abilities;

import java.util.Map;

/** Complete immutable abilities.yml snapshot, including the shared energy policy. */
public record AbilitySettings(int maximumEnergy, int energyRegenerationPerSecond, long selectionCooldownMillis, Map<String, AbilityDefinition> definitions) {
}
