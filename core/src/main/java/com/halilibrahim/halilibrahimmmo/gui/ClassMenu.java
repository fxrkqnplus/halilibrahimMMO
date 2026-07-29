package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.classes.ClassEffectType;
import com.halilibrahim.halilibrahimmmo.classes.ClassLevelProgress;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassProgress;
import com.halilibrahim.halilibrahimmmo.classes.TalentNodeDefinition;
import com.halilibrahim.halilibrahimmmo.classes.TalentPointCategory;
import com.halilibrahim.halilibrahimmmo.classes.TalentSpendResult;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
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

/**
 * The class hub behind {@code /sınıf}: a header for the class the player is
 * actually playing, the class icons (clicking any of them opens the full
 * picker, {@link ClassIntroMenu}), and that class's talent tree.
 *
 * <p><strong>The tree is laid out as two labelled blocks, not one run of
 * items.</strong> Talents are paid for out of two pools that cannot be spent on
 * each other, and the previous screen listed all of them in one unbroken line
 * that ran off the end of its row and over the frame - the only way to tell
 * which pool a talent belonged to was to hover it and read a lore line. Each
 * pool now owns its own labelled row block, so the split is visible before
 * anything is hovered, and its remaining-points counter sits in the same row as
 * the class icons where both are read together.</p>
 */
