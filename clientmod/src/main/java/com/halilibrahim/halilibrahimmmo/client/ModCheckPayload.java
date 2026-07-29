package com.halilibrahim.halilibrahimmmo.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The {@code hmmo:modcheck} payload, carried as an opaque byte array.
 *
 * <p>Keeping it opaque is deliberate: the actual field layout lives in
 * {@link ModCheckProtocol}, which is a byte-for-byte mirror of the server
 * plugin's class of the same name. Encoding structure here as well would create
 * a second place for the two halves to drift apart.</p>
 */
public record ModCheckPayload(byte[] data) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(ModCheckProtocol.NAMESPACE, ModCheckProtocol.PATH);

    public static final CustomPacketPayload.Type<ModCheckPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ModCheckPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBytes(payload.data()),
            buffer -> {
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                return new ModCheckPayload(data);
            }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
