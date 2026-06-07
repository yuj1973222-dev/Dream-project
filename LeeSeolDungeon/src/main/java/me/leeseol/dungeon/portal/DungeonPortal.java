package me.leeseol.dungeon.portal;

public final class DungeonPortal {
    private final String id;
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final String targetWorld;
    private final long cooldownMillis;
    private final String message;
    private final String sound;

    public DungeonPortal(
            String id,
            String worldName,
            int x1,
            int y1,
            int z1,
            int x2,
            int y2,
            int z2,
            String targetWorld,
            long cooldownSeconds,
            String message,
            String sound
    ) {
        this.id = id;
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.targetWorld = targetWorld;
        this.cooldownMillis = Math.max(1L, cooldownSeconds) * 1000L;
        this.message = message;
        this.sound = sound;
    }

    public String id() {
        return id;
    }

    public String targetWorld() {
        return targetWorld;
    }

    public String worldName() {
        return worldName;
    }

    public int centerX() {
        return (minX + maxX) / 2;
    }

    public int centerZ() {
        return (minZ + maxZ) / 2;
    }

    public long cooldownMillis() {
        return cooldownMillis;
    }

    public String message() {
        return message;
    }

    public String sound() {
        return sound;
    }

    public boolean contains(String worldName, int x, int y, int z) {
        return this.worldName.equalsIgnoreCase(worldName)
                && x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}
