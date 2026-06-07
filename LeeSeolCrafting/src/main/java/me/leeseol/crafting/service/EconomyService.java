package me.leeseol.crafting.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyService {
    private final JavaPlugin plugin;
    private Economy economy;

    public EconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
        if (economy == null) {
            plugin.getLogger().warning("Vault economy provider not found. Crafting costs are disabled.");
        }
    }

    public boolean has(OfflinePlayer player, long amount) {
        return amount <= 0L || (economy != null && economy.has(player, amount));
    }

    public boolean withdraw(OfflinePlayer player, long amount) {
        if (amount <= 0L) {
            return true;
        }
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }
}
