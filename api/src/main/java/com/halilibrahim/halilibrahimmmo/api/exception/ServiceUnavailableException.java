package com.halilibrahim.halilibrahimmmo.api.exception;

/**
 * Thrown when a consumer plugin requests the {@link com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI}
 * before HalilIbrahimMMO has finished registering it with the Bukkit {@code ServicesManager},
 * or after it has been unregistered during shutdown.
 *
 * <p>Consumers should either depend on HalilIbrahimMMO in {@code paper-plugin.yml} (guaranteeing
 * load order) or call {@link com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOProvider#isAvailable()}
 * before {@link com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOProvider#get()}.</p>
 */
public final class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
