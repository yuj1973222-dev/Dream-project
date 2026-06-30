package me.leeseol.town.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ChatMode;
import me.leeseol.town.service.TownService;
import me.leeseol.town.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class TownChatListener implements Listener {
    private final LeeSeolTownPlugin plugin;
    private final TownService townService;

    public TownChatListener(LeeSeolTownPlugin plugin, TownService townService) {
        this.plugin = plugin;
        this.townService = townService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatMode mode = townService.chatMode(player);
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (mode == ChatMode.GLOBAL) {
                townService.broadcastGlobalChat(player, event.message());
                return;
            }
            if (mode == ChatMode.TOWN) {
                townService.sendTownChat(player, event.message());
            } else if (mode == ChatMode.NATION) {
                townService.sendNationChat(player, event.message());
            } else {
                player.sendMessage(Text.component("&cUnknown chat mode."));
            }
        });
    }
}
