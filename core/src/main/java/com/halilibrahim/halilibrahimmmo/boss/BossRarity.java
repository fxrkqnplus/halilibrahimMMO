package com.halilibrahim.halilibrahimmmo.boss;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Color;

import java.util.Optional;

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
 * <p>{@link #auraColor()} is the second such case, and it exists because of a
 * real crash: on Paper 26.2 {@link org.bukkit.Particle#FLASH} is a coloured
 * particle and refuses to spawn without an {@link Color} payload
 * ({@code IllegalArgumentException: missing required data class
 * org.bukkit.Color}). The spawn burst was being raised without one, which threw
 * inside the entity task <em>after</em> the creature had already been created -
 * so the admin panel's summon produced no message, no bar and no boss at all.
 * Giving each rarity its own colour fixes the crash and makes the flash itself
 * say which tier just arrived.</p>
 *
 * <p>Everything numeric about a rarity - how much health it multiplies, what it
 * drops, whether it runs - lives in {@code bosslar.yml}. This enum is only the
 * bounded set of names those settings may be keyed by.</p>
 */
public enum BossRarity {

    SIRADAN(MessageKey.BOSS_NAME_SIRADAN, MessageKey.BOSS_SPAWN_SIRADAN, MessageKey.BOSS_DEATH_SIRADAN,
            MessageKey.BOSS_HEAD_SIRADAN, MessageKey.BOSS_BAR_SIRADAN, BossBar.Color.WHITE,
            Color.fromRGB(0xDD, 0xDD, 0xDD)),
    NADIR(MessageKey.BOSS_NAME_NADIR, MessageKey.BOSS_SPAWN_NADIR, MessageKey.BOSS_DEATH_NADIR,
            MessageKey.BOSS_HEAD_NADIR, MessageKey.BOSS_BAR_NADIR, BossBar.Color.GREEN,
            Color.fromRGB(0x55, 0xFF, 0x55)),
    EPIK(MessageKey.BOSS_NAME_EPIK, MessageKey.BOSS_SPAWN_EPIK, MessageKey.BOSS_DEATH_EPIK,
            MessageKey.BOSS_HEAD_EPIK, MessageKey.BOSS_BAR_EPIK, BossBar.Color.PURPLE,
            Color.fromRGB(0xAA, 0x00, 0xFF)),
    EFSANEVI(MessageKey.BOSS_NAME_EFSANEVI, MessageKey.BOSS_SPAWN_EFSANEVI, MessageKey.BOSS_DEATH_EFSANEVI,
            MessageKey.BOSS_HEAD_EFSANEVI, MessageKey.BOSS_BAR_EFSANEVI, BossBar.Color.YELLOW,
            Color.fromRGB(0xFF, 0xD7, 0x00),
            MessageKey.BOSS_TITLE_EFSANEVI, MessageKey.BOSS_SUBTITLE_EFSANEVI, false),
    MISTIK(MessageKey.BOSS_NAME_MISTIK, MessageKey.BOSS_SPAWN_MISTIK, MessageKey.BOSS_DEATH_MISTIK,
            MessageKey.BOSS_HEAD_MISTIK, MessageKey.BOSS_BAR_MISTIK, BossBar.Color.PINK,
            Color.fromRGB(0xFF, 0x55, 0xFF),
            MessageKey.BOSS_TITLE_MISTIK, MessageKey.BOSS_SUBTITLE_MISTIK, true);

    private final MessageKey nameplateKey;
    private final MessageKey spawnBroadcastKey;
    private final MessageKey deathBroadcastKey;
    private final MessageKey headKey;
    private final MessageKey bossBarKey;
    private final BossBar.Color barColor;
    private final Color auraColor;
    private final MessageKey arrivalTitleKey;
    private final MessageKey arrivalSubtitleKey;
    private final boolean elderGuardianOmen;

    BossRarity(
            MessageKey nameplateKey, MessageKey spawnBroadcastKey, MessageKey deathBroadcastKey,
            MessageKey headKey, MessageKey bossBarKey, BossBar.Color barColor, Color auraColor
    ) {
        this(nameplateKey, spawnBroadcastKey, deathBroadcastKey, headKey, bossBarKey, barColor, auraColor,
                null, null, false);
    }

    BossRarity(
            MessageKey nameplateKey, MessageKey spawnBroadcastKey, MessageKey deathBroadcastKey,
            MessageKey headKey, MessageKey bossBarKey, BossBar.Color barColor, Color auraColor,
            MessageKey arrivalTitleKey, MessageKey arrivalSubtitleKey, boolean elderGuardianOmen
    ) {
        this.nameplateKey = nameplateKey;
        this.spawnBroadcastKey = spawnBroadcastKey;
        this.deathBroadcastKey = deathBroadcastKey;
        this.headKey = headKey;
        this.bossBarKey = bossBarKey;
        this.barColor = barColor;
        this.auraColor = auraColor;
        this.arrivalTitleKey = arrivalTitleKey;
        this.arrivalSubtitleKey = arrivalSubtitleKey;
        this.elderGuardianOmen = elderGuardianOmen;
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

    /**
     * The tint of the spawn flash and of every coloured particle this boss
     * raises. Required, not decorative: {@link org.bukkit.Particle#FLASH}
     * throws without a colour payload on this server version.
     */
    public Color auraColor() {
        return auraColor;
    }

    /**
     * The on-screen title this rarity's arrival puts in front of every player,
     * or empty when it arrives with a chat broadcast alone.
     *
     * <p>Only the top two tiers announce themselves this way, and that is the
     * whole point of doing it: a boss appears every half hour, so a title on
     * each of them would be an interruption rather than an event. An Efsanevi
     * shows up roughly one time in eleven and a Mistik one in a hundred and ten
     * - rare enough that stopping what you are doing to read it is the correct
     * reaction.</p>
     */
    public Optional<MessageKey> arrivalTitleKey() {
        return Optional.ofNullable(arrivalTitleKey);
    }

    public Optional<MessageKey> arrivalSubtitleKey() {
        return Optional.ofNullable(arrivalSubtitleKey);
    }

    /**
     * Whether this rarity's arrival is heralded by the elder guardian's own
     * appearance - the full-screen apparition and the curse that goes with it.
     *
     * <p>Reserved for Mistik alone. It is the loudest thing vanilla can put in
     * front of a player without hurting them, so it means exactly one thing on
     * this server, and a player who sees it knows what has just spawned before
     * they read a word.</p>
     */
    public boolean summonsElderGuardianOmen() {
        return elderGuardianOmen;
    }
}
