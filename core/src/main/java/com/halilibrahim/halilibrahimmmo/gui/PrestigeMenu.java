package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeResult;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Prestige requirement checklist plus a confirm button - the only way to prestige, opened by {@code /prestij}. */
public final class PrestigeMenu implements GuiView {

    private static final int INFO_SLOT = 3;
    private static final int CONFIRM_SLOT = 5;
    private static final int BACK_SLOT = 8;

    private final GuiServices services;

    public PrestigeMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_PRESTIGE_TITLE);
    }

    @Override
    public int size() {
        return 9;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        inventory.setItem(CONFIRM_SLOT, GuiItems.of(Material.TOTEM_OF_UNDYING,
                services.messages().render(MessageKey.GUI_PRESTIGE_CONFIRM_ITEM),
                List.of(services.messages().render(MessageKey.GUI_PRESTIGE_CONFIRM_ITEM_LORE))));
        services.profiles().load(player.getUniqueId(), player.getName()).thenAccept(profile ->
                player.getScheduler().run(services.plugin(), task -> {
                    List<Component> lore = new ArrayList<>();
                    lore.add(services.messages().render(MessageKey.GUI_PRESTIGE_INFO_ITEM_LORE, Map.of(
                            "prestij", Integer.toString(profile.prestige()),
                            "maksimum", Integer.toString(services.prestige().settings().maximumPrestige())
                    )));
                    lore.add(services.messages().render(MessageKey.GUI_PRESTIGE_INFO_ITEM_LORE_DETAIL));
                    lore.add(services.messages().render(MessageKey.PRESTIGE_REQUIREMENT_HEADER));
                    lore.addAll(requirementLines(player.getUniqueId(), profile));
                    inventory.setItem(INFO_SLOT, GuiItems.of(Material.NETHER_STAR, services.messages().render(MessageKey.GUI_PRESTIGE_INFO_ITEM), lore));
                }, null));
    }

    /** Same checklist {@code PrestigeCommand} builds - small acceptable duplication between the command/GUI pair. */
    private List<Component> requirementLines(UUID playerId, PlayerProfile profile) {
        List<Component> lines = new ArrayList<>();
        int minimumSkillLevel = services.prestige().settings().minimumSkillLevel();
        for (var definition : services.skills().definitions()) {
            int level = services.skills().cached(playerId, definition.id()).map(progress -> progress.level()).orElse(1);
            boolean met = level >= minimumSkillLevel;
            lines.add(services.messages().render(met ? MessageKey.PRESTIGE_REQUIREMENT_SKILL_MET : MessageKey.PRESTIGE_REQUIREMENT_SKILL_UNMET, Map.of(
                    "beceri", definition.displayName(), "seviye", Integer.toString(level), "gereken", Integer.toString(minimumSkillLevel)
            )));
        }
        String classId = profile.selectedClass();
        if (classId.isEmpty()) {
            lines.add(services.messages().render(MessageKey.PRESTIGE_REQUIREMENT_NO_CLASS));
            return lines;
        }
        var classDefinition = services.classes().settings().definitions().get(classId);
        int classLevel = services.classes().classProgress(playerId, classId).level();
        int classLevelCap = services.classes().settings().classLevelCurve().maximumLevel();
        boolean classLevelMet = classLevel >= classLevelCap;
        lines.add(services.messages().render(classLevelMet ? MessageKey.PRESTIGE_REQUIREMENT_CLASS_LEVEL_MET : MessageKey.PRESTIGE_REQUIREMENT_CLASS_LEVEL_UNMET, Map.of(
                "sinif", classDefinition.displayName(), "seviye", Integer.toString(classLevel), "gereken", Integer.toString(classLevelCap)
        )));
        var synergySkill = services.skills().settings().definitions().get(classDefinition.synergySkillId());
        int synergyLevel = services.skills().cached(playerId, classDefinition.synergySkillId()).map(progress -> progress.level()).orElse(1);
        int synergyCap = services.skills().settings().curve().maximumLevel();
        boolean synergyMet = synergyLevel >= synergyCap;
        lines.add(services.messages().render(synergyMet ? MessageKey.PRESTIGE_REQUIREMENT_SYNERGY_MET : MessageKey.PRESTIGE_REQUIREMENT_SYNERGY_UNMET, Map.of(
                "beceri", synergySkill.displayName(), "seviye", Integer.toString(synergyLevel), "gereken", Integer.toString(synergyCap)
        )));
        return lines;
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new ProgressionMenu(services));
            return;
        }
        if (slot != CONFIRM_SLOT) {
            return;
        }
        services.prestige().prestige(player.getUniqueId(), player.getName()).thenAccept(result ->
                player.getScheduler().run(services.plugin(), task -> {
                    MessageKey key = switch (result.status()) {
                        case PRESTIGED -> MessageKey.PRESTIGE_SUCCESS;
                        case REQUIREMENTS_NOT_MET -> MessageKey.PRESTIGE_LEVEL_REQUIRED;
                        case MAXIMUM_REACHED -> MessageKey.PRESTIGE_MAXIMUM;
                    };
                    services.messages().send(player, key, Map.of(
                            "prestij", Integer.toString(result.prestige()),
                            "maksimum", Integer.toString(services.prestige().settings().maximumPrestige())
                    ));
                    if (result.status() == PrestigeResult.Status.PRESTIGED) {
                        services.feedback().celebratePrestige(player, result.prestige());
                        services.skills().applyMilestoneEffects(player);
                        services.sidebar().refresh(player);
                        celebratePrestigeFanfare(player, result.prestige());
                        services.titles().evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyTitles(player, unlocked));
                        services.cosmetics().evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyCosmetics(player, unlocked));
                    }
                    GuiManager.open(player, new PrestigeMenu(services));
                }, null));
    }

    private void celebratePrestigeFanfare(Player player, int newPrestige) {
        services.messages().broadcast(MessageKey.PRESTIGE_BROADCAST, Map.of("oyuncu", player.getName(), "prestij", Integer.toString(newPrestige)));
        player.getWorld().spawn(player.getLocation(), Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().withColor(org.bukkit.Color.YELLOW, org.bukkit.Color.PURPLE)
                    .with(FireworkEffect.Type.BURST).trail(true).flicker(true).build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
        });
        Bukkit.getOnlinePlayers().forEach(online -> {
            online.playSound(online.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
            online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        });
    }

    private void notifyTitles(Player player, List<TitleDefinition> unlocked) {
        if (unlocked.isEmpty()) {
            return;
        }
        player.getScheduler().run(services.plugin(), task -> unlocked.forEach(definition ->
                services.messages().send(player, MessageKey.TITLE_UNLOCKED, Map.of("unvan", definition.displayName()))), null);
    }

    private void notifyCosmetics(Player player, List<CosmeticDefinition> unlocked) {
        if (unlocked.isEmpty()) {
            return;
        }
        player.getScheduler().run(services.plugin(), task -> unlocked.forEach(definition ->
                services.messages().send(player, MessageKey.COSMETIC_UNLOCKED, Map.of("kozmetik", definition.displayName()))), null);
    }
}
