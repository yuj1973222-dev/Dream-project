package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ChatMode;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.NationColor;
import me.leeseol.town.model.NationColorPalette;
import me.leeseol.town.model.NeutralZone;
import me.leeseol.town.model.Town;
import me.leeseol.town.model.War;
import me.leeseol.town.model.WarMode;
import me.leeseol.town.model.WarStatus;
import me.leeseol.town.storage.TownStore;
import me.leeseol.town.structure.StructureDefinition;
import me.leeseol.town.util.Text;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TownService {
    private static final long DISBAND_CONFIRM_MILLIS = 30_000L;
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery domainQuery;
    private final TownConfirmationService confirmationService;
    private final TownDisplayService displayService;
    private final TownMembershipService membershipService;
    private final NationService nationService;
    private final ClaimService claimService;
    private final WarService warService;
    private final Map<UUID, PendingDisband> pendingDisbands = new HashMap<>();

    public TownService(LeeSeolTownPlugin plugin, TownStore store) {
        this.plugin = plugin;
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
        store.load();
        if (store.playerTown(player.getUniqueId()) != null) {
            player.sendMessage(plugin.msg("already-in-town"));
            return true;
        }

        String id = TownStore.idFromName(name);
        if (id.isBlank()) {
            player.sendMessage("/party create <name>");
            return true;
        }
        if (store.town(id) != null) {
            player.sendMessage(plugin.msg("town-exists"));
            return true;
        }

        Town town = new Town(id, name, player.getUniqueId(), System.currentTimeMillis());
        store.addTown(town);
        store.save();
        updateIdentity(player);
        player.sendMessage(plugin.msg("town-created").replace("%party%", town.name()).replace("%town%", town.name()));
        return true;
    }

    public boolean invite(Player sender, String targetName) {
        if (targetName == null || targetName.isBlank()) {
            sender.sendMessage("/party invite <player>");
            return true;
        }
        store.load();
        Town town = requireLeaderTown(sender);
        if (town == null) {
            return true;
        }
        Player localTarget = Bukkit.getPlayerExact(targetName);
        UUID targetId = localTarget == null ? resolveOfflineUuid(targetName) : localTarget.getUniqueId();
        if (targetId != null && store.playerTown(targetId) != null) {
            sender.sendMessage(plugin.msg("already-in-town"));
            return true;
        }
        if (town.members().size() >= plugin.partyMaxMembers()) {
            sender.sendMessage(plugin.msg("party-full").replace("%count%", String.valueOf(plugin.partyMaxMembers())));
            return true;
        }

        if (targetId != null) {
            town.invites().add(targetId);
        }
        town.inviteNames().add(TownStore.normalizeName(targetName));
        store.save();
        sender.sendMessage(plugin.msg("invite-sent").replace("%player%", targetName));
        if (localTarget != null) {
            localTarget.sendMessage(inviteMessage(town));
        } else {
            sendRemoteInvite(sender, targetName, town);
        }
        return true;
    }

    public boolean joinTown(Player player, String townName) {
        return acceptInvite(player, townName);
    }

    public boolean acceptInvite(Player player, String townName) {
        store.load();
        if (store.playerTown(player.getUniqueId()) != null) {
            player.sendMessage(plugin.msg("already-in-town"));
            return true;
        }

        Town town = store.town(TownStore.idFromName(townName));
        if (town == null) {
            player.sendMessage(plugin.msg("town-not-found").replace("%town%", townName));
            return true;
        }
        if (town.members().size() >= plugin.partyMaxMembers()) {
            player.sendMessage(plugin.msg("party-full").replace("%count%", String.valueOf(plugin.partyMaxMembers())));
            return true;
        }
        boolean invitedByUuid = town.invites().remove(player.getUniqueId());
        boolean invitedByName = town.inviteNames().remove(TownStore.normalizeName(player.getName()));
        if (!invitedByUuid && !invitedByName && !player.hasPermission("leeseoltown.admin")) {
            player.sendMessage(plugin.msg("no-invite"));
            return true;
        }

        town.members().add(player.getUniqueId());
        store.rebuildIndexes();
        store.save();
        updateIdentity(player);
        player.sendMessage(plugin.msg("joined").replace("%party%", town.name()).replace("%town%", town.name()));
        broadcastTown(town, Text.component(plugin.msgRaw("party-joined")
                .replace("%player%", player.getName())
                .replace("%party%", town.name())
                .replace("%town%", town.name())), player);
        return true;
    }

    public boolean denyInvite(Player player, String townName) {
        store.load();
        Town town = store.town(TownStore.idFromName(townName));
        if (town == null) {
            player.sendMessage(plugin.msg("town-not-found").replace("%town%", townName));
            return true;
        }

        boolean removedByUuid = town.invites().remove(player.getUniqueId());
        boolean removedByName = town.inviteNames().remove(TownStore.normalizeName(player.getName()));
        if (!removedByUuid && !removedByName) {
            player.sendMessage(plugin.msg("no-invite"));
            return true;
        }

        store.save();
        player.sendMessage(plugin.msg("invite-denied").replace("%party%", town.name()).replace("%town%", town.name()));
        return true;
    }

    public boolean leaveTown(Player player) {
        store.load();
        Town town = store.playerTown(player.getUniqueId());
        if (town == null) {
            player.sendMessage(plugin.msg("not-in-town"));
            return true;
        }
        if (town.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.msg("leader-cannot-leave"));
            return true;
        }

        town.members().remove(player.getUniqueId());
        town.invites().remove(player.getUniqueId());
        town.inviteNames().remove(TownStore.normalizeName(player.getName()));
        store.rebuildIndexes();
        store.save();
        updateIdentity(player);
        player.sendMessage(plugin.msg("left"));
        broadcastTown(town, Text.component(plugin.msgRaw("party-left-broadcast")
                .replace("%player%", player.getName())
                .replace("%party%", town.name())
                .replace("%town%", town.name())), player);
        return true;
    }

    public boolean disbandTown(Player player) {
        store.load();
        Town town = requireStrictLeaderTown(player);
        if (town == null) {
            return true;
        }
        if (!confirmDisband(player, "town", town.id(), "disband-town-warning", "/party disband", town.name())) {
            return true;
        }

        Set<UUID> memberIds = new LinkedHashSet<>(town.members());
        store.removeTown(town);
        store.save();
        for (Player member : onlineMembers(memberIds)) {
            updateIdentity(member);
        }
        broadcastMembers(memberIds, Text.component(plugin.msgRaw("disbanded")
                .replace("%party%", town.name())
                .replace("%town%", town.name())), player);
        return true;
    }

    public boolean transferLeader(Player player, String targetName) {
        store.load();
        Town town = requireStrictLeaderTown(player);
        if (town == null) {
            return true;
        }

        UUID targetId = resolveOfflineUuid(targetName);
        if (targetId == null || !town.isMember(targetId)) {
            player.sendMessage(plugin.msg("party-member-not-found").replace("%player%", targetName));
            return true;
        }
        if (targetId.equals(player.getUniqueId())) {
            player.sendMessage(plugin.msg("party-transfer-self"));
            return true;
        }

        String confirmId = town.id() + ":" + targetId;
        if (!confirmAction(player, "transfer", confirmId, "party-transfer-warning", "/party transfer " + targetName, targetName)) {
            return true;
        }

        town.setLeader(targetId);
        store.save();
        updateAllIdentities();
        broadcastTown(town, Text.component(plugin.msgRaw("party-transfer-done")
                .replace("%player%", targetName)
                .replace("%party%", town.name())
                .replace("%town%", town.name())), player);
        return true;
    }

    public boolean kickMember(Player player, String targetName) {
        store.load();
        Town town = requireStrictLeaderTown(player);
        if (town == null) {
            return true;
        }

        UUID targetId = resolveOfflineUuid(targetName);
        if (targetId == null || !town.isMember(targetId)) {
            player.sendMessage(plugin.msg("party-member-not-found").replace("%player%", targetName));
            return true;
        }
        if (targetId.equals(player.getUniqueId())) {
            player.sendMessage(plugin.msg("party-kick-self"));
            return true;
        }

        Player target = Bukkit.getPlayer(targetId);
        String displayName = target == null || target.getName() == null ? targetName : target.getName();
        town.members().remove(targetId);
        town.invites().remove(targetId);
        town.inviteNames().remove(TownStore.normalizeName(displayName));
        store.rebuildIndexes();
        store.save();

        if (target != null) {
            updateIdentity(target);
            target.sendMessage(plugin.msg("party-kick-target")
                    .replace("%party%", town.name())
                    .replace("%town%", town.name()));
        } else {
            sendRemoteMessage(player, displayName, plainJson(Text.stripColor(plugin.msg("party-kick-target")
                    .replace("%party%", town.name())
                    .replace("%town%", town.name()))));
        }
        broadcastTown(town, Text.component(plugin.msgRaw("party-kicked")
                .replace("%player%", displayName)
                .replace("%party%", town.name())
                .replace("%town%", town.name())), player);
        return true;
    }

    public boolean claimChunk(Player player) {
        if (!player.hasPermission("leeseoltown.claim")) {
            player.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        store.load();

        Town town = store.playerTown(player.getUniqueId());
        if (town == null) {
            player.sendMessage(plugin.msg("not-in-town"));
            return true;
        }
        Nation nation = town.nationId() == null ? null : store.nation(town.nationId());
        if (nation == null) {
            player.sendMessage(plugin.msg("claim-nation-required"));
            return true;
        }
        processExpiredWarState();
        if (!ensureNationActive(player, nation)) {
            return true;
        }
        if (!canManageNation(player, town, nation)) {
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
        Town owner = store.claimTown(claim);
        if (owner != null) {
            player.sendMessage(plugin.msg("claim-owned"));
            return true;
        }
        if (!isAdjacentToNationClaim(nation, claim)) {
            player.sendMessage(plugin.msg("claim-adjacent-required"));
            return true;
        }

        int nextClaimCount = nationClaimCount(nation) + 1;
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
        refreshNationClaimMarkers();
        player.sendMessage(plugin.msg("claim-bought")
                .replace("%cost%", player.hasPermission("leeseoltown.admin") ? "관리자 우회" : plugin.formatMoney(cost))
                .replace("%upkeep%", plugin.formatMoney(plugin.dailyChunkUpkeep(claim, nextClaimCount)))
                .replace("%zone%", plugin.claimZoneName(claim)));
        return true;
    }

    public boolean sendClaimPrice(Player player) {
        store.load();
        Town town = store.playerTown(player.getUniqueId());
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
        int nextClaimCount = nationClaimCount(nation) + 1;
        player.sendMessage(plugin.msg("claim-price")
                .replace("%chunk%", claim.display())
                .replace("%zone%", plugin.claimZoneName(claim))
                .replace("%cost%", plugin.formatMoney(plugin.chunkClaimCost(claim, nation, nextClaimCount)))
                .replace("%upkeep%", plugin.formatMoney(plugin.dailyChunkUpkeep(claim, nextClaimCount))));
        return true;
    }

    public boolean unclaimChunk(Player player) {
        store.load();
        Town town = store.playerTown(player.getUniqueId());
        if (town == null) {
            player.sendMessage(plugin.msg("not-in-town"));
            return true;
        }

        ClaimKey claim = ClaimKey.from(player.getLocation());
        Town owner = store.claimTown(claim);
        if (owner == null) {
            player.sendMessage(plugin.msg("claim-not-owned"));
            return true;
        }
        Nation nation = owner.nationId() == null ? null : store.nation(owner.nationId());
        if (nation != null) {
            processExpiredWarState();
            if (!ensureNationActive(player, nation)) {
                return true;
            }
            if (!canManageNation(player, town, nation)) {
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
        refreshNationClaimMarkers();
        player.sendMessage(plugin.msg("claim-removed"));
        return true;
    }

    public boolean createNation(Player player, String name, String colorKey, List<String> extraPartyNames) {
        if (colorKey == null || colorKey.isBlank()) {
            player.sendMessage("/party nation create <name> <color> [party...]");
            return true;
        }

        store.load();
        NationColorPalette palette = NationColorPalette.from(plugin.getConfig());
        NationColor color = palette.resolve(colorKey, usedNationColorKeys());
        if (color == null) {
            if (palette.keys().contains(NationColor.normalizeKey(colorKey))) {
                player.sendMessage(plugin.msg("nation-color-taken").replace("%color%", colorKey));
            } else {
                player.sendMessage(plugin.msg("nation-color-invalid").replace("%colors%", String.join(", ", palette.keys())));
            }
            return true;
        }

        Town town = player.hasPermission("leeseoltown.admin") ? requireLeaderTown(player) : requireStrictLeaderTown(player);
        if (town == null) {
            return true;
        }
        if (town.nationId() != null) {
            player.sendMessage(plugin.msg("already-in-nation"));
            return true;
        }
        Set<Town> nationParties = new LinkedHashSet<>();
        nationParties.add(town);
        if (!extraPartyNames.isEmpty() && !player.hasPermission("leeseoltown.admin")) {
            player.sendMessage(plugin.msg("nation-extra-party-admin-only"));
            return true;
        }
        for (String partyName : extraPartyNames) {
            Town extra = store.town(TownStore.idFromName(partyName));
            if (extra == null) {
                player.sendMessage(plugin.msg("town-not-found").replace("%town%", partyName));
                return true;
            }
            if (extra.nationId() != null) {
                player.sendMessage(plugin.msg("party-already-in-nation").replace("%party%", extra.name()).replace("%town%", extra.name()));
                return true;
            }
            nationParties.add(extra);
        }
        int memberCount = nationParties.stream().mapToInt(candidate -> candidate.members().size()).sum();
        boolean bypassRequirements = player.hasPermission("leeseoltown.admin");
        if (!bypassRequirements && memberCount < plugin.nationRequiredMembers()) {
            player.sendMessage(plugin.msg("nation-member-required").replace("%count%", String.valueOf(plugin.nationRequiredMembers())));
            return true;
        }

        String id = TownStore.idFromName(name);
        Nation nation = new Nation(id, name, color, town.id(), System.currentTimeMillis());
        nation.setLastUpkeepPeriod(plugin.currentUpkeepPeriod());
        nation.townIds().clear();
        for (Town party : nationParties) {
            nation.townIds().add(party.id());
            party.setNationId(id);
        }
        store.addNation(nation);
        store.save();
        refreshNationClaimMarkers();
        updateAllIdentities();
        giveNationBeacon(player);
        broadcastNation(nation, Text.component(plugin.msgRaw("nation-created")
                .replace("%nation%", nation.name())
                .replace("%color%", nation.color().displayName())
                .replace("%members%", String.valueOf(memberCount))
                .replace("%tax%", plugin.formatMoney(dailyNationUpkeep(nation)))));
        return true;
    }

    public List<String> nationColorKeys() {
        return NationColorPalette.from(plugin.getConfig()).keys();
    }

    private Set<String> usedNationColorKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (Nation nation : store.nations()) {
            if (nation.color() != null) {
                keys.add(nation.color().key());
            }
        }
        return keys;
    }

    public boolean disbandNation(Player player) {
        store.load();
        Town town = player.hasPermission("leeseoltown.admin") ? requireLeaderTown(player) : requireStrictLeaderTown(player);
        if (town == null) {
            return true;
        }

        Nation nation = town.nationId() == null ? null : store.nation(town.nationId());
        if (nation == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        if (!player.hasPermission("leeseoltown.admin") && !town.id().equals(nation.capitalTownId())) {
            player.sendMessage(plugin.msg("not-nation-leader"));
            return true;
        }
        if (!confirmDisband(player, "nation", nation.id(), "disband-nation-warning", "/party nation disband", nation.name())) {
            return true;
        }

        List<Player> onlineMembers = onlineNationMembers(nation);
        store.removeNation(nation);
        store.save();
        refreshNationClaimMarkers();
        updateAllIdentities();
        for (Player member : onlineMembers) {
            member.sendMessage(plugin.msg("nation-disbanded").replace("%nation%", nation.name()));
        }
        return true;
    }

    public boolean setNationPvp(Player player, boolean enabled) {
        store.load();
        Town town = store.playerTown(player.getUniqueId());
        Nation nation = town == null || town.nationId() == null ? null : store.nation(town.nationId());
        if (town == null || nation == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        processExpiredWarState();
        if (!ensureNationActive(player, nation)) {
            return true;
        }
        if (!canManageNation(player, town, nation)) {
            player.sendMessage(plugin.msg("not-nation-leader"));
            return true;
        }
        nation.setPvpEnabled(enabled);
        store.save();
        broadcastNation(nation, Text.component(plugin.msgRaw(enabled ? "nation-pvp-on" : "nation-pvp-off")
                .replace("%nation%", nation.name())));
        return true;
    }

    public boolean setNationBuildProtection(Player player, boolean enabled) {
        store.load();
        Town town = store.playerTown(player.getUniqueId());
        Nation nation = town == null || town.nationId() == null ? null : store.nation(town.nationId());
        if (town == null || nation == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        processExpiredWarState();
        if (!ensureNationActive(player, nation)) {
            return true;
        }
        if (!canManageNation(player, town, nation)) {
            player.sendMessage(plugin.msg("not-nation-leader"));
            return true;
        }
        nation.setBuildProtectionEnabled(enabled);
        store.save();
        broadcastNation(nation, Text.component(plugin.msgRaw(enabled ? "nation-build-protection-on" : "nation-build-protection-off")
                .replace("%nation%", nation.name())));
        return true;
    }

    public boolean depositNationTreasury(Player player, double amount) {
        if (amount <= 0.0D) {
            player.sendMessage("/party nation deposit <amount>");
            return true;
        }
        store.load();
        Town town = store.playerTown(player.getUniqueId());
        Nation nation = town == null || town.nationId() == null ? null : store.nation(town.nationId());
        if (town == null || nation == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        processExpiredWarState();
        if (!plugin.economy().available()) {
            player.sendMessage(plugin.msg("economy-missing"));
            return true;
        }
        if (!plugin.economy().has(player, amount) || !plugin.economy().withdraw(player, amount)) {
            player.sendMessage(plugin.msg("not-enough-money").replace("%cost%", plugin.formatMoney(amount)));
            return true;
        }
        nation.setTreasury(nation.treasury() + amount);
        store.save();
        broadcastNation(nation, Text.component(plugin.msgRaw("nation-treasury-deposited")
                .replace("%player%", player.getName())
                .replace("%amount%", plugin.formatMoney(amount))
                .replace("%treasury%", plugin.formatMoney(nation.treasury()))));
        return true;
    }

    public boolean sendNationTreasury(Player player) {
        store.load();
        Town town = store.playerTown(player.getUniqueId());
        Nation nation = town == null || town.nationId() == null ? null : store.nation(town.nationId());
        if (town == null || nation == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        processExpiredWarState();
        player.sendMessage(plugin.msg("nation-treasury-info")
                .replace("%nation%", nation.name())
                .replace("%treasury%", plugin.formatMoney(nation.treasury()))
                .replace("%karma%", String.valueOf(nation.karma()))
                .replace("%debt%", plugin.formatMoney(nation.debtAmount())));
        player.sendMessage(Text.component("&7일일 유지비: &e" + plugin.formatMoney(dailyNationUpkeep(nation))
                + " &7| 유지비 체납: &c" + plugin.formatMoney(nation.upkeepDebt())
                + " &7| 정산일: &f" + (nation.lastUpkeepPeriod() == null ? "-" : nation.lastUpkeepPeriod())));
        return true;
    }

    public boolean sendNationUpkeep(Player player) {
        store.load();
        processExpiredWarState();
        Town town = store.playerTown(player.getUniqueId());
        Nation nation = town == null || town.nationId() == null ? null : store.nation(town.nationId());
        if (town == null || nation == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        player.sendMessage(plugin.msg("nation-upkeep-info")
                .replace("%nation%", nation.name())
                .replace("%upkeep%", plugin.formatMoney(dailyNationUpkeep(nation)))
                .replace("%treasury%", plugin.formatMoney(nation.treasury()))
                .replace("%debt%", plugin.formatMoney(nation.upkeepDebt()))
                .replace("%period%", nation.lastUpkeepPeriod() == null ? "-" : nation.lastUpkeepPeriod()));
        return true;
    }

    public boolean payNationUpkeep(Player player) {
        store.load();
        processExpiredWarState();
        Town town = store.playerTown(player.getUniqueId());
        Nation nation = requireNationLeader(player, town);
        if (nation == null) {
            return true;
        }
        if (nation.upkeepDebt() <= 0.0D) {
            player.sendMessage(plugin.msg("nation-upkeep-no-debt"));
            return true;
        }
        if (nation.treasury() < nation.upkeepDebt()) {
            player.sendMessage(plugin.msg("nation-upkeep-not-enough")
                    .replace("%amount%", plugin.formatMoney(nation.upkeepDebt()))
                    .replace("%treasury%", plugin.formatMoney(nation.treasury())));
            return true;
        }
        double amount = nation.upkeepDebt();
        nation.setTreasury(nation.treasury() - amount);
        nation.setUpkeepDebt(0.0D);
        if (!hasExpiredWarDebt(nation)) {
            nation.setFunctionsSuspended(false);
        }
        store.save();
        broadcastNation(nation, Text.component(plugin.msgRaw("nation-upkeep-paid")
                .replace("%nation%", nation.name())
                .replace("%amount%", plugin.formatMoney(amount))
                .replace("%treasury%", plugin.formatMoney(nation.treasury()))));
        return true;
    }

    public boolean declareWar(Player player, String targetNationName) {
        return declareWar(player, targetNationName, WarMode.INVASION);
    }

    public boolean declareWar(Player player, String targetNationName, WarMode mode) {
        store.load();
        processExpiredWarState();
        WarMode warMode = mode == null ? WarMode.INVASION : mode;
        Town town = store.playerTown(player.getUniqueId());
        Nation attacker = requireNationLeader(player, town);
        if (attacker == null || !ensureNationActive(player, attacker)) {
            return true;
        }
        Nation defender = nationByName(targetNationName);
        if (defender == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        if (attacker.id().equals(defender.id())) {
            player.sendMessage(plugin.msg("war-self"));
            return true;
        }
        if (findWarBetween(attacker.id(), defender.id()) != null) {
            player.sendMessage(plugin.msg("war-already-exists"));
            return true;
        }

        War war = new War(War.id(attacker.id(), defender.id()), attacker.id(), defender.id(), warMode, WarStatus.PENDING, System.currentTimeMillis());
        store.addWar(war);
        store.save();
        broadcastNation(attacker, Text.component(plugin.msgRaw("war-declared-attacker")
                .replace("%attacker%", attacker.name())
                .replace("%defender%", defender.name())
                .replace("%mode%", warMode.displayName())));
        broadcastNation(defender, Text.component(plugin.msgRaw("war-declared-defender")
                .replace("%attacker%", attacker.name())
                .replace("%defender%", defender.name())
                .replace("%mode%", warMode.displayName())));
        return true;
    }

    public boolean acceptWar(Player player, String attackerNationName) {
        store.load();
        processExpiredWarState();
        Town town = store.playerTown(player.getUniqueId());
        Nation defender = requireNationLeader(player, town);
        if (defender == null || !ensureNationActive(player, defender)) {
            return true;
        }
        Nation attacker = nationByName(attackerNationName);
        if (attacker == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        War war = findPendingWar(attacker.id(), defender.id());
        if (war == null) {
            player.sendMessage(plugin.msg("war-not-found"));
            return true;
        }

        long protectionUntil = System.currentTimeMillis() + plugin.warProtectionMillis();
        war.setStatus(WarStatus.ACTIVE);
        war.setDefenderProtectionActive(true);
        war.setProtectionUntil(protectionUntil);
        store.save();
        broadcastNation(attacker, Text.component(plugin.msgRaw("war-accepted-attacker")
                .replace("%attacker%", attacker.name())
                .replace("%defender%", defender.name())));
        broadcastNation(defender, Text.component(plugin.msgRaw("war-accepted-defender")
                .replace("%attacker%", attacker.name())
                .replace("%defender%", defender.name())
                .replace("%minutes%", String.valueOf(plugin.warProtectionMillis() / 60_000L))));
        return true;
    }

    public boolean surrenderWar(Player player, String enemyNationName) {
        store.load();
        processExpiredWarState();
        Town town = store.playerTown(player.getUniqueId());
        Nation surrenderer = requireNationLeader(player, town);
        if (surrenderer == null) {
            return true;
        }
        if (!ensureNationActive(player, surrenderer)) {
            return true;
        }
        Nation winner = nationByName(enemyNationName);
        if (winner == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        War war = findWarBetween(surrenderer.id(), winner.id());
        if (war == null) {
            player.sendMessage(plugin.msg("war-not-found"));
            return true;
        }

        surrenderer.addKarma(-10);
        surrenderer.setSurrenderWinStreak(0);
        winner.addKarma(10);
        winner.setSurrenderWinStreak(winner.surrenderWinStreak() + 1);
        boolean streakPenalty = false;
        if (winner.surrenderWinStreak() >= 3) {
            winner.addKarma(-10);
            winner.setSurrenderWinStreak(0);
            streakPenalty = true;
        }
        applySurrenderPayment(surrenderer, winner);
        store.removeWar(war);
        store.save();

        broadcastNation(surrenderer, Text.component(plugin.msgRaw("war-surrendered")
                .replace("%loser%", surrenderer.name())
                .replace("%winner%", winner.name())
                .replace("%karma%", String.valueOf(surrenderer.karma()))));
        broadcastNation(winner, Text.component(plugin.msgRaw(streakPenalty ? "war-surrender-received-streak" : "war-surrender-received")
                .replace("%loser%", surrenderer.name())
                .replace("%winner%", winner.name())
                .replace("%karma%", String.valueOf(winner.karma()))
                .replace("%amount%", plugin.formatMoney(plugin.warSurrenderPayment()))));
        return true;
    }

    public boolean releaseWarProtection(Player player, String enemyNationName) {
        store.load();
        processExpiredWarState();
        Town town = store.playerTown(player.getUniqueId());
        Nation defender = requireNationLeader(player, town);
        if (defender == null) {
            return true;
        }
        Nation attacker = nationByName(enemyNationName);
        if (attacker == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        War war = findWarBetween(attacker.id(), defender.id());
        if (war == null || war.status() != WarStatus.ACTIVE || !war.defenderNationId().equals(defender.id()) || !war.defenderProtectionActive()) {
            player.sendMessage(plugin.msg("war-protection-not-active"));
            return true;
        }
        war.setDefenderProtectionActive(false);
        war.setProtectionUntil(0L);
        store.save();
        broadcastNation(defender, Text.component(plugin.msgRaw("war-protection-released-self")
                .replace("%nation%", defender.name())
                .replace("%enemy%", attacker.name())));
        broadcastNation(attacker, Text.component(plugin.msgRaw("war-protection-released-enemy")
                .replace("%nation%", defender.name())
                .replace("%enemy%", attacker.name())));
        return true;
    }

    public boolean payWarDebt(Player player) {
        store.load();
        processExpiredWarState();
        Town town = store.playerTown(player.getUniqueId());
        Nation nation = requireNationLeader(player, town);
        if (nation == null) {
            return true;
        }
        if (nation.debtAmount() <= 0.0D || nation.debtCreditorNationId() == null) {
            player.sendMessage(plugin.msg("war-no-debt"));
            return true;
        }
        if (nation.treasury() < nation.debtAmount()) {
            player.sendMessage(plugin.msg("war-debt-not-enough")
                    .replace("%amount%", plugin.formatMoney(nation.debtAmount()))
                    .replace("%treasury%", plugin.formatMoney(nation.treasury())));
            return true;
        }

        Nation creditor = store.nation(nation.debtCreditorNationId());
        double amount = nation.debtAmount();
        nation.setTreasury(nation.treasury() - amount);
        if (creditor != null) {
            creditor.setTreasury(creditor.treasury() + amount);
        }
        clearDebt(nation);
        store.save();
        broadcastNation(nation, Text.component(plugin.msgRaw("war-debt-paid")
                .replace("%nation%", nation.name())
                .replace("%amount%", plugin.formatMoney(amount))));
        if (creditor != null) {
            broadcastNation(creditor, Text.component(plugin.msgRaw("war-debt-paid-creditor")
                    .replace("%nation%", nation.name())
                    .replace("%amount%", plugin.formatMoney(amount))));
        }
        return true;
    }

    public boolean finishWar(Player player, String winnerName, String loserName) {
        if (!player.hasPermission("leeseoltown.admin")) {
            player.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        store.load();
        processExpiredWarState();
        Nation winner = nationByName(winnerName);
        Nation loser = nationByName(loserName);
        if (winner == null || loser == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        War war = findWarBetween(winner.id(), loser.id());
        if (war == null) {
            player.sendMessage(plugin.msg("war-not-found"));
            return true;
        }

        winner.addKarma(10);
        winner.setSurrenderWinStreak(0);
        loser.setSurrenderWinStreak(0);
        store.removeWar(war);
        store.save();
        broadcastNation(winner, Text.component(plugin.msgRaw("war-finished-winner")
                .replace("%winner%", winner.name())
                .replace("%loser%", loser.name())
                .replace("%karma%", String.valueOf(winner.karma()))));
        broadcastNation(loser, Text.component(plugin.msgRaw("war-finished-loser")
                .replace("%winner%", winner.name())
                .replace("%loser%", loser.name())));
        return true;
    }

    public void setChatMode(Player player, ChatMode mode) {
        store.setChatMode(player.getUniqueId(), mode);
        player.sendMessage(plugin.msg("chat-mode").replace("%mode%", mode.displayName()));
    }

    public void sendTownChat(Player player, Component message) {
        Town town = store.playerTown(player.getUniqueId());
        if (town == null) {
            player.sendMessage(plugin.msg("chat-no-town"));
            return;
        }
        Component line = chatLine("town-format", player, message);
        broadcastTown(town, line);
    }

    public void sendNationChat(Player player, Component message) {
        Town town = store.playerTown(player.getUniqueId());
        Nation nation = town == null || town.nationId() == null ? null : store.nation(town.nationId());
        if (nation == null) {
            player.sendMessage(plugin.msg("chat-no-nation"));
            return;
        }
        Component line = chatLine("nation-format", player, message);
        broadcastNation(nation, line);
    }

    public void broadcastGlobalChat(Player player, Component message) {
        Component line = chatLine("global-format", player, message);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.sendMessage(line);
        }
        Bukkit.getConsoleSender().sendMessage(line);
    }

    public Component chatLine(String formatPath, Player player, Component message) {
        String format = plugin.getConfig().getString("chat." + formatPath, "%rank%%affiliation%%player%: %message%");
        String beforeMessage = format
                .replace("%rank%", rankPrefix(player))
                .replace("%affiliation%", affiliationPrefix(player))
                .replace("%player%", player.getName());
        String[] parts = beforeMessage.split("%message%", 2);
        Component component = Text.component(parts[0]).append(message);
        if (parts.length > 1) {
            component = component.append(Text.component(parts[1]));
        }
        return component;
    }

    public String affiliationPrefix(Player player) {
        Town town = store.playerTown(player.getUniqueId());
        if (town == null) {
            return "";
        }

        String townPrefix = plugin.getConfig().getString("prefix.town", "&#8FD9A8%town% ")
                .replace("%town%", town.name());
        Nation nation = town.nationId() == null ? null : store.nation(town.nationId());
        if (nation == null) {
            return townPrefix;
        }
        String nationPrefix = plugin.getConfig().getString("prefix.nation", "%nation_color%%nation% ")
                .replace("%town%", town.name())
                .replace("%nation%", nation.name())
                .replace("%nation_color%", nation.color().legacyPrefix());
        return nationPrefix;
    }

    public String rankPrefix(Player player) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            String leeseolRank = PlaceholderAPI.setPlaceholders(player, "%leeseolranks_prefix%");
            if (leeseolRank != null
                    && !leeseolRank.isBlank()
                    && !leeseolRank.equals("%leeseolranks_prefix%")) {
                return leeseolRank;
            }
            String rankImage = rankImage(player);
            if (!rankImage.isBlank()) {
                return rankImage + " ";
            }
            if (player.hasPermission("betterranks.admin")) {
                String image = PlaceholderAPI.setPlaceholders(player, "%img_admin%");
                return image == null || image.isBlank() ? "" : image + " ";
            }
            if (player.hasPermission("betterranks.player")) {
                String image = PlaceholderAPI.setPlaceholders(player, "%img_player%");
                return image == null || image.isBlank() ? "" : image + " ";
            }
        }
        if (player.hasPermission("betterranks.admin")) {
            return "&c[관리자] ";
        }
        return "";
    }

    private String rankImage(Player player) {
        String imageKey = null;
        if (player.hasPermission("leeseolranks.rank.s")) {
            imageKey = "rank_s";
        } else if (player.hasPermission("leeseolranks.rank.a")) {
            imageKey = "rank_a";
        } else if (player.hasPermission("leeseolranks.rank.b")) {
            imageKey = "rank_b";
        } else if (player.hasPermission("leeseolranks.rank.c")) {
            imageKey = "rank_c";
        } else if (player.hasPermission("leeseolranks.rank.d")) {
            imageKey = "rank_d";
        }
        if (imageKey == null) {
            return "";
        }
        String image = PlaceholderAPI.setPlaceholders(player, "%img_" + imageKey + "%");
        return image == null || image.isBlank() ? "" : image;
    }

    public void updateIdentity(Player player) {
        String prefix = rankPrefix(player) + affiliationPrefix(player);
        String rawName = prefix + "&f" + player.getName();
        Component name = Text.component(rawName);
        String legacyName = Text.color(rawName);
        player.setDisplayName(legacyName);
        player.setPlayerListName(legacyName);
        player.displayName(name);
        player.playerListName(name);
    }

    public void updateAllIdentities() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateIdentity(player);
        }
    }

    public boolean canBuild(Player player, ClaimKey claim) {
        Town owner = store.claimTown(claim);
        if (owner == null || player.hasPermission("leeseoltown.admin")) {
            return true;
        }
        Nation nation = owner.nationId() == null ? null : store.nation(owner.nationId());
        if (nation == null) {
            return owner.isMember(player.getUniqueId());
        }
        processExpiredWarState();
        if (nation.functionsSuspended()) {
            return true;
        }
        Nation playerNation = playerNation(player);
        if (isWarProtectedAgainst(nation, playerNation)) {
            return false;
        }
        if (!nation.buildProtectionEnabled()) {
            return true;
        }
        return isNationMember(player.getUniqueId(), nation);
    }

    public boolean shouldCancelNationBeaconPlace(Player player, ClaimKey claim) {
        Town town = store.playerTown(player.getUniqueId());
        Nation nation = town == null || town.nationId() == null ? null : store.nation(town.nationId());
        if (nation == null || nation.beaconClaim() != null) {
            return false;
        }
        if (!canManageNation(player, town, nation)) {
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
        Town owner = store.claimTown(claim);
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
        refreshNationClaimMarkers();
        player.sendMessage(plugin.msg("beacon-placed")
                .replace("%nation%", nation.name())
                .replace("%chunk%", claim.display()));
        return false;
    }

    public boolean canPlaceNationCoreStructure(Player player, ClaimKey claim) {
        Town town = store.playerTown(player.getUniqueId());
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
        if (!canManageNation(player, town, nation)) {
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
        Town owner = store.claimTown(claim);
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
        Town town = store.playerTown(player.getUniqueId());
        Nation nation = town == null || town.nationId() == null ? null : store.nation(town.nationId());
        if (town == null || nation == null || nation.beaconClaim() != null) {
            return false;
        }
        Town owner = store.claimTown(claim);
        nation.setBeaconClaim(claim);
        if (owner == null) {
            town.claims().add(claim);
            store.rebuildIndexes();
        }
        store.save();
        refreshNationClaimMarkers();
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
            Town owner = store.claimTown(claim);
            if (owner != null && nationId.equals(owner.nationId()) && owner.claims().remove(claim)) {
                store.rebuildIndexes();
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        store.save();
        refreshNationClaimMarkers();
    }

    public boolean shouldApplyBeaconFatigue(Player player, ClaimKey claim) {
        Nation nation = nationForBeaconClaim(claim);
        return nation != null
                && !nation.functionsSuspended()
                && !isNationMember(player.getUniqueId(), nation)
                && !player.hasPermission("leeseoltown.admin");
    }

    public boolean shouldBlockWarEntry(Player player, ClaimKey claim) {
        if (player.hasPermission("leeseoltown.admin")) {
            return false;
        }
        processExpiredWarState();
        Nation ownerNation = nationForClaim(claim);
        if (ownerNation == null || ownerNation.functionsSuspended()) {
            return false;
        }
        Nation playerNation = playerNation(player);
        return isWarProtectedAgainst(ownerNation, playerNation);
    }

    public boolean canPvp(Player attacker, Player victim) {
        Nation nation = nationForClaim(ClaimKey.from(victim.getLocation()));
        if (nation == null) {
            nation = nationForClaim(ClaimKey.from(attacker.getLocation()));
        }
        return nation == null || nation.functionsSuspended() || nation.pvpEnabled();
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
        processExpiredWarState();
        for (War war : store.wars()) {
            if ((war.status() == WarStatus.PENDING || war.status() == WarStatus.ACTIVE)
                    && war.involves(nation.id())) {
                return true;
            }
        }
        return false;
    }

    public Town claimTown(ClaimKey claim) {
        return store.claimTown(claim);
    }

    public ChatMode chatMode(Player player) {
        return store.chatMode(player.getUniqueId());
    }

    public Town playerTown(Player player) {
        return store.playerTown(player.getUniqueId());
    }

    public Nation playerNation(Player player) {
        Town town = playerTown(player);
        return town == null || town.nationId() == null ? null : store.nation(town.nationId());
    }

    public Collection<Town> towns() {
        return store.towns();
    }

    public Collection<Nation> nations() {
        return store.nations();
    }

    public void sendSelfInfo(Player player) {
        Town town = playerTown(player);
        Nation nation = playerNation(player);

        player.sendMessage(Text.component("&#BEEBFF[소속 정보]"));
        if (town == null) {
            player.sendMessage(Text.component("&7파티: &f없음"));
            player.sendMessage(Text.component("&7국가: &f없음"));
            player.sendMessage(Text.component("&7상태: &f파티를 생성하거나 초대를 받아 가입할 수 있습니다."));
            return;
        }

        String role = town.isLeader(player.getUniqueId()) ? "대표" : "구성원";
        player.sendMessage(Text.component("&7파티: &#8FD9A8" + town.name() + " &8(" + role + ")"));
        player.sendMessage(Text.component("&7파티 인원: &f" + town.members().size() + "/" + plugin.partyMaxMembers()));
        player.sendMessage(Text.component("&7소유 청크: &f" + town.claims().size()));

        if (nation == null) {
            player.sendMessage(Text.component("&7국가: &f없음"));
        } else {
            player.sendMessage(Text.component("&7국가: " + nation.color().legacyPrefix() + nation.name()
                    + " &8(" + nation.color().displayName() + ")"));
            int memberCount = nationMemberCount(nation);
            player.sendMessage(Text.component("&7국가 인원: &f" + memberCount));
            player.sendMessage(Text.component("&7카르마: &f" + nation.karma()));
            player.sendMessage(Text.component("&7국고: &e" + plugin.formatMoney(nation.treasury())));
            player.sendMessage(Text.component("&7일일 국가 유지비: &e" + plugin.formatMoney(dailyNationUpkeep(nation))));
            if (nation.upkeepDebt() > 0.0D) {
                player.sendMessage(Text.component("&7유지비 체납: &c" + plugin.formatMoney(nation.upkeepDebt())));
            }
            if (nation.debtAmount() > 0.0D) {
                player.sendMessage(Text.component("&7전쟁 체납: &c" + plugin.formatMoney(nation.debtAmount())));
            }
            if (nation.functionsSuspended()) {
                player.sendMessage(Text.component("&c국가 기능 정지 상태"));
            }
        }
        player.sendMessage(Text.component("&7채팅 모드: &f" + chatMode(player).displayName()));
    }

    private boolean canManageNation(Player player, Town town, Nation nation) {
        return player.hasPermission("leeseoltown.admin")
                || (town != null
                && nation != null
                && town.id().equals(nation.capitalTownId())
                && town.isLeader(player.getUniqueId()));
    }

    private void refreshNationClaimMarkers() {
        if (plugin.blueMapNationClaimMarkers() != null) {
            plugin.blueMapNationClaimMarkers().refreshLater();
        }
    }

    private boolean isNationMember(UUID playerId, Nation nation) {
        if (nation == null) {
            return false;
        }
        for (String townId : nation.townIds()) {
            Town town = store.town(townId);
            if (town != null && town.isMember(playerId)) {
                return true;
            }
        }
        return false;
    }

    private Nation nationForBeaconClaim(ClaimKey claim) {
        for (Nation nation : store.nations()) {
            if (claim.equals(nation.beaconClaim())) {
                return nation;
            }
        }
        return null;
    }

    private boolean isAdjacentToNationClaim(Nation nation, ClaimKey claim) {
        return isNationClaim(nation, new ClaimKey(claim.world(), claim.x() + 1, claim.z()))
                || isNationClaim(nation, new ClaimKey(claim.world(), claim.x() - 1, claim.z()))
                || isNationClaim(nation, new ClaimKey(claim.world(), claim.x(), claim.z() + 1))
                || isNationClaim(nation, new ClaimKey(claim.world(), claim.x(), claim.z() - 1));
    }

    private boolean isNationClaim(Nation nation, ClaimKey claim) {
        Town owner = store.claimTown(claim);
        return owner != null && owner.nationId() != null && owner.nationId().equals(nation.id());
    }

    private void giveNationBeacon(Player player) {
        StructureDefinition definition = plugin.structureRegistry().get("nation_core");
        ItemStack core = definition == null ? null : plugin.structureCoreItemService().createCoreItem(definition, 1);
        if (core == null) {
            player.sendMessage(plugin.msg("structure-missing-itemsadder-block").replace("%structure%", "leeseolwar:capital_core"));
            return;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(core);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.sendMessage(plugin.msg("nation-beacon-given"));
    }

    private Nation requireNationLeader(Player player, Town town) {
        if (town == null) {
            player.sendMessage(plugin.msg("not-in-town"));
            return null;
        }
        Nation nation = town.nationId() == null ? null : store.nation(town.nationId());
        if (nation == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return null;
        }
        if (!canManageNation(player, town, nation)) {
            player.sendMessage(plugin.msg("not-nation-leader"));
            return null;
        }
        return nation;
    }

    private boolean ensureNationActive(Player player, Nation nation) {
        if (nation == null || !nation.functionsSuspended()) {
            return true;
        }
        if (nation.upkeepDebt() > 0.0D) {
            player.sendMessage(plugin.msg("nation-upkeep-suspended")
                    .replace("%nation%", nation.name())
                    .replace("%amount%", plugin.formatMoney(nation.upkeepDebt())));
            return false;
        }
        player.sendMessage(plugin.msg("nation-suspended")
                .replace("%nation%", nation.name())
                .replace("%amount%", plugin.formatMoney(nation.debtAmount())));
        return false;
    }

    private Nation nationByName(String name) {
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

    private War findWarBetween(String firstNationId, String secondNationId) {
        for (War war : store.wars()) {
            if (war.between(firstNationId, secondNationId)) {
                return war;
            }
        }
        return null;
    }

    private War findPendingWar(String attackerNationId, String defenderNationId) {
        for (War war : store.wars()) {
            if (war.status() == WarStatus.PENDING
                    && war.attackerNationId().equals(attackerNationId)
                    && war.defenderNationId().equals(defenderNationId)) {
                return war;
            }
        }
        return null;
    }

    private void applySurrenderPayment(Nation surrenderer, Nation winner) {
        double amount = plugin.warSurrenderPayment();
        if (amount <= 0.0D) {
            return;
        }
        if (surrenderer.treasury() >= amount) {
            surrenderer.setTreasury(surrenderer.treasury() - amount);
            winner.setTreasury(winner.treasury() + amount);
            clearDebt(surrenderer);
            return;
        }
        surrenderer.setDebtCreditorNationId(winner.id());
        surrenderer.setDebtAmount(amount);
        surrenderer.setDebtDeadline(System.currentTimeMillis() + plugin.warDebtMillis());
        surrenderer.setFunctionsSuspended(false);
        broadcastNation(surrenderer, Text.component(plugin.msgRaw("war-debt-created")
                .replace("%winner%", winner.name())
                .replace("%amount%", plugin.formatMoney(amount))
                .replace("%hours%", String.valueOf(plugin.warDebtMillis() / 3_600_000L))));
    }

    private void clearDebt(Nation nation) {
        nation.setDebtCreditorNationId(null);
        nation.setDebtAmount(0.0D);
        nation.setDebtDeadline(0L);
        nation.setFunctionsSuspended(nation.upkeepDebt() > 0.0D);
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

    private void processExpiredWarState() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (War war : store.wars()) {
            if (war.status() == WarStatus.ACTIVE
                    && war.defenderProtectionActive()
                    && war.protectionUntil() > 0L
                    && now >= war.protectionUntil()) {
                war.setDefenderProtectionActive(false);
                war.setProtectionUntil(0L);
                changed = true;
                Nation defender = store.nation(war.defenderNationId());
                Nation attacker = store.nation(war.attackerNationId());
                if (defender != null) {
                    broadcastNation(defender, Text.component(plugin.msgRaw("war-protection-expired")
                            .replace("%nation%", defender.name())
                            .replace("%enemy%", attacker == null ? "-" : attacker.name())));
                }
                if (attacker != null && defender != null) {
                    broadcastNation(attacker, Text.component(plugin.msgRaw("war-protection-expired-enemy")
                            .replace("%nation%", defender.name())
                            .replace("%enemy%", attacker.name())));
                }
            }
        }

        for (Nation nation : store.nations()) {
            if (nation.debtAmount() <= 0.0D || nation.debtDeadline() <= 0L || now < nation.debtDeadline() || nation.functionsSuspended()) {
                continue;
            }
            nation.setFunctionsSuspended(true);
            changed = true;
            for (War war : store.wars()) {
                if (war.status() == WarStatus.ACTIVE && war.defenderNationId().equals(nation.id())) {
                    war.setDefenderProtectionActive(false);
                    war.setProtectionUntil(0L);
                }
            }
            broadcastNation(nation, Text.component(plugin.msgRaw("war-debt-expired")
                    .replace("%nation%", nation.name())
                    .replace("%amount%", plugin.formatMoney(nation.debtAmount()))));
            Nation creditor = store.nation(nation.debtCreditorNationId());
            if (creditor != null) {
                broadcastNation(creditor, Text.component(plugin.msgRaw("war-debt-expired-creditor")
                        .replace("%nation%", nation.name())
                        .replace("%amount%", plugin.formatMoney(nation.debtAmount()))));
            }
        }

        if (changed) {
            store.save();
        }
    }

    private long nationTax(Nation nation) {
        return dailyNationUpkeep(nation);
    }

    public void collectDueUpkeep(boolean force) {
        if (!plugin.upkeepEnabled()) {
            return;
        }
        store.load();
        processExpiredWarState();

        boolean changed = false;
        String period = plugin.currentUpkeepPeriod();
        for (Nation nation : store.nations()) {
            if (!force && period.equals(nation.lastUpkeepPeriod())) {
                continue;
            }
            if (!force && (nation.lastUpkeepPeriod() == null || nation.lastUpkeepPeriod().isBlank())) {
                nation.setLastUpkeepPeriod(period);
                changed = true;
                continue;
            }
            if (!force && inUpkeepGrace(nation)) {
                nation.setLastUpkeepPeriod(period);
                changed = true;
                continue;
            }

            long upkeep = dailyNationUpkeep(nation);
            nation.setLastUpkeepPeriod(period);
            if (upkeep <= 0L) {
                changed = true;
                continue;
            }

            double paid = Math.min(nation.treasury(), upkeep);
            if (paid > 0.0D) {
                nation.setTreasury(nation.treasury() - paid);
            }
            double unpaid = upkeep - paid;
            if (unpaid > 0.0D) {
                nation.setUpkeepDebt(nation.upkeepDebt() + unpaid);
                nation.setFunctionsSuspended(true);
                broadcastNation(nation, Text.component(plugin.msgRaw("nation-upkeep-failed")
                        .replace("%nation%", nation.name())
                        .replace("%amount%", plugin.formatMoney(upkeep))
                        .replace("%debt%", plugin.formatMoney(nation.upkeepDebt()))));
            } else {
                broadcastNation(nation, Text.component(plugin.msgRaw("nation-upkeep-collected")
                        .replace("%nation%", nation.name())
                        .replace("%amount%", plugin.formatMoney(upkeep))
                        .replace("%treasury%", plugin.formatMoney(nation.treasury()))));
            }
            changed = true;
        }

        if (changed) {
            store.save();
        }
    }

    private long dailyNationUpkeep(Nation nation) {
        if (nation == null || !plugin.upkeepEnabled()) {
            return 0L;
        }
        long total = safeAdd(plugin.dailyBaseNationUpkeep(), plugin.dailyMemberUpkeep(nationMemberCount(nation)));
        List<Long> claimCosts = new ArrayList<>();
        int claimCount = nationClaimCount(nation);
        for (ClaimKey claim : nationClaims(nation)) {
            claimCosts.add(plugin.dailyChunkUpkeep(claim, claimCount));
        }
        claimCosts.sort(Long::compareTo);
        int freeChunks = plugin.freeUpkeepChunks();
        for (int index = freeChunks; index < claimCosts.size(); index++) {
            total = safeAdd(total, claimCosts.get(index));
        }
        return total;
    }

    private boolean inUpkeepGrace(Nation nation) {
        int graceDays = plugin.upkeepGraceDays();
        if (graceDays <= 0) {
            return false;
        }
        long graceMillis = graceDays * 86_400_000L;
        return System.currentTimeMillis() - nation.createdAt() < graceMillis;
    }

    private boolean hasExpiredWarDebt(Nation nation) {
        return nation.debtAmount() > 0.0D
                && nation.debtDeadline() > 0L
                && System.currentTimeMillis() >= nation.debtDeadline();
    }

    private int nationClaimCount(Nation nation) {
        return nationClaims(nation).size();
    }

    private List<ClaimKey> nationClaims(Nation nation) {
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

    private long safeAdd(long left, long right) {
        long result = left + right;
        return result < left ? Long.MAX_VALUE : result;
    }

    private boolean confirmDisband(Player player, String kind, String id, String messageKey, String command, String name) {
        return confirmAction(player, kind, id, messageKey, command, name);
    }

    private boolean confirmAction(Player player, String kind, String id, String messageKey, String command, String name) {
        long now = System.currentTimeMillis();
        PendingDisband pending = pendingDisbands.get(player.getUniqueId());
        if (pending != null && pending.matches(kind, id, now)) {
            pendingDisbands.remove(player.getUniqueId());
            return true;
        }

        pendingDisbands.put(player.getUniqueId(), new PendingDisband(kind, id, now + DISBAND_CONFIRM_MILLIS));
        player.sendMessage(plugin.msg(messageKey)
                .replace("%name%", name)
                .replace("%command%", command)
                .replace("%seconds%", String.valueOf(DISBAND_CONFIRM_MILLIS / 1000L)));
        return false;
    }

    private Town requireStrictLeaderTown(Player player) {
        Town town = store.playerTown(player.getUniqueId());
        if (town == null) {
            player.sendMessage(plugin.msg("not-in-town"));
            return null;
        }
        if (!town.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.msg("not-town-leader"));
            return null;
        }
        return town;
    }

    private Town requireLeaderTown(Player player) {
        Town town = store.playerTown(player.getUniqueId());
        if (town == null) {
            player.sendMessage(plugin.msg("not-in-town"));
            return null;
        }
        if (!town.isLeader(player.getUniqueId()) && !player.hasPermission("leeseoltown.admin")) {
            player.sendMessage(plugin.msg("not-town-leader"));
            return null;
        }
        return town;
    }

    private List<Player> onlineMembers(Town town) {
        return onlineMembers(town.members());
    }

    private List<Player> onlineMembers(Collection<UUID> memberIds) {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : memberIds) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    private List<Player> onlineNationMembers(Nation nation) {
        List<Player> players = new ArrayList<>();
        for (String townId : nation.townIds()) {
            Town town = store.town(townId);
            if (town != null) {
                players.addAll(onlineMembers(town));
            }
        }
        return players;
    }

    private void broadcastTown(Town town, Component message) {
        for (Player player : onlineMembers(town)) {
            player.sendMessage(message);
        }
    }

    private void broadcastTown(Town town, Component message, Player messenger) {
        broadcastMembers(town.members(), message, messenger);
    }

    private void broadcastMembers(Collection<UUID> memberIds, Component message, Player messenger) {
        String json = plainJson(PLAIN.serialize(message));
        for (UUID uuid : memberIds) {
            Player local = Bukkit.getPlayer(uuid);
            if (local != null) {
                local.sendMessage(message);
                continue;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String name = offlinePlayer.getName();
            if (name != null && !name.isBlank()) {
                sendRemoteMessage(messenger, name, json);
            }
        }
    }

    private void broadcastNation(Nation nation, Component message) {
        for (String townId : nation.townIds()) {
            Town town = store.town(townId);
            if (town != null) {
                broadcastTown(town, message);
            }
        }
    }

    private Component inviteMessage(Town town) {
        String partyName = town.name();
        String acceptCommand = "/party accept " + partyName;
        String denyCommand = "/party deny " + partyName;
        Component message = Text.component(plugin.msg("invited")
                .replace("%party%", partyName)
                .replace("%town%", partyName));
        Component acceptButton = Text.component(plugin.msgRaw("invite-accept-button"))
                .clickEvent(ClickEvent.runCommand(acceptCommand))
                .hoverEvent(HoverEvent.showText(Text.component(plugin.msgRaw("invite-accept-hover")
                        .replace("%party%", partyName)
                        .replace("%town%", partyName))));
        Component denyButton = Text.component(plugin.msgRaw("invite-deny-button"))
                .clickEvent(ClickEvent.runCommand(denyCommand))
                .hoverEvent(HoverEvent.showText(Text.component(plugin.msgRaw("invite-deny-hover")
                        .replace("%party%", partyName)
                        .replace("%town%", partyName))));
        return message.append(Component.space()).append(acceptButton).append(Component.space()).append(denyButton);
    }

    @SuppressWarnings("deprecation")
    private UUID resolveOfflineUuid(String playerName) {
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            return offlinePlayer.getUniqueId();
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Could not resolve player UUID for party invite: " + playerName);
            return null;
        }
    }

    private void sendRemoteInvite(Player sender, String targetName, Town town) {
        sendRemoteMessage(sender, targetName, inviteJson(town));
    }

    private void sendRemoteMessage(Player sender, String targetName, String json) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeUTF("MessageRaw");
            output.writeUTF(targetName);
            output.writeUTF(json);
            sender.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to send cross-server party message: " + exception.getMessage());
        }
    }

    private String inviteJson(Town town) {
        String partyName = town.name();
        String acceptCommand = "/party accept " + partyName;
        String denyCommand = "/party deny " + partyName;
        String acceptHover = plugin.msgRaw("invite-accept-hover")
                .replace("%party%", partyName)
                .replace("%town%", partyName);
        String denyHover = plugin.msgRaw("invite-deny-hover")
                .replace("%party%", partyName)
                .replace("%town%", partyName);
        return "["
                + "{\"text\":\"[파티] \",\"color\":\"#8FD9A8\"},"
                + "{\"text\":\"" + json(partyName) + "\",\"color\":\"yellow\"},"
                + "{\"text\":\" 파티에서 초대가 왔습니다. \",\"color\":\"white\"},"
                + "{\"text\":\"[수락]\",\"color\":\"#8FD9A8\",\"bold\":true,"
                + "\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + json(acceptCommand) + "\"},"
                + "\"hoverEvent\":{\"action\":\"show_text\",\"contents\":{\"text\":\"" + json(Text.stripColor(acceptHover)) + "\",\"color\":\"white\"}}},"
                + "{\"text\":\" \",\"color\":\"white\"},"
                + "{\"text\":\"[거절]\",\"color\":\"#FF9AA2\",\"bold\":true,"
                + "\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + json(denyCommand) + "\"},"
                + "\"hoverEvent\":{\"action\":\"show_text\",\"contents\":{\"text\":\"" + json(Text.stripColor(denyHover)) + "\",\"color\":\"white\"}}}"
                + "]";
    }

    private String plainJson(String message) {
        return "{\"text\":\"" + json(message) + "\",\"color\":\"white\"}";
    }

    private static String json(String input) {
        String value = input == null ? "" : input;
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    public String info(Town town) {
        Nation nation = town.nationId() == null ? null : store.nation(town.nationId());
        OfflinePlayer leader = Bukkit.getOfflinePlayer(town.leader());
        String nationText = nation == null ? "-" : nation.name() + " tax=" + plugin.formatMoney(nationTax(nation)) + " karma=" + nation.karma();
        return Text.color("&b" + town.name()
                + " &7leader=&f" + (leader.getName() == null ? town.leader() : leader.getName())
                + " &7members=&f" + town.members().size() + "/" + plugin.partyMaxMembers()
                + " &7claims=&f" + town.claims().size()
                + " &7nation=&f" + nationText);
    }

    private int nationMemberCount(Nation nation) {
        int count = 0;
        for (String townId : nation.townIds()) {
            Town town = store.town(townId);
            if (town != null) {
                count += town.members().size();
            }
        }
        return count;
    }

    private record PendingDisband(String kind, String id, long expiresAt) {
        private boolean matches(String targetKind, String targetId, long now) {
            return kind.equals(targetKind) && id.equals(targetId) && now <= expiresAt;
        }
    }
}
