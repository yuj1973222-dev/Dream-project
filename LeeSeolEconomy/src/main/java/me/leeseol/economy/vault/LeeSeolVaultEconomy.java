package me.leeseol.economy.vault;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import me.leeseol.economy.storage.BalanceStore;
import me.leeseol.economy.util.Money;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;

public final class LeeSeolVaultEconomy implements Economy {
    private final LeeSeolEconomyPlugin plugin;
    private final BalanceStore store;
    private final Money money;

    public LeeSeolVaultEconomy(LeeSeolEconomyPlugin plugin, BalanceStore store, Money money) {
        this.plugin = plugin;
        this.store = store;
        this.money = money;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "LeeSeolEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 0;
    }

    @Override
    public String format(double amount) {
        return money.format(Math.max(0L, Math.round(amount)));
    }

    @Override
    public String currencyNamePlural() {
        return "원";
    }

    @Override
    public String currencyNameSingular() {
        return "원";
    }

    @Override
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return store.getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        long normalized = money.normalize(amount);
        if (normalized <= 0L) {
            return response(0D, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Invalid amount");
        }
        if (!store.withdraw(player.getUniqueId(), normalized)) {
            return response(normalized, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Not enough money");
        }
        return response(normalized, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        long normalized = money.normalize(amount);
        if (normalized <= 0L) {
            return response(0D, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Invalid amount");
        }
        store.deposit(player.getUniqueId(), normalized);
        return response(normalized, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return bankUnsupported();
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        store.getBalance(Bukkit.getOfflinePlayer(playerName).getUniqueId());
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        store.getBalance(player.getUniqueId());
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    private EconomyResponse bankUnsupported() {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank is not supported");
    }

    private EconomyResponse response(double amount, double balance, EconomyResponse.ResponseType type, String error) {
        return new EconomyResponse(amount, balance, type, error);
    }
}
