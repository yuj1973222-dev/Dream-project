package me.leeseol.jobs.service;

import net.milkbowl.vault.economy.Economy;
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
            plugin.getLogger().warning("Vault economy provider not found. Job rewards are disabled.");
        }
    }

    public boolean deposit(org.bukkit.OfflinePlayer player, long amount) {
        if (economy == null || amount <= 0L) {
            return false;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
}
