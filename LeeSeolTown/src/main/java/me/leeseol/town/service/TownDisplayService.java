package me.leeseol.town.service;

import me.clip.placeholderapi.PlaceholderAPI;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ChatMode;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.Town;
import me.leeseol.town.storage.TownStore;
import me.leeseol.town.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class TownDisplayService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;

    public TownDisplayService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
    }

    public void setChatMode(Player player, ChatMode mode) {
        store.setChatMode(player.getUniqueId(), mode);
        player.sendMessage(plugin.msg("chat-mode").replace("%mode%", mode.displayName()));
    }

    public void sendTownChat(Player player, Component message) {
        Town town = query.playerTown(player);
        if (town == null) {
            player.sendMessage(plugin.msg("chat-no-town"));
            return;
        }
        Component line = chatLine("town-format", player, message);
        broadcastTown(town, line);
    }

    public void sendNationChat(Player player, Component message) {
        Town town = query.playerTown(player);
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
        Town town = query.playerTown(player);
        if (town == null) {
            return "";
        }

        String townPrefix = plugin.getConfig().getString("prefix.town", "&#8FD9A8%town% ")
                .replace("%town%", town.name());
        Nation nation = town.nationId() == null ? null : store.nation(town.nationId());
        if (nation == null) {
            return townPrefix;
        }
        return plugin.getConfig().getString("prefix.nation", "%nation_color%%nation% ")
                .replace("%town%", town.name())
                .replace("%nation%", nation.name())
                .replace("%nation_color%", nation.color().legacyPrefix());
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
            return "&c[愿由ъ옄] ";
        }
        return "";
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

    public void refreshNationClaimMarkers() {
        if (plugin.blueMapNationClaimMarkers() != null) {
            plugin.blueMapNationClaimMarkers().refreshLater();
        }
    }

    public void sendSelfInfo(Player player) {
        Town town = query.playerTown(player);
        Nation nation = query.playerNation(player);

        player.sendMessage(Text.component("&#BEEBFF[?뚯냽 ?뺣낫]"));
        if (town == null) {
            player.sendMessage(Text.component("&7?뚰떚: &f?놁쓬"));
            player.sendMessage(Text.component("&7援??: &f?놁쓬"));
            player.sendMessage(Text.component("&7?곹깭: &f?뚰떚瑜??앹꽦?섍굅??珥덈?瑜?諛쏆븘 媛?낇븷 ???덉뒿?덈떎."));
            return;
        }

        String role = town.isLeader(player.getUniqueId()) ? "leader" : "member";
        player.sendMessage(Text.component("&7?뚰떚: &#8FD9A8" + town.name() + " &8(" + role + ")"));
        player.sendMessage(Text.component("&7?뚰떚 ?몄썝: &f" + town.members().size() + "/" + plugin.partyMaxMembers()));
        player.sendMessage(Text.component("&7?뚯쑀 泥?겕: &f" + town.claims().size()));

        if (nation == null) {
            player.sendMessage(Text.component("&7援??: &f?놁쓬"));
        } else {
            player.sendMessage(Text.component("&7援??: " + nation.color().legacyPrefix() + nation.name()
                    + " &8(" + nation.color().displayName() + ")"));
            int memberCount = query.nationMemberCount(nation);
            player.sendMessage(Text.component("&7援?? ?몄썝: &f" + memberCount));
            player.sendMessage(Text.component("&7移대Ⅴ留? &f" + nation.karma()));
            player.sendMessage(Text.component("&7援?퀬: &e" + plugin.formatMoney(nation.treasury())));
            player.sendMessage(Text.component("&7?쇱씪 援?? ?좎?鍮? &e"
                    + plugin.formatMoney(plugin.townService().dailyNationUpkeep(nation))));
            if (nation.upkeepDebt() > 0.0D) {
                player.sendMessage(Text.component("&7?좎?鍮?泥대궔: &c" + plugin.formatMoney(nation.upkeepDebt())));
            }
            if (nation.debtAmount() > 0.0D) {
                player.sendMessage(Text.component("&7?꾩웳 泥대궔: &c" + plugin.formatMoney(nation.debtAmount())));
            }
            if (nation.functionsSuspended()) {
                player.sendMessage(Text.component("&c援?? 湲곕뒫 ?뺤? ?곹깭"));
            }
        }
        player.sendMessage(Text.component("&7梨꾪똿 紐⑤뱶: &f" + store.chatMode(player.getUniqueId()).displayName()));
    }

    public String info(Town town) {
        Nation nation = town.nationId() == null ? null : store.nation(town.nationId());
        OfflinePlayer leader = Bukkit.getOfflinePlayer(town.leader());
        String nationText = nation == null ? "-" : nation.name()
                + " tax=" + plugin.formatMoney(plugin.townService().dailyNationUpkeep(nation))
                + " karma=" + nation.karma();
        return Text.color("&b" + town.name()
                + " &7leader=&f" + (leader.getName() == null ? town.leader() : leader.getName())
                + " &7members=&f" + town.members().size() + "/" + plugin.partyMaxMembers()
                + " &7claims=&f" + town.claims().size()
                + " &7nation=&f" + nationText);
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

    private void broadcastTown(Town town, Component message) {
        for (Player player : onlineMembers(town)) {
            player.sendMessage(message);
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

    private java.util.List<Player> onlineMembers(Town town) {
        java.util.List<Player> players = new java.util.ArrayList<>();
        for (java.util.UUID uuid : town.members()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }
}
