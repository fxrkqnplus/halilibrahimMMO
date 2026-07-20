package com.halilibrahim.halilibrahimmmo.placeholder;

import com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Isolates every reference to PlaceholderAPI's types into this one class.
 *
 * <p>{@link com.halilibrahim.halilibrahimmmo.HalilIbrahimMMOPlugin} must never
 * declare a field/local variable typed {@link HalilIbrahimMMOExpansion} (or
 * any PlaceholderAPI type) itself - bytecode verification of the main plugin
 * class resolves every type its methods mention, branch-reachability aside,
 * so doing that would throw {@code NoClassDefFoundError} and fail the whole
 * plugin's load whenever PlaceholderAPI is absent, {@code paper-plugin.yml}'s
 * {@code required: false} notwithstanding. This class is only ever loaded
 * from inside the already-passed {@code getPlugin("PlaceholderAPI") != null}
 * guard, so it is never linked/verified when PlaceholderAPI is missing.</p>
 */
public final class PlaceholderApiHook {

    private PlaceholderApiHook() {
        throw new UnsupportedOperationException("PlaceholderApiHook is a static utility class");
    }

    public static Runnable register(JavaPlugin plugin, HalilIbrahimMMOAPI api) {
        HalilIbrahimMMOExpansion expansion = new HalilIbrahimMMOExpansion(plugin, api);
        expansion.register();
        return expansion::unregister;
    }
}
