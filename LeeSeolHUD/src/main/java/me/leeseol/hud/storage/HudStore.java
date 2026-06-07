package me.leeseol.hud.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.leeseol.hud.LeeSeolHudPlugin;
import me.leeseol.hud.model.HudPreference;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class HudStore {
    private final LeeSeolHudPlugin plugin;
    private final Map<UUID, HudPreference> preferences = new HashMap<>();
    private File file;

    public HudStore(LeeSeolHudPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        preferences.clear();
        file = resolveFile();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                preferences.put(uuid, new HudPreference(
                    players.getBoolean(key + ".compass", defaultCompass()),
                    players.getBoolean(key + ".target-health", defaultTargetHealth())
                ));
            } catch (IllegalArgumentException ignored) {
                // Bad old data should not stop the server.
            }
        }
    }

    public HudPreference preference(UUID uuid) {
        return preferences.computeIfAbsent(uuid, ignored -> new HudPreference(defaultCompass(), defaultTargetHealth()));
    }

    public void save() {
        if (!plugin.getConfig().getBoolean("settings.save-preferences", true)) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, HudPreference> entry : preferences.entrySet()) {
            String path = "players." + entry.getKey();
            yaml.set(path + ".compass", entry.getValue().compass());
            yaml.set(path + ".target-health", entry.getValue().targetHealth());
        }
        try {
            File target = file == null ? resolveFile() : file;
            File parent = target.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(target);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save HUD preferences: " + exception.getMessage());
        }
    }

    private File resolveFile() {
        String configured = plugin.getConfig().getString("storage.data-file", "");
        if (configured != null && !configured.isBlank()) {
            return new File(configured);
        }
        return new File(plugin.getDataFolder(), "preferences.yml");
    }

    private boolean defaultCompass() {
        return plugin.getConfig().getBoolean("compass.default-enabled", true);
    }

    private boolean defaultTargetHealth() {
        return plugin.getConfig().getBoolean("target-health.default-enabled", true);
    }
}
