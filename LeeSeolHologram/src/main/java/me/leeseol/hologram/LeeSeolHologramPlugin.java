package me.leeseol.hologram;

import me.leeseol.hologram.command.HologramCommand;
import me.leeseol.hologram.service.HologramService;
import me.leeseol.hologram.storage.HologramStore;
import me.leeseol.hologram.util.Text;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolHologramPlugin extends JavaPlugin {
    private HologramStore store;
    private HologramService hologramService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createServices();
        registerCommands();
        getLogger().info("LeeSeolHologram enabled. holograms=" + hologramService.holograms().size());
    }

    @Override
    public void onDisable() {
        if (hologramService != null) {
            hologramService.shutdown();
        }
    }

    public void reloadAll() {
        reloadConfig();
        hologramService.reload();
    }

    private void createServices() {
        if (hologramService != null) {
            hologramService.shutdown();
        }
        this.store = new HologramStore(this);
        this.hologramService = new HologramService(this, store);
        this.hologramService.reload();
    }

    private void registerCommands() {
        HologramCommand hologramCommand = new HologramCommand(this, hologramService);
        PluginCommand command = getCommand("holo");
        if (command == null) {
            getLogger().warning("Command is missing from plugin.yml: holo");
            return;
        }
        command.setExecutor(hologramCommand);
        command.setTabCompleter(hologramCommand);
    }

    public String msg(String key) {
        return Text.color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages." + key, ""));
    }

    public double defaultLineSpacing() {
        return getConfig().getDouble("settings.default-line-spacing", 0.28D);
    }

    public double createYOffset() {
        return getConfig().getDouble("settings.create-y-offset", 2.2D);
    }

    public HologramService hologramService() {
        return hologramService;
    }
}
