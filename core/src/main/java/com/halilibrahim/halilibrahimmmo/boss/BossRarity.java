package com.halilibrahim.halilibrahimmmo.boss;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;

/**
 * How dangerous - and how valuable - one world boss is.
 *
 * <p>Every rarity owns four fully-authored {@link MessageKey}s of its own
 * rather than sharing one template fed a rarity label. That is the same
 * discipline the per-cause XP bossbars and the per-board leaderboard labels
 * already follow, and here it buys something concrete: a rarity's colour lives
 * in {@code messages.yml} where a server owner can change it, instead of being
 * a colour constant compiled into Java that no {@code MessageKey} placeholder
 * could carry anyway.</p>
 *
 * <p>Everything numeric about a rarity - how much health it multiplies, what it
 * drops, whether it runs - lives in {@code bosslar.yml}. This enum is only the
 * bounded set of names those settings may be keyed by.</p>
 */
public enum BossRarity {

    SIRADAN(MessageKey.BOSS_NAME_SIRADAN, MessageKey.BOSS_SPAWN_SIRADAN, MessageKey.BOSS_DEATH_SIRADAN, MessageKey.BOSS_HEAD_SIRADAN),
    NADIR(MessageKey.BOSS_NAME_NADIR, MessageKey.BOSS_SPAWN_NADIR, MessageKey.BOSS_DEATH_NADIR, MessageKey.BOSS_HEAD_NADIR),
    EPIK(MessageKey.BOSS_NAME_EPIK, MessageKey.BOSS_SPAWN_EPIK, MessageKey.BOSS_DEATH_EPIK, MessageKey.BOSS_HEAD_EPIK),
    EFSANEVI(MessageKey.BOSS_NAME_EFSANEVI, MessageKey.BOSS_SPAWN_EFSANEVI, MessageKey.BOSS_DEATH_EFSANEVI, MessageKey.BOSS_HEAD_EFSANEVI),
    MISTIK(MessageKey.BOSS_NAME_MISTIK, MessageKey.BOSS_SPAWN_MISTIK, MessageKey.BOSS_DEATH_MISTIK, MessageKey.BOSS_HEAD_MISTIK);

    private final MessageKey nameplateKey;
    private final MessageKey spawnBroadcastKey;
    private final MessageKey deathBroadcastKey;
    private final MessageKey headKey;

    BossRarity(MessageKey nameplateKey, MessageKey spawnBroadcastKey, MessageKey deathBroadcastKey, MessageKey headKey) {
        this.nameplateKey = nameplateKey;
        this.spawnBroadcastKey = spawnBroadcastKey;
        this.deathBroadcastKey = deathBroadcastKey;
        this.headKey = headKey;
    }

    /** The floating name above the boss's head. */
    public MessageKey nameplateKey() {
        return nameplateKey;
    }

    public MessageKey spawnBroadcastKey() {
        return spawnBroadcastKey;
    }

    public MessageKey deathBroadcastKey() {
        return deathBroadcastKey;
    }

    /** The name on the head this boss leaves behind. */
    public MessageKey headKey() {
        return headKey;
    }
}
