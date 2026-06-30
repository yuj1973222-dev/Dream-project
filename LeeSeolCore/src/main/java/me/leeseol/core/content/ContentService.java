package me.leeseol.core.content;

import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class ContentService {
    private final LeeSeolCorePlugin plugin;
    private final ContentRegionService regionService;
    private final ContentRegistry registry = new ContentRegistry();
    private Runnable afterChange = () -> {
    };

    public ContentService(LeeSeolCorePlugin plugin, ContentRegionService regionService) {
        this.plugin = plugin;
        this.regionService = regionService;
    }

    public void reload() {
        registry.clear();
        File file = dataFile();
        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        registry.loadFrom(data);
        plugin.getLogger().info("LeeSeolCore loaded contents=" + registry.list().size());
        int syncedRegions = ContentRegionSynchronizer.syncLoadedRegions(registry.list(), regionService);
        if (syncedRegions > 0) {
            plugin.getLogger().info("LeeSeolCore synced content regions=" + syncedRegions);
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        registry.saveTo(data);

        try {
            File file = dataFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save contents.yml: " + exception.getMessage());
        }
    }

    public ContentEntry add(
            ContentType type,
            String id,
            String displayName,
            ContentArea area,
            ContentSpawn spawn
    ) {
        ContentEntry entry = registry.add(type, id, displayName, area, spawn);
        if (!regionService.upsert(entry)) {
            registry.remove(type, id);
            throw new IllegalStateException("WorldGuard region creation failed");
        }
        save();
        afterChange.run();
        return entry;
    }

    public Optional<ContentEntry> remove(ContentType type, String id) {
        Optional<ContentEntry> removed = registry.find(type, id);
        if (removed.isEmpty()) {
            return Optional.empty();
        }
        if (!regionService.remove(removed.get())) {
            throw new IllegalStateException("WorldGuard region removal failed");
        }
        registry.remove(type, id);
        save();
        afterChange.run();
        return removed;
    }

    public ContentEntry rename(ContentType type, String id, String displayName) {
        ContentEntry entry = registry.rename(type, id, displayName);
        save();
        afterChange.run();
        return entry;
    }

    public ContentEntry setRadius(ContentType type, String id, int radius) {
        ContentEntry entry = registry.setRadius(type, id, radius);
        save();
        afterChange.run();
        return entry;
    }

    public ContentEntry setSpawn(ContentType type, String id, ContentSpawn spawn) {
        ContentEntry entry = registry.setSpawn(type, id, spawn);
        save();
        afterChange.run();
        return entry;
    }

    public Optional<ContentEntry> find(ContentType type, String id) {
        return registry.find(type, id);
    }

    public List<ContentEntry> list() {
        return registry.list();
    }

    public List<ContentEntry> list(ContentType type) {
        return registry.list(type);
    }

    public boolean regionServiceAvailable() {
        return regionService.available();
    }

    public void setAfterChange(Runnable afterChange) {
        this.afterChange = afterChange == null ? () -> {
        } : afterChange;
    }

    public File dataFile() {
        String dataPath = plugin.getConfig().getString("content-registry.data-file", "contents.yml");
        File configured = new File(dataPath == null || dataPath.isBlank() ? "contents.yml" : dataPath);
        return configured.isAbsolute() ? configured : new File(plugin.getDataFolder(), configured.getPath());
    }
}
