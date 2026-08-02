package com.halilibrahim.halilibrahimmmo.resourcepack;

import com.halilibrahim.halilibrahimmmo.locale.GlyphBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.function.BooleanSupplier;

/**
 * Builds the action bar's pixel bars, already wearing the resource pack's font.
 *
 * <p>Bound once from the bootstrap in the shape {@code GuiManager.bindGuard}
 * established: the services that draw an action bar - energy and abilities -
 * have no reason to know the pack exists, and threading a {@link
 * ResourcePackService} through both of them to read one boolean would be worse
 * than one bind point.</p>
 *
 * <p><strong>The font is welded to the value, not written in the template.</strong>
 * {@code messages.yml} only ever writes {@code <bar>}; the {@code hmmo:pano}
 * font travels with the component through the {@code Placeholder.component}
 * door. A string placeholder would have needed a {@code <font:...>} wrapper in
 * the YAML, and deleting that wrapper while editing a template would silently
 * turn the bar into a row of empty boxes. Nobody can break it from config.</p>
 *
 * <p>Defaults to off and returns {@link Component#empty()} until bound, so an
 * unbound or disabled server produces action bars byte-for-byte identical to
 * the ones it produced before this class existed.</p>
 */
public final class ActionBarGlyphs {

    private static final Key FONT = Key.key(ItemModels.NAMESPACE, "pano");

    private static volatile BooleanSupplier enabled = () -> false;

    private ActionBarGlyphs() {
        throw new UnsupportedOperationException("ActionBarGlyphs is a static utility class");
    }

    /** Called once during startup with a live read of {@code eylem-cubugu-barlari}. */
    public static void bind(BooleanSupplier supplier) {
        enabled = supplier == null ? () -> false : supplier;
    }

    /** Restores the default for a clean shutdown, so a reload cannot inherit a stale supplier. */
    public static void unbind() {
        enabled = () -> false;
    }

    /**
     * @return the bar, or {@link Component#empty()} when bars are switched off.
     *         Every template that uses this keeps its own numbers, so an empty
     *         component removes decoration and never information.
     */
    public static Component bar(int current, int maximum) {
        if (!enabled.getAsBoolean()) {
            return Component.empty();
        }
        return Component.text(GlyphBar.of(current, maximum)).font(FONT);
    }

    /** Same, for a countdown: the bar drains as the remaining time does. */
    public static Component remaining(long remainingMillis, long totalMillis) {
        if (totalMillis <= 0L) {
            return Component.empty();
        }
        return bar((int) Math.max(0L, remainingMillis / 100L), (int) (totalMillis / 100L));
    }
}
