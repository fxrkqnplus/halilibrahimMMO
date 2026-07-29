package com.halilibrahim.halilibrahimmmo.modcheck;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Plugin-message based implementation of {@link ModVerificationService}.
 *
 * <p>Timeline for one join, all of it on the player's own region scheduler so
 * no Bukkit API is ever touched from the plugin-message thread:</p>
 * <ol>
 *   <li>{@code brand-gecikme-tik} ticks after join, read the client brand.
 *       A non-Fabric brand fails immediately; a {@code null} brand does not,
 *       because a client that simply has not sent one yet still deserves the
 *       handshake to decide.</li>
 *   <li>Send the challenge (protocol version + nonce).</li>
 *   <li>{@code zaman-asimi-saniye} later, if nothing came back, fail.</li>
 * </ol>
 */
public final class PluginMessageModVerificationService implements ModVerificationService {

    private static final String FILE_NAME = "modlar.yml";

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final PluginLogger logger;
    private final SecureRandom random = new SecureRandom();
    private final Map<UUID, ModVerificationState> states = new ConcurrentHashMap<>();
    private final AtomicReference<ModVerificationSettings> settings = new AtomicReference<>();

    public PluginMessageModVerificationService(JavaPlugin plugin, MessageService messages, PluginLogger logger) {
        this.plugin = plugin;
        this.messages = messages;
        this.logger = logger;
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void disable() {
        states.clear();
    }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        try {
            settings.set(parse(YamlConfiguration.loadConfiguration(file)));
        } catch (RuntimeException exception) {
            throw new LifecycleException("modlar.yml could not be loaded.", exception);
        }
    }

    private ModVerificationSettings parse(YamlConfiguration yaml) {
        ConfigurationSection section = yaml.getConfigurationSection("dogrulama");
        if (section == null) {
            throw new IllegalArgumentException("modlar.yml is missing the 'dogrulama' section.");
        }

        List<RequiredMod> required = new ArrayList<>();
        for (Map<?, ?> entry : yaml.getMapList("zorunlu-modlar")) {
            required.add(new RequiredMod(
                    string(entry, "id"),
                    string(entry, "gorunen-ad"),
                    string(entry, "minimum-versiyon")
            ));
        }
        if (required.isEmpty()) {
            throw new IllegalArgumentException("modlar.yml lists no required mods; disable verification instead.");
        }

        List<ForbiddenMod> forbidden = new ArrayList<>();
        for (Map<?, ?> entry : yaml.getMapList("yasakli-modlar")) {
            forbidden.add(new ForbiddenMod(string(entry, "id"), string(entry, "gorunen-ad")));
        }

        return new ModVerificationSettings(
                section.getBoolean("etkin", true),
                section.getLong("zaman-asimi-saniye", 10L),
                section.getBoolean("brand-kontrolu", true),
                section.getLong("brand-gecikme-tik", 20L),
                section.getInt("azami-mod-sayisi", 512),
                section.getString("bypass-izni", "hmmo.mod.bypass"),
                required,
                forbidden
        );
    }

    private static String string(Map<?, ?> entry, String key) {
        Object value = entry.get(key);
        if (value == null) {
            throw new IllegalArgumentException("modlar.yml mod entry is missing '" + key + "'.");
        }
        return value.toString();
    }

    @Override
    public ModVerificationSettings settings() {
        ModVerificationSettings value = settings.get();
        if (value == null) {
            throw new IllegalStateException("ModVerificationService.enable() has not run yet.");
        }
        return value;
    }

