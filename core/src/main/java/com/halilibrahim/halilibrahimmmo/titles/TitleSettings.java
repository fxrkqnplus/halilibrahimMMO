package com.halilibrahim.halilibrahimmmo.titles;

import java.util.Map;

/** Immutable parsed {@code titles.yml} snapshot. */
public record TitleSettings(Map<String, TitleDefinition> definitions) {
}
