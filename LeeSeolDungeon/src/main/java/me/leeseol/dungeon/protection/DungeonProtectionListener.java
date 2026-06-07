package me.leeseol.dungeon.protection;

import me.leeseol.dungeon.LeeSeolDungeonPlugin;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public final class DungeonProtectionListener implements Listener {
    private final LeeSeolDungeonPlugin plugin;

    public DungeonProtectionListener(LeeSeolDungeonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        if (isProtected(event.getBlock().getWorld(), event.getPlayer())
                && !plugin.getConfig().getBoolean("dungeon-protection.block-break", false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.msg("protected"));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent event) {
        if (isProtected(event.getBlock().getWorld(), event.getPlayer())
                && !plugin.getConfig().getBoolean("dungeon-protection.block-place", false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.msg("protected"));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (isProtected(event.getBlock().getWorld(), event.getPlayer())
                && !plugin.getConfig().getBoolean("dungeon-protection.buckets", false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.msg("protected"));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (isProtected(event.getBlock().getWorld(), event.getPlayer())
                && !plugin.getConfig().getBoolean("dungeon-protection.buckets", false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.msg("protected"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isProtected(event.getLocation().getWorld(), null)
                && !plugin.getConfig().getBoolean("dungeon-protection.explosions", false)) {
            event.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (isProtected(event.getBlock().getWorld(), null)
                && !plugin.getConfig().getBoolean("dungeon-protection.explosions", false)) {
            event.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (isProtected(event.getBlock().getWorld(), null)
                && !plugin.getConfig().getBoolean("dungeon-protection.mob-griefing-block-change", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (isProtected(event.getBlock().getWorld(), null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (isProtected(event.getBlock().getWorld(), player)) {
            event.setCancelled(true);
        }
    }

    private boolean isProtected(World world, Player player) {
        if (world == null || !plugin.getConfig().getBoolean("dungeon-protection.enabled", true) || !plugin.roleAllows("dungeon")) {
            return false;
        }
        if (player != null
                && plugin.getConfig().getBoolean("dungeon-protection.allow-admin-bypass", true)
                && player.hasPermission("leeseoldungeon.bypass")) {
            return false;
        }
        for (String worldName : plugin.getConfig().getStringList("dungeon-protection.worlds")) {
            if (world.getName().equalsIgnoreCase(worldName)) {
                return true;
            }
        }
        return false;
    }
}
