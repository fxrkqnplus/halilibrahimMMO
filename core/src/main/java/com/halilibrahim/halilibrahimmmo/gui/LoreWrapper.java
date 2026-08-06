package com.halilibrahim.halilibrahimmmo.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Breaks an over-long tooltip line into several, at word boundaries, keeping
 * every fragment's own colour and decoration.
 *
 * <p>Minecraft does not wrap item lore: a line simply keeps going to the right,
 * so a two-sentence description ends up as one thin ribbon running off the far
 * edge of the screen, and a player has to read it in a straight line while the
 * item itself is somewhere behind them. Every description this plugin writes -
 * a quest's, an ability's, a talent's, a handbook page's - is prose, so this is
 * applied centrally in {@link GuiItems} rather than at each of the several
 * dozen call sites that build lore.</p>
 *
 * <p><strong>Wrapping has to happen on the rendered component, not on the
 * template.</strong> A line is assembled from a MiniMessage template plus
 * unparsed placeholder values, so its final length is only known after
 * rendering, and by then it is a tree of styled fragments rather than a string.
 * The walk below flattens that tree into (text, style) runs - merging each
 * parent's style into its children the way the renderer itself does - refills
 * the runs into lines, and rebuilds one component per line. A fragment that
 * straddles a break is split, so a coloured word never loses its colour halfway
 * through.</p>
 */
final class LoreWrapper {

    /**
     * How many characters a lore line may carry before it is broken.
     *
     * <p>Chosen against the vanilla tooltip rather than against any font
     * measurement: at the default GUI scale a tooltip stays comfortably inside
     * the window at roughly this width, and Minecraft's own item descriptions
     * sit in the same range. It is a plain character count on purpose - the
     * server cannot know the client's GUI scale, resource pack or font, so a
     * pixel-accurate answer is not available and a slightly conservative
     * character count is the honest approximation.</p>
     */
    private static final int MAXIMUM_LINE_LENGTH = 42;

    private LoreWrapper() {
        throw new UnsupportedOperationException("LoreWrapper is a static utility class");
    }

    /** Every input line, in order, each replaced by however many lines it needed. */
    static List<Component> wrapAll(List<Component> lines) {
        List<Component> wrapped = new ArrayList<>(lines.size());
        for (Component line : lines) {
            wrapped.addAll(wrap(line));
        }
        return wrapped;
    }

    static List<Component> wrap(Component line) {
        List<Run> runs = new ArrayList<>();
        flatten(line, Style.empty(), runs);
        int length = runs.stream().mapToInt(run -> run.text().length()).sum();
        if (length <= MAXIMUM_LINE_LENGTH) {
            return List.of(line);
        }
        return rebuild(runs);
    }

    /**
     * Depth-first walk producing one run per styled text fragment. The parent's
     * style fills in only what the child leaves unset, which is exactly how
     * Adventure resolves inherited style at render time - so a run carries the
     * colour the player actually sees.
     *
     * <p><strong>The direction of this merge is the whole point and it was
     * backwards until Faz 24.</strong> {@code a.merge(b, IF_ABSENT_ON_TARGET)}
     * applies {@code b} onto {@code a} only where {@code a} has nothing, so
     * merging the child into the parent meant the parent won every property it
     * had set - a red phrase inside a grey sentence came back out grey the
     * moment the line was long enough to wrap. Merging the parent into the
     * child is the inheritance the renderer actually performs.</p>
     */
    private static void flatten(Component component, Style inherited, List<Run> runs) {
        Style resolved = component.style().merge(inherited, Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
        String own = component instanceof TextComponent text
                ? text.content()
                // Anything that is not plain text (a translatable, a score) has
                // no content of its own to read, so it is serialised in
                // isolation - with its children stripped, since the walk below
                // visits those separately and would otherwise duplicate them.
                : PlainTextComponentSerializer.plainText().serialize(component.children(List.of()));
        if (!own.isEmpty()) {
            runs.add(new Run(own, resolved));
        }
        for (Component child : component.children()) {
            flatten(child, resolved, runs);
        }
    }

    /**
     * Refills the runs into lines no longer than {@link #MAXIMUM_LINE_LENGTH}.
     *
     * <p><strong>The break points are chosen against the whole line, not against
     * one fragment of it.</strong> Faz 22 walked the runs one at a time and
     * looked for a space inside whichever run happened to overflow - so a line
     * built as "<em>Bekleme süresi:</em> <em>7 dakika 30 saniye</em>" could only
     * break inside the second fragment, and a fragment with no space of its own
     * was cut straight through the middle of a word. That is exactly the
     * "breaks in the wrong places" this fixes: the text is laid out as one
     * string, broken where a reader would break it, and the styled runs are then
     * sliced to match.</p>
     *
     * <p>A single word longer than the whole budget - a URL, a run of digits -
     * is still broken mid-word, because the alternative is the runaway line this
     * class exists to prevent.</p>
     */
    private static List<Component> rebuild(List<Run> runs) {
        StringBuilder joined = new StringBuilder();
        runs.forEach(run -> joined.append(run.text()));
        String text = joined.toString();

        List<Component> lines = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            start = skipSpaces(text, start);
            if (start >= text.length()) {
                break;
            }
            int end = lineEnd(text, start);
            lines.add(slice(runs, start, end));
            start = end;
        }
        return lines.isEmpty() ? List.of(Component.empty()) : lines;
    }

    /**
     * Where the line starting at {@code start} should end.
     *
     * <p>The last space that still leaves the line inside the budget, so the
     * break lands between words; a hard cut at the budget when the first word
     * alone is already too long.</p>
     */
    private static int lineEnd(String text, int start) {
        int limit = start + MAXIMUM_LINE_LENGTH;
        if (limit >= text.length()) {
            return text.length();
        }
        // Searching from limit rather than limit-1: a space sitting exactly on
        // the boundary is a legitimate break and costs nothing to take.
        int space = text.lastIndexOf(' ', limit);
        return space > start ? space : limit;
    }

    private static int skipSpaces(String text, int index) {
        int cursor = index;
        while (cursor < text.length() && text.charAt(cursor) == ' ') {
            cursor++;
        }
        return cursor;
    }

    /**
     * The half-open range {@code [start, end)} of the joined text, rebuilt as a
     * component with every fragment's own style intact - which is what keeps a
     * coloured word coloured when a break falls inside it.
     */
    private static Component slice(List<Run> runs, int start, int end) {
        List<Run> taken = new ArrayList<>();
        int cursor = 0;
        for (Run run : runs) {
            int runStart = cursor;
            int runEnd = cursor + run.text().length();
            cursor = runEnd;
            if (runEnd <= start || runStart >= end) {
                continue;
            }
            int from = Math.max(start, runStart) - runStart;
            int to = Math.min(end, runEnd) - runStart;
            if (to > from) {
                taken.add(new Run(run.text().substring(from, to), run.style()));
            }
        }
        return toComponent(taken);
    }

    private static Component toComponent(List<Run> runs) {
        Component line = Component.empty();
        for (Run run : runs) {
            line = line.append(Component.text(run.text()).style(run.style()));
        }
        return line;
    }

    private record Run(String text, Style style) {
    }
}
