package com.halilibrahim.halilibrahimmmo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Draws the two things a vanilla client has no way to be told about: the
 * coloured edge of the screen while an ability runs, and the badge naming the
 * ability and counting it down.
 *
 * <p>Both are drawn from the same {@link AbilityHudState}, so the HUD and the
 * inventory screen can never disagree about what is running.</p>
 */
public final class AbilityHudRenderer {

    /** How many nested rectangles the edge tint is built from. */
    private static final int VIGNETTE_BANDS = 10;
    /** How far in from the edge the tint reaches, as a share of the shorter screen dimension. */
    private static final double VIGNETTE_DEPTH = 0.14D;
    /** Alpha of the innermost band at full intensity, 0-255. */
    private static final int VIGNETTE_MAXIMUM_ALPHA = 150;

    private static final int BADGE_ICON_SIZE = 16;
    private static final int BADGE_PADDING = 4;

    /** One heartbeat, in milliseconds - what the pulsing tint breathes to. */
    private static final double PULSE_PERIOD_MILLIS = 900.0D;
    /** How far the pulse swings either side of the steady value, as a share of it. */
    private static final double PULSE_SWING = 0.35D;

    /** How far the combo line sits above the bottom of the screen. */
    private static final int COMBO_BOTTOM_MARGIN = 46;
    /** How far a shaking combo line is thrown either way, in pixels. */
    private static final int COMBO_SHAKE_PIXELS = 2;
    /** From this rung upward the line shakes. Mirrors {@code ComboLadder.SHAKING_TIER} on the server. */
    private static final int COMBO_SHAKING_TIER = 5;

    private final AbilityHudState state;

    public AbilityHudRenderer(AbilityHudState state) {
        this.state = state;
    }

    /**
     * The coloured edge. Built as a stack of nested one-band-thick rectangles
     * with falling alpha rather than as a real gradient: the result is the same
     * soft fade inwards, it needs nothing but {@code fill}, and it therefore
     * cannot break on a render-pipeline change.
     *
     * <p>Drawn only on the HUD, never over a screen: a menu is not the fight.</p>
     */
    public void renderVignette(GuiGraphicsExtractor extractor) {
        if (!state.isActive() || state.intensity() <= 0) {
            return;
        }
        // Two independent multipliers, and they answer two different questions.
        // The fade says how much of the ability is left: it falls across the
        // whole activation and is already nothing in the final second, so an
        // ability visibly runs out instead of blinking off. The pulse says the
        // ability is a frenzy: it breathes around whatever the fade currently
        // allows, so a dying Kan Yemini beats fainter rather than beating on at
        // full strength until the instant it stops.
        double strength = state.fade();
        if (state.pulses()) {
            double phase = System.currentTimeMillis() % (long) PULSE_PERIOD_MILLIS / PULSE_PERIOD_MILLIS;
            strength *= 1.0D + PULSE_SWING * Math.sin(phase * 2 * Math.PI);
        }
        if (strength <= 0.01D) {
            return;
        }
        int width = extractor.guiWidth();
        int height = extractor.guiHeight();
        // The pulse widens the band as well as brightening it, which is what
        // makes it read as the tint closing in rather than as a flicker.
        int depth = (int) (Math.min(width, height) * VIGNETTE_DEPTH * Math.min(1.35D, Math.max(0.4D, strength)));
        if (depth < VIGNETTE_BANDS) {
            return;
        }
        int band = depth / VIGNETTE_BANDS;
        int colour = state.colorRgb();
        for (int index = 0; index < VIGNETTE_BANDS; index++) {
            // The outermost band is the strongest; each step inward fades.
            double falloff = 1.0D - (double) index / VIGNETTE_BANDS;
            int alpha = (int) (VIGNETTE_MAXIMUM_ALPHA * falloff * falloff * state.intensity() / 100.0D * strength);
            if (alpha <= 0) {
                continue;
            }
            int argb = (alpha << 24) | colour;
            int inset = index * band;
            extractor.fill(0, inset, width, inset + band, argb);
            extractor.fill(0, height - inset - band, width, height - inset, argb);
            extractor.fill(inset, inset + band, inset + band, height - inset - band, argb);
            extractor.fill(width - inset - band, inset + band, width - inset, height - inset - band, argb);
        }
    }

