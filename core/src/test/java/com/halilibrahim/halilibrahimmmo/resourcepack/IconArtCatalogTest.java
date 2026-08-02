package com.halilibrahim.halilibrahimmmo.resourcepack;

import com.halilibrahim.halilibrahimmmo.locale.IconPack;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binds {@link IconPack} to the resource pack generator's mask file the same
 * way {@code MessageCatalogTest} binds {@code MessageKey} to
 * {@code messages.yml}: exact 1:1, no icon without art and no art without an
 * icon.
 *
 * <p>Worth failing the build over, because neither half fails loudly on its
 * own. An icon with no art silently falls back to Unifont on a client that
 * <em>has</em> the pack - it just looks wrong, and only in game. Art for an id
 * that no longer exists is dead weight nobody would ever notice.</p>
 *
 * <p>The generator's inputs live in {@code tools/}, outside this Gradle module,
 * and are read here by relative path. That is deliberate: duplicating the mask
 * file into {@code src/test/resources} would create a second copy that can
 * disagree with the one the generator actually reads, which is precisely the
 * class of bug this test exists to prevent. A missing file <strong>fails</strong>
 * rather than skipping - a silently skipped test is worse than no test.</p>
 */
final class IconArtCatalogTest {

    private static final int GLYPH_SIZE = 8;
    private static final Pattern ENTRY = Pattern.compile(
            "\"([a-z0-9_-]+)\"\\s*:\\s*\\{(.*?)\\n    }", Pattern.DOTALL);
    private static final Pattern MASK_ROW = Pattern.compile("\"([^\"]*)\"");

    /** Gradle runs tests with the module directory as the working directory. */
    private static Path generatorDir() {
        Path relative = Path.of("..", "tools", "kaynak-paketi");
        assertTrue(Files.isDirectory(relative),
                "Generator inputs not found at " + relative.toAbsolutePath()
                        + ". This test reads tools/kaynak-paketi directly on purpose - see the class javadoc.");
        return relative;
    }

