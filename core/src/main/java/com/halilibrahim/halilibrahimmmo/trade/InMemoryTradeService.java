package com.halilibrahim.halilibrahimmmo.trade;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.FeatureUnlockRules;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The whole trade system: pending requests, live sessions and the item
 * bookkeeping, all in memory.
 *
 * <p>Every mutating method here is called from the main thread - a command or a
 * GUI click - which is what makes the check-then-act sequences inside them
 * safe without locks, the same thread-confinement argument
 * {@code YamlAbilityService}'s activation path already documents. The maps are
 * concurrent only because the sidebar reads them from the render sweep.</p>
 */
public final class InMemoryTradeService implements TradeService {

    private final ProfileService profiles;
    private final ClassService classes;
    private final ActivationService activation;
    private final MessageService messages;
    private final AtomicReference<TradeScreenRenderer> renderer = new AtomicReference<>();
    private final ConcurrentHashMap<UUID, TradeSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    public InMemoryTradeService(
            ProfileService profiles, ClassService classes, ActivationService activation, MessageService messages
    ) {
        this.profiles = profiles;
        this.classes = classes;
        this.activation = activation;
        this.messages = messages;
    }

    /** Wired once by the plugin bootstrap, after the GUI layer exists. */
    public void bindRenderer(TradeScreenRenderer screenRenderer) {
        renderer.set(screenRenderer);
    }

    @Override
    public void enable() {
        // No YAML settings - reload() stays the PluginService default no-op.
    }

    @Override
    public void disable() {
        // Returning goods here would mean touching inventories during shutdown;
        // instead every session is cancelled by the quit handler as players are
        // disconnected, which runs first and has a live Player to hand.
        sessions.clear();
        pendingRequests.clear();
    }

    @Override
    public boolean isUnlocked(UUID playerId) {
        return meetsClassLevel(playerId, FeatureUnlockRules.TRADE_CLASS_LEVEL);
    }

    private boolean meetsClassLevel(UUID playerId, int requiredLevel) {
        return profiles.cached(playerId).map(profile -> {
            String classId = profile.selectedClass();
            int classLevel = classId.isEmpty() ? 0 : classes.classProgress(profile.playerId(), classId).level();
            return FeatureUnlockRules.isUnlocked(classLevel, profile.prestige(), requiredLevel);
        }).orElse(false);
    }

