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

    private volatile String displayName = "";
    private volatile ItemStack icon = ItemStack.EMPTY;
    private volatile int colorRgb = 0xFFFFFF;
    private volatile int intensity;
    private volatile boolean highlightHostiles;
    private volatile boolean endless;
    private volatile long expiresAtMillis;
    private volatile long lastMessageMillis;
    private volatile boolean active;

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
        endless = message.remainingMillis() == HudProtocol.NO_EXPIRY;
        expiresAtMillis = endless ? Long.MAX_VALUE : System.currentTimeMillis() + message.remainingMillis();
        lastMessageMillis = System.currentTimeMillis();
        active = true;
    }

    public void clear() {
        active = false;
        highlightHostiles = false;
        icon = ItemStack.EMPTY;
        displayName = "";
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
