package me.leeseol.core.content;

public record ContentSpawn(
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
    public ContentSpawn {
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("content spawn world is required");
        }
    }
}
