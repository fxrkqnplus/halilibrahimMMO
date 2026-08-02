package com.halilibrahim.halilibrahimmmo.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * What the client currently believes is running, and when it stops believing it.
 *
 * <p>Deliberately a single mutable holder rather than anything cleverer: the
 * server can only ever have one ability equipped per player, every message
 * carries the whole picture, and the newest message always wins. There is
 * nothing here to merge and nothing to keep in step.</p>
 *
 * <p><strong>The expiry is computed from the local clock, not counted down in
 * ticks.</strong> The server re-sends once a second while an ability runs, so a
 * dropped packet or a paused client corrects itself on the next message instead
 * of drifting; and if the server stops talking altogether - a kick, a crash,
 * a reload - the state expires on its own rather than sticking on screen
 * forever.</p>
 */
public final class AbilityHudState {

    /**
     * How long after the last message the HUD gives up on its own.
     *
     * <p>The server refreshes every second, so anything past a few of those
     * means it has stopped talking. Without this a disconnect mid-ability would
     * leave a red screen edge on the title menu.</p>
     */
    private static final long STALE_AFTER_MILLIS = 5_000L;

    /**
     * How long before the end the tint has finished fading out.
     *
     * <p>The fade runs across the whole duration and reaches nothing exactly
     * here, so the last second of an ability is always already clear - which is
     * what makes "it is about to end" readable without a number.</p>
     */
    private static final long FADE_FLOOR_MILLIS = 1_000L;

    private volatile String displayName = "";
    private volatile ItemStack icon = ItemStack.EMPTY;
    private volatile int colorRgb = 0xFFFFFF;
    private volatile int intensity;
    private volatile boolean highlightHostiles;
    private volatile boolean pulsing;
    private volatile boolean proximityGlow;
    private volatile boolean endless;
    private volatile long expiresAtMillis;
    private volatile long totalMillis;
    private volatile long lastMessageMillis;
    private volatile boolean active;
    private volatile String comboLine = "";
    private volatile int comboTier;

    public void accept(HudProtocol.Message message) {
        if (message.type() == HudProtocol.TYPE_CLEAR) {
            clear();
            return;
        }
        displayName = message.displayName();
        icon = resolveIcon(message.iconItem());
        colorRgb = message.colorRgb() & 0xFFFFFF;
        intensity = message.intensity();
        highlightHostiles = message.highlightsHostiles();
        pulsing = message.pulses();
        proximityGlow = message.glowScalesWithDistance();
        endless = message.remainingMillis() == HudProtocol.NO_EXPIRY;
        expiresAtMillis = endless ? Long.MAX_VALUE : System.currentTimeMillis() + message.remainingMillis();
        totalMillis = Math.max(1L, message.totalMillis());
        comboLine = message.comboLine();
        comboTier = message.comboTier();
        lastMessageMillis = System.currentTimeMillis();
        active = true;
    }

    public void clear() {
        active = false;
        highlightHostiles = false;
        pulsing = false;
        proximityGlow = false;
        icon = ItemStack.EMPTY;
        displayName = "";
        comboLine = "";
        comboTier = 0;
    }

    /** {@code true} while there is something to draw - and no longer, whatever the server does next. */
    public boolean isActive() {
        long now = System.currentTimeMillis();
        if (!active || now - lastMessageMillis > STALE_AFTER_MILLIS || now >= expiresAtMillis) {
            active = false;
            return false;
        }
        return true;
    }

    public boolean highlightsHostiles() {
        return isActive() && highlightHostiles;
    }

    public String displayName() {
        return displayName;
    }

    public ItemStack icon() {
        return icon;
    }

    public int colorRgb() {
        return colorRgb;
    }

    /** 0-100, from the rung the ability sits on: how strongly the screen edge is tinted. */
    public int intensity() {
        return intensity;
    }

    public boolean isEndless() {
        return endless;
    }

    public long remainingMillis() {
        return endless ? 0L : Math.max(0L, expiresAtMillis - System.currentTimeMillis());
    }

    public boolean pulses() {
        return pulsing;
    }

    public boolean glowScalesWithDistance() {
        return isActive() && proximityGlow;
    }

    /** The frenzy line the server wrote, or empty when this ability has none. */
    public String comboLine() {
        return isActive() ? comboLine : "";
    }

    public int comboTier() {
        return isActive() ? comboTier : 0;
    }

    /**
     * How much of the effect is still on screen, {@code 1.0} down to {@code 0.0}.
     *
     * <p>Falls linearly across the whole activation and hits zero
     * {@link #FADE_FLOOR_MILLIS} before the end, so an ability visibly dims as
     * it runs out instead of vanishing between two frames. An ability with no
     * expiry (Creative) never fades - there is nothing to count down to.</p>
     */
    public double fade() {
        if (endless) {
            return 1.0D;
        }
        long span = Math.max(1L, totalMillis - FADE_FLOOR_MILLIS);
        long left = remainingMillis() - FADE_FLOOR_MILLIS;
        return Math.max(0.0D, Math.min(1.0D, (double) left / span));
    }

    /**
     * Maps the Bukkit material name the server sent onto a real item.
     *
     * <p>Bukkit's material names are the vanilla registry ids in upper case, so
     * lower-casing is the whole translation. An unknown name falls back to a
     * blaze powder rather than failing: the badge is worth drawing even when the
     * server names an item this client's version does not have.</p>
     */
    private static ItemStack resolveIcon(String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return new ItemStack(Items.BLAZE_POWDER);
        }
        Item item = BuiltInRegistries.ITEM.getValue(
                Identifier.fromNamespaceAndPath("minecraft", materialName.toLowerCase(Locale.ROOT)));
        return item == null ? new ItemStack(Items.BLAZE_POWDER) : new ItemStack(item);
    }
}
