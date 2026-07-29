package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.classes.ClassExperienceAwardResult;
import com.halilibrahim.halilibrahimmmo.classes.ClassLevelProgress;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassProgress;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.NumberFormatter;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.skills.SkillAdminAwardResult;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Offline-capable admin editor for one registered player's skill/class levels
 * and XP, plus force-setting their class - the GUI-only, click-only
 * replacement for the deleted {@code /rpg oyuncu} chat commands (Phase 10.1
 * deleted them; this phase deliberately reintroduces the capability, scoped
 * to {@code /haliladmin} and offline-reachable). Every mutation reopens this
 * same menu with freshly-read values, mirroring {@code ClassMenu}/
 * {@code AbilitiesMenu}'s existing reopen-after-action pattern.
 */
public final class AdminPlayerEditMenu implements GuiView {

    private static final int INFO_SLOT = 4;
    private static final int FIRST_SKILL_LEVEL_SLOT = 10;
    private static final int FIRST_SKILL_XP_SLOT = 19;
    private static final int CLASS_LEVEL_SLOT = 30;
    private static final int CLASS_XP_SLOT = 31;
    private static final int WEALTH_SLOT = 32;
    private static final int FORCE_CLASS_SLOT = 40;
    private static final int BACK_SLOT = 49;

    private final GuiServices services;
    private final UUID targetId;
    private final String targetName;
    private final Map<Integer, String> slotToSkillLevelId = new LinkedHashMap<>();
    private final Map<Integer, String> slotToSkillXpId = new LinkedHashMap<>();
    private final Map<String, Integer> currentSkillLevels = new LinkedHashMap<>();
    private String currentClassId = "";
    private int currentClassLevel = 1;

