package com.halilibrahim.halilibrahimmmo.modcheck;

import java.util.Locale;

/**
 * One entry of {@code modlar.yml}'s {@code zorunlu-modlar} list.
 *
 * <p>{@code id} is the mod's own identifier from its {@code fabric.mod.json} -
 * never its jar file name, which is not authoritative and would kick every
 * correctly-configured player if guessed wrong. {@code minimumVersion} is a
 * lower bound: a player running something newer always passes.</p>
 */
public record RequiredMod(String id, String displayName, String minimumVersion) {

    public RequiredMod {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Required mod id must not be blank.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Required mod display name must not be blank: " + id);
        }
        if (minimumVersion == null || minimumVersion.isBlank()) {
            throw new IllegalArgumentException("Required mod minimum version must not be blank: " + id);
        }
        id = id.trim().toLowerCase(Locale.ROOT);
        displayName = displayName.trim();
        minimumVersion = minimumVersion.trim();
    }
}
