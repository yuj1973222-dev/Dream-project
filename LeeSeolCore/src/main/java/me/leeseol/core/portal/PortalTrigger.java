package me.leeseol.core.portal;

import java.util.List;
import java.util.Locale;

public final class PortalTrigger {
    private final String id;
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final long cooldownMillis;
    private final List<PortalAction> actions;

    public PortalTrigger(
            String id,
            String worldName,
            int x1,
            int y1,
            int z1,
            int x2,
            int y2,
            int z2,
            long cooldownSeconds,
            List<PortalAction> actions
    ) {
        this.id = id;
        this.worldName = worldName.toLowerCase(Locale.ROOT);
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.cooldownMillis = Math.max(0, cooldownSeconds) * 1000L;
        this.actions = List.copyOf(actions);
    }

    public String getId() {
        return id;
    }

    public long getCooldownMillis() {
        return cooldownMillis;
    }

    public List<PortalAction> getActions() {
        return actions;
    }

    public boolean contains(String world, int x, int y, int z) {
        if (world == null || !worldName.equals(world.toLowerCase(Locale.ROOT))) {
            return false;
        }

        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}
