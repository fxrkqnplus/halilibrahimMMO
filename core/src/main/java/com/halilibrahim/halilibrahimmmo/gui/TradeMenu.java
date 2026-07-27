package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.trade.TradeSession;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * The trade table: your offer down the left, theirs down the right, a confirm
 * button underneath each side.
 *
 * <p>Nothing here stores items. Every stack shown is a copy rendered from
 * {@link TradeSession}, which holds the only real one - clicking your own
 * inventory hands a stack to the session (and the session takes it out of your
 * inventory), clicking one of your offered stacks asks for it back. Because the
 * clicks themselves are cancelled like in every other menu of this plugin,
 * there is never a moment where Bukkit and the session disagree about who owns
 * what.</p>
 *
 * <p>Closing the window ends the trade. That is deliberate rather than a
 * limitation: a trade is a live negotiation, and an empty chair should return
 * everyone's goods immediately instead of leaving items parked in memory.</p>
 */
public final class TradeMenu implements GuiView {

    private static final int SIZE = 54;
    /** Left half: this player's own offer. Right half: their partner's. */
    private static final int[] OWN_SLOTS = {0, 1, 2, 9, 10, 11, 18, 19, 20, 27, 28, 29};
    private static final int[] PARTNER_SLOTS = {6, 7, 8, 15, 16, 17, 24, 25, 26, 33, 34, 35};
    private static final int OWN_STATUS_SLOT = 37;
    private static final int PARTNER_STATUS_SLOT = 43;
    private static final int CONFIRM_SLOT = 40;
    private static final int HINT_SLOT = 4;
    private static final int CANCEL_SLOT = 49;

    private final GuiServices services;
    private final UUID viewerId;

    public TradeMenu(GuiServices services, UUID viewerId) {
        this.services = services;
        this.viewerId = viewerId;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_TRADE_TITLE);
    }

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        render(inventory);
    }

    /** Re-renders in place, so refreshing after every offer never closes and reopens the window. */
    public void render(Inventory inventory) {
        ItemStack divider = GuiItems.border();
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, divider);
        }
        TradeSession session = services.trades().activeSession(viewerId).orElse(null);
        if (session == null) {
            return;
        }
        UUID partnerId = session.partnerOf(viewerId);
        renderOffer(inventory, OWN_SLOTS, session.offerOf(viewerId));
        renderOffer(inventory, PARTNER_SLOTS, session.offerOf(partnerId));

        inventory.setItem(HINT_SLOT, GuiItems.of(Material.BOOK,
                services.messages().render(MessageKey.GUI_TRADE_HINT_ITEM),
                List.of(services.messages().render(MessageKey.GUI_TRADE_HINT_LORE))));
        inventory.setItem(OWN_STATUS_SLOT, statusItem(session.hasConfirmed(viewerId), MessageKey.GUI_TRADE_OWN_STATUS));
        inventory.setItem(PARTNER_STATUS_SLOT, statusItem(session.hasConfirmed(partnerId), MessageKey.GUI_TRADE_PARTNER_STATUS));
        inventory.setItem(CONFIRM_SLOT, GuiItems.of(
                session.hasConfirmed(viewerId) ? Material.LIME_DYE : Material.EMERALD,
                services.messages().render(MessageKey.GUI_TRADE_CONFIRM_ITEM),
                List.of(services.messages().render(MessageKey.GUI_TRADE_CONFIRM_LORE)),
                session.hasConfirmed(viewerId)
        ));
        inventory.setItem(CANCEL_SLOT, GuiItems.of(Material.BARRIER,
                services.messages().render(MessageKey.GUI_TRADE_CANCEL_ITEM)));
    }

    private ItemStack statusItem(boolean confirmed, MessageKey nameKey) {
        return GuiItems.of(
                confirmed ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                services.messages().render(nameKey),
                List.of(services.messages().render(
                        confirmed ? MessageKey.GUI_TRADE_STATUS_READY : MessageKey.GUI_TRADE_STATUS_WAITING))
        );
    }

    private void renderOffer(Inventory inventory, int[] slots, List<ItemStack> offer) {
        for (int index = 0; index < slots.length; index++) {
            inventory.setItem(slots[index], index < offer.size() ? offer.get(index).clone() : GuiItems.filler());
        }
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == CANCEL_SLOT) {
            services.trades().cancel(player);
            return;
        }
        if (slot == CONFIRM_SLOT) {
            services.trades().confirm(player);
            return;
        }
        int offerIndex = indexOf(OWN_SLOTS, slot);
        if (offerIndex >= 0) {
            services.trades().withdraw(player, offerIndex);
        }
    }

    @Override
    public void handleOwnInventoryClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (clicked != null && !clicked.getType().isAir()) {
            services.trades().offer(player, slot);
        }
    }

    /**
     * The window is gone, so the negotiation is over. {@code cancel} is
     * idempotent and already knows when a trade completed normally, which is
     * why closing both windows at the end of a successful trade does not undo
     * it.
     */
    @Override
    public void handleClose(Player player) {
        services.trades().cancel(player);
    }

    private static int indexOf(int[] slots, int slot) {
        for (int index = 0; index < slots.length; index++) {
            if (slots[index] == slot) {
                return index;
            }
        }
        return -1;
    }
}
