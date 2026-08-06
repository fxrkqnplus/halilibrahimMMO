package com.halilibrahim.halilibrahimmmo.hud;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The single place the {@code hmmo:hud} wire format is written, mirrored
 * byte-for-byte by the {@code hmmo-client} Fabric mod's class of the same name.
 *
 * <p>Written exactly like {@code ModCheckProtocol} and for the same reason: two
 * artefacts built by two different toolchains have to agree on a layout, so the
 * layout lives in one class on each side and nowhere else. Plain JDK
 * {@link DataOutputStream}, no third-party serialisation, no reflection.</p>
 *
 * <p><strong>One-directional.</strong> The server tells the client what to
 * draw; the client never answers. There is nothing the HUD could report that
 * the server does not already know, and a channel the client can talk back on
 * is a channel that has to be validated.</p>
 *
 * <h2>Layout (version 3)</h2>
 * <pre>
 * byte  protocol version
 * byte  message type: 0 = clear, 1 = ability
 * -- type 1 only --
 * UTF   ability id
 * UTF   display name
 * UTF   icon material name (a Bukkit Material name; the client maps it to an item)
 * int   RGB colour of the ability
 * int   milliseconds remaining, or -1 when it does not run out (Creative)
 * byte  intensity, 0-100: how strongly the screen edge is tinted; 0 = no tint at all
 * byte  flags, see the FLAG_ constants
 * UTF   combo line, already rendered to plain text; empty when there is none
 * byte  combo tier, 0-6
 * </pre>
 *
 * <h2>What changed in version 3</h2>
 * <ul>
 *   <li><strong>{@code totalMillis} is gone.</strong> It existed only to drive a
 *   fade that ran across an ability's whole duration; §24 replaced that with a
 *   fixed five-second run-out the client can compute from the remaining time
 *   alone, so the field had no reader left. A field nothing reads is a field
 *   that quietly rots.</li>
 *   <li><strong>One "highlight hostiles" bit became three.</strong> Asil Kurt
 *   lights up everything <em>except</em> monsters plus other players; Kan Yemini
 *   lights up monsters and ordinary creatures but never players. A single bit
 *   could not say that, and the difference is the whole point: one is a predator
 *   the world flees from, the other is a hunter of monsters.</li>
 * </ul>
 *
 * <p><strong>Version 3 is not backward compatible, and that is the cheap
 * failure here.</strong> A client on an older mod reads the version byte, does
 * not recognise it, and draws nothing - it does not crash, and it does not get
 * kicked, because {@code modlar.yml}'s minimum version is deliberately not
 * raised in step (§18.3). The HUD is the one thing on this server that is
 * allowed to simply be absent until a player updates.</p>
 */
public final class HudProtocol {

    /** Plugin messaging channel. Registered outgoing only - see the class note. */
    public static final String CHANNEL = "hmmo:hud";

    public static final byte PROTOCOL_VERSION = 3;

    public static final byte TYPE_CLEAR = 0;
    public static final byte TYPE_ABILITY = 1;

    /** Set when the screen edge should breathe in and out rather than sitting still. */
    public static final int FLAG_PULSE = 1;
    /** Set when a highlighted entity should burn brighter the closer it gets. */
    public static final int FLAG_PROXIMITY_GLOW = 2;
    /** Set when hostile creatures - anything the game counts as an enemy - should glow. */
    public static final int FLAG_GLOW_MONSTERS = 4;
    /** Set when ordinary creatures - animals, villagers, anything that is not an enemy - should glow. */
    public static final int FLAG_GLOW_CREATURES = 8;
    /** Set when other players should glow. */
    public static final int FLAG_GLOW_PLAYERS = 16;

    /** Sent as {@code remainingMillis} for an ability with no expiry worth counting down. */
    public static final int NO_EXPIRY = -1;

    private HudProtocol() {
        throw new UnsupportedOperationException("HudProtocol is a static utility class");
    }

    public static byte[] clear() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(2);
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeByte(PROTOCOL_VERSION);
            out.writeByte(TYPE_CLEAR);
        } catch (IOException exception) {
            throw new IllegalStateException("HUD clear packet could not be written.", exception);
        }
        return buffer.toByteArray();
    }

    public static byte[] ability(
            String abilityId, String displayName, String iconMaterial,
            int colorRgb, int remainingMillis, int intensity, int flags,
            String comboLine, int comboTier
    ) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(192);
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeByte(PROTOCOL_VERSION);
            out.writeByte(TYPE_ABILITY);
            out.writeUTF(abilityId);
            out.writeUTF(displayName);
            out.writeUTF(iconMaterial);
            out.writeInt(colorRgb);
            out.writeInt(remainingMillis);
            out.writeByte(Math.max(0, Math.min(100, intensity)));
            out.writeByte(flags);
            out.writeUTF(comboLine == null ? "" : comboLine);
            out.writeByte(Math.max(0, Math.min(Byte.MAX_VALUE, comboTier)));
        } catch (IOException exception) {
            throw new IllegalStateException("HUD ability packet could not be written.", exception);
        }
        return buffer.toByteArray();
    }
}
