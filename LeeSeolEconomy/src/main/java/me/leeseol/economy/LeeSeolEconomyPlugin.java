package me.leeseol.economy;

import me.leeseol.economy.command.EconomyCommand;
import me.leeseol.economy.command.MarketCommand;
import me.leeseol.economy.command.MenuBridgeCommand;
import me.leeseol.economy.ledger.EconomyLedger;
import me.leeseol.economy.market.MarketManager;
import me.leeseol.economy.npc.NpcCommand;
import me.leeseol.economy.npc.NpcManager;
import me.leeseol.economy.servermenu.ServerMenuManager;
import me.leeseol.economy.shop.ShopCommand;
import me.leeseol.economy.shop.ShopManager;
import me.leeseol.economy.storage.BalanceStore;
import me.leeseol.economy.util.Money;
import me.leeseol.economy.util.Text;
import me.leeseol.economy.vault.LeeSeolVaultEconomy;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class LeeSeolEconomyPlugin extends JavaPlugin {
    public static final String BUNGEE_CHANNEL = "BungeeCord";

    private BalanceStore balanceStore;
    private Money money;
    private ShopManager shopManager;
    private NpcManager npcManager;
    private ServerMenuManager serverMenuManager;
    private MarketManager marketManager;
    private EconomyLedger ledger;
    private LeeSeolVaultEconomy vaultEconomy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, BUNGEE_CHANNEL);
        createManagers();
        registerCommands();
        registerVaultEconomy();
        getLogger().info("LeeSeolEconomy enabled. shops=" + shopManager.shopCount() + ", npcs=" + npcManager.configuredNpcCount());
    }

    @Override
    public void onDisable() {
        if (npcManager != null) {
            npcManager.removeSpawnedNpcs();
        }
        if (vaultEconomy != null) {
            getServer().getServicesManager().unregister(vaultEconomy);
        }
        if (marketManager != null) {
            marketManager.save();
        }
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, BUNGEE_CHANNEL);
    }

    public void reloadAll() {
        reloadConfig();
        this.money = new Money(this);
        this.balanceStore = new BalanceStore(this, getConfig().getString("storage.balances-file"), getConfig().getLong("currency.starting-balance", 0L));
        ledger.reload();
        shopManager.reload();
        npcManager.reload();
        serverMenuManager.reload();
        marketManager.reload();
        registerVaultEconomy();
    }

    private void createManagers() {
        this.money = new Money(this);
        this.balanceStore = new BalanceStore(this, getConfig().getString("storage.balances-file"), getConfig().getLong("currency.starting-balance", 0L));
        this.ledger = new EconomyLedger(this);
        this.shopManager = new ShopManager(this);
        this.npcManager = new NpcManager(this, shopManager);
        this.serverMenuManager = new ServerMenuManager(this);
        this.marketManager = new MarketManager(this);

        getServer().getPluginManager().registerEvents(shopManager, this);
        getServer().getPluginManager().registerEvents(npcManager, this);
        getServer().getPluginManager().registerEvents(serverMenuManager, this);

        shopManager.reload();
        npcManager.reload();
        serverMenuManager.reload();
        marketManager.reload();
    }

    private void registerCommands() {
        EconomyCommand economyCommand = new EconomyCommand(this);
        register("won", economyCommand);
        register("pay", economyCommand);
        register("shop", new ShopCommand(this, shopManager));
        register("wonnpc", new NpcCommand(this, npcManager));
        register("market", new MarketCommand(this, marketManager));
        register("servermenu", serverMenuManager);
        register("leeseolmenu", new MenuBridgeCommand(this));
    }

    private void register(String commandName, Object executor) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().warning("Command is missing from plugin.yml: " + commandName);
            return;
        }
        if (executor instanceof org.bukkit.command.CommandExecutor commandExecutor) {
            command.setExecutor(commandExecutor);
        }
        if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    private void registerVaultEconomy() {
        if (!getConfig().getBoolean("features.vault-provider", true)) {
            return;
        }
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found. Economy provider registration skipped.");
            return;
        }
        if (vaultEconomy != null) {
            getServer().getServicesManager().unregister(vaultEconomy);
        }
        vaultEconomy = new LeeSeolVaultEconomy(this, balanceStore, money);
        getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, vaultEconomy, this, ServicePriority.Highest);
        getLogger().info("Registered LeeSeolEconomy as Vault economy provider.");
    }

    public String msg(String key) {
        return Text.color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages." + key, ""));
    }

    public String msgRaw(String key) {
        return Text.color(getConfig().getString("messages." + key, ""));
    }

    public BalanceStore balanceStore() {
        return balanceStore;
    }

    public Money money() {
        return money;
    }

    public ShopManager shopManager() {
        return shopManager;
    }

    public ServerMenuManager serverMenuManager() {
        return serverMenuManager;
    }

    public MarketManager marketManager() {
        return marketManager;
    }

    public EconomyLedger ledger() {
        return ledger;
    }

    public boolean featureEnabled(String path) {
        return getConfig().getBoolean("features." + path, true);
    }

    public void sendPlayerToServer(Player player, String targetServer) {
        if (targetServer == null || targetServer.isBlank()) {
            return;
        }

        // Velocity 환경에서 동작 확인 필요: BungeeCord 호환 Connect subchannel을 사용한다.
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Connect");
            out.writeUTF(targetServer);
            player.sendPluginMessage(this, BUNGEE_CHANNEL, bytes.toByteArray());
        } catch (IOException exception) {
            getLogger().warning("Failed to send player to server " + targetServer + ": " + exception.getMessage());
        }
    }
}
