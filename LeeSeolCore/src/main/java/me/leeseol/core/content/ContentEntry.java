package me.leeseol.core.content;

public record ContentEntry(
        ContentType type,
        String id,
        String displayName,
        String world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        double centerX,
        double centerY,
        double centerZ,
        int radius,
        ContentSpawn spawn,
        String regionId,
        String preset
) {
    public static final int DEFAULT_RADIUS = 300;
    public static final String DEFAULT_PRESET = "common";

    public ContentEntry {
        ContentRegistry.validateId(id);
        if (type == null) {
            throw new IllegalArgumentException("content type is required");
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = id;
        }
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("content world is required");
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("content radius must be positive");
        }
        if (spawn == null) {
            throw new IllegalArgumentException("content spawn is required");
        }
        if (regionId == null || regionId.isBlank()) {
            regionId = defaultRegionId(type, id);
        }
        if (preset == null || preset.isBlank()) {
            preset = DEFAULT_PRESET;
        }
    }

    public static ContentEntry create(
            ContentType type,
            String id,
            String displayName,
            ContentArea area,
            ContentSpawn spawn
    ) {
        return new ContentEntry(
                type,
                id,
                displayName,
                area.world(),
                area.minX(),
                area.minY(),
                area.minZ(),
                area.maxX(),
                area.maxY(),
                area.maxZ(),
                area.centerX(),
                area.centerY(),
                area.centerZ(),
                DEFAULT_RADIUS,
                spawn,
                defaultRegionId(type, id),
                DEFAULT_PRESET
        );
    }

    public ContentArea area() {
        return new ContentArea(world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public ContentEntry withDisplayName(String newDisplayName) {
        return new ContentEntry(
                type, id, newDisplayName, world, minX, minY, minZ, maxX, maxY, maxZ,
                centerX, centerY, centerZ, radius, spawn, regionId, preset
        );
    }

    public ContentEntry withRadius(int newRadius) {
        return new ContentEntry(
                type, id, displayName, world, minX, minY, minZ, maxX, maxY, maxZ,
                centerX, centerY, centerZ, newRadius, spawn, regionId, preset
        );
    }

    public ContentEntry withSpawn(ContentSpawn newSpawn) {
        return new ContentEntry(
                type, id, displayName, world, minX, minY, minZ, maxX, maxY, maxZ,
                centerX, centerY, centerZ, radius, newSpawn, regionId, preset
        );
    }

    public static String defaultRegionId(ContentType type, String id) {
        return "leeseol_content_" + type.key() + "_" + id;
    }
}
