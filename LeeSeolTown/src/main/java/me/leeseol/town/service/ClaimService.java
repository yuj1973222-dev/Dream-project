package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.NeutralZone;
import me.leeseol.town.model.Town;
import me.leeseol.town.model.War;
import me.leeseol.town.model.WarStatus;
import me.leeseol.town.storage.TownStore;
import org.bukkit.entity.Player;

public final class ClaimService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;
    private final NationService nations;
    private final TownDisplayService display;

    public ClaimService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query,
                        NationService nations, TownDisplayService display) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
        this.nations = nations;
        this.display = display;
    }

    public boolean claimChunk(Player player) {
        if (!player.hasPermission("leeseoltown.claim")) {
            player.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        store.load();

        Town town = query.playerTown(player);
        if (town == null) {
            player.sendMessage(plugin.msg("not-in-town"));
            return true;
        }
        Nation nation = town.nationId() == null ? null : store.nation(town.nationId());
        if (nation == null) {
            player.sendMessage(plugin.msg("claim-nation-required"));
            return true;
        }
        plugin.townService().processExpiredWarState();
        if (!nations.ensureNationActive(player, nation)) {
            return true;
        }
        if (!nations.canManageNation(player, town, nation)) {
            player.sendMessage(plugin.msg("not-nation-leader"));
            return true;
        }
        if (nation.beaconClaim() == null) {
            player.sendMessage(plugin.msg("claim-beacon-required"));
            return true;
        }

        ClaimKey claim = ClaimKey.from(player.getLocation());
        NeutralZone neutralZone = plugin.neutralZones().claimBlockedBy(claim);
        if (neutralZone != null && !player.hasPermission("leeseoltown.neutral.bypass")) {
            player.sendMessage(plugin.msg("neutral-zone-claim-blocked")
                    .replace("%zone%", neutralZone.id())
                    .replace("%buffer%", String.valueOf(neutralZone.claimBufferChunks())));
            return true;
        }
        Town owner = query.claimTown(claim);
        if (owner != null) {
            player.sendMessage(plugin.msg("claim-owned"));
            return true;
        }
        if (!query.isAdjacentToNationClaim(nation, claim)) {
            player.sendMessage(plugin.msg("claim-adjacent-required"));
            return true;
        }

        int nextClaimCount = query.nationClaimCount(nation) + 1;
        double cost = plugin.chunkClaimCost(claim, nation, nextClaimCount);
        if (!player.hasPermission("leeseoltown.admin") && plugin.economyEnabled() && cost > 0.0D) {
            if (!plugin.economy().available()) {
                player.sendMessage(plugin.msg("economy-missing"));
                return true;
            }
            if (!plugin.economy().has(player, cost)) {
                player.sendMessage(plugin.msg("not-enough-money").replace("%cost%", plugin.economy().format(cost)));
                return true;
            }
            if (!plugin.economy().withdraw(player, cost)) {
                player.sendMessage(plugin.msg("not-enough-money").replace("%cost%", plugin.economy().format(cost)));
                return true;
            }
        }

        town.claims().add(claim);
        store.rebuildIndexes();
        store.save();
        display.refreshNationClaimMarkers();
        player.sendMessage(plugin.msg("claim-bought")
                .replace("%cost%", player.hasPermission("leeseoltown.admin") ? "愿由ъ옄 ?고쉶" : plugin.formatMoney(cost))
                .replace("%upkeep%", plugin.formatMoney(plugin.dailyChunkUpkeep(claim, nextClaimCount)))
                .replace("%zone%", plugin.claimZoneName(claim)));
        return true;
    }

    public boolean sendClaimPrice(Player player) {
        store.load();
        Town town = query.playerTown(player);
        if (town == null) {
            player.sendMessage(plugin.msg("not-in-town"));
            return true;
        }
        Nation nation = town.nationId() == null ? null : store.nation(town.nationId());
        if (nation == null) {
            player.sendMessage(plugin.msg("claim-nation-required"));
            return true;
        }
        ClaimKey claim = ClaimKey.from(player.getLocation());
        NeutralZone neutralZone = plugin.neutralZones().claimBlockedBy(claim);
        if (neutralZone != null && !player.hasPermission("leeseoltown.neutral.bypass")) {
            player.sendMessage(plugin.msg("neutral-zone-claim-blocked")
                    .replace("%zone%", neutralZone.id())
                    .replace("%buffer%", String.valueOf(neutralZone.claimBufferChunks())));
            return true;
        }
        int nextClaimCount = query.nationClaimCount(nation) + 1;
        player.sendMessage(plugin.msg("claim-price")
                .replace("%chunk%", claim.display())
                .replace("%zone%", plugin.claimZoneName(claim))
                .replace("%cost%", plugin.formatMoney(plugin.chunkClaimCost(claim, nation, nextClaimCount)))
                .replace("%upkeep%", plugin.formatMoney(plugin.dailyChunkUpkeep(claim, nextClaimCount))));
        return true;
    }

    public boolean unclaimChunk(Player player) {
        store.load();
        Town town = query.playerTown(player);
        if (town == null) {
            player.sendMessage(plugin.msg("not-in-town"));
            return true;
        }

        ClaimKey claim = ClaimKey.from(player.getLocation());
        Town owner = query.claimTown(claim);
        if (owner == null) {
            player.sendMessage(plugin.msg("claim-not-owned"));
            return true;
        }
        Nation nation = owner.nationId() == null ? null : store.nation(owner.nationId());
        if (nation != null) {
            plugin.townService().processExpiredWarState();
            if (!nations.ensureNationActive(player, nation)) {
                return true;
            }
            if (!nations.canManageNation(player, town, nation)) {
                player.sendMessage(plugin.msg("not-nation-leader"));
                return true;
            }
            if (claim.equals(nation.beaconClaim())) {
                player.sendMessage(plugin.msg("claim-beacon-cannot-unclaim"));
                return true;
            }
        } else {
            if (!owner.id().equals(town.id())) {
                player.sendMessage(plugin.msg("claim-not-owned"));
                return true;
            }
            if (!town.isLeader(player.getUniqueId()) && !player.hasPermission("leeseoltown.admin")) {
                player.sendMessage(plugin.msg("not-town-leader"));
                return true;
            }
        }

        owner.claims().remove(claim);
        store.rebuildIndexes();
        store.save();
        display.refreshNationClaimMarkers();
        player.sendMessage(plugin.msg("claim-removed"));
        return true;
    }

    public boolean canBuild(Player player, ClaimKey claim) {
        Town owner = query.claimTown(claim);
        if (owner == null || player.hasPermission("leeseoltown.admin")) {
            return true;
        }
        Nation nation = owner.nationId() == null ? null : store.nation(owner.nationId());
        if (nation == null) {
            return owner.isMember(player.getUniqueId());
        }
        plugin.townService().processExpiredWarState();
        if (nation.functionsSuspended()) {
            return true;
        }
        Nation playerNation = query.playerNation(player);
        if (isWarProtectedAgainst(nation, playerNation)) {
            return false;
        }
        if (!nation.buildProtectionEnabled()) {
            return true;
        }
        return query.isNationMember(player.getUniqueId(), nation);
    }

    public boolean shouldCancelNationBeaconPlace(Player player, ClaimKey claim) {
        Town town = query.playerTown(player);
        Nation nation = town == null || town.nationId() == null ? null : store.nation(town.nationId());
        if (nation == null || nation.beaconClaim() != null) {
            return false;
        }
        if (!nations.canManageNation(player, town, nation)) {
            player.sendMessage(plugin.msg("beacon-nation-leader-required"));
            return true;
        }
        NeutralZone neutralZone = plugin.neutralZones().claimBlockedBy(claim);
        if (neutralZone != null && !player.hasPermission("leeseoltown.neutral.bypass")) {
            player.sendMessage(plugin.msg("neutral-zone-claim-blocked")
                    .replace("%zone%", neutralZone.id())
                    .replace("%buffer%", String.valueOf(neutralZone.claimBufferChunks())));
            return true;
        }
        Town owner = query.claimTown(claim);
        if (owner != null) {
            Nation ownerNation = owner.nationId() == null ? null : store.nation(owner.nationId());
            if (ownerNation == null || !ownerNation.id().equals(nation.id())) {
                player.sendMessage(plugin.msg("claim-owned"));
                return true;
            }
        }
        nation.setBeaconClaim(claim);
        if (owner == null) {
            town.claims().add(claim);
            store.rebuildIndexes();
        }
        store.save();
        display.refreshNationClaimMarkers();
        player.sendMessage(plugin.msg("beacon-placed")
                .replace("%nation%", nation.name())
                .replace("%chunk%", claim.display()));
        return false;
    }

    public boolean canPlaceNationCoreStructure(Player player, ClaimKey claim) {
        Town town = query.playerTown(player);
        Nation nation = town == null || town.nationId() == null ? null : store.nation(town.nationId());
        if (nation == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return false;
        }
        if (nation.beaconClaim() != null) {
            player.sendMessage(plugin.msg("structure-nation-core-exists")
                    .replace("%chunk%", nation.beaconClaim().display()));
            return false;
        }
        if (!nations.canManageNation(player, town, nation)) {
            player.sendMessage(plugin.msg("beacon-nation-leader-required"));
            return false;
        }
        NeutralZone neutralZone = plugin.neutralZones().claimBlockedBy(claim);
        if (neutralZone != null && !player.hasPermission("leeseoltown.neutral.bypass")) {
            player.sendMessage(plugin.msg("neutral-zone-claim-blocked")
                    .replace("%zone%", neutralZone.id())
                    .replace("%buffer%", String.valueOf(neutralZone.claimBufferChunks())));
            return false;
        }
        Town owner = query.claimTown(claim);
        if (owner == null) {
            return true;
        }
        Nation ownerNation = owner.nationId() == null ? null : store.nation(owner.nationId());
        if (ownerNation != null && ownerNation.id().equals(nation.id())) {
            return true;
        }
        player.sendMessage(plugin.msg("claim-owned"));
        return false;
    }

    public boolean registerNationCoreStructure(Player player, ClaimKey claim) {
        Town town = query.playerTown(player);
        Nation nation = town == null || town.nationId() == null ? null : store.nation(town.nationId());
        if (town == null || nation == null || nation.beaconClaim() != null) {
            return false;
        }
        Town owner = query.claimTown(claim);
        nation.setBeaconClaim(claim);
        if (owner == null) {
            town.claims().add(claim);
            store.rebuildIndexes();
        }
        store.save();
        display.refreshNationClaimMarkers();
        player.sendMessage(plugin.msg("beacon-placed")
                .replace("%nation%", nation.name())
                .replace("%chunk%", claim.display()));
        return true;
    }

    public void undoNationCoreStructure(String nationId, ClaimKey claim, boolean removeCreatedClaim) {
        Nation nation = store.nation(nationId);
        if (nation == null || claim == null) {
            return;
        }
        boolean changed = false;
        if (claim.equals(nation.beaconClaim())) {
            nation.setBeaconClaim(null);
            changed = true;
        }
        if (removeCreatedClaim) {
            Town owner = query.claimTown(claim);
            if (owner != null && nationId.equals(owner.nationId()) && owner.claims().remove(claim)) {
                store.rebuildIndexes();
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        store.save();
        display.refreshNationClaimMarkers();
    }

    public boolean shouldApplyBeaconFatigue(Player player, ClaimKey claim) {
        Nation nation = query.nationForBeaconClaim(claim);
        return nation != null
                && !nation.functionsSuspended()
                && !query.isNationMember(player.getUniqueId(), nation)
                && !player.hasPermission("leeseoltown.admin");
    }

    public boolean shouldBlockWarEntry(Player player, ClaimKey claim) {
        if (player.hasPermission("leeseoltown.admin")) {
            return false;
        }
        plugin.townService().processExpiredWarState();
        Nation ownerNation = query.nationForClaim(claim);
        if (ownerNation == null || ownerNation.functionsSuspended()) {
            return false;
        }
        Nation playerNation = query.playerNation(player);
        return isWarProtectedAgainst(ownerNation, playerNation);
    }

    private boolean isWarProtectedAgainst(Nation protectedNation, Nation enemyNation) {
        if (protectedNation == null || enemyNation == null || protectedNation.id().equals(enemyNation.id())) {
            return false;
        }
        long now = System.currentTimeMillis();
        for (War war : store.wars()) {
            if (war.status() == WarStatus.ACTIVE
                    && war.defenderProtectionActive()
                    && war.protectionUntil() > now
                    && war.defenderNationId().equals(protectedNation.id())
                    && war.attackerNationId().equals(enemyNation.id())) {
                return true;
            }
        }
        return false;
    }
}
