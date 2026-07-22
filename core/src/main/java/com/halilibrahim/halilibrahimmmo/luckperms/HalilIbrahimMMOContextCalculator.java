package com.halilibrahim.halilibrahimmmo.luckperms;

import com.halilibrahim.halilibrahimmmo.api.HalilIbrahimMMOAPI;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import org.bukkit.entity.Player;

/**
 * Read-only LuckPerms context provider exposing this plugin's progression
 * state, so server admins can write permission rules conditioned on it
 * (e.g. class or prestige) without this plugin ever writing to LuckPerms
 * itself. Reads exclusively through {@link HalilIbrahimMMOAPI}'s synchronous
 * cached-snapshot methods - LuckPerms recalculates contexts synchronously.
 * {@code halilibrahimmmo:level} is Güç (the sum of every configured skill's
 * level, Phase 11), not an independent XP-tracked level.
 */
public final class HalilIbrahimMMOContextCalculator implements ContextCalculator<Player> {

    private final HalilIbrahimMMOAPI api;

    public HalilIbrahimMMOContextCalculator(HalilIbrahimMMOAPI api) {
        this.api = api;
    }

    @Override
    public void calculate(Player target, ContextConsumer consumer) {
        api.findCachedPlayer(target.getUniqueId()).ifPresent(snapshot -> {
            consumer.accept("halilibrahimmmo:level", Integer.toString(snapshot.globalLevel()));
            consumer.accept("halilibrahimmmo:prestige", Integer.toString(snapshot.prestige()));
            if (!snapshot.selectedClassId().isEmpty()) {
                consumer.accept("halilibrahimmmo:class", snapshot.selectedClassId());
            }
        });
    }
}
