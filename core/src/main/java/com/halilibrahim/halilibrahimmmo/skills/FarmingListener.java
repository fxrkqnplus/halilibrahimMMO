package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.Set;

/**
 * Awards the tarımcılık skill for everything around the field that is not a
 * block break. Harvesting itself goes through the ordinary block path in
 * {@code SkillListener} - crops are listed as tarımcılık's own block materials
 * there, exempt from the player-placed gate but only counted once fully grown.
 */
public final class FarmingListener implements Listener {

    private static final Set<Material> TILLABLE = Set.of(
            Material.DIRT, Material.GRASS_BLOCK, Material.DIRT_PATH,
            Material.COARSE_DIRT, Material.ROOTED_DIRT, Material.PODZOL, Material.MYCELIUM
    );

    private final JavaPlugin plugin;
    private final SkillService skills;
    private final ActivationService activation;
    private final SkillAwardHandler awards;

    public FarmingListener(JavaPlugin plugin, SkillService skills, ActivationService activation, SkillAwardHandler awards) {
        this.plugin = plugin;
        this.skills = skills;
        this.activation = activation;
        this.awards = awards;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTrade(PlayerTradeEvent event) {
        if (!isEligible(event.getPlayer())) {
            return;
        }
        award(event.getPlayer(), SkillAction.KOYLU_TICARETI, null);
    }

    /**
     * Only crops count. Bone meal also works on grass, saplings and several
     * other blocks, and none of those are farming - {@link Ageable} is exactly
     * the "this is a growing crop" test.
     */
    @EventHandler(ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        Player player = event.getPlayer();
        if (player == null || !isEligible(player) || !(event.getBlock().getBlockData() instanceof Ageable)) {
            return;
        }
        award(player, SkillAction.KEMIK_TOZU, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !isEligible(player)) {
            return;
        }
        award(player, SkillAction.EKMEK_URETIMI, event.getRecipe().getResult().getType());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        if (!isEligible(event.getPlayer())) {
            return;
        }
        award(event.getPlayer(), SkillAction.URUN_PISIRME, event.getItemType());
    }

    /**
     * There is no dedicated "soil was tilled" event, so this checks one tick
     * later whether the block actually became farmland. That turns a right
     * click that merely <em>looked</em> like tilling (a hoe on a block another
     * plugin protected, or on soil that cannot be tilled from below) into a
     * no-op, instead of paying out for every click of a hoe against the ground.
     */
    @EventHandler(ignoreCancelled = true)
    public void onTill(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        if (block == null || !TILLABLE.contains(block.getType())
                || !Tag.ITEMS_HOES.isTagged(player.getInventory().getItemInMainHand().getType())
                || !isEligible(player)) {
            return;
        }
        player.getScheduler().runDelayed(plugin, task -> {
            if (block.getType() == Material.FARMLAND) {
                award(player, SkillAction.CAPA_KULLANIMI, null);
            }
        }, null, 1L);
    }

    private boolean isEligible(Player player) {
        return activation.cached(player.getUniqueId()) && SkillAwardHandler.isGameplayGameMode(player);
    }

    private void award(Player player, SkillAction action, Material context) {
        skills.awardAction(player.getUniqueId(), player.getName(), action, context)
                .thenAccept((Optional<SkillAwardResult> result) -> awards.handle(player, result));
    }
}
