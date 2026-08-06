package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeRewardType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The screen a prestige opens: two permanent rewards, pick one.
 *
 * <p>Shown immediately after prestiging and, until the choice is spent, in
 * place of every other MMO screen the player tries to open
 * ({@link PrestigeRewardGate}). That is deliberate rather than aggressive: the
 * choice is the reward, and a player who dismisses the window and forgets about
 * it has silently thrown away the payout for hours of grinding. There is no
 * refusing it and nothing else to decide, so leaving it open is the honest
 * shape.</p>
 *
 * <p>A player holding more than one unspent choice - possible after an admin
 * edits their rank, and after ten prestiges in a row if they never looked -
 * simply sees the screen again after each pick, with the counter one lower.</p>
 */
public final class PrestigeRewardMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    /**
     * Two slots, either side of the middle, so the screen reads as a fork
     * rather than as a row with gaps in it. Sized from
     * {@link PrestigeRewardType#SELECTABLE_COUNT} at render time, so this array
     * is the layout and that constant is the count - they cannot disagree.
     */
    private static final int[] REWARD_SLOTS = {20, 24};

    private final GuiServices services;
    private final Map<Integer, PrestigeRewardType> slotToReward = new LinkedHashMap<>();
    private volatile int prestige;

    public PrestigeRewardMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_PRESTIGE_REWARD_TITLE);
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.pane(Material.PURPLE_STAINED_GLASS_PANE), GuiItems.filler());
        services.profiles().load(player.getUniqueId(), player.getName()).thenAccept(profile ->
                player.getScheduler().run(services.plugin(), task -> {
                    prestige = profile.prestige();
                    render(player, inventory);
                }, null));
    }

    private void render(Player player, Inventory inventory) {
        int pending = services.prestigeRewards().pendingChoices(player.getUniqueId(), prestige);
        Map<PrestigeRewardType, Integer> owned = services.prestigeRewards().cached(player.getUniqueId());
        inventory.setItem(HEADER_SLOT, GuiItems.of(Material.NETHER_STAR,
                services.messages().render(MessageKey.GUI_PRESTIGE_REWARD_HEADER),
                List.of(
                        services.messages().render(MessageKey.GUI_PRESTIGE_REWARD_HEADER_LORE, Map.of(
                                "prestij", Integer.toString(prestige),
                                "hak", Integer.toString(pending)
                        )),
                        services.messages().render(MessageKey.GUI_PRESTIGE_REWARD_HEADER_HINT)
                )));

        slotToReward.clear();
        List<PrestigeRewardType> types = PrestigeRewardType.selectable();
        for (int index = 0; index < types.size() && index < REWARD_SLOTS.length; index++) {
            PrestigeRewardType type = types.get(index);
            slotToReward.put(REWARD_SLOTS[index], type);
            inventory.setItem(REWARD_SLOTS[index], rewardItem(type, owned.getOrDefault(type, 0)));
        }
    }

    private ItemStack rewardItem(PrestigeRewardType type, int alreadyOwned) {
        double value = services.prestigeRewards().values().getOrDefault(type, 0.0D);
        List<Component> lore = new ArrayList<>();
        lore.add(services.messages().render(type.loreKey(), Map.of("deger", displayValue(type, value))));
        lore.add(services.messages().render(MessageKey.GUI_PRESTIGE_REWARD_OWNED, Map.of(
                "sayi", Integer.toString(alreadyOwned),
                "toplam", displayValue(type, value * alreadyOwned)
        )));
        lore.add(services.messages().render(MessageKey.GUI_PRESTIGE_REWARD_PICK_HINT));
        return GuiItems.of(type.icon(), services.messages().render(type.nameKey()), lore, alreadyOwned > 0);
    }

    /**
     * Each reward is stated in the unit a player already reads it in: hearts on
     * the HUD, a percentage for the two fractional bonuses, a plain number for
     * energy. The configured value is half-heart points and 0-1 fractions -
     * that translation is this screen's job, exactly as it is the skill
     * milestone and talent screens' job.
     */
    private String displayValue(PrestigeRewardType type, double value) {
        return switch (type) {
            case CAN -> format(value / 2);
            case ENERJI -> format(value);
            case HASAR, BILGELIK -> format(value * 100);
        };
    }

    private String format(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        PrestigeRewardType type = slotToReward.get(slot);
        if (type == null) {
            return;
        }
        services.prestigeRewards().choose(player.getUniqueId(), type, prestige).thenAccept(chosen ->
                player.getScheduler().run(services.plugin(), task -> {
                    if (!chosen) {
                        // Only reachable when a second click beat the repaint;
                        // the gate below decides what happens next either way.
                        return;
                    }
                    services.messages().send(player, MessageKey.PRESTIGE_REWARD_CHOSEN, Map.of(
                            "odul", services.messages().plain(type.nameKey(), Map.of())
                    ));
                    services.feedback().celebrateTalentUnlocked(player);
                    services.prestigeRewards().applyPlayerEffects(player);
                    services.energy().reconcile(player);
                    services.sidebar().refresh(player);
                    // Straight back to the hub once the last choice is spent -
                    // the guard sends them here again if any remain.
                    GuiManager.open(player, new AnaMenu(services));
                }, null));
    }
}
