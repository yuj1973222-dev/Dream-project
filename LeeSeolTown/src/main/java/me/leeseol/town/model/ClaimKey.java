package me.leeseol.town.model;

import org.bukkit.Chunk;
import org.bukkit.Location;

public record ClaimKey(String world, int x, int z) {
    public static ClaimKey from(Location location) {
        return from(location.getChunk());
    }

    public static ClaimKey from(Chunk chunk) {
        return new ClaimKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public static ClaimKey parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String[] parts = input.split(":");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new ClaimKey(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public String serialize() {
        return world + ":" + x + ":" + z;
    }

    public String display() {
        return world + " " + x + ", " + z;
    }
}
