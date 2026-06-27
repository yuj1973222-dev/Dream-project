package me.leeseol.economy.ledger;

import java.util.Map;

public record LedgerSnapshot(
    String period,
    Map<String, Long> issued,
    Map<String, Long> removed,
    Map<String, Long> transferred,
    long totalIssued,
    long totalRemoved,
    long totalTransferred
) {
    public long netIssued() {
        return totalIssued - totalRemoved;
    }
}
