package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.classes.TalentNodeDefinition;
import com.halilibrahim.halilibrahimmmo.locale.DurationFormatter;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import net.kyori.adventure.text.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Says what one rank of a talent actually buys, in the ability's own units.
 *
 * <p><strong>Every ability talent used to be described as a percentage,
 * because a percentage is what {@code classes.yml} stores.</strong> "Bekleme
 * süresini %50 azaltır" is true and useless: a player cannot act on it without
 * first knowing the base cooldown, which is nowhere on the screen, and a
 * percentage of a small number ("kabiliyet etkisini %100 arttır" on a
 * three-level enchantment) reads as far larger than it is. The ratio stays the
 * mechanism - it is what lets one talent scale with an ability whose base value
 * is later retuned - and this class turns it back into the number the player is
 * looking at: −15 saniye, +1 Verimlilik, +%5 can.</p>
 *
 * <p>Each line also carries the ability's <em>configured</em> value and what it
 * currently is at the ranks already spent, so the talent is read against
 * something. One line per axis rather than three separate lines: a tooltip is
 * short, and the unit only has to be written once if base, step and total sit
 * together.</p>
 */
final class TalentEffectLore {

    private final MessageService messages;
    private final AbilityService abilities;

    TalentEffectLore(MessageService messages, AbilityService abilities) {
        this.messages = messages;
        this.abilities = abilities;
    }

    /**
     * The "what does one rank give me" line, absolute wherever an absolute
     * answer exists.
     *
     * <p>A talent whose ability has been removed from configuration falls back
     * to describing itself as a plain ratio - the rank is still refunded by the
     * spend rules, and there is nothing to state it against.</p>
     */
    Component perRank(TalentNodeDefinition talent, int rank) {
        return switch (talent.effectType()) {
            // Health is stated in hearts, matching the skill milestone screen
            // and the player's own HUD - the configured value is half-hearts.
            case MAX_HEALTH -> messages.render(MessageKey.GUI_TALENT_LORE_HEALTH,
                    Map.of("kalp", format(talent.valuePerRank() / 2)));
            case PVE_DAMAGE_MULTIPLIER -> messages.render(MessageKey.GUI_TALENT_LORE_DAMAGE,
                    Map.of("yuzde", format(talent.valuePerRank() * 100)));
            case MAX_ENERGY -> messages.render(MessageKey.GUI_TALENT_LORE_ENERGY,
                    Map.of("deger", format(talent.valuePerRank())));
            case ABILITY_ENERGY_COST_REDUCTION -> ability(talent)
                    .map(definition -> messages.render(MessageKey.GUI_TALENT_LORE_ABILITY_COST, scale(
                            talent, definition.energyCost() * talent.valuePerRank(),
                            definition.energyCost(), definition.energyCost() * (1 - talent.valuePerRank() * rank))))
                    .orElseGet(() -> unknownAbility(talent));
            case ABILITY_COOLDOWN_REDUCTION -> ability(talent)
                    .map(definition -> messages.render(MessageKey.GUI_TALENT_LORE_ABILITY_COOLDOWN, Map.of(
                            "kabiliyet", definition.displayName(),
                            "rank", duration(definition.cooldownMillis() * talent.valuePerRank()),
                            "temel", duration(definition.cooldownMillis()),
                            "mevcut", duration(definition.cooldownMillis() * (1 - talent.valuePerRank() * rank))
                    )))
                    .orElseGet(() -> unknownAbility(talent));
            case ABILITY_DURATION_BONUS -> ability(talent)
                    .map(definition -> messages.render(MessageKey.GUI_TALENT_LORE_ABILITY_DURATION, Map.of(
                            "kabiliyet", definition.displayName(),
                            "rank", duration(definition.activeMillis() * talent.valuePerRank()),
                            "temel", duration(definition.activeMillis()),
                            "mevcut", duration(definition.activeMillis() * (1 + talent.valuePerRank() * rank))
                    )))
                    .orElseGet(() -> unknownAbility(talent));
            case ABILITY_BLOCK_ENERGY_REDUCTION -> ability(talent)
                    .map(definition -> messages.render(MessageKey.GUI_TALENT_LORE_ABILITY_BLOCK_ENERGY, scale(
                            talent, talent.valuePerRank(), definition.energyPerBlock(),
                            Math.max(1, definition.energyPerBlock() - talent.valuePerRank() * rank))))
                    .orElseGet(() -> unknownAbility(talent));
            case ABILITY_LIMIT_BONUS -> ability(talent)
                    .map(definition -> messages.render(limitKey(definition), scale(
                            talent, talent.valuePerRank(), definition.limit(),
                            definition.limit() + talent.valuePerRank() * rank)))
                    .orElseGet(() -> unknownAbility(talent));
            case ABILITY_EFFECT_BONUS -> ability(talent)
                    .map(definition -> effectLine(talent, definition, rank))
                    .orElseGet(() -> unknownAbility(talent));
        };
    }

