package com.halilibrahim.halilibrahimmmo.modcheck;

import java.util.Locale;

/**
 * One entry of {@code modlar.yml}'s {@code yasakli-modlar} list.
 *
 * <p>Deliberately not an anti-cheat: a modified client can simply omit the mod
 * from the list it reports. This only stops an honest player from joining with
 * something the server does not want, which is exactly what it is for.</p>
 */
public record ForbiddenMod(String id, String displayName) {

    public ForbiddenMod {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Forbidden mod id must not be blank.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Forbidden mod display name must not be blank: " + id);
        }
        id = id.trim().toLowerCase(Locale.ROOT);
        displayName = displayName.trim();
    }
}
