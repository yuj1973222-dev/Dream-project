package me.leeseol.hologram.service;

import me.leeseol.hologram.LeeSeolHologramPlugin;
import me.leeseol.hologram.model.Hologram;
import me.leeseol.hologram.storage.HologramStore;
import me.leeseol.hologram.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class HologramService {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final String TAG = "leeseol_hologram";
    private static final String ID_TAG_PREFIX = "leeseol_hologram_id:";

    private final LeeSeolHologramPlugin plugin;
    private final HologramStore store;
    private final Map<String, List<UUID>> spawned = new HashMap<>();

    public HologramService(LeeSeolHologramPlugin plugin, HologramStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void reload() {
        despawnAll();
        removeTaggedArmorStands();
        store.load();
        spawnAll();
        plugin.getLogger().info("LeeSeolHologram loaded holograms=" + store.holograms().size());
    }

    public void shutdown() {
        despawnAll();
        store.save();
    }

    public boolean create(Player player, String id, String text) {
        if (!validId(id)) {
            player.sendMessage(plugin.msg("invalid-id"));
            return true;
        }
        if (store.hologram(id) != null) {
            player.sendMessage(plugin.msg("exists"));
            return true;
        }

        Location location = player.getLocation().add(0.0D, plugin.createYOffset(), 0.0D);
        String line = text == null || text.isBlank() ? "&#BEEBFF새 홀로그램" : text;
        Hologram hologram = new Hologram(HologramStore.normalize(id), location, plugin.defaultLineSpacing(), List.of(line));
        store.put(hologram);
        store.save();
        spawn(hologram);
        player.sendMessage(plugin.msg("created").replace("%id%", hologram.id()));
        return true;
    }

    public boolean delete(Player player, String id) {
        Hologram removed = store.remove(id);
        if (removed == null) {
            player.sendMessage(plugin.msg("not-found").replace("%id%", id));
            return true;
        }
        despawn(removed.id());
        store.save();
        player.sendMessage(plugin.msg("deleted").replace("%id%", removed.id()));
        return true;
    }

    public boolean moveHere(Player player, String id) {
        Hologram hologram = require(player, id);
        if (hologram == null) {
            return true;
        }
        hologram.setLocation(player.getLocation().add(0.0D, plugin.createYOffset(), 0.0D));
        store.save();
        respawn(hologram);
        player.sendMessage(plugin.msg("moved").replace("%id%", hologram.id()));
        return true;
    }

    public boolean addLine(Player player, String id, String text) {
        Hologram hologram = require(player, id);
        if (hologram == null) {
            return true;
        }
        hologram.lines().add(text);
        store.save();
        respawn(hologram);
        player.sendMessage(plugin.msg("line-added").replace("%id%", hologram.id()));
        return true;
    }

    public boolean setLine(Player player, String id, int index, String text) {
        Hologram hologram = require(player, id);
        if (hologram == null || !validLine(player, hologram, index)) {
            return true;
        }
        hologram.lines().set(index - 1, text);
        store.save();
        respawn(hologram);
        player.sendMessage(plugin.msg("line-set")
                .replace("%id%", hologram.id())
                .replace("%line%", String.valueOf(index)));
        return true;
    }

    public boolean insertLine(Player player, String id, int index, String text) {
        Hologram hologram = require(player, id);
        if (hologram == null) {
            return true;
        }
        if (index < 1 || index > hologram.lines().size() + 1) {
            player.sendMessage(plugin.msg("invalid-line"));
            return true;
        }
        hologram.lines().add(index - 1, text);
        store.save();
        respawn(hologram);
        player.sendMessage(plugin.msg("line-inserted")
                .replace("%id%", hologram.id())
                .replace("%line%", String.valueOf(index)));
        return true;
    }

    public boolean removeLine(Player player, String id, int index) {
        Hologram hologram = require(player, id);
        if (hologram == null || !validLine(player, hologram, index)) {
            return true;
        }
        hologram.lines().remove(index - 1);
        if (hologram.lines().isEmpty()) {
            hologram.lines().add("");
        }
        store.save();
        respawn(hologram);
        player.sendMessage(plugin.msg("line-removed")
                .replace("%id%", hologram.id())
                .replace("%line%", String.valueOf(index)));
        return true;
    }

    public boolean setSpacing(Player player, String id, double spacing) {
        Hologram hologram = require(player, id);
        if (hologram == null) {
            return true;
        }
        if (spacing < 0.1D || spacing > 2.0D) {
            player.sendMessage(plugin.msg("invalid-number"));
            return true;
        }
        hologram.setLineSpacing(spacing);
        store.save();
        respawn(hologram);
        player.sendMessage(plugin.msg("spacing-set")
                .replace("%id%", hologram.id())
                .replace("%spacing%", String.format("%.2f", spacing)));
        return true;
    }

    public Hologram hologram(String id) {
        return store.hologram(id);
    }

    public Collection<Hologram> holograms() {
        return store.holograms();
    }

    public boolean validId(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }

    public void spawnAll() {
        for (Hologram hologram : store.holograms()) {
            spawn(hologram);
        }
    }

    public void respawn(Hologram hologram) {
        despawn(hologram.id());
        spawn(hologram);
    }

    private Hologram require(Player player, String id) {
        Hologram hologram = store.hologram(id);
        if (hologram == null) {
            player.sendMessage(plugin.msg("not-found").replace("%id%", id));
        }
        return hologram;
    }

    private boolean validLine(Player player, Hologram hologram, int index) {
        if (index < 1 || index > hologram.lines().size()) {
            player.sendMessage(plugin.msg("invalid-line"));
            return false;
        }
        return true;
    }

    private void spawn(Hologram hologram) {
        World world = Bukkit.getWorld(hologram.worldName());
        if (world == null) {
            plugin.getLogger().warning("Skipping hologram in unloaded world: " + hologram.id() + " world=" + hologram.worldName());
            return;
        }

        List<UUID> entityIds = new ArrayList<>();
        for (int index = 0; index < hologram.lines().size(); index++) {
            String line = hologram.lines().get(index);
            Location location = hologram.location(world).add(0.0D, -index * hologram.lineSpacing(), 0.0D);
            ArmorStand stand = world.spawn(location, ArmorStand.class, entity -> configure(entity, hologram, line));
            entityIds.add(stand.getUniqueId());
        }
        spawned.put(hologram.id(), entityIds);
    }

    private void configure(ArmorStand stand, Hologram hologram, String line) {
        stand.addScoreboardTag(TAG);
        stand.addScoreboardTag(ID_TAG_PREFIX + hologram.id());
        stand.customName(Text.component(line));
        stand.setCustomNameVisible(true);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setSmall(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setPersistent(false);
    }

    private void despawn(String id) {
        List<UUID> entityIds = spawned.remove(id);
        if (entityIds == null) {
            return;
        }
        for (UUID uuid : entityIds) {
            for (World world : Bukkit.getWorlds()) {
                ArmorStand stand = world.getEntitiesByClass(ArmorStand.class).stream()
                        .filter(entity -> entity.getUniqueId().equals(uuid))
                        .findFirst()
                        .orElse(null);
                if (stand != null) {
                    stand.remove();
                    break;
                }
            }
        }
    }

    private void despawnAll() {
        for (String id : new ArrayList<>(spawned.keySet())) {
            despawn(id);
        }
    }

    private void removeTaggedArmorStands() {
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (stand.getScoreboardTags().contains(TAG)) {
                    stand.remove();
                }
            }
        }
    }
}
