package me.leeseol.town.service;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.NeutralZone;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;

public final class WorldGuardNeutralZoneRegions {
    private final LeeSeolTownPlugin plugin;

    public WorldGuardNeutralZoneRegions(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
    }

    public void syncAll(Collection<NeutralZone> zones) {
        if (!enabled()) {
            return;
        }
        for (NeutralZone zone : zones) {
            upsert(zone);
        }
    }

    public void upsert(NeutralZone zone) {
        if (!enabled() || !worldGuardAvailable()) {
            return;
        }
        try {
            Object manager = regionManager(zone.world());
            if (manager == null) {
                plugin.getLogger().warning("Could not find WorldGuard region manager for neutral zone world: " + zone.world());
                return;
            }

            Object region = protectedCuboidRegion(zone);
            setPriority(region);
            setStateFlag(region, "BUILD", flagState("build"));
            setStateFlag(region, "BLOCK_BREAK", flagState("block-break"));
            setStateFlag(region, "BLOCK_PLACE", flagState("block-place"));
            setStateFlag(region, "PVP", flagState("pvp"));

            Class<?> protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");
            manager.getClass().getMethod("addRegion", protectedRegionClass).invoke(manager, region);
            manager.getClass().getMethod("saveChanges").invoke(manager);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.getLogger().warning("Could not sync WorldGuard region for neutral zone " + zone.id() + ": " + exception.getMessage());
        }
    }

    public void remove(NeutralZone zone) {
        if (!enabled() || !worldGuardAvailable()) {
            return;
        }
        try {
            Object manager = regionManager(zone.world());
            if (manager == null) {
                return;
            }
            manager.getClass().getMethod("removeRegion", String.class).invoke(manager, regionId(zone));
            manager.getClass().getMethod("saveChanges").invoke(manager);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.getLogger().warning("Could not remove WorldGuard region for neutral zone " + zone.id() + ": " + exception.getMessage());
        }
    }

    public String regionId(NeutralZone zone) {
        return regionPrefix() + zone.id().toLowerCase(Locale.ROOT);
    }

    public boolean regionExists(NeutralZone zone) {
        if (!enabled() || !worldGuardAvailable()) {
            return false;
        }
        try {
            Object manager = regionManager(zone.world());
            if (manager == null) {
                return false;
            }
            Object region = manager.getClass().getMethod("getRegion", String.class).invoke(manager, regionId(zone));
            return region != null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.getLogger().warning("Could not inspect WorldGuard region for neutral zone "
                    + zone.id() + ": " + exception.getMessage());
            return false;
        }
    }

    public boolean enabledByConfig() {
        return enabled();
    }

    public boolean worldGuardAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    private Object regionManager(String worldName) throws ReflectiveOperationException {
        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null) {
            return null;
        }
        Object worldGuard = Class.forName("com.sk89q.worldguard.WorldGuard").getMethod("getInstance").invoke(null);
        Object platform = worldGuard.getClass().getMethod("getPlatform").invoke(worldGuard);
        Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
        com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(bukkitWorld);
        return container.getClass().getMethod("get", com.sk89q.worldedit.world.World.class).invoke(container, adaptedWorld);
    }

    private Object protectedCuboidRegion(NeutralZone zone) throws ReflectiveOperationException {
        Class<?> regionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion");
        Constructor<?> constructor = regionClass.getConstructor(String.class, BlockVector3.class, BlockVector3.class);
        return constructor.newInstance(
                regionId(zone),
                BlockVector3.at(zone.minX(), zone.minY(), zone.minZ()),
                BlockVector3.at(zone.maxX(), zone.maxY(), zone.maxZ())
        );
    }

    private void setPriority(Object region) throws ReflectiveOperationException {
        Method setPriority = region.getClass().getMethod("setPriority", int.class);
        setPriority.invoke(region, plugin.getConfig().getInt("neutral-zones.worldguard.priority", 50));
    }

    private void setStateFlag(Object region, String flagName, String stateName) throws ReflectiveOperationException {
        if (stateName == null || stateName.isBlank() || stateName.equalsIgnoreCase("NONE")) {
            return;
        }
        Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
        Field flagField = flagsClass.getField(flagName);
        Object flag = flagField.get(null);
        Class<?> flagClass = Class.forName("com.sk89q.worldguard.protection.flags.Flag");
        Class<?> stateClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag$State");
        Object state = enumValue(stateClass, stateName);
        Method setFlag = region.getClass().getMethod("setFlag", flagClass, Object.class);
        setFlag.invoke(region, flag, state);
    }

    private Object enumValue(Class<?> enumClass, String value) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Object enumValue = Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), value.trim().toUpperCase(Locale.ROOT));
        return enumValue;
    }

    private String flagState(String key) {
        return plugin.getConfig().getString("neutral-zones.worldguard.flags." + key, "DENY");
    }

    private String regionPrefix() {
        return plugin.getConfig().getString("neutral-zones.worldguard.region-prefix", "leeseol_neutral_");
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("neutral-zones.worldguard.enabled", true);
    }

}
