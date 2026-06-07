package me.leeseol.core.portal;

public final class PortalCuboidSelection {
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public PortalCuboidSelection(
            String worldName,
            int x1,
            int y1,
            int z1,
            int x2,
            int y2,
            int z2
    ) {
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public String format() {
        return worldName + " "
                + minX + ", " + minY + ", " + minZ
                + " -> "
                + maxX + ", " + maxY + ", " + maxZ;
    }
}
