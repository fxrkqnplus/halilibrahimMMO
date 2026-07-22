package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Displays the invoking player's asynchronously loaded global profile. */
public final class ProfileCommand implements BasicCommand {

    private final ProfileService profiles;
    private final MessageService messages;
    private final JavaPlugin plugin;
    private final ClassService classes;
    private final ProgressionService progression;

    public ProfileCommand(JavaPlugin plugin, ProfileService profiles, MessageService messages, ClassService classes, ProgressionService progression) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.messages = messages;
        this.classes = classes;
        this.progression = progression;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        profiles.load(player.getUniqueId(), player.getName()).thenAccept(profile ->
                player.getScheduler().run(plugin, task -> {
                    String guc = Integer.toString(progression.cachedGlobalLevel(player.getUniqueId()));
                    if (profile.selectedClass().isEmpty()) {
                        messages.send(player, MessageKey.PROFILE_OVERVIEW_NO_CLASS, Map.of(
                                "seviye", guc,
                                "prestij", Integer.toString(profile.prestige())
                        ));
                    } else {
                        messages.send(player, MessageKey.PROFILE_OVERVIEW, Map.of(
                                "seviye", guc,
                                "prestij", Integer.toString(profile.prestige()),
                                "sinif", classes.displayName(profile.selectedClass())
                        ));
                    }
                }, null));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        return List.of();
    }
}
