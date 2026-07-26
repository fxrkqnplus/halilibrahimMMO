package com.halilibrahim.halilibrahimmmo.skills;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The bounded set of one-off gameplay actions a skill may be rewarded for,
 * beyond the passive block-break/mob-kill sources {@link SkillSource} covers.
 *
 * <p>Same "YAML names a bounded enum constant, Java owns the real behaviour"
 * convention {@code AbilityEffectType}/{@code SkillMilestoneEffectType} already
 * use: {@code skills.yml}'s {@code eylemler} section keys are the {@link #id()}
 * values below, and a dedicated listener fires exactly one of these per real
 * gameplay event. An action is claimed by at most one skill - validated when
 * {@code skills.yml} is parsed.</p>
 */
public enum SkillAction {

    /** Two animals produced a baby after being fed their breeding item. */
    HAYVAN_CIFTLESTIRME("hayvan-ciftlestirme"),
    /** A wolf, cat, parrot, horse, donkey, mule, llama or camel was tamed. */
    HAYVAN_EVCILLESTIRME("hayvan-evcillestirme"),
    /** Ticked periodically while the player is leading livestock on a lead. */
    HAYVAN_TASIMA("hayvan-tasima"),
    /** A cake was crafted. */
    PASTA_URETIMI("pasta-uretimi"),
    /** A sheep was sheared. */
    KOYUN_KIRPMA("koyun-kirpma"),
    /** A cow, mooshroom or goat was milked with a bucket. */
    SUT_SAGMA("sut-sagma"),
    /** Cooked animal meat was taken out of a furnace/smoker/campfire container. */
    ET_PISIRME("et-pisirme"),

    /** A trade with a villager or wandering trader was completed. */
    KOYLU_TICARETI("koylu-ticareti"),
    /** Bone meal was applied to a crop. */
    KEMIK_TOZU("kemik-tozu"),
    /** Bread was crafted. */
    EKMEK_URETIMI("ekmek-uretimi"),
    /** Soil was tilled into farmland with a hoe. */
    CAPA_KULLANIMI("capa-kullanimi"),
    /** A cooked crop product was taken out of a furnace/smoker container. */
    URUN_PISIRME("urun-pisirme");

    private static final Map<String, SkillAction> BY_ID = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(SkillAction::id, Function.identity()));

    private final String id;

    SkillAction(String id) {
        this.id = id;
    }

    /** The key this action is written as inside {@code skills.yml}'s {@code eylemler} section. */
    public String id() {
        return id;
    }

    public static Optional<SkillAction> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id.toLowerCase(Locale.ROOT)));
    }
}
