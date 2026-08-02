package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.classes.ClassLevelProgress;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassProgress;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.progression.FeatureUnlockRules;
import com.halilibrahim.halilibrahimmmo.locale.NumberFormatter;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import com.halilibrahim.halilibrahimmmo.skills.SkillProgress;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import com.halilibrahim.halilibrahimmmo.trade.TradeRequestResult;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Another player's whole record on one screen, opened by {@code /profil <isim>}.
 *
 * <p><strong>Every number here is paired with a standing.</strong> "Güç 312" on
 * its own says nothing to somebody who has never seen anyone else's; "Güç 312 -
 * 4th" is the thing worth looking somebody up for. That is why this screen
 * exists separately from {@code ProfileMenu}, which is a player's own summary
 * and needs no comparison.</p>
 *
 * <p>The trade button is the one interactive part, and it is deliberately
 * strict: it only appears when both sides have actually unlocked trading and
 * the target is online, because a request that cannot be answered is worse than
 * no button at all.</p>
 */
public final class PlayerProfileMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int POWER_SLOT = 10;
    private static final int REPUTATION_SLOT = 11;
    private static final int CLASS_SLOT = 12;
    private static final int TITLE_SLOT = 14;
    private static final int COSMETIC_SLOT = 15;
    private static final int TALENT_SLOT = 16;
    private static final int FIRST_SKILL_SLOT = 19;
    private static final int ABILITY_SLOT = 31;
    private static final int TRADE_SLOT = 40;
    private static final int BACK_SLOT = 49;

    private final GuiServices services;
    private final UUID targetId;
    private final String targetName;
    private volatile boolean tradeOffered;

    public PlayerProfileMenu(GuiServices services, UUID targetId, String targetName) {
        this.services = services;
        this.targetId = targetId;
        this.targetName = targetName;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_PLAYER_PROFILE_TITLE, Map.of("oyuncu", targetName));
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player viewer, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.border(), GuiItems.filler());
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        inventory.setItem(HEADER_SLOT, GuiItems.of(Material.PLAYER_HEAD,
                services.messages().render(MessageKey.GUI_PLAYER_PROFILE_LOADING)));

        // One composed future rather than eight independent ones: the screen is
        // only worth painting once every piece has landed, and painting it in
        // fragments would let a slow rank query redraw over a finished row.
        CompletableFuture<PlayerProfile> profileFuture = services.profiles().load(targetId, targetName);
        CompletableFuture<PlayerClassProgress> classFuture = services.classes().load(targetId, targetName);
        CompletableFuture<Long> reputationFuture = services.reputation().load(targetId);
        CompletableFuture<Set<String>> titlesFuture = services.titles().unlockedIds(targetId);
        CompletableFuture<Set<String>> cosmeticsFuture = services.cosmetics().unlockedIds(targetId);
        CompletableFuture<Void> skillsLoaded = CompletableFuture.allOf(services.skills().definitions().stream()
                .map(definition -> services.skills().load(targetId, definition.id()))
                .toArray(CompletableFuture[]::new));

        CompletableFuture.allOf(profileFuture, classFuture, reputationFuture, titlesFuture, cosmeticsFuture, skillsLoaded)
                .thenCompose(ignored -> {
                    PlayerProfile profile = profileFuture.join();
                    long reputation = reputationFuture.join();
                    int power = services.progression().cachedGlobalLevel(targetId);
                    return services.leaderboards().powerRank(profile.prestige(), power)
                            .thenCompose(powerRank -> services.leaderboards().reputationRank(reputation)
                                    .thenCompose(reputationRank -> skillRanks()
                                            .thenApply(ranks -> new Standings(power, powerRank, reputation, reputationRank, ranks))));
                })
                .thenAccept(standings -> viewer.getScheduler().run(services.plugin(), task -> render(
                        inventory, viewer, profileFuture.join(), classFuture.join(),
                        titlesFuture.join(), cosmeticsFuture.join(), standings
                ), null));
    }

    private CompletableFuture<Map<String, Integer>> skillRanks() {
        Map<String, Integer> ranks = new LinkedHashMap<>();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (SkillDefinition definition : services.skills().definitions()) {
            SkillProgress progress = services.skills().cached(targetId, definition.id())
                    .orElse(new SkillProgress(targetId, definition.id(), 1, 0L));
            chain = chain.thenCompose(ignored -> services.leaderboards()
                    .skillRank(definition.id(), progress.level(), progress.experience())
                    .thenAccept(rank -> ranks.put(definition.id(), rank)));
        }
        return chain.thenApply(ignored -> ranks);
    }

    private void render(
            Inventory inventory, Player viewer, PlayerProfile profile, PlayerClassProgress classProgress,
            Set<String> unlockedTitles, Set<String> unlockedCosmetics, Standings standings
    ) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        String classId = profile.selectedClass();
        PlayerClassDefinition classDefinition = services.classes().settings().definitions().get(classId);

        inventory.setItem(HEADER_SLOT, GuiItems.playerHead(target,
                services.messages().render(MessageKey.GUI_PLAYER_PROFILE_HEADER_ITEM, Map.of("oyuncu", targetName)),
                List.of(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_HEADER_LORE, Map.of(
                        "prestij", Integer.toString(profile.prestige()),
                        "sinif", classDefinition == null
                                ? services.messages().plain(MessageKey.GUI_PLAYER_PROFILE_NO_CLASS, Map.of())
                                : classDefinition.displayName()
                )))));

        inventory.setItem(POWER_SLOT, GuiItems.of(Material.NETHERITE_INGOT,
                services.messages().render(MessageKey.GUI_PLAYER_PROFILE_POWER_ITEM, Map.of("guc", Integer.toString(standings.power()))),
                List.of(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_RANK_LORE, Map.of("sira", Integer.toString(standings.powerRank()))))));

        inventory.setItem(REPUTATION_SLOT, GuiItems.of(Material.DIAMOND,
                services.messages().render(MessageKey.GUI_PLAYER_PROFILE_REPUTATION_ITEM, Map.of("sayginlik", NumberFormatter.compact(standings.reputation()))),
                List.of(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_RANK_LORE, Map.of("sira", Integer.toString(standings.reputationRank()))))));

        inventory.setItem(CLASS_SLOT, classItem(classDefinition, classProgress, classId));
        inventory.setItem(TITLE_SLOT, unlockedItem(Material.NAME_TAG, MessageKey.GUI_PLAYER_PROFILE_TITLES_ITEM,
                unlockedTitles.size(), services.titles().definitions().size(),
                services.titles().definitions().stream()
                        .filter(definition -> unlockedTitles.contains(definition.id()))
                        .map(TitleDefinition::displayName).toList()));
        inventory.setItem(COSMETIC_SLOT, unlockedItem(Material.FIREWORK_STAR, MessageKey.GUI_PLAYER_PROFILE_COSMETICS_ITEM,
                unlockedCosmetics.size(), services.cosmetics().definitions().size(),
                services.cosmetics().definitions().stream()
                        .filter(definition -> unlockedCosmetics.contains(definition.id()))
                        .map(CosmeticDefinition::displayName).toList()));
        inventory.setItem(TALENT_SLOT, talentItem(classDefinition, classProgress, classId));
        inventory.setItem(ABILITY_SLOT, abilityItem(classDefinition, classProgress, classId));

        int slot = FIRST_SKILL_SLOT;
        for (SkillDefinition definition : services.skills().definitions()) {
            SkillProgress progress = services.skills().cached(targetId, definition.id())
                    .orElse(new SkillProgress(targetId, definition.id(), 1, 0L));
            inventory.setItem(slot++, GuiItems.of(definition.icon(),
                    services.messages().render(MessageKey.GUI_PLAYER_PROFILE_SKILL_ITEM, Map.of(
                            "beceri", definition.displayName(), "seviye", Integer.toString(progress.level())
                    )),
                    List.of(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_RANK_LORE, Map.of(
                            "sira", Integer.toString(standings.skillRanks().getOrDefault(definition.id(), 0))
                    ))),
                    definition.model()));
        }

        inventory.setItem(TRADE_SLOT, tradeButton(viewer));
    }

    private ItemStack classItem(PlayerClassDefinition classDefinition, PlayerClassProgress classProgress, String classId) {
        if (classDefinition == null) {
            return GuiItems.of(Material.BARRIER, services.messages().render(MessageKey.GUI_PLAYER_PROFILE_NO_CLASS));
        }
        ClassLevelProgress levelProgress = classProgress.levelProgress(classId);
        return GuiItems.of(classDefinition.icon(),
                services.messages().render(MessageKey.GUI_PLAYER_PROFILE_CLASS_ITEM, Map.of("sinif", classDefinition.displayName())),
                List.of(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_CLASS_LORE, Map.of(
                        "seviye", Integer.toString(levelProgress.level()),
                        "xp", Long.toString(levelProgress.experience())
                ))), classDefinition.model());
    }

    /**
     * The target's focus tree as a list of what they actually put points into,
     * not an empty grid of what they could have. A tree with nothing spent says
     * so in one line instead of showing every talent at rank 0.
     */
    private ItemStack talentItem(PlayerClassDefinition classDefinition, PlayerClassProgress classProgress, String classId) {
        List<Component> lore = new ArrayList<>();
        if (classDefinition == null) {
            lore.add(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_LIST_EMPTY));
            return GuiItems.of(Material.BOOK, services.messages().render(MessageKey.GUI_PLAYER_PROFILE_TALENTS_ITEM), lore);
        }
        Map<String, Integer> ranks = classProgress.talentRanks().getOrDefault(classId, Map.of());
        classDefinition.talents().values().stream()
                .filter(talent -> ranks.getOrDefault(talent.id(), 0) > 0)
                .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName()))
                .forEach(talent -> lore.add(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_TALENT_LINE, Map.of(
                        "yetenek", talent.displayName(),
                        "rank", Integer.toString(ranks.get(talent.id())),
                        "maksimum", Integer.toString(talent.maximumRank())
                ))));
        if (lore.isEmpty()) {
            lore.add(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_LIST_EMPTY));
        }
        return GuiItems.of(Material.BOOK, services.messages().render(MessageKey.GUI_PLAYER_PROFILE_TALENTS_ITEM), lore);
    }

    private ItemStack abilityItem(PlayerClassDefinition classDefinition, PlayerClassProgress classProgress, String classId) {
        List<Component> lore = new ArrayList<>();
        if (classDefinition == null) {
            lore.add(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_LIST_EMPTY));
            return GuiItems.of(Material.BLAZE_POWDER, services.messages().render(MessageKey.GUI_PLAYER_PROFILE_ABILITIES_ITEM), lore);
        }
        int classLevel = classProgress.levelProgress(classId).level();
        services.abilities().definitions().stream()
                .filter(definition -> definition.classId().equals(classId))
                .sorted((left, right) -> Integer.compare(left.requiredLevel(), right.requiredLevel()))
                .forEach(definition -> lore.add(services.messages().render(
                        classLevel >= definition.requiredLevel()
                                ? MessageKey.GUI_PLAYER_PROFILE_ABILITY_OPEN
                                : MessageKey.GUI_PLAYER_PROFILE_ABILITY_LOCKED,
                        Map.of("kabiliyet", definition.displayName(), "seviye", Integer.toString(definition.requiredLevel()))
                )));
        if (lore.isEmpty()) {
            lore.add(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_LIST_EMPTY));
        }
        return GuiItems.of(Material.BLAZE_POWDER, services.messages().render(MessageKey.GUI_PLAYER_PROFILE_ABILITIES_ITEM), lore);
    }

    private ItemStack unlockedItem(Material icon, MessageKey nameKey, int unlocked, int total, List<String> names) {
        List<Component> lore = new ArrayList<>();
        lore.add(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_UNLOCKED_COUNT, Map.of(
                "acik", Integer.toString(unlocked), "toplam", Integer.toString(total)
        )));
        if (names.isEmpty()) {
            lore.add(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_LIST_EMPTY));
        } else {
            names.forEach(name -> lore.add(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_LIST_LINE, Map.of("ad", name))));
        }
        return GuiItems.of(icon, services.messages().render(nameKey), lore);
    }

    /**
     * Only a real button when a request could actually succeed: both sides
     * unlocked, the target online, and the viewer looking at somebody other
     * than themselves. Anything else renders as a greyed explanation, so the
     * screen never invites a click it will refuse.
     */
    private ItemStack tradeButton(Player viewer) {
        Player target = Bukkit.getPlayer(targetId);
        if (viewer.getUniqueId().equals(targetId)) {
            tradeOffered = false;
            return GuiItems.of(Material.GRAY_DYE, services.messages().render(MessageKey.GUI_PLAYER_PROFILE_TRADE_SELF));
        }
        if (target == null) {
            tradeOffered = false;
            return GuiItems.of(Material.GRAY_DYE, services.messages().render(MessageKey.GUI_PLAYER_PROFILE_TRADE_OFFLINE));
        }
        if (!services.trades().isUnlocked(viewer.getUniqueId()) || !services.trades().isUnlocked(targetId)) {
            tradeOffered = false;
            return GuiItems.of(Material.GRAY_DYE, services.messages().render(MessageKey.GUI_PLAYER_PROFILE_TRADE_LOCKED));
        }
        tradeOffered = true;
        return GuiItems.of(Material.EMERALD,
                services.messages().render(MessageKey.GUI_PLAYER_PROFILE_TRADE_ITEM, Map.of("oyuncu", targetName)),
                List.of(services.messages().render(MessageKey.GUI_PLAYER_PROFILE_TRADE_LORE)));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AnaMenu(services));
            return;
        }
        if (slot != TRADE_SLOT || !tradeOffered) {
            return;
        }
        TradeRequestResult result = services.trades().request(player, targetName);
        MessageKey key = switch (result) {
            case SENT -> MessageKey.TRADE_REQUEST_SENT;
            case SELF -> MessageKey.TRADE_SELF;
            case BUSY -> MessageKey.TRADE_BUSY;
            case LOCKED -> MessageKey.TRADE_LOCKED;
            case PARTNER_LOCKED -> MessageKey.TRADE_PARTNER_LOCKED;
            case PARTNER_OFFLINE -> MessageKey.TRADE_PARTNER_OFFLINE;
            case PARTNER_NOT_ACTIVATED -> MessageKey.TRADE_PARTNER_NOT_ACTIVATED;
        };
        services.messages().send(player, key, Map.of(
                "oyuncu", targetName,
                "seviye", Integer.toString(FeatureUnlockRules.TRADE_CLASS_LEVEL)
        ));
        player.closeInventory();
    }

    /** Everything that needed a database round trip, gathered before a single item is painted. */
    private record Standings(int power, int powerRank, long reputation, int reputationRank, Map<String, Integer> skillRanks) {
    }
}
