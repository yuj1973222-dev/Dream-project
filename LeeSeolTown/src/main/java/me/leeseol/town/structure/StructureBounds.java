package me.leeseol.town.structure;

import java.util.LinkedHashSet;
import java.util.Set;
import me.leeseol.town.model.ClaimKey;

public final class StructureBounds {
    private StructureBounds() {
    }

    public static int anchorBlockX(int chunkX) {
        return chunkX * 16 + 8;
    }

    public static int anchorBlockZ(int chunkZ) {
        return chunkZ * 16 + 8;
    }

    public static Set<ClaimKey> affectedClaims(StructureDefinition definition, ClaimKey placementClaim) {
        int minX = minBlockX(definition, placementClaim);
        int maxX = minX + definition.width() - 1;
        int minZ = minBlockZ(definition, placementClaim);
        int maxZ = minZ + definition.length() - 1;

        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxChunkZ = Math.floorDiv(maxZ, 16);

        Set<ClaimKey> claims = new LinkedHashSet<>();
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                claims.add(new ClaimKey(placementClaim.world(), x, z));
            }
        }
        return claims;
    }

    public static boolean fitsInsidePlacementChunk(StructureDefinition definition, ClaimKey placementClaim) {
        return affectedClaims(definition, placementClaim).equals(Set.of(placementClaim));
    }

    public static int minBlockX(StructureDefinition definition, ClaimKey placementClaim) {
        return anchorBlockX(placementClaim.x()) + definition.offsetX();
    }

    public static int minBlockZ(StructureDefinition definition, ClaimKey placementClaim) {
        return anchorBlockZ(placementClaim.z()) + definition.offsetZ();
    }
}
