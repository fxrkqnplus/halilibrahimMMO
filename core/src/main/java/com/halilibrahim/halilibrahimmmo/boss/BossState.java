package com.halilibrahim.halilibrahimmmo.boss;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Everything about a living world boss that only exists while the server is up:
 * its health bar and viewers, which phase it is in, and the clocks its brain
 * reads.
 *
 * <p><strong>Deliberately not persisted, and this is a known, accepted
 * limitation.</strong> A boss's identity - its rarity, name and rewards - rides
 * along in the entity's own {@code PersistentDataContainer} and survives a
 * restart; its behaviour does not. A boss that outlived a restart keeps every
 * stat and every reward and simply fights like a very strong ordinary mob.
 * Writing a table for it would mean reconciling rows against entities that may
 * have been killed, despawned or unloaded while the server was down, in
 * exchange for making one already-announced boss slightly cleverer.</p>
 *
 * <p>Every field is {@code volatile} because the bar is repainted from the
 * boss's own entity scheduler while {@code markEngaged} arrives on whichever
 * region the attacker was standing in.</p>
 */
final class BossState {

    private final BossRarity rarity;
    private final BossBar bossBar;
    private final String name;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private volatile BossPhase phase = BossPhase.ROAMING;
    private volatile long lastTeleportMillis;
    private volatile long lastAttackMillis;
    private volatile long lastShotMillis;
    private volatile long lastRoamMillis;
    private volatile long lastVandalismMillis;
    private volatile int escapesUsed;
    private volatile boolean engaged;
    private volatile Location roamTarget;

    BossState(BossRarity rarity, String name, BossBar bossBar) {
        this.rarity = rarity;
        this.name = name;
        this.bossBar = bossBar;
    }

    BossRarity rarity() {
        return rarity;
    }

    BossBar bossBar() {
        return bossBar;
    }

    String name() {
        return name;
    }

    Set<UUID> viewers() {
        return viewers;
    }

    BossPhase phase() {
        return phase;
    }

    void enterPhase(BossPhase next) {
        phase = next;
    }

    long lastTeleportMillis() {
        return lastTeleportMillis;
    }

    void markTeleported(long now) {
        lastTeleportMillis = now;
    }

    long lastAttackMillis() {
        return lastAttackMillis;
    }

    void markAttacked(long now) {
        lastAttackMillis = now;
    }

    long lastShotMillis() {
        return lastShotMillis;
    }

    void markShot(long now) {
        lastShotMillis = now;
    }

    long lastRoamMillis() {
        return lastRoamMillis;
    }

    void markRoamed(long now) {
        lastRoamMillis = now;
    }

    long lastVandalismMillis() {
        return lastVandalismMillis;
    }

    void markVandalised(long now) {
        lastVandalismMillis = now;
    }

    int escapesUsed() {
        return escapesUsed;
    }

    void markEscaped() {
        escapesUsed++;
    }

    Location roamTarget() {
        return roamTarget;
    }

    void setRoamTarget(Location target) {
        roamTarget = target;
    }

    boolean isEngaged() {
        return engaged;
    }

    void markEngaged() {
        engaged = true;
    }
}
