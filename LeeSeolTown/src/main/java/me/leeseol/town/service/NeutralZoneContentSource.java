package me.leeseol.town.service;

import me.leeseol.town.model.NeutralZone;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public final class NeutralZoneContentSource {
    private NeutralZoneContentSource() {
    }

    public static List<NeutralZone> loadCoreNeutralZones(ConfigurationSection data, int claimBufferChunks) {
        ConfigurationSection neutral = data.getConfigurationSection("contents.neutral");
        if (neutral == null) {
            return List.of();
        }

        List<NeutralZone> zones = new ArrayList<>();
        for (String id : neutral.getKeys(false)) {
            ConfigurationSection section = neutral.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            String world = section.getString("world", "");
            if (world.isBlank()) {
                continue;
            }
            zones.add(new NeutralZone(
                    id,
                    world,
                    Math.min(section.getInt("box.min.x"), section.getInt("box.max.x")),
                    Math.min(section.getInt("box.min.y"), section.getInt("box.max.y")),
                    Math.min(section.getInt("box.min.z"), section.getInt("box.max.z")),
                    Math.max(section.getInt("box.min.x"), section.getInt("box.max.x")),
                    Math.max(section.getInt("box.min.y"), section.getInt("box.max.y")),
                    Math.max(section.getInt("box.min.z"), section.getInt("box.max.z")),
                    Math.max(0, claimBufferChunks)
            ));
        }
        return zones;
    }
}
