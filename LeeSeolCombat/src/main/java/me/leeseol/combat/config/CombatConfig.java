package me.leeseol.combat.config;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public final class CombatConfig {
    private boolean combatTagEnabled;
    private int combatDurationSeconds;
    private Set<String> combatWorlds;
    private boolean notifyOnRefresh;
    private boolean combatLogoutKill;
    private boolean cloneEnabled;
    private Set<String> cloneWorlds;
    private String cloneMode;
    private String cloneNameFormat;
    private boolean removeCloneOnReturn;
    private boolean dropInventoryOnCloneKill;
    private boolean applyDeathOnNextJoin;
    private boolean showHealthInName;
    private boolean spectatorCloneEnabled;
    private boolean hitboxEnabled;
    private double hitboxYOffset;
    private double hitboxWidth;
    private double hitboxHeight;
    private int hitboxLengthBlocks;
    private String hitboxDirection;
    private boolean hitboxGroundOnSpawn;
    private int hitboxMaxGroundSearchBlocks;
    private boolean hitboxSyncToNpc;
    private long hitboxSyncIntervalTicks;
    private boolean respawnDeadOnJoin;
    private boolean teleportToSpawnOnPendingDeath;
    private String pendingDeathSpawnWorld;
    private boolean pvpRewardsEnabled;
    private Set<String> pvpRewardWorlds;
    private int pvpPointsPerKill;
    private long pvpRepeatKillCooldownSeconds;
    private boolean pvpIgnoreSameTownOrNation;
    private boolean pvpTrophyEnabled;

    public void load(FileConfiguration config) {
        combatTagEnabled = config.getBoolean("combat-tag.enabled", true);
        combatDurationSeconds = Math.max(1, config.getInt("combat-tag.duration-seconds", 15));
        combatWorlds = readWorlds(config.getStringList("combat-tag.enabled-worlds"));
        notifyOnRefresh = config.getBoolean("combat-tag.notify-on-refresh", false);
        combatLogoutKill = config.getBoolean("combat-logout.kill-during-combat", true);
        cloneEnabled = config.getBoolean("logout-clone.enabled", true);
        cloneWorlds = readWorlds(config.getStringList("logout-clone.enabled-worlds"));
        cloneMode = config.getString("logout-clone.mode", "lying-corpse").toLowerCase(Locale.ROOT);
        cloneNameFormat = config.getString("logout-clone.name-format", "&c%player%의 전투 잔상");
        removeCloneOnReturn = config.getBoolean("logout-clone.remove-on-owner-return", true);
        dropInventoryOnCloneKill = config.getBoolean("logout-clone.drop-inventory-on-kill", true);
        applyDeathOnNextJoin = config.getBoolean("logout-clone.apply-death-on-next-join", true);
        showHealthInName = config.getBoolean("logout-clone.show-health-in-name", true);
        spectatorCloneEnabled = config.getBoolean("spectator-clone.enabled", true);
        hitboxEnabled = config.getBoolean("logout-clone.hitbox.enabled", true);
        hitboxYOffset = config.getDouble("logout-clone.hitbox.y-offset", 0.05D);
        hitboxWidth = Math.max(0.1D, config.getDouble("logout-clone.hitbox.width", 1.0D));
        hitboxHeight = Math.max(0.1D, config.getDouble("logout-clone.hitbox.height", 0.3D));
        hitboxLengthBlocks = Math.max(1, Math.min(4, config.getInt("logout-clone.hitbox.length-blocks", 2)));
        hitboxDirection = config.getString("logout-clone.hitbox.direction", "PLAYER").toUpperCase(Locale.ROOT);
        hitboxGroundOnSpawn = config.getBoolean("logout-clone.hitbox.ground-on-spawn", true);
        hitboxMaxGroundSearchBlocks = Math.max(1, Math.min(256, config.getInt("logout-clone.hitbox.max-ground-search-blocks", 48)));
        hitboxSyncToNpc = config.getBoolean("logout-clone.hitbox.sync-to-npc", false);
        hitboxSyncIntervalTicks = Math.max(1L, Math.min(20L, config.getLong("logout-clone.hitbox.sync-interval-ticks", 2L)));
        respawnDeadOnJoin = config.getBoolean("death-cleanup.respawn-dead-on-join", true);
        teleportToSpawnOnPendingDeath = config.getBoolean("death-cleanup.teleport-to-spawn-on-pending-death", true);
        pendingDeathSpawnWorld = config.getString("death-cleanup.spawn-world", "world");
        pvpRewardsEnabled = config.getBoolean("pvp-rewards.enabled", true);
        pvpRewardWorlds = readWorlds(config.getStringList("pvp-rewards.enabled-worlds"));
        pvpPointsPerKill = Math.max(0, config.getInt("pvp-rewards.points-per-kill", 1));
        pvpRepeatKillCooldownSeconds = Math.max(0L, config.getLong("pvp-rewards.repeat-kill-cooldown-seconds", 600L));
        pvpIgnoreSameTownOrNation = config.getBoolean("pvp-rewards.ignore-same-town-or-nation", true);
        pvpTrophyEnabled = config.getBoolean("pvp-rewards.trophy.enabled", true);
    }

    public boolean isCombatTagEnabled() {
        return combatTagEnabled;
    }

    public int combatDurationSeconds() {
        return combatDurationSeconds;
    }

    public boolean notifyOnRefresh() {
        return notifyOnRefresh;
    }

    public boolean combatLogoutKill() {
        return combatLogoutKill;
    }

    public boolean isCloneEnabled() {
        return cloneEnabled;
    }

    public String cloneNameFormat() {
        return cloneNameFormat;
    }

    public String cloneMode() {
        return cloneMode;
    }

    public boolean removeCloneOnReturn() {
        return removeCloneOnReturn;
    }

    public boolean dropInventoryOnCloneKill() {
        return dropInventoryOnCloneKill;
    }

    public boolean applyDeathOnNextJoin() {
        return applyDeathOnNextJoin;
    }

    public boolean showHealthInName() {
        return showHealthInName;
    }

    public boolean spectatorCloneEnabled() {
        return spectatorCloneEnabled;
    }

    public boolean hitboxEnabled() {
        return hitboxEnabled;
    }

    public double hitboxYOffset() {
        return hitboxYOffset;
    }

    public double hitboxWidth() {
        return hitboxWidth;
    }

    public double hitboxHeight() {
        return hitboxHeight;
    }

    public int hitboxLengthBlocks() {
        return hitboxLengthBlocks;
    }

    public String hitboxDirection() {
        return hitboxDirection;
    }

    public boolean hitboxGroundOnSpawn() {
        return hitboxGroundOnSpawn;
    }

    public int hitboxMaxGroundSearchBlocks() {
        return hitboxMaxGroundSearchBlocks;
    }

    public boolean hitboxSyncToNpc() {
        return hitboxSyncToNpc;
    }

    public long hitboxSyncIntervalTicks() {
        return hitboxSyncIntervalTicks;
    }

    public boolean respawnDeadOnJoin() {
        return respawnDeadOnJoin;
    }

    public boolean teleportToSpawnOnPendingDeath() {
        return teleportToSpawnOnPendingDeath;
    }

    public String pendingDeathSpawnWorld() {
        return pendingDeathSpawnWorld;
    }

    public boolean pvpRewardsEnabled() {
        return pvpRewardsEnabled;
    }

    public int pvpPointsPerKill() {
        return pvpPointsPerKill;
    }

    public long pvpRepeatKillCooldownSeconds() {
        return pvpRepeatKillCooldownSeconds;
    }

    public boolean pvpIgnoreSameTownOrNation() {
        return pvpIgnoreSameTownOrNation;
    }

    public boolean pvpTrophyEnabled() {
        return pvpTrophyEnabled;
    }

    public boolean isCombatWorld(Player player) {
        return player != null && isWorldEnabled(player.getWorld(), combatWorlds);
    }

    public boolean isCloneWorld(Player player) {
        return player != null && isWorldEnabled(player.getWorld(), cloneWorlds);
    }

    public boolean isPvpRewardWorld(Player player) {
        return player != null && isWorldEnabled(player.getWorld(), pvpRewardWorlds);
    }

    public int combatWorldCount() {
        return combatWorlds.size();
    }

    public int cloneWorldCount() {
        return cloneWorlds.size();
    }

    private static boolean isWorldEnabled(World world, Set<String> worlds) {
        if (world == null) {
            return false;
        }
        return worlds.isEmpty() || worlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    private static Set<String> readWorlds(List<String> values) {
        Set<String> worlds = new HashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                worlds.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return worlds;
    }
}
