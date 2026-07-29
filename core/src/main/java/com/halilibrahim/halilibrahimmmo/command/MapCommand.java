package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Collection;
import java.util.List;

/**
 * {@code /harita} (alias {@code /map}) hands out the live web-map address.
 *
 * <p>The URL itself lives in {@code messages.yml} rather than in a settings
 * file, because it has to sit inside MiniMessage's {@code click:open_url} tag
 * to be clickable and a tag argument cannot be an unparsed placeholder. One
 * line, one place to change it.</p>
 */
public final class MapCommand implements BasicCommand {

    private final MessageService messages;

    public MapCommand(MessageService messages) {
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        messages.send(stack.getSender(), MessageKey.MAP_LINK);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return List.of();
    }
}
