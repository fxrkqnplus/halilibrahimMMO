package com.halilibrahim.halilibrahimmmo.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A minimal, reflection-free service locator used as HalilIbrahimMMO's dependency
 * injection mechanism.
 *
 * <p>Every internal system is registered exactly once, explicitly, during plugin
 * bootstrap ({@code HalilIbrahimMMOPlugin#onEnable()}) and resolved by interface
 * type wherever it is needed. This avoids the classpath weight, reflective
 * scanning, and startup cost of a full DI framework (Guice, Spring, ...) while
 * still keeping every system decoupled from concrete implementations - exactly
 * what a plugin expected to support 500+ concurrent players needs.</p>
 *
 * <p>Not thread-safe by design: all registrations happen sequentially on the main
 * thread during {@code onEnable()}, before any other thread can observe this
 * container. Reads after startup are effectively immutable.</p>
 */
public final class ServiceContainer {

    private final Map<Class<?>, Object> services = new LinkedHashMap<>();

    public <T> void register(Class<T> type, T instance) {
        if (instance == null) {
            throw new LifecycleException("Cannot register a null instance for service: " + type.getName());
        }
        if (services.containsKey(type)) {
            throw new LifecycleException("Service already registered: " + type.getName());
        }
        services.put(type, instance);
    }

    public <T> T resolve(Class<T> type) {
        Object instance = services.get(type);
        if (instance == null) {
            throw new LifecycleException("No service registered for type: " + type.getName());
        }
        return type.cast(instance);
    }

    public boolean isRegistered(Class<?> type) {
        return services.containsKey(type);
    }

    public Collection<Object> allServices() {
        return Collections.unmodifiableCollection(services.values());
    }

    public void clear() {
        services.clear();
    }
}
