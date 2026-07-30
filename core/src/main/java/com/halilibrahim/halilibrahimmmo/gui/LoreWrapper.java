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
     * style is merged into the child only where the child leaves it unset,
     * which is exactly how Adventure resolves inherited style at render time -
     * so a run carries the colour the player actually sees.
     */
    private static void flatten(Component component, Style inherited, List<Run> runs) {
        Style resolved = inherited.merge(component.style(), Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
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
     * <p>Breaks are taken at spaces. A single word longer than the whole line
     * budget - a URL, a run of digits - is broken mid-word instead, because the
     * alternative is the exact runaway line this class exists to prevent.</p>
     */
    private static List<Component> rebuild(List<Run> runs) {
        List<Component> lines = new ArrayList<>();
        List<Run> current = new ArrayList<>();
        int used = 0;
        for (Run run : runs) {
            String remaining = run.text();
            while (!remaining.isEmpty()) {
                int room = MAXIMUM_LINE_LENGTH - used;
                if (room <= 0) {
                    lines.add(toComponent(current));
                    current = new ArrayList<>();
                    used = 0;
                    remaining = stripLeadingSpace(remaining);
                    continue;
                }
                if (remaining.length() <= room) {
                    current.add(new Run(remaining, run.style()));
                    used += remaining.length();
                    break;
                }
                int cut = breakPoint(remaining, room);
                current.add(new Run(remaining.substring(0, cut), run.style()));
                lines.add(toComponent(current));
                current = new ArrayList<>();
                used = 0;
                remaining = stripLeadingSpace(remaining.substring(cut));
            }
        }
        if (!current.isEmpty()) {
            lines.add(toComponent(current));
        }
        return lines;
    }

    /** The last space at or before {@code room}, or a hard cut when the fragment holds no space at all. */
    private static int breakPoint(String text, int room) {
        int space = text.lastIndexOf(' ', room);
        return space > 0 ? space : room;
    }

    private static String stripLeadingSpace(String text) {
        int index = 0;
        while (index < text.length() && text.charAt(index) == ' ') {
            index++;
        }
        return text.substring(index);
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
