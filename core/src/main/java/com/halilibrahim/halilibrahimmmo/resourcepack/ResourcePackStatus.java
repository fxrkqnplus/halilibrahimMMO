package com.halilibrahim.halilibrahimmmo.resourcepack;

/** Where a currently-online player stands in the resource pack handshake. */
public enum ResourcePackStatus {

    /** Pack sent, waiting for the client to finish downloading (or for the watchdog). */
    PENDING,

    /** The client reported the pack as successfully loaded. */
    APPLIED,

    /**
     * Holder of the bypass permission. Unlike {@code ModVerificationStatus.EXEMPT},
     * an exempt player is still <em>sent</em> the pack - just never forced and
     * never kicked - so staff see the server the way players see it without
     * having to give up their own bypass.
     */
    EXEMPT
}
