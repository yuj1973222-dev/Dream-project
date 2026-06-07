package me.leeseol.quest.listener;

import me.leeseol.quest.LeeSeolQuestPlugin;
import me.leeseol.quest.gui.QuestGui;
import me.leeseol.quest.model.ObjectiveType;
import me.leeseol.quest.service.QuestService;
import org.bukkit.ChatColor;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class QuestObjectiveListener implements Listener {
    private final LeeSeolQuestPlugin plugin;
    private final QuestService questService;
    private final QuestGui questGui;

    public QuestObjectiveListener(LeeSeolQuestPlugin plugin, QuestService questService, QuestGui questGui) {
        this.plugin = plugin;
        this.questService = questService;
        this.questGui = questGui;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> questService.autoStartTutorial(event.getPlayer()), 40L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        questGui.handleClick(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        questService.progressObjective(event.getPlayer(), ObjectiveType.MINE_BLOCK, null, event.getBlock().getType(), 1);
        if (event.getBlock().getBlockData() instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge()) {
            questService.progressObjective(
                event.getPlayer(),
                ObjectiveType.HARVEST_CROP,
                event.getBlock().getType().name(),
                event.getBlock().getType(),
                1
            );
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            questService.progressObjective(event.getPlayer(), ObjectiveType.FISH, null, null, 1);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && killer != victim) {
            questService.progressObjective(killer, ObjectiveType.KILL_PLAYER, victim.getName(), null, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onNpcClick(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!entity.hasMetadata("NPC")) {
            return;
        }
        String name = ChatColor.stripColor(entity.getName());
        questService.progressObjective(event.getPlayer(), ObjectiveType.NPC_DIALOGUE, name, null, 1);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        questService.progressObjective(player, ObjectiveType.DUNGEON_ENTER, player.getWorld().getName(), null, 1);
    }
}
