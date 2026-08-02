///usr/bin/env java --source 26 "$0" "$@" ; exit $?

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Builds the HalilIbrahimMMO server resource pack from the palette and mask
 * files next to this source, and prints the {@code adres}/{@code sha1} lines
 * to paste into {@code kaynak-paketi.yml}.
 *
 * <p>Run with the JDK's single-file source launcher - it is never compiled by
 * either Gradle chain and adds no dependency to either:</p>
 * <pre>
 *   java tools\kaynak-paketi\PaketUretici.java --surum 1.0.0
 * </pre>
 *
 * <p><strong>Determinism is the point.</strong> The SHA-1 this prints is pinned
 * in a live server's config, so identical inputs must produce a byte-identical
 * zip: entries are written in sorted order, every timestamp is zeroed and the
 * deflate level is fixed. {@code Compress-Archive} cannot do this - it embeds
 * the current time and yields a different digest on every run.</p>
 *
 * <p><strong>Vanilla assets are read from the real client jar, never guessed.</strong>
 * {@code assets/minecraft/font/default.json} is derived from the installed 26.2
 * jar and our provider appended, so a future Mojang change to that list cannot
 * be silently reverted by a stale copy. The generator refuses to run if the
 * jar's declared {@code resource_major} is not the format this pack targets.</p>
 */
public final class PaketUretici {

    /** Verified against the real 26.2 client jar's version.json (resource_major). */
    private static final int PACK_FORMAT = 88;

    private static final String NAMESPACE = "hmmo";
    private static final int ICON_SHEET_COLUMNS = 8;
    private static final int GLYPH_SIZE = 8;

    /** Must stay in step with {@code resourcepack.ItemModels.PREFIX} on the server side. */
    private static final String PREFIX = "simge/";

    private static Path root;

    public static void main(String[] args) throws Exception {
        root = Path.of(PaketUretici.class.getProtectionDomain().getCodeSource() == null
                ? "tools/kaynak-paketi" : "tools/kaynak-paketi");
        if (!Files.isDirectory(root)) {
            // Launched from inside the tools directory rather than the repo root.
            root = Path.of(".");
        }

        String mode = args.length > 0 ? args[0] : "--yardim";
        switch (mode) {
            case "--referans-cikar" -> extractReference(clientJar(args));
            case "--surum" -> build(argAfter(args, "--surum", "1.0.0"));
            case "--dogrula" -> verify(argAfter(args, "--surum", "1.0.0"));
            default -> usage();
        }
    }

    private static void usage() {
        System.out.println("""
                HalilIbrahimMMO kaynak paketi ureticisi

                  --referans-cikar [jar yolu]  Gercek 26.2 istemci jar'indan vanilla
                                               varliklari referans/ altina cikarir.
                                               Once bunu calistirin.
                  --surum <x.y.z>              Paketi uretir, cikti/ altina yazar ve
                                               kaynak-paketi.yml icin adres+sha1 basar.
                  --dogrula --surum <x.y.z>    Yeniden uretip SHA-1'in degismedigini
                                               dogrular (belirlenimcilik testi).
                """);
    }

    // ---------------------------------------------------------------- reference

    private static Path clientJar(String[] args) {
        for (int i = 1; i < args.length; i++) {
            if (!args[i].startsWith("--")) {
                return Path.of(args[i]);
            }
        }
        return Path.of(System.getenv("APPDATA"), ".minecraft", "versions", "26.2", "26.2.jar");
    }

