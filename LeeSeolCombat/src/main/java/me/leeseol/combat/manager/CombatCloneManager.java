package me.leeseol.combat.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.leeseol.combat.LeeSeolCombatPlugin;
import me.leeseol.combat.model.CombatClone;
import me.leeseol.combat.util.ItemSnapshots;
import me.leeseol.combat.util.Text;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.trait.SkinTrait;
import net.citizensnpcs.trait.SleepTrait;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

public final class CombatCloneManager {
    private static final String HITBOX_TAG = "leeseolcombat_hitbox";
    private static final String HITBOX_OWNER_TAG_PREFIX = "leeseolcombat_owner_";

    private final LeeSeolCombatPlugin plugin;
    private final Map<Integer, CombatClone> clonesByNpcId = new HashMap<>();
    private final Map<UUID, CombatClone> clonesByOwner = new HashMap<>();
    private final Map<UUID, CombatClone> clonesByHitbox = new HashMap<>();
    private NPCRegistry registry;

    public CombatCloneManager(LeeSeolCombatPlugin plugin) {
        this.plugin = plugin;
    }

    public void initializeRegistry() {
        if (registry != null) {
            return;
        }
        registry = CitizensAPI.createInMemoryNPCRegistry("leeseolcombat");
    }

    public int cleanupStaleHitboxes() {
        int removed = 0;
        for (World world : plugin.getServer().getWorlds()) {
            removed += cleanupStaleHitboxes(world);
        }
        if (removed > 0) {
            plugin.getLogger().info("Removed stale combat hitboxes: " + removed);
        }
        return removed;
    }

