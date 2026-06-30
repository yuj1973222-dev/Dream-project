package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ChatMode;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.Town;
import me.leeseol.town.model.WarMode;
import me.leeseol.town.storage.TownStore;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public final class TownService {
    private final TownStore store;
    private final TownDomainQuery domainQuery;
    private final TownConfirmationService confirmationService;
    private final TownDisplayService displayService;
    private final TownMembershipService membershipService;
    private final NationService nationService;
    private final ClaimService claimService;
    private final WarService warService;

    public TownService(LeeSeolTownPlugin plugin, TownStore store) {
        this.store = store;
        this.domainQuery = new TownDomainQuery(store);
        this.confirmationService = new TownConfirmationService(plugin);
        this.displayService = new TownDisplayService(plugin, store, domainQuery);
        this.membershipService = new TownMembershipService(plugin, store, domainQuery, confirmationService, displayService);
        this.nationService = new NationService(plugin, store, domainQuery, confirmationService, displayService);
        this.claimService = new ClaimService(plugin, store, domainQuery, nationService, displayService);
        this.warService = new WarService(plugin, store, domainQuery, nationService, displayService);
    }

    public void reload() {
        store.load();
        updateAllIdentities();
    }

    public boolean createTown(Player player, String name) {
        return membershipService.createTown(player, name);
    }

    public boolean invite(Player sender, String targetName) {
        return membershipService.invite(sender, targetName);
    }

    public boolean joinTown(Player player, String townName) {
        return membershipService.joinTown(player, townName);
    }

    public boolean acceptInvite(Player player, String townName) {
        return membershipService.acceptInvite(player, townName);
    }

    public boolean denyInvite(Player player, String townName) {
        return membershipService.denyInvite(player, townName);
    }

    public boolean leaveTown(Player player) {
        return membershipService.leaveTown(player);
    }

    public boolean disbandTown(Player player) {
        return membershipService.disbandTown(player);
    }

    public boolean transferLeader(Player player, String targetName) {
        return membershipService.transferLeader(player, targetName);
    }

    public boolean kickMember(Player player, String targetName) {
        return membershipService.kickMember(player, targetName);
    }

    public boolean claimChunk(Player player) {
        return claimService.claimChunk(player);
    }

    public boolean sendClaimPrice(Player player) {
        return claimService.sendClaimPrice(player);
    }

    public boolean unclaimChunk(Player player) {
        return claimService.unclaimChunk(player);
    }

    public boolean createNation(Player player, String name, String colorKey, List<String> extraPartyNames) {
        return nationService.createNation(player, name, colorKey, extraPartyNames);
    }

    public List<String> nationColorKeys() {
        return nationService.nationColorKeys();
    }

    public boolean disbandNation(Player player) {
        return nationService.disbandNation(player);
    }

    public boolean setNationPvp(Player player, boolean enabled) {
        return nationService.setNationPvp(player, enabled);
    }

    public boolean setNationBuildProtection(Player player, boolean enabled) {
        return nationService.setNationBuildProtection(player, enabled);
    }

    public boolean depositNationTreasury(Player player, double amount) {
        return nationService.depositNationTreasury(player, amount);
    }

    public boolean sendNationTreasury(Player player) {
        return nationService.sendNationTreasury(player);
    }

    public boolean sendNationUpkeep(Player player) {
        return nationService.sendNationUpkeep(player);
    }

    public boolean payNationUpkeep(Player player) {
        return nationService.payNationUpkeep(player);
    }

    public boolean declareWar(Player player, String targetNationName) {
        return warService.declareWar(player, targetNationName);
    }

    public boolean declareWar(Player player, String targetNationName, WarMode mode) {
        return warService.declareWar(player, targetNationName, mode);
    }

    public boolean acceptWar(Player player, String attackerNationName) {
        return warService.acceptWar(player, attackerNationName);
    }

    public boolean surrenderWar(Player player, String enemyNationName) {
        return warService.surrenderWar(player, enemyNationName);
    }

    public boolean releaseWarProtection(Player player, String enemyNationName) {
        return warService.releaseWarProtection(player, enemyNationName);
    }

    public boolean payWarDebt(Player player) {
        return warService.payWarDebt(player);
    }

    public boolean finishWar(Player player, String winnerName, String loserName) {
        return warService.finishWar(player, winnerName, loserName);
    }

    public void setChatMode(Player player, ChatMode mode) {
        displayService.setChatMode(player, mode);
    }

    public void sendTownChat(Player player, Component message) {
        displayService.sendTownChat(player, message);
    }

    public void sendNationChat(Player player, Component message) {
        displayService.sendNationChat(player, message);
    }

    public void broadcastGlobalChat(Player player, Component message) {
        displayService.broadcastGlobalChat(player, message);
    }

    public Component chatLine(String formatPath, Player player, Component message) {
        return displayService.chatLine(formatPath, player, message);
    }

    public String affiliationPrefix(Player player) {
        return displayService.affiliationPrefix(player);
    }

    public String rankPrefix(Player player) {
        return displayService.rankPrefix(player);
    }


    public void updateIdentity(Player player) {
        displayService.updateIdentity(player);
    }

    public void updateAllIdentities() {
        displayService.updateAllIdentities();
    }

    public boolean canBuild(Player player, ClaimKey claim) {
        return claimService.canBuild(player, claim);
    }

    public boolean shouldCancelNationBeaconPlace(Player player, ClaimKey claim) {
        return claimService.shouldCancelNationBeaconPlace(player, claim);
    }

    public boolean canPlaceNationCoreStructure(Player player, ClaimKey claim) {
        return claimService.canPlaceNationCoreStructure(player, claim);
    }

    public boolean registerNationCoreStructure(Player player, ClaimKey claim) {
        return claimService.registerNationCoreStructure(player, claim);
    }

    public void undoNationCoreStructure(String nationId, ClaimKey claim, boolean removeCreatedClaim) {
        claimService.undoNationCoreStructure(nationId, claim, removeCreatedClaim);
    }

    public boolean shouldApplyBeaconFatigue(Player player, ClaimKey claim) {
        return claimService.shouldApplyBeaconFatigue(player, claim);
    }

    public boolean shouldBlockWarEntry(Player player, ClaimKey claim) {
        return claimService.shouldBlockWarEntry(player, claim);
    }

    public boolean canPvp(Player attacker, Player victim) {
        return warService.canPvp(attacker, victim);
    }

    public Nation nationForClaim(ClaimKey claim) {
        return domainQuery.nationForClaim(claim);
    }

    public String nationIdForClaim(ClaimKey claim) {
        return domainQuery.nationIdForClaim(claim);
    }

    public boolean nationHasOpenWar(Nation nation) {
        return domainQuery.nationHasOpenWar(nation);
    }

    public Town claimTown(ClaimKey claim) {
        return domainQuery.claimTown(claim);
    }

    public ChatMode chatMode(Player player) {
        return store.chatMode(player.getUniqueId());
    }

    public Town playerTown(Player player) {
        return domainQuery.playerTown(player);
    }

    public Nation playerNation(Player player) {
        return domainQuery.playerNation(player);
    }

    public Collection<Town> towns() {
        return domainQuery.towns();
    }

    public Collection<Nation> nations() {
        return domainQuery.nations();
    }

    public void sendSelfInfo(Player player) {
        displayService.sendSelfInfo(player);
    }

    public boolean canManageNation(Player player, Town town, Nation nation) {
        return nationService.canManageNation(player, town, nation);
    }


    public Nation requireNationLeader(Player player, Town town) {
        return nationService.requireNationLeader(player, town);
    }

    public boolean ensureNationActive(Player player, Nation nation) {
        return nationService.ensureNationActive(player, nation);
    }


    public void processExpiredWarState() {
        warService.processExpiredWarState();
    }


    public void collectDueUpkeep(boolean force) {
        nationService.collectDueUpkeep(force);
    }

    public long dailyNationUpkeep(Nation nation) {
        return nationService.dailyNationUpkeep(nation);
    }


    public String info(Town town) {
        return displayService.info(town);
    }
}