    /**
     * The ability's own icon, name and remaining time.
     *
     * <p>Drawn wherever the caller asks, because it has two homes: the top left
     * of the HUD during play, and the column beside an open inventory where
     * vanilla lists potion effects - which is exactly where a player already
     * looks to find out what is currently affecting them.</p>
     *
     * @return the height the badge occupied, so a caller stacking things can carry on below it
     */
    public int renderBadge(GuiGraphicsExtractor extractor, int x, int y) {
        if (!state.isActive()) {
            return 0;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Component name = Component.literal(state.displayName());
        Component timer = Component.literal(state.isEndless() ? "∞" : formatRemaining(state.remainingMillis()));
        int textWidth = Math.max(minecraft.font.width(name), minecraft.font.width(timer));
        int width = BADGE_PADDING * 3 + BADGE_ICON_SIZE + textWidth;
        int height = BADGE_PADDING * 2 + BADGE_ICON_SIZE;

        extractor.fill(x, y, x + width, y + height, 0xB0100010);
        // A one-pixel edge in the ability's own colour is what ties the badge to
        // the screen tint without repainting the whole panel.
        extractor.fill(x, y, x + width, y + 1, 0xFF000000 | state.colorRgb());
        extractor.fill(x, y + height - 1, x + width, y + height, 0xFF000000 | state.colorRgb());

        ItemStack icon = state.icon();
        if (!icon.isEmpty()) {
            extractor.item(icon, x + BADGE_PADDING, y + BADGE_PADDING);
        }
        int textX = x + BADGE_PADDING * 2 + BADGE_ICON_SIZE;
        extractor.text(minecraft.font, name, textX, y + BADGE_PADDING, 0xFF000000 | state.colorRgb());
        extractor.text(minecraft.font, timer, textX, y + BADGE_PADDING + 10, 0xFFC8C8C8);
        return height;
    }

    /**
     * The frenzy line, centred low on the screen where a player's eye already
     * is during a fight.
     *
     * <p>The words and the rung both come from the server - they are Turkish
     * content and belong in {@code messages.yml}, not compiled into a mod. All
     * this decides is where to put the line and how hard to shake it: from the
     * top two rungs it jitters a couple of pixels each frame, which is the
     * difference between a label and a shout.</p>
     */
    public void renderCombo(GuiGraphicsExtractor extractor) {
        String line = state.comboLine();
        if (line.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Component text = Component.literal(line);
        int width = minecraft.font.width(text);
        int x = (extractor.guiWidth() - width) / 2;
        int y = extractor.guiHeight() - COMBO_BOTTOM_MARGIN;
        if (state.comboTier() >= COMBO_SHAKING_TIER) {
            long now = System.currentTimeMillis();
            x += (int) (now / 40 % (COMBO_SHAKE_PIXELS * 2 + 1)) - COMBO_SHAKE_PIXELS;
            y += (int) (now / 55 % (COMBO_SHAKE_PIXELS * 2 + 1)) - COMBO_SHAKE_PIXELS;
        }
        // Drawn in the ability's own colour, which is what ties it to the screen
        // edge it belongs to; the fade takes it down with everything else.
        int alpha = (int) (255 * Math.max(0.15D, state.fade()));
        extractor.text(minecraft.font, text, x, y, (alpha << 24) | state.colorRgb());
    }

    /** {@code m:ss} past a minute, plain seconds below it - the shape a countdown is read in. */
    private static String formatRemaining(long millis) {
        long totalSeconds = Math.max(0L, (millis + 999L) / 1000L);
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
