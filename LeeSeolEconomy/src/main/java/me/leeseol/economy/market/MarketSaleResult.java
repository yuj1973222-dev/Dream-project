package me.leeseol.economy.market;

public record MarketSaleResult(
    MarketOffer offer,
    int requestedAmount,
    int acceptedAmount,
    long payout,
    long nextUnitPrice,
    String failureReason
) {
    public boolean success() {
        return acceptedAmount > 0 && payout > 0L;
    }
}
