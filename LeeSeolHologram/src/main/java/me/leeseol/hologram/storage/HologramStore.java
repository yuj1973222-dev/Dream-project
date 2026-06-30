package me.leeseol.hologram.storage;

import me.leeseol.hologram.model.Hologram;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HologramStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Hologram> holograms = new LinkedHashMap<>();

    public HologramStore(JavaPlugin plugin) {
        this.plugin = plugin;
        String dataPath = plugin.getConfig().getString("storage.data-file", "data.yml");
        File configured = new File(dataPath == null || dataPath.isBlank() ? "data.yml" : dataPath);
        this.file = configured.isAbsolute() ? configured : new File(plugin.getDataFolder(), configured.getPath());
    }

    public void load() {
        holograms.clear();
        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = data.getConfigurationSection("holograms");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            String base = "holograms." + id + ".";
            List<String> lines = data.getStringList(base + "lines");
            if (lines.isEmpty()) {
                lines = List.of("");
            }

            Hologram hologram = new Hologram(
                    id,
                    data.getString(base + "world", ""),
                    data.getDouble(base + "x"),
                    data.getDouble(base + "y"),
                    data.getDouble(base + "z"),
                    (float) data.getDouble(base + "yaw"),
                    (float) data.getDouble(base + "pitch"),
                    data.getDouble(base + "line-spacing", plugin.getConfig().getDouble("settings.default-line-spacing", 0.28D)),
                    lines
            );
            holograms.put(id.toLowerCase(), hologram);
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Hologram hologram : holograms.values()) {
            String base = "holograms." + hologram.id() + ".";
            data.set(base + "world", hologram.worldName());
            data.set(base + "x", hologram.x());
            data.set(base + "y", hologram.y());
            data.set(base + "z", hologram.z());
            data.set(base + "yaw", hologram.yaw());
            data.set(base + "pitch", hologram.pitch());
            data.set(base + "line-spacing", hologram.lineSpacing());
            data.set(base + "lines", hologram.lines());
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save hologram data: " + exception.getMessage());
        }
    }

    public Hologram hologram(String id) {
        return holograms.get(normalize(id));
    }

    public Collection<Hologram> holograms() {
        return holograms.values();
    }

    public void put(Hologram hologram) {
        holograms.put(normalize(hologram.id()), hologram);
    }

    public Hologram remove(String id) {
        return holograms.remove(normalize(id));
    }

    public static String normalize(String id) {
        return id == null ? "" : id.toLowerCase();
    }
}