    private static String read(String name) throws IOException {
        Path file = generatorDir().resolve(name);
        assertTrue(Files.isRegularFile(file), "Missing generator input: " + file);
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    /** @return icon id to its raw JSON body, skipping {@code _}-prefixed documentation keys. */
    private static Map<String, String> icons() throws IOException {
        String json = read("simgeler.json");
        int at = json.indexOf("\"simgeler\"");
        assertTrue(at > 0, "simgeler.json is missing the 'simgeler' object.");
        // Start inside the wrapper object, otherwise "simgeler" itself matches
        // as the first entry and swallows the first real icon's body with it.
        int open = json.indexOf('{', at);
        assertTrue(open > at, "simgeler.json's 'simgeler' object is not opened.");

        Map<String, String> out = new LinkedHashMap<>();
        Matcher matcher = ENTRY.matcher(json.substring(open + 1));
        while (matcher.find()) {
            out.put(matcher.group(1), matcher.group(2));
        }
        return out;
    }

    private static Set<String> paletteKeys() throws IOException {
        String json = read("palet.json");
        int start = json.indexOf("\"renkler\"");
        int end = json.indexOf("\"gradyanlar\"");
        assertTrue(start > 0 && end > start, "palet.json is missing 'renkler' or 'gradyanlar'.");

        Set<String> keys = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"#").matcher(json.substring(start, end));
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    private static Set<String> gradientNames() throws IOException {
        String json = read("palet.json");
        int start = json.indexOf("\"gradyanlar\"");
        assertTrue(start > 0, "palet.json is missing 'gradyanlar'.");

        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\"([a-z]+)\"\\s*:\\s*\\{\\s*\"baslangic\"").matcher(json.substring(start));
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    @Test
    void everyIconPackEntryHasArtAndViceVersa() throws IOException {
        Set<String> declared = new TreeSet<>();
        for (IconPack icon : IconPack.values()) {
            declared.add(icon.id());
        }
        assertEquals(declared, new TreeSet<>(icons().keySet()),
                "IconPack and tools/kaynak-paketi/simgeler.json must match exactly - "
                        + "no icon without art, no art without an icon");
    }

    /**
     * Every mask draws the glyph at the codepoint {@link IconPack} already uses.
     * Changing one would break both halves of the design at once: the ~332
     * existing {@code <simge:...>} uses would render vanilla art, and a client
     * without the pack would show a box instead of today's glyph.
     */
    @Test
    void everyMaskTargetsTheCodepointIconPackAlreadyUses() throws IOException {
        Map<String, String> icons = icons();
        List<String> wrong = new ArrayList<>();
        for (IconPack icon : IconPack.values()) {
            String body = icons.get(icon.id());
            if (body == null) {
                continue;
            }
            Matcher matcher = Pattern.compile("\"kodnoktasi\"\\s*:\\s*\"U\\+([0-9A-F]+)\"").matcher(body);
            assertTrue(matcher.find(), icon.id() + ": missing 'kodnoktasi'.");
            int declared = Integer.parseInt(matcher.group(1), 16);
            int actual = icon.glyph().codePointAt(0);
            if (declared != actual) {
                wrong.add("%s: art says U+%04X, IconPack says U+%04X".formatted(icon.id(), declared, actual));
            }
        }
        assertTrue(wrong.isEmpty(), "Codepoint mismatch between IconPack and the generator masks: " + wrong);
    }

    @Test
    void everyMaskIsExactlyEightByEight() throws IOException {
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, String> entry : icons().entrySet()) {
            List<String> rows = maskRows(entry.getValue());
            if (rows.size() != GLYPH_SIZE) {
                wrong.add(entry.getKey() + ": " + rows.size() + " rows");
                continue;
            }
            for (String row : rows) {
                if (row.length() != GLYPH_SIZE) {
                    wrong.add(entry.getKey() + ": row '" + row + "' is " + row.length() + " wide");
                }
            }
        }
        assertTrue(wrong.isEmpty(), "Masks must be exactly " + GLYPH_SIZE + "x" + GLYPH_SIZE + ": " + wrong);
    }