    public AdminPlayerEditMenu(GuiServices services, UUID targetId, String targetName) {
        this.services = services;
        this.targetId = targetId;
        this.targetName = targetName;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ADMIN_EDIT_TITLE);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        CompletableFuture<PlayerProfile> profileFuture = services.profiles().load(targetId, targetName);
        CompletableFuture<PlayerClassProgress> classFuture = services.classes().load(targetId, targetName);
        CompletableFuture<?>[] skillFutures = services.skills().definitions().stream()
                .map(definition -> services.skills().load(targetId, definition.id()))
                .toArray(CompletableFuture[]::new);
        CompletableFuture<Void> allSkillsLoaded = CompletableFuture.allOf(skillFutures);
        profileFuture.thenCompose(profile -> classFuture.thenCompose(classProgress -> allSkillsLoaded.thenAccept(ignored ->
                player.getScheduler().run(services.plugin(), task -> render(inventory, profile, classProgress), null))));
    }

    private void render(Inventory inventory, PlayerProfile profile, PlayerClassProgress classProgress) {
        currentClassId = profile.selectedClass();
        int guc = services.progression().cachedGlobalLevel(targetId);
        inventory.setItem(INFO_SLOT, GuiItems.of(Material.PLAYER_HEAD,
                services.messages().render(MessageKey.GUI_ADMIN_EDIT_INFO_ITEM, Map.of("oyuncu", targetName)),
                List.of(currentClassId.isEmpty()
                        ? services.messages().render(MessageKey.GUI_ADMIN_EDIT_INFO_LORE_NO_CLASS, Map.of(
                                "seviye", Integer.toString(guc), "prestij", Integer.toString(profile.prestige())))
                        : services.messages().render(MessageKey.GUI_ADMIN_EDIT_INFO_LORE, Map.of(
                                "seviye", Integer.toString(guc), "prestij", Integer.toString(profile.prestige()),
                                "sinif", services.classes().displayName(currentClassId))))));

        int levelSlot = FIRST_SKILL_LEVEL_SLOT;
        int xpSlot = FIRST_SKILL_XP_SLOT;
        for (SkillDefinition definition : services.skills().definitions()) {
            int level = services.skills().cached(targetId, definition.id()).map(progress -> progress.level()).orElse(1);
            long xp = services.skills().cached(targetId, definition.id()).map(progress -> progress.experience()).orElse(0L);
            currentSkillLevels.put(definition.id(), level);
            slotToSkillLevelId.put(levelSlot, definition.id());
            inventory.setItem(levelSlot++, GuiItems.of(Material.EXPERIENCE_BOTTLE,
                    services.messages().render(MessageKey.GUI_ADMIN_EDIT_SKILL_LEVEL_ITEM, Map.of("beceri", definition.displayName(), "seviye", Integer.toString(level))),
                    List.of(services.messages().render(MessageKey.GUI_ADMIN_EDIT_SKILL_LEVEL_LORE))));
            slotToSkillXpId.put(xpSlot, definition.id());
            inventory.setItem(xpSlot++, GuiItems.of(Material.GLOWSTONE_DUST,
                    services.messages().render(MessageKey.GUI_ADMIN_EDIT_SKILL_XP_ITEM, Map.of("beceri", definition.displayName(), "xp", Long.toString(xp))),
                    List.of(services.messages().render(MessageKey.GUI_ADMIN_EDIT_SKILL_XP_LORE))));
        }

        if (currentClassId.isEmpty()) {
            inventory.setItem(CLASS_LEVEL_SLOT, GuiItems.of(Material.BARRIER, services.messages().render(MessageKey.GUI_ADMIN_EDIT_NO_CLASS_ITEM)));
            currentClassLevel = 1;
        } else {
            PlayerClassDefinition classDefinition = services.classes().settings().definitions().get(currentClassId);
            ClassLevelProgress levelProgress = classProgress.levelProgress(currentClassId);
            currentClassLevel = levelProgress.level();
            String className = classDefinition == null ? currentClassId : classDefinition.displayName();
            inventory.setItem(CLASS_LEVEL_SLOT, GuiItems.of(Material.NETHERITE_SWORD,
                    services.messages().render(MessageKey.GUI_ADMIN_EDIT_CLASS_LEVEL_ITEM, Map.of("sinif", className, "seviye", Integer.toString(currentClassLevel))),
                    List.of(services.messages().render(MessageKey.GUI_ADMIN_EDIT_CLASS_LEVEL_LORE))));
            inventory.setItem(CLASS_XP_SLOT, GuiItems.of(Material.NETHER_STAR,
                    services.messages().render(MessageKey.GUI_ADMIN_EDIT_CLASS_XP_ITEM, Map.of("sinif", className, "xp", Long.toString(levelProgress.experience()))),
                    List.of(services.messages().render(MessageKey.GUI_ADMIN_EDIT_CLASS_XP_LORE))));
        }

        inventory.setItem(WEALTH_SLOT, GuiItems.of(Material.DIAMOND,
                services.messages().render(MessageKey.GUI_ADMIN_EDIT_WEALTH_ITEM, Map.of(
                        "zenginlik", NumberFormatter.compact(services.wealth().cached(targetId))
                )),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_EDIT_WEALTH_LORE))));

        inventory.setItem(FORCE_CLASS_SLOT, GuiItems.of(Material.ANVIL,
                services.messages().render(MessageKey.GUI_ADMIN_EDIT_FORCE_CLASS_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_EDIT_FORCE_CLASS_LORE))));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AdminAllPlayersMenu(services));
            return;
        }
        if (slot == FORCE_CLASS_SLOT) {
            GuiManager.open(player, new AdminClassPickerMenu(services, targetId, targetName));
            return;
        }
        if (slot == WEALTH_SLOT) {
            promptForWealth(player);
            return;
        }
        String levelSkillId = slotToSkillLevelId.get(slot);
        if (levelSkillId != null) {
            adjustSkillLevel(player, levelSkillId, click);
            return;
        }
        String xpSkillId = slotToSkillXpId.get(slot);
        if (xpSkillId != null) {
            addSkillExperience(player, xpSkillId, click);
            return;
        }
        if (slot == CLASS_LEVEL_SLOT && !currentClassId.isEmpty()) {
            adjustClassLevel(player, click);
            return;
        }
        if (slot == CLASS_XP_SLOT && !currentClassId.isEmpty()) {
            addClassExperience(player, click);
        }
    }

    private void adjustSkillLevel(Player player, String skillId, ClickType click) {
        int delta = switch (click) {
            case LEFT -> 1;
            case SHIFT_LEFT -> 10;
            case RIGHT -> -1;
            case SHIFT_RIGHT -> -10;
            default -> 0;
        };
        if (delta == 0) {
            return;
        }
        int newLevel = currentSkillLevels.getOrDefault(skillId, 1) + delta;
        services.skills().adminSetLevel(targetId, skillId, newLevel).thenAccept(result -> {
            services.logger().info("Yonetici " + player.getName() + ", " + targetName + " oyuncusunun " + skillId + " becerisini " + result.newLevel() + ". seviyeye ayarladi.");
            celebrateSkillLevelUpIfNeeded(skillId, result);
            player.getScheduler().run(services.plugin(), task -> GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName)), null);
        });
    }

    private void addSkillExperience(Player player, String skillId, ClickType click) {
        long amount = switch (click) {
            case LEFT -> 50L;
            case SHIFT_LEFT -> 500L;
            default -> 0L;
        };
        if (amount == 0L) {
            return;
        }
        services.skills().adminAddExperience(targetId, skillId, amount).thenAccept(result -> {
            services.logger().info("Yonetici " + player.getName() + ", " + targetName + " oyuncusunun " + skillId + " becerisine " + amount + " XP ekledi.");
            celebrateSkillLevelUpIfNeeded(skillId, result);
            player.getScheduler().run(services.plugin(), task -> GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName)), null);
        });
    }

    private void adjustClassLevel(Player player, ClickType click) {
        int delta = switch (click) {
            case LEFT -> 1;
            case SHIFT_LEFT -> 10;
            case RIGHT -> -1;
            case SHIFT_RIGHT -> -10;
            default -> 0;
        };
        if (delta == 0) {
            return;
        }
        int newLevel = currentClassLevel + delta;
        services.classes().adminSetClassLevel(targetId, targetName, currentClassId, newLevel).thenAccept(result -> {
            services.logger().info("Yonetici " + player.getName() + ", " + targetName + " oyuncusunun " + currentClassId + " sinif seviyesini " + result.newLevel() + " olarak ayarladi.");
            celebrateClassLevelUpIfNeeded(currentClassId, result);
            player.getScheduler().run(services.plugin(), task -> GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName)), null);
        });
    }

    private void addClassExperience(Player player, ClickType click) {
        long amount = switch (click) {
            case LEFT -> 50L;
            case SHIFT_LEFT -> 500L;
            default -> 0L;
        };
        if (amount == 0L) {
            return;
        }
        services.classes().adminAddClassExperience(targetId, targetName, currentClassId, amount).thenAccept(result -> {
            services.logger().info("Yonetici " + player.getName() + ", " + targetName + " oyuncusunun " + currentClassId + " sinif XP'sine " + amount + " ekledi.");
            celebrateClassLevelUpIfNeeded(currentClassId, result);
            player.getScheduler().run(services.plugin(), task -> GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName)), null);
        });
    }

    /**
     * The one admin field a click-increment row cannot express.
     *
     * <p>Zenginlik totals routinely run into the millions, so stepping one at a
     * time - or even five hundred at a time - is not a correction tool, it is a
     * repetitive-strain injury. The panel asks for the number in chat instead
     * ({@link ChatPromptService}), which keeps the admin surface GUI-driven
     * without pretending a number can be clicked into existence.</p>
     *
     * <p>This is also the only path in the plugin that can <em>lower</em> a
     * wealth total, and it is deliberately an overwrite rather than a delta:
     * staff correcting a rollback know what the number should be, not what to
     * add to reach it.</p>
     */
    private void promptForWealth(Player player) {
        services.chatPrompts().await(player, MessageKey.ADMIN_WEALTH_PROMPT, reply -> {
            long total;
            try {
                total = Long.parseLong(reply.replace(".", "").replace(",", "").trim());
            } catch (NumberFormatException exception) {
                services.messages().send(player, MessageKey.ADMIN_WEALTH_INVALID, Map.of("deger", reply));
                GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName));
                return;
            }
            if (total < 0) {
                services.messages().send(player, MessageKey.ADMIN_WEALTH_INVALID, Map.of("deger", reply));
                GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName));
                return;
            }
            services.wealth().adminSetTotal(targetId, total).thenAccept(applied ->
                    player.getScheduler().run(services.plugin(), task -> {
                        services.logger().info("Yonetici " + player.getName() + ", " + targetName
                                + " oyuncusunun zenginligini " + applied + " olarak ayarladi.");
                        services.messages().send(player, MessageKey.ADMIN_WEALTH_SET, Map.of(
                                "oyuncu", targetName, "zenginlik", Long.toString(applied)
                        ));
                        Player target = Bukkit.getPlayer(targetId);
                        if (target != null) {
                            services.sidebar().refresh(target);
                        }
                        GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName));
                    }, null));
        });
    }

    /**
     * Mirrors {@code SkillListener#handleAward}'s level-up feedback for the
     * organic block/mob XP path - admin-granted skill mutations bypassed the
     * entire {@code FeedbackService}/sidebar/Güç-milestone pipeline before
     * Phase 12 (a silent no-op level-up for the target player). A no-op if
     * the target is currently offline (feedback is live-player-only, exactly
     * like every other bossbar/sound/title moment in this codebase).
     */
    private void celebrateSkillLevelUpIfNeeded(String skillId, SkillAdminAwardResult result) {
        if (!result.leveledUp()) {
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            return;
        }
        SkillDefinition definition = services.skills().settings().definitions().get(skillId);
        String displayName = definition == null ? skillId : definition.displayName();
        target.getScheduler().run(services.plugin(), task -> {
            services.feedback().celebrateSkillLevelUp(target, displayName, result.newLevel());
            announceGucMilestones(target, result.newLevel() - result.previousLevel());
            services.skills().applyMilestoneEffects(target);
            services.sidebar().refresh(target);
        }, null);
    }

    /** Mirrors {@code SkillListener#announceGucMilestones} - see that method's javadoc for the delta-based derivation. */
    private void announceGucMilestones(Player target, int delta) {
        if (delta <= 0) {
            return;
        }
        int newGuc = services.progression().cachedGlobalLevel(target.getUniqueId());
        int previousGuc = newGuc - delta;
        for (int milestone : new int[] {25, 50, 75, 100}) {
            if (previousGuc < milestone && newGuc >= milestone) {
                services.messages().broadcast(MessageKey.GUC_BROADCAST, Map.of("oyuncu", target.getName(), "guc", Integer.toString(newGuc)));
            }
        }
    }

    /**
     * Mirrors {@code ClassSynergyBridge#onSkillExperienceGained}'s level-up
     * feedback for the organic class-XP path - admin-granted class mutations
     * bypassed the entire {@code FeedbackService}/ability-unlock/sidebar
     * pipeline before Phase 12. A no-op if the target is currently offline.
     */
    private void celebrateClassLevelUpIfNeeded(String classId, ClassExperienceAwardResult result) {
        if (!result.leveledUp()) {
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            return;
        }
        String className = services.classes().displayName(classId);
        target.getScheduler().run(services.plugin(), task -> {
            services.feedback().celebrateClassLevelUp(target, className, result.newLevel());
            notifyUnlockedAbilities(target, classId, result.previousLevel(), result.newLevel());
            services.sidebar().refresh(target);
        }, null);
    }

    private void notifyUnlockedAbilities(Player player, String classId, int previousLevel, int newLevel) {
        for (var ability : services.abilities().definitions()) {
            if (!ability.classId().equals(classId)) {
                continue;
            }
            if (ability.requiredLevel() > previousLevel && ability.requiredLevel() <= newLevel) {
                services.feedback().celebrateAbilityUnlocked(player, ability.displayName());
            }
        }
    }
}
