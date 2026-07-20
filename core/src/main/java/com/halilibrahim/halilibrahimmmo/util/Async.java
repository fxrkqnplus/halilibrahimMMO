package com.halilibrahim.halilibrahimmmo.util;

import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Safely hops the result of off-thread work (database calls, HTTP calls, ...)
 * back onto a server-safe thread using Paper's {@code GlobalRegionScheduler}.
 *
 * <p>This is the only sanctioned way any future system should consume a
 * {@link CompletableFuture} produced by {@code DatabaseService} when it needs to
 * touch Bukkit API afterwards (spawning particles, sending a title, mutating
 * world state, ...) - never call {@code .join()}/{@code .get()} from the main
 * thread, and never touch Bukkit API directly from the database thread pool's
 * callback.</p>
 */
public final class Async {

    private final Plugin plugin;

    public Async(Plugin plugin) {
        this.plugin = plugin;
    }

    public <T> void onGlobalRegion(CompletableFuture<T> future, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        future.whenComplete((result, throwable) -> plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            if (throwable != null) {
                onFailure.accept(throwable);
            } else {
                onSuccess.accept(result);
            }
        }));
    }
}