    /**
     * The "one cell rule" in test form: ink must not reach the last column, so a
     * glyph's advance stays at one character cell and {@code LoreWrapper}'s
     * 42-character budget keeps meaning what it means today.
     */
    @Test
    void noMaskInksTheLastColumn() throws IOException {
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, String> entry : icons().entrySet()) {
            for (String row : maskRows(entry.getValue())) {
                if (row.length() == GLYPH_SIZE && row.charAt(GLYPH_SIZE - 1) != '.') {
                    wrong.add(entry.getKey());
                    break;
                }
            }
        }
        assertTrue(wrong.isEmpty(),
                "These icons ink the final column, which widens the glyph past one character cell "
                        + "and silently invalidates LoreWrapper.MAXIMUM_LINE_LENGTH: " + wrong);
    }

    @Test
    void everyMaskCharacterIsAPaletteKeyOrTheGradientMarker() throws IOException {
        Set<String> palette = paletteKeys();
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, String> entry : icons().entrySet()) {
            for (String row : maskRows(entry.getValue())) {
                for (char key : row.toCharArray()) {
                    if (key != 'G' && !palette.contains(String.valueOf(key))) {
                        wrong.add(entry.getKey() + " -> '" + key + "'");
                    }
                }
            }
        }
        assertTrue(wrong.isEmpty(), "Mask characters with no palet.json entry: " + wrong);
    }

    /** A 'G' pixel is painted from a named gradient, so the name must resolve. */
    @Test
    void gradientMarkersHaveAMatchingGradient() throws IOException {
        Set<String> gradients = gradientNames();
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, String> entry : icons().entrySet()) {
            boolean usesMarker = maskRows(entry.getValue()).stream().anyMatch(row -> row.indexOf('G') >= 0);
            Matcher matcher = Pattern.compile("\"gradyan\"\\s*:\\s*\"([a-z]+)\"").matcher(entry.getValue());
            String named = matcher.find() ? matcher.group(1) : null;

            if (usesMarker && named == null) {
                wrong.add(entry.getKey() + ": mask uses 'G' but declares no gradient");
            } else if (!usesMarker && named != null) {
                wrong.add(entry.getKey() + ": declares gradient '" + named + "' but no 'G' pixel uses it");
            } else if (named != null && !gradients.contains(named)) {
                wrong.add(entry.getKey() + ": unknown gradient '" + named + "'");
            }
        }
        assertTrue(wrong.isEmpty(), "Gradient problems: " + wrong);
    }

    /**
     * Every {@code model:} an owner can write in a bundled YAML must exist in
     * the generator's manifest.
     *
     * <p>This is the only place a typo can be caught. A wrong model key shows
     * the missing-model cube in game and logs absolutely nothing server-side,
     * so without this test the failure mode is "an icon looks broken and nobody
     * knows why".</p>
     */
    @Test
    void everyModelReferencedByAShippedYamlExists() throws IOException {
        Set<String> known = new TreeSet<>(models().keySet());
        Path resources = Path.of("src", "main", "resources");

        List<String> unknown = new ArrayList<>();
        for (String file : List.of("skills.yml", "classes.yml", "abilities.yml",
                "quests.yml", "rehber.yml", "titles.yml", "cosmetics.yml")) {
            Path yaml = resources.resolve(file);
            assertTrue(Files.isRegularFile(yaml), "Missing bundled resource: " + yaml);
            Matcher matcher = Pattern.compile("^\\s+model:\\s*(\\S+)\\s*$", Pattern.MULTILINE)
                    .matcher(Files.readString(yaml, StandardCharsets.UTF_8));
            while (matcher.find()) {
                if (!known.contains(matcher.group(1))) {
                    unknown.add(file + " -> " + matcher.group(1));
                }
            }
        }
        assertTrue(unknown.isEmpty(),
                "These YAML 'model:' values have no entry in tools/kaynak-paketi/esya-modelleri.json, "
                        + "so they would render as the missing-model cube with nothing in the log: " + unknown);
    }

    /** Every authored texture must name an emblem the icon masks actually define. */
    @Test
    void everyModelEmblemIsAKnownIconMask() throws IOException {
        Set<String> masks = icons().keySet();
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, String> entry : models().entrySet()) {
            Matcher matcher = Pattern.compile("\"simge\"\\s*:\\s*\"([a-z0-9_-]+)\"").matcher(entry.getValue());
            if (!matcher.find()) {
                wrong.add(entry.getKey() + ": no 'simge'");
            } else if (!masks.contains(matcher.group(1))) {
                wrong.add(entry.getKey() + " -> " + matcher.group(1));
            }
        }
        assertTrue(wrong.isEmpty(), "Item models pointing at an unknown icon mask: " + wrong);
    }

    /** @return model id to its raw JSON body. */
    private static Map<String, String> models() throws IOException {
        String json = read("esya-modelleri.json");
        int at = json.indexOf("\"modeller\"");
        assertTrue(at > 0, "esya-modelleri.json is missing the 'modeller' object.");
        int open = json.indexOf('{', at);

        Map<String, String> out = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("\"([a-z0-9-]+)\"\\s*:\\s*\\{([^}]*)}").matcher(json.substring(open + 1));
        while (matcher.find()) {
            out.put(matcher.group(1), matcher.group(2));
        }
        return out;
    }

    private static List<String> maskRows(String body) {
        int at = body.indexOf("\"maske\"");
        assertTrue(at > 0, "Icon entry is missing 'maske': " + body);
        int open = body.indexOf('[', at);
        int close = body.indexOf(']', open);

        List<String> rows = new ArrayList<>();
        Matcher matcher = MASK_ROW.matcher(body.substring(open, close));
        while (matcher.find()) {
            rows.add(matcher.group(1));
        }
        return rows;
    }
}
