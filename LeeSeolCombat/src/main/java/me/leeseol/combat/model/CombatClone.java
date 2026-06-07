package me.leeseol.combat.model;

import java.util.List;
import java.util.UUID;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public final class CombatClone {
    private final UUID ownerId;
    private final String ownerName;
    private final NPC npc;
    private final Location dropLocation;
    private final BlockFace face;
    private final List<ItemStack> drops;
    private final List<Entity> hitboxes;
    private final double maxHealth;
    private double health;

    public CombatClone(UUID ownerId, String ownerName, NPC npc, Location dropLocation, BlockFace face,
                       List<ItemStack> drops, List<Entity> hitboxes, double health, double maxHealth) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.npc = npc;
        this.dropLocation = dropLocation;
        this.face = face;
        this.drops = drops;
        this.hitboxes = hitboxes;
        this.health = health;
        this.maxHealth = maxHealth;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String ownerName() {
        return ownerName;
    }

    public NPC npc() {
        return npc;
    }

    public Location dropLocation() {
        return dropLocation;
    }

    public BlockFace face() {
        return face;
    }

    public List<ItemStack> drops() {
        return drops;
    }

    public List<Entity> hitboxes() {
        return hitboxes;
    }

    public double health() {
        return health;
    }

    public double maxHealth() {
        return maxHealth;
    }

    public void damage(double amount) {
        health = Math.max(0.0D, health - Math.max(0.0D, amount));
    }
}
