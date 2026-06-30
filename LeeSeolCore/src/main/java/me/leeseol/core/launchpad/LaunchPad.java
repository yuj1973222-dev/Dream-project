package me.leeseol.core.launchpad;

import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

public final class LaunchPad {
    private final String id;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final Material block;
    private final double forward;
    private final double upward;
    private final long cooldownMillis;
    private final String message;
    private final Sound sound;
    private final Particle particle;

    public LaunchPad(
            String id,
            String worldName,
            int x,
            int y,
            int z,
            Material block,
            double forward,
            double upward,
            long cooldownSeconds,
            String message,
            Sound sound,
            Particle particle
    ) {
        this.id = id;
        this.worldName = worldName.toLowerCase(Locale.ROOT);
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
        this.forward = forward;
        this.upward = upward;
        this.cooldownMillis = Math.max(0, cooldownSeconds) * 1000L;
        this.message = message;
        this.sound = sound;
        this.particle = particle;
    }

    public String getId() {
        return id;
    }

    public Material getBlock() {
        return block;
    }

    public double getForward() {
        return forward;
    }

    public double getUpward() {
        return upward;
    }

    public long getCooldownMillis() {
        return cooldownMillis;
    }

    public String getMessage() {
        return message;
    }

    public Sound getSound() {
        return sound;
    }

    public Particle getParticle() {
        return particle;
    }

    public boolean matches(String world, int blockX, int blockY, int blockZ) {
        return world != null
                && worldName.equals(world.toLowerCase(Locale.ROOT))
                && x == blockX
                && y == blockY
                && z == blockZ;
    }
}
