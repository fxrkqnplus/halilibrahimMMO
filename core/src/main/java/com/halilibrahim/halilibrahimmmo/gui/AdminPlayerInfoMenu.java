package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import com.halilibrahim.halilibrahimmmo.skills.SkillProgress;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-only summary of one online player's profile/class/skills for {@code /haliladmin}. */
public final class AdminPlayerInfoMenu implements GuiView {

    private static final int INFO_SLOT = 13;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;
    private final Player target;

    public AdminPlayerInfoMenu(GuiServices services, Player target) {
        this.services = services;
        this.target = target;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ADMIN_PLAYER_INFO_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        Optional<PlayerProfile> cached = services.profiles().cached(target.getUniqueId());
        if (cached.isEmpty()) {
            return;
        }
        PlayerProfile profile = cached.get();
        String skillList = services.skills().definitions().stream()
                .map(definition -> definition.displayName() + " " + skillLevelOf(definition))
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
        String guc = Integer.toString(services.progression().cachedGlobalLevel(target.getUniqueId()));
        Component lore = profile.selectedClass().isEmpty()
                ? services.messages().render(MessageKey.ADMIN_PLAYER_INFO_NO_CLASS, Map.of(
                        "oyuncu", profile.playerName(),
                        "seviye", guc,
                        "prestij", Integer.toString(profile.prestige()),
                        "beceriler", skillList
                ))
                : services.messages().render(MessageKey.ADMIN_PLAYER_INFO, Map.of(
                        "oyuncu", profile.playerName(),
                        "seviye", guc,
                        "prestij", Integer.toString(profile.prestige()),
                        "sinif", services.classes().displayName(profile.selectedClass()),
                        "beceriler", skillList
                ));
        inventory.setItem(INFO_SLOT, GuiItems.of(Material.PLAYER_HEAD, Component.text(profile.playerName()), List.of(lore)));
    }

    private int skillLevelOf(SkillDefinition definition) {
        return services.skills().cached(target.getUniqueId(), definition.id()).map(SkillProgress::level).orElse(1);
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AdminOnlinePlayersMenu(services));
        }
    }
}
