package com.halilibrahim.halilibrahimmmo.modcheck;

/**
 * Thrown when a client's reply on the {@code hmmo:modcheck} channel cannot be
 * trusted: wrong protocol version, truncated payload, an implausible mod count,
 * or trailing bytes. Never swallowed - a malformed reply always fails
 * verification rather than being treated as an empty mod list.
 */
public final class ModCheckProtocolException extends RuntimeException {

    public ModCheckProtocolException(String message) {
        super(message);
    }

    public ModCheckProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
