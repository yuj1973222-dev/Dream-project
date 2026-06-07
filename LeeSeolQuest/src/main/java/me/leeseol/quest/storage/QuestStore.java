package me.leeseol.quest.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.leeseol.quest.LeeSeolQuestPlugin;
import me.leeseol.quest.model.PlayerQuestData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class QuestStore {
    private final LeeSeolQuestPlugin plugin;
    private final Map<UUID, PlayerQuestData> players = new HashMap<>();
    private File file;

    public QuestStore(LeeSeolQuestPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        players.clear();
        String configuredPath = plugin.getConfig().getString("settings.data-file", "");
        file = configuredPath == null || configuredPath.isBlank()
            ? new File(plugin.getDataFolder(), "data.yml")
            : new File(configuredPath);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Failed to create quest data directory: " + parent);
        }
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection playersSection = yaml.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String key : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection section = playersSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }

                PlayerQuestData data = new PlayerQuestData();
                data.activeQuestId(section.getString("active-quest"));
                data.stageNumber(section.getInt("stage"));
                data.progress(section.getInt("progress"));
                data.completedQuests().addAll(section.getStringList("completed"));
                players.put(uuid, data);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipping invalid quest player UUID: " + key);
            }
        }
    }

    public void save() {
        if (file == null) {
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerQuestData> entry : players.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerQuestData data = entry.getValue();
            yaml.set(path + ".active-quest", data.activeQuestId());
            yaml.set(path + ".stage", data.stageNumber());
            yaml.set(path + ".progress", data.progress());
            yaml.set(path + ".completed", List.copyOf(data.completedQuests()));
        }

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save quest data: " + exception.getMessage());
        }
    }

    public PlayerQuestData data(UUID uuid) {
        return players.computeIfAbsent(uuid, ignored -> new PlayerQuestData());
    }

    public void reset(UUID uuid) {
        players.remove(uuid);
    }
}
