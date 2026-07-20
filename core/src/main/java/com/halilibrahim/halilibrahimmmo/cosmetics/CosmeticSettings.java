package com.halilibrahim.halilibrahimmmo.cosmetics;

import java.util.Map;

/** Immutable parsed {@code cosmetics.yml} snapshot. */
public record CosmeticSettings(Map<String, CosmeticDefinition> definitions) {
}
