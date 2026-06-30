package me.leeseol.core.content;

public record ContentArea(
        String world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
) {
    public ContentArea {
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("content world is required");
        }

        int normalizedMinX = Math.min(minX, maxX);
        int normalizedMinY = Math.min(minY, maxY);
        int normalizedMinZ = Math.min(minZ, maxZ);
        int normalizedMaxX = Math.max(minX, maxX);
        int normalizedMaxY = Math.max(minY, maxY);
        int normalizedMaxZ = Math.max(minZ, maxZ);

        minX = normalizedMinX;
        minY = normalizedMinY;
        minZ = normalizedMinZ;
        maxX = normalizedMaxX;
        maxY = normalizedMaxY;
        maxZ = normalizedMaxZ;
    }

    public double centerX() {
        return (minX + maxX) / 2.0D;
    }

    public double centerY() {
        return (minY + maxY) / 2.0D;
    }

    public double centerZ() {
        return (minZ + maxZ) / 2.0D;
    }
}