    /**
     * Copies the handful of vanilla files this pack must not guess at. Nothing
     * extracted here is ever committed - {@code referans/} is gitignored,
     * because these are Mojang's copyrighted assets.
     */
    private static void extractReference(Path jar) throws IOException {
        if (!Files.isRegularFile(jar)) {
            throw new IOException("Istemci jar'i bulunamadi: " + jar
                    + "\nOyunu bir kez 26.2 ile calistirin ya da jar yolunu argüman olarak verin.");
        }
        Path out = root.resolve("referans");
        Files.createDirectories(out);

        List<String> wanted = List.of(
                "version.json",
                "assets/minecraft/font/default.json",
                "assets/minecraft/font/include/default.json",
                "assets/minecraft/font/include/space.json",
                "assets/minecraft/font/include/unifont.json"
        );

        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (String name : wanted) {
                ZipEntry entry = zip.getEntry(name);
                if (entry == null) {
                    System.out.println("  ! bulunamadi, atlandi: " + name);
                    continue;
                }
                Path target = out.resolve(name.replace('/', '_'));
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.write(target, in.readAllBytes());
                }
                System.out.println("  cikarildi: " + name);
            }
        }

        int major = declaredPackFormat(out.resolve("version.json"));
        System.out.println();
        System.out.println("Istemci jar'i resource_major = " + major + " bildiriyor.");
        if (major != PACK_FORMAT) {
            throw new IOException("Bu uretici pack format " + PACK_FORMAT + " icin yazildi ama kurulu istemci "
                    + major + " bildiriyor.\nPaketi bu haliyle uretmeyin: PaketUretici.PACK_FORMAT'i guncelleyip"
                    + " font/sprite yollarini yeniden dogrulayin.");
        }
        System.out.println("Beklenen formatla ortusuyor. Simdi: --surum <x.y.z>");
    }

    private static int declaredPackFormat(Path versionJson) throws IOException {
        String text = Files.readString(versionJson, StandardCharsets.UTF_8);
        int at = text.indexOf("\"resource_major\"");
        if (at < 0) {
            throw new IOException("version.json icinde resource_major yok.");
        }
        int colon = text.indexOf(':', at);
        int end = colon + 1;
        while (end < text.length() && !Character.isDigit(text.charAt(end))) {
            end++;
        }
        int start = end;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        return Integer.parseInt(text.substring(start, end));
    }

    // -------------------------------------------------------------------- build

    private static void build(String version) throws Exception {
        Palette palette = Palette.load(root.resolve("palet.json"));
        Map<String, byte[]> files = new TreeMap<>();

        files.put("pack.mcmeta", packMeta().getBytes(StandardCharsets.UTF_8));
        files.put("pack.png", packIcon(palette));
        files.put("assets/minecraft/font/default.json", defaultFont().getBytes(StandardCharsets.UTF_8));

        IconSheet icons = IconSheet.build(root.resolve("simgeler.json"), palette);
        files.put("assets/" + NAMESPACE + "/textures/font/simgeler.png", icons.png());
        files.put("assets/" + NAMESPACE + "/font/simgeler.json", icons.fontJson().getBytes(StandardCharsets.UTF_8));

        Panel.build(palette, files);
        ItemTextures.build(root, palette, files);

        Path out = root.resolve("cikti");
        Files.createDirectories(out);
        String zipName = "hmmo-paket-" + version + ".zip";
        Path zipPath = out.resolve(zipName);
        byte[] zip = deterministicZip(files);
        Files.write(zipPath, zip);

        String sha1 = sha1Hex(zip);
        Files.writeString(out.resolve("ozet.txt"), version + " " + sha1 + " " + zip.length + "\n",
                StandardCharsets.UTF_8);

        System.out.println();
        System.out.println(out.resolve(zipName));
        System.out.printf(Locale.ROOT, "  dosya : %d adet%n", files.size());
        System.out.printf(Locale.ROOT, "  boyut : %,d bayt (250 MB sinirinin %%%.3f'u)%n",
                zip.length, zip.length * 100.0 / (250L * 1024 * 1024));
        System.out.println("  sha1  : " + sha1);
        System.out.println();
        System.out.println("  --- kaynak-paketi.yml icin ---");
        System.out.println("  adres: \"https://kaynak.fxrkqn.org/" + zipName + "\"");
        System.out.println("  sha1: \"" + sha1 + "\"");
        System.out.println();
        System.out.println("  Once zip'i yukleyin, indigini ve ozetin tuttugunu dogrulayin,");
        System.out.println("  ANCAK SONRA bu iki satiri yazip etkin: true yapin.");
    }

    private static void verify(String version) throws Exception {
        Path summary = root.resolve("cikti").resolve("ozet.txt");
        if (!Files.isRegularFile(summary)) {
            throw new IOException("cikti/ozet.txt yok - once --surum ile uretin.");
        }
        String[] previous = Files.readString(summary, StandardCharsets.UTF_8).trim().split("\\s+");

        Palette palette = Palette.load(root.resolve("palet.json"));
        Map<String, byte[]> files = new TreeMap<>();
        files.put("pack.mcmeta", packMeta().getBytes(StandardCharsets.UTF_8));
        files.put("pack.png", packIcon(palette));
        files.put("assets/minecraft/font/default.json", defaultFont().getBytes(StandardCharsets.UTF_8));
        IconSheet icons = IconSheet.build(root.resolve("simgeler.json"), palette);
        files.put("assets/" + NAMESPACE + "/textures/font/simgeler.png", icons.png());
        files.put("assets/" + NAMESPACE + "/font/simgeler.json", icons.fontJson().getBytes(StandardCharsets.UTF_8));
        Panel.build(palette, files);
        ItemTextures.build(root, palette, files);

        String sha1 = sha1Hex(deterministicZip(files));
        if (sha1.equals(previous[1])) {
            System.out.println("Belirlenimci: SHA-1 degismedi (" + sha1 + ").");
        } else {
            System.out.println("DEGISTI!");
            System.out.println("  onceki : " + previous[1]);
            System.out.println("  simdi  : " + sha1);
            System.out.println("Girdiler degistiyse bu beklenen bir sonuctur; zip'i yeniden yukleyip");
            System.out.println("kaynak-paketi.yml'deki sha1'i guncelleyin.");
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ content

    /**
     * Since 1.21.9 the pack format is declared as a {@code min_format}/
     * {@code max_format} pair rather than a single {@code pack_format} integer.
     */
    private static String packMeta() {
        return """
                {
                  "pack": {
                    "description": "HalilIbrahim Sofrasi - sunucu kaynak paketi",
                    "min_format": %d,
                    "max_format": %d
                  }
                }
                """.formatted(PACK_FORMAT, PACK_FORMAT);
    }

    /**
     * Vanilla's own three providers, reproduced exactly, plus ours appended.
     *
     * <p>The order is load-bearing and the opposite of the intuitive reading:
     * {@code FontManager} reverses this list before installing it and
     * {@code FontSet#computeGlyphInfo} returns the first provider that yields a
     * glyph, so <strong>the provider written last wins</strong>. Ours must stay
     * at the bottom.</p>
     */
    private static String defaultFont() {
        return """
                {
                  "providers": [
                    { "type": "reference", "id": "minecraft:include/space" },
                    { "type": "reference", "id": "minecraft:include/default", "filter": { "uniform": false } },
                    { "type": "reference", "id": "minecraft:include/unifont" },
                    { "type": "reference", "id": "%s:simgeler" }
                  ]
                }
                """.formatted(NAMESPACE);
    }

    /** The 64x64 mark shown next to the pack in the client's pack list. */
    private static byte[] packIcon(Palette palette) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int ground = palette.color("z");
        int start = palette.gradientStart("kimlik");
        int end = palette.gradientEnd("kimlik");
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                image.setRGB(x, y, ground);
            }
        }
        // The same four-pointed mark the sidebar uses, scaled up 6x and centred.
        String[] mask = {
                "...G....", "...G....", "..GGG...", "GGGGGGG.",
                "GGGGGGG.", "..GGG...", "...G....", "...G...."
        };
        int scale = 6;
        int offset = (64 - GLYPH_SIZE * scale) / 2;
        for (int y = 0; y < GLYPH_SIZE; y++) {
            for (int x = 0; x < GLYPH_SIZE; x++) {
                if (mask[y].charAt(x) != 'G') {
                    continue;
                }
                int argb = lerp(start, end, GLYPH_SIZE <= 1 ? 0.0 : x / (double) (GLYPH_SIZE - 1));
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        image.setRGB(offset + x * scale + dx, offset + y * scale + dy, argb);
                    }
                }
            }
        }
        return png(image);
    }

    // ------------------------------------------------------------------- helpers

    /**
     * Writes a zip whose bytes depend only on its contents: sorted entry order,
     * zeroed timestamps, fixed compression level. Without all three the SHA-1
     * changes on every run and cannot be pinned in a config file.
     */
    private static byte[] deterministicZip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            zip.setLevel(Deflater.BEST_COMPRESSION);
            List<String> names = new ArrayList<>(files.keySet());
            names.sort(Comparator.naturalOrder());
            for (String name : names) {
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(0L);
                entry.setCreationTime(java.nio.file.attribute.FileTime.fromMillis(0L));
                entry.setLastModifiedTime(java.nio.file.attribute.FileTime.fromMillis(0L));
                entry.setLastAccessTime(java.nio.file.attribute.FileTime.fromMillis(0L));
                zip.putNextEntry(entry);
                zip.write(files.get(name));
                zip.closeEntry();
            }
        }
        return buffer.toByteArray();
    }

    private static String sha1Hex(byte[] data) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(data);
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            hex.append(Character.forDigit((value >> 4) & 0xF, 16));
            hex.append(Character.forDigit(value & 0xF, 16));
        }
        return hex.toString();
    }

    static byte[] png(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", (OutputStream) out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    static int lerp(int from, int to, double t) {
        int a = channel(from, 24), r = channel(from, 16), g = channel(from, 8), b = channel(from, 0);
        int a2 = channel(to, 24), r2 = channel(to, 16), g2 = channel(to, 8), b2 = channel(to, 0);
        return (round(a, a2, t) << 24) | (round(r, r2, t) << 16) | (round(g, g2, t) << 8) | round(b, b2, t);
    }

    private static int channel(int argb, int shift) {
        return (argb >>> shift) & 0xFF;
    }

    private static int round(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }

    private static String argAfter(String[] args, String flag, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) {
                return args[i + 1];
            }
        }
        return fallback;
    }

    // ------------------------------------------------------------------- palette

    /** The one place a colour name becomes an ARGB int. */
    record Palette(Map<String, Integer> colors, Map<String, int[]> gradients, Map<String, String> axes) {

        static Palette load(Path file) throws IOException {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Map<String, Integer> colors = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : Json.objectOfStrings(json, "renkler").entrySet()) {
                colors.put(entry.getKey(), parseArgb(entry.getValue()));
            }
            Map<String, int[]> gradients = new LinkedHashMap<>();
            Map<String, String> axes = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : Json.nestedObjects(json, "gradyanlar").entrySet()) {
                Map<String, String> body = Json.objectOfStringsFromBody(entry.getValue());
                if (!body.containsKey("baslangic")) {
                    continue;
                }
                gradients.put(entry.getKey(),
                        new int[]{parseArgb(body.get("baslangic")), parseArgb(body.get("bitis"))});
                axes.put(entry.getKey(), body.getOrDefault("yon", "yatay"));
            }
            return new Palette(colors, gradients, axes);
        }

        int color(String key) {
            Integer value = colors.get(key);
            if (value == null) {
                throw new IllegalArgumentException("palet.json'da tanimsiz renk anahtari: '" + key + "'");
            }
            return value;
        }

        int gradientStart(String name) {
            return gradient(name)[0];
        }

        int gradientEnd(String name) {
            return gradient(name)[1];
        }

        boolean vertical(String name) {
            return "dikey".equals(axes.getOrDefault(name, "yatay"));
        }

        private int[] gradient(String name) {
            int[] value = gradients.get(name);
            if (value == null) {
                throw new IllegalArgumentException("palet.json'da tanimsiz gradyan: '" + name + "'");
            }
            return value;
        }

        private static int parseArgb(String hex) {
            String value = hex.startsWith("#") ? hex.substring(1) : hex;
            if (value.length() == 6) {
                return 0xFF000000 | Integer.parseInt(value, 16);
            }
            if (value.length() == 8) {
                return (int) Long.parseLong(value, 16);
            }
            throw new IllegalArgumentException("Renk #RRGGBB ya da #RRGGBBAA olmali: " + hex);
        }
    }

    // ---------------------------------------------------------------- icon sheet

    /** The 28 icon glyphs, laid out on one texture and described by one font file. */
    record IconSheet(byte[] png, String fontJson) {

        /** @return icon id to its mask rows, so the item textures can reuse the same drawings. */
        static Map<String, List<String>> masks(Path file) throws IOException {
            Map<String, List<String>> out = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry
                    : Json.nestedObjects(Files.readString(file, StandardCharsets.UTF_8), "simgeler").entrySet()) {
                out.put(entry.getKey(), Json.stringArray(entry.getValue(), "maske"));
            }
            return out;
        }

        static IconSheet build(Path file, Palette palette) throws IOException {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Map<String, String> entries = Json.nestedObjects(json, "simgeler");
            if (entries.isEmpty()) {
                throw new IOException("simgeler.json icinde 'simgeler' bos.");
            }

            int rows = (entries.size() + ICON_SHEET_COLUMNS - 1) / ICON_SHEET_COLUMNS;
            BufferedImage sheet = new BufferedImage(
                    ICON_SHEET_COLUMNS * GLYPH_SIZE, rows * GLYPH_SIZE, BufferedImage.TYPE_INT_ARGB);

            List<StringBuilder> chars = new ArrayList<>();
            for (int i = 0; i < rows; i++) {
                chars.add(new StringBuilder());
            }

            int index = 0;
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                String body = entry.getValue();
                List<String> mask = Json.stringArray(body, "maske");
                if (mask.size() != GLYPH_SIZE) {
                    throw new IOException(entry.getKey() + ": maske " + GLYPH_SIZE + " satir olmali, "
                            + mask.size() + " var.");
                }
                String gradient = Json.optionalString(body, "gradyan");
                int codepoint = parseCodepoint(Json.requireString(body, "kodnoktasi"), entry.getKey());

                int cellX = (index % ICON_SHEET_COLUMNS) * GLYPH_SIZE;
                int cellY = (index / ICON_SHEET_COLUMNS) * GLYPH_SIZE;
                paint(sheet, cellX, cellY, mask, palette, gradient, entry.getKey());

                chars.get(index / ICON_SHEET_COLUMNS).append(Character.toChars(codepoint));
                index++;
            }
            // Minecraft requires every row of the chars grid to be the same
            // length; the trailing cells are padded with U+0000, which the game
            // treats as "no glyph here".
            int width = ICON_SHEET_COLUMNS;
            StringBuilder charsJson = new StringBuilder();
            for (int i = 0; i < chars.size(); i++) {
                StringBuilder row = chars.get(i);
                while (row.codePointCount(0, row.length()) < width) {
                    row.append('\u0000');
                }
                if (i > 0) {
                    charsJson.append(",\n");
                }
                charsJson.append("        \"").append(escape(row.toString())).append('"');
            }

            String font = """
                    {
                      "providers": [
                        {
                          "type": "bitmap",
                          "file": "%s:font/simgeler.png",
                          "height": %d,
                          "ascent": %d,
                          "chars": [
                    %s
                          ]
                        }
                      ]
                    }
                    """.formatted(NAMESPACE, GLYPH_SIZE, 7, charsJson);

            // Qualified: the record's own png() accessor would otherwise shadow
            // the static helper of the same name.
            return new IconSheet(PaketUretici.png(sheet), font);
        }

        private static void paint(BufferedImage sheet, int originX, int originY, List<String> mask,
                                  Palette palette, String gradient, String id) {
            int minX = GLYPH_SIZE, maxX = -1, minY = GLYPH_SIZE, maxY = -1;
            for (int y = 0; y < GLYPH_SIZE; y++) {
                String row = mask.get(y);
                for (int x = 0; x < row.length(); x++) {
                    if (row.charAt(x) == '.') {
                        continue;
                    }
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
            if (maxX < 0) {
                throw new IllegalArgumentException(id + ": maske tamamen bos.");
            }
            // The one-cell rule: ink must not reach the last column, or the
            // glyph's advance grows and LoreWrapper's 42-character budget stops
            // matching what the player actually sees.
            if (maxX > GLYPH_SIZE - 2) {
                throw new IllegalArgumentException(id + ": murekkep son sutuna tasiyor (tek hucre kurali).");
            }

            for (int y = 0; y < GLYPH_SIZE; y++) {
                String row = mask.get(y);
                for (int x = 0; x < row.length(); x++) {
                    char key = row.charAt(x);
                    if (key == '.') {
                        continue;
                    }
                    int argb;
                    if (key == 'G') {
                        if (gradient == null) {
                            throw new IllegalArgumentException(id + ": maskede 'G' var ama 'gradyan' alani yok.");
                        }
                        boolean vertical = palette.vertical(gradient);
                        int from = vertical ? minY : minX;
                        int to = vertical ? maxY : maxX;
                        int at = vertical ? y : x;
                        double t = to == from ? 0.0 : (at - from) / (double) (to - from);
                        argb = lerp(palette.gradientStart(gradient), palette.gradientEnd(gradient), t);
                    } else {
                        argb = palette.color(String.valueOf(key));
                    }
                    sheet.setRGB(originX + x, originY + y, argb);
                }
            }
        }

        private static int parseCodepoint(String value, String id) {
            if (!value.startsWith("U+")) {
                throw new IllegalArgumentException(id + ": kod noktasi 'U+XXXX' biciminde olmali: " + value);
            }
            return Integer.parseInt(value.substring(2), 16);
        }

        private static String escape(String value) {
            StringBuilder out = new StringBuilder();
            value.codePoints().forEach(cp -> out.append(String.format(Locale.ROOT, "\\u%04X", cp)));
            return out.toString();
        }
    }

    // ------------------------------------------------------------------ hmmo:pano

    /**
     * The pack's second font: wide, multi-cell glyphs that must never appear in
     * item lore, plus the boss bar sprites that share their palette.
     *
     * <p>Kept separate from {@code hmmo:simgeler} on purpose. The icon font
     * redraws existing BMP codepoints at one character cell each, so it cannot
     * change how wide anything is; this one draws bars several cells wide at
     * Private Use Area codepoints, which would silently invalidate
     * {@code LoreWrapper}'s 42-character budget if it were ever used in a
     * tooltip. Two fonts is what makes "never in lore" a rule a test can
     * enforce.</p>
     */
    static final class Panel {

        /** Must stay in step with {@code locale.GlyphBar}. */
        private static final int CELL_EMPTY = 0xF210;
        private static final int CELL_FULL = 0xF211;
        private static final int CAP_LEFT = 0xF212;
        private static final int CAP_RIGHT = 0xF213;

        private static final int BAR_HEIGHT = 8;
        private static final int CELL_WIDTH = 5;
        private static final int CAP_WIDTH = 3;

        /** Signed space advances live at U+F0nn; see {@link #spaceProvider()}. */
        private static final int SPACE_FIRST = 0xF000;
        /** The four menu panel backgrounds, one per chest size in use. */
        private static final int PANEL_FIRST = 0xF100;

        /**
         * A chest GUI panel is 176 wide; a row of slots is 18 tall and the
         * player-inventory strip below is identical across sizes.
         */
        private static final int PANEL_WIDTH = 176;
        private static final int PANEL_BASE_HEIGHT = 114;
        private static final int ROW_HEIGHT = 18;

        /**
         * {@code AbstractContainerScreen} draws the title at (leftPos+8, topPos+6)
         * and the default font puts the baseline 7px below a glyph's top, so the
         * baseline sits 13px below the panel's top edge. A background glyph with
         * this ascent therefore starts exactly at the panel's top edge.
         */
        private static final int TITLE_BASELINE = 13;

        private Panel() {
        }

        static void build(Palette palette, Map<String, byte[]> files) {
            // One strip, four glyphs, laid out left to right at a fixed pitch so
            // the chars grid below is a single row.
            int pitch = 8;
            BufferedImage strip = new BufferedImage(pitch * 4, BAR_HEIGHT, BufferedImage.TYPE_INT_ARGB);

            drawCell(strip, 0 * pitch, palette.color("e"), palette.color("z"));
            drawCell(strip, 1 * pitch, palette.gradientStart("kimlik"), palette.gradientEnd("kimlik"));
            drawCap(strip, 2 * pitch, palette.color("E"), true);
            drawCap(strip, 3 * pitch, palette.color("E"), false);

            files.put("assets/" + NAMESPACE + "/textures/font/pano.png", png(strip));

            StringBuilder providers = new StringBuilder();
            providers.append("""
                            {
                              "type": "bitmap",
                              "file": "%s:font/pano.png",
                              "height": %d,
                              "ascent": 7,
                              "chars": ["\\u%04X\\u%04X\\u%04X\\u%04X"]
                            }"""
                    .formatted(NAMESPACE, BAR_HEIGHT, CELL_EMPTY, CELL_FULL, CAP_LEFT, CAP_RIGHT));
            providers.append(",\n").append(spaceProvider());
            for (int rows = 3; rows <= 6; rows++) {
                BufferedImage panel = panel(palette, rows);
                files.put("assets/" + NAMESPACE + "/textures/font/panel_" + rows + ".png", png(panel));
                providers.append(",\n").append("""
                                {
                                  "type": "bitmap",
                                  "file": "%s:font/panel_%d.png",
                                  "height": %d,
                                  "ascent": %d,
                                  "chars": ["\\u%04X"]
                                }"""
                        .formatted(NAMESPACE, rows, panel.getHeight(), TITLE_BASELINE, PANEL_FIRST + (rows - 3)));
            }

            files.put("assets/" + NAMESPACE + "/font/pano.json",
                    ("{\n  \"providers\": [\n" + providers.toString().indent(4) + "  ]\n}\n")
                            .getBytes(StandardCharsets.UTF_8));

            bossBars(palette, files);
        }

        /**
         * Signed advances at U+F000.., in powers of two so any offset is a short
         * binary composition. Negative space is what lets a title glyph paint the
         * whole panel: pull the pen back to the panel's left edge, draw, pull
         * back again, then let the real title render where it always did.
         */
        private static String spaceProvider() {
            StringBuilder advances = new StringBuilder();
            for (int bit = 0; bit < 9; bit++) {
                int magnitude = 1 << bit;
                if (bit > 0) {
                    advances.append(",\n");
                }
                advances.append("    \"\\u%04X\": %d,\n".formatted(SPACE_FIRST + bit, -magnitude));
                advances.append("    \"\\u%04X\": %d".formatted(SPACE_FIRST + 9 + bit, magnitude));
            }
            return """
                    {
                      "type": "space",
                      "advances": {
                    %s
                      }
                    }""".formatted(advances);
        }

        /**
         * One menu background. Deliberately geometric rather than a redrawn
         * chest: a flat ground, a two-tone frame in the identity gradient and a
         * darker well behind the slot grid, so a menu reads as this server's
         * screen rather than as a chest someone painted over.
         */
        private static BufferedImage panel(Palette palette, int rows) {
            int height = PANEL_BASE_HEIGHT + rows * ROW_HEIGHT;
            BufferedImage image = new BufferedImage(PANEL_WIDTH, height, BufferedImage.TYPE_INT_ARGB);

            int ground = palette.color("Z");
            int well = palette.color("z");
            int frameStart = palette.gradientStart("kimlik");
            int frameEnd = palette.gradientEnd("kimlik");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < PANEL_WIDTH; x++) {
                    image.setRGB(x, y, ground);
                }
            }
            // The slot grid sits at x=7..168, starting 17px down; darkening just
            // that area makes the clickable region obvious without drawing 54
            // individual slots.
            for (int y = 17; y < 17 + rows * ROW_HEIGHT && y < height; y++) {
                for (int x = 7; x < PANEL_WIDTH - 7; x++) {
                    image.setRGB(x, y, well);
                }
            }
            for (int x = 0; x < PANEL_WIDTH; x++) {
                int argb = lerp(frameStart, frameEnd, x / (double) (PANEL_WIDTH - 1));
                image.setRGB(x, 0, argb);
                image.setRGB(x, height - 1, argb);
            }
            for (int y = 0; y < height; y++) {
                int argb = lerp(frameStart, frameEnd, y / (double) (height - 1));
                image.setRGB(0, y, argb);
                image.setRGB(PANEL_WIDTH - 1, y, argb);
            }
            return image;
        }

        /** A cell is a filled block with a one-pixel darker outline, vertically centred. */
        private static void drawCell(BufferedImage strip, int originX, int fill, int outline) {
            for (int y = 2; y <= 5; y++) {
                for (int x = 0; x < CELL_WIDTH; x++) {
                    boolean edge = y == 2 || y == 5;
                    strip.setRGB(originX + x, y, edge ? outline : fill);
                }
            }
        }

        private static void drawCap(BufferedImage strip, int originX, int color, boolean left) {
            for (int y = 1; y <= 6; y++) {
                int x = left ? 0 : CAP_WIDTH - 1;
                strip.setRGB(originX + x, y, color);
            }
            for (int x = 0; x < CAP_WIDTH; x++) {
                strip.setRGB(originX + x, 1, color);
                strip.setRGB(originX + x, 6, color);
            }
        }

        /**
         * The seven vanilla boss bar colours plus the four notched variants,
         * redrawn in the pack's palette.
         *
         * <p>Colour identity is deliberately preserved rather than flattened
         * into the purple/blue theme: {@code BossRarity} maps purple to Epik and
         * pink to Mistik, and players have already learned that. The pack
         * changes the <em>surface</em> - a darker trough, a cleaner cap - not
         * what each colour means. Confirmed 182x5 with no .mcmeta by reading the
         * real 26.2 client jar.</p>
         */
        private static void bossBars(Palette palette, Map<String, byte[]> files) {
            Map<String, Integer> colors = new LinkedHashMap<>();
            colors.put("pink", palette.color("p"));
            colors.put("blue", palette.color("b"));
            colors.put("red", palette.color("k"));
            colors.put("green", palette.color("y"));
            colors.put("yellow", palette.color("a"));
            colors.put("purple", palette.color("m"));
            colors.put("white", palette.color("w"));

            for (Map.Entry<String, Integer> entry : colors.entrySet()) {
                put(files, entry.getKey() + "_background", bar(palette.color("z"), palette.color("e"), 0));
                put(files, entry.getKey() + "_progress", bar(entry.getValue(), lighten(entry.getValue()), 0));
            }
            for (int notches : new int[]{6, 10, 12, 20}) {
                put(files, "notched_" + notches + "_background", bar(palette.color("z"), palette.color("e"), notches));
                put(files, "notched_" + notches + "_progress",
                        bar(palette.color("m"), palette.color("M"), notches));
            }
        }

        private static void put(Map<String, byte[]> files, String name, BufferedImage image) {
            files.put("assets/minecraft/textures/gui/sprites/boss_bar/" + name + ".png", png(image));
        }

        private static BufferedImage bar(int fill, int highlight, int notches) {
            BufferedImage image = new BufferedImage(182, 5, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 182; x++) {
                for (int y = 0; y < 5; y++) {
                    // Row 1 catches the light, row 4 is the shadowed lip: two
                    // rows of shading is all a five-pixel bar can carry.
                    image.setRGB(x, y, y == 1 ? highlight : fill);
                }
            }
            if (notches > 1) {
                int step = 182 / notches;
                for (int notch = 1; notch < notches; notch++) {
                    int x = Math.min(181, notch * step);
                    for (int y = 0; y < 5; y++) {
                        image.setRGB(x, y, 0xFF000000);
                    }
                }
            }
            return image;
        }

        private static int lighten(int argb) {
            return lerp(argb, 0xFFFFFFFF, 0.35);
        }
    }

    // -------------------------------------------------------------- item textures

    /**
     * The GUI item icons: a 16x16 plate with one of {@link IconSheet}'s own 8x8
     * masks centred on it.
     *
     * <p>Composing rather than drawing is what keeps ~60 textures maintainable
     * by hand: the emblems are the <em>same</em> masks the font uses, so fixing
     * an icon fixes both its glyph and its item texture, and the two can never
     * drift apart. Variety comes from the plate - shape, fill and border - not
     * from sixty separate drawings.</p>
     */
    static final class ItemTextures {

        private static final int TILE = 16;
        private static final int EMBLEM_OFFSET = (TILE - GLYPH_SIZE) / 2;

        private ItemTextures() {
        }

        static void build(Path root, Palette palette, Map<String, byte[]> files) throws IOException {
            Path manifest = root.resolve("esya-modelleri.json");
            if (!Files.isRegularFile(manifest)) {
                return;
            }
            Map<String, List<String>> masks = IconSheet.masks(root.resolve("simgeler.json"));
            Map<String, String> models = Json.nestedObjects(Files.readString(manifest, StandardCharsets.UTF_8), "modeller");

            for (Map.Entry<String, String> entry : models.entrySet()) {
                String id = entry.getKey();
                String body = entry.getValue();
                BufferedImage tile = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
                if (body.contains("\"maske16\"")) {
                    // A shape in its own right rather than a badge: the wolf
                    // parts are silhouettes, and an 8x8 emblem on a plate cannot
                    // be a snout.
                    drawRawMask(tile, palette, Json.stringArray(body, "maske16"), id);
                } else {
                    String emblemId = Json.requireString(body, "simge");
                    List<String> mask = masks.get(emblemId);
                    if (mask == null) {
                        throw new IOException(id + ": 'simge' bilinmeyen bir maskeyi gosteriyor: " + emblemId);
                    }
                    drawPlate(tile, palette, Json.requireString(body, "plaka"),
                            Json.requireString(body, "zemin"), Json.requireString(body, "kenar"));
                    drawEmblem(tile, palette, mask, Json.optionalString(body, "simge-renk"));
                }

                files.put("assets/" + NAMESPACE + "/textures/item/" + PREFIX + id + ".png", png(tile));
                files.put("assets/" + NAMESPACE + "/models/item/" + PREFIX + id + ".json", ("""
                        {
                          "parent": "minecraft:item/generated",
                          "textures": { "layer0": "%s:item/%s%s" }
                        }
                        """.formatted(NAMESPACE, PREFIX, id)).getBytes(StandardCharsets.UTF_8));
                files.put("assets/" + NAMESPACE + "/items/" + PREFIX + id + ".json", ("""
                        {
                          "model": { "type": "minecraft:model", "model": "%s:item/%s%s" }
                        }
                        """.formatted(NAMESPACE, PREFIX, id)).getBytes(StandardCharsets.UTF_8));
            }
        }

        /** A literal 16x16 mask, for icons that are a shape rather than an emblem on a plate. */
        private static void drawRawMask(BufferedImage tile, Palette palette, List<String> mask, String id) {
            if (mask.size() != TILE) {
                throw new IllegalArgumentException(id + ": maske16 tam " + TILE + " satir olmali, " + mask.size() + " var.");
            }
            for (int y = 0; y < TILE; y++) {
                String row = mask.get(y);
                if (row.length() != TILE) {
                    throw new IllegalArgumentException(id + ": maske16 satiri " + TILE + " karakter olmali: '" + row + "'");
                }
                for (int x = 0; x < TILE; x++) {
                    char key = row.charAt(x);
                    if (key != '.') {
                        tile.setRGB(x, y, palette.color(String.valueOf(key)));
                    }
                }
            }
        }

        private static void drawPlate(BufferedImage tile, Palette palette, String shape, String fill, String edge) {
            if ("yok".equals(shape)) {
                return;
            }
            int fillArgb = palette.color(fill);
            int edgeArgb = palette.color(edge);
            for (int y = 0; y < TILE; y++) {
                for (int x = 0; x < TILE; x++) {
                    if (!inside(shape, x, y)) {
                        continue;
                    }
                    tile.setRGB(x, y, onEdge(shape, x, y) ? edgeArgb : fillArgb);
                }
            }
        }

        /** Plate silhouettes, kept to integer maths so the output is bit-stable. */
        private static boolean inside(String shape, int x, int y) {
            return switch (shape) {
                case "kare" -> true;
                // A 16x16 disc: squared distance from the centre of the tile.
                case "yuvarlak" -> {
                    int dx = 2 * x - (TILE - 1);
                    int dy = 2 * y - (TILE - 1);
                    yield dx * dx + dy * dy <= (TILE - 1) * (TILE - 1);
                }
                // Square shoulders, tapering to a point at the bottom.
                case "kalkan" -> y <= 9 || Math.abs(2 * x - (TILE - 1)) <= 2 * (TILE - 1 - y) + 1;
                default -> throw new IllegalArgumentException("Bilinmeyen plaka sekli: " + shape);
            };
        }

        private static boolean onEdge(String shape, int x, int y) {
            return !inside(shape, x - 1, y) || !inside(shape, x + 1, y)
                    || !inside(shape, x, y - 1) || !inside(shape, x, y + 1)
                    || x == 0 || y == 0 || x == TILE - 1 || y == TILE - 1;
        }

        private static void drawEmblem(BufferedImage tile, Palette palette, List<String> mask, String flatColor) {
            Integer flat = flatColor == null ? null : palette.color(flatColor);
            for (int y = 0; y < GLYPH_SIZE; y++) {
                String row = mask.get(y);
                for (int x = 0; x < row.length() && x < GLYPH_SIZE; x++) {
                    char key = row.charAt(x);
                    if (key == '.') {
                        continue;
                    }
                    int argb;
                    if (flat != null) {
                        argb = flat;
                    } else if (key == 'G') {
                        // Gradient emblems collapse to the identity gradient's
                        // midpoint here: an 8-pixel ramp inside a plate reads as
                        // noise, and the plate's border already carries colour.
                        argb = lerp(palette.gradientStart("kimlik"), palette.gradientEnd("kimlik"), 0.5);
                    } else {
                        argb = palette.color(String.valueOf(key));
                    }
                    tile.setRGB(EMBLEM_OFFSET + x, EMBLEM_OFFSET + y, argb);
                }
            }
        }
    }

    // ------------------------------------------------------------- tiny JSON reader

    /**
     * A deliberately minimal reader for the three hand-written config files in
     * this directory. Not a general JSON parser and never sees untrusted input -
     * pulling in Gson or Jackson would give this standalone tool a dependency
     * it exists specifically to avoid.
     */
    static final class Json {

        private Json() {
        }

        /** @return the body text of {@code "key": { ... }}, braces excluded. */
        static String objectBody(String json, String key) {
            int at = json.indexOf('"' + key + '"');
            if (at < 0) {
                throw new IllegalArgumentException("JSON'da '" + key + "' anahtari yok.");
            }
            int open = json.indexOf('{', at);
            int depth = 0;
            for (int i = open; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(open + 1, i);
                    }
                }
            }
            throw new IllegalArgumentException("'" + key + "' nesnesi kapanmamis.");
        }

        static Map<String, String> objectOfStrings(String json, String key) {
            return objectOfStringsFromBody(objectBody(json, key));
        }

        /** Top-level {@code "name": "value"} pairs, skipping nested objects and arrays. */
        static Map<String, String> objectOfStringsFromBody(String body) {
            Map<String, String> out = new LinkedHashMap<>();
            int i = 0;
            while (i < body.length()) {
                int nameStart = body.indexOf('"', i);
                if (nameStart < 0) {
                    break;
                }
                int nameEnd = body.indexOf('"', nameStart + 1);
                String name = body.substring(nameStart + 1, nameEnd);
                int colon = body.indexOf(':', nameEnd);
                if (colon < 0) {
                    break;
                }
                int valueStart = colon + 1;
                while (valueStart < body.length() && Character.isWhitespace(body.charAt(valueStart))) {
                    valueStart++;
                }
                if (valueStart >= body.length()) {
                    break;
                }
                char first = body.charAt(valueStart);
                if (first == '"') {
                    int valueEnd = body.indexOf('"', valueStart + 1);
                    out.put(name, body.substring(valueStart + 1, valueEnd));
                    i = valueEnd + 1;
                } else if (first == '{' || first == '[') {
                    i = skipBalanced(body, valueStart);
                } else {
                    int valueEnd = valueStart;
                    while (valueEnd < body.length() && body.charAt(valueEnd) != ',' && body.charAt(valueEnd) != '\n') {
                        valueEnd++;
                    }
                    out.put(name, body.substring(valueStart, valueEnd).trim());
                    i = valueEnd;
                }
            }
            return out;
        }

        /** @return each {@code "name": { ... }} member of an object, name to raw body. */
        static Map<String, String> nestedObjects(String json, String key) {
            String body = objectBody(json, key);
            Map<String, String> out = new LinkedHashMap<>();
            int i = 0;
            while (i < body.length()) {
                int nameStart = body.indexOf('"', i);
                if (nameStart < 0) {
                    break;
                }
                int nameEnd = body.indexOf('"', nameStart + 1);
                String name = body.substring(nameStart + 1, nameEnd);
                int colon = body.indexOf(':', nameEnd);
                if (colon < 0) {
                    break;
                }
                int valueStart = colon + 1;
                while (valueStart < body.length() && Character.isWhitespace(body.charAt(valueStart))) {
                    valueStart++;
                }
                if (valueStart >= body.length()) {
                    break;
                }
                char first = body.charAt(valueStart);
                if (first == '{') {
                    int end = skipBalanced(body, valueStart);
                    if (!name.startsWith("_")) {
                        out.put(name, body.substring(valueStart + 1, end - 1));
                    }
                    i = end;
                } else if (first == '[') {
                    i = skipBalanced(body, valueStart);
                } else if (first == '"') {
                    i = body.indexOf('"', valueStart + 1) + 1;
                } else {
                    i = valueStart + 1;
                }
            }
            return out;
        }

        static List<String> stringArray(String body, String key) {
            int at = body.indexOf('"' + key + '"');
            if (at < 0) {
                throw new IllegalArgumentException("'" + key + "' dizisi yok.");
            }
            int open = body.indexOf('[', at);
            int close = skipBalanced(body, open);
            String inner = body.substring(open + 1, close - 1);
            List<String> out = new ArrayList<>();
            int i = 0;
            while (true) {
                int start = inner.indexOf('"', i);
                if (start < 0) {
                    break;
                }
                int end = inner.indexOf('"', start + 1);
                out.add(inner.substring(start + 1, end));
                i = end + 1;
            }
            return out;
        }

        static String requireString(String body, String key) {
            String value = optionalString(body, key);
            if (value == null) {
                throw new IllegalArgumentException("'" + key + "' alani zorunlu.");
            }
            return value;
        }

        static String optionalString(String body, String key) {
            int at = body.indexOf('"' + key + '"');
            if (at < 0) {
                return null;
            }
            int colon = body.indexOf(':', at);
            int start = body.indexOf('"', colon);
            if (start < 0) {
                return null;
            }
            int end = body.indexOf('"', start + 1);
            return body.substring(start + 1, end);
        }

        /** @return the index just past the balanced {@code {...}} or {@code [...]} at {@code from}. */
        private static int skipBalanced(String text, int from) {
            char open = text.charAt(from);
            char close = open == '{' ? '}' : ']';
            int depth = 0;
            boolean inString = false;
            for (int i = from; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                    inString = !inString;
                } else if (!inString) {
                    if (c == open) {
                        depth++;
                    } else if (c == close) {
                        depth--;
                        if (depth == 0) {
                            return i + 1;
                        }
                    }
                }
            }
            return text.length();
        }
    }
}
