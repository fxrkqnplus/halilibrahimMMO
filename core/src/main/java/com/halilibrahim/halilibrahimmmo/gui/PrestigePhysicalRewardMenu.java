package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigePhysicalReward;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The second half of a prestige: a crate of real items, and it must be taken.
 *
 * <p>The permanent reward makes a number go up forever, which is the right
 * shape for a milestone and completely invisible in the moment. This is the
 * other half - something in the hands, right now, that the player chose. Both
 * are compulsory and the gate opens them one after the other, permanent first,
 * because the permanent one is the decision worth thinking about and the crate
 * is the celebration.</p>
 *
 * <p>The crates themselves are entirely {@code prestige.yml}: a server owner
 * adds, removes or retunes one without a line of Java, and the parser refuses to
 * load a file offering fewer than two (a choice of one is not a choice) or a
 * crate containing nothing.</p>
 */
public final class PrestigePhysicalRewardMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int[] REWARD_SLOTS = {19, 20, 21, 22, 23, 24, 25};

    private final GuiServices services;
    private final Map<Integer, String> slotToReward = new LinkedHashMap<>();
    private volatile int prestige;

    public PrestigePhysicalRewardMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_PRESTIGE_PHYSICAL_TITLE);
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.pane(Material.YELLOW_STAINED_GLASS_PANE), GuiItems.filler());
        services.profiles().load(player.getUniqueId(), player.getName()).thenAccept(profile ->
                player.getScheduler().run(services.plugin(), task -> {
                    prestige = profile.prestige();
                    render(player, inventory);
                }, null));
    }

    private void render(Player player, Inventory inventory) {
        int pending = services.prestigeRewards().pendingPhysicalChoices(player.getUniqueId(), prestige);
        inventory.setItem(HEADER_SLOT, GuiItems.of(Material.CHEST,
                services.messages().render(MessageKey.GUI_PRESTIGE_PHYSICAL_HEADER),
                List.of(
                        services.messages().render(MessageKey.GUI_PRESTIGE_PHYSICAL_HEADER_LORE, Map.of(
                                "prestij", Integer.toString(prestige),
                                "hak", Integer.toString(pending)
                        )),
                        services.messages().render(MessageKey.GUI_PRESTIGE_PHYSICAL_HEADER_HINT)
                )));

        slotToReward.clear();
        int index = 0;
        for (PrestigePhysicalReward reward : services.prestige().settings().physicalRewards().values()) {
            if (index >= REWARD_SLOTS.length) {
                break;
            }
            slotToReward.put(REWARD_SLOTS[index], reward.id());
            inventory.setItem(REWARD_SLOTS[index++], crateItem(reward));
        }
    }

    /** The crate, with its whole contents listed - nobody should have to guess what is inside. */
    private ItemStack crateItem(PrestigePhysicalReward reward) {
        List<Component> lore = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : reward.contents()) {
            lore.add(services.messages().render(MessageKey.GUI_PRESTIGE_PHYSICAL_ENTRY, Map.of(
                    "adet", Integer.toString(entry.getValue())
            ), Map.of("esya", Component.translatable(entry.getKey().translationKey()))));
        }
        lore.add(services.messages().render(MessageKey.GUI_PRESTIGE_PHYSICAL_PICK_HINT));
        return GuiItems.of(reward.icon(), Component.text(reward.displayName()), lore);
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        String rewardId = slotToReward.get(slot);
        if (rewardId == null) {
            return;
        }
        PrestigePhysicalReward reward = services.prestige().settings().physicalRewards().get(rewardId);
        if (reward == null) {
            return;
        }
        services.prestigeRewards().claimPhysical(player.getUniqueId(), prestige).thenAccept(claimed ->
                player.getScheduler().run(services.plugin(), task -> {
                    if (!claimed) {
                        // A second click that beat the repaint; the gate decides
                        // what happens next either way.
                        return;
                    }
                    give(player, reward);
                    services.messages().send(player, MessageKey.PRESTIGE_PHYSICAL_CHOSEN,
                            Map.of(), Map.of("odul", Component.text(reward.displayName())));
                    services.feedback().celebrateTalentUnlocked(player);
                    GuiManager.open(player, new AnaMenu(services));
                }, null));
    }

    /**
     * Hands the crate over. Anything the inventory cannot take is dropped at the
     * player's feet rather than deleted: a reward for ten hours of play must not
     * be lost to a full hotbar.
     */
    private void give(Player player, PrestigePhysicalReward reward) {
        for (Map.Entry<Material, Integer> entry : reward.contents()) {
            player.getInventory().addItem(new ItemStack(entry.getKey(), entry.getValue())).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }
}
