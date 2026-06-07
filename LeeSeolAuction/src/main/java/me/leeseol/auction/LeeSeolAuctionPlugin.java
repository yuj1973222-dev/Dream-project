package me.leeseol.auction;

import me.leeseol.auction.command.AuctionCommand;
import me.leeseol.auction.economy.VaultEconomyHook;
import me.leeseol.auction.gui.AuctionGui;
import me.leeseol.auction.service.AuctionService;
import me.leeseol.auction.storage.AuctionStore;
import me.leeseol.auction.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class LeeSeolAuctionPlugin extends JavaPlugin {
    private AuctionStore auctionStore;
    private VaultEconomyHook economy;
    private AuctionService auctionService;
    private AuctionGui auctionGui;
    private BukkitTask guiRefreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!auctionEnabled()) {
            registerDisabledCommand();
            getLogger().info("LeeSeolAuction is disabled by config.");
            return;
        }

        createServices();
        registerCommands();
        getServer().getPluginManager().registerEvents(auctionGui, this);
        startGuiRefreshTask();
        getLogger().info("LeeSeolAuction enabled. submitted=" + auctionService.submittedCount());
    }

    @Override
    public void onDisable() {
        if (guiRefreshTask != null) {
            guiRefreshTask.cancel();
            guiRefreshTask = null;
        }
        if (auctionService != null) {
            auctionService.save();
        }
    }

    public void reloadAll() {
        reloadConfig();
        if (auctionService != null) {
            auctionService.reload();
        }
    }

    private void createServices() {
        this.auctionStore = new AuctionStore(this);
        this.economy = new VaultEconomyHook(this);
        this.auctionService = new AuctionService(this, auctionStore, economy);
        this.auctionGui = new AuctionGui(this, auctionService);
        this.auctionService.reload();
    }

    private void registerCommands() {
        AuctionCommand executor = new AuctionCommand(this, auctionService, auctionGui);
        PluginCommand command = getCommand("auction");
        if (command == null) {
            getLogger().warning("Command is missing from plugin.yml: auction");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerDisabledCommand() {
        PluginCommand command = getCommand("auction");
        if (command == null) {
            getLogger().warning("Command is missing from plugin.yml: auction");
            return;
        }
        command.setExecutor((sender, ignoredCommand, ignoredLabel, ignoredArgs) -> {
            sender.sendMessage(msg("disabled"));
            return true;
        });
        command.setTabCompleter((sender, ignoredCommand, ignoredAlias, ignoredArgs) -> java.util.Collections.emptyList());
    }

    private void startGuiRefreshTask() {
        long period = Math.max(20L, getConfig().getLong("settings.gui-refresh-ticks", 40L));
        guiRefreshTask = getServer().getScheduler().runTaskTimer(this, auctionGui::refreshOpenMainMenus, period, period);
    }

    public String msg(String key) {
        return Text.color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages." + key, ""));
    }

    public boolean auctionEnabled() {
        return getConfig().getBoolean("settings.enabled", true);
    }

    public boolean auctionAvailable(Player player) {
        if (!auctionEnabled()) {
            return false;
        }
        String worldName = player.getWorld().getName();
        for (String blockedWorld : getConfig().getStringList("settings.blocked-worlds")) {
            if (worldName.equalsIgnoreCase(blockedWorld)
                    && !player.hasPermission(getConfig().getString("settings.blocked-world-bypass-permission", "leeseolauction.world-bypass"))) {
                return false;
            }
        }
        return true;
    }

    public long defaultStartingBid() {
        return getConfig().getLong("settings.default-starting-bid", 1000L);
    }

    public long bidIncrement() {
        return getConfig().getLong("settings.bid-increment", 500L);
    }
}
