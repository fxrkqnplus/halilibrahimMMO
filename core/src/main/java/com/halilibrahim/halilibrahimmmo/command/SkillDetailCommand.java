package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.gui.GuiManager;
import com.halilibrahim.halilibrahimmmo.gui.GuiServices;
import com.halilibrahim.halilibrahimmmo.gui.SkillDetailMenu;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingTask;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** GUI-first, per-skill command ({@code /madencilik}, {@code /odunculuk}, {@code /kazicilik}, {@code /avcilik}). */
public final class SkillDetailCommand implements BasicCommand {

    private final GuiServices services;
    private final String skillId;

    public SkillDetailCommand(GuiServices services, String skillId) {
        this.services = services;
        this.skillId = skillId;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            services.messages().send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        GuiManager.open(player, new SkillDetailMenu(services, skillId));
        completeOnboarding(player);
    }

    /** Any one of the 4 skill commands satisfies this - not "use all 4". */
    private void completeOnboarding(Player player) {
        services.onboarding().complete(player.getUniqueId(), player.getName(), OnboardingTask.BECERI_KOMUTU_KULLANMA).thenAccept(newlyCompleted -> {
            if (!newlyCompleted) {
                return;
            }
            String classId = services.profiles().cached(player.getUniqueId()).map(profile -> profile.selectedClass()).orElse("");
            if (!classId.isEmpty()) {
                services.classes().awardClassExperience(player.getUniqueId(), player.getName(), classId, OnboardingTask.BECERI_KOMUTU_KULLANMA.rewardExperience());
            }
            player.getScheduler().run(services.plugin(), task -> {
                services.messages().send(player, MessageKey.ONBOARDING_COMMAND_COMPLETED, Map.of("xp", Long.toString(OnboardingTask.BECERI_KOMUTU_KULLANMA.rewardExperience())));
                services.sidebar().refresh(player);
            }, null);
        });
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return List.of();
    }
}
