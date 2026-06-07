package me.leeseol.town;

import me.leeseol.town.command.ChannelChatCommand;
import me.leeseol.town.command.TownCommand;
import me.leeseol.town.hook.TownPlaceholderExpansion;
import me.leeseol.town.hook.VaultEconomyHook;
import me.leeseol.town.listener.ClaimProtectionListener;
import me.leeseol.town.listener.IdentityListener;
import me.leeseol.town.listener.NationRuleListener;
import me.leeseol.town.listener.TownChatListener;
import me.leeseol.town.model.ChatMode;
import me.leeseol.town.service.TownService;
import me.leeseol.town.storage.TownStore;
import me.leeseol.town.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolTownPlugin extends JavaPlugin {
    private TownStore store;
    private TownService townService;
    private VaultEconomyHook economy;
    private TownPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        createServices();
        registerListeners();
        registerCommands();
        registerPlaceholderExpansion();
        getLogger().info("LeeSeolTown enabled. towns=" + townService.towns().size() + ", nations=" + townService.nations().size());
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        if (store != null) {
            store.save();
        }
    }

    public void reloadAll() {
        reloadConfig();
        economy.reload();
        townService.reload();
    }

    private void createServices() {
        this.store = new TownStore(this);
        this.economy = new VaultEconomyHook(this);
        this.townService = new TownService(this, store);
        this.townService.reload();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new IdentityListener(townService), this);
        getServer().getPluginManager().registerEvents(new ClaimProtectionListener(this, townService), this);
        getServer().getPluginManager().registerEvents(new NationRuleListener(this, townService), this);
        getServer().getPluginManager().registerEvents(new TownChatListener(this, townService), this);
    }

    private void registerCommands() {
        TownCommand townCommand = new TownCommand(this, townService);
        register("town", townCommand);
        register("tc", new ChannelChatCommand(townService, ChatMode.TOWN));
        register("nc", new ChannelChatCommand(townService, ChatMode.NATION));
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

    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("PlaceholderAPI not found. Town placeholders are disabled.");
            return;
        }
        placeholderExpansion = new TownPlaceholderExpansion(this);
        placeholderExpansion.register();
        getLogger().info("Registered PlaceholderAPI placeholders: %leeseoltown_rank%, %leeseoltown_affiliation%, %leeseoltown_has_party%, %leeseoltown_party%, %leeseoltown_town%, %leeseoltown_nation%");
    }

    public String msg(String key) {
        return Text.color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages." + key, ""));
    }

    public String msgRaw(String key) {
        return Text.color(getConfig().getString("messages." + key, ""));
    }

    public int minTownMembers() {
        return getConfig().getInt("settings.min-town-members", 2);
    }

    public int partyMaxMembers() {
        return getConfig().getInt("settings.party-max-members", 4);
    }

    public int nationRequiredMembers() {
        return getConfig().getInt("settings.nation-required-members", 5);
    }

    public int federationRequiredTowns() {
        return getConfig().getInt("settings.federation-required-towns", 4);
    }

    public int federationRequiredMembers() {
        return getConfig().getInt("settings.federation-required-members", 20);
    }

    public boolean economyEnabled() {
        return getConfig().getBoolean("economy.enabled", true);
    }

    public double chunkClaimCost() {
        return getConfig().getDouble("economy.chunk-claim-cost", 10000.0D);
    }

    public long nationTax(int memberCount) {
        int requiredMembers = Math.max(1, nationRequiredMembers());
        double base = getConfig().getDouble("economy.nation-tax-base", 10000.0D);
        double value = base * Math.exp(Math.max(0, memberCount - requiredMembers));
        if (!Double.isFinite(value) || value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.round(value);
    }

    public long nationTax(int memberCount, int karma) {
        long baseTax = nationTax(memberCount);
        if (karma >= 0 || baseTax == Long.MAX_VALUE) {
            return baseTax;
        }
        double value = baseTax * (1.0D + Math.abs(karma) / 100.0D);
        if (!Double.isFinite(value) || value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.round(value);
    }

    public double warSurrenderPayment() {
        return getConfig().getDouble("war.surrender-payment", 100000.0D);
    }

    public long warProtectionMillis() {
        return Math.max(1L, getConfig().getLong("war.protection-minutes", 10L)) * 60_000L;
    }

    public long warDebtMillis() {
        return Math.max(1L, getConfig().getLong("war.debt-hours", 12L)) * 3_600_000L;
    }

    public String formatMoney(double amount) {
        return economy.available() ? economy.format(amount) : String.format("%,.0f원", amount);
    }

    public VaultEconomyHook economy() {
        return economy;
    }

    public TownService townService() {
        return townService;
    }
}
