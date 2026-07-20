package com.halilibrahim.halilibrahimmmo.classes;

import java.util.Map;

/** Immutable configured class with its rankable talent branches. */
public record PlayerClassDefinition(String id, String displayName, Map<String, TalentNodeDefinition> talents) {
}
