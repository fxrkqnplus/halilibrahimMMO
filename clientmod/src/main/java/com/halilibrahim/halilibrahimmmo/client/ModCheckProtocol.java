package com.halilibrahim.halilibrahimmmo.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Byte-for-byte mirror of the server plugin's
 * {@code com.halilibrahim.halilibrahimmmo.modcheck.ModCheckProtocol}.
 *
 * <p><strong>If you change one, change the other in the same commit.</strong>
 * There is no shared module between the plugin and this mod - they are built by
 * two different toolchains against two different platforms - so this duplication
 * is deliberate and is the only place it exists.</p>
 */
final class ModCheckProtocol {

    /** Split into two parts because {@code Identifier} is built from a namespace and a path. */
    static final String NAMESPACE = "hmmo";

    static final String PATH = "modcheck";

    /** Must stay identical to the server plugin's {@code ModCheckProtocol.CHANNEL}. */
    static final String CHANNEL = NAMESPACE + ":" + PATH;

    static final byte PROTOCOL_VERSION = 1;

    static final int NONCE_LENGTH = 16;

    /**
     * Ceiling on the reply we are willing to build, matching the server's own
     * limit. A pack large enough to exceed it is a configuration problem, and
     * failing loudly beats sending a silently truncated list that would get the
     * player kicked for "missing" mods they actually have.
     */
    static final int MAXIMUM_PAYLOAD_BYTES = 30_000;

    private ModCheckProtocol() {
    }

    /** @return the nonce carried by a server challenge, or {@code null} if the payload is not one. */
    static byte[] readRequestNonce(byte[] payload) {
        if (payload == null || payload.length != 1 + NONCE_LENGTH) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (in.readByte() != PROTOCOL_VERSION) {
                return null;
            }
            byte[] nonce = new byte[NONCE_LENGTH];
            in.readFully(nonce);
            return nonce;
        } catch (IOException exception) {
            return null;
        }
    }

    /**
     * @return the reply payload, or {@code null} when it would exceed
     *         {@link #MAXIMUM_PAYLOAD_BYTES}. The caller then sends nothing and
     *         lets the server's timeout report a clear failure.
     */
    static byte[] response(byte[] nonce, Map<String, String> mods) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeByte(PROTOCOL_VERSION);
            out.write(nonce);
            out.writeInt(mods.size());
            for (Map.Entry<String, String> mod : mods.entrySet()) {
                out.writeUTF(mod.getKey());
                out.writeUTF(mod.getValue());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        byte[] payload = buffer.toByteArray();
        return payload.length > MAXIMUM_PAYLOAD_BYTES ? null : payload;
    }
}
