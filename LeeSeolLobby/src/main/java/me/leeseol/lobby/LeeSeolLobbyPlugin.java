package me.leeseol.lobby;

import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolLobbyPlugin extends JavaPlugin implements Listener {
    private static final String BYPASS_PERMISSION = "leeseollobby.bypass";
    private static final String DEFAULT_LOGO_GLYPH = "\uE301";
    private int tabHeaderTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().runTask(this, () -> {
            applyWorldRules();
            getServer().getOnlinePlayers().forEach(this::preparePlayer);
        });

        startTabHeaderTask();
    }

    @Override
    public void onDisable() {
        if (tabHeaderTaskId != -1) {
            getServer().getScheduler().cancelTask(tabHeaderTaskId);
            tabHeaderTaskId = -1;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("lobbysetspawn")) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in game.");
            return true;
        }

        if (!player.hasPermission("leeseollobby.admin")) {
            player.sendMessage(ChatColor.RED + "亦낅슦釉????곷뮸??덈뼄.");
            return true;
        }

        Location location = player.getLocation();
        getConfig().set("world", location.getWorld().getName());
        getConfig().set("spawn.use-world-spawn", false);
        getConfig().set("spawn.x", location.getX());
        getConfig().set("spawn.y", location.getY());
        getConfig().set("spawn.z", location.getZ());
        getConfig().set("spawn.yaw", location.getYaw());
        getConfig().set("spawn.pitch", location.getPitch());
        saveConfig();

        location.getWorld().setSpawnLocation(location);
        player.sendMessage(color(getConfig().getString("messages.spawn-set")));
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        int delay = Math.max(1, getConfig().getInt("spawn.teleport-delay-ticks", 5));
        getServer().getScheduler().runTaskLater(this, () -> preparePlayer(event.getPlayer()), delay);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(spawnLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!getConfig().getBoolean("rules.prevent-block-break", true) || canBypass(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(color(getConfig().getString("messages.build-denied")));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!getConfig().getBoolean("rules.prevent-block-place", true) || canBypass(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(color(getConfig().getString("messages.build-denied")));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (getConfig().getBoolean("rules.prevent-damage", true) && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!getConfig().getBoolean("rules.prevent-hunger", true) || !(event.getEntity() instanceof Player player)) {
            return;
        }

        event.setCancelled(true);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (getConfig().getBoolean("rules.prevent-mob-spawning", true)
            && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            event.setCancelled(true);
        }
    }

    private void preparePlayer(Player player) {
        Location spawn = spawnLocation();
        player.teleport(spawn);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);

        if (getConfig().getBoolean("rules.force-adventure", true) && !canBypass(player)) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        sendTabHeader(player);
    }

    private void startTabHeaderTask() {
        if (!getConfig().getBoolean("tab-header.enabled", true)) {
            return;
        }

        long interval = Math.max(20L, getConfig().getLong("tab-header.interval-ticks", 40L));
        tabHeaderTaskId = getServer().getScheduler().runTaskTimer(
            this,
            () -> getServer().getOnlinePlayers().forEach(this::sendTabHeader),
            20L,
            interval
        ).getTaskId();
    }

    private void sendTabHeader(Player player) {
        if (!getConfig().getBoolean("tab-header.enabled", true)) {
            return;
        }

        Component header = Component.text("\n".repeat(boundedConfigInt("tab-header.logo-leading-lines", 0, 0, 5)))
            .append(Component.text(logoGlyph())
            .font(Key.key(getConfig().getString("tab-header.logo-font", "minecraft:default")))
            .append(Component.text(" ".repeat(boundedConfigInt("tab-header.logo-trailing-spaces", 0, 0, 20)))))
            .append(Component.text("\n".repeat(1 + boundedConfigInt("tab-header.logo-ping-gap-lines", 0, 0, 5))))
            .append(Component.text(
                getConfig().getString("tab-header.ping-label", "PING : ") + player.getPing() + "ms",
                configTextColor("tab-header.ping-color", "#87DFFF")
            ));

        String footerText = getConfig().getString("tab-header.footer", "\uC628\uB77C\uC778 \uC811\uC18D\uC790 : ")
            + getServer().getOnlinePlayers().size()
            + "\uBA85 / "
            + boundedConfigInt("tab-header.max-display-players", 500, 1, 100000)
            + "\uBA85";
        Component footer = Component.text(footerText, configTextColor("tab-header.footer-color", "#D8C6A3"));

        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    private int boundedConfigInt(String path, int defaultValue, int min, int max) {
        int value = getConfig().getInt(path, defaultValue);
        return Math.max(min, Math.min(max, value));
    }

    private TextColor configTextColor(String path, String defaultHex) {
        String configured = getConfig().getString(path, defaultHex);
        TextColor color = TextColor.fromHexString(configured == null ? defaultHex : configured);
        return color == null ? TextColor.fromHexString(defaultHex) : color;
    }

    private String logoGlyph() {
        String configured = getConfig().getString("tab-header.logo-glyph");
        if (configured == null || configured.isBlank()) {
            return DEFAULT_LOGO_GLYPH;
        }
        if (configured.startsWith("\\u") && configured.length() == 6) {
            try {
                return String.valueOf((char) Integer.parseInt(configured.substring(2), 16));
            } catch (NumberFormatException ignored) {
                return DEFAULT_LOGO_GLYPH;
            }
        }
        return configured;
    }

    private void applyWorldRules() {
        World world = lobbyWorld();
        if (getConfig().getBoolean("rules.force-peaceful", true)) {
            world.setDifficulty(Difficulty.PEACEFUL);
        }
        if (getConfig().getBoolean("rules.disable-pvp", true)) {
            world.setPVP(false);
        }
        if (getConfig().getBoolean("rules.prevent-mob-spawning", true)) {
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        }
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setSpawnLocation(spawnLocation());
    }

    private Location spawnLocation() {
        World world = lobbyWorld();
        if (getConfig().getBoolean("spawn.use-world-spawn", true)) {
            Location spawn = world.getSpawnLocation();
            return new Location(world, spawn.getBlockX() + 0.5D, spawn.getY(), spawn.getBlockZ() + 0.5D, spawn.getYaw(), spawn.getPitch());
        }

        return new Location(
            world,
            getConfig().getDouble("spawn.x"),
            getConfig().getDouble("spawn.y"),
            getConfig().getDouble("spawn.z"),
            (float) getConfig().getDouble("spawn.yaw"),
            (float) getConfig().getDouble("spawn.pitch")
        );
    }

    private World lobbyWorld() {
        String worldName = getConfig().getString("world", "world");
        return Objects.requireNonNull(getServer().getWorld(worldName), "Lobby world not found: " + worldName);
    }

    private boolean canBypass(Player player) {
        return player.hasPermission(BYPASS_PERMISSION);
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
