package me.leeseol.core.content;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

public final class WorldGuardContentRegionService implements ContentRegionService {
    private final LeeSeolCorePlugin plugin;

    public WorldGuardContentRegionService(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean available() {
        return enabledByConfig() && Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    @Override
    public boolean upsert(ContentEntry entry) {
        if (!available()) {
            return false;
        }
        try {
            Object manager = regionManager(entry.world());
            if (manager == null) {
                plugin.getLogger().warning("Could not find WorldGuard region manager for content world: " + entry.world());
                return false;
            }

            Object region = protectedCuboidRegion(entry);
            setPriority(region);
            applyPresetFlags(region, entry.preset());

            Class<?> protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");
            manager.getClass().getMethod("addRegion", protectedRegionClass).invoke(manager, region);
            manager.getClass().getMethod("saveChanges").invoke(manager);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.getLogger().warning("Could not sync WorldGuard content region "
                    + entry.regionId() + ": " + exception.getMessage());
            return false;
        }
    }

    @Override
    public boolean remove(ContentEntry entry) {
        if (!available()) {
            return false;
        }
        try {
            Object manager = regionManager(entry.world());
            if (manager == null) {
                return false;
            }
            manager.getClass().getMethod("removeRegion", String.class).invoke(manager, entry.regionId());
            manager.getClass().getMethod("saveChanges").invoke(manager);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.getLogger().warning("Could not remove WorldGuard content region "
                    + entry.regionId() + ": " + exception.getMessage());
            return false;
        }
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

    private Object protectedCuboidRegion(ContentEntry entry) throws ReflectiveOperationException {
        Class<?> regionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion");
        Constructor<?> constructor = regionClass.getConstructor(String.class, BlockVector3.class, BlockVector3.class);
        return constructor.newInstance(
                entry.regionId(),
                BlockVector3.at(entry.minX(), entry.minY(), entry.minZ()),
                BlockVector3.at(entry.maxX(), entry.maxY(), entry.maxZ())
        );
    }

    private void setPriority(Object region) throws ReflectiveOperationException {
        Method setPriority = region.getClass().getMethod("setPriority", int.class);
        setPriority.invoke(region, plugin.getConfig().getInt("content-registry.worldguard.priority", 60));
    }

    private void applyPresetFlags(Object region, String preset) throws ReflectiveOperationException {
        setStateFlag(region, "BUILD", flagState(preset, "build"));
        setStateFlag(region, "BLOCK_BREAK", flagState(preset, "block-break"));
        setStateFlag(region, "BLOCK_PLACE", flagState(preset, "block-place"));
        setStateFlag(region, "PVP", flagState(preset, "pvp"));
        setStateFlag(region, "MOB_SPAWNING", flagState(preset, "mob-spawning"));
        setStateFlag(region, "TNT", flagState(preset, "tnt"));
        setStateFlag(region, "CREEPER_EXPLOSION", flagState(preset, "creeper-explosion"));
        setStateFlag(region, "OTHER_EXPLOSION", flagState(preset, "other-explosion"));
        setStateFlag(region, "FIRE_SPREAD", flagState(preset, "fire-spread"));
        setStateFlag(region, "USE", flagState(preset, "use"));
        setStateFlag(region, "INTERACT", flagState(preset, "interact"));
    }

    private void setStateFlag(Object region, String flagName, String stateName) throws ReflectiveOperationException {
        if (stateName == null || stateName.isBlank() || stateName.equalsIgnoreCase("NONE")) {
            return;
        }
        Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
        Field flagField;
        try {
            flagField = flagsClass.getField(flagName);
        } catch (NoSuchFieldException exception) {
            plugin.getLogger().warning("WorldGuard flag not found for content region: " + flagName);
            return;
        }
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

    private String flagState(String preset, String key) {
        String presetKey = preset == null || preset.isBlank() ? ContentEntry.DEFAULT_PRESET : preset;
        String path = "content-registry.worldguard.presets." + presetKey + ".flags." + key;
        String commonPath = "content-registry.worldguard.presets.common.flags." + key;
        return plugin.getConfig().getString(path, plugin.getConfig().getString(commonPath, "NONE"));
    }

    private boolean enabledByConfig() {
        return plugin.getConfig().getBoolean("content-registry.worldguard.enabled", true);
    }
}
