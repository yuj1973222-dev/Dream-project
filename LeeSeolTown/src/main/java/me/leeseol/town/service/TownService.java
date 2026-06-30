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

    private void refreshNationClaimMarkers() {
        if (plugin.blueMapNationClaimMarkers() != null) {
            plugin.blueMapNationClaimMarkers().refreshLater();
        }
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

    public Nation requireNationLeader(Player player, Town town) {
        return nationService.requireNationLeader(player, town);
    }

    public boolean ensureNationActive(Player player, Nation nation) {
        return nationService.ensureNationActive(player, nation);
    }

    

    

    

    public void processExpiredWarState() {
        warService.processExpiredWarState();
    }

    private long nationTax(Nation nation) {
        return dailyNationUpkeep(nation);
    }

    public void collectDueUpkeep(boolean force) {
        nationService.collectDueUpkeep(force);
    }

    public long dailyNationUpkeep(Nation nation) {
        return nationService.dailyNationUpkeep(nation);
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
        return displayService.info(town);
    }

    private record PendingDisband(String kind, String id, long expiresAt) {
        private boolean matches(String targetKind, String targetId, long now) {
            return kind.equals(targetKind) && id.equals(targetId) && now <= expiresAt;
        }
    }
}
