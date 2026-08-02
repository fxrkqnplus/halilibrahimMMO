package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.classes.ClassLevelProgress;
import com.halilibrahim.halilibrahimmmo.classes.ClassModifiers;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.locale.DurationFormatter;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The class's four abilities, laid out as the four rungs they are.
 *
 * <p>Every class now owns exactly four abilities, unlocking at class level
 * 10/20/35/50, so this screen is built around four fixed, evenly spaced
 * pedestals rather than a running list that filled two rows and could silently
 * overflow. Each pedestal carries its rung number under it, which is what turns
 * "these are my abilities" into "this is how far along my class I am".</p>
 *
 * <p>Clicking a pedestal equips that ability; the button below the row takes
 * the current one back off. Activation itself still happens by double
 * right-clicking a matching tool in the world (see
 * {@code AbilityActivationListener}), never from here.</p>
 */
public final class AbilitiesMenu implements GuiView {

    /** Row 3's inner slots, evenly spaced so four abilities read as four rungs. */
    private static final int[] ABILITY_SLOTS = {19, 21, 23, 25};
    /** Directly below each ability, carrying that rung's number. */
    private static final int[] RUNG_SLOTS = {28, 30, 32, 34};
    private static final int HEADER_SLOT = 4;
    private static final int NO_CLASS_SLOT = 22;
    private static final int UNEQUIP_SLOT = 39;
    private static final int BACK_SLOT = 41;

    private final GuiServices services;
    private final Map<Integer, String> slotToAbilityId = new LinkedHashMap<>();
    private volatile boolean hasSelection;

