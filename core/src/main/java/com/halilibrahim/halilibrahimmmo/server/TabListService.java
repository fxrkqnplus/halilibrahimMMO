package com.halilibrahim.halilibrahimmmo.server;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import com.halilibrahim.halilibrahimmmo.titles.TitleChatStyle;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Map;

/**
 * Owns everything the TAB list shows: the header/footer banner (server name,
 * player count, live TPS, the viewer's own ping and the connection address) and
 * each entry's name, prefixed with that player's equipped title in the title's
 * own colour.
 *
 * <p><strong>Repainted on a timer rather than on events.</strong> TPS and ping
 * are continuous values with no event to hook, so there is no "changed" moment
 * to react to - and the whole list is cheap to rebuild (one component per
 * online player). {@code sunucu.yml} owns the interval.</p>
 *
 * <p>The title tag is built as a directly-styled {@link Component} rather than
 * through a {@code MessageKey} template, the same documented exception
 * {@code TitleChatListener} and the sidebar's name line already carry: a
 * title's colour is per-title configured data, and {@code MessageService} only
 * substitutes unparsed string placeholders, which cannot vary their own
 * colour.</p>
 */
public final class TabListService implements PluginService {

    /** Above this the server is keeping up; the footer says so in green. */
    private static final double HEALTHY_TPS = 19.0D;
    /** Below this the server is genuinely struggling rather than merely busy. */
    private static final double LAGGING_TPS = 15.0D;

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final ServerInfoService serverInfo;
    private final TitleService titles;
    private final ProgressionService progression;
    private ScheduledTask refreshTask;

    public TabListService(
            JavaPlugin plugin, MessageService messages, ServerInfoService serverInfo,
            TitleService titles, ProgressionService progression
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.serverInfo = serverInfo;
        this.titles = titles;
        this.progression = progression;
    }

    @Override
    public void enable() {
        if (!serverInfo.settings().tabListEnabled()) {
            return;
        }
        long period = serverInfo.settings().tabRefreshTicks();
        refreshTask = plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, task -> Bukkit.getOnlinePlayers().forEach(this::render), period, period);
    }

    @Override
    public void disable() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    /**
     * Restarts the repaint loop against the freshly-read interval, and turns it
     * off entirely if the file now disables the list - otherwise a reload that
     * flipped {@code etkin} to {@code false} would leave the old task painting.
     */
    @Override
    public void reload() {
        disable();
        enable();
    }

    private void render(Player player) {
        double tps = Math.min(20.0D, Bukkit.getTPS()[0]);
        Map<String, String> placeholders = Map.of(
                "cevrimici", Integer.toString(Bukkit.getOnlinePlayers().size()),
                "maksimum", Integer.toString(Bukkit.getMaxPlayers()),
                "tps", String.format(Locale.ROOT, "%.2f", tps),
                "ping", Integer.toString(Math.max(0, player.getPing()))
        );
        player.sendPlayerListHeaderAndFooter(
                messages.render(MessageKey.TAB_HEADER, placeholders),
                messages.render(footerKey(tps), placeholders)
        );
        player.playerListName(entryName(player));
    }

    /**
     * One fully-authored footer per server-health band rather than a single
     * template fed a Java-chosen colour - the same discipline every per-rarity
     * boss line and per-cause XP bar already follows, and the only way a
     * server owner can restyle the TPS readout from {@code messages.yml}.
     */
    private MessageKey footerKey(double tps) {
        if (tps >= HEALTHY_TPS) {
            return MessageKey.TAB_FOOTER_HEALTHY;
        }
        return tps >= LAGGING_TPS ? MessageKey.TAB_FOOTER_BUSY : MessageKey.TAB_FOOTER_LAGGING;
    }

    private Component entryName(Player player) {
        Component name = messages.render(MessageKey.TAB_PLAYER_ENTRY, Map.of(
                "oyuncu", player.getName(),
                "guc", Integer.toString(progression.cachedGlobalLevel(player.getUniqueId()))
        ));
        return titles.equippedDisplayName(player.getUniqueId())
                .map(titleName -> titleTag(player, titleName).append(name))
                .orElse(name);
    }

    private Component titleTag(Player player, String titleName) {
        TitleChatStyle style = titles.equippedStyle(player.getUniqueId()).orElse(TitleChatStyle.MAVI);
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(titleName).color(style.color()).decoration(TextDecoration.BOLD, style.bold()))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }
}