public final class ClassMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int FIRST_CLASS_SLOT = 10;
    private static final int LAST_CLASS_SLOT = 14;
    private static final int FEATURE_POINTS_SLOT = 15;
    private static final int ABILITY_POINTS_SLOT = 16;
    private static final int FEATURE_SECTION_SLOT = 19;
    private static final int[] FEATURE_SLOTS = {20, 21, 22, 23, 24, 25};
    private static final int ABILITY_SECTION_SLOT = 28;
    private static final int[] ABILITY_SLOTS = {29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int BACK_SLOT = 53;

    private final GuiServices services;
    private final Map<Integer, String> slotToClassId = new LinkedHashMap<>();
    private final Map<Integer, String> slotToTalentId = new LinkedHashMap<>();

    public ClassMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_CLASS_TITLE);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        services.profiles().load(player.getUniqueId(), player.getName())
                .thenCompose(profile -> services.classes().load(player.getUniqueId(), player.getName())
                        .thenApply(progress -> new Object[]{profile, progress}))
                .thenAccept(values -> player.getScheduler().run(services.plugin(), task ->
                        render(player, inventory, (PlayerProfile) values[0], (PlayerClassProgress) values[1]), null));
    }

    private void render(Player player, Inventory inventory, PlayerProfile profile, PlayerClassProgress progress) {
        PlayerClassDefinition selected = services.classes().settings().definitions().get(profile.selectedClass());
        // The talent tree is the player's own class's screen, so it wears that
        // class's colour - the same pane the abilities screen is framed in.
        GuiItems.frame(inventory,
                selected == null ? GuiItems.border() : GuiItems.pane(selected.frame()),
                GuiItems.filler());
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        inventory.setItem(HEADER_SLOT, headerItem(player, selected, progress));

        int slot = FIRST_CLASS_SLOT;
        for (PlayerClassDefinition definition : services.classes().definitions().stream()
                .sorted(Comparator.comparing(PlayerClassDefinition::displayName, String.CASE_INSENSITIVE_ORDER)).toList()) {
            if (slot > LAST_CLASS_SLOT) {
                break;
            }
            boolean isSelected = definition.id().equals(profile.selectedClass());
            slotToClassId.put(slot, definition.id());
            inventory.setItem(slot++, GuiItems.of(
                    definition.icon(),
                    services.messages().render(MessageKey.GUI_CLASS_ITEM, Map.of("sinif", definition.displayName())),
                    List.of(services.messages().render(isSelected
                            ? MessageKey.GUI_CLASS_ITEM_LORE_SELECTED : MessageKey.GUI_CLASS_ITEM_LORE_UNSELECTED)),
                    isSelected
            ));
        }

        inventory.setItem(FEATURE_POINTS_SLOT, pointsItem(
                profile, progress, TalentPointCategory.OZELLIK, Material.GOLD_NUGGET,
                MessageKey.GUI_CLASS_FEATURE_POINTS_ITEM, MessageKey.GUI_CLASS_FEATURE_POINTS_LORE
        ));
        inventory.setItem(ABILITY_POINTS_SLOT, pointsItem(
                profile, progress, TalentPointCategory.KABILIYET, Material.AMETHYST_SHARD,
                MessageKey.GUI_CLASS_ABILITY_POINTS_ITEM, MessageKey.GUI_CLASS_ABILITY_POINTS_LORE
        ));

        if (selected == null) {
            inventory.setItem(FEATURE_SECTION_SLOT, GuiItems.of(Material.BARRIER,
                    services.messages().render(MessageKey.GUI_CLASS_NO_CLASS_ITEM),
                    List.of(services.messages().render(MessageKey.GUI_CLASS_NO_CLASS_LORE))));
            return;
        }

        inventory.setItem(FEATURE_SECTION_SLOT, GuiItems.of(Material.GOLD_INGOT,
                services.messages().render(MessageKey.GUI_TALENT_CATEGORY_FEATURE),
                List.of(services.messages().render(MessageKey.GUI_TALENT_CATEGORY_FEATURE_LORE))));
        inventory.setItem(ABILITY_SECTION_SLOT, GuiItems.of(Material.AMETHYST_CLUSTER,
                services.messages().render(MessageKey.GUI_TALENT_CATEGORY_ABILITY),
                List.of(services.messages().render(MessageKey.GUI_TALENT_CATEGORY_ABILITY_LORE))));

        Map<String, Integer> ranks = progress.talentRanks().getOrDefault(selected.id(), Map.of());
        placeTalents(inventory, ranks, talentsOf(selected, TalentPointCategory.OZELLIK), FEATURE_SLOTS, Material.BOOK);
        placeTalents(inventory, ranks, talentsOf(selected, TalentPointCategory.KABILIYET), ABILITY_SLOTS, Material.ENCHANTED_BOOK);
    }

    /** Alphabetical inside a pool, so a talent stays in the same place between opens. */
    private List<TalentNodeDefinition> talentsOf(PlayerClassDefinition definition, TalentPointCategory category) {
        return definition.talents().values().stream()
                .filter(talent -> talent.effectType().category() == category)
                .sorted(Comparator.comparing(TalentNodeDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void placeTalents(
            Inventory inventory, Map<String, Integer> ranks, List<TalentNodeDefinition> talents, int[] slots, Material icon
    ) {
        for (int index = 0; index < talents.size() && index < slots.length; index++) {
            TalentNodeDefinition talent = talents.get(index);
            int rank = ranks.getOrDefault(talent.id(), 0);
            slotToTalentId.put(slots[index], talent.id());
            List<Component> lore = new ArrayList<>();
            lore.add(effectLore(talent));
            lore.add(currentEffectLore(talent, rank));
            lore.add(services.messages().render(MessageKey.GUI_TALENT_ITEM_LORE, Map.of(
                    "rank", Integer.toString(rank),
                    "maksimum", Integer.toString(talent.maximumRank())
            )));
            inventory.setItem(slots[index], GuiItems.of(
                    icon,
                    services.messages().render(MessageKey.GUI_TALENT_ITEM, Map.of("yetenek", talent.displayName())),
                    lore,
                    rank >= talent.maximumRank()
            ));
        }
    }

    /** The class actually being played, with its own level track - the one thing every talent decision is made against. */
    private ItemStack headerItem(Player player, PlayerClassDefinition selected, PlayerClassProgress progress) {
        if (selected == null) {
            return GuiItems.playerHead(player,
                    services.messages().render(MessageKey.GUI_CLASS_HEADER_NONE_ITEM),
                    List.of(services.messages().render(MessageKey.GUI_CLASS_NO_CLASS_LORE)));
        }
        ClassLevelProgress level = progress.levelProgress(selected.id());
        long required = level.level() >= services.classes().settings().classLevelCurve().maximumLevel()
                ? 0L
                : services.classes().settings().classLevelCurve().requiredForCurrentLevel(level.level());
        return GuiItems.of(selected.icon(),
                services.messages().render(MessageKey.GUI_CLASS_HEADER_ITEM, Map.of("sinif", selected.displayName())),
                List.of(services.messages().render(MessageKey.GUI_CLASS_HEADER_LORE, Map.of(
                        "seviye", Integer.toString(level.level()),
                        "xp", Long.toString(level.experience()),
                        "gereken", Long.toString(required)
                ))));
    }

    private ItemStack pointsItem(
            PlayerProfile profile, PlayerClassProgress progress, TalentPointCategory category,
            Material icon, MessageKey name, MessageKey lore
    ) {
        int total = services.classes().pointsFor(profile, category);
        int available = services.classes().availablePoints(profile, progress, category);
        return GuiItems.of(icon, services.messages().render(name),
                List.of(services.messages().render(lore, Map.of(
                        "kalan", Integer.toString(available),
                        "toplam", Integer.toString(total)
                ))));
    }

    /**
     * What one rank of this talent actually buys, branched per
     * {@link com.halilibrahim.halilibrahimmmo.classes.ClassEffectType} so the
     * player never has to guess from the name alone. Mirrors
     * {@link SkillMilestoneInfoMenu}'s per-effect-type lore, including its
     * multiplier-to-percentage conversion for the two ratio-shaped effects.
     */
    private Component effectLore(TalentNodeDefinition talent) {
        return switch (talent.effectType()) {
            // Health is stated in hearts, matching the skill milestone screen and
            // the player's own HUD - the configured value is half-heart points.
            case MAX_HEALTH -> services.messages().render(MessageKey.GUI_TALENT_LORE_HEALTH,
                    Map.of("kalp", formatAmount(talent.valuePerRank() / 2)));
            case PVE_DAMAGE_MULTIPLIER -> services.messages().render(MessageKey.GUI_TALENT_LORE_DAMAGE,
                    Map.of("yuzde", formatAmount(talent.valuePerRank() * 100)));
            case MAX_ENERGY -> services.messages().render(MessageKey.GUI_TALENT_LORE_ENERGY,
                    Map.of("deger", formatAmount(talent.valuePerRank())));
            case ABILITY_ENERGY_COST_REDUCTION -> services.messages().render(MessageKey.GUI_TALENT_LORE_ABILITY_COST,
                    Map.of("kabiliyet", abilityName(talent.abilityId()), "yuzde", formatAmount(talent.valuePerRank() * 100)));
            case ABILITY_COOLDOWN_REDUCTION -> services.messages().render(MessageKey.GUI_TALENT_LORE_ABILITY_COOLDOWN,
                    Map.of("kabiliyet", abilityName(talent.abilityId()), "yuzde", formatAmount(talent.valuePerRank() * 100)));
            case ABILITY_LIMIT_BONUS -> services.messages().render(MessageKey.GUI_TALENT_LORE_ABILITY_LIMIT,
                    Map.of("kabiliyet", abilityName(talent.abilityId()), "deger", formatAmount(talent.valuePerRank())));
            case ABILITY_DURATION_BONUS -> services.messages().render(MessageKey.GUI_TALENT_LORE_ABILITY_DURATION,
                    Map.of("kabiliyet", abilityName(talent.abilityId()), "yuzde", formatAmount(talent.valuePerRank() * 100)));
            case ABILITY_EFFECT_BONUS -> services.messages().render(MessageKey.GUI_TALENT_LORE_ABILITY_EFFECT,
                    Map.of("kabiliyet", abilityName(talent.abilityId()), "yuzde", formatAmount(talent.valuePerRank() * 100)));
            // A flat number of energy off each swept block, so unlike every
            // other reduction above it is stated as a count rather than a share.
            case ABILITY_BLOCK_ENERGY_REDUCTION -> services.messages().render(MessageKey.GUI_TALENT_LORE_ABILITY_BLOCK_ENERGY,
                    Map.of("kabiliyet", abilityName(talent.abilityId()), "deger", formatAmount(talent.valuePerRank())));
        };
    }

    /** The total already earned, so spending the next point is an informed decision rather than a blind one. */
    private Component currentEffectLore(TalentNodeDefinition talent, int rank) {
        double total = talent.valuePerRank() * rank;
        if (talent.effectType() == ClassEffectType.MAX_HEALTH) {
            return services.messages().render(MessageKey.GUI_TALENT_LORE_CURRENT_HEART, Map.of("kalp", formatAmount(total / 2)));
        }
        boolean percentage = talent.effectType() != ClassEffectType.MAX_ENERGY
                && talent.effectType() != ClassEffectType.ABILITY_LIMIT_BONUS
                && talent.effectType() != ClassEffectType.ABILITY_BLOCK_ENERGY_REDUCTION;
        return services.messages().render(percentage ? MessageKey.GUI_TALENT_LORE_CURRENT_PERCENT : MessageKey.GUI_TALENT_LORE_CURRENT,
                Map.of("deger", formatAmount(percentage ? total * 100 : total)));
    }

    private String abilityName(String abilityId) {
        return services.abilities().definitions().stream()
                .filter(definition -> definition.id().equals(abilityId))
                .map(AbilityDefinition::displayName)
                .findFirst()
                .orElse(abilityId);
    }

    /** Whole numbers read as "2", not "2.0"; fractional talent values keep one decimal. */
    private String formatAmount(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new ProgressionMenu(services));
            return;
        }
        // Selecting from here no longer changes the class on the spot: it hands
        // off to the same picker a brand-new player sees, which explains each
        // class again and routes through the confirmation screen where the
        // cooldown and the class-level reset are spelled out.
        if (slotToClassId.containsKey(slot)) {
            GuiManager.open(player, new ClassIntroMenu(services, true));
            return;
        }
        String talentId = slotToTalentId.get(slot);
        if (talentId != null) {
            spendTalent(player, talentId);
        }
    }

    private void spendTalent(Player player, String talentId) {
        services.classes().spend(player.getUniqueId(), player.getName(), talentId).thenAccept(result ->
                player.getScheduler().run(services.plugin(), task -> {
                    if (result.status() == TalentSpendResult.Status.SPENT) {
                        services.classes().applyPlayerEffects(player);
                        services.energy().reconcile(player);
                        services.feedback().celebrateTalentUnlocked(player);
                        services.messages().send(player, MessageKey.CLASS_TALENT_SPENT, Map.of(
                                "yetenek", result.talentId(), "rank", Integer.toString(result.rank()), "kalan", Integer.toString(result.availablePoints())
                        ));
                    } else {
                        MessageKey key = switch (result.status()) {
                            case NO_CLASS -> MessageKey.CLASS_NO_SELECTION;
                            case UNKNOWN_TALENT -> MessageKey.CLASS_UNKNOWN_TALENT;
                            case MAXIMUM_RANK -> MessageKey.CLASS_TALENT_MAXIMUM;
                            case NO_POINTS -> MessageKey.CLASS_NO_POINTS;
                            case SPENT -> throw new IllegalStateException();
                        };
                        services.messages().send(player, key, Map.of("yetenek", result.talentId()));
                    }
                    GuiManager.open(player, new ClassMenu(services));
                }, null));
    }
}
