package me.leeseol.town.structure;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import java.util.Objects;
import org.bukkit.World;
import org.bukkit.block.BlockState;

public final class StructureUndoEdit {
    private final World world;
    private final EditSession pasteSession;
    private final BlockState markerState;

    StructureUndoEdit(World world, EditSession pasteSession, BlockState markerState) {
        this.world = Objects.requireNonNull(world, "world");
        this.pasteSession = Objects.requireNonNull(pasteSession, "pasteSession");
        this.markerState = markerState;
    }

    public void undo() {
        com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(world);
        try (EditSession undoSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
            pasteSession.undo(undoSession);
        }
        if (markerState != null) {
            markerState.update(true, false);
        }
    }
}
