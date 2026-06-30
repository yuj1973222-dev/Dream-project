package me.leeseol.dungeon.loot;

public final class LootChestSpot {
    private final String id;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final String table;
    private final double chance;
    private final long respawnMillis;
    private long lastRollMillis;

    public LootChestSpot(String id, String worldName, int x, int y, int z, String table, double chance, long respawnSeconds) {
        this.id = id;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.table = table;
        this.chance = Math.max(0.0D, Math.min(1.0D, chance));
        this.respawnMillis = Math.max(1L, respawnSeconds) * 1000L;
    }

    public String id() {
        return id;
    }

    public String worldName() {
        return worldName;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public String table() {
        return table;
    }

    public double chance() {
        return chance;
    }

    public boolean canRoll(long now) {
        return now - lastRollMillis >= respawnMillis;
    }

    public void markRolled(long now) {
        lastRollMillis = now;
    }
}