    /**
     * The effect scalar means something different per ability, so its line is
     * chosen by the ability's own effect type: a ratio for the three that carry
     * one, raw damage for the two that do not.
     */
    private Component effectLine(TalentNodeDefinition talent, AbilityDefinition definition, int rank) {
        double base = definition.damageMultiplier();
        double current = base * (1 + talent.valuePerRank() * rank);
        return switch (definition.effectType()) {
            case LIFE_STEAL, EXTRA_DROP_CHANCE, DAMAGE_BUFF -> messages.render(
                    MessageKey.GUI_TALENT_LORE_ABILITY_EFFECT_PERCENT, Map.of(
                            "kabiliyet", definition.displayName(),
                            "rank", format(base * talent.valuePerRank() * 100),
                            "temel", format(base * 100),
                            "mevcut", format(current * 100)
                    ));
            default -> messages.render(MessageKey.GUI_TALENT_LORE_ABILITY_EFFECT_DAMAGE, Map.of(
                    "kabiliyet", definition.displayName(),
                    "rank", format(base * talent.valuePerRank()),
                    "temel", format(base),
                    "mevcut", format(current)
            ));
        };
    }

    /**
     * What {@code limit} counts for this ability - a block ceiling, a radius, an
     * enchantment level, a potion level or a head count. One fully-authored
     * template per shape rather than one template fed a Turkish unit word from
     * Java, the same discipline every other per-shape label in this codebase
     * follows.
     */
    private MessageKey limitKey(AbilityDefinition definition) {
        return switch (definition.effectType()) {
            case TOOL_EFFICIENCY -> MessageKey.GUI_TALENT_LORE_ABILITY_LIMIT_EFFICIENCY;
            case LIFE_STEAL -> MessageKey.GUI_TALENT_LORE_ABILITY_LIMIT_SPEED;
            case GUARDIAN_CALL -> MessageKey.GUI_TALENT_LORE_ABILITY_LIMIT_ANIMALS;
            case EXCAVATE, TREE_FELL, VEIN_MINE -> MessageKey.GUI_TALENT_LORE_ABILITY_LIMIT_BLOCKS;
            default -> MessageKey.GUI_TALENT_LORE_ABILITY_LIMIT_RADIUS;
        };
    }

    /** The total already accumulated, for the three talents that add to the player rather than to an ability. */
    Optional<Component> characterTotal(TalentNodeDefinition talent, int rank) {
        double total = talent.valuePerRank() * rank;
        return switch (talent.effectType()) {
            case MAX_HEALTH -> Optional.of(messages.render(MessageKey.GUI_TALENT_LORE_CURRENT_HEART,
                    Map.of("kalp", format(total / 2))));
            case PVE_DAMAGE_MULTIPLIER -> Optional.of(messages.render(MessageKey.GUI_TALENT_LORE_CURRENT_PERCENT,
                    Map.of("deger", format(total * 100))));
            case MAX_ENERGY -> Optional.of(messages.render(MessageKey.GUI_TALENT_LORE_CURRENT,
                    Map.of("deger", format(total))));
            // Every ability talent already states its own base and current
            // value on its single line; a second total would repeat it.
            default -> Optional.empty();
        };
    }

    private Map<String, String> scale(TalentNodeDefinition talent, double step, double base, double current) {
        return Map.of(
                "kabiliyet", ability(talent).map(AbilityDefinition::displayName).orElse(talent.abilityId()),
                "rank", format(step),
                "temel", format(base),
                "mevcut", format(current)
        );
    }

    private Component unknownAbility(TalentNodeDefinition talent) {
        return messages.render(MessageKey.GUI_TALENT_LORE_ABILITY_MISSING, Map.of("kabiliyet", talent.abilityId()));
    }

    private Optional<AbilityDefinition> ability(TalentNodeDefinition talent) {
        return abilities.definitions().stream()
                .filter(definition -> definition.id().equals(talent.abilityId()))
                .findFirst();
    }

    private String duration(double millis) {
        return DurationFormatter.ofMillis(messages, Math.max(0L, Math.round(millis)));
    }

    /** Whole numbers read as "2", not "2.0"; fractional values keep one decimal. */
    private String format(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }
}
