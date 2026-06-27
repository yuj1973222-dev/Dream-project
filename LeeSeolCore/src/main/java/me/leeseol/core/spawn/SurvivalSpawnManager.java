package me.leeseol.core.spawn;

import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SurvivalSpawnManager implements Listener, CommandExecutor {
    private static final String RESPAWN_PATH = "survival-respawn";
    private static final String RETURN_PATH = "survival-spawn-return";

    private final LeeSeolCorePlugin plugin;
    private final Map<UUID, String> deathWorlds = new HashMap<>();
    private final Map<UUID, BukkitTask> returnTasks = new HashMap<>();
    private final Map<UUID, BlockLocation> returnOrigins = new HashMap<>();
    private final Map<UUID, Long> returnStartedAt = new HashMap<>();

    private Set<String> respawnWorlds = Set.of();
    private Set<String> returnWorlds = Set.of();
    private long warmupMillis = 8000L;
    private long particleIntervalTicks = 10L;
    private int particleCount = 28;
    private double particleOffsetX = 0.45D;
    private double particleOffsetY = 0.85D;
    private double particleOffsetZ = 0.45D;
    private double particleSpeed = 0.02D;
    private boolean cancelOnMove = true;
    private boolean cancelOnDamage = true;
    private Particle particle = Particle.PORTAL;
    private Sound startSound;
    private Sound completeSound = Sound.ENTITY_ENDERMAN_TELEPORT;

    public SurvivalSpawnManager(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        respawnWorlds = loadWorldSet(RESPAWN_PATH + ".worlds");
        returnWorlds = loadWorldSet(RETURN_PATH + ".worlds");
        warmupMillis = Math.max(1L, plugin.getConfig().getLong(RETURN_PATH + ".warmup-seconds", 8L)) * 1000L;
        particleIntervalTicks = Math.max(1L, plugin.getConfig().getLong(RETURN_PATH + ".particle.interval-ticks", 10L));
        particleCount = Math.max(1, plugin.getConfig().getInt(RETURN_PATH + ".particle.count", 28));
        particleOffsetX = Math.max(0.0D, plugin.getConfig().getDouble(RETURN_PATH + ".particle.offset-x", 0.45D));
        particleOffsetY = Math.max(0.0D, plugin.getConfig().getDouble(RETURN_PATH + ".particle.offset-y", 0.85D));
        particleOffsetZ = Math.max(0.0D, plugin.getConfig().getDouble(RETURN_PATH + ".particle.offset-z", 0.45D));
        particleSpeed = Math.max(0.0D, plugin.getConfig().getDouble(RETURN_PATH + ".particle.speed", 0.02D));
        cancelOnMove = plugin.getConfig().getBoolean(RETURN_PATH + ".cancel-on-move", true);
        cancelOnDamage = plugin.getConfig().getBoolean(RETURN_PATH + ".cancel-on-damage", true);
        particle = parseParticle(plugin.getConfig().getString(RETURN_PATH + ".particle.type", "PORTAL"), Particle.PORTAL);
        startSound = parseSound(plugin.getConfig().getString(RETURN_PATH + ".sound.start", ""));
        completeSound = parseSound(plugin.getConfig().getString(RETURN_PATH + ".sound.complete", "ENTITY_ENDERMAN_TELEPORT"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message(RETURN_PATH + ".messages.player-only", "&c게임 안에서만 사용할 수 있습니다."));
            return true;
        }
        startReturn(player);
        return true;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        deathWorlds.put(player.getUniqueId(), player.getWorld().getName());
        cancelReturn(player, null);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean(RESPAWN_PATH + ".enabled", false)) {
            return;
        }

        String deathWorld = deathWorlds.remove(event.getPlayer().getUniqueId());
        if (deathWorld == null || !respawnWorlds.contains(deathWorld.toLowerCase(Locale.ROOT))) {
            return;
        }

        Location target = targetLocation(RESPAWN_PATH);
        if (target == null) {
            plugin.getLogger().warning("Survival respawn target is not available.");
            return;
        }
        event.setRespawnLocation(target);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!cancelOnMove) {
            return;
        }
        BlockLocation origin = returnOrigins.get(event.getPlayer().getUniqueId());
        if (origin == null || event.getTo() == null || origin.matches(event.getTo())) {
            return;
        }
        cancelReturn(event.getPlayer(), RETURN_PATH + ".messages.cancelled-move");
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!cancelOnDamage || !(event.getEntity() instanceof Player player)) {
            return;
        }
        cancelReturn(player, RETURN_PATH + ".messages.cancelled-damage");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelReturn(event.getPlayer(), null);
        deathWorlds.remove(event.getPlayer().getUniqueId());
    }

    private void startReturn(Player player) {
        if (!plugin.getConfig().getBoolean(RETURN_PATH + ".enabled", false)) {
            player.sendMessage(message(RETURN_PATH + ".messages.disabled", "&c현재 사용할 수 없습니다."));
            return;
        }
        if (!player.hasPermission("leeseolcore.survivalspawn")) {
            player.sendMessage(message(RETURN_PATH + ".messages.no-permission", "&c권한이 없습니다."));
            return;
        }
        if (!returnWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
            player.sendMessage(message(RETURN_PATH + ".messages.wrong-world", "&c이 월드에서는 사용할 수 없습니다."));
            return;
        }
        if (returnTasks.containsKey(player.getUniqueId())) {
            player.sendMessage(message(RETURN_PATH + ".messages.already-returning", "&e이미 귀환 중입니다."));
            return;
        }

        Location target = targetLocation(RETURN_PATH);
        if (target == null) {
            player.sendMessage(message(RETURN_PATH + ".messages.target-missing", "&c스폰 위치를 찾을 수 없습니다."));
            return;
        }

        UUID playerId = player.getUniqueId();
        returnOrigins.put(playerId, BlockLocation.from(player.getLocation()));
        returnStartedAt.put(playerId, System.currentTimeMillis());
        player.sendMessage(message(RETURN_PATH + ".messages.started", "&b%seconds%초 뒤 서바이벌 스폰으로 귀환합니다.")
                .replace("%seconds%", Long.toString(warmupMillis / 1000L)));
        playSound(player, startSound);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> tickReturn(playerId),
                1L,
                particleIntervalTicks
        );
        returnTasks.put(playerId, task);
    }

    private void tickReturn(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            cancelReturn(playerId);
            return;
        }

        if (System.currentTimeMillis() - returnStartedAt.getOrDefault(playerId, 0L) >= warmupMillis) {
            completeReturn(player);
            return;
        }

        Location particleLocation = player.getLocation().add(0.0D, 1.0D, 0.0D);
        player.getWorld().spawnParticle(
                particle,
                particleLocation,
                particleCount,
                particleOffsetX,
                particleOffsetY,
                particleOffsetZ,
                particleSpeed
        );
    }

    private void completeReturn(Player player) {
        cancelReturn(player.getUniqueId());
        Location target = targetLocation(RETURN_PATH);
        if (target == null) {
            player.sendMessage(message(RETURN_PATH + ".messages.target-missing", "&c스폰 위치를 찾을 수 없습니다."));
            return;
        }
        player.teleport(target);
        playSound(player, completeSound);
        player.sendMessage(message(RETURN_PATH + ".messages.completed", "&a서바이벌 스폰으로 이동했습니다."));
    }

    private void cancelReturn(Player player, String messagePath) {
        if (!returnTasks.containsKey(player.getUniqueId())) {
            return;
        }
        cancelReturn(player.getUniqueId());
        if (messagePath != null) {
            player.sendMessage(message(messagePath, "&c귀환이 취소되었습니다."));
        }
    }

    private void cancelReturn(UUID playerId) {
        BukkitTask task = returnTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        returnOrigins.remove(playerId);
        returnStartedAt.remove(playerId);
    }

    private Location targetLocation(String basePath) {
        String worldName = plugin.getConfig().getString(basePath + ".target-world", "world");
        World world = plugin.getServer().getWorld(worldName == null ? "world" : worldName);
        if (world == null) {
            return null;
        }
        if (!plugin.getConfig().getBoolean(basePath + ".target-location.enabled", false)) {
            return world.getSpawnLocation();
        }
        double x = plugin.getConfig().getDouble(basePath + ".target-location.x", world.getSpawnLocation().getX());
        double y = plugin.getConfig().getDouble(basePath + ".target-location.y", world.getSpawnLocation().getY());
        double z = plugin.getConfig().getDouble(basePath + ".target-location.z", world.getSpawnLocation().getZ());
        float yaw = (float) plugin.getConfig().getDouble(basePath + ".target-location.yaw", world.getSpawnLocation().getYaw());
        float pitch = (float) plugin.getConfig().getDouble(basePath + ".target-location.pitch", world.getSpawnLocation().getPitch());
        return new Location(world, x, y, z, yaw, pitch);
    }

    private Set<String> loadWorldSet(String path) {
        Set<String> worlds = new HashSet<>();
        for (String worldName : plugin.getConfig().getStringList(path)) {
            if (worldName != null && !worldName.isBlank()) {
                worlds.add(worldName.toLowerCase(Locale.ROOT));
            }
        }
        return worlds;
    }

    private Particle parseParticle(String raw, Particle fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Particle.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid survival-spawn-return particle: " + raw);
            return fallback;
        }
    }

    private Sound parseSound(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Sound.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid survival-spawn-return sound: " + raw);
            return null;
        }
    }

    private void playSound(Player player, Sound sound) {
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
        }
    }

    private String message(String path, String fallback) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(path, fallback));
    }

    private record BlockLocation(String worldName, int x, int y, int z) {
        private static BlockLocation from(Location location) {
            return new BlockLocation(
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );
        }

        private boolean matches(Location location) {
            return location.getWorld() != null
                    && worldName.equalsIgnoreCase(location.getWorld().getName())
                    && x == location.getBlockX()
                    && y == location.getBlockY()
                    && z == location.getBlockZ();
        }
    }
}
