package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Lists every configured cosmetic (locked/unlocked) and equips one by id. */
public final class CosmeticCommand implements BasicCommand {

    private final JavaPlugin plugin;
    private final CosmeticService cosmetics;
    private final MessageService messages;

    public CosmeticCommand(JavaPlugin plugin, CosmeticService cosmetics, MessageService messages) {
        this.plugin = plugin;
        this.cosmetics = cosmetics;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sec")) {
            equip(player, args[1]);
            return;
        }
        list(player);
    }

    private void list(Player player) {
        cosmetics.unlockedIds(player.getUniqueId()).thenAccept(unlocked ->
                player.getScheduler().run(plugin, task -> {
                    List<CosmeticDefinition> sorted = cosmetics.definitions().stream()
                            .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
                    messages.send(player, MessageKey.COSMETIC_LIST, Map.of(
                            "acik", joined(sorted, unlocked, true),
                            "kilitli", joined(sorted, unlocked, false)
                    ));
                }, null));
    }

    private String joined(List<CosmeticDefinition> sorted, Set<String> unlocked, boolean wantUnlocked) {
        return sorted.stream()
                .filter(definition -> unlocked.contains(definition.id()) == wantUnlocked)
                .map(CosmeticDefinition::displayName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private void equip(Player player, String cosmeticId) {
        cosmetics.equip(player.getUniqueId(), player.getName(), cosmeticId).thenAccept(result ->
                player.getScheduler().run(plugin, task -> {
                    MessageKey key = switch (result.status()) {
                        case EQUIPPED -> MessageKey.COSMETIC_EQUIPPED;
                        case ALREADY_EQUIPPED -> MessageKey.COSMETIC_ALREADY_EQUIPPED;
                        case NOT_UNLOCKED -> MessageKey.COSMETIC_NOT_UNLOCKED;
                        case UNKNOWN_COSMETIC -> MessageKey.COSMETIC_UNKNOWN;
                    };
                    messages.send(player, key, Map.of("kozmetik", displayNameOrId(result.cosmeticId())));
                }, null));
    }

    private String displayNameOrId(String cosmeticId) {
        CosmeticDefinition definition = cosmetics.settings().definitions().get(cosmeticId);
        return definition == null ? cosmeticId : definition.displayName();
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length == 1) {
            return List.of("sec").stream().filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sec")) {
            return cosmetics.definitions().stream().map(CosmeticDefinition::id)
                    .filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
