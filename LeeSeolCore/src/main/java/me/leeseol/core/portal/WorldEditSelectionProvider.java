package me.leeseol.core.portal;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.entity.Player;

public final class WorldEditSelectionProvider {
    private WorldEditSelectionProvider() {
    }

    public static PortalCuboidSelection getSelection(Player player) throws IncompleteRegionException {
        LocalSession session = WorldEdit.getInstance()
                .getSessionManager()
                .get(BukkitAdapter.adapt(player));
        Region region = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        return new PortalCuboidSelection(
                player.getWorld().getName(),
                min.x(),
                min.y(),
                min.z(),
                max.x(),
                max.y(),
                max.z()
        );
    }
}
