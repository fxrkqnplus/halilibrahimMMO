package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeResult;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeService;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Explicit two-step prestige command to avoid accidental global XP resets. */
public final class PrestigeCommand implements BasicCommand {

    private final JavaPlugin plugin;
    private final PrestigeService prestige;
    private final ProfileService profiles;
    private final ProgressionService progression;
    private final TitleService titles;
    private final CosmeticService cosmetics;
    private final FeedbackService feedback;
    private final MessageService messages;
    private final SidebarService sidebar;
    private final SkillService skills;
    private final ClassService classes;

    public PrestigeCommand(
            JavaPlugin plugin,
            PrestigeService prestige,
            ProfileService profiles,
            ProgressionService progression,
            TitleService titles,
            CosmeticService cosmetics,
            FeedbackService feedback,
            MessageService messages,
            SidebarService sidebar,
            SkillService skills,
            ClassService classes
    ) {
        this.plugin = plugin;
        this.prestige = prestige;
        this.profiles = profiles;
        this.progression = progression;
        this.titles = titles;
        this.cosmetics = cosmetics;
        this.feedback = feedback;
        this.messages = messages;
        this.sidebar = sidebar;
        this.skills = skills;
        this.classes = classes;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("onayla")) {
            confirm(player);
            return;
        }
        profiles.load(player.getUniqueId(), player.getName()).thenAccept(profile ->
                player.getScheduler().run(plugin, task -> {
                    List<Component> lines = new ArrayList<>();
                    lines.add(messages.render(MessageKey.PRESTIGE_STATUS, Map.of(
                            "prestij", Integer.toString(profile.prestige()),
                            "maksimum", Integer.toString(prestige.settings().maximumPrestige())
                    )));
                    lines.add(messages.render(MessageKey.PRESTIGE_REQUIREMENT_HEADER));
                    lines.addAll(requirementLines(player.getUniqueId(), profile));
                    player.sendMessage(messages.prefixed(Component.join(JoinConfiguration.newlines(), lines)));
                }, null));
    }

    /** Same checklist {@code PrestigeMenu} builds - small acceptable duplication between the command/GUI pair. */
    private List<Component> requirementLines(UUID playerId, PlayerProfile profile) {
        List<Component> lines = new ArrayList<>();
        int minimumSkillLevel = prestige.settings().minimumSkillLevel();
        for (var definition : skills.definitions()) {
            int level = skills.cached(playerId, definition.id()).map(progress -> progress.level()).orElse(1);
            boolean met = level >= minimumSkillLevel;
            lines.add(messages.render(met ? MessageKey.PRESTIGE_REQUIREMENT_SKILL_MET : MessageKey.PRESTIGE_REQUIREMENT_SKILL_UNMET, Map.of(
                    "beceri", definition.displayName(), "seviye", Integer.toString(level), "gereken", Integer.toString(minimumSkillLevel)
            )));
        }
        String classId = profile.selectedClass();
        if (classId.isEmpty()) {
            lines.add(messages.render(MessageKey.PRESTIGE_REQUIREMENT_NO_CLASS));
            return lines;
        }
        var classDefinition = classes.settings().definitions().get(classId);
        int classLevel = classes.classProgress(playerId, classId).level();
        int classLevelCap = classes.settings().classLevelCurve().maximumLevel();
        boolean classLevelMet = classLevel >= classLevelCap;
        lines.add(messages.render(classLevelMet ? MessageKey.PRESTIGE_REQUIREMENT_CLASS_LEVEL_MET : MessageKey.PRESTIGE_REQUIREMENT_CLASS_LEVEL_UNMET, Map.of(
                "sinif", classDefinition.displayName(), "seviye", Integer.toString(classLevel), "gereken", Integer.toString(classLevelCap)
        )));
        var synergySkill = skills.settings().definitions().get(classDefinition.synergySkillId());
        int synergyLevel = skills.cached(playerId, classDefinition.synergySkillId()).map(progress -> progress.level()).orElse(1);
        int synergyCap = skills.settings().curve().maximumLevel();
        boolean synergyMet = synergyLevel >= synergyCap;
        lines.add(messages.render(synergyMet ? MessageKey.PRESTIGE_REQUIREMENT_SYNERGY_MET : MessageKey.PRESTIGE_REQUIREMENT_SYNERGY_UNMET, Map.of(
                "beceri", synergySkill.displayName(), "seviye", Integer.toString(synergyLevel), "gereken", Integer.toString(synergyCap)
        )));
        return lines;
    }

    private void confirm(Player player) {
        prestige.prestige(player.getUniqueId(), player.getName()).thenAccept(result ->
                player.getScheduler().run(plugin, task -> {
                    MessageKey key = switch (result.status()) {
                        case PRESTIGED -> MessageKey.PRESTIGE_SUCCESS;
                        case REQUIREMENTS_NOT_MET -> MessageKey.PRESTIGE_LEVEL_REQUIRED;
                        case MAXIMUM_REACHED -> MessageKey.PRESTIGE_MAXIMUM;
                    };
                    messages.send(player, key, Map.of(
                            "prestij", Integer.toString(result.prestige()),
                            "maksimum", Integer.toString(prestige.settings().maximumPrestige())
                    ));
                    if (result.status() == PrestigeResult.Status.PRESTIGED) {
                        feedback.celebratePrestige(player, result.prestige());
                        skills.applyMilestoneEffects(player);
                        sidebar.refresh(player);
                        celebratePrestigeFanfare(player, result.prestige());
                        titles.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyTitles(player, unlocked));
                        cosmetics.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyCosmetics(player, unlocked));
                    }
                }, null));
    }

    private void celebratePrestigeFanfare(Player player, int newPrestige) {
        messages.broadcast(MessageKey.PRESTIGE_BROADCAST, Map.of("oyuncu", player.getName(), "prestij", Integer.toString(newPrestige)));
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
        player.getScheduler().run(plugin, task -> unlocked.forEach(definition ->
                messages.send(player, MessageKey.TITLE_UNLOCKED, Map.of("unvan", definition.displayName()))), null);
    }

    private void notifyCosmetics(Player player, List<CosmeticDefinition> unlocked) {
        if (unlocked.isEmpty()) {
            return;
        }
        player.getScheduler().run(plugin, task -> unlocked.forEach(definition ->
                messages.send(player, MessageKey.COSMETIC_UNLOCKED, Map.of("kozmetik", definition.displayName()))), null);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return args.length == 1
                ? List.of("onayla").stream().filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList()
                : List.of();
    }
}
