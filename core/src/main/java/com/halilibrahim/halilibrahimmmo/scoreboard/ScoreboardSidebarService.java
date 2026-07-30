package com.halilibrahim.halilibrahimmmo.scoreboard;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.abilities.ActiveAbilityStatus;
import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.backpack.BackpackService;
import com.halilibrahim.halilibrahimmmo.classes.ClassLevelProgress;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.home.HomeService;
import com.halilibrahim.halilibrahimmmo.locale.DurationFormatter;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.locale.NumberFormatter;
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
import java.util.EnumMap;
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
 *
 * <p><strong>Every line competes for the same fifteen slots</strong>
 * ({@link #MAXIMUM_LINES}), and a player who pins the maximum three quests in
 * each of the three sections is asking for nine of them on their own. That is
 * why dividers and the footer are marked {@link SidebarLine#decorative()}: when
 * the layout overflows, decoration is dropped before information is
 * ({@link #trimToFit}). It is also why Güç, prestij and Zenginlik share one
 * line and why the three quest sections share one header, each entry carrying
 * its type in its own colour instead - three headers and three dividers cost
 * six lines to say what three colours already say, and the permanent section
 * was routinely rendering its header with every one of its quests cut off
 * below it.</p>
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
    private final BackpackService backpacks;
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
            BackpackService backpacks,
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
        this.backpacks = backpacks;
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
                : List.of(information(messages.render(MessageKey.SIDEBAR_LINE_ACTIVATION_PROMPT)), footer()));

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

    /**
     * One rendered panel line and how readily it may be sacrificed when the
     * layout runs past {@link #MAXIMUM_LINES}.
     *
     * <p>A plain boolean was enough while the only expendable lines were
     * separators, but the quest block now carries a heading per type and those
     * are somewhere in between: worth more than a divider, worth less than the
     * quest under them. {@code shedOrder} orders that - the highest number goes
     * first, and {@link #KEEP} is never dropped while anything above it
     * remains.</p>
     */
    private record SidebarLine(Component text, int shedOrder) {
    }

    /** A line carrying something the player reads a value off. Never sacrificed. */
    private static final int KEEP = 0;
    /** A quest-type heading: goes only after every separator has already gone. */
    private static final int SHED_HEADING = 1;
    /** Separators and the {@code /panel} footer: nothing is lost by dropping them. */
    private static final int SHED_DECORATION = 2;

    private static SidebarLine information(Component text) {
        return new SidebarLine(text, KEEP);
    }

    private static SidebarLine heading(Component text) {
        return new SidebarLine(text, SHED_HEADING);
    }

    private static SidebarLine decoration(Component text) {
        return new SidebarLine(text, SHED_DECORATION);
    }

    private SidebarLine footer() {
        return decoration(messages.render(MessageKey.SIDEBAR_LINE_FOOTER_PANEL_HINT));
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

    private List<SidebarLine> activatedLines(UUID playerId, PlayerProfile profile) {
        List<SidebarLine> lines = new ArrayList<>();
        lines.add(information(nameLine(playerId, profile)));
        // Güç, prestij and Zenginlik share one line, as do class and class
        // level. All five are read together anyway, and every line they give
        // back is a pinned quest that fits.
        lines.add(information(messages.render(MessageKey.SIDEBAR_LINE_POWER, Map.of(
                "seviye", Integer.toString(progression.cachedGlobalLevel(playerId)),
                "prestij", Integer.toString(profile.prestige()),
                "zenginlik", NumberFormatter.compact(wealth.cached(playerId))
        ))));

        String classId = profile.selectedClass();
        if (classId.isEmpty()) {
            lines.add(decoration(divider()));
            lines.add(information(messages.render(MessageKey.SIDEBAR_LINE_CLASS_NONE)));
            lines.add(information(messages.render(MessageKey.SIDEBAR_LINE_NO_CLASS_NUDGE)));
            addOnboardingLines(lines, playerId);
            lines.add(footer());
            return lines;
        }

        lines.add(decoration(divider()));
        String className = classes.displayName(classId);
        ClassLevelProgress classProgress = classes.classProgress(playerId, classId);
        long requiredForClassLevel = classProgress.level() >= classes.settings().classLevelCurve().maximumLevel()
                ? 0L
                : classes.settings().classLevelCurve().requiredForCurrentLevel(classProgress.level());
        lines.add(information(messages.render(MessageKey.SIDEBAR_LINE_CLASS_LEVEL, Map.of(
                "sinif", className,
                "seviye", Integer.toString(classProgress.level()),
                "xp", Long.toString(classProgress.experience()),
                "gereken", Long.toString(requiredForClassLevel)
        ))));
        lines.add(information(abilityLine(playerId)));

        addFeatureLines(lines, playerId);
        addOnboardingLines(lines, playerId);
        addQuestSection(lines, playerId);

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
     * The {@code /ev}, {@code /takas} and {@code /çanta} state, and a live line
     * while a trade is open.
     *
     * <p>Shown only once at least one of the three is unlocked. A brand-new
     * player has none of them, and telling them about features they cannot use
     * yet would cost one of the panel's fifteen lines at exactly the moment the
     * onboarding section needs them most - by the time any of them unlocks
     * (class level 15, 20 or 30), onboarding is long finished.</p>
     *
     * <p>All three share a single line rather than taking one each, for the same
     * reason Güç, prestij and Zenginlik do: each is one word of state, and every
     * line they give back is a pinned quest that fits.</p>
     */
    private void addFeatureLines(List<SidebarLine> lines, UUID playerId) {
        boolean homeUnlocked = homes.isUnlocked(playerId);
        boolean tradeUnlocked = trades.isUnlocked(playerId);
        boolean backpackUnlocked = backpacks.isUnlocked(playerId);
        Optional<TradeSession> session = trades.activeSession(playerId);
        if (!homeUnlocked && !tradeUnlocked && !backpackUnlocked && session.isEmpty()) {
            return;
        }
        lines.add(information(messages.render(MessageKey.SIDEBAR_LINE_FEATURES, Map.of(
                "ev", homeStatus(playerId, homeUnlocked),
                "takas", messages.plain(tradeUnlocked
                        ? MessageKey.SIDEBAR_FEATURE_OPEN : MessageKey.SIDEBAR_FEATURE_LOCKED, Map.of()),
                "canta", messages.plain(backpackUnlocked
                        ? MessageKey.SIDEBAR_FEATURE_OPEN : MessageKey.SIDEBAR_FEATURE_LOCKED, Map.of())
        ))));
        session.ifPresent(active -> {
            String partnerName = Bukkit.getOfflinePlayer(active.partnerOf(playerId)).getName();
            lines.add(information(messages.render(MessageKey.SIDEBAR_LINE_TRADE_ACTIVE, Map.of(
                    "oyuncu", partnerName == null ? "?" : partnerName
            ))));
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

    /** Purely cosmetic separator between sidebar sections - no service-layer meaning. */
    private Component divider() {
        return Component.text("――――――――――", NamedTextColor.DARK_GRAY).decoration(TextDecoration.STRIKETHROUGH, true);
    }

    /** Omitted entirely once every onboarding task is complete, per the "omitted not blank" convention. */
    private void addOnboardingLines(List<SidebarLine> lines, UUID playerId) {
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
        lines.add(decoration(divider()));
        lines.add(information(messages.render(MessageKey.SIDEBAR_LINE_ONBOARDING_HEADER)));
        remaining.forEach(key -> lines.add(information(messages.render(key))));
    }

    /**
     * One labelled quest block covering all three types, omitted entirely when
     * it would be empty.
     *
     * <p><strong>Pins decide what it contains, up to three per type.</strong>
     * Permanent quests appear only when pinned - there are dozens of them and
     * nobody is "assigned" any, so listing them all was never an option. For
     * daily and weekly the pins act as a filter over the quests the period
     * actually handed out: pinning nothing keeps the default of showing every
     * assigned one, and pinning some narrows the block to those. Pinning is
     * deliberately not allowed to <em>add</em> an unassigned daily, because an
     * unassigned pool quest cannot make progress at all - showing one would be
     * a line that never moves.</p>
     *
     * <p><strong>Each type gets its own heading again.</strong> Phase 20 had
     * collapsed all three under one label and let the entry's own colour say
     * which was which, purely to buy back the six lines three headed sections
     * cost - and it did fit, but a player reading a colour to work out whether
     * something resets tomorrow is reading the wrong thing. The headings are
     * back, and the line budget is answered where it should have been: they are
     * marked {@link #SHED_HEADING}, so a panel too full for them loses a
     * heading rather than a quest, one at a time from the bottom, and only
     * after every separator has already gone.</p>
     *
     * <p>Names come from each quest's {@code kisa-ad} rather than its full
     * title: a scoreboard line does not wrap, it truncates, and the full titles
     * are written to be read in a menu tooltip.</p>
     */
    private void addQuestSection(List<SidebarLine> lines, UUID playerId) {
        Map<String, QuestProgress> progress = quests.cachedAll(playerId);
        Set<String> dismissed = quests.cachedDismissals(playerId);
        Map<QuestType, List<QuestDefinition>> shown = new EnumMap<>(QuestType.class);
        for (QuestType type : QuestType.values()) {
            Set<String> pinned = quests.cachedPins(playerId, type);
            boolean pinsOnly = type == QuestType.PERMANENT || !pinned.isEmpty();
            List<QuestDefinition> ofType = quests.definitions().stream()
                    .filter(definition -> definition.type() == type)
                    .filter(definition -> progress.containsKey(definition.id()))
                    .filter(definition -> !pinsOnly || pinned.contains(definition.id()))
                    // A finished quest the player took off the panel stays off
                    // it. The dismissal is only honoured while the quest is
                    // still finished, so a daily that rolls over into a new
                    // period comes back as the fresh quest it now is.
                    .filter(definition -> !dismissed.contains(definition.id())
                            || !progress.get(definition.id()).completed())
                    .toList();
            if (!ofType.isEmpty()) {
                shown.put(type, ofType);
            }
        }
        if (shown.isEmpty()) {
            return;
        }
        lines.add(decoration(divider()));
        for (Map.Entry<QuestType, List<QuestDefinition>> section : shown.entrySet()) {
            lines.add(heading(messages.render(questHeaderKey(section.getKey()))));
            for (QuestDefinition definition : section.getValue()) {
                QuestProgress current = progress.get(definition.id());
                lines.add(information(messages.render(questEntryKey(definition.type(), current.completed()), Map.of(
                        "gorev", definition.shortName(),
                        "ilerleme", current.progress() + "/" + definition.target(),
                        "xp", Long.toString(definition.rewardExperience())
                ))));
            }
        }
    }

    /**
     * One fully-authored heading per type rather than a shared template fed a
     * Turkish word from Java - the same discipline every other per-shape label
     * in this codebase follows, and it is also what keeps each heading the same
     * colour as the entries beneath it.
     */
    private MessageKey questHeaderKey(QuestType type) {
        return switch (type) {
            case DAILY -> MessageKey.SIDEBAR_LINE_QUESTS_DAILY_HEADER;
            case WEEKLY -> MessageKey.SIDEBAR_LINE_QUESTS_WEEKLY_HEADER;
            case PERMANENT -> MessageKey.SIDEBAR_LINE_QUESTS_PERMANENT_HEADER;
        };
    }

    /**
     * A finished quest gets its own template carrying "Tamamlandı" rather than
     * sitting at a maxed-out fraction indistinguishable from an unclaimed one.
     */
    private MessageKey questEntryKey(QuestType type, boolean completed) {
        return switch (type) {
            case DAILY -> completed ? MessageKey.SIDEBAR_LINE_QUEST_DAILY_DONE : MessageKey.SIDEBAR_LINE_QUEST_DAILY;
            case WEEKLY -> completed ? MessageKey.SIDEBAR_LINE_QUEST_WEEKLY_DONE : MessageKey.SIDEBAR_LINE_QUEST_WEEKLY;
            case PERMANENT -> completed
                    ? MessageKey.SIDEBAR_LINE_QUEST_PERMANENT_DONE : MessageKey.SIDEBAR_LINE_QUEST_PERMANENT;
        };
    }

    /**
     * Minecraft's sidebar renders at most {@link #MAXIMUM_LINES} entries and
     * silently drops the rest, so trimming is done here rather than being left
     * to the client - the client's own truncation is what used to leave the
     * permanent quest header standing over an empty space.
     *
     * <p>Lines are given up in order of how little they carry, from the bottom
     * up: first separators and the {@code /panel} footer, then quest headings,
     * and only when neither is left does anything a player reads a value off
     * get dropped. So a panel full of pinned quests loses its separators, then
     * at worst the last heading - never a quest while a divider is still
     * standing. The final cut is from the end because the list is built
     * most-important-first.</p>
     */
    private List<Component> trimToFit(List<SidebarLine> lines) {
        List<SidebarLine> remaining = new ArrayList<>(lines);
        for (int order = SHED_DECORATION; order > KEEP && remaining.size() > MAXIMUM_LINES; order--) {
            for (int i = remaining.size() - 1; i >= 0 && remaining.size() > MAXIMUM_LINES; i--) {
                if (remaining.get(i).shedOrder() == order) {
                    remaining.remove(i);
                }
            }
        }
        return remaining.stream()
                .limit(MAXIMUM_LINES)
                .map(SidebarLine::text)
                .toList();
    }
}
