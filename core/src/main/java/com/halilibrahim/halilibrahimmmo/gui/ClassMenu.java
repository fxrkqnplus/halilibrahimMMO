package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.classes.ClassEffectType;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The class hub behind {@code /sinif}: the four class icons (clicking any of
 * them opens the full picker, {@link ClassIntroMenu}) plus the selected class's
 * own talent tree, where every rankable node explains what one more rank buys.
 */
public final class ClassMenu implements GuiView {

    private static final int FIRST_CLASS_SLOT = 10;
    private static final int FEATURE_POINTS_SLOT = 15;
    private static final int ABILITY_POINTS_SLOT = 16;
    private static final int FIRST_TALENT_SLOT = 28;
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
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.profiles().load(player.getUniqueId(), player.getName())
                .thenCompose(profile -> services.classes().load(player.getUniqueId(), player.getName())
                        .thenApply(progress -> new Object[]{profile, progress}))
                .thenAccept(values -> player.getScheduler().run(services.plugin(), task ->
                        render(inventory, (PlayerProfile) values[0], (PlayerClassProgress) values[1]), null));
    }

    private void render(Inventory inventory, PlayerProfile profile, PlayerClassProgress progress) {
        List<PlayerClassDefinition> definitions = services.classes().definitions().stream()
                .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
        int slot = FIRST_CLASS_SLOT;
        for (PlayerClassDefinition definition : definitions) {
            boolean selected = definition.id().equals(profile.selectedClass());
            slotToClassId.put(slot, definition.id());
            inventory.setItem(slot++, GuiItems.of(
                    definition.icon(),
                    services.messages().render(MessageKey.GUI_CLASS_ITEM, Map.of("sinif", definition.displayName())),
                    List.of(services.messages().render(selected ? MessageKey.GUI_CLASS_ITEM_LORE_SELECTED : MessageKey.GUI_CLASS_ITEM_LORE_UNSELECTED)),
                    selected
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

        PlayerClassDefinition selectedDefinition = services.classes().settings().definitions().get(profile.selectedClass());
        if (selectedDefinition == null) {
            return;
        }
        Map<String, Integer> ranks = progress.talentRanks().getOrDefault(selectedDefinition.id(), Map.of());
        // Feature talents first, then ability talents: the tree reads as the two
        // pools it is actually paid for out of, rather than as one flat list the
        // player has to check the tooltip of to tell apart.
        List<TalentNodeDefinition> talents = selectedDefinition.talents().values().stream()
                .sorted((left, right) -> Integer.compare(
                        left.effectType().category().ordinal(), right.effectType().category().ordinal()))
                .toList();
        int talentSlot = FIRST_TALENT_SLOT;
        for (TalentNodeDefinition talent : talents) {
            boolean feature = talent.effectType().category() == TalentPointCategory.OZELLIK;
            slotToTalentId.put(talentSlot, talent.id());
            int rank = ranks.getOrDefault(talent.id(), 0);
            List<Component> lore = new ArrayList<>();
            lore.add(services.messages().render(feature
                    ? MessageKey.GUI_TALENT_CATEGORY_FEATURE : MessageKey.GUI_TALENT_CATEGORY_ABILITY));
            lore.add(effectLore(talent));
            lore.add(currentEffectLore(talent, rank));
            lore.add(services.messages().render(MessageKey.GUI_TALENT_ITEM_LORE, Map.of(
                    "rank", Integer.toString(rank),
                    "maksimum", Integer.toString(talent.maximumRank())
            )));
            inventory.setItem(talentSlot++, GuiItems.of(
                    feature ? Material.BOOK : Material.ENCHANTED_BOOK,
                    services.messages().render(MessageKey.GUI_TALENT_ITEM, Map.of("yetenek", talent.displayName())),
                    lore,
                    rank >= talent.maximumRank()
            ));
        }
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
            case MAX_HEALTH -> services.messages().render(MessageKey.GUI_TALENT_LORE_HEALTH,
                    Map.of("deger", formatAmount(talent.valuePerRank())));
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
        };
    }

    /** The total already earned, so spending the next point is an informed decision rather than a blind one. */
    private Component currentEffectLore(TalentNodeDefinition talent, int rank) {
        double total = talent.valuePerRank() * rank;
        boolean percentage = talent.effectType() != ClassEffectType.MAX_HEALTH
                && talent.effectType() != ClassEffectType.MAX_ENERGY
                && talent.effectType() != ClassEffectType.ABILITY_LIMIT_BONUS;
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
