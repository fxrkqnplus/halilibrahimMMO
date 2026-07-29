package com.halilibrahim.halilibrahimmmo.modcheck;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The single place the {@code hmmo:modcheck} wire format is written and read.
 * Both halves of the handshake (this plugin and the {@code hmmo-client} Fabric
 * mod) must agree byte-for-byte, so the format lives in exactly one class here
 * and is mirrored in exactly one class there.
 *
 * <p><strong>Request</strong> (server to client): protocol version byte,
 * then {@value #NONCE_LENGTH} nonce bytes.</p>
 *
 * <p><strong>Response</strong> (client to server): protocol version byte,
 * {@value #NONCE_LENGTH} nonce bytes echoed back unchanged, a mod count, then
 * that many {@code (id, version)} UTF pairs.</p>
 *
 * <p>The nonce stops a replayed recording of somebody else's reply from being
 * accepted. It is explicitly <em>not</em> a signature: the client is untrusted
 * by construction (see {@code modlar.yml}'s header).</p>
 */
public final class ModCheckProtocol {

    /** Plugin messaging channel, registered both incoming and outgoing. */
    public static final String CHANNEL = "hmmo:modcheck";

    public static final byte PROTOCOL_VERSION = 1;

    public static final int NONCE_LENGTH = 16;

    /**
     * Hard ceiling on a single reply, well under Minecraft's ~32 KiB custom
     * payload limit. A reply larger than this is rejected outright instead of
     * being parsed, so a hostile client cannot make the server allocate on demand.
     */
    private static final int MAXIMUM_PAYLOAD_BYTES = 30_000;

    private ModCheckProtocol() {
    }

    public static byte[] request(byte[] nonce) {
        if (nonce == null || nonce.length != NONCE_LENGTH) {
            throw new IllegalArgumentException("Nonce must be exactly " + NONCE_LENGTH + " bytes.");
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(1 + NONCE_LENGTH);
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeByte(PROTOCOL_VERSION);
            out.write(nonce);
        } catch (IOException exception) {
            throw new ModCheckProtocolException("Mod check request could not be written.", exception);
        }
        return buffer.toByteArray();
    }

    /**
     * Parses a client reply. Every field is bounds-checked before it is used.
     *
     * @param maximumModCount configured ceiling from {@code modlar.yml}
     * @throws ModCheckProtocolException on any structural problem
     */
    public static ModCheckResponse readResponse(byte[] payload, int maximumModCount) {
        if (payload == null || payload.length == 0) {
            throw new ModCheckProtocolException("Mod check response was empty.");
        }
        if (payload.length > MAXIMUM_PAYLOAD_BYTES) {
            throw new ModCheckProtocolException("Mod check response was too large: " + payload.length + " bytes.");
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            byte version = in.readByte();
            if (version != PROTOCOL_VERSION) {
                throw new ModCheckProtocolException("Unsupported mod check protocol version: " + version);
            }

            byte[] nonce = new byte[NONCE_LENGTH];
            in.readFully(nonce);

            int count = in.readInt();
            if (count < 0 || count > maximumModCount) {
                throw new ModCheckProtocolException("Implausible reported mod count: " + count);
            }

            Map<String, String> mods = new LinkedHashMap<>();
            for (int index = 0; index < count; index++) {
                String id = in.readUTF().trim().toLowerCase(Locale.ROOT);
                String modVersion = in.readUTF().trim();
                if (id.isEmpty() || modVersion.isEmpty()) {
                    throw new ModCheckProtocolException("Reported mod entry had a blank id or version.");
                }
                mods.put(id, modVersion);
            }

            int trailing = in.available();
            if (trailing != 0) {
                throw new ModCheckProtocolException("Mod check response had " + trailing + " trailing bytes.");
            }
            return new ModCheckResponse(nonce, Map.copyOf(mods));
        } catch (IOException exception) {
            throw new ModCheckProtocolException("Mod check response was truncated or malformed.", exception);
        }
    }
}
