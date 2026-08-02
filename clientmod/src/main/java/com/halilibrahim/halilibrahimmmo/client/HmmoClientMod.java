package com.halilibrahim.halilibrahimmmo.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@code hmmo-client}: answer the HalilIbrahimMMO server's mod list challenge,
 * and draw the two things the server has no way of drawing itself.
 *
 * <p>Phase 18 shipped this mod deliberately doing nothing but the handshake, on
 * the grounds that it is mandatory and every behaviour it adds is behaviour
 * every player is forced to accept. Faz 22 adds exactly two, both of which are
 * <em>server-driven presentation</em> rather than behaviour: a coloured edge to
 * the screen while an ability runs, and that ability listed where the game
 * already lists what is affecting you. Neither can be done from the server -
 * there is no packet for either - and neither changes a single thing about what
 * an ability does. A client that ignores every one of these messages plays the
 * same game with a plainer screen.</p>
 *
 * <p>Still no screens of its own, no commands, no key bindings, no config file.
 * On a single-player world, or on any server that never sends anything, this
 * class registers its receivers and is never invoked again.</p>
 */
public final class HmmoClientMod implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("hmmo-client");

    /** Where the badge sits on the ordinary HUD. */
    private static final int HUD_BADGE_X = 4;
    private static final int HUD_BADGE_Y = 4;
    /**
     * Where the badge sits beside an open container: in the column to the left
     * of the panel, which is where vanilla draws the potion-effect list and
     * therefore where a player already looks to see what is affecting them.
     */
    private static final int SCREEN_BADGE_GAP = 6;
    private static final int SCREEN_BADGE_WIDTH_GUESS = 110;
    /** The size of a vanilla container panel, which every one of them is centred at. */
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;

    private final AbilityHudState hudState = new AbilityHudState();
    private final AbilityHudRenderer hudRenderer = new AbilityHudRenderer(hudState);
    private final HostileGlowTicker glowTicker = new HostileGlowTicker(hudState);

    @Override
    public void onInitializeClient() {
        registerModCheck();
        registerHud();
    }

    private void registerModCheck() {
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
     * The HUD half: one clientbound channel, one HUD element, one screen hook
     * and one tick hook.
     *
     * <p>Registered unconditionally. Everything downstream reads
     * {@link AbilityHudState}, which is empty until a server says otherwise and
     * expires on its own if one stops talking - so on a single-player world all
     * of this costs an {@code isActive()} check per frame.</p>
     */
    private void registerHud() {
        PayloadTypeRegistry.clientboundPlay().register(HudPayload.TYPE, HudPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(HudPayload.TYPE, (payload, context) -> {
            HudProtocol.Message message = HudProtocol.read(payload.data());
            if (message == null) {
                return;
            }
            context.client().execute(() -> hudState.accept(message));
        });

        // Added last so the tint sits over the rest of the HUD rather than
        // under the hotbar; the badge goes in the same pass because both are
        // read off the same state and neither is worth its own element.
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(HudProtocol.NAMESPACE, "ability_overlay"),
                (extractor, deltaTracker) -> {
                    hudRenderer.renderVignette(extractor);
                    hudRenderer.renderBadge(extractor, HUD_BADGE_X, HUD_BADGE_Y);
                    // Low and centred, where a player's eye already is mid-fight -
                    // and only on the HUD, never over a screen: the frenzy is
                    // something you are in, not something you read in a menu.
                    hudRenderer.renderCombo(extractor);
                });

        // The HUD is not drawn while a screen is open, so the badge is drawn
        // again over any container screen - which is precisely where the
        // request wanted it.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) {
                return;
            }
            ScreenEvents.afterExtract(screen).register((ignoredScreen, extractor, mouseX, mouseY, tickDelta) ->
                    hudRenderer.renderBadge(extractor, badgeX(extractor.guiWidth()), badgeY(extractor.guiHeight())));
        });

        ClientTickEvents.END_CLIENT_TICK.register(glowTicker::tick);
        // Leaving a server must not leave a red screen edge on the menu, nor a
        // creature glowing in a world we are no longer in.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            hudState.clear();
            glowTicker.tick(client);
        });
    }

    /**
     * Left of the container panel, where vanilla lists potion effects.
     *
     * <p><strong>Derived from the screen size rather than read off the
     * screen.</strong> {@code AbstractContainerScreen}'s {@code leftPos} and
     * {@code topPos} are protected, and reaching them would mean either a mixin
     * or an access widener - a lot of build machinery, and a lot of things that
     * can break on a game update, to learn a number that is simply "the panel is
     * centred". A container panel is {@value #PANEL_WIDTH} wide in almost every
     * case, so the centred arithmetic lands on the same pixel; a wider one only
     * moves the badge a little further from its edge.</p>
     *
     * <p>Clamped so a narrow window gets an overlapping badge rather than one
     * drawn off the side of the screen.</p>
     */
    private static int badgeX(int guiWidth) {
        int panelLeft = (guiWidth - PANEL_WIDTH) / 2;
        return Math.max(2, panelLeft - SCREEN_BADGE_GAP - SCREEN_BADGE_WIDTH_GUESS);
    }

    private static int badgeY(int guiHeight) {
        return Math.max(2, (guiHeight - PANEL_HEIGHT) / 2);
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