    public AbilitiesMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ABILITIES_TITLE);
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        services.onboardingRewards().complete(player, OnboardingTask.KABILIYETLERI_INCELEME, MessageKey.ONBOARDING_ABILITIES_COMPLETED);
        services.profiles().load(player.getUniqueId(), player.getName())
                .thenAccept(profile -> player.getScheduler().run(services.plugin(), task ->
                        render(inventory, player, profile.selectedClass()), null));
    }

    private void render(Inventory inventory, Player player, String selectedClassId) {
        PlayerClassDefinition classDefinition = services.classes().settings().definitions().get(selectedClassId);
        // Framed in the player's own class colour: this is the one screen that
        // is entirely about their class, and it should look like it.
        GuiItems.frame(inventory,
                classDefinition == null ? GuiItems.border() : GuiItems.pane(classDefinition.frame()),
                GuiItems.filler());
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        if (selectedClassId.isEmpty() || classDefinition == null) {
            inventory.setItem(NO_CLASS_SLOT, GuiItems.of(Material.BARRIER, services.messages().render(MessageKey.GUI_ABILITIES_NO_CLASS)));
            return;
        }
        String selectedAbilityId = services.abilities().selected(player.getUniqueId()).orElse(null);
        hasSelection = selectedAbilityId != null;
        ClassLevelProgress progress = services.classes().classProgress(player.getUniqueId(), selectedClassId);
        List<AbilityDefinition> definitions = services.abilities().definitions().stream()
                .filter(definition -> definition.classId().equals(selectedClassId))
                .sorted(Comparator.comparingInt(AbilityDefinition::requiredLevel)
                        .thenComparing(AbilityDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .limit(ABILITY_SLOTS.length)
                .toList();

        ClassModifiers modifiers = services.classes().modifiers(player.getUniqueId());
        inventory.setItem(HEADER_SLOT, header(classDefinition, progress, definitions));
        for (int index = 0; index < definitions.size(); index++) {
            AbilityDefinition definition = definitions.get(index);
            inventory.setItem(RUNG_SLOTS[index], rungMarker(index, definition, progress.level()));
            inventory.setItem(ABILITY_SLOTS[index], abilityItem(definition, progress.level(), selectedAbilityId, modifiers));
            if (progress.level() >= definition.requiredLevel()) {
                slotToAbilityId.put(ABILITY_SLOTS[index], definition.id());
            }
        }
        inventory.setItem(UNEQUIP_SLOT, unequipButton(selectedAbilityId));
    }

    private ItemStack header(PlayerClassDefinition classDefinition, ClassLevelProgress progress, List<AbilityDefinition> definitions) {
        long unlocked = definitions.stream().filter(definition -> progress.level() >= definition.requiredLevel()).count();
        return GuiItems.of(classDefinition.icon(),
                services.messages().render(MessageKey.GUI_ABILITIES_HEADER_ITEM, Map.of("sinif", classDefinition.displayName())),
                List.of(services.messages().render(MessageKey.GUI_ABILITIES_HEADER_LORE, Map.of(
                        "seviye", Integer.toString(progress.level()),
                        "acik", Long.toString(unlocked),
                        "toplam", Integer.toString(definitions.size())
                ))), classDefinition.model());
    }

    /**
     * The rung number under each pedestal, green once its level is behind the
     * player and grey while it is still ahead - so the row doubles as a
     * progress track for the class itself, not just a list of four items.
     */
    private ItemStack rungMarker(int index, AbilityDefinition definition, int classLevel) {
        boolean reached = classLevel >= definition.requiredLevel();
        Material material = reached ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        return GuiItems.of(material, services.messages().render(
                reached ? MessageKey.GUI_ABILITIES_RUNG_REACHED : MessageKey.GUI_ABILITIES_RUNG_LOCKED,
                Map.of("kademe", Integer.toString(index + 1), "seviye", Integer.toString(definition.requiredLevel()))
        ));
    }

    /**
     * One ability's tooltip, showing <strong>this player's</strong> numbers
     * rather than the configured ones.
     *
     * <p>Every value here goes through the same {@link ClassModifiers} the
     * activation path itself uses, so a Kan Yemini raised from 15% to 35% by the
     * focus tree reads as 35%, and a Madenci Refleksleri talented from +3 to +5
     * Efficiency reads as +5. The screen used to show the configured defaults
     * forever, which meant the one place a player goes to look at their
     * abilities was the one place their talents were invisible - and quietly
     * told them the points they had spent had done nothing.</p>
     */
    private ItemStack abilityItem(AbilityDefinition definition, int classLevel, String selectedAbilityId, ClassModifiers modifiers) {
        if (classLevel < definition.requiredLevel()) {
            return GuiItems.of(
                    Material.BARRIER,
                    services.messages().render(MessageKey.GUI_ABILITY_LOCKED_ITEM, Map.of("kabiliyet", definition.displayName())),
                    List.of(services.messages().render(MessageKey.GUI_ABILITY_LOCKED_LORE, Map.of("seviye", Integer.toString(definition.requiredLevel()))))
            );
        }
        boolean selected = definition.id().equals(selectedAbilityId);
        int energyCost = modifiers.adjustedEnergyCost(definition.id(), definition.energyCost());
        long cooldownMillis = modifiers.adjustedCooldownMillis(definition.id(), definition.cooldownMillis());
        long activeMillis = modifiers.adjustedDurationMillis(definition.id(), definition.activeMillis());
        int limit = modifiers.adjustedLimit(definition.id(), definition.limit());
        double effect = modifiers.adjustedEffectValue(definition.id(), definition.damageMultiplier());
        int energyPerBlock = modifiers.adjustedEnergyPerBlock(definition.id(), definition.energyPerBlock());

        List<Component> lore = new ArrayList<>();
        lore.add(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_DESCRIPTION, Map.of("aciklama", definition.description())));
        lore.add(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE, Map.of(
                "seviye", Integer.toString(definition.requiredLevel()),
                "bekleme", DurationFormatter.ofMillis(services.messages(), cooldownMillis)
        )));
        lore.add(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_DURATION, Map.of(
                "sure", DurationFormatter.ofMillis(services.messages(), activeMillis)
        )));
        effectLore(definition, limit, effect).ifPresent(lore::add);
        if (energyCost > 0) {
            lore.add(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_ENERGY_LINE, Map.of("enerji", Integer.toString(energyCost))));
        }
        // Only the sweep abilities have a running per-block cost, and it is the
        // number that decides how much of a vein or a tree they actually take.
        if (energyPerBlock > 0) {
            lore.add(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_ENERGY_PER_BLOCK, Map.of(
                    "enerji", Integer.toString(energyPerBlock)
            )));
        }
        if (isUpgraded(definition, energyCost, cooldownMillis, activeMillis, limit, effect, energyPerBlock)) {
            lore.add(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_UPGRADED));
        }
        lore.add(services.messages().render(selected
                ? MessageKey.GUI_ABILITY_SELECTED_MARKER
                : MessageKey.GUI_ABILITY_EQUIP_HINT));
        return GuiItems.of(definition.icon(), abilityName(definition), lore, selected, definition.model());
    }

    /**
     * The one number that is this ability's own, in the unit it belongs to -
     * hearts of lifesteal, levels of Efficiency, blocks of radius. Chosen by
     * effect type with a fully-authored template per shape, so no Turkish unit
     * word is ever built in Java.
     */
    private Optional<Component> effectLore(AbilityDefinition definition, int limit, double effect) {
        return switch (definition.effectType()) {
            case LIFE_STEAL -> Optional.of(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_EFFECT_LIFE_STEAL, Map.of(
                    "yuzde", format(effect * 100), "seviye", Integer.toString(limit))));
            case DAMAGE_BUFF -> Optional.of(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_EFFECT_DAMAGE_BUFF,
                    Map.of("yuzde", format((effect - 1) * 100))));
            case EXTRA_DROP_CHANCE -> Optional.of(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_EFFECT_CHANCE,
                    Map.of("yuzde", format(effect * 100))));
            case KNOCKBACK_SLAM -> Optional.of(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_EFFECT_SLAM,
                    Map.of("hasar", format(effect), "yaricap", Integer.toString(limit))));
            case TOOL_EFFICIENCY -> Optional.of(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_EFFECT_EFFICIENCY,
                    Map.of("seviye", Integer.toString(limit))));
            case GUARDIAN_CALL -> Optional.of(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_EFFECT_GUARDIANS,
                    Map.of("sayi", Integer.toString(limit), "enerji", Integer.toString(definition.energyPerHit()))));
            case EXCAVATE, TREE_FELL, VEIN_MINE -> Optional.of(services.messages().render(
                    MessageKey.GUI_ABILITY_ITEM_LORE_EFFECT_BLOCKS, Map.of("sayi", Integer.toString(limit))));
            case MINE_BOMB, ORE_REVEAL, GROWTH_SURGE, LOVE_CALL, BLAST_MINE, BULLDOZER -> Optional.of(
                    services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_EFFECT_RADIUS,
                            Map.of("yaricap", Integer.toString(limit))));
            // The rest are described entirely by their duration, which already
            // has its own line.
            default -> Optional.empty();
        };
    }

    /** Whether any of this ability's numbers has been moved off its configured value by the focus tree. */
    private boolean isUpgraded(
            AbilityDefinition definition, int energyCost, long cooldownMillis,
            long activeMillis, int limit, double effect, int energyPerBlock
    ) {
        return energyCost != definition.energyCost() || cooldownMillis != definition.cooldownMillis()
                || activeMillis != definition.activeMillis() || limit != definition.limit()
                || Math.abs(effect - definition.damageMultiplier()) > 1.0E-6D
                || energyPerBlock != definition.energyPerBlock();
    }

    private String format(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }

    private ItemStack unequipButton(String selectedAbilityId) {
        if (selectedAbilityId == null) {
            return GuiItems.of(Material.GRAY_DYE,
                    services.messages().render(MessageKey.GUI_ABILITY_UNEQUIP_EMPTY_ITEM),
                    List.of(services.messages().render(MessageKey.GUI_ABILITY_UNEQUIP_EMPTY_LORE)));
        }
        return GuiItems.of(Material.RED_DYE,
                services.messages().render(MessageKey.GUI_ABILITY_UNEQUIP_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ABILITY_UNEQUIP_LORE)));
    }

    /**
     * Built directly as a coloured {@link Component} rather than through a
     * {@code MessageKey} template: an ability's colour is per-ability
     * configured data ({@code gorsel.renk}), and {@code MessageService} only
     * substitutes unparsed string placeholders, which cannot vary a
     * placeholder's own colour per invocation. This is the same documented
     * exception §10.6 established for per-title chat tags and §12.12 reused
     * for the sidebar name line.
     */
    private Component abilityName(AbilityDefinition definition) {
        return Component.text(definition.displayName(), definition.visual().color().textColor());
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AnaMenu(services));
            return;
        }
        if (slot == UNEQUIP_SLOT) {
            if (!hasSelection) {
                return;
            }
            if (services.abilities().unequip(player)) {
                services.messages().send(player, MessageKey.ABILITY_UNEQUIPPED);
                services.feedback().celebrateAbilityUnequipped(player);
                services.sidebar().refresh(player);
                GuiManager.open(player, new AbilitiesMenu(services));
            }
            return;
        }
        String abilityId = slotToAbilityId.get(slot);
        if (abilityId != null) {
            services.abilities().select(player, abilityId);
            services.sidebar().refresh(player);
            GuiManager.open(player, new AbilitiesMenu(services));
        }
    }
}