    public int cleanupStaleHitboxes(Chunk chunk) {
        if (chunk == null) {
            return 0;
        }
        int removed = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Interaction interaction && isCombatHitbox(interaction)) {
                interaction.remove();
                removed++;
            }
        }
        return removed;
    }

    private int cleanupStaleHitboxes(World world) {
        int removed = 0;
        for (Interaction interaction : world.getEntitiesByClass(Interaction.class)) {
            if (isCombatHitbox(interaction)) {
                interaction.remove();
                removed++;
            }
        }
        return removed;
    }

    public void spawnLogoutClone(Player player) {
        spawnClone(player, "logout");
    }

    public void spawnSpectatorClone(Player player) {
        spawnClone(player, "spectator");
    }

    private void spawnClone(Player player, String reason) {
        if (!plugin.combatConfig().isCloneEnabled()
                || player == null
                || player.hasPermission("leeseolcombat.bypass")
                || !plugin.combatConfig().isCloneWorld(player)) {
            return;
        }
        initializeRegistry();
        removeClone(player.getUniqueId());

        Location location = player.getLocation().clone();
        if (plugin.combatConfig().hitboxGroundOnSpawn()) {
            location = groundedLocation(location);
        }
        location.setPitch(0.0F);
        applyConfiguredSleepDirection(location);
        BlockFace face = sleepFace(location);
        String rawName = plugin.combatConfig().cloneNameFormat().replace("%player%", player.getName());
        boolean playerCorpse = isPlayerCorpseMode();
        EntityType entityType = playerCorpse ? EntityType.PLAYER : EntityType.HUSK;
        NPC npc = registry.createNPC(entityType, Text.color(rawName));
        npc.setProtected(false);
        if (playerCorpse) {
            applySkin(npc, player);
        }
        npc.spawn(location);
        pinVisualDirection(npc, face);
        if (playerCorpse) {
            applySleepingPose(npc, location);
        }
        applyEquipment(npc, player.getInventory());
        List<Entity> hitboxes = playerCorpse ? spawnHitbox(location, face) : List.of();

        double maxHealth = 1.0D;
        double health = 1.0D;
        CombatClone clone = new CombatClone(
                player.getUniqueId(),
                player.getName(),
                npc,
                location,
                face,
                ItemSnapshots.snapshot(player),
                hitboxes,
                health,
                maxHealth
        );
        clonesByNpcId.put(npc.getId(), clone);
        clonesByOwner.put(player.getUniqueId(), clone);
        for (Entity hitbox : hitboxes) {
            clonesByHitbox.put(hitbox.getUniqueId(), clone);
        }
        updateName(clone);
        plugin.getLogger().info("Spawned combat clone for " + player.getName()
                + " reason=" + reason
                + " hitboxes=" + hitboxes.size()
                + " at " + locationSummary(location));
    }

    public boolean damageClone(NPC npc, Player attacker, double damage) {
        if (npc == null) {
            return false;
        }
        CombatClone clone = clonesByNpcId.get(npc.getId());
        if (clone == null) {
            return false;
        }
        if (attacker == null) {
            return true;
        }
        clone.damage(damage);
        if (clone.health() <= 0.0D) {
            killClone(clone, attacker);
        } else {
            updateName(clone);
        }
        return true;
    }

    public boolean damageHitbox(Entity entity, Player attacker, double damage) {
        if (entity == null) {
            return false;
        }
        CombatClone clone = clonesByHitbox.get(entity.getUniqueId());
        if (clone == null) {
            return false;
        }
        if (attacker == null) {
            return true;
        }
        clone.damage(damage);
        if (clone.health() <= 0.0D) {
            killClone(clone, attacker);
        } else {
            updateName(clone);
        }
        return true;
    }

    public void punishCombatLogout(Player player) {
        if (player == null) {
            return;
        }
        ItemSnapshots.drop(player.getLocation(), ItemSnapshots.snapshot(player));
        ItemSnapshots.clear(player);
        plugin.pendingDeathStore().mark(player.getUniqueId());
        plugin.getLogger().info("Applied combat logout death to " + player.getName());
    }

    public void removeClone(UUID ownerId) {
        CombatClone clone = clonesByOwner.remove(ownerId);
        if (clone == null) {
            return;
        }
        clonesByNpcId.remove(clone.npc().getId());
        removeHitbox(clone);
        if (clone.npc().isSpawned()) {
            clone.npc().despawn();
        }
        clone.npc().destroy();
    }

    public void removeAll() {
        for (CombatClone clone : clonesByOwner.values()) {
            if (clone.npc().isSpawned()) {
                clone.npc().despawn();
            }
            removeHitbox(clone);
            clone.npc().destroy();
        }
        clonesByNpcId.clear();
        clonesByOwner.clear();
        clonesByHitbox.clear();
    }

    public int activeCloneCount() {
        return clonesByOwner.size();
    }

    public int activeHitboxCount() {
        return clonesByHitbox.size();
    }

    public boolean registryInitialized() {
        return registry != null;
    }

    private void killClone(CombatClone clone, Player attacker) {
        clonesByNpcId.remove(clone.npc().getId());
        clonesByOwner.remove(clone.ownerId());
        removeHitbox(clone);
        if (plugin.combatConfig().dropInventoryOnCloneKill()) {
            ItemSnapshots.drop(clone.dropLocation(), clone.drops());
        }
        if (plugin.combatConfig().applyDeathOnNextJoin()) {
            Player owner = plugin.getServer().getPlayer(clone.ownerId());
            if (owner != null && owner.isOnline()) {
                applyForcedDeathState(owner);
            } else {
                plugin.pendingDeathStore().mark(clone.ownerId());
            }
        }
        if (clone.npc().isSpawned()) {
            clone.npc().despawn();
        }
        clone.npc().destroy();
        if (attacker != null && attacker.isOnline()) {
            plugin.message(attacker, "clone-killed", "%player%", clone.ownerName());
        }
        plugin.getLogger().info("Combat clone killed: " + clone.ownerName()
                + (attacker == null ? "" : " by " + attacker.getName()));
    }

    public void applyForcedDeathState(Player player) {
        if (player == null) {
            return;
        }
        ItemSnapshots.clear(player);
        teleportToSpawn(player);
        double health = Math.max(1.0D, maxHealth(player));
        player.setHealth(health);
        player.setFoodLevel(20);
        player.setSaturation(5.0F);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
    }

    private void teleportToSpawn(Player player) {
        if (!plugin.combatConfig().teleportToSpawnOnPendingDeath()) {
            return;
        }
        World spawnWorld = plugin.getServer().getWorld(plugin.combatConfig().pendingDeathSpawnWorld());
        if (spawnWorld == null) {
            spawnWorld = player.getWorld();
        }
        Location spawn = spawnWorld.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D);
        spawn.setYaw(player.getLocation().getYaw());
        spawn.setPitch(player.getLocation().getPitch());
        player.teleport(spawn);
    }

    private List<Entity> spawnHitbox(Location location, BlockFace face) {
        List<Entity> hitboxes = new ArrayList<>();
        if (!plugin.combatConfig().hitboxEnabled() || location.getWorld() == null) {
            return hitboxes;
        }
        int blocks = plugin.combatConfig().hitboxLengthBlocks();
        double width = plugin.combatConfig().hitboxWidth();
        for (int i = 0; i < blocks; i++) {
            Location hitboxLocation = hitboxLocation(location, face, i);
            try {
                hitboxes.add(location.getWorld().spawn(hitboxLocation, Interaction.class, interaction -> {
                    interaction.setInteractionWidth((float) width);
                    interaction.setInteractionHeight((float) plugin.combatConfig().hitboxHeight());
                    interaction.setResponsive(true);
                    interaction.setGravity(false);
                    interaction.setSilent(true);
                    interaction.setPersistent(false);
                    interaction.addScoreboardTag(HITBOX_TAG);
                    interaction.addScoreboardTag(HITBOX_OWNER_TAG_PREFIX + locationSummary(location).replace(' ', '_'));
                }));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                plugin.getLogger().warning("Failed to spawn combat interaction hitbox at "
                        + locationSummary(hitboxLocation) + ": " + exception.getMessage());
            }
        }
        return hitboxes;
    }

    private Location hitboxLocation(Location base, BlockFace face, int index) {
        double width = plugin.combatConfig().hitboxWidth();
        Vector side = corpseSideVector(face);
        double offset = index == 0 ? 0.0D : index * width;
        return base.clone().add(
                side.getX() * offset,
                plugin.combatConfig().hitboxYOffset(),
                side.getZ() * offset
        );
    }

    private Location groundedLocation(Location location) {
        if (location.getWorld() == null) {
            return location;
        }
        Location grounded = location.clone();
        int maxSearch = plugin.combatConfig().hitboxMaxGroundSearchBlocks();
        int minY = location.getWorld().getMinHeight();
        int startY = Math.min(location.getBlockY(), location.getWorld().getMaxHeight() - 1);
        for (int y = startY; y >= Math.max(minY, startY - maxSearch); y--) {
            Block block = location.getWorld().getBlockAt(location.getBlockX(), y, location.getBlockZ());
            if (!block.isPassable() && block.getBoundingBox().getVolume() > 0.0D) {
                grounded.setY(block.getBoundingBox().getMaxY());
                return grounded;
            }
        }
        return location;
    }

    private static void pinVisualDirection(NPC npc, BlockFace face) {
        if (!npc.isSpawned() || npc.getEntity() == null) {
            return;
        }
        Entity entity = npc.getEntity();
        entity.setRotation(faceYaw(face), 0.0F);
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.setCollidable(false);
        }
    }

    private boolean isPlayerCorpseMode() {
        String mode = plugin.combatConfig().cloneMode();
        return mode.equals("lying-corpse") || mode.equals("lying-player");
    }

    private static void applySkin(NPC npc, Player player) {
        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
        skinTrait.setSkinPersistent(player);
        skinTrait.setShouldUpdateSkins(false);
    }

    private static void applySleepingPose(NPC npc, Location location) {
        SleepTrait sleepTrait = npc.getOrAddTrait(SleepTrait.class);
        // SleepTrait는 생성 시 한 번만 적용한다. 반복 호출하면 일어났다 눕는 자세 흔들림이 생긴다.
        sleepTrait.setSleeping(location);
    }

    private void applyConfiguredSleepDirection(Location location) {
        BlockFace face = configuredFace();
        if (face != null) {
            location.setYaw(faceYaw(face));
        }
    }

    private BlockFace sleepFace(Location location) {
        BlockFace configured = configuredFace();
        return configured == null ? yawFace(location.getYaw()) : configured;
    }

    private BlockFace configuredFace() {
        return switch (plugin.combatConfig().hitboxDirection()) {
            case "NORTH" -> BlockFace.NORTH;
            case "SOUTH" -> BlockFace.SOUTH;
            case "EAST" -> BlockFace.EAST;
            case "WEST" -> BlockFace.WEST;
            default -> null;
        };
    }

    private static BlockFace yawFace(float yaw) {
        float normalized = (yaw % 360.0F + 360.0F) % 360.0F;
        if (normalized < 45.0F || normalized >= 315.0F) {
            return BlockFace.SOUTH;
        }
        if (normalized < 135.0F) {
            return BlockFace.WEST;
        }
        if (normalized < 225.0F) {
            return BlockFace.NORTH;
        }
        return BlockFace.EAST;
    }

    private static float faceYaw(BlockFace face) {
        return switch (face) {
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static Vector corpseSideVector(BlockFace face) {
        return switch (face) {
            // Z축 방향으로 누우면 시각상 오른쪽으로 몸이 치우치므로 오른쪽 판정을 추가한다.
            case NORTH -> new Vector(1.0D, 0.0D, 0.0D);
            case SOUTH -> new Vector(-1.0D, 0.0D, 0.0D);
            // X축 방향으로 누우면 시각상 왼쪽으로 몸이 치우치므로 왼쪽 판정을 추가한다.
            case EAST -> new Vector(0.0D, 0.0D, -1.0D);
            case WEST -> new Vector(0.0D, 0.0D, 1.0D);
            default -> new Vector(-1.0D, 0.0D, 0.0D);
        };
    }

    private void removeHitbox(CombatClone clone) {
        for (Entity hitbox : clone.hitboxes()) {
            clonesByHitbox.remove(hitbox.getUniqueId());
            if (!hitbox.isDead()) {
                hitbox.remove();
            }
        }
    }

    private void updateName(CombatClone clone) {
        if (!plugin.combatConfig().showHealthInName()) {
            return;
        }
        String health = String.format("%.1f", clone.health());
        String maxHealth = String.format("%.1f", clone.maxHealth());
        clone.npc().setName(Text.color("&c" + clone.ownerName() + " &7(" + health + "/" + maxHealth + ")"));
    }

    private static void applyEquipment(NPC npc, PlayerInventory inventory) {
        Equipment equipment = npc.getOrAddTrait(Equipment.class);
        equipment.set(Equipment.EquipmentSlot.HELMET, ItemSnapshots.copy(inventory.getHelmet()));
        equipment.set(Equipment.EquipmentSlot.CHESTPLATE, ItemSnapshots.copy(inventory.getChestplate()));
        equipment.set(Equipment.EquipmentSlot.LEGGINGS, ItemSnapshots.copy(inventory.getLeggings()));
        equipment.set(Equipment.EquipmentSlot.BOOTS, ItemSnapshots.copy(inventory.getBoots()));
        equipment.set(Equipment.EquipmentSlot.HAND, ItemSnapshots.copy(inventory.getItemInMainHand()));
        equipment.set(Equipment.EquipmentSlot.OFF_HAND, ItemSnapshots.copy(inventory.getItemInOffHand()));
    }

    private static double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return 20.0D;
        }
        return Math.max(1.0D, attribute.getValue());
    }

    private static String locationSummary(Location location) {
        return location.getWorld().getName() + " "
                + location.getBlockX() + " "
                + location.getBlockY() + " "
                + location.getBlockZ();
    }

    public Player playerDamager(Entity entity) {
        if (entity instanceof Player player && !player.hasMetadata("NPC")) {
            return player;
        }
        return null;
    }

    private static boolean isCombatHitbox(Interaction interaction) {
        if (interaction.getScoreboardTags().contains(HITBOX_TAG)) {
            return true;
        }
        for (String tag : interaction.getScoreboardTags()) {
            if (tag.startsWith(HITBOX_OWNER_TAG_PREFIX)) {
                return true;
            }
        }
        return false;
    }
}
