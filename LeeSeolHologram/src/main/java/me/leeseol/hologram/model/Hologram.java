package me.leeseol.hologram.model;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public final class Hologram {
    private final String id;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private double lineSpacing;
    private final List<String> lines = new ArrayList<>();

    public Hologram(String id, Location location, double lineSpacing, List<String> lines) {
        this.id = id;
        setLocation(location);
        this.lineSpacing = lineSpacing;
        this.lines.addAll(lines);
    }

    public Hologram(String id, String worldName, double x, double y, double z, float yaw, float pitch, double lineSpacing, List<String> lines) {
        this.id = id;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.lineSpacing = lineSpacing;
        this.lines.addAll(lines);
    }

    public String id() {
        return id;
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public double lineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(double lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public List<String> lines() {
        return lines;
    }

    public Location location(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void setLocation(Location location) {
        World world = location.getWorld();
        this.worldName = world == null ? "" : world.getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }
}
