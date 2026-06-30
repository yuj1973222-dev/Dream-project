package me.leeseol.town.service;

import java.util.ArrayList;
import java.util.List;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.Town;
import me.leeseol.town.model.War;
import me.leeseol.town.model.WarMode;
import me.leeseol.town.model.WarStatus;
import me.leeseol.town.storage.TownStore;
import me.leeseol.town.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class WarService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;
    private final NationService nations;
    private final TownDisplayService display;

    public WarService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query,
                      NationService nations, TownDisplayService display) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
        this.nations = nations;
        this.display = display;
    }

    public boolean declareWar(Player player, String targetNationName) {
        return declareWar(player, targetNationName, WarMode.INVASION);
    }

    public boolean declareWar(Player player, String targetNationName, WarMode mode) {
        store.load();
        processExpiredWarState();
        WarMode warMode = mode == null ? WarMode.INVASION : mode;
        Town town = query.playerTown(player);
        Nation attacker = nations.requireNationLeader(player, town);
        if (attacker == null || !nations.ensureNationActive(player, attacker)) {
            return true;
        }
        Nation defender = query.nationByName(targetNationName);
        if (defender == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        if (attacker.id().equals(defender.id())) {
            player.sendMessage(plugin.msg("war-self"));
            return true;
        }
        if (query.findWarBetween(attacker.id(), defender.id()) != null) {
            player.sendMessage(plugin.msg("war-already-exists"));
            return true;
        }

        War war = new War(War.id(attacker.id(), defender.id()), attacker.id(), defender.id(), warMode,
                WarStatus.PENDING, System.currentTimeMillis());
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
        Town town = query.playerTown(player);
        Nation defender = nations.requireNationLeader(player, town);
        if (defender == null || !nations.ensureNationActive(player, defender)) {
            return true;
        }
        Nation attacker = query.nationByName(attackerNationName);
        if (attacker == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        War war = query.findPendingWar(attacker.id(), defender.id());
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
        Town town = query.playerTown(player);
        Nation surrenderer = nations.requireNationLeader(player, town);
        if (surrenderer == null) {
            return true;
        }
        if (!nations.ensureNationActive(player, surrenderer)) {
            return true;
        }
        Nation winner = query.nationByName(enemyNationName);
        if (winner == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        War war = query.findWarBetween(surrenderer.id(), winner.id());
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
        Town town = query.playerTown(player);
        Nation defender = nations.requireNationLeader(player, town);
        if (defender == null) {
            return true;
        }
        Nation attacker = query.nationByName(enemyNationName);
        if (attacker == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        War war = query.findWarBetween(attacker.id(), defender.id());
        if (war == null || war.status() != WarStatus.ACTIVE || !war.defenderNationId().equals(defender.id())
                || !war.defenderProtectionActive()) {
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
        Town town = query.playerTown(player);
        Nation nation = nations.requireNationLeader(player, town);
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
        Nation winner = query.nationByName(winnerName);
        Nation loser = query.nationByName(loserName);
        if (winner == null || loser == null) {
            player.sendMessage(plugin.msg("nation-not-found"));
            return true;
        }
        War war = query.findWarBetween(winner.id(), loser.id());
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

    public boolean canPvp(Player attacker, Player victim) {
        Nation nation = query.nationForClaim(ClaimKey.from(victim.getLocation()));
        if (nation == null) {
            nation = query.nationForClaim(ClaimKey.from(attacker.getLocation()));
        }
        return nation == null || nation.functionsSuspended() || nation.pvpEnabled();
    }

    public void processExpiredWarState() {
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

        for (Nation nation : query.nations()) {
            if (nation.debtAmount() <= 0.0D || nation.debtDeadline() <= 0L || now < nation.debtDeadline()
                    || nation.functionsSuspended()) {
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

    private List<Player> onlineMembers(Town town) {
        List<Player> players = new ArrayList<>();
        for (java.util.UUID uuid : town.members()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    private void broadcastNation(Nation nation, Component message) {
        for (String townId : nation.townIds()) {
            Town town = store.town(townId);
            if (town != null) {
                for (Player player : onlineMembers(town)) {
                    player.sendMessage(message);
                }
            }
        }
    }
}
