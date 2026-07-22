package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassProgress;
import com.halilibrahim.halilibrahimmmo.classes.TalentNodeDefinition;
import com.halilibrahim.halilibrahimmmo.classes.TalentSpendResult;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Class selection and talent spending, mirroring {@code /sinif sec}/{@code /sinif ac}. */
public final class ClassMenu implements GuiView {

    private static final int FIRST_CLASS_SLOT = 10;
    private static final int INFO_SLOT = 16;
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
                    Material.DIAMOND_SWORD,
                    services.messages().render(MessageKey.GUI_CLASS_ITEM, Map.of("sinif", definition.displayName())),
                    List.of(services.messages().render(selected ? MessageKey.GUI_CLASS_ITEM_LORE_SELECTED : MessageKey.GUI_CLASS_ITEM_LORE_UNSELECTED)),
                    selected
            ));
        }

        int levelsPerPoint = services.classes().settings().levelsPerTalentPoint();
        int guc = services.progression().cachedGlobalLevel(profile.playerId());
        int budget = levelsPerPoint == 0 ? 0 : profile.prestige() * 4 + guc / levelsPerPoint;
        inventory.setItem(INFO_SLOT, GuiItems.of(Material.GOLD_NUGGET,
                services.messages().render(MessageKey.GUI_CLASS_INFO_ITEM),
                List.of(services.messages().render(MessageKey.GUI_CLASS_INFO_ITEM_LORE, Map.of(
                        "harcanan", Integer.toString(progress.spentPoints()),
                        "toplam", Integer.toString(budget)
                )))
        ));

        PlayerClassDefinition selectedDefinition = services.classes().settings().definitions().get(profile.selectedClass());
        if (selectedDefinition == null) {
            return;
        }
        Map<String, Integer> ranks = progress.talentRanks().getOrDefault(selectedDefinition.id(), Map.of());
        int talentSlot = FIRST_TALENT_SLOT;
        for (TalentNodeDefinition talent : selectedDefinition.talents().values()) {
            slotToTalentId.put(talentSlot, talent.id());
            int rank = ranks.getOrDefault(talent.id(), 0);
            inventory.setItem(talentSlot++, GuiItems.of(
                    Material.BOOK,
                    services.messages().render(MessageKey.GUI_TALENT_ITEM, Map.of("yetenek", talent.displayName())),
                    List.of(services.messages().render(MessageKey.GUI_TALENT_ITEM_LORE, Map.of(
                            "rank", Integer.toString(rank),
                            "maksimum", Integer.toString(talent.maximumRank())
                    ))),
                    rank >= talent.maximumRank()
            ));
        }
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new ProgressionMenu(services));
            return;
        }
        String classId = slotToClassId.get(slot);
        if (classId != null) {
            selectClass(player, classId);
            return;
        }
        String talentId = slotToTalentId.get(slot);
        if (talentId != null) {
            spendTalent(player, talentId);
        }
    }

    private void selectClass(Player player, String classId) {
        services.classes().select(player.getUniqueId(), player.getName(), classId).thenAccept(result ->
                player.getScheduler().run(services.plugin(), task -> {
                    switch (result.status()) {
                        case SELECTED -> {
                            services.classes().applyPlayerEffects(player);
                            services.energy().reconcile(player);
                            services.abilities().clearSelection(player.getUniqueId());
                            services.sidebar().refresh(player);
                            services.feedback().celebrateClassSelected(player);
                            services.messages().send(player, MessageKey.CLASS_SELECTED, Map.of("sinif", services.classes().displayName(result.classId())));
                        }
                        case ALREADY_SELECTED -> services.messages().send(player, MessageKey.CLASS_ALREADY_SELECTED, Map.of("sinif", services.classes().displayName(result.classId())));
                        case UNKNOWN_CLASS -> services.messages().send(player, MessageKey.CLASS_UNKNOWN, Map.of("sinif", result.classId()));
                        case COOLDOWN -> services.messages().send(player, MessageKey.CLASS_COOLDOWN, Map.of("sure", Long.toString(Math.max(1, (result.remainingMillis() + 999) / 1000))));
                    }
                    GuiManager.open(player, new ClassMenu(services));
                }, null));
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
