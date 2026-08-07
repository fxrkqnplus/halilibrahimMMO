package com.halilibrahim.halilibrahimmmo.core;

import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads every configuration file the plugin ships, before any service tries to,
 * and says in plain Turkish what is wrong with the ones that are broken.
 *
 * <p><strong>Why this exists.</strong> Each service parses its own file and
 * throws a {@link LifecycleException} when it cannot, which is correct - but a
 * server owner who has hand-edited three files and mistyped one of them gets a
 * single English stack trace naming the first service that happened to be
 * started, halfway down a boot log, with no line number. That is the least
 * useful moment in the plugin's life to be terse. This pass answers three
 * questions the stack trace does not: <em>which file</em>, <em>which line</em>,
 * and <em>is it the only one</em>.</p>
 *
 * <p><strong>It reports; it does not decide.</strong> A missing file is normal -
 * the services copy their own defaults out of the jar - and a file with a
 * missing section may still be perfectly loadable by the service that owns it,
 * which knows its own rules and this class deliberately does not duplicate. The
 * only thing treated as fatal here is YAML that does not parse at all, because
 * every reader of that file is about to fail in the same way and saying so once,
 * clearly, beats saying it five times in five stack traces.</p>
 *
 * <p>Console-only and Turkish, following §9's rule for operator-facing text.</p>
 */
public final class ConfigIntegrityCheck {

    /**
     * Every file the jar ships, mapped to the top-level section a healthy copy
     * has.
     *
     * <p>The section name is a <em>hint</em>, not a schema: it catches the one
     * mistake that produces no parse error and no useful exception either -
     * pasting a file's contents over a different file. A blank value means the
     * file has no single defining section and only its syntax is checked.</p>
     */
    private static final Map<String, String> SHIPPED_FILES = shippedFiles();

    private static Map<String, String> shippedFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("config.yml", "veritabani");
        files.put("messages.yml", "onek");
        files.put("progression.yml", "");
        files.put("skills.yml", "");
        files.put("abilities.yml", "");
        files.put("classes.yml", "siniflar");
        files.put("prestige.yml", "fiziksel-oduller");
        files.put("quests.yml", "");
        files.put("collections.yml", "");
        files.put("leaderboards.yml", "");
        files.put("titles.yml", "");
        files.put("cosmetics.yml", "");
        files.put("feedback.yml", "");
        files.put("sayginlik.yml", "");
        files.put("bosslar.yml", "");
        files.put("modlar.yml", "");
        files.put("sunucu.yml", "");
        files.put("rehber.yml", "kategoriler");
        files.put("yedek.yml", "");
        return Map.copyOf(files);
    }

    private ConfigIntegrityCheck() {
        throw new UnsupportedOperationException("ConfigIntegrityCheck is a static utility class");
    }

    /**
     * Runs the pass.
     *
     * @return every file that could not be parsed at all, in the order they were
     *         checked - empty when the configuration is healthy
     */
    public static List<String> run(JavaPlugin plugin, PluginLogger logger) {
        List<String> broken = new ArrayList<>();
        File folder = plugin.getDataFolder();
        if (!folder.isDirectory()) {
            // First boot: nothing has been written yet and every service is
            // about to copy its own default. Not a problem, and saying so stops
            // an owner reading the next few lines as errors.
            logger.info("Ilk acilis: yapilandirma klasoru yok, varsayilan dosyalar olusturulacak.");
            return broken;
        }
        for (Map.Entry<String, String> entry : SHIPPED_FILES.entrySet()) {
            File file = new File(folder, entry.getKey());
            if (!file.isFile()) {
                logger.debug(() -> entry.getKey() + " henuz yok, varsayilani yazilacak.");
                continue;
            }
            String failure = describeFailure(file, entry.getValue());
            if (failure != null) {
                broken.add(entry.getKey());
                logger.error("YAPILANDIRMA HATASI - " + entry.getKey() + ": " + failure);
            }
        }
        if (broken.isEmpty()) {
            logger.debug(() -> "Yapilandirma butunlugu kontrol edildi, sorun yok.");
            return broken;
        }
        logger.error("Toplam " + broken.size() + " dosya okunamadi: " + String.join(", ", broken));
        logger.error("Bozuk dosyayi duzeltin ya da SILIN - silinen her dosya bir sonraki aciliste "
                + "varsayilan haliyle yeniden olusturulur ve hicbir oyuncu verisi kaybolmaz.");
        return broken;
    }

    /**
     * What is wrong with one file, or {@code null} when it is fine.
     *
     * <p>The three answers are deliberately different sentences rather than one
     * generic "invalid": an owner who has left a tab character in the file, one
     * whose disk refused the read, and one who has pasted the wrong file each
     * need a different next step.</p>
     */
    private static String describeFailure(File file, String expectedSection) {
        String contents;
        try {
            contents = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "dosya okunamadi (" + exception.getMessage() + "). Izinleri ve diski kontrol edin.";
        }
        YamlConfiguration parsed = new YamlConfiguration();
        try {
            parsed.loadFromString(contents);
        } catch (InvalidConfigurationException exception) {
            // SnakeYAML's message carries the line and column, which is the one
            // piece of information an owner actually needs; it is passed through
            // rather than summarised.
            return "YAML sozdizimi bozuk. Ayrinti: " + firstLine(exception.getMessage());
        }
        if (!expectedSection.isEmpty() && !parsed.contains(expectedSection)) {
            return "beklenen '" + expectedSection + "' bolumu yok. Dosyanin icerigi baska bir dosyaya mi ait?";
        }
        return null;
    }

    /** SnakeYAML answers with a paragraph; the first line is the part with the position in it. */
    private static String firstLine(String message) {
        if (message == null || message.isBlank()) {
            return "bilinmeyen sozdizimi hatasi";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message.trim() : message.substring(0, newline).trim();
    }
}
