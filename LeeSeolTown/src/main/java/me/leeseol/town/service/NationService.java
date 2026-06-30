package me.leeseol.town.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.NationColor;
import me.leeseol.town.model.NationColorPalette;
import me.leeseol.town.model.Town;
import me.leeseol.town.model.War;
import me.leeseol.town.model.WarStatus;
import me.leeseol.town.storage.TownStore;
import me.leeseol.town.structure.StructureDefinition;
import me.leeseol.town.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class NationService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;
    private final TownConfirmationService confirmations;
    private final TownDisplayService display;

    public NationService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query,
                         TownConfirmationService confirmations, TownDisplayService display) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
        this.confirmations = confirmations;
        this.display = display;
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
                player.sendMessage(plugin.msg("party-already-in-nation")
                        .replace("%party%", extra.name())
                        .replace("%town%", extra.name()));
                return true;
            }
            nationParties.add(extra);
        }
        int memberCount = nationParties.stream().mapToInt(candidate -> candidate.members().size()).sum();
        boolean bypassRequirements = player.hasPermission("leeseoltown.admin");
        if (!bypassRequirements && memberCount < plugin.nationRequiredMembers()) {
            player.sendMessage(plugin.msg("nation-member-required")
                    .replace("%count%", String.valueOf(plugin.nationRequiredMembers())));
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
        display.refreshNationClaimMarkers();
        display.updateAllIdentities();
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
        if (!confirmations.confirm(player, "nation", nation.id(), "disband-nation-warning",
                "/party nation disband", nation.name())) {
            return true;
        }

        List<Player> onlineMembers = onlineNationMembers(nation);
        store.removeNation(nation);
        store.save();
        display.refreshNationClaimMarkers();
        display.updateAllIdentities();
        for (Player member : onlineMembers) {
            member.sendMessage(plugin.msg("nation-disbanded").replace("%nation%", nation.name()));
        }
        return true;
    }

    public boolean setNationPvp(Player player, boolean enabled) {
        store.load();
        Town town = query.playerTown(player);
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
        Town town = query.playerTown(player);
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
        Town town = query.playerTown(player);
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
        Town town = query.playerTown(player);
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
        player.sendMessage(Text.component("&7?쇱씪 ?좎?鍮? &e" + plugin.formatMoney(dailyNationUpkeep(nation))
                + " &7| ?좎?鍮?泥대궔: &c" + plugin.formatMoney(nation.upkeepDebt())
                + " &7| ?뺤궛?? &f" + (nation.lastUpkeepPeriod() == null ? "-" : nation.lastUpkeepPeriod())));
        return true;
    }

    public boolean sendNationUpkeep(Player player) {
        store.load();
        processExpiredWarState();
        Town town = query.playerTown(player);
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
        Town town = query.playerTown(player);
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

    public void collectDueUpkeep(boolean force) {
        if (!plugin.upkeepEnabled()) {
            return;
        }
        store.load();
        processExpiredWarState();

        boolean changed = false;
        String period = plugin.currentUpkeepPeriod();
        for (Nation nation : query.nations()) {
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

    public long dailyNationUpkeep(Nation nation) {
        if (nation == null || !plugin.upkeepEnabled()) {
            return 0L;
        }
        long total = safeAdd(plugin.dailyBaseNationUpkeep(), plugin.dailyMemberUpkeep(query.nationMemberCount(nation)));
        List<Long> claimCosts = new ArrayList<>();
        int claimCount = query.nationClaimCount(nation);
        for (ClaimKey claim : query.nationClaims(nation)) {
            claimCosts.add(plugin.dailyChunkUpkeep(claim, claimCount));
        }
        claimCosts.sort(Long::compareTo);
        int freeChunks = plugin.freeUpkeepChunks();
        for (int index = freeChunks; index < claimCosts.size(); index++) {
            total = safeAdd(total, claimCosts.get(index));
        }
        return total;
    }

    public boolean ensureNationActive(Player player, Nation nation) {
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

    public boolean canManageNation(Player player, Town town, Nation nation) {
        return player.hasPermission("leeseoltown.admin")
                || (town != null
                && nation != null
                && town.id().equals(nation.capitalTownId())
                && town.isLeader(player.getUniqueId()));
    }

    public Nation requireNationLeader(Player player, Town town) {
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

    private Set<String> usedNationColorKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (Nation nation : query.nations()) {
            if (nation.color() != null) {
                keys.add(nation.color().key());
            }
        }
        return keys;
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

    private void giveNationBeacon(Player player) {
        StructureDefinition definition = plugin.structureRegistry().get("nation_core");
        ItemStack core = definition == null ? null : plugin.structureCoreItemService().createCoreItem(definition, 1);
        if (core == null) {
            player.sendMessage(plugin.msg("structure-missing-itemsadder-block")
                    .replace("%structure%", "leeseolwar:capital_core"));
            return;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(core);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.sendMessage(plugin.msg("nation-beacon-given"));
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

        for (Nation nation : query.nations()) {
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
