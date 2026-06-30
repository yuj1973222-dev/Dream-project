package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.NeutralZone;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NeutralZoneManager {
    private static final long CORE_REFRESH_THROTTLE_MILLIS = 2_000L;

    private final LeeSeolTownPlugin plugin;
    private final Map<String, NeutralZone> zones = new LinkedHashMap<>();
    private Source source = Source.LEGACY;
    private long lastCoreModified = -1L;
    private long lastCoreCheckMillis = 0L;

    public NeutralZoneManager(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        zones.clear();
        if (loadCoreContentZones()) {
            refreshBlueMapMarkers();
            plugin.getLogger().info("LeeSeolTown loaded neutral zones=" + zones.size() + " source=LeeSeolCore");
            return;
        }

        File file = dataFile();
        if (!file.exists()) {
            source = Source.LEGACY;
            save();
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = data.getConfigurationSection("zones");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            String base = id + ".";
            String world = section.getString(base + "world", "");
            if (world.isBlank()) {
                plugin.getLogger().warning("Skipping neutral zone with missing world: " + id);
                continue;
            }
            NeutralZone zone = new NeutralZone(
                    id,
                    world,
                    Math.min(section.getInt(base + "min-x"), section.getInt(base + "max-x")),
                    Math.min(section.getInt(base + "min-y"), section.getInt(base + "max-y")),
                    Math.min(section.getInt(base + "min-z"), section.getInt(base + "max-z")),
                    Math.max(section.getInt(base + "min-x"), section.getInt(base + "max-x")),
                    Math.max(section.getInt(base + "min-y"), section.getInt(base + "max-y")),
                    Math.max(section.getInt(base + "min-z"), section.getInt(base + "max-z")),
                    Math.max(0, section.getInt(base + "claim-buffer-chunks", defaultClaimBufferChunks()))
            );
            zones.put(zone.id(), zone);
        }
        source = Source.LEGACY;
        plugin.getLogger().info("LeeSeolTown loaded neutral zones=" + zones.size() + " source=legacy");
    }

    public void save() {
        if (source == Source.CORE_CONTENT) {
            return;
        }
        YamlConfiguration data = new YamlConfiguration();
        for (NeutralZone zone : zones.values()) {
            String base = "zones." + zone.id() + ".";
            data.set(base + "world", zone.world());
            data.set(base + "min-x", zone.minX());
            data.set(base + "min-y", zone.minY());
            data.set(base + "min-z", zone.minZ());
            data.set(base + "max-x", zone.maxX());
            data.set(base + "max-y", zone.maxY());
            data.set(base + "max-z", zone.maxZ());
            data.set(base + "claim-buffer-chunks", zone.claimBufferChunks());
        }

        try {
            File file = dataFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save neutral zones: " + exception.getMessage());
        }
    }

    public NeutralZone zone(String id) {
        reloadIfCoreChanged();
        return zones.get(id);
    }

    public Collection<NeutralZone> zones() {
        reloadIfCoreChanged();
        return zones.values();
    }

    public NeutralZone zoneAt(Location location) {
        reloadIfCoreChanged();
        for (NeutralZone zone : zones.values()) {
            if (zone.contains(location)) {
                return zone;
            }
        }
        return null;
    }

    public NeutralZone claimBlockedBy(ClaimKey claim) {
        reloadIfCoreChanged();
        for (NeutralZone zone : zones.values()) {
            if (zone.blocksClaim(claim)) {
                return zone;
            }
        }
        return null;
    }

    public void put(NeutralZone zone) {
        zones.put(zone.id(), zone);
        save();
        syncWorldGuardRegion(zone);
        refreshBlueMapMarkers();
    }

    public NeutralZone remove(String id) {
        NeutralZone removed = zones.remove(id);
        if (removed != null) {
            save();
            removeWorldGuardRegion(removed);
            refreshBlueMapMarkers();
        }
        return removed;
    }

    public int defaultClaimBufferChunks() {
        return Math.max(0, plugin.getConfig().getInt("neutral-zones.default-claim-buffer-chunks", 2));
    }

    public boolean blockBreakEnabled() {
        return plugin.getConfig().getBoolean("neutral-zones.protection.block-break", true);
    }

    public boolean blockPlaceEnabled() {
        return plugin.getConfig().getBoolean("neutral-zones.protection.block-place", true);
    }

    public boolean pvpEnabled() {
        return plugin.getConfig().getBoolean("neutral-zones.protection.pvp", true);
    }

    public File dataFile() {
        String dataPath = plugin.getConfig().getString("neutral-zones.data-file", "neutral-zones.yml");
        File configured = new File(dataPath == null || dataPath.isBlank() ? "neutral-zones.yml" : dataPath);
        return configured.isAbsolute() ? configured : new File(plugin.getDataFolder(), configured.getPath());
    }

    public File coreContentFile() {
        String dataPath = plugin.getConfig().getString("neutral-zones.core-content-file", "plugins/LeeSeolCore/contents.yml");
        File configured = new File(dataPath == null || dataPath.isBlank() ? "plugins/LeeSeolCore/contents.yml" : dataPath);
        return configured.isAbsolute() ? configured : configured;
    }

    public boolean usingCoreContent() {
        return source == Source.CORE_CONTENT;
    }

    private boolean loadCoreContentZones() {
        File file = coreContentFile();
        if (!file.exists()) {
            lastCoreModified = -1L;
            return false;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        Collection<NeutralZone> coreZones = NeutralZoneContentSource.loadCoreNeutralZones(data, defaultClaimBufferChunks());
        lastCoreModified = file.lastModified();
        if (coreZones.isEmpty()) {
            return false;
        }
        for (NeutralZone zone : coreZones) {
            zones.put(zone.id(), zone);
        }
        source = Source.CORE_CONTENT;
        return true;
    }

    private void reloadIfCoreChanged() {
        long now = System.currentTimeMillis();
        if (now - lastCoreCheckMillis < CORE_REFRESH_THROTTLE_MILLIS) {
            return;
        }
        lastCoreCheckMillis = now;

        File file = coreContentFile();
        long modified = file.exists() ? file.lastModified() : -1L;
        if (modified == lastCoreModified) {
            return;
        }

        reload();
    }

    private void refreshBlueMapMarkers() {
        if (plugin.blueMapNeutralZoneMarkers() != null) {
            plugin.blueMapNeutralZoneMarkers().refreshLater();
        }
    }

    private void syncWorldGuardRegion(NeutralZone zone) {
        if (plugin.worldGuardNeutralZoneRegions() != null) {
            plugin.worldGuardNeutralZoneRegions().upsert(zone);
        }
    }

    private void removeWorldGuardRegion(NeutralZone zone) {
        if (plugin.worldGuardNeutralZoneRegions() != null) {
            plugin.worldGuardNeutralZoneRegions().remove(zone);
        }
    }

    private enum Source {
        CORE_CONTENT,
        LEGACY
    }
}
