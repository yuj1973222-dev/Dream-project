package me.leeseol.ranks;

import me.leeseol.ranks.command.RankAdminCommand;
import me.leeseol.ranks.command.RankInfoCommand;
import me.leeseol.ranks.command.RankUpCommand;
import me.leeseol.ranks.hook.RankPlaceholderExpansion;
import me.leeseol.ranks.listener.RankListener;
import me.leeseol.ranks.model.Rank;
import me.leeseol.ranks.service.RankRequirementService;
import me.leeseol.ranks.storage.PermissionService;
import me.leeseol.ranks.storage.RankStore;
import me.leeseol.ranks.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolRanksPlugin extends JavaPlugin {
    private RankStore rankStore;
    private PermissionService permissionService;
    private RankRequirementService rankRequirementService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        rankStore = new RankStore(this);
        permissionService = new PermissionService(this);
        rankRequirementService = new RankRequirementService(this);
        rankStore.load();
        rankRequirementService.reload();
        getServer().getPluginManager().registerEvents(new RankListener(this), this);
        RankUpCommand rankUpCommand = new RankUpCommand(this);
        getCommand("rank").setExecutor(new RankInfoCommand(this, rankUpCommand));
        getCommand("rankup").setExecutor(rankUpCommand);
        getCommand("leeseolrank").setExecutor(new RankAdminCommand(this));
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new RankPlaceholderExpansion(this).register();
            getLogger().info("Registered PlaceholderAPI expansion: leeseolranks");
        }
        for (Player player : getServer().getOnlinePlayers()) {
            permissionService.apply(player);
            permissionService.syncGlobalRank(player.getName(), rankStore.getOrCreate(player).rank());
        }
        getLogger().info("LeeSeolRanks enabled. players=" + rankStore.all().size());
    }

    @Override
    public void onDisable() {
        if (permissionService != null) {
            permissionService.clearAll();
        }
        if (rankStore != null) {
            rankStore.save();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        rankStore = new RankStore(this);
        rankStore.load();
        rankRequirementService.reload();
        for (Player player : getServer().getOnlinePlayers()) {
            permissionService.apply(player);
            permissionService.syncGlobalRank(player.getName(), rankStore.getOrCreate(player).rank());
        }
    }

    public int requiredKills(Rank rank) {
        if (rank == null || !rank.progressionRank() || rank == Rank.PLAYER) {
            return 0;
        }
        return rankRequirementService.requiredKills(rank);
    }

    public RankStore rankStore() {
        return rankStore;
    }

    public PermissionService permissionService() {
        return permissionService;
    }

    public RankRequirementService rankRequirementService() {
        return rankRequirementService;
    }

    public void message(CommandSender sender, String key, String... replacements) {
        String prefix = getConfig().getString("messages.prefix", "");
        String message = getConfig().getString("messages." + key, "");
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }
        Text.send(sender, prefix + message);
    }

}
