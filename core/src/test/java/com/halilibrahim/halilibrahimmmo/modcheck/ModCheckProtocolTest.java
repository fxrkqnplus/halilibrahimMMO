package com.halilibrahim.halilibrahimmmo.modcheck;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ModCheckProtocolTest {

    private static byte[] nonce() {
        byte[] nonce = new byte[ModCheckProtocol.NONCE_LENGTH];
        for (int index = 0; index < nonce.length; index++) {
            nonce[index] = (byte) index;
        }
        return nonce;
    }

    /** Mirrors what the {@code hmmo-client} mod writes, so the two halves are tested against each other. */
    private static byte[] clientResponse(byte version, byte[] nonce, int declaredCount, Map<String, String> mods) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeByte(version);
            out.write(nonce);
            out.writeInt(declaredCount);
            for (Map.Entry<String, String> mod : mods.entrySet()) {
                out.writeUTF(mod.getKey());
                out.writeUTF(mod.getValue());
            }
        }
        return buffer.toByteArray();
    }

    @Test
    void requestCarriesVersionAndNonce() {
        byte[] request = ModCheckProtocol.request(nonce());
        assertEquals(1 + ModCheckProtocol.NONCE_LENGTH, request.length);
        assertEquals(ModCheckProtocol.PROTOCOL_VERSION, request[0]);
        assertArrayEquals(nonce(), Arrays.copyOfRange(request, 1, request.length));
    }

    @Test
    void requestRejectsWrongSizedNonce() {
        assertThrows(IllegalArgumentException.class, () -> ModCheckProtocol.request(new byte[4]));
        assertThrows(IllegalArgumentException.class, () -> ModCheckProtocol.request(null));
    }

    @Test
    void roundTripsAModList() throws IOException {
        Map<String, String> mods = new LinkedHashMap<>();
        mods.put("fabric-api", "0.154.2+26.2");
        mods.put("sodium", "0.9.1+mc26.2");
        mods.put("hmmo-client", "1.0.0");

        ModCheckResponse response = ModCheckProtocol.readResponse(
                clientResponse(ModCheckProtocol.PROTOCOL_VERSION, nonce(), mods.size(), mods), 512);

        assertArrayEquals(nonce(), response.nonce());
        assertEquals(3, response.modCount());
        assertEquals("0.9.1+mc26.2", response.mods().get("sodium"));
    }

    @Test
    void modIdsAreNormalisedToLowercase() throws IOException {
        Map<String, String> mods = Map.of("Sodium", "0.9.1+mc26.2");
        ModCheckResponse response = ModCheckProtocol.readResponse(
                clientResponse(ModCheckProtocol.PROTOCOL_VERSION, nonce(), 1, mods), 512);
        assertEquals("0.9.1+mc26.2", response.mods().get("sodium"));
    }

    @Test
    void emptyPayloadIsRejected() {
        assertThrows(ModCheckProtocolException.class, () -> ModCheckProtocol.readResponse(new byte[0], 512));
        assertThrows(ModCheckProtocolException.class, () -> ModCheckProtocol.readResponse(null, 512));
    }

    @Test
    void wrongProtocolVersionIsRejected() throws IOException {
        byte[] payload = clientResponse((byte) 99, nonce(), 0, Map.of());
        assertThrows(ModCheckProtocolException.class, () -> ModCheckProtocol.readResponse(payload, 512));
    }

    @Test
    void truncatedPayloadIsRejected() throws IOException {
        Map<String, String> mods = Map.of("sodium", "0.9.1+mc26.2");
        byte[] full = clientResponse(ModCheckProtocol.PROTOCOL_VERSION, nonce(), 1, mods);
        byte[] cut = Arrays.copyOf(full, full.length - 3);
        assertThrows(ModCheckProtocolException.class, () -> ModCheckProtocol.readResponse(cut, 512));
    }

    /** A count larger than configured must be rejected before anything is allocated for it. */
    @Test
    void implausibleModCountIsRejected() throws IOException {
        byte[] payload = clientResponse(ModCheckProtocol.PROTOCOL_VERSION, nonce(), Integer.MAX_VALUE, Map.of());
        assertThrows(ModCheckProtocolException.class, () -> ModCheckProtocol.readResponse(payload, 512));
    }

    @Test
    void negativeModCountIsRejected() throws IOException {
        byte[] payload = clientResponse(ModCheckProtocol.PROTOCOL_VERSION, nonce(), -1, Map.of());
        assertThrows(ModCheckProtocolException.class, () -> ModCheckProtocol.readResponse(payload, 512));
    }

    /** A client under-declaring its count would otherwise leave unread bytes behind. */
    @Test
    void trailingBytesAreRejected() throws IOException {
        Map<String, String> mods = new LinkedHashMap<>();
        mods.put("sodium", "0.9.1+mc26.2");
        mods.put("lithium", "0.25.2+mc26.2");
        byte[] payload = clientResponse(ModCheckProtocol.PROTOCOL_VERSION, nonce(), 1, mods);
        assertThrows(ModCheckProtocolException.class, () -> ModCheckProtocol.readResponse(payload, 512));
    }

    @Test
    void blankModEntryIsRejected() throws IOException {
        byte[] payload = clientResponse(ModCheckProtocol.PROTOCOL_VERSION, nonce(), 1, Map.of("", "1.0.0"));
        assertThrows(ModCheckProtocolException.class, () -> ModCheckProtocol.readResponse(payload, 512));
    }
}
