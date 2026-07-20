package com.halilibrahim.halilibrahimmmo.energy;

/**
 * Small mutable value object for one player's ephemeral energy balance.
 *
 * <p>Deliberately not thread-safe: correctness depends entirely on every
 * caller (ability activation, regeneration, reconciliation) running on the
 * owning player's own Paper region/entity scheduler, which serializes
 * per-entity, rather than on any lock here. Calling any mutator from a
 * thread other than that player's own scheduler would reintroduce a
 * check-then-act energy double-spend race.</p>
 */
final class EnergyState {

    private int current;

    EnergyState(int maximum) {
        this.current = maximum;
    }

    int current() {
        return current;
    }

    boolean tryConsume(int amount) {
        if (amount > current) {
            return false;
        }
        current -= amount;
        return true;
    }

    boolean regenerate(int maximum, int amount) {
        int replenished = Math.min(maximum, current + amount);
        if (replenished == current) {
            return false;
        }
        current = replenished;
        return true;
    }

    boolean clamp(int maximum) {
        if (current <= maximum) return false;
        current = maximum;
        return true;
    }
}
