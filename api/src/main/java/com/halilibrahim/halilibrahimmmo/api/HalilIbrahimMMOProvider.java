package com.halilibrahim.halilibrahimmmo.api;

import com.halilibrahim.halilibrahimmmo.api.exception.ServiceUnavailableException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Static entry point used by third-party plugins to obtain the {@link HalilIbrahimMMOAPI}.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * if (HalilIbrahimMMOProvider.isAvailable()) {
 *     HalilIbrahimMMOAPI api = HalilIbrahimMMOProvider.get();
 *     boolean debug = api.isDebugModeEnabled();
 * }
 * }</pre>
 */
public final class HalilIbrahimMMOProvider {

    private HalilIbrahimMMOProvider() {
        throw new UnsupportedOperationException("HalilIbrahimMMOProvider is a static utility class");
    }

    /**
     * @return the active {@link HalilIbrahimMMOAPI} instance.
     * @throws ServiceUnavailableException if HalilIbrahimMMO has not finished enabling yet,
     *                                     or is currently disabled/reloading.
     */
    public static HalilIbrahimMMOAPI get() {
        RegisteredServiceProvider<HalilIbrahimMMOAPI> registration =
                Bukkit.getServicesManager().getRegistration(HalilIbrahimMMOAPI.class);

        if (registration == null) {
            throw new ServiceUnavailableException(
                    "HalilIbrahimMMOAPI is not registered yet. Make sure HalilIbrahimMMO is installed, "
                            + "enabled, and declared as a dependency of your plugin in paper-plugin.yml."
            );
        }

        return registration.getProvider();
    }

    /**
     * @return {@code true} if the API is currently registered and safe to fetch via {@link #get()}.
     */
    public static boolean isAvailable() {
        return Bukkit.getServicesManager().getRegistration(HalilIbrahimMMOAPI.class) != null;
    }
}
