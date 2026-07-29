package com.halilibrahim.halilibrahimmmo.modcheck;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One connected player's handshake state.
 *
 * <p>Mutated from three different threads - the join listener, the player's
 * region scheduler (brand check and timeout), and Paper's plugin-message
 * dispatch - so every field is either final or volatile, and the
 * "who gets to decide this player's fate" race is settled once by
 * {@link #resolve()}.</p>
 */
final class ModVerificationState {

    private final byte[] nonce;
    private final AtomicBoolean resolved = new AtomicBoolean();

    private volatile ModVerificationStatus status;
    private volatile String clientBrand = "";
    private volatile int reportedModCount;

    private ModVerificationState(byte[] nonce, ModVerificationStatus status) {
        this.nonce = nonce;
        this.status = status;
    }

    static ModVerificationState pending(byte[] nonce) {
        return new ModVerificationState(nonce.clone(), ModVerificationStatus.PENDING);
    }

    /** An exempt player is created already resolved, so nothing can later kick them. */
    static ModVerificationState exempt() {
        ModVerificationState state = new ModVerificationState(new byte[0], ModVerificationStatus.EXEMPT);
        state.resolved.set(true);
        return state;
    }

    /**
     * @return {@code true} for the single caller that wins the right to decide
     *         this handshake. Every later caller (a late reply arriving after
     *         the timeout fired, a second reply from a chatty client) gets
     *         {@code false} and must do nothing.
     */
    boolean resolve() {
        return resolved.compareAndSet(false, true);
    }

    boolean isResolved() {
        return resolved.get();
    }

    byte[] nonce() {
        return nonce;
    }

    ModVerificationStatus status() {
        return status;
    }

    void markVerified() {
        this.status = ModVerificationStatus.VERIFIED;
    }

    String clientBrand() {
        return clientBrand;
    }

    void clientBrand(String brand) {
        this.clientBrand = brand == null ? "" : brand;
    }

    int reportedModCount() {
        return reportedModCount;
    }

    void reportedModCount(int count) {
        this.reportedModCount = count;
    }

    ModVerificationSnapshot toSnapshot() {
        return new ModVerificationSnapshot(status, clientBrand, reportedModCount);
    }
}
