package com.halilibrahim.halilibrahimmmo.modcheck;

import java.util.Map;

/**
 * A parsed client reply: the echoed nonce plus every mod the client claims to
 * have loaded, keyed by lowercase mod id.
 *
 * <p>The map is typically far larger than the configured required list -
 * Fabric API alone contributes roughly a hundred jar-in-jar sub-modules, each
 * reported as its own mod.</p>
 */
public record ModCheckResponse(byte[] nonce, Map<String, String> mods) {

    public ModCheckResponse {
        nonce = nonce.clone();
        mods = Map.copyOf(mods);
    }

    @Override
    public byte[] nonce() {
        return nonce.clone();
    }

    public int modCount() {
        return mods.size();
    }
}
