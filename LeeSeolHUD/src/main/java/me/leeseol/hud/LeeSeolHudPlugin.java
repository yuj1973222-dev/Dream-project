package me.leeseol.hud;

import me.leeseol.hud.command.HudAdminCommand;
import me.leeseol.hud.command.HudCommand;
import me.leeseol.hud.hook.HudPlaceholderExpansion;
import me.leeseol.hud.listener.HealthDisplayListener;
import me.leeseol.hud.listener.HudPlayerListener;
import me.leeseol.hud.listener.TargetDamageListener;
import me.leeseol.hud.service.CompassHudService;
import me.leeseol.hud.service.HealthDisplayService;
import me.leeseol.hud.service.PlayerHudStateService;
import me.leeseol.hud.service.TargetHealthService;
import me.leeseol.hud.storage.HudStore;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolHudPlugin extends JavaPlugin {
    private HudStore hudStore;
    private PlayerHudStateService stateService;
    private CompassHudService compassHudService;
    private HealthDisplayService healthDisplayService;
    private TargetHealthService targetHealthService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeServices();
        registerCommands();
        registerListeners();
        registerPlaceholders();
        startServices();
        getLogger().info("LeeSeolHUD enabled.");
    }

    @Override
    public void onDisable() {
        if (compassHudService != null) {
            compassHudService.stop();
        }
        if (targetHealthService != null) {
            targetHealthService.stop();
        }
        if (hudStore != null) {
            hudStore.save();
        }
    }

    public void reloadHud() {
        reloadConfig();
        hudStore.load();
        startServices();
        Bukkit.getOnlinePlayers().forEach(compassHudService::update);
    }

    public PlayerHudStateService stateService() {
        return stateService;
    }

    public CompassHudService compassHudService() {
        return compassHudService;
    }

    public TargetHealthService targetHealthService() {
        return targetHealthService;
    }

    public HealthDisplayService healthDisplayService() {
        return healthDisplayService;
    }

    private void initializeServices() {
        hudStore = new HudStore(this);
        hudStore.load();
        stateService = new PlayerHudStateService(hudStore);
        compassHudService = new CompassHudService(this, stateService);
        healthDisplayService = new HealthDisplayService(this);
        targetHealthService = new TargetHealthService(this, stateService);
    }

    private void startServices() {
        compassHudService.start();
        targetHealthService.start();
    }

    private void registerCommands() {
        HudCommand hudCommand = new HudCommand(this);
        PluginCommand hud = getCommand("hud");
        if (hud != null) {
            hud.setExecutor(hudCommand);
            hud.setTabCompleter(hudCommand);
        }
        PluginCommand compassHud = getCommand("compasshud");
        if (compassHud != null) {
            compassHud.setExecutor(hudCommand);
            compassHud.setTabCompleter(hudCommand);
        }

        HudAdminCommand adminCommand = new HudAdminCommand(this);
        PluginCommand lshud = getCommand("lshud");
        if (lshud != null) {
            lshud.setExecutor(adminCommand);
            lshud.setTabCompleter(adminCommand);
        }
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new HudPlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new HealthDisplayListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TargetDamageListener(this), this);
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new HudPlaceholderExpansion(this).register();
            getLogger().info("Registered PlaceholderAPI expansion: leeseolhud");
        }
    }
}
