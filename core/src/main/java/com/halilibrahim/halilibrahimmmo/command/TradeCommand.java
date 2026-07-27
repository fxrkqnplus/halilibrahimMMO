package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.progression.FeatureUnlockRules;
import com.halilibrahim.halilibrahimmmo.trade.TradeRequestResult;
import com.halilibrahim.halilibrahimmmo.trade.TradeService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /takas} (alias {@code /trade}): {@code <oyuncu>} to invite,
 * {@code kabul}/{@code ret} to answer an invitation, {@code iptal} to walk away
 * from a live trade.
 *
 * <p>The second and last command in this plugin that takes an argument, for the
 * same reason {@code /ev} does: naming the person you want to trade with is not
 * something a menu can ask. Everything after the invitation - putting items up,
 * taking them back, confirming - happens in the trade screen.</p>
 */
public final class TradeCommand implements BasicCommand {

    private static final String ACCEPT_ACTION = "kabul";
    private static final String DECLINE_ACTION = "ret";
    private static final String CANCEL_ACTION = "iptal";

    private final TradeService trades;
    private final MessageService messages;

    public TradeCommand(TradeService trades, MessageService messages) {
        this.trades = trades;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        if (args.length == 0) {
            messages.send(player, MessageKey.TRADE_USAGE);
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case ACCEPT_ACTION -> {
                if (!trades.accept(player)) {
                    messages.send(player, MessageKey.TRADE_NO_REQUEST);
                }
            }
            case DECLINE_ACTION -> {
                if (trades.decline(player)) {
                    messages.send(player, MessageKey.TRADE_DECLINED);
                } else {
                    messages.send(player, MessageKey.TRADE_NO_REQUEST);
                }
            }
            case CANCEL_ACTION -> trades.cancel(player);
            default -> report(player, args[0], trades.request(player, args[0]));
        }
    }

    private void report(Player player, String targetName, TradeRequestResult result) {
        switch (result) {
            case SENT -> messages.send(player, MessageKey.TRADE_REQUEST_SENT, Map.of("oyuncu", targetName));
            case SELF -> messages.send(player, MessageKey.TRADE_SELF);
            case PARTNER_OFFLINE -> messages.send(player, MessageKey.TRADE_PARTNER_OFFLINE, Map.of("oyuncu", targetName));
            case BUSY -> messages.send(player, MessageKey.TRADE_BUSY);
            case PARTNER_NOT_ACTIVATED -> messages.send(player, MessageKey.TRADE_PARTNER_NOT_ACTIVATED, Map.of("oyuncu", targetName));
            case LOCKED -> messages.send(player, MessageKey.TRADE_LOCKED, Map.of(
                    "seviye", Integer.toString(FeatureUnlockRules.TRADE_CLASS_LEVEL)
            ));
            case PARTNER_LOCKED -> messages.send(player, MessageKey.TRADE_PARTNER_LOCKED, Map.of(
                    "oyuncu", targetName,
                    "seviye", Integer.toString(FeatureUnlockRules.TRADE_PARTNER_CLASS_LEVEL)
            ));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length > 1) {
            return List.of();
        }
        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>(List.of(ACCEPT_ACTION, DECLINE_ACTION, CANCEL_ACTION));
        Bukkit.getOnlinePlayers().forEach(online -> suggestions.add(online.getName()));
        return suggestions.stream().filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
