package me.leeseol.town.structure;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import me.leeseol.town.LeeSeolTownPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public final class WorldEditStructurePaster {
    private final LeeSeolTownPlugin plugin;

    public WorldEditStructurePaster(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean schematicExists(StructureDefinition definition) {
        if (!definition.hasSchematic()) {
            return false;
        }
        return schematicFile(definition).isFile();
    }

    public boolean itemsAdderBlockAvailable(StructureDefinition definition) {
        if (definition.itemsAdderBlock().isBlank()) {
            return true;
        }
        try {
            return customBlock(definition) != null;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    public boolean isAreaClear(
            World world,
            StructureDefinition definition,
            int anchorX,
            int anchorY,
            int anchorZ,
            boolean force,
            Block ignored
    ) {
        if (force) {
            return true;
        }
        Set<Material> softBlocks = softBlocks();
        int minX = anchorX + definition.offsetX();
        int minY = anchorY + definition.offsetY();
        int minZ = anchorZ + definition.offsetZ();
        for (int x = minX; x < minX + definition.width(); x++) {
            for (int y = minY; y < minY + definition.height(); y++) {
                if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                    return false;
                }
                for (int z = minZ; z < minZ + definition.length(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (ignored != null && sameBlock(block, ignored)) {
                        continue;
                    }
                    if (!softBlocks.contains(block.getType())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void paste(World world, StructureDefinition definition, int anchorX, int anchorY, int anchorZ)
            throws IOException, WorldEditException {
        pasteWithUndo(world, definition, anchorX, anchorY, anchorZ);
    }

    public StructureUndoEdit pasteWithUndo(World world, StructureDefinition definition, int anchorX, int anchorY, int anchorZ)
            throws IOException, WorldEditException {
        if (!definition.hasSchematic()) {
            throw new IOException("No linked schematic configured for " + definition.id());
        }
        File file = schematicFile(definition);
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            throw new IOException("Unsupported schematic format: " + file.getName());
        }
        BlockState markerState = markerState(world, definition, anchorX, anchorY, anchorZ);
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(world);
            try (EditSession session = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(session)
                        .to(BlockVector3.at(anchorX + definition.offsetX(), anchorY + definition.offsetY(), anchorZ + definition.offsetZ()))
                        .ignoreAirBlocks(true)
                        .build();
                Operations.complete(operation);
                session.flushSession();
                return new StructureUndoEdit(world, session, markerState);
            }
        }
    }

    public boolean placeItemsAdderMarker(World world, StructureDefinition definition, int anchorX, int anchorY, int anchorZ) {
        if (definition.itemsAdderBlock().isBlank()) {
            return true;
        }
        try {
            Class<?> customBlockClass = Class.forName("dev.lone.itemsadder.api.CustomBlock");
            Object customBlock = customBlock(definition);
            if (customBlock == null) {
                return false;
            }
            Location location = world.getBlockAt(
                    anchorX + definition.markerOffsetX(),
                    anchorY + definition.markerOffsetY(),
                    anchorZ + definition.markerOffsetZ()
            ).getLocation();
            Object result = place(customBlockClass, customBlock, definition.itemsAdderBlock(), location);
            return !(result instanceof Boolean booleanResult) || booleanResult;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private Object customBlock(StructureDefinition definition) throws ReflectiveOperationException {
        Class<?> customBlockClass = Class.forName("dev.lone.itemsadder.api.CustomBlock");
        Method getInstance = customBlockClass.getMethod("getInstance", String.class);
        return getInstance.invoke(null, definition.itemsAdderBlock());
    }

    private Object place(Class<?> customBlockClass, Object customBlock, String namespacedId, Location location)
            throws ReflectiveOperationException {
        try {
            return customBlockClass.getMethod("place", Location.class).invoke(customBlock, location);
        } catch (NoSuchMethodException ignored) {
            return customBlockClass.getMethod("place", String.class, Location.class).invoke(null, namespacedId, location);
        }
    }

    private File schematicFile(StructureDefinition definition) {
        if (!definition.hasSchematic()) {
            throw new IllegalArgumentException("No linked schematic configured for " + definition.id());
        }
        File directory = new File(plugin.getDataFolder(), plugin.structureRegistry().schematicDirectory());
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }
        File file = new File(directory, definition.schematic());
        if (!file.isFile()) {
            copyBundledSchematicIfPresent(definition, file);
        }
        return file;
    }

    private BlockState markerState(World world, StructureDefinition definition, int anchorX, int anchorY, int anchorZ) {
        if (definition.itemsAdderBlock().isBlank()) {
            return null;
        }
        return world.getBlockAt(
                anchorX + definition.markerOffsetX(),
                anchorY + definition.markerOffsetY(),
                anchorZ + definition.markerOffsetZ()
        ).getState();
    }

    private void copyBundledSchematicIfPresent(StructureDefinition definition, File target) {
        if (!definition.hasSchematic()) {
            return;
        }
        String resourcePath = "structures/" + definition.schematic().replace('\\', '/');
        if (resourcePath.contains("..") || resourcePath.startsWith("/") || resourcePath.contains(":")) {
            return;
        }
        try (InputStream input = plugin.getResource(resourcePath)) {
            if (input == null) {
                return;
            }
            File parent = target.getParentFile();
            if (parent != null && !parent.isDirectory()) {
                parent.mkdirs();
            }
            Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            plugin.getLogger().warning("Bundled schematic copy failed: " + resourcePath);
        }
    }

    private Set<Material> softBlocks() {
        Set<Material> blocks = new HashSet<>();
        for (String raw : plugin.structureRegistry().softOverwriteBlocks()) {
            Material material = Material.matchMaterial(raw);
            if (material != null) {
                blocks.add(material);
            }
        }
        blocks.add(Material.AIR);
        blocks.add(Material.CAVE_AIR);
        blocks.add(Material.VOID_AIR);
        return blocks;
    }

    private boolean sameBlock(Block first, Block second) {
        return first.getWorld().equals(second.getWorld())
                && first.getX() == second.getX()
                && first.getY() == second.getY()
                && first.getZ() == second.getZ();
    }
}
