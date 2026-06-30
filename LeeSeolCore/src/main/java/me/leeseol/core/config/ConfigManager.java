package me.leeseol.core.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.World;

public final class ConfigManager {
    private final LeeSeolCorePlugin plugin;
    private Set<String> enabledWorlds = Collections.emptySet();

    public ConfigManager(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        List<String> worlds = plugin.getConfig().getStringList("enabled-worlds");
        Set<String> loadedWorlds = new HashSet<>();

        for (String world : worlds) {
            if (world == null || world.isBlank()) {
                continue;
            }
            loadedWorlds.add(world.toLowerCase(Locale.ROOT));
        }

        enabledWorlds = Collections.unmodifiableSet(loadedWorlds);
    }

    public boolean isWorldEnabled(World world) {
        return world != null && enabledWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    public Set<String> getEnabledWorlds() {
        return enabledWorlds;
    }
}