    @Override
    public void begin(Player player) {
        ModVerificationSettings current = settings();
        if (!current.enabled()) {
            return;
        }
        if (player.hasPermission(current.bypassPermission())) {
            states.put(player.getUniqueId(), ModVerificationState.exempt());
            return;
        }

        byte[] nonce = new byte[ModCheckProtocol.NONCE_LENGTH];
        random.nextBytes(nonce);
        ModVerificationState state = ModVerificationState.pending(nonce);
        states.put(player.getUniqueId(), state);

        byte[] request = ModCheckProtocol.request(nonce);
        long brandDelay = Math.max(1L, current.brandDelayTicks());

        player.getScheduler().runDelayed(plugin, task -> {
            if (state.isResolved() || !player.isOnline()) {
                return;
            }
            state.clientBrand(player.getClientBrandName());
            // Claiming the decision before kicking is what stops the timeout
            // task (or a late reply) from also acting on the same player.
            if (current.brandCheckEnabled() && isDefinitelyNotFabric(state.clientBrand()) && state.resolve()) {
                fail(player, MessageKey.MODCHECK_KICK_NOT_FABRIC, Map.of(),
                        "Fabric olmayan istemci (" + state.clientBrand() + ")");
                return;
            }
            player.sendPluginMessage(plugin, ModCheckProtocol.CHANNEL, request);
        }, null, brandDelay);

        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline() || !state.resolve()) {
                return;
            }
            fail(player, MessageKey.MODCHECK_KICK_TIMEOUT, Map.of(), "istemci mod listesini gondermedi");
        }, null, brandDelay + current.timeoutTicks());
    }

    /**
     * A blank brand means "the client has not told us yet", which is not
     * evidence of anything - only an explicitly non-Fabric brand fails here.
     */
    private static boolean isDefinitelyNotFabric(String brand) {
        return !brand.isBlank() && !brand.toLowerCase(Locale.ROOT).contains("fabric");
    }

    @Override
    public void handleResponse(Player player, byte[] payload) {
        ModVerificationState state = states.get(player.getUniqueId());
        if (state == null || !state.resolve()) {
            return;
        }
        ModVerificationSettings current = settings();

        ModCheckResponse response;
        try {
            response = ModCheckProtocol.readResponse(payload, current.maximumModCount());
        } catch (ModCheckProtocolException exception) {
            fail(player, MessageKey.MODCHECK_KICK_INVALID, Map.of(), "gecersiz cevap: " + exception.getMessage());
            return;
        }
        if (!MessageDigest.isEqual(response.nonce(), state.nonce())) {
            fail(player, MessageKey.MODCHECK_KICK_INVALID, Map.of(), "nonce eslesmedi");
            return;
        }

        state.reportedModCount(response.modCount());
        logger.debug(() -> "Mod dogrulama - " + player.getName() + " " + response.modCount()
                + " mod bildirdi: " + String.join(", ", response.mods().keySet()));

        evaluate(player, state, current, response);
    }

    private void evaluate(Player player, ModVerificationState state, ModVerificationSettings current, ModCheckResponse response) {
        Map<String, ForbiddenMod> forbidden = current.forbiddenById();
        for (String reportedId : response.mods().keySet()) {
            ForbiddenMod match = forbidden.get(reportedId);
            if (match != null) {
                fail(player, MessageKey.MODCHECK_KICK_FORBIDDEN, Map.of("mod", match.displayName()),
                        "yasakli mod: " + match.id());
                return;
            }
        }

        List<String> missing = new ArrayList<>();
        Map<String, String> outdated = new LinkedHashMap<>();
        RequiredMod firstOutdated = null;

        for (RequiredMod requiredMod : current.requiredMods()) {
            String reportedVersion = response.mods().get(requiredMod.id());
            if (reportedVersion == null) {
                missing.add(requiredMod.displayName());
                continue;
            }
            if (!isAcceptableVersion(requiredMod, reportedVersion)) {
                if (firstOutdated == null) {
                    firstOutdated = requiredMod;
                    outdated.put("mod", requiredMod.displayName());
                    outdated.put("gereken", requiredMod.minimumVersion());
                    outdated.put("mevcut", reportedVersion);
                }
            }
        }

        if (!missing.isEmpty()) {
            fail(player, MessageKey.MODCHECK_KICK_MISSING, Map.of("modlar", String.join(", ", missing)),
                    "eksik mod: " + String.join(", ", missing));
            return;
        }
        if (firstOutdated != null) {
            fail(player, MessageKey.MODCHECK_KICK_OUTDATED, outdated,
                    "eski surum: " + firstOutdated.id() + " -> " + outdated.get("mevcut"));
            return;
        }

        state.markVerified();
        player.getScheduler().run(plugin, task -> messages.send(player, MessageKey.MODCHECK_VERIFIED), null);
    }

    /**
     * An unparseable version is a failure, never a pass - a mod that renamed
     * its versioning scheme must be noticed and fixed in {@code modlar.yml},
     * not silently waved through.
     */
    private boolean isAcceptableVersion(RequiredMod requiredMod, String reportedVersion) {
        try {
            return ModVersions.isAtLeast(reportedVersion, requiredMod.minimumVersion());
        } catch (IllegalArgumentException exception) {
            logger.warn("Mod surumu karsilastirilamadi (" + requiredMod.id() + "): bildirilen='"
                    + reportedVersion + "', beklenen='" + requiredMod.minimumVersion() + "'.");
            return false;
        }
    }

    /**
     * Kicks with a two-part message: the specific reason, then the standing
     * instruction to re-run the mod installer. Rendered rather than sent, since
     * a kick screen is not chat and must not carry the chat prefix.
     */
    private void fail(Player player, MessageKey reason,
                      Map<String, String> placeholders, String consoleReason) {
        Component message = messages.render(reason, placeholders)
                .append(Component.newline())
                .append(Component.newline())
                .append(messages.render(MessageKey.MODCHECK_INSTALLER_HINT));

        logger.info("Mod dogrulamasi basarisiz - " + player.getName() + ": " + consoleReason + ".");
        player.getScheduler().run(plugin, task -> player.kick(message), null);
    }

    @Override
    public void forget(UUID playerId) {
        ModVerificationState state = states.remove(playerId);
        if (state != null) {
            state.resolve();
        }
    }

    @Override
    public Optional<ModVerificationSnapshot> snapshot(UUID playerId) {
        return Optional.ofNullable(states.get(playerId)).map(ModVerificationState::toSnapshot);
    }
}
