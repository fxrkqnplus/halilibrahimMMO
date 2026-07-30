package com.halilibrahim.halilibrahimmmo.client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * The read half of the {@code hmmo:hud} wire format, a byte-for-byte mirror of
 * the server plugin's class of the same name.
 *
 * <p>Written the same way {@link ModCheckProtocol} is, for the same reason: the
 * two halves are built by two different toolchains and can only agree if the
 * layout lives in exactly one class on each side. Plain JDK streams, no
 * dependencies.</p>
 *
 * <p>Every field is bounds-checked and a malformed message is answered with
 * {@code null} rather than an exception. A HUD is not worth crashing a client
 * over, and a server on an older protocol version simply gets a plain
 * screen.</p>
 */
public final class HudProtocol {

    public static final String NAMESPACE = "hmmo";
    public static final String PATH = "hud";

    public static final byte PROTOCOL_VERSION = 1;

    public static final byte TYPE_CLEAR = 0;
    public static final byte TYPE_ABILITY = 1;

    public static final int FLAG_HIGHLIGHT_HOSTILES = 1;

    /** Sent as the remaining time for an ability with no expiry worth counting down. */
    public static final int NO_EXPIRY = -1;

    /** Refuses anything larger outright rather than allocating on a stranger's say-so. */
    private static final int MAXIMUM_PAYLOAD_BYTES = 4096;

    private HudProtocol() {
    }

    /**
     * One decoded message.
     *
     * @param abilityId     empty for {@link #TYPE_CLEAR}
     * @param remainingTicksAtReceipt milliseconds left when the message arrived,
     *                      or {@link #NO_EXPIRY}
     */
    public record Message(
            byte type,
            String abilityId,
            String displayName,
            String iconItem,
            int colorRgb,
            int remainingMillis,
            int totalMillis,
            int intensity,
            int flags
    ) {

        public boolean highlightsHostiles() {
            return (flags & FLAG_HIGHLIGHT_HOSTILES) != 0;
        }
    }

    /** @return the decoded message, or {@code null} when it is not one we understand */
    public static Message read(byte[] payload) {
        if (payload == null || payload.length == 0 || payload.length > MAXIMUM_PAYLOAD_BYTES) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (in.readByte() != PROTOCOL_VERSION) {
                return null;
            }
            byte type = in.readByte();
            if (type == TYPE_CLEAR) {
                return new Message(TYPE_CLEAR, "", "", "", 0, 0, 0, 0, 0);
            }
            if (type != TYPE_ABILITY) {
                return null;
            }
            String abilityId = in.readUTF();
            String displayName = in.readUTF();
            String iconItem = in.readUTF();
            int colorRgb = in.readInt();
            int remainingMillis = in.readInt();
            int totalMillis = in.readInt();
            int intensity = in.readByte();
            int flags = in.readByte();
            if (abilityId.isEmpty()) {
                return null;
            }
            return new Message(TYPE_ABILITY, abilityId, displayName, iconItem, colorRgb,
                    remainingMillis, Math.max(0, totalMillis), Math.max(0, Math.min(100, intensity)), flags);
        } catch (IOException exception) {
            return null;
        }
    }
}
