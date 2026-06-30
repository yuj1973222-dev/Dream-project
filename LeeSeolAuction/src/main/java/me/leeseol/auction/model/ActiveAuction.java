package me.leeseol.auction.model;

import java.util.UUID;

public final class ActiveAuction {
    private final long lotId;
    private final long startingBid;
    private long bidIncrement;
    private long currentBid;
    private UUID bidderUuid;
    private String bidderName;
    private final long openedAt;

    public ActiveAuction(long lotId, long startingBid, long bidIncrement, long openedAt) {
        this.lotId = lotId;
        this.startingBid = Math.max(0L, startingBid);
        this.bidIncrement = Math.max(1L, bidIncrement);
        this.openedAt = openedAt;
    }

    public long lotId() {
        return lotId;
    }

    public long startingBid() {
        return startingBid;
    }

    public long bidIncrement() {
        return bidIncrement;
    }

    public void setBidIncrement(long bidIncrement) {
        this.bidIncrement = Math.max(1L, bidIncrement);
    }

    public long currentBid() {
        return currentBid;
    }

    public void setCurrentBid(long currentBid) {
        this.currentBid = Math.max(0L, currentBid);
    }

    public UUID bidderUuid() {
        return bidderUuid;
    }

    public void setBidderUuid(UUID bidderUuid) {
        this.bidderUuid = bidderUuid;
    }

    public String bidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public long openedAt() {
        return openedAt;
    }

    public boolean hasBidder() {
        return bidderUuid != null;
    }
}
