package com.halilibrahim.halilibrahimmmo.api.classes;

import java.util.Collection;

/** Immutable public description of one configured player class. */
public record ClassDefinitionSnapshot(String id, String displayName, Collection<TalentNodeSnapshot> talents) {
}
