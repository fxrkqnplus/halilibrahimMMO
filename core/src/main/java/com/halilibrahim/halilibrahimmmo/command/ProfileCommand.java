package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.activation.ActivatedPlayer;
import com.halilibrahim.halilibrahimmmo.gui.GuiManager;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
import com.halilibrahim.halilibrahimmmo.gui.PlayerProfileMenu;
import com.halilibrahim.halilibrahimmmo.gui.PlayerProfileMenu;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /profil} opens the player's own profile screen; {@code /profil <isim>}
 * opens somebody else's.
 *
 * <p><strong>The one argument this command takes is a player name</strong>,
 * the same exception {@code /ev} and {@code /takas} already carry: a menu
 * cannot collect a name, and a clickable list of every registered player is
 * exactly what a name lookup exists instead of. The bare form is unchanged and
 * still needs no argument.</p>
 *
 * <p>An offline player is looked up through the activated-players table rather
 * than refused: nearly everything the profile screen shows is persisted, and
 * being able to look somebody up while they are away is most of why one would
 * type a name at all. Only the trade button needs them present, and that button
 * says so itself.</p>
 */
public final class ProfileCommand implements BasicCommand {

    private final GuiServices services;

    public ProfileCommand(GuiServices services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            services.messages().send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (args.length == 0) {
            // The same screen /profil <isim> opens, pointed at yourself:
            // there was never a reason for your own profile to be a
            // three-line strip while everybody else's was a full record.
            GuiManager.open(player, new PlayerProfileMenu(services, player.getUniqueId(), player.getName()));
            return;
        }
        String requested = args[0];
        Player online = Bukkit.getPlayerExact(requested);
        if (online != null) {
            GuiManager.open(player, new PlayerProfileMenu(services, online.getUniqueId(), online.getName()));
            return;
        }
        services.activation().findAllActivated().thenAccept(registered -> {
            Optional<ActivatedPlayer> match = registered.stream()
                    .filter(candidate -> candidate.playerName().equalsIgnoreCase(requested))
                    .findFirst();
            player.getScheduler().run(services.plugin(), task -> match.ifPresentOrElse(
                    found -> GuiManager.open(player, new PlayerProfileMenu(services, found.playerId(), found.playerName())),
                    () -> services.messages().send(player, MessageKey.PROFILE_PLAYER_UNKNOWN, Map.of("oyuncu", requested))
            ), null);
        });
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length > 1) {
            return List.of();
        }
        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }
}
