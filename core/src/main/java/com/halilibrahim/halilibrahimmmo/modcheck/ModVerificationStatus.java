package com.halilibrahim.halilibrahimmmo.modcheck;

/** Where a currently-online player stands in the mod handshake. */
public enum ModVerificationStatus {

    /** Request sent, waiting for the client's reply (or for the timeout). */
    PENDING,

    /** Reply received and every rule passed. */
    VERIFIED,

    /** Holder of the bypass permission - never asked, never kicked. */
    EXEMPT
}
