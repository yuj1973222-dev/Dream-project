package me.leeseol.combat;

import me.leeseol.combat.command.CombatCommand;
import me.leeseol.combat.config.CombatConfig;
import me.leeseol.combat.listener.CitizensCombatCloneListener;
import me.leeseol.combat.listener.HitboxCleanupListener;
import me.leeseol.combat.listener.PvpCombatListener;
import me.leeseol.combat.listener.PvpRewardListener;
import me.leeseol.combat.listener.SessionListener;
import me.leeseol.combat.manager.CombatCloneManager;
import me.leeseol.combat.manager.CombatTagManager;
import me.leeseol.combat.service.PvpRewardService;
import me.leeseol.combat.storage.PendingDeathStore;
import me.leeseol.combat.storage.PvpPointStore;
import me.leeseol.combat.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolCombatPlugin extends JavaPlugin {
    private final CombatConfig combatConfig = new CombatConfig();
    private CombatTagManager combatTagManager;
    private CombatCloneManager combatCloneManager;
    private PendingDeathStore pendingDeathStore;
    private PvpPointStore pvpPointStore;
    private PvpRewardService pvpRewardService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        pendingDeathStore = new PendingDeathStore(this);
        pendingDeathStore.load();
        pvpPointStore = new PvpPointStore(this);
        pvpPointStore.load();
        combatTagManager = new CombatTagManager(this);
        combatCloneManager = new CombatCloneManager(this);
        pvpRewardService = new PvpRewardService(this, pvpPointStore);
        reloadPluginConfig();
        combatCloneManager.initializeRegistry();
        combatCloneManager.cleanupStaleHitboxes();
        getServer().getScheduler().runTaskLater(this, () -> combatCloneManager.cleanupStaleHitboxes(), 40L);

        getServer().getPluginManager().registerEvents(new PvpCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new SessionListener(this), this);
        getServer().getPluginManager().registerEvents(new CitizensCombatCloneListener(this), this);
        getServer().getPluginManager().registerEvents(new HitboxCleanupListener(this), this);
        getServer().getPluginManager().registerEvents(new PvpRewardListener(this), this);
        getServer().getScheduler().runTaskTimer(this, combatTagManager::tick, 20L, 20L);
        getCommand("leeseolcombat").setExecutor(new CombatCommand(this));

        getLogger().info("LeeSeolCombat enabled. combatWorlds=" + combatConfig.combatWorldCount()
                + ", cloneWorlds=" + combatConfig.cloneWorldCount());
    }

    @Override
    public void onDisable() {
        if (combatCloneManager != null) {
            combatCloneManager.removeAll();
        }
        if (pvpRewardService != null) {
            pvpRewardService.save();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        combatConfig.load(getConfig());
        if (pvpPointStore != null) {
            pvpPointStore.load();
        }
    }

    public void setSpectatorCloneEnabled(boolean enabled) {
        getConfig().set("spectator-clone.enabled", enabled);
        saveConfig();
        combatConfig.load(getConfig());
    }

    public CombatConfig combatConfig() {
        return combatConfig;
    }

    public CombatTagManager combatTagManager() {
        return combatTagManager;
    }

    public CombatCloneManager combatCloneManager() {
        return combatCloneManager;
    }

    public PendingDeathStore pendingDeathStore() {
        return pendingDeathStore;
    }

    public PvpRewardService pvpRewardService() {
        return pvpRewardService;
    }

    public void message(CommandSender sender, String key, String... replacements) {
        String prefix = getConfig().getString("messages.prefix", "");
        String message = getConfig().getString("messages." + key, "");
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }
        Text.send(sender, prefix + message);
    }
}
