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
    /** Thickness of the coloured frame around the badge, in pixels. */
    private static final int BADGE_BORDER = 1;

    /** One heartbeat, in milliseconds - what the pulsing tint breathes to. */
    private static final double PULSE_PERIOD_MILLIS = 900.0D;
    /** How far the pulse swings either side of the steady value, as a share of it. */
    private static final double PULSE_SWING = 0.35D;

    /**
     * How far the combo line sits above the bottom of the screen.
     *
     * <p>Vanilla draws the action bar - which on this server is the energy
     * readout - at {@code guiHeight - 68}, above the hotbar and the experience
     * bar. Sitting the frenzy line at 46 put it straight through that. This
     * clears the action bar by a line and a half, so at the top of the ladder,
     * where the text is at its largest, the two still do not touch.</p>
     */
    private static final int COMBO_BOTTOM_MARGIN = 88;
    /** How far a shaking combo line is thrown either way, in pixels. */
    private static final int COMBO_SHAKE_PIXELS = 2;
    /** From this rung upward the line shakes. Mirrors {@code ComboLadder.SHAKING_TIER} on the server. */
    private static final int COMBO_SHAKING_TIER = 5;
    /** The top rung of the ladder. Mirrors {@code ComboLadder}'s last tier on the server. */
    private static final int COMBO_TOP_TIER = 6;
    /** How large the line is drawn on rung one, as a multiple of the normal font size. */
    private static final float COMBO_SCALE_MINIMUM = 1.0F;
    /** How large it is drawn on the top rung. */
    private static final float COMBO_SCALE_MAXIMUM = 2.0F;

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
     * <p>Drawn right-aligned to the {@code x} the caller gives, because it has
     * two homes and both of them are on the right: the top right corner of the
     * HUD during play, and beside an open inventory panel. Anchoring to the
     * right edge is what keeps it from drifting as the name and the countdown
     * change width - a badge whose left edge moves every second is a badge that
     * twitches.</p>
     *
     * @param rightX the x the badge's <em>right</em> edge sits on
     * @return the height the badge occupied, so a caller stacking things can carry on below it
     */
    public int renderBadge(GuiGraphicsExtractor extractor, int rightX, int y) {
        if (!state.isActive()) {
            return 0;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Component name = Component.literal(state.displayName());
        Component timer = Component.literal(state.isEndless() ? "∞" : formatRemaining(state.remainingMillis()));
        int textWidth = Math.max(minecraft.font.width(name), minecraft.font.width(timer));
        int width = BADGE_PADDING * 3 + BADGE_ICON_SIZE + textWidth;
        int height = BADGE_PADDING * 2 + BADGE_ICON_SIZE;
        int x = Math.max(2, rightX - width);

        extractor.fill(x, y, x + width, y + height, 0xB0100010);
        // A frame on all four sides in the ability's own colour. Faz 22 drew
        // only the top and bottom edges, which read as two loose rules rather
        // than as a plate - the badge had no left or right end and ran into
        // whatever was beside it.
        int edge = 0xFF000000 | state.colorRgb();
        extractor.fill(x, y, x + width, y + BADGE_BORDER, edge);
        extractor.fill(x, y + height - BADGE_BORDER, x + width, y + height, edge);
        extractor.fill(x, y + BADGE_BORDER, x + BADGE_BORDER, y + height - BADGE_BORDER, edge);
        extractor.fill(x + width - BADGE_BORDER, y + BADGE_BORDER, x + width, y + height - BADGE_BORDER, edge);

        ItemStack icon = state.icon();
        if (!icon.isEmpty()) {
            extractor.item(icon, x + BADGE_PADDING, y + BADGE_PADDING);
        }
        int textX = x + BADGE_PADDING * 2 + BADGE_ICON_SIZE;
        extractor.text(minecraft.font, name, textX, y + BADGE_PADDING, edge);
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
        int tier = state.comboTier();
        float scale = comboScale(tier);
        int width = minecraft.font.width(text);
        // Positioned in screen pixels and only then scaled about that point, so
        // the line stays centred and stays the same distance off the bottom as
        // it grows - scaling first would drag it toward the top left corner,
        // because a scale is about the origin and not about the text.
        float centreX = extractor.guiWidth() / 2.0F;
        float baseY = extractor.guiHeight() - COMBO_BOTTOM_MARGIN;
        float shakeX = 0.0F;
        float shakeY = 0.0F;
        if (tier >= COMBO_SHAKING_TIER) {
            long now = System.currentTimeMillis();
            shakeX = (now / 40 % (COMBO_SHAKE_PIXELS * 2 + 1)) - COMBO_SHAKE_PIXELS;
            shakeY = (now / 55 % (COMBO_SHAKE_PIXELS * 2 + 1)) - COMBO_SHAKE_PIXELS;
        }
        // Drawn in the ability's own colour, which is what ties it to the screen
        // edge it belongs to; the fade takes it down with everything else.
        int alpha = (int) (255 * Math.max(0.15D, state.fade()));
        extractor.pose().pushMatrix();
        extractor.pose().translate(centreX + shakeX, baseY + shakeY);
        extractor.pose().scale(scale);
        extractor.text(minecraft.font, text, -width / 2, -minecraft.font.lineHeight / 2,
                (alpha << 24) | state.colorRgb());
        extractor.pose().popMatrix();
    }

    /**
     * How large the frenzy line is drawn, by the rung it is on.
     *
     * <p>The ladder's sentences get more violent the further up it goes, and a
     * shout that is set in the same size as a label is not a shout. Growing the
     * text is the only part of "louder" a HUD can actually express, so it does
     * that and leaves the words alone - those are the server's, in
     * {@code messages.yml}, where an owner can rewrite them.</p>
     *
     * <p>Linear between the first and the top rung, so adding a rung on the
     * server changes the spacing rather than breaking the ends.</p>
     */
    private static float comboScale(int tier) {
        if (tier <= 1) {
            return COMBO_SCALE_MINIMUM;
        }
        float climbed = (float) (Math.min(tier, COMBO_TOP_TIER) - 1) / (COMBO_TOP_TIER - 1);
        return COMBO_SCALE_MINIMUM + climbed * (COMBO_SCALE_MAXIMUM - COMBO_SCALE_MINIMUM);
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
