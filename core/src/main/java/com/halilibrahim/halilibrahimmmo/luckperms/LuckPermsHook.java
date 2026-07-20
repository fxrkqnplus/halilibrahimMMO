package com.halilibrahim.halilibrahimmmo.luckperms;

import com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI;
import net.luckperms.api.LuckPermsProvider;

/**
 * Isolates every reference to LuckPerms' types into this one class.
 *
 * <p>{@link com.halilibrahim.halilibrahimmmo.HalilIbrahimMMOPlugin} must never
 * declare a field/local variable typed {@link HalilIbrahimMMOContextCalculator}
 * (or any LuckPerms type) itself - bytecode verification of the main plugin
 * class resolves every type its methods mention, branch-reachability aside,
 * so doing that would throw {@code NoClassDefFoundError} and fail the whole
 * plugin's load whenever LuckPerms is absent, {@code paper-plugin.yml}'s
 * {@code required: false} notwithstanding. This class is only ever loaded
 * from inside the already-passed {@code getPlugin("LuckPerms") != null}
 * guard, so it is never linked/verified when LuckPerms is missing.</p>
 */
public final class LuckPermsHook {

    private LuckPermsHook() {
        throw new UnsupportedOperationException("LuckPermsHook is a static utility class");
    }

    public static Runnable register(HalilIbrahimMMOAPI api) {
        HalilIbrahimMMOContextCalculator calculator = new HalilIbrahimMMOContextCalculator(api);
        LuckPermsProvider.get().getContextManager().registerCalculator(calculator);
        return () -> LuckPermsProvider.get().getContextManager().unregisterCalculator(calculator);
    }
}
