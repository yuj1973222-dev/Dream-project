package me.leeseol.lobby;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.leeseol.lobby.limbo.LimboCommandPolicy;
import me.leeseol.lobby.limbo.LobbyQueuePluginMessage;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class LeeSeolLobbyPlugin extends JavaPlugin implements Listener, PluginMessageListener {
    private static final String QUEUE_CHANNEL = "leeseol:queue";
    private static final String BYPASS_PERMISSION = "leeseollobby.bypass";
    private static final String DEFAULT_LOGO_GLYPH = "\uE301";
    private final Set<UUID> limboPlayers = ConcurrentHashMap.newKeySet();
    private int tabHeaderTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerIncomingPluginChannel(this, QUEUE_CHANNEL, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, QUEUE_CHANNEL);

        getServer().getScheduler().runTask(this, () -> {
            applyWorldRules();
            ensureLimboWorld();
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
        getServer().getMessenger().unregisterIncomingPluginChannel(this, QUEUE_CHANNEL, this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, QUEUE_CHANNEL);
        limboPlayers.clear();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player carrier, byte[] message) {
        if (!QUEUE_CHANNEL.equals(channel)) {
            return;
        }

        LobbyQueuePluginMessage.read(message).ifPresent(payload -> {
            if (LobbyQueuePluginMessage.LIMBO_REQUEST.equals(payload.action())) {
                handleLimboRequest(carrier, payload);
            } else if (LobbyQueuePluginMessage.LOBBY_REQUEST.equals(payload.action())) {
                Player target = getServer().getPlayer(payload.playerId());
                if (target != null) {
                    leaveLimbo(target, true);
                }
            }
        });
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
            player.sendMessage(ChatColor.RED + "로비 관리 권한이 없습니다.");
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
        if (isLimbo(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(color(getConfig().getString("messages.limbo-restricted")));
            return;
        }
        if (!getConfig().getBoolean("rules.prevent-block-break", true) || canBypass(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(color(getConfig().getString("messages.build-denied")));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isLimbo(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(color(getConfig().getString("messages.limbo-restricted")));
            return;
        }
        if (!getConfig().getBoolean("rules.prevent-block-place", true) || canBypass(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(color(getConfig().getString("messages.build-denied")));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (isLimbo(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(color(getConfig().getString("messages.limbo-restricted")));
            return;
        }
        if (!getConfig().getBoolean("rules.prevent-item-drop", true) || canBypass(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(color(getConfig().getString("messages.item-drop-denied")));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isLimbo(player)) {
            event.setCancelled(true);
            return;
        }
        if (getConfig().getBoolean("rules.prevent-damage", true) && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && isLimbo(player)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            return;
        }
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        if (!getConfig().getBoolean("void-return.enabled", true)) {
            return;
        }
        if (event.getTo() == null || event.getTo().getY() > getConfig().getDouble("void-return.y", -16.0D)) {
            return;
        }
        if (!event.getPlayer().getWorld().equals(lobbyWorld())) {
            return;
        }

        event.getPlayer().teleport(spawnLocation());
        event.getPlayer().sendMessage(color(getConfig().getString("messages.void-return")));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!isLimbo(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(color(getConfig().getString("messages.limbo-restricted")));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!isLimbo(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(color(getConfig().getString("messages.limbo-restricted")));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !isLimbo(player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!isLimbo(event.getPlayer())) {
            return;
        }

        if (!limboCommandPolicy().isAllowed(event.getMessage())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(color(getConfig().getString("messages.limbo-command-denied")));
            return;
        }

        event.setCancelled(true);
        leaveLimbo(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!isLimboWorld(event.getFrom()) || isLimbo(player)) {
            return;
        }

        if (limboPlayers.remove(player.getUniqueId())) {
            sendQueueLeave(player, "left-limbo-world");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        limboPlayers.remove(event.getPlayer().getUniqueId());
    }

    private void preparePlayer(Player player) {
        if (isLimbo(player)) {
            sendTabHeader(player);
            return;
        }

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

    private void handleLimboRequest(Player carrier, LobbyQueuePluginMessage.Message payload) {
        Player target = getServer().getPlayer(payload.playerId());
        if (target == null) {
            sendLimboResult(carrier, payload, false, getConfig().getString("messages.limbo-player-missing"));
            return;
        }

        String requestedWorld = payload.limboWorld() == null || payload.limboWorld().isBlank()
                ? getConfig().getString("limbo.world", "limbo")
                : payload.limboWorld();
        World limboWorld = limboWorld(requestedWorld);
        if (limboWorld == null) {
            limboPlayers.remove(target.getUniqueId());
            sendLimboResult(carrier, payload, false, getConfig().getString("messages.limbo-world-missing"));
            target.sendMessage(color(getConfig().getString("messages.limbo-world-missing")));
            return;
        }

        boolean moved = target.teleport(limboSpawnLocation(limboWorld));
        if (!moved) {
            limboPlayers.remove(target.getUniqueId());
            sendLimboResult(carrier, payload, false, getConfig().getString("messages.limbo-teleport-failed"));
            target.sendMessage(color(getConfig().getString("messages.limbo-teleport-failed")));
            return;
        }

        limboPlayers.add(target.getUniqueId());
        target.setGameMode(GameMode.ADVENTURE);
        target.sendMessage(color(getConfig().getString("messages.limbo-entered")));
        sendLimboResult(carrier, payload, true, "");
    }

    private void leaveLimbo(Player player, boolean notifyProxy) {
        boolean wasWaiting = limboPlayers.remove(player.getUniqueId()) || isLimbo(player);
        player.teleport(spawnLocation());
        player.sendMessage(color(getConfig().getString("messages.limbo-left")));
        if (notifyProxy && wasWaiting) {
            sendQueueLeave(player, "lobby-command");
        }
    }

    private Location limboSpawnLocation(World world) {
        if (getConfig().getBoolean("limbo.spawn.use-world-spawn", true)) {
            Location spawn = world.getSpawnLocation();
            return new Location(world, spawn.getBlockX() + 0.5D, spawn.getY(), spawn.getBlockZ() + 0.5D, spawn.getYaw(), spawn.getPitch());
        }

        return new Location(
            world,
            getConfig().getDouble("limbo.spawn.x"),
            getConfig().getDouble("limbo.spawn.y"),
            getConfig().getDouble("limbo.spawn.z"),
            (float) getConfig().getDouble("limbo.spawn.yaw"),
            (float) getConfig().getDouble("limbo.spawn.pitch")
        );
    }

    private World ensureLimboWorld() {
        return limboWorld(getConfig().getString("limbo.world", "limbo"));
    }

    private World limboWorld(String worldName) {
        World world = getServer().getWorld(worldName);
        if (world != null || !getConfig().getBoolean("limbo.create-if-missing", true)) {
            return world;
        }

        World created = new WorldCreator(worldName)
            .generateStructures(false)
            .createWorld();
        if (created != null) {
            applyLimboWorldRules(created);
        }
        return created;
    }

    private void applyLimboWorldRules(World world) {
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setPVP(false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setSpawnLocation(limboSpawnLocation(world));
    }

    private LimboCommandPolicy limboCommandPolicy() {
        Set<String> allowed = new HashSet<>(getConfig().getStringList("limbo.allowed-commands"));
        if (allowed.isEmpty()) {
            allowed.add("lobby");
            allowed.add("로비");
        }
        return new LimboCommandPolicy(allowed);
    }

    private boolean isLimbo(Player player) {
        return isLimboWorld(player.getWorld());
    }

    private boolean isLimboWorld(World world) {
        return world != null && world.getName().equalsIgnoreCase(getConfig().getString("limbo.world", "limbo"));
    }

    private void sendLimboResult(Player carrier, LobbyQueuePluginMessage.Message payload, boolean success, String message) {
        carrier.sendPluginMessage(
            this,
            QUEUE_CHANNEL,
            LobbyQueuePluginMessage.limboResult(payload.requestId(), payload.playerId(), success, message == null ? "" : message)
        );
    }

    private void sendQueueLeave(Player player, String reason) {
        player.sendPluginMessage(this, QUEUE_CHANNEL, LobbyQueuePluginMessage.queueLeave(player.getUniqueId(), reason));
    }

    private boolean canBypass(Player player) {
        return player.hasPermission(BYPASS_PERMISSION);
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
