package me.leeseol.dungeon;

import me.leeseol.dungeon.command.DungeonCommand;
import me.leeseol.dungeon.inventory.InventorySyncManager;
import me.leeseol.dungeon.loot.LootChestManager;
import me.leeseol.dungeon.portal.DungeonPortalManager;
import me.leeseol.dungeon.portal.PortalListener;
import me.leeseol.dungeon.protection.DungeonProtectionListener;
import me.leeseol.dungeon.returning.ReturnLocationManager;
import me.leeseol.dungeon.util.Text;
import me.leeseol.dungeon.world.DungeonWorldManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolDungeonPlugin extends JavaPlugin {
    private InventorySyncManager inventorySyncManager;
    private DungeonPortalManager portalManager;
    private LootChestManager lootChestManager;
    private ReturnLocationManager returnLocationManager;
    private DungeonWorldManager worldManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        inventorySyncManager = new InventorySyncManager(this);
        returnLocationManager = new ReturnLocationManager(this);
        worldManager = new DungeonWorldManager(this);
        portalManager = new DungeonPortalManager(this, inventorySyncManager, returnLocationManager);
        lootChestManager = new LootChestManager(this);

        reloadDungeon();

        getServer().getPluginManager().registerEvents(inventorySyncManager, this);
        getServer().getPluginManager().registerEvents(returnLocationManager, this);
        getServer().getPluginManager().registerEvents(new PortalListener(portalManager), this);
        getServer().getPluginManager().registerEvents(new DungeonProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(lootChestManager, this);

        registerCommands();
        getLogger().info("LeeSeolDungeon enabled. role=" + serverRole()
                + ", portals=" + portalManager.portalCount()
                + ", lootSpots=" + lootChestManager.spotCount());
    }

    @Override
    public void onDisable() {
        if (lootChestManager != null) {
            lootChestManager.stop();
        }
        if (inventorySyncManager != null) {
            inventorySyncManager.saveOnlinePlayers();
            inventorySyncManager.saveStore();
        }
    }

    public void reloadDungeon() {
        reloadConfig();
        worldManager.reload();
        inventorySyncManager.reload();
        returnLocationManager.reload();
        portalManager.reload();
        lootChestManager.reload();
    }

    private void registerCommands() {
        DungeonCommand executor = new DungeonCommand(this, portalManager, lootChestManager);
        PluginCommand command = getCommand("dungeon");
        if (command == null) {
            getLogger().warning("Command missing from plugin.yml: dungeon");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public InventorySyncManager inventorySyncManager() {
        return inventorySyncManager;
    }

    public DungeonPortalManager portalManager() {
        return portalManager;
    }

    public LootChestManager lootChestManager() {
        return lootChestManager;
    }

    public DungeonWorldManager worldManager() {
        return worldManager;
    }

    public String serverRole() {
        return getConfig().getString("server.role", "both").toLowerCase();
    }

    public boolean roleAllows(String featureRole) {
        String role = serverRole();
        return role.equals("both") || role.equalsIgnoreCase(featureRole);
    }

    public String msg(String key) {
        return Text.color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages." + key, ""));
    }
}
