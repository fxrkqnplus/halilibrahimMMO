package com.halilibrahim.halilibrahimmmo.boss;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.bossbar.BossBar;

/**
 * How dangerous - and how valuable - one world boss is.
 *
 * <p>Every rarity owns five fully-authored {@link MessageKey}s of its own
 * rather than sharing one template fed a rarity label. That is the same
 * discipline the per-cause XP bossbars and the per-board leaderboard labels
 * already follow, and here it buys something concrete: a rarity's colour lives
 * in {@code messages.yml} where a server owner can change it, instead of being
 * a colour constant compiled into Java that no {@code MessageKey} placeholder
 * could carry anyway.</p>
 *
 * <p>{@link #barColor()} is the one colour that <em>has</em> to be a Java
 * constant, because Adventure's boss bar takes its colour as an enum rather
 * than as part of its text - the same bounded-enum shape {@code ClassBarColor}
 * already uses for class XP bars.</p>
 *
 * <p>Everything numeric about a rarity - how much health it multiplies, what it
 * drops, whether it runs - lives in {@code bosslar.yml}. This enum is only the
 * bounded set of names those settings may be keyed by.</p>
 */
public enum BossRarity {

    SIRADAN(MessageKey.BOSS_NAME_SIRADAN, MessageKey.BOSS_SPAWN_SIRADAN, MessageKey.BOSS_DEATH_SIRADAN,
            MessageKey.BOSS_HEAD_SIRADAN, MessageKey.BOSS_BAR_SIRADAN, BossBar.Color.WHITE),
    NADIR(MessageKey.BOSS_NAME_NADIR, MessageKey.BOSS_SPAWN_NADIR, MessageKey.BOSS_DEATH_NADIR,
            MessageKey.BOSS_HEAD_NADIR, MessageKey.BOSS_BAR_NADIR, BossBar.Color.GREEN),
    EPIK(MessageKey.BOSS_NAME_EPIK, MessageKey.BOSS_SPAWN_EPIK, MessageKey.BOSS_DEATH_EPIK,
            MessageKey.BOSS_HEAD_EPIK, MessageKey.BOSS_BAR_EPIK, BossBar.Color.PURPLE),
    EFSANEVI(MessageKey.BOSS_NAME_EFSANEVI, MessageKey.BOSS_SPAWN_EFSANEVI, MessageKey.BOSS_DEATH_EFSANEVI,
            MessageKey.BOSS_HEAD_EFSANEVI, MessageKey.BOSS_BAR_EFSANEVI, BossBar.Color.YELLOW),
    MISTIK(MessageKey.BOSS_NAME_MISTIK, MessageKey.BOSS_SPAWN_MISTIK, MessageKey.BOSS_DEATH_MISTIK,
            MessageKey.BOSS_HEAD_MISTIK, MessageKey.BOSS_BAR_MISTIK, BossBar.Color.PINK);

    private final MessageKey nameplateKey;
    private final MessageKey spawnBroadcastKey;
    private final MessageKey deathBroadcastKey;
    private final MessageKey headKey;
    private final MessageKey bossBarKey;
    private final BossBar.Color barColor;

    BossRarity(
            MessageKey nameplateKey, MessageKey spawnBroadcastKey, MessageKey deathBroadcastKey,
            MessageKey headKey, MessageKey bossBarKey, BossBar.Color barColor
    ) {
        this.nameplateKey = nameplateKey;
        this.spawnBroadcastKey = spawnBroadcastKey;
        this.deathBroadcastKey = deathBroadcastKey;
        this.headKey = headKey;
        this.bossBarKey = bossBarKey;
        this.barColor = barColor;
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

    /** The health bar at the top of the screen: the boss's name, its health and its rarity. */
    public MessageKey bossBarKey() {
        return bossBarKey;
    }

    public BossBar.Color barColor() {
        return barColor;
    }
}
