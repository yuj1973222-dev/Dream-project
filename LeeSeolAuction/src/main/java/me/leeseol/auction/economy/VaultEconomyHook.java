package me.leeseol.auction.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultEconomyHook {
    private final JavaPlugin plugin;
    private Economy economy;

    public VaultEconomyHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        this.economy = provider == null ? null : provider.getProvider();
    }

    public boolean available() {
        return economy != null;
    }

    public boolean has(OfflinePlayer player, long amount) {
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, long amount) {
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, long amount) {
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public String format(long amount) {
        if (economy == null) {
            return amount + "원";
        }
        return economy.format(amount);
    }
}
