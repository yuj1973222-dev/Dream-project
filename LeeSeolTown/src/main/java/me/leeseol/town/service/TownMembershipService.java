package me.leeseol.town.service;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.Town;
import me.leeseol.town.storage.TownStore;
import me.leeseol.town.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class TownMembershipService {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;
    private final TownConfirmationService confirmations;
    private final TownDisplayService display;

    public TownMembershipService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query,
                                 TownConfirmationService confirmations, TownDisplayService display) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
        this.confirmations = confirmations;
        this.display = display;
    }

    public boolean createTown(Player player, String name) {
        store.load();
        if (query.playerTown(player) != null) {
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
        display.updateIdentity(player);
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
        if (targetId != null && query.playerTown(targetId) != null) {
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
        if (query.playerTown(player) != null) {
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
        display.updateIdentity(player);
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
        Town town = query.playerTown(player);
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
        display.updateIdentity(player);
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
        if (!confirmations.confirm(player, "town", town.id(), "disband-town-warning", "/party disband", town.name())) {
            return true;
        }

        Set<UUID> memberIds = new LinkedHashSet<>(town.members());
        store.removeTown(town);
        store.save();
        for (Player member : onlineMembers(memberIds)) {
            display.updateIdentity(member);
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
        if (!confirmations.confirm(player, "transfer", confirmId, "party-transfer-warning",
                "/party transfer " + targetName, targetName)) {
            return true;
        }

        town.setLeader(targetId);
        store.save();
        display.updateAllIdentities();
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
            display.updateIdentity(target);
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

    private Town requireStrictLeaderTown(Player player) {
        Town town = query.playerTown(player);
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
        Town town = query.playerTown(player);
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
                + "{\"text\":\"[?뚰떚] \",\"color\":\"#8FD9A8\"},"
                + "{\"text\":\"" + json(partyName) + "\",\"color\":\"yellow\"},"
                + "{\"text\":\" ?뚰떚?먯꽌 珥덈?媛 ?붿뒿?덈떎. \",\"color\":\"white\"},"
                + "{\"text\":\"[?섎씫]\",\"color\":\"#8FD9A8\",\"bold\":true,"
                + "\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + json(acceptCommand) + "\"},"
                + "\"hoverEvent\":{\"action\":\"show_text\",\"contents\":{\"text\":\"" + json(Text.stripColor(acceptHover)) + "\",\"color\":\"white\"}}},"
                + "{\"text\":\" \",\"color\":\"white\"},"
                + "{\"text\":\"[嫄곗젅]\",\"color\":\"#FF9AA2\",\"bold\":true,"
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
}
