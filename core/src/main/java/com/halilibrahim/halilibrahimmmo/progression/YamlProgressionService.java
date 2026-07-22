package com.halilibrahim.halilibrahimmmo.progression;

import com.halilibrahim.halilibrahimmmo.prestige.BankedPowerService;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Güç calculator: sums every configured skill's level, live from
 * {@link SkillService}, plus any Güç permanently banked across past
 * prestiges ({@link BankedPowerService}) - so a prestige reset (which drops
 * every skill back to level 1) never lowers a player's Güç.
 */
public final class YamlProgressionService implements ProgressionService {

    private final SkillService skills;
    private final BankedPowerService bankedPower;

    public YamlProgressionService(SkillService skills, BankedPowerService bankedPower) {
        this.skills = skills;
        this.bankedPower = bankedPower;
    }

    @Override
    public void enable() {
        // Nothing to load - Güç has no configuration or persistence of its own.
    }

    @Override
    public void disable() {
        // Nothing to release.
    }

    @Override
    public int cachedGlobalLevel(UUID playerId) {
        int total = bankedPower.cached(playerId);
        for (var definition : skills.definitions()) {
            total += skills.cached(playerId, definition.id()).map(progress -> progress.level()).orElse(1);
        }
        return total;
    }

    @Override
    public CompletableFuture<Integer> globalLevel(UUID playerId) {
        CompletableFuture<Integer> total = bankedPower.load(playerId).thenApply(Integer::intValue);
        for (var definition : skills.definitions()) {
            total = total.thenCompose(sum -> skills.load(playerId, definition.id())
                    .thenApply(progress -> sum + progress.level()));
        }
        return total;
    }
}
