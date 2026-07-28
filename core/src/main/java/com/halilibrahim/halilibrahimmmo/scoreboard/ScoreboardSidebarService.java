package com.halilibrahim.halilibrahimmmo.scoreboard;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.abilities.ActiveAbilityStatus;
import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.classes.ClassLevelProgress;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.home.HomeService;
import com.halilibrahim.halilibrahimmmo.locale.DurationFormatter;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingService;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingTask;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import com.halilibrahim.halilibrahimmmo.quests.QuestDefinition;
import com.halilibrahim.halilibrahimmmo.quests.QuestProgress;
import com.halilibrahim.halilibrahimmmo.quests.QuestService;
import com.halilibrahim.halilibrahimmmo.quests.QuestType;
import com.halilibrahim.halilibrahimmmo.titles.TitleChatStyle;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import com.halilibrahim.halilibrahimmmo.trade.TradeService;
import com.halilibrahim.halilibrahimmmo.trade.TradeSession;
import com.halilibrahim.halilibrahimmmo.wealth.WealthService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link Scoreboard}/{@link Objective}-backed implementation of {@link SidebarService}.
 *
 * <p>Every online player gets their own per-player {@link Scoreboard} instance
 * (never the shared main scoreboard, which stays untouched for other plugins).
 * Lines are rendered through one {@link Team} per line, each holding a single
 * invisible legacy-colour-code entry - the standard technique for showing
 * arbitrary, non-numeric text on a sidebar line since scoreboard entries
 * stopped being tied to real player names (Minecraft 1.13+). This keeps every
 * line an already-rendered {@link Component} (Turkish, MiniMessage-formatted,
 * routed through {@link MessageService}/{@link MessageKey}) rather than a
 * legacy 16-character player-name string.</p>
 *
 * <p>The rendered line count genuinely varies by player state (unactivated:
 * a single prompt plus footer; activated-no-class: several lines; activated-
 * with-class: potentially over a dozen once per-ability and per-daily-quest
 * lines are included) - {@link #lineEntry(int)} generates as many distinct
 * invisible entries as needed (not capped at a fixed array size), and
 * {@link #render} always unregisters any team beyond the current pass's line
 * count so a shrinking layout never leaves stale text on screen.</p>
 */
public final class ScoreboardSidebarService implements SidebarService {

    private static final String OBJECTIVE_NAME = "hmmo_sidebar";
    private static final String TEAM_PREFIX = "hmmo_line_";
    private static final String HEX = "0123456789abcdef";
    /** How many entries a vanilla sidebar objective actually displays; anything past this is dropped by the client. */
    private static final int MAXIMUM_LINES = 15;

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final ProfileService profiles;
    private final ClassService classes;
    private final ProgressionService progression;
    private final ActivationService activation;
    private final AbilityService abilities;
    private final QuestService quests;
    private final TitleService titles;
    private final OnboardingService onboarding;
    private final HomeService homes;
    private final TradeService trades;
    private final WealthService wealth;
    private final ConcurrentHashMap<UUID, Boolean> visible = new ConcurrentHashMap<>();
    private ScheduledTask refreshTask;

    public ScoreboardSidebarService(
            JavaPlugin plugin,
            MessageService messages,
            ProfileService profiles,
            ClassService classes,
            ProgressionService progression,
            ActivationService activation,
            AbilityService abilities,
            QuestService quests,
            TitleService titles,
            OnboardingService onboarding,
            HomeService homes,
            TradeService trades,
            WealthService wealth
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.profiles = profiles;
        this.classes = classes;
        this.progression = progression;
        this.activation = activation;
        this.abilities = abilities;
        this.quests = quests;
        this.titles = titles;
        this.onboarding = onboarding;
        this.homes = homes;
        this.trades = trades;
        this.wealth = wealth;
    }

    @Override
    public void enable() {
        // Every skill/class XP gain and title change updates its backing cache instantly, but
        // only a hand-picked set of call sites (quest/collection progress, class/ability selection,
        // prestige) ever explicitly call refresh(...) - this periodic sweep is the safety net that
        // makes every other stat (skill XP, class-synergy XP, admin edits, title equips) show up
        // live instead of only after a /panel toggle. Mirrors PlayerEnergyService's exact
        // once-per-player-per-tick pattern; refresh(...) is already a no-op for a hidden sidebar.
        refreshTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task ->
                Bukkit.getOnlinePlayers().forEach(player -> player.getScheduler().run(plugin, ignored -> refresh(player), null)),
                40L, 40L);
    }

    @Override
    public void disable() {
        visible.clear();
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    @Override
    public void initialize(Player player) {
        visible.put(player.getUniqueId(), true);
        Scoreboard board = newBoard();
        player.setScoreboard(board);
        render(board, player.getUniqueId());
    }

    @Override
    public void remove(Player player) {
        visible.remove(player.getUniqueId());
    }

    @Override
    public boolean toggle(Player player) {
        boolean next = !visible.getOrDefault(player.getUniqueId(), true);
        visible.put(player.getUniqueId(), next);
        if (next) {
            Scoreboard board = newBoard();
            player.setScoreboard(board);
            render(board, player.getUniqueId());
        } else {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager != null) {
                player.setScoreboard(manager.getMainScoreboard());
            }
        }
        return next;
    }

    @Override
    public void refresh(Player player) {
        if (!visible.getOrDefault(player.getUniqueId(), true)) {
            return;
        }
        Scoreboard board = player.getScoreboard();
        if (board == null || board.getObjective(OBJECTIVE_NAME) == null) {
            board = newBoard();
            player.setScoreboard(board);
        }
        render(board, player.getUniqueId());
    }

    private Scoreboard newBoard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            throw new IllegalStateException("ScoreboardManager is not available yet.");
        }
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, messages.render(MessageKey.SIDEBAR_TITLE));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        return board;
    }

    /** A distinct, invisible (legacy colour code only) scoreboard entry for line {@code index} - never shown, never a real player name. */
    private static String lineEntry(int index) {
        if (index < HEX.length()) {
            return "§" + HEX.charAt(index);
        }
        return "§" + HEX.charAt(index / HEX.length()) + "§" + HEX.charAt(index % HEX.length());
    }

    private void render(Scoreboard board, UUID playerId) {
        Objective objective = board.getObjective(OBJECTIVE_NAME);
        PlayerProfile profile = profiles.cached(playerId).orElse(null);
        if (objective == null || profile == null) {
            return;
        }

        List<Component> lines = trimToFit(activation.cached(playerId) ? activatedLines(playerId, profile)
                : List.of(messages.render(MessageKey.SIDEBAR_LINE_ACTIVATION_PROMPT), footer()));

        for (int i = 0; i < lines.size(); i++) {
            String entry = lineEntry(i);
            String teamName = TEAM_PREFIX + i;
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                team.addEntry(entry);
            }
            team.prefix(lines.get(i));
            objective.getScore(entry).setScore(lines.size() - i);
        }
        // A render pass can legitimately use fewer lines than a previous one -
        // any team beyond the current line count must be unregistered, not
        // left with stale text.
        for (int i = lines.size(); board.getTeam(TEAM_PREFIX + i) != null; i++) {
            board.getTeam(TEAM_PREFIX + i).unregister();
        }
    }

    private Component footer() {
        return messages.render(MessageKey.SIDEBAR_LINE_FOOTER_PANEL_HINT);
    }

    /**
     * The equipped title's tag is inserted between the "Oyuncu:" label and
     * the player's name (e.g. "Oyuncu: [EFSANE] Vesen"), not prepended before
     * the whole line - fixed from an earlier bug where {@code tag.append(base)}
     * put the tag first. The label is rendered from its own no-placeholder
     * {@code MessageKey} rather than reusing {@link MessageKey#SIDEBAR_LINE_NAME}'s
     * "Oyuncu: <name>" template, since the title tag's per-title dynamic
     * color (from {@link TitleChatStyle}) has to be built in Java either way -
     * the same documented exception §10.6 already established for chat tags.
     */
    private Component nameLine(UUID playerId, PlayerProfile profile) {
        Optional<String> titleName = titles.equippedDisplayName(playerId);
        if (titleName.isEmpty()) {
            return messages.render(MessageKey.SIDEBAR_LINE_NAME, Map.of("oyuncu", profile.playerName()));
        }
        TitleChatStyle style = titles.equippedStyle(playerId).orElse(TitleChatStyle.MAVI);
        Component tag = Component.text("[", NamedTextColor.GRAY)
                .append(Component.text(titleName.get()).color(style.color()).decoration(TextDecoration.BOLD, style.bold()))
                .append(Component.text("] ", NamedTextColor.GRAY));
        return messages.render(MessageKey.SIDEBAR_LINE_NAME_LABEL)
                .append(tag)
                .append(Component.text(profile.playerName(), NamedTextColor.WHITE));
    }

    private List<Component> activatedLines(UUID playerId, PlayerProfile profile) {
        List<Component> lines = new ArrayList<>();
        lines.add(nameLine(playerId, profile));
        // Güç and prestij share a line, as do class and class level. The panel
        // has fifteen lines to work with in total (see MAXIMUM_LINES) and these
        // four numbers are read together anyway, so pairing them buys back four
        // lines for the quest sections without hiding anything.
        lines.add(messages.render(MessageKey.SIDEBAR_LINE_POWER, Map.of(
                "seviye", Integer.toString(progression.cachedGlobalLevel(playerId)),
                "prestij", Integer.toString(profile.prestige())
        )));
        lines.add(messages.render(MessageKey.SIDEBAR_LINE_WEALTH, Map.of(
                "zenginlik", Long.toString(wealth.cached(playerId))
        )));

        String classId = profile.selectedClass();
        if (classId.isEmpty()) {
            lines.add(divider());
            lines.add(messages.render(MessageKey.SIDEBAR_LINE_CLASS_NONE));
            lines.add(messages.render(MessageKey.SIDEBAR_LINE_NO_CLASS_NUDGE));
            addOnboardingLines(lines, playerId);
            lines.add(divider());
            lines.add(footer());
            return lines;
        }

        lines.add(divider());
        String className = classes.displayName(classId);
        ClassLevelProgress classProgress = classes.classProgress(playerId, classId);
        long requiredForClassLevel = classProgress.level() >= classes.settings().classLevelCurve().maximumLevel()
                ? 0L
                : classes.settings().classLevelCurve().requiredForCurrentLevel(classProgress.level());
        lines.add(messages.render(MessageKey.SIDEBAR_LINE_CLASS_LEVEL, Map.of(
                "sinif", className,
                "seviye", Integer.toString(classProgress.level()),
                "xp", Long.toString(classProgress.experience()),
                "gereken", Long.toString(requiredForClassLevel)
        )));
        lines.add(abilityLine(playerId));

        addFeatureLines(lines, playerId);
        addOnboardingLines(lines, playerId);
        addQuestSections(lines, playerId);

        lines.add(divider());
        lines.add(footer());
        return lines;
    }

    /**
     * One line for whatever the player's abilities are currently doing: the
     * running one and its remaining time if there is one, otherwise which one
     * is armed and ready, otherwise a nudge to pick one.
     *
     * <p>This replaced a line per unlocked ability. Tabiat Vârisi owns eight of
     * them, which on its own overran the sidebar's fifteen-line ceiling before
     * a single quest was listed - so the list was already being silently
     * truncated by the client, and the ability screen shows the full set
     * anyway.</p>
     */
    private Component abilityLine(UUID playerId) {
        Optional<ActiveAbilityStatus> active = abilities.activeStatus(playerId);
        if (active.isPresent()) {
            Optional<String> name = abilities.definitions().stream()
                    .filter(definition -> definition.id().equals(active.get().abilityId()))
                    .map(definition -> definition.displayName())
                    .findFirst();
            if (name.isPresent()) {
                return messages.render(MessageKey.SIDEBAR_LINE_ACTIVE_ABILITY, Map.of(
                        "kabiliyet", name.get(),
                        "kalan", DurationFormatter.ofMillis(messages, active.get().expiresAtMillis() - System.currentTimeMillis())
                ));
            }
        }
        Optional<String> selectedName = abilities.selected(playerId)
                .flatMap(selectedId -> abilities.definitions().stream()
                        .filter(definition -> definition.id().equals(selectedId))
                        .map(definition -> definition.displayName())
                        .findFirst());
        return selectedName
                .map(name -> messages.render(MessageKey.SIDEBAR_LINE_ABILITY_SELECTED, Map.of("kabiliyet", name)))
                .orElseGet(() -> messages.render(MessageKey.SIDEBAR_LINE_ABILITY_NONE));
    }

    /**
     * The {@code /ev} and {@code /takas} state, and a live line while a trade is
     * open.
     *
     * <p>Shown only once at least one of the two is unlocked. A brand-new player
     * has neither, and telling them about two features they cannot use yet would
     * cost two of the panel's fifteen lines at exactly the moment the onboarding
     * section needs them most - by the time either unlocks (class level 15 or
     * 20), onboarding is long finished.</p>
     */
    private void addFeatureLines(List<Component> lines, UUID playerId) {
        boolean homeUnlocked = homes.isUnlocked(playerId);
        boolean tradeUnlocked = trades.isUnlocked(playerId);
        Optional<TradeSession> session = trades.activeSession(playerId);
        if (!homeUnlocked && !tradeUnlocked && session.isEmpty()) {
            return;
        }
        lines.add(divider());
        lines.add(messages.render(MessageKey.SIDEBAR_LINE_FEATURES, Map.of(
                "ev", homeStatus(playerId, homeUnlocked),
                "takas", messages.plain(tradeUnlocked
                        ? MessageKey.SIDEBAR_FEATURE_OPEN : MessageKey.SIDEBAR_FEATURE_LOCKED, Map.of())
        )));
        session.ifPresent(active -> {
            String partnerName = Bukkit.getOfflinePlayer(active.partnerOf(playerId)).getName();
            lines.add(messages.render(MessageKey.SIDEBAR_LINE_TRADE_ACTIVE, Map.of(
                    "oyuncu", partnerName == null ? "?" : partnerName
            )));
        });
    }

    /** An unlocked home reads as its own name once one is set, which is more useful than a tick. */
    private String homeStatus(UUID playerId, boolean unlocked) {
        if (!unlocked) {
            return messages.plain(MessageKey.SIDEBAR_FEATURE_LOCKED, Map.of());
        }
        return homes.cached(playerId)
                .map(home -> home.name())
                .orElseGet(() -> messages.plain(MessageKey.SIDEBAR_FEATURE_UNSET, Map.of()));
    }

    /** Daily, weekly and permanent each get their own labelled block, so they never read as one undifferentiated list. */
    private void addQuestSections(List<Component> lines, UUID playerId) {
        Map<String, QuestProgress> questProgress = quests.cachedAll(playerId);
        addQuestSection(lines, questProgress, playerId, QuestType.DAILY, MessageKey.SIDEBAR_LINE_QUESTS_DAILY_HEADER);
        addQuestSection(lines, questProgress, playerId, QuestType.WEEKLY, MessageKey.SIDEBAR_LINE_QUESTS_WEEKLY_HEADER);
        addQuestSection(lines, questProgress, playerId, QuestType.PERMANENT, MessageKey.SIDEBAR_LINE_QUESTS_PERMANENT_HEADER);
    }

    /** Purely cosmetic separator between sidebar sections - no service-layer meaning. */
    private Component divider() {
        return Component.text("――――――――――", NamedTextColor.DARK_GRAY).decoration(TextDecoration.STRIKETHROUGH, true);
    }

    /** Omitted entirely once every onboarding task is complete, per the "omitted not blank" convention. */
    private void addOnboardingLines(List<Component> lines, UUID playerId) {
        Set<String> completed = onboarding.cachedCompleted(playerId);
        List<MessageKey> remaining = new ArrayList<>();
        if (!completed.contains(OnboardingTask.ILK_MENU_ACMA.id())) {
            remaining.add(MessageKey.SIDEBAR_LINE_ONBOARDING_MENU);
        }
        if (!completed.contains(OnboardingTask.BECERILERI_INCELEME.id())) {
            remaining.add(MessageKey.SIDEBAR_LINE_ONBOARDING_SKILLS);
        }
        if (!completed.contains(OnboardingTask.KABILIYETLERI_INCELEME.id())) {
            remaining.add(MessageKey.SIDEBAR_LINE_ONBOARDING_ABILITIES);
        }
        if (!completed.contains(OnboardingTask.BECERI_KOMUTU_KULLANMA.id())) {
            remaining.add(MessageKey.SIDEBAR_LINE_ONBOARDING_SKILL_COMMAND);
        }
        if (remaining.isEmpty()) {
            return;
        }
        lines.add(divider());
        lines.add(messages.render(MessageKey.SIDEBAR_LINE_ONBOARDING_HEADER));
        remaining.forEach(key -> lines.add(messages.render(key)));
    }

    /**
     * A labelled block for one quest type, omitted entirely when it would be
     * empty. A finished quest switches to its own green "Tamamlandı" template
     * carrying the XP it paid out, instead of sitting at a maxed-out progress
     * fraction that looks identical to an unclaimed one.
     *
     * <p><strong>Pins decide what a section contains.</strong> The permanent
     * section exists only because of them - there are dozens of permanent
     * quests and nobody is "assigned" any, so listing them all was never an
     * option and listing none was the old behaviour. For daily and weekly the
     * pins act as a filter over the quests the period actually handed out:
     * pinning nothing keeps the old default of showing every assigned one, and
     * pinning some narrows the section to those. Pinning is deliberately not
     * allowed to <em>add</em> an unassigned daily, because an unassigned pool
     * quest cannot make progress at all - showing one would be a line that
     * never moves.</p>
     *
     * <p>Names come from each quest's {@code kisa-ad} rather than its full
     * title: a scoreboard line does not wrap, it truncates, and the full titles
     * are written to be read in a menu tooltip.</p>
     */
    private void addQuestSection(
            List<Component> lines, Map<String, QuestProgress> progress, UUID playerId, QuestType type, MessageKey header
    ) {
        Set<String> pinned = quests.cachedPins(playerId, type);
        boolean pinsOnly = type == QuestType.PERMANENT || !pinned.isEmpty();
        List<QuestDefinition> shown = quests.definitions().stream()
                .filter(definition -> definition.type() == type)
                .filter(definition -> progress.containsKey(definition.id()))
                .filter(definition -> !pinsOnly || pinned.contains(definition.id()))
                .toList();
        if (shown.isEmpty()) {
            return;
        }
        lines.add(divider());
        lines.add(messages.render(header));
        shown.forEach(definition -> {
            QuestProgress current = progress.get(definition.id());
            if (current.completed()) {
                lines.add(messages.render(MessageKey.SIDEBAR_LINE_QUEST_COMPLETED_ENTRY, Map.of(
                        "gorev", definition.shortName(),
                        "xp", Long.toString(definition.rewardExperience())
                )));
                return;
            }
            lines.add(messages.render(MessageKey.SIDEBAR_LINE_QUEST_DAILY_ENTRY, Map.of(
                    "gorev", definition.shortName(),
                    "ilerleme", current.progress() + "/" + definition.target(),
                    "xp", Long.toString(definition.rewardExperience())
            )));
        });
    }

    /**
     * Minecraft's sidebar renders at most {@link #MAXIMUM_LINES} entries and
     * silently drops the rest, so trimming is done here rather than being left
     * to the client. Lines are already ordered most-important-first, and the
     * footer is kept because it is the only thing telling a player how to turn
     * the panel off - so the overflow is taken from just above it, which sheds
     * weekly quests before daily ones and quests before anything else.
     */
    private List<Component> trimToFit(List<Component> lines) {
        if (lines.size() <= MAXIMUM_LINES) {
            return lines;
        }
        List<Component> trimmed = new ArrayList<>(lines.subList(0, MAXIMUM_LINES - 1));
        trimmed.add(lines.get(lines.size() - 1));
        return trimmed;
    }
}
