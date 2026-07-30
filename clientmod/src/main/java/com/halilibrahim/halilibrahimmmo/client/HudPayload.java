package com.halilibrahim.halilibrahimmmo.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The {@code hmmo:hud} payload, carried as an opaque byte array for exactly the
 * reason {@link ModCheckPayload} is: the field layout lives in
 * {@link HudProtocol}, which mirrors the server's class of the same name, and
 * encoding it here as well would create a second place for the two halves to
 * drift apart.
 *
 * <p>Registered clientbound only - the server talks, the client listens. There
 * is nothing the HUD could tell the server that it does not already know.</p>
 */
public record HudPayload(byte[] data) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(HudProtocol.NAMESPACE, HudProtocol.PATH);

    public static final CustomPacketPayload.Type<HudPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, HudPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBytes(payload.data()),
            buffer -> {
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                return new HudPayload(data);
            }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
