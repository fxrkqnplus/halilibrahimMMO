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
     * How long before the end the tint starts giving way.
     *
     * <p>Faz 22 faded linearly across the <em>whole</em> activation, and on a
     * forty-five second Kan Yemini that meant the border was already half gone
     * twenty seconds in - the effect looked like it was failing rather than
     * running. It now sits at full strength for as long as the ability is
     * genuinely under way and only lets go over the last stretch, which is the
     * part a player actually needs warning about.</p>
     */
    private static final long FADE_WINDOW_MILLIS = 5_000L;

    /**
     * How long one rung of the fade lasts.
     *
     * <p>The fade steps rather than slides, and that is the point: a smooth
     * ramp over five seconds is indistinguishable from a border that is simply
     * dim, whereas five visible drops one second apart read as a countdown
     * without printing a number. {@link #FADE_WINDOW_MILLIS} divided by this is
     * the number of rungs, so changing either keeps the arithmetic honest.</p>
     */
    private static final long FADE_STEP_MILLIS = 1_000L;

    private volatile String displayName = "";
    private volatile ItemStack icon = ItemStack.EMPTY;
    private volatile int colorRgb = 0xFFFFFF;
    private volatile int intensity;
    private volatile boolean glowMonsters;
    private volatile boolean glowCreatures;
    private volatile boolean glowPlayers;
    private volatile boolean pulsing;
    private volatile boolean proximityGlow;
    private volatile boolean endless;
    private volatile long expiresAtMillis;
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
        glowMonsters = message.glowsMonsters();
        glowCreatures = message.glowsCreatures();
        glowPlayers = message.glowsPlayers();
        pulsing = message.pulses();
        proximityGlow = message.glowScalesWithDistance();
        endless = message.remainingMillis() == HudProtocol.NO_EXPIRY;
        expiresAtMillis = endless ? Long.MAX_VALUE : System.currentTimeMillis() + message.remainingMillis();
        comboLine = message.comboLine();
        comboTier = message.comboTier();
        lastMessageMillis = System.currentTimeMillis();
        active = true;
    }

    public void clear() {
        active = false;
        glowMonsters = false;
        glowCreatures = false;
        glowPlayers = false;
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

    /** {@code true} while anything at all should be lit up, so the ticker can bail in one check. */
    public boolean glowsAnything() {
        return isActive() && (glowMonsters || glowCreatures || glowPlayers);
    }

    public boolean glowsMonsters() {
        return glowMonsters;
    }

    public boolean glowsCreatures() {
        return glowCreatures;
    }

    public boolean glowsPlayers() {
        return glowPlayers;
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
     * <p>Full strength until {@link #FADE_WINDOW_MILLIS} are left, then one
     * visible step down per {@link #FADE_STEP_MILLIS} until it is gone - so the
     * border holds while the ability is doing its work and then plainly counts
     * itself out.</p>
     *
     * <p>An ability shorter than the window fades for the whole of its life,
     * which is the honest reading: a two-second call <em>is</em> all ending. An
     * ability with no expiry (Creative) never fades - there is nothing to count
     * down to.</p>
     */
    public double fade() {
        if (endless) {
            return 1.0D;
        }
        long remaining = remainingMillis();
        if (remaining >= FADE_WINDOW_MILLIS) {
            return 1.0D;
        }
        long rungs = FADE_WINDOW_MILLIS / FADE_STEP_MILLIS;
        // Floored, not rounded: the first drop has to land the moment the final
        // window opens, otherwise the top rung is held for a whole extra second
        // and the step nobody sees is the one that mattered.
        return (double) (remaining / FADE_STEP_MILLIS) / rungs;
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
