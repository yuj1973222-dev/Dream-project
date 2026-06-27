package me.leeseol.economy.storage;

public record BalanceSnapshot(
    int accounts,
    long totalBalance,
    long highestBalance,
    long averageBalance
) {
}
