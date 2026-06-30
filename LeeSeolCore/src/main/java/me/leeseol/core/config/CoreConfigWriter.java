package me.leeseol.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.configuration.ConfigurationSection;

public final class CoreConfigWriter {
    private final LeeSeolCorePlugin plugin;

    public CoreConfigWriter(LeeSeolCorePlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void set(String path, Object value) {
        plugin.getConfig().set(path, value);
    }

    public void setIfMissing(String path, Object value) {
        if (!plugin.getConfig().contains(path)) {
            plugin.getConfig().set(path, value);
        }
    }

    public void addEnabledWorld(String worldName) {
        List<String> worlds = new ArrayList<>(plugin.getConfig().getStringList("enabled-worlds"));
        for (String world : worlds) {
            if (world.equalsIgnoreCase(worldName)) {
                return;
            }
        }
        worlds.add(worldName);
        plugin.getConfig().set("enabled-worlds", worlds);
    }

    public void saveAndReload() {
        plugin.saveConfig();
        plugin.reloadCoreConfig();
    }

    public boolean isConfigurationSection(String path) {
        return plugin.getConfig().isConfigurationSection(path);
    }

    public ConfigurationSection section(String path) {
        return plugin.getConfig().getConfigurationSection(path);
    }

    public String getString(String path, String fallback) {
        return plugin.getConfig().getString(path, fallback);
    }

    public int getInt(String path) {
        return plugin.getConfig().getInt(path);
    }
}
