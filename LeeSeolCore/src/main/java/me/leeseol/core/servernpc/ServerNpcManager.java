package me.leeseol.core.servernpc;

import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ServerNpcManager implements Listener {
    private static final String RIGHT_CLICK_EVENT = "net.citizensnpcs.api.event.NPCRightClickEvent";
    private static final String LEFT_CLICK_EVENT = "net.citizensnpcs.api.event.NPCLeftClickEvent";
    private static final String SKIN_TRAIT = "net.citizensnpcs.trait.SkinTrait";
    private static final String EQUIPMENT_TRAIT = "net.citizensnpcs.api.trait.trait.Equipment";
    private static final String EQUIPMENT_SLOT = "net.citizensnpcs.api.trait.trait.Equipment$EquipmentSlot";
    private static final String ENTITY_POSE_TRAIT = "net.citizensnpcs.trait.EntityPoseTrait";
    private static final String ENTITY_POSE = "net.citizensnpcs.trait.EntityPoseTrait$EntityPose";

    private final LeeSeolCorePlugin plugin;
    private final Map<Integer, ServerNpcTarget> targetsByCitizensId = new HashMap<>();
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();
    private boolean citizensEventsRegistered;

    public ServerNpcManager(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        targetsByCitizensId.clear();
        HandlerList.unregisterAll(this);
        citizensEventsRegistered = false;

        if (!enabled()) {
            return;
        }
        loadTargets();
        if (targetsByCitizensId.isEmpty()) {
            return;
        }
        registerCitizensClickEvent(RIGHT_CLICK_EVENT);
        registerCitizensClickEvent(LEFT_CLICK_EVENT);
        if (citizensEventsRegistered) {
            plugin.getLogger().info("Server NPC movement enabled. npcs=" + targetsByCitizensId.size());
        }
    }

    public boolean handleCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!sender.hasPermission("leeseolcore.admin")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }
        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        String action = args[1].toLowerCase();
        return switch (action) {
            case "create" -> create(sender, args);
            case "bind" -> bind(sender, args);
            case "remove", "delete" -> remove(sender, args);
            case "list" -> list(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean create(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in game.");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(color("&e/leeseolcore servernpc create <id> <targetServer> [displayName]"));
            return true;
        }
        if (!isCitizensAvailable()) {
            sender.sendMessage(configMessage("server-npcs.messages.citizens-missing", "&cCitizens 플러그인이 로비에 설치되어 있어야 합니다."));
            return true;
        }

        String id = args[2];
        String targetServer = args[3];
        String displayName = args.length >= 5 ? join(args, 4) : plugin.getConfig().getString("server-npcs.default-name", "&a서바이벌 입장");
        Integer citizensId = createCitizensNpc(player.getLocation(), color(displayName));
        if (citizensId == null) {
            sender.sendMessage(color("&cCitizens NPC 생성에 실패했습니다. 서버 로그를 확인하세요."));
            return true;
        }

        saveTarget(id, citizensId, targetServer);
        sender.sendMessage(color("&a서버 이동 NPC를 생성했습니다: &f" + id + " &7(Citizens #" + citizensId + " -> " + targetServer + ")"));
        sender.sendMessage(color("&7기본 외형: 스킨 &f" + defaultSkin() + " &7| 손 아이템 &f" + defaultMainHand() + " &7| 자세 &f" + defaultPose()));
        return true;
    }

    private boolean bind(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(color("&e/leeseolcore servernpc bind <id> <citizensNpcId> <targetServer>"));
            return true;
        }
        Integer citizensId = parseInt(args[3]);
        if (citizensId == null) {
            sender.sendMessage(color("&cCitizens NPC ID는 숫자여야 합니다."));
            return true;
        }
        saveTarget(args[2], citizensId, args[4]);
        sender.sendMessage(color("&a서버 이동 NPC를 연결했습니다: &f" + args[2] + " &7(Citizens #" + citizensId + " -> " + args[4] + ")"));
        sender.sendMessage(color("&7기본 외형을 적용했습니다: 스킨 &f" + defaultSkin() + " &7| 손 아이템 &f" + defaultMainHand() + " &7| 자세 &f" + defaultPose()));
        return true;
    }

    private boolean remove(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&e/leeseolcore servernpc remove <id>"));
            return true;
        }
        String path = "server-npcs.npcs." + args[2];
        if (!plugin.getConfig().isConfigurationSection(path)) {
            sender.sendMessage(color("&c등록된 서버 이동 NPC가 없습니다: &f" + args[2]));
            return true;
        }
        int citizensId = plugin.getConfig().getInt(path + ".citizens-id", -1);
        plugin.getConfig().set(path, null);
        plugin.saveConfig();
        plugin.reloadCoreConfig();
        boolean destroyed = destroyCitizensNpc(citizensId);
        if (destroyed) {
            sender.sendMessage(color("&a서버 이동 NPC와 Citizens NPC를 제거했습니다: &f" + args[2] + " &7(#" + citizensId + ")"));
        } else {
            sender.sendMessage(color("&a서버 이동 NPC 연결을 제거했습니다: &f" + args[2]));
            sender.sendMessage(color("&7Citizens NPC가 이미 없거나 Citizens가 비활성 상태입니다. 남아 있으면 &f/npc select " + citizensId + " &7후 &f/npc remove&7로 지울 수 있습니다."));
        }
        return true;
    }

    private boolean list(org.bukkit.command.CommandSender sender) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("server-npcs.npcs");
        if (root == null || root.getKeys(false).isEmpty()) {
            sender.sendMessage(color("&7등록된 서버 이동 NPC가 없습니다."));
            return true;
        }
        sender.sendMessage(color("&a서버 이동 NPC:"));
        for (String id : root.getKeys(false)) {
            String base = "server-npcs.npcs." + id;
            sender.sendMessage(color("&7- &f" + id
                    + " &8(Citizens #" + plugin.getConfig().getInt(base + ".citizens-id")
                    + " -> " + plugin.getConfig().getString(base + ".target-server", "?") + ")"));
        }
        return true;
    }

    private void saveTarget(String id, int citizensId, String targetServer) {
        String path = "server-npcs.npcs." + id;
        plugin.getConfig().set("server-npcs.enabled", true);
        plugin.getConfig().set(path + ".citizens-id", citizensId);
        plugin.getConfig().set(path + ".target-server", targetServer);
        if (!plugin.getConfig().contains(path + ".skin")) {
            plugin.getConfig().set(path + ".skin", defaultSkin());
        }
        if (!plugin.getConfig().contains(path + ".main-hand")) {
            plugin.getConfig().set(path + ".main-hand", defaultMainHand());
        }
        if (!plugin.getConfig().contains(path + ".pose")) {
            plugin.getConfig().set(path + ".pose", defaultPose());
        }
        if (!plugin.getConfig().contains(path + ".message")) {
            plugin.getConfig().set(path + ".message", "&a서바이벌 서버로 이동합니다.");
        }
        if (!plugin.getConfig().contains(path + ".permission")) {
            plugin.getConfig().set(path + ".permission", "");
        }
        plugin.saveConfig();
        plugin.reloadCoreConfig();
    }

    private void loadTargets() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("server-npcs.npcs");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            int citizensId = section.getInt("citizens-id", -1);
            String targetServer = section.getString("target-server", plugin.getConfig().getString("server-npcs.default-target-server", "survival"));
            if (citizensId < 0 || targetServer == null || targetServer.isBlank()) {
                plugin.getLogger().warning("Skipping invalid server NPC: " + id);
                continue;
            }
            targetsByCitizensId.put(citizensId, new ServerNpcTarget(
                    id,
                    citizensId,
                    targetServer,
                    section.getString("permission", ""),
                    section.getString("message", plugin.getConfig().getString("server-npcs.messages.moving", "&a%server% 서버로 이동합니다."))
            ));
            applyCitizensVisuals(
                    citizensId,
                    section.getString("skin", defaultSkin()),
                    section.getString("main-hand", defaultMainHand()),
                    section.getString("pose", defaultPose())
            );
        }
    }

    private void registerCitizensClickEvent(String className) {
        if (!isCitizensAvailable()) {
            plugin.getLogger().warning("Citizens is not enabled. Server NPC movement is waiting for Citizens.");
            return;
        }
        try {
            Class<?> rawClass = Class.forName(className);
            if (!Event.class.isAssignableFrom(rawClass)) {
                return;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) rawClass;
            EventExecutor executor = (listener, event) -> handleCitizensClick(event);
            plugin.getServer().getPluginManager().registerEvent(eventClass, this, EventPriority.NORMAL, executor, plugin, true);
            citizensEventsRegistered = true;
        } catch (ClassNotFoundException exception) {
            plugin.getLogger().warning("Citizens event class not found: " + className);
        } catch (LinkageError error) {
            plugin.getLogger().warning("Citizens event class could not be loaded: " + error.getMessage());
        }
    }

    private void handleCitizensClick(Event event) {
        Object npc = invokeNoArgs(event, "getNPC");
        Object clicker = invokeNoArgs(event, "getClicker");
        if (!(clicker instanceof Player player) || npc == null) {
            return;
        }

        Integer citizensId = getNpcId(npc);
        if (citizensId == null) {
            return;
        }
        ServerNpcTarget target = targetsByCitizensId.get(citizensId);
        if (target == null) {
            return;
        }
        if (!target.permission().isBlank() && !player.hasPermission(target.permission())) {
            player.sendMessage(configMessage("server-npcs.messages.no-permission", "&c권한이 없습니다."));
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = plugin.getConfig().getLong("server-npcs.click-cooldown-millis", 1000L);
        long lastClick = clickCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastClick < cooldownMillis) {
            return;
        }
        clickCooldowns.put(player.getUniqueId(), now);

        player.sendMessage(color(target.message().replace("%server%", target.targetServer())));
        plugin.sendPlayerToServer(player, target.targetServer());
    }

    private Integer createCitizensNpc(Location location, String displayName) {
        try {
            Object registry = citizensRegistry();
            Method createNpc = registry.getClass().getMethod("createNPC", EntityType.class, String.class);
            Object npc = createNpc.invoke(registry, EntityType.PLAYER, displayName);
            Method spawn = npc.getClass().getMethod("spawn", Location.class);
            spawn.invoke(npc, location);
            return getNpcId(npc);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            plugin.getLogger().warning("Citizens NPC creation API failed: " + exception.getMessage());
            return null;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            plugin.getLogger().warning("Citizens NPC creation failed: " + cause.getMessage());
            return null;
        } catch (LinkageError error) {
            plugin.getLogger().warning("Citizens NPC creation API could not be loaded: " + error.getMessage());
            return null;
        }
    }

    private void applyCitizensVisuals(int citizensId, String skinName, String mainHandMaterial, String poseName) {
        if (!isCitizensAvailable()) {
            return;
        }
        Object npc = citizensNpc(citizensId);
        if (npc == null) {
            return;
        }
        applySkin(npc, skinName);
        applyMainHand(npc, mainHandMaterial);
        applyPose(npc, poseName);
    }

    private void applySkin(Object npc, String skinName) {
        String normalized = skinName == null ? "" : skinName.trim();
        if (normalized.isBlank()) {
            return;
        }
        try {
            Class<?> traitClass = Class.forName(SKIN_TRAIT);
            Object trait = getOrAddTrait(npc, traitClass);
            invokeIfPresent(trait, "setShouldUpdateSkins", new Class<?>[] { boolean.class }, true);
            Method setSkinName = traitClass.getMethod("setSkinName", String.class, boolean.class);
            setSkinName.invoke(trait, normalized, true);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            plugin.getLogger().warning("Could not apply Citizens skin '" + normalized + "': " + exception.getMessage());
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            plugin.getLogger().warning("Could not apply Citizens skin '" + normalized + "': " + cause.getMessage());
        } catch (LinkageError error) {
            plugin.getLogger().warning("Could not load Citizens skin API: " + error.getMessage());
        }
    }

    private void applyMainHand(Object npc, String materialName) {
        String normalized = materialName == null ? "" : materialName.trim();
        if (normalized.isBlank() || normalized.equalsIgnoreCase("NONE")) {
            return;
        }
        Material material = Material.matchMaterial(normalized);
        if (material == null || material.isAir()) {
            plugin.getLogger().warning("Skipping invalid server NPC hand item: " + normalized);
            return;
        }
        try {
            Class<?> traitClass = Class.forName(EQUIPMENT_TRAIT);
            Class<?> slotClass = Class.forName(EQUIPMENT_SLOT);
            Object trait = getOrAddTrait(npc, traitClass);
            Object handSlot = enumValue(slotClass, "HAND");
            Method set = traitClass.getMethod("set", slotClass, ItemStack.class);
            set.invoke(trait, handSlot, new ItemStack(material));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            plugin.getLogger().warning("Could not apply Citizens hand item '" + normalized + "': " + exception.getMessage());
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            plugin.getLogger().warning("Could not apply Citizens hand item '" + normalized + "': " + cause.getMessage());
        } catch (LinkageError error) {
            plugin.getLogger().warning("Could not load Citizens equipment API: " + error.getMessage());
        }
    }

    private void applyPose(Object npc, String poseName) {
        String normalized = poseName == null ? "" : poseName.trim();
        if (normalized.isBlank() || normalized.equalsIgnoreCase("NONE")) {
            return;
        }
        try {
            Class<?> traitClass = Class.forName(ENTITY_POSE_TRAIT);
            Class<?> poseClass = Class.forName(ENTITY_POSE);
            Object trait = getOrAddTrait(npc, traitClass);
            Object pose = enumValue(poseClass, normalized);
            Method setPose = traitClass.getMethod("setPose", poseClass);
            setPose.invoke(trait, pose);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Skipping invalid server NPC pose: " + normalized);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            plugin.getLogger().warning("Could not apply Citizens pose '" + normalized + "': " + exception.getMessage());
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            plugin.getLogger().warning("Could not apply Citizens pose '" + normalized + "': " + cause.getMessage());
        } catch (LinkageError error) {
            plugin.getLogger().warning("Could not load Citizens pose API: " + error.getMessage());
        }
    }

    private Object getOrAddTrait(Object npc, Class<?> traitClass) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getOrAddTrait = npc.getClass().getMethod("getOrAddTrait", Class.class);
        return getOrAddTrait.invoke(npc, traitClass);
    }

    private Object enumValue(Class<?> enumClass, String value) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Object enumValue = Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), value.trim().toUpperCase(Locale.ROOT));
        return enumValue;
    }

    private boolean destroyCitizensNpc(int citizensId) {
        if (citizensId < 0 || !isCitizensAvailable()) {
            return false;
        }
        Object npc = citizensNpc(citizensId);
        if (npc == null) {
            return false;
        }
        try {
            npc.getClass().getMethod("destroy").invoke(npc);
            return true;
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            plugin.getLogger().warning("Could not destroy Citizens NPC #" + citizensId + ": " + exception.getMessage());
            return false;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            plugin.getLogger().warning("Could not destroy Citizens NPC #" + citizensId + ": " + cause.getMessage());
            return false;
        }
    }

    private Object citizensNpc(int citizensId) {
        try {
            Object registry = citizensRegistry();
            return registry.getClass().getMethod("getById", int.class).invoke(registry, citizensId);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            plugin.getLogger().warning("Citizens NPC lookup failed: " + exception.getMessage());
            return null;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            plugin.getLogger().warning("Citizens NPC lookup failed: " + cause.getMessage());
            return null;
        } catch (LinkageError error) {
            plugin.getLogger().warning("Citizens NPC lookup API could not be loaded: " + error.getMessage());
            return null;
        }
    }

    private Object citizensRegistry() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> apiClass = Class.forName("net.citizensnpcs.api.CitizensAPI");
        return apiClass.getMethod("getNPCRegistry").invoke(null);
    }

    private void invokeIfPresent(Object target, String methodName, Class<?>[] parameterTypes, Object... values) {
        try {
            target.getClass().getMethod(methodName, parameterTypes).invoke(target, values);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    private Object invokeNoArgs(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private Integer getNpcId(Object npc) {
        Object rawId = invokeNoArgs(npc, "getId");
        if (rawId instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("server-npcs.enabled", false);
    }

    private boolean isCitizensAvailable() {
        return plugin.getServer().getPluginManager().isPluginEnabled("Citizens");
    }

    private String defaultSkin() {
        return plugin.getConfig().getString("server-npcs.default-skin", "lee_seol");
    }

    private String defaultMainHand() {
        return plugin.getConfig().getString("server-npcs.default-main-hand", "IRON_SWORD");
    }

    private String defaultPose() {
        return plugin.getConfig().getString("server-npcs.default-pose", "STANDING");
    }

    private String configMessage(String path, String fallback) {
        return color(plugin.getConfig().getString(path, fallback));
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String join(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int index = startIndex; index < args.length; index++) {
            if (index > startIndex) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.toString();
    }

    private void sendHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(color("&e/leeseolcore servernpc create <id> <targetServer> [displayName]"));
        sender.sendMessage(color("&e/leeseolcore servernpc bind <id> <citizensNpcId> <targetServer>"));
        sender.sendMessage(color("&e/leeseolcore servernpc list"));
        sender.sendMessage(color("&e/leeseolcore servernpc remove <id>"));
        sender.sendMessage(color("&7remove/delete는 등록 연결과 Citizens NPC를 함께 제거합니다."));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    private record ServerNpcTarget(
            String id,
            int citizensId,
            String targetServer,
            String permission,
            String message
    ) {
    }
}
