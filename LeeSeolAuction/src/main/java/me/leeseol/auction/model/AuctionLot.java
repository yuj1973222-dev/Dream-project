package me.leeseol.auction.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class AuctionLot {
    public enum Status {
        SUBMITTED,
        ACTIVE,
        SOLD
    }

    private final long id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private Status status;
    private long createdAt;

    public AuctionLot(long id, UUID sellerUuid, String sellerName, ItemStack item, Status status, long createdAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item = item;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    public UUID sellerUuid() {
        return sellerUuid;
    }

    public String sellerName() {
        return sellerName;
    }

    public ItemStack item() {
        return item;
    }

    public Status status() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long createdAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
