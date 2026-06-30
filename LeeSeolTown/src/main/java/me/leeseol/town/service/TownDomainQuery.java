package me.leeseol.town.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.Town;
import me.leeseol.town.model.War;
import me.leeseol.town.model.WarStatus;
import me.leeseol.town.storage.TownStore;
import org.bukkit.entity.Player;

public final class TownDomainQuery {
    private final TownStore store;

    public TownDomainQuery(TownStore store) {
        this.store = store;
    }

    public Town playerTown(Player player) {
        return player == null ? null : store.playerTown(player.getUniqueId());
    }

    public Town playerTown(UUID playerId) {
        return playerId == null ? null : store.playerTown(playerId);
    }

    public Nation playerNation(Player player) {
        Town town = playerTown(player);
        return town == null || town.nationId() == null ? null : store.nation(town.nationId());
    }

    public Town claimTown(ClaimKey claim) {
        return store.claimTown(claim);
    }

    public Nation nationForClaim(ClaimKey claim) {
        Town owner = store.claimTown(claim);
        return owner == null || owner.nationId() == null ? null : store.nation(owner.nationId());
    }

    public String nationIdForClaim(ClaimKey claim) {
        Nation nation = nationForClaim(claim);
        return nation == null ? null : nation.id();
    }

    public boolean nationHasOpenWar(Nation nation) {
        if (nation == null) {
            return false;
        }
        for (War war : store.wars()) {
            if ((war.status() == WarStatus.PENDING || war.status() == WarStatus.ACTIVE)
                    && war.involves(nation.id())) {
                return true;
            }
        }
        return false;
    }

    public Nation nationByName(String name) {
        String id = TownStore.idFromName(name);
        Nation nation = store.nation(id);
        if (nation != null) {
            return nation;
        }
        for (Nation candidate : store.nations()) {
            if (candidate.name().equalsIgnoreCase(name)) {
                return candidate;
            }
        }
        return null;
    }

    public War findWarBetween(String firstNationId, String secondNationId) {
        for (War war : store.wars()) {
            if (war.between(firstNationId, secondNationId)) {
                return war;
            }
        }
        return null;
    }

    public War findPendingWar(String attackerNationId, String defenderNationId) {
        for (War war : store.wars()) {
            if (war.status() == WarStatus.PENDING
                    && war.attackerNationId().equals(attackerNationId)
                    && war.defenderNationId().equals(defenderNationId)) {
                return war;
            }
        }
        return null;
    }

    public boolean isNationMember(UUID playerId, Nation nation) {
        if (playerId == null || nation == null) {
            return false;
        }
        for (String townId : nation.townIds()) {
            Town town = store.town(townId);
            if (town != null && town.members().contains(playerId)) {
                return true;
            }
        }
        return false;
    }

    public Nation nationForBeaconClaim(ClaimKey claim) {
        for (Nation nation : store.nations()) {
            if (claim.equals(nation.beaconClaim())) {
                return nation;
            }
        }
        return null;
    }

    public int nationMemberCount(Nation nation) {
        int count = 0;
        if (nation == null) {
            return count;
        }
        for (String townId : nation.townIds()) {
            Town town = store.town(townId);
            if (town != null) {
                count += town.members().size();
            }
        }
        return count;
    }

    public int nationClaimCount(Nation nation) {
        return nationClaims(nation).size();
    }

    public List<ClaimKey> nationClaims(Nation nation) {
        List<ClaimKey> claims = new ArrayList<>();
        if (nation == null) {
            return claims;
        }
        for (String townId : nation.townIds()) {
            Town town = store.town(townId);
            if (town != null) {
                claims.addAll(town.claims());
            }
        }
        return claims;
    }

    public boolean isAdjacentToNationClaim(Nation nation, ClaimKey claim) {
        return isNationClaim(nation, new ClaimKey(claim.world(), claim.x() + 1, claim.z()))
                || isNationClaim(nation, new ClaimKey(claim.world(), claim.x() - 1, claim.z()))
                || isNationClaim(nation, new ClaimKey(claim.world(), claim.x(), claim.z() + 1))
                || isNationClaim(nation, new ClaimKey(claim.world(), claim.x(), claim.z() - 1));
    }

    public boolean isNationClaim(Nation nation, ClaimKey claim) {
        Town owner = store.claimTown(claim);
        return owner != null && owner.nationId() != null && owner.nationId().equals(nation.id());
    }

    public Collection<Town> towns() {
        return store.towns();
    }

    public Collection<Nation> nations() {
        return store.nations();
    }
}
