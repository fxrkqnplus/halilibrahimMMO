package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.classes.*;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

/** Command-only class selection and talent allocation until inventory GUIs arrive. */
public final class ClassCommand implements BasicCommand {
    private final JavaPlugin plugin; private final ClassService classes; private final ProfileService profiles; private final EnergyService energy; private final MessageService messages;
    public ClassCommand(JavaPlugin plugin, ClassService classes, ProfileService profiles, EnergyService energy, MessageService messages) { this.plugin = plugin; this.classes = classes; this.profiles = profiles; this.energy = energy; this.messages = messages; }
    @Override public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) { messages.send(stack.getSender(), MessageKey.PLAYER_ONLY); return; }
        if (args.length == 0) { list(player); return; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sec" -> { if (args.length != 2) { list(player); return; } select(player, args[1]); }
            case "agac" -> tree(player);
            case "ac" -> { if (args.length != 2) { messages.send(player, MessageKey.CLASS_USAGE); return; } spend(player, args[1]); }
            default -> list(player);
        }
    }
    private void list(Player player) { String all = classes.definitions().stream().map(def -> def.id() + " (" + def.displayName() + ")").sorted().reduce((a,b)->a+", "+b).orElse("-"); messages.send(player, MessageKey.CLASS_LIST, Map.of("siniflar", all)); }
    private void select(Player player, String id) { classes.select(player.getUniqueId(), player.getName(), id).thenAccept(result -> player.getScheduler().run(plugin, task -> { switch (result.status()) { case SELECTED -> { classes.applyPlayerEffects(player); energy.reconcile(player); messages.send(player, MessageKey.CLASS_SELECTED, Map.of("sinif", classes.displayName(result.classId()))); } case ALREADY_SELECTED -> messages.send(player, MessageKey.CLASS_ALREADY_SELECTED, Map.of("sinif", classes.displayName(result.classId()))); case UNKNOWN_CLASS -> messages.send(player, MessageKey.CLASS_UNKNOWN, Map.of("sinif", result.classId())); case COOLDOWN -> messages.send(player, MessageKey.CLASS_COOLDOWN, Map.of("sure", Long.toString(Math.max(1, (result.remainingMillis() + 999) / 1000)))); } }, null)); }
    private void tree(Player player) {
        profiles.load(player.getUniqueId(), player.getName()).thenCompose(profile -> classes.load(player.getUniqueId(), player.getName()).thenApply(progress -> new Object[] {profile, progress}))
                .thenAccept(values -> player.getScheduler().run(plugin, task -> {
                    com.halilibrahim.halilibrahimmmo.profile.PlayerProfile profile = (com.halilibrahim.halilibrahimmmo.profile.PlayerProfile) values[0];
                    PlayerClassProgress progress = (PlayerClassProgress) values[1];
                    if (profile.selectedClass().isEmpty()) { messages.send(player, MessageKey.CLASS_NO_SELECTION); return; }
                    PlayerClassDefinition definition = classes.settings().definitions().get(profile.selectedClass());
                    if (definition == null) { messages.send(player, MessageKey.CLASS_UNKNOWN, Map.of("sinif", profile.selectedClass())); return; }
                    String talents = definition.talents().values().stream().map(talent -> talent.id() + " " + progress.talentRanks().getOrDefault(definition.id(), Map.of()).getOrDefault(talent.id(), 0) + "/" + talent.maximumRank()).reduce((left, right) -> left + ", " + right).orElse("-");
                    int budget = classes.settings().levelsPerTalentPoint() == 0 ? 0 : profile.prestige() * 4 + profile.globalLevel() / classes.settings().levelsPerTalentPoint();
                    messages.send(player, MessageKey.CLASS_TREE, Map.of("sinif", definition.displayName(), "harcanan", Integer.toString(progress.spentPoints()), "toplam", Integer.toString(budget), "yetenekler", talents));
                }, null));
    }
    private void spend(Player player, String id) { classes.spend(player.getUniqueId(), player.getName(), id).thenAccept(result -> player.getScheduler().run(plugin, task -> { if (result.status() == TalentSpendResult.Status.SPENT) { classes.applyPlayerEffects(player); energy.reconcile(player); messages.send(player, MessageKey.CLASS_TALENT_SPENT, Map.of("yetenek", result.talentId(), "rank", Integer.toString(result.rank()), "kalan", Integer.toString(result.availablePoints()))); return; } MessageKey key = switch (result.status()) { case NO_CLASS -> MessageKey.CLASS_NO_SELECTION; case UNKNOWN_TALENT -> MessageKey.CLASS_UNKNOWN_TALENT; case MAXIMUM_RANK -> MessageKey.CLASS_TALENT_MAXIMUM; case NO_POINTS -> MessageKey.CLASS_NO_POINTS; case SPENT -> throw new IllegalStateException(); }; messages.send(player, key, Map.of("yetenek", result.talentId())); }, null)); }
    @Override public Collection<String> suggest(CommandSourceStack stack, String[] args) { if (args.length == 1) return List.of("sec", "agac", "ac").stream().filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList(); if (args.length == 2 && args[0].equalsIgnoreCase("sec")) return classes.definitions().stream().map(PlayerClassDefinition::id).filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT))).toList(); return List.of(); }
}
