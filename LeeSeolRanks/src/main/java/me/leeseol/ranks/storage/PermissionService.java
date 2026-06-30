package me.leeseol.ranks.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.leeseol.ranks.LeeSeolRanksPlugin;
import me.leeseol.ranks.model.Rank;
import me.leeseol.ranks.model.RankData;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

public final class PermissionService {
    private static final Set<String> LEGACY_VISUAL_PERMISSIONS = Set.of(
            "betterranks.player",
            "betterranks.admin",
            "leeseolranks.admin",
            "leeseolranks.dev"
    );

    private final LeeSeolRanksPlugin plugin;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public PermissionService(LeeSeolRanksPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player) {
        clear(player);
        RankData data = plugin.rankStore().getOrCreate(player);
        PermissionAttachment attachment = player.addAttachment(plugin);
        for (Rank rank : Rank.values()) {
            attachment.setPermission(rank.permission(), rank == data.rank());
        }
        attachment.setPermission("leeseolranks.dev", data.rank() == Rank.DEV);
        attachment.setPermission("leeseolranks.admin", data.rank() == Rank.ADMIN || data.rank() == Rank.DEV);
        attachment.setPermission("betterranks.player", data.rank() == Rank.PLAYER);
        attachment.setPermission("betterranks.admin", data.rank() == Rank.ADMIN || data.rank() == Rank.DEV);
        if (data.rank().staff()) {
            for (String permission : staffAdminPermissions()) {
                attachment.setPermission(permission, true);
            }
        }
        attachments.put(player.getUniqueId(), attachment);
        player.recalculatePermissions();
    }

    public void syncGlobalRank(String playerName, Rank rank) {
        if (!plugin.getConfig().getBoolean("luckperms-sync.enabled", true)) {
            return;
        }
        if (playerName == null || playerName.isBlank()) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            plugin.getLogger().warning("LuckPerms is not enabled; skipped global rank sync for " + playerName);
            return;
        }

        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (Rank existingRank : Rank.values()) {
            runLuckPerms(console, "lp user " + playerName + " permission unset " + existingRank.permission());
        }
        for (String permission : LEGACY_VISUAL_PERMISSIONS) {
            runLuckPerms(console, "lp user " + playerName + " permission unset " + permission);
        }
        for (String permission : staffAdminPermissions()) {
            runLuckPerms(console, "lp user " + playerName + " permission unset " + permission);
        }
        runLuckPerms(console, "lp user " + playerName + " permission set " + rank.permission() + " true");
        if (rank == Rank.PLAYER) {
            runLuckPerms(console, "lp user " + playerName + " permission set betterranks.player true");
        }
        if (rank == Rank.ADMIN || rank == Rank.DEV) {
            runLuckPerms(console, "lp user " + playerName + " permission set betterranks.admin true");
            runLuckPerms(console, "lp user " + playerName + " permission set leeseolranks.admin true");
            for (String permission : staffAdminPermissions()) {
                runLuckPerms(console, "lp user " + playerName + " permission set " + permission + " true");
            }
        }
        if (rank == Rank.DEV) {
            runLuckPerms(console, "lp user " + playerName + " permission set leeseolranks.dev true");
        } else {
            runLuckPerms(console, "lp user " + playerName + " permission unset leeseolranks.dev");
        }
    }

    private void runLuckPerms(ConsoleCommandSender console, String command) {
        Bukkit.dispatchCommand(console, command);
    }

    private List<String> staffAdminPermissions() {
        return plugin.getConfig().getStringList("staff.admin-permissions").stream()
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .toList();
    }

    public void clear(Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            player.removeAttachment(attachment);
        }
    }

    public void clearAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            clear(player);
        }
    }
}
