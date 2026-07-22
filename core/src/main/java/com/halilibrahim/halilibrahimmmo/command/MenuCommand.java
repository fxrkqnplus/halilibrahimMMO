package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.gui.AnaMenu;
import com.halilibrahim.halilibrahimmmo.gui.GuiManager;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingTask;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Opens the main MMO menu via {@code /menu}. */
public final class MenuCommand implements BasicCommand {

    private final GuiServices services;

    public MenuCommand(GuiServices services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            services.messages().send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        GuiManager.open(player, new AnaMenu(services));
        completeOnboarding(player, OnboardingTask.ILK_MENU_ACMA, MessageKey.ONBOARDING_MENU_COMPLETED);
    }

    private void completeOnboarding(Player player, OnboardingTask task, MessageKey completedMessage) {
        services.onboarding().complete(player.getUniqueId(), player.getName(), task).thenAccept(newlyCompleted -> {
            if (!newlyCompleted) {
                return;
            }
            String classId = services.profiles().cached(player.getUniqueId()).map(profile -> profile.selectedClass()).orElse("");
            if (!classId.isEmpty()) {
                services.classes().awardClassExperience(player.getUniqueId(), player.getName(), classId, task.rewardExperience());
            }
            player.getScheduler().run(services.plugin(), scheduledTask -> {
                services.messages().send(player, completedMessage, Map.of("xp", Long.toString(task.rewardExperience())));
                services.sidebar().refresh(player);
            }, null);
        });
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return List.of();
    }
}
