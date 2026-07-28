package com.halilibrahim.halilibrahimmmo.quests;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A bounded set of specific deeds a quest can ask for, when the deed is neither
 * "break this" nor "kill that".
 *
 * <p>These exist because some of the best objectives in Minecraft are shaped
 * like a story rather than a counter: killing a ghast with its own fireball is
 * a thing you learn to do, not a thing you grind. Each constant is backed by a
 * real event handler in {@code QuestDeedListener}, which is exactly why this is
 * a closed enum and not a free-text key - a deed nobody listens for would be a
 * quest that can never be finished.</p>
 */
public enum QuestDeed {

    /** A ghast killed by a fireball it launched itself, reflected back at it. */
    GHAST_ALEV_TOPU("ghast-alev-topu"),
    /** Bartering with a piglin by giving it gold. */
    PIGLIN_TAKASI("piglin-takasi"),
    /** Completing a trade with a villager or wandering trader. */
    KOYLU_TAKASI("koylu-takasi"),
    /** Enchanting an item at an enchanting table. */
    BUYULEME("buyuleme"),
    /** Reeling in a fish (treasure and junk count too - it is the catch that matters). */
    BALIK_TUTMA("balik-tutma"),
    /** Taming any tameable creature. */
    EVCILLESTIRME("evcillestirme");

    private static final Map<String, QuestDeed> BY_ID = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(QuestDeed::id, Function.identity()));

    private final String id;

    QuestDeed(String id) {
        this.id = id;
    }

    /** The name this deed is written as in {@code quests.yml}. */
    public String id() {
        return id;
    }

    public static Optional<QuestDeed> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id.toLowerCase(Locale.ROOT)));
    }
}
