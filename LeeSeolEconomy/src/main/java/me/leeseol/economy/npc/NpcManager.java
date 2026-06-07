package me.leeseol.economy.npc;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import me.leeseol.economy.shop.ShopManager;
import me.leeseol.economy.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NpcManager implements Listener {
    private final LeeSeolEconomyPlugin plugin;
    private final ShopManager shopManager;
    private final NamespacedKey npcIdKey;
    private final NamespacedKey shopIdKey;
    private final Map<String, UUID> spawned = new HashMap<>();

    public NpcManager(LeeSeolEconomyPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.npcIdKey = new NamespacedKey(plugin, "shop_npc_id");
        this.shopIdKey = new NamespacedKey(plugin, "shop_id");
    }

    public void reload() {
        removeSpawnedNpcs();
        if (!plugin.featureEnabled("npcs")) {
            return;
        }

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("npcs");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }
            spawn(id, section);
        }
    }

    public int configuredNpcCount() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("npcs");
        return root == null ? 0 : root.getKeys(false).size();
    }

    public void removeSpawnedNpcs() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isShopNpc(entity)) {
                    entity.remove();
                }
            }
        }
        spawned.clear();
    }

    public boolean create(Player player, String id, String shopId, String displayName, String skinName) {
        if (!shopManager.hasShop(shopId)) {
            return false;
        }
        Location location = player.getLocation();
        String path = "npcs." + id;
        plugin.getConfig().set(path + ".enabled", true);
        plugin.getConfig().set(path + ".shop", shopId);
        plugin.getConfig().set(path + ".world", location.getWorld().getName());
        plugin.getConfig().set(path + ".x", location.getX());
        plugin.getConfig().set(path + ".y", location.getY());
        plugin.getConfig().set(path + ".z", location.getZ());
        plugin.getConfig().set(path + ".yaw", location.getYaw());
        plugin.getConfig().set(path + ".pitch", location.getPitch());
        plugin.getConfig().set(path + ".type", "VILLAGER");
        plugin.getConfig().set(path + ".skin", normalizeSkin(skinName));
        plugin.getConfig().set(path + ".name", displayName == null || displayName.isBlank() ? "&e상점" : displayName);
        plugin.saveConfig();
        reload();
        return true;
    }

    public boolean setSkin(String id, String skinName) {
        String path = "npcs." + id;
        if (!plugin.getConfig().contains(path)) {
            return false;
        }
        plugin.getConfig().set(path + ".skin", normalizeSkin(skinName));
        plugin.saveConfig();
        reload();
        return true;
    }

    public boolean remove(String id) {
        if (!plugin.getConfig().contains("npcs." + id)) {
            return false;
        }
        plugin.getConfig().set("npcs." + id, null);
        plugin.saveConfig();
        reload();
        return true;
    }

    public Iterable<String> npcIds() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("npcs");
        return root == null ? java.util.List.of() : root.getKeys(false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        if (openShop(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onNpcPreciseInteract(PlayerInteractAtEntityEvent event) {
        if (openShop(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onNpcDamage(EntityDamageEvent event) {
        if (isShopNpc(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private boolean openShop(Player player, Entity entity) {
        if (!isShopNpc(entity)) {
            return false;
        }
        String shopId = entity.getPersistentDataContainer().get(shopIdKey, PersistentDataType.STRING);
        shopManager.open(player, shopId);
        return true;
    }

    private boolean isShopNpc(Entity entity) {
        return entity.getPersistentDataContainer().has(npcIdKey, PersistentDataType.STRING);
    }

    private void spawn(String id, ConfigurationSection section) {
        World world = plugin.getServer().getWorld(section.getString("world", "world"));
        if (world == null) {
            plugin.getLogger().warning("Skipping shop NPC " + id + ": world is not loaded.");
            return;
        }
        String shopId = section.getString("shop", plugin.getConfig().getString("shops.default-shop", "general"));
        if (!shopManager.hasShop(shopId)) {
            plugin.getLogger().warning("Skipping shop NPC " + id + ": shop not found: " + shopId);
            return;
        }

        Location location = new Location(
            world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw"),
            (float) section.getDouble("pitch")
        );

        String skinName = section.getString("skin", "");
        if (skinName != null && !skinName.isBlank()) {
            spawnSkinHeadNpc(id, shopId, location, section.getString("name", "&e상점"), skinName);
            return;
        }

        EntityType type = parseEntityType(section.getString("type", "VILLAGER"));
        if (!type.isAlive()) {
            type = EntityType.VILLAGER;
        }
        Entity entity = world.spawnEntity(location, type);
        tagEntity(entity, id, shopId);
        entity.setCustomName(Text.color(section.getString("name", "&e상점")));
        entity.setCustomNameVisible(true);
        entity.setPersistent(false);
        entity.setGravity(false);

        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.setAI(false);
            livingEntity.setInvulnerable(true);
            livingEntity.setSilent(true);
            livingEntity.setCollidable(false);
            livingEntity.setRemoveWhenFarAway(false);
        }
        if (entity instanceof Villager villager) {
            villager.setAdult();
            villager.setProfession(Villager.Profession.LIBRARIAN);
        }
        spawned.put(id, entity.getUniqueId());
    }

    private void spawnSkinHeadNpc(String id, String shopId, Location location, String displayName, String skinName) {
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        tagEntity(armorStand, id, shopId);
        armorStand.setCustomName(Text.color(displayName));
        armorStand.setCustomNameVisible(true);
        armorStand.setPersistent(false);
        armorStand.setGravity(false);
        armorStand.setVisible(false);
        armorStand.setInvulnerable(true);
        armorStand.setSilent(true);
        armorStand.setCollidable(false);
        armorStand.setRemoveWhenFarAway(false);
        armorStand.setBasePlate(false);
        armorStand.setArms(false);
        armorStand.getEquipment().setHelmet(playerHead(skinName));
        spawned.put(id, armorStand.getUniqueId());
    }

    private void tagEntity(Entity entity, String id, String shopId) {
        entity.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, id);
        entity.getPersistentDataContainer().set(shopIdKey, PersistentDataType.STRING, shopId);
    }

    private ItemStack playerHead(String skinName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(skinName));
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    private String normalizeSkin(String skinName) {
        if (skinName == null || skinName.isBlank() || skinName.equalsIgnoreCase("none")) {
            return "";
        }
        return skinName;
    }

    private EntityType parseEntityType(String raw) {
        if (raw == null) {
            return EntityType.VILLAGER;
        }
        try {
            return EntityType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid NPC entity type: " + raw);
            return EntityType.VILLAGER;
        }
    }
}
