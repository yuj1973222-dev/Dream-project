package me.leeseol.town.model;

import org.bukkit.Location;
import org.bukkit.World;

public record NeutralZone(
        String id,
        String world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        int claimBufferChunks
) {
    public static NeutralZone fromLocations(String id, Location first, Location second, int claimBufferChunks) {
        World firstWorld = first.getWorld();
        World secondWorld = second.getWorld();
        if (firstWorld == null || secondWorld == null || !firstWorld.getName().equals(secondWorld.getName())) {
            throw new IllegalArgumentException("neutral zone positions must be in the same world");
        }
        return new NeutralZone(
                id,
                firstWorld.getName(),
                Math.min(first.getBlockX(), second.getBlockX()),
                Math.min(first.getBlockY(), second.getBlockY()),
                Math.min(first.getBlockZ(), second.getBlockZ()),
                Math.max(first.getBlockX(), second.getBlockX()),
                Math.max(first.getBlockY(), second.getBlockY()),
                Math.max(first.getBlockZ(), second.getBlockZ()),
                Math.max(0, claimBufferChunks)
        );
    }

    public boolean contains(Location location) {
        World locationWorld = location.getWorld();
        return locationWorld != null
                && world.equals(locationWorld.getName())
                && location.getBlockX() >= minX
                && location.getBlockX() <= maxX
                && location.getBlockY() >= minY
                && location.getBlockY() <= maxY
                && location.getBlockZ() >= minZ
                && location.getBlockZ() <= maxZ;
    }

    public boolean blocksClaim(ClaimKey claim) {
        return world.equals(claim.world())
                && claim.x() >= minClaimChunkX()
                && claim.x() <= maxClaimChunkX()
                && claim.z() >= minClaimChunkZ()
                && claim.z() <= maxClaimChunkZ();
    }

    public int minClaimChunkX() {
        return Math.floorDiv(minX, 16) - claimBufferChunks;
    }

    public int maxClaimChunkX() {
        return Math.floorDiv(maxX, 16) + claimBufferChunks;
    }

    public int minClaimChunkZ() {
        return Math.floorDiv(minZ, 16) - claimBufferChunks;
    }

    public int maxClaimChunkZ() {
        return Math.floorDiv(maxZ, 16) + claimBufferChunks;
    }

    public String blockRange() {
        return world + " " + minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ;
    }

    public String claimRange() {
        return world + " chunk " + minClaimChunkX() + "," + minClaimChunkZ() + " -> " + maxClaimChunkX() + "," + maxClaimChunkZ();
    }
}
