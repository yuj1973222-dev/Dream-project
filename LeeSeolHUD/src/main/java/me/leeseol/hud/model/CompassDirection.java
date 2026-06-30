package me.leeseol.hud.model;

public enum CompassDirection {
    S,
    SW,
    W,
    NW,
    N,
    NE,
    E,
    SE;

    private static final CompassDirection[] VALUES = values();

    public static CompassDirection fromYaw(float yaw) {
        float normalized = (yaw % 360.0F + 360.0F) % 360.0F;
        int index = Math.round(normalized / 45.0F) % VALUES.length;
        return VALUES[index];
    }
}