    @Override
    public Optional<TradeSession> activeSession(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    @Override
    public Optional<String> pendingRequestFrom(UUID playerId) {
        PendingRequest request = pendingRequests.get(playerId);
        if (request == null || request.hasLapsed()) {
            return Optional.empty();
        }
        return Optional.of(request.senderName());
    }

    @Override
    public TradeRequestResult request(Player sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            return TradeRequestResult.PARTNER_OFFLINE;
        }
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            return TradeRequestResult.SELF;
        }
        if (!isUnlocked(sender.getUniqueId())) {
            return TradeRequestResult.LOCKED;
        }
        if (!activation.cached(target.getUniqueId())) {
            return TradeRequestResult.PARTNER_NOT_ACTIVATED;
        }
        if (!meetsClassLevel(target.getUniqueId(), FeatureUnlockRules.TRADE_PARTNER_CLASS_LEVEL)) {
            return TradeRequestResult.PARTNER_LOCKED;
        }
        if (sessions.containsKey(sender.getUniqueId()) || sessions.containsKey(target.getUniqueId())) {
            return TradeRequestResult.BUSY;
        }
        PendingRequest existing = pendingRequests.get(target.getUniqueId());
        if (existing != null && !existing.hasLapsed()) {
            return TradeRequestResult.BUSY;
        }
        pendingRequests.put(target.getUniqueId(), new PendingRequest(
                sender.getUniqueId(), sender.getName(), System.currentTimeMillis() + REQUEST_TIMEOUT_MILLIS
        ));
        messages.send(target, MessageKey.TRADE_REQUEST_RECEIVED, Map.of("oyuncu", sender.getName()));
        return TradeRequestResult.SENT;
    }

    @Override
    public boolean accept(Player player) {
        PendingRequest request = pendingRequests.remove(player.getUniqueId());
        if (request == null || request.hasLapsed()) {
            return false;
        }
        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender == null || sessions.containsKey(sender.getUniqueId()) || sessions.containsKey(player.getUniqueId())) {
            return false;
        }
        TradeSession session = new TradeSession(sender.getUniqueId(), player.getUniqueId());
        sessions.put(sender.getUniqueId(), session);
        sessions.put(player.getUniqueId(), session);
        messages.send(sender, MessageKey.TRADE_STARTED, Map.of("oyuncu", player.getName()));
        messages.send(player, MessageKey.TRADE_STARTED, Map.of("oyuncu", sender.getName()));
        TradeScreenRenderer screens = renderer.get();
        if (screens != null) {
            screens.open(sender, session);
            screens.open(player, session);
        }
        return true;
    }

    @Override
    public boolean decline(Player player) {
        PendingRequest request = pendingRequests.remove(player.getUniqueId());
        if (request == null || request.hasLapsed()) {
            return false;
        }
        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender != null) {
            messages.send(sender, MessageKey.TRADE_DECLINED_BY, Map.of("oyuncu", player.getName()));
        }
        return true;
    }

    @Override
    public void offer(Player player, int inventorySlot) {
        TradeSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isFinished()) {
            return;
        }
        List<ItemStack> offer = session.offerOf(player.getUniqueId());
        if (offer.size() >= TradeSession.MAXIMUM_OFFER_SIZE) {
            messages.send(player, MessageKey.TRADE_OFFER_FULL);
            return;
        }
        ItemStack item = player.getInventory().getItem(inventorySlot);
        if (item == null || item.getType().isAir()) {
            return;
        }
        // Taken out of the inventory rather than mirrored: the session becomes
        // the single authoritative holder of the stack for the trade's lifetime.
        player.getInventory().setItem(inventorySlot, null);
        offer.add(item);
        session.clearConfirmations();
        refresh(session);
    }

    @Override
    public void withdraw(Player player, int offerIndex) {
        TradeSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isFinished()) {
            return;
        }
        List<ItemStack> offer = session.offerOf(player.getUniqueId());
        if (offerIndex < 0 || offerIndex >= offer.size()) {
            return;
        }
        ItemStack item = offer.remove(offerIndex);
        returnItems(player, List.of(item));
        session.clearConfirmations();
        refresh(session);
    }

    @Override
    public void confirm(Player player) {
        TradeSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isFinished()) {
            return;
        }
        session.confirm(player.getUniqueId());
        if (!session.bothConfirmed()) {
            Player partner = Bukkit.getPlayer(session.partnerOf(player.getUniqueId()));
            if (partner != null) {
                messages.send(partner, MessageKey.TRADE_PARTNER_CONFIRMED, Map.of("oyuncu", player.getName()));
            }
            refresh(session);
            return;
        }
        complete(session);
    }

    /**
     * Both sides are ready: each walks away with the other's stacks. Marked
     * finished before anything moves, so the window closes this triggers cannot
     * re-enter {@link #cancel} and hand the goods back as well.
     */
    private void complete(TradeSession session) {
        session.markFinished();
        Player first = Bukkit.getPlayer(session.firstPlayerId());
        Player second = Bukkit.getPlayer(session.secondPlayerId());
        List<ItemStack> firstOffer = session.drain(session.firstPlayerId());
        List<ItemStack> secondOffer = session.drain(session.secondPlayerId());
        deliver(first, secondOffer);
        deliver(second, firstOffer);
        end(session, MessageKey.TRADE_COMPLETED, Map.of());
    }

    /**
     * A player who went offline in the same tick the trade completed still gets
     * their side: the goods are held for nobody, so they go back to whoever is
     * still here rather than vanishing.
     */
    private void deliver(Player recipient, List<ItemStack> items) {
        if (recipient != null) {
            returnItems(recipient, items);
        }
    }

    @Override
    public void cancel(Player player) {
        TradeSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isFinished()) {
            sessions.remove(player.getUniqueId());
            return;
        }
        session.markFinished();
        Player first = Bukkit.getPlayer(session.firstPlayerId());
        Player second = Bukkit.getPlayer(session.secondPlayerId());
        List<ItemStack> firstOffer = session.drain(session.firstPlayerId());
        List<ItemStack> secondOffer = session.drain(session.secondPlayerId());
        deliver(first, firstOffer);
        deliver(second, secondOffer);
        end(session, MessageKey.TRADE_CANCELLED, Map.of());
    }

    private void end(TradeSession session, MessageKey message, Map<String, String> placeholders) {
        sessions.remove(session.firstPlayerId());
        sessions.remove(session.secondPlayerId());
        TradeScreenRenderer screens = renderer.get();
        for (UUID playerId : List.of(session.firstPlayerId(), session.secondPlayerId())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            if (screens != null) {
                screens.close(player);
            }
            messages.send(player, message, placeholders);
        }
    }

    private void refresh(TradeSession session) {
        TradeScreenRenderer screens = renderer.get();
        if (screens != null) {
            screens.refresh(session);
        }
    }

    @Override
    public void returnItems(Player player, Iterable<ItemStack> items) {
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            player.getInventory().addItem(item).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    @Override
    public void evict(UUID playerId) {
        pendingRequests.remove(playerId);
        pendingRequests.values().removeIf(request -> request.senderId().equals(playerId));
        sessions.remove(playerId);
    }

    private record PendingRequest(UUID senderId, String senderName, long expiresAtMillis) {

        boolean hasLapsed() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }
}
