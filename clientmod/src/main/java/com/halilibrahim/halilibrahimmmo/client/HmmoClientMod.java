package com.halilibrahim.halilibrahimmmo.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The whole of {@code hmmo-client}: answer the HalilIbrahimMMO server's mod
 * list challenge, and do nothing else.
 *
 * <p>No screens, no commands, no key bindings, no config file. The mod is
 * mandatory on the server, so every line of behaviour it adds is behaviour
 * every player is forced to accept - which is why it adds none.</p>
 *
 * <p>On a single-player world, or on any server that never sends a challenge,
 * this class registers a receiver and is then never invoked again.</p>
 */
public final class HmmoClientMod implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("hmmo-client");

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.clientboundPlay().register(ModCheckPayload.TYPE, ModCheckPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ModCheckPayload.TYPE, ModCheckPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(ModCheckPayload.TYPE, (payload, context) -> {
            byte[] nonce = ModCheckProtocol.readRequestNonce(payload.data());
            if (nonce == null) {
                LOGGER.warn("Sunucudan taninmayan bir mod kontrol istegi geldi, yok sayildi.");
                return;
            }
            byte[] response = ModCheckProtocol.response(nonce, collectMods());
            if (response == null) {
                LOGGER.error("Mod listesi tek pakete sigmayacak kadar buyuk, yanit gonderilmedi.");
                return;
            }
            // The receiver already runs on the client thread, but sending is
            // scheduled explicitly so this never depends on that staying true.
            context.client().execute(() -> ClientPlayNetworking.send(new ModCheckPayload(response)));
        });
    }

    /**
     * Every loaded mod, id to version. Includes Fabric API's jar-in-jar
     * sub-modules, which is why a normal client reports far more entries than
     * the number of jars in its mods folder.
     */
    private static Map<String, String> collectMods() {
        Map<String, String> mods = new LinkedHashMap<>();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            mods.put(
                    container.getMetadata().getId().toLowerCase(Locale.ROOT),
                    container.getMetadata().getVersion().getFriendlyString()
            );
        }
        return mods;
    }
}
