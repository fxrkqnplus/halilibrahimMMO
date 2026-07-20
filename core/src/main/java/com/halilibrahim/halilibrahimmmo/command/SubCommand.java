package com.halilibrahim.halilibrahimmmo.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * One branch of an umbrella {@code BasicCommand} (e.g. {@code yenile} under
 * {@code /rpg}). Kept deliberately tiny so future phases can add dozens of
 * subcommands under {@code /rpg} and other root commands without RpgCommand-style
 * classes ever growing into a god-class.
 */
public interface SubCommand {

    /**
     * @return the literal typed by the player after the root command, e.g. {@code "yenile"}.
     * Always lowercase ASCII - matched case-insensitively.
     */
    String name();

    /**
     * @return the permission node required to run this subcommand.
     */
    String permission();

    /**
     * @param args arguments after the subcommand literal itself (i.e. with {@code args[0]}
     *             from the root command already stripped).
     */
    void execute(CommandSourceStack sourceStack, String[] args);
}
