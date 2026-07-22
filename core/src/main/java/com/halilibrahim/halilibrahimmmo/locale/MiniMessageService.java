package com.halilibrahim.halilibrahimmmo.locale;

import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

/**
 * Loads {@code messages.yml} and renders every {@link MessageKey} through
 * {@link MiniMessage}. Like {@code config.yml}, the bundled Turkish resource is
 * copied verbatim on first run and never rewritten in place.
 */
public final class MiniMessageService implements MessageService {

    private static final String FILE_NAME = "messages.yml";

    private final JavaPlugin plugin;
    private final PluginLogger logger;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private volatile Map<MessageKey, String> templates = new EnumMap<>(MessageKey.class);

    public MiniMessageService(JavaPlugin plugin, PluginLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public void enable() {
        loadFromDisk();
    }

    @Override
    public void disable() {
        // Nothing to release - templates are just an in-memory snapshot.
    }

    @Override
    public void reload() {
        loadFromDisk();
    }

    @Override
    public Component render(MessageKey key, Map<String, String> placeholders) {
        String template = templates.getOrDefault(key, key.path());

        TagResolver.Builder resolverBuilder = TagResolver.builder();
        placeholders.forEach((name, value) -> resolverBuilder.resolver(Placeholder.unparsed(name, value)));

        return miniMessage.deserialize(template, resolverBuilder.build());
    }

    @Override
    public Component render(MessageKey key) {
        return render(key, Map.of());
    }

    @Override
    public void send(Audience audience, MessageKey key, Map<String, String> placeholders) {
        audience.sendMessage(prefixed(render(key, placeholders)));
    }

    @Override
    public void send(Audience audience, MessageKey key) {
        send(audience, key, Map.of());
    }

    @Override
    public void broadcast(MessageKey key, Map<String, String> placeholders) {
        Component rendered = prefixed(render(key, placeholders));
        plugin.getServer().getOnlinePlayers().forEach(player -> player.sendMessage(rendered));
    }

    @Override
    public Component prefixed(Component message) {
        return render(MessageKey.PREFIX).append(message);
    }

    private void loadFromDisk() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<MessageKey, String> loaded = new EnumMap<>(MessageKey.class);

        for (MessageKey key : MessageKey.values()) {
            String raw = yaml.getString(key.path());
            if (raw == null) {
                logger.warn("messages.yml icinde eksik mesaj anahtari: '" + key.path()
                        + "' - eklenti bu anahtar yerine gecici bir metin kullanacak.");
                raw = key.path();
            }
            loaded.put(key, raw);
        }

        this.templates = loaded;
    }
}
