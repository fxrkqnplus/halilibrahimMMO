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

    public static final byte PROTOCOL_VERSION = 3;

    public static final byte TYPE_CLEAR = 0;
    public static final byte TYPE_ABILITY = 1;
    /** The set of players currently in Asil Kurt's form. About other people, not the recipient. */
    public static final byte TYPE_WOLF_FORMS = 2;

    /** Refuses a longer list outright rather than allocating on a stranger's say-so. */
    private static final int MAXIMUM_WOLF_FORMS = 64;

    /** The screen edge breathes in and out rather than sitting still. */
    public static final int FLAG_PULSE = 1;
    /** A highlighted entity burns brighter the closer it gets. */
    public static final int FLAG_PROXIMITY_GLOW = 2;
    /** Hostile creatures - anything the game counts as an enemy - glow. */
    public static final int FLAG_GLOW_MONSTERS = 4;
    /** Ordinary creatures - animals, villagers, anything that is not an enemy - glow. */
    public static final int FLAG_GLOW_CREATURES = 8;
    /** Other players glow. */
    public static final int FLAG_GLOW_PLAYERS = 16;

    /** Sent as the remaining time for an ability with no expiry worth counting down. */
    public static final int NO_EXPIRY = -1;

    /** Refuses anything larger outright rather than allocating on a stranger's say-so. */
    private static final int MAXIMUM_PAYLOAD_BYTES = 4096;

    private HudProtocol() {
    }

    /**
     * One decoded message.
     *
     * @param abilityId      empty for {@link #TYPE_CLEAR}
     * @param remainingMillis milliseconds left when the message arrived, or
     *                       {@link #NO_EXPIRY}
     */
    /**
     * A decoded wolf-form broadcast.
     *
     * <p>Its own record rather than a field on {@link Message}, because it is a
     * genuinely different message: it says nothing about the recipient's own
     * ability, and mixing it into a record whose every other field would be
     * empty would mean every reader of that record having to know which fields
     * were meaningful this time.</p>
     */
    public record WolfForms(java.util.Set<java.util.UUID> playerIds) {
    }

    /** @return the decoded wolf-form set, or {@code null} when this is not one */
    public static WolfForms readWolfForms(byte[] payload) {
        if (payload == null || payload.length < 3 || payload.length > MAXIMUM_PAYLOAD_BYTES) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (in.readByte() != PROTOCOL_VERSION || in.readByte() != TYPE_WOLF_FORMS) {
                return null;
            }
            int count = in.readByte();
            if (count < 0 || count > MAXIMUM_WOLF_FORMS) {
                return null;
            }
            java.util.Set<java.util.UUID> playerIds = new java.util.HashSet<>(Math.max(1, count));
            for (int index = 0; index < count; index++) {
                playerIds.add(new java.util.UUID(in.readLong(), in.readLong()));
            }
            return new WolfForms(java.util.Set.copyOf(playerIds));
        } catch (IOException exception) {
            return null;
        }
    }

    /** @return the message type byte, or {@code -1} when the payload is not ours at all */
    public static byte peekType(byte[] payload) {
        if (payload == null || payload.length < 2 || payload[0] != PROTOCOL_VERSION) {
            return -1;
        }
        return payload[1];
    }

    public record Message(
            byte type,
            String abilityId,
            String displayName,
            String iconItem,
            int colorRgb,
            int remainingMillis,
            int intensity,
            int flags,
            String comboLine,
            int comboTier
    ) {

        public boolean pulses() {
            return (flags & FLAG_PULSE) != 0;
        }

        public boolean glowScalesWithDistance() {
            return (flags & FLAG_PROXIMITY_GLOW) != 0;
        }

        public boolean glowsMonsters() {
            return (flags & FLAG_GLOW_MONSTERS) != 0;
        }

        public boolean glowsCreatures() {
            return (flags & FLAG_GLOW_CREATURES) != 0;
        }

        public boolean glowsPlayers() {
            return (flags & FLAG_GLOW_PLAYERS) != 0;
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
                return new Message(TYPE_CLEAR, "", "", "", 0, 0, 0, 0, "", 0);
            }
            if (type != TYPE_ABILITY) {
                return null;
            }
            String abilityId = in.readUTF();
            String displayName = in.readUTF();
            String iconItem = in.readUTF();
            int colorRgb = in.readInt();
            int remainingMillis = in.readInt();
            int intensity = in.readByte();
            int flags = in.readByte();
            String comboLine = in.readUTF();
            int comboTier = in.readByte();
            if (abilityId.isEmpty()) {
                return null;
            }
            return new Message(TYPE_ABILITY, abilityId, displayName, iconItem, colorRgb,
                    remainingMillis, Math.max(0, Math.min(100, intensity)), flags,
                    comboLine, Math.max(0, comboTier));
        } catch (IOException exception) {
            return null;
        }
    }
}
