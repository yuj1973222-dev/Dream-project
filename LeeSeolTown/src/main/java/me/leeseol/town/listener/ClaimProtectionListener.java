package me.leeseol.town.listener;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.Town;
import me.leeseol.town.service.TownService;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public final class ClaimProtectionListener implements Listener {
    private final LeeSeolTownPlugin plugin;
    private final TownService townService;

    public ClaimProtectionListener(LeeSeolTownPlugin plugin, TownService townService) {
        this.plugin = plugin;
        this.townService = townService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        ClaimKey claim = ClaimKey.from(event.getBlock().getLocation());
        if (townService.canBuild(event.getPlayer(), claim)) {
            return;
        }
        event.setCancelled(true);
        Town owner = townService.claimTown(claim);
        event.getPlayer().sendMessage(plugin.msg("protected").replace("%town%", owner == null ? "unknown" : owner.name()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ClaimKey claim = ClaimKey.from(event.getBlockPlaced().getLocation());
        if (event.getBlockPlaced().getType() == Material.BEACON && plugin.structureRegistry().enabled()) {
            Nation nation = townService.playerNation(event.getPlayer());
            if (nation != null && nation.beaconClaim() == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.msg("nation-core-custom-required"));
                return;
            }
        }
        if (event.getBlockPlaced().getType() == Material.BEACON
                && townService.shouldCancelNationBeaconPlace(event.getPlayer(), claim)) {
            event.setCancelled(true);
            return;
        }
        if (townService.canBuild(event.getPlayer(), claim)) {
            return;
        }
        event.setCancelled(true);
        Town owner = townService.claimTown(claim);
        event.getPlayer().sendMessage(plugin.msg("protected").replace("%town%", owner == null ? "unknown" : owner.name()));
    }
}
