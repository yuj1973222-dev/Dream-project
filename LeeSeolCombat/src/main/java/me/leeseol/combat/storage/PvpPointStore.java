package me.leeseol.combat.storage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.leeseol.combat.LeeSeolCombatPlugin;
import me.leeseol.combat.model.PvpRecord;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PvpPointStore {
    private final LeeSeolCombatPlugin plugin;
    private final Map<UUID, PvpRecord> records = new HashMap<>();
    private File file;

    public PvpPointStore(LeeSeolCombatPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        records.clear();
        file = resolveFile();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "players." + key;
                records.put(uuid, new PvpRecord(
                    uuid,
                    players.getString(key + ".name", ""),
                    yaml.getInt(path + ".points", 0),
                    yaml.getInt(path + ".kills", 0)
                ));
            } catch (IllegalArgumentException ignored) {
                // Ignore bad legacy entries.
            }
        }
    }

    public PvpRecord getOrCreate(OfflinePlayer player) {
        return records.computeIfAbsent(player.getUniqueId(), ignored ->
            new PvpRecord(player.getUniqueId(), player.getName() == null ? "" : player.getName(), 0, 0));
    }

    public Collection<PvpRecord> all() {
        return records.values();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PvpRecord record : records.values()) {
            String path = "players." + record.uuid();
            yaml.set(path + ".name", record.name());
            yaml.set(path + ".points", record.points());
            yaml.set(path + ".kills", record.kills());
        }
        try {
            File target = file == null ? resolveFile() : file;
            File parent = target.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(target);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save PVP point data: " + exception.getMessage());
        }
    }

    private File resolveFile() {
        String configured = plugin.getConfig().getString("pvp-rewards.data-file", "/opt/minecraft/shared/combat/pvp.yml");
        if (configured == null || configured.isBlank()) {
            configured = new File(plugin.getDataFolder(), "pvp.yml").getAbsolutePath();
        }
        return new File(configured);
    }
}
