package me.leeseol.town.hook;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public final class VaultEconomyHook {
    private final JavaPlugin plugin;
    private Object economy;
    private Method hasMethod;
    private Method withdrawMethod;
    private Method balanceMethod;
    private Method formatMethod;

    public VaultEconomyHook(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        economy = null;
        hasMethod = null;
        withdrawMethod = null;
        balanceMethod = null;
        formatMethod = null;

        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }

        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = plugin.getServer().getServicesManager().getRegistration(economyClass);
            if (registration == null) {
                return;
            }
            economy = registration.getProvider();
            hasMethod = economyClass.getMethod("has", OfflinePlayer.class, double.class);
            withdrawMethod = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            balanceMethod = economyClass.getMethod("getBalance", OfflinePlayer.class);
            formatMethod = economyClass.getMethod("format", double.class);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Vault economy hook unavailable: " + exception.getMessage());
            economy = null;
        }
    }

    public boolean available() {
        return economy != null;
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!available()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(hasMethod.invoke(economy, player, amount));
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Vault balance check failed: " + exception.getMessage());
            return false;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!available()) {
            return false;
        }
        try {
            Object response = withdrawMethod.invoke(economy, player, amount);
            Method transactionSuccess = response.getClass().getMethod("transactionSuccess");
            return Boolean.TRUE.equals(transactionSuccess.invoke(response));
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Vault withdraw failed: " + exception.getMessage());
            return false;
        }
    }

    public double balance(OfflinePlayer player) {
        if (!available()) {
            return 0.0D;
        }
        try {
            Object value = balanceMethod.invoke(economy, player);
            return value instanceof Number number ? number.doubleValue() : 0.0D;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Vault balance read failed: " + exception.getMessage());
            return 0.0D;
        }
    }

    public String format(double amount) {
        if (!available()) {
            return String.format("%,.0f", amount);
        }
        try {
            return String.valueOf(formatMethod.invoke(economy, amount));
        } catch (ReflectiveOperationException exception) {
            return String.format("%,.0f", amount);
        }
    }
}
