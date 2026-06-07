package me.leeseol.town.storage;

import me.leeseol.town.model.ChatMode;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.NationType;
import me.leeseol.town.model.Town;
import me.leeseol.town.model.War;
import me.leeseol.town.model.WarStatus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TownStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Town> towns = new LinkedHashMap<>();
    private final Map<String, Nation> nations = new LinkedHashMap<>();
    private final Map<String, War> wars = new LinkedHashMap<>();
    private final Map<UUID, String> playerTownIds = new HashMap<>();
    private final Map<ClaimKey, String> claimTownIds = new HashMap<>();
    private final Map<UUID, ChatMode> chatModes = new HashMap<>();

    public TownStore(JavaPlugin plugin) {
        this.plugin = plugin;
        String dataPath = plugin.getConfig().getString("storage.data-file", "data.yml");
        File configured = new File(dataPath == null || dataPath.isBlank() ? "data.yml" : dataPath);
        this.file = configured.isAbsolute() ? configured : new File(plugin.getDataFolder(), configured.getPath());
    }

    public void load() {
        towns.clear();
        nations.clear();
        wars.clear();
        playerTownIds.clear();
        claimTownIds.clear();

        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        loadTowns(data.getConfigurationSection("towns"));
        loadNations(data.getConfigurationSection("nations"));
        loadWars(data.getConfigurationSection("wars"));
        rebuildIndexes();
        plugin.getLogger().info("LeeSeolTown loaded towns=" + towns.size() + ", nations=" + nations.size() + ", claims=" + claimTownIds.size());
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        saveTowns(data);
        saveNations(data);
        saveWars(data);

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save LeeSeolTown data: " + exception.getMessage());
        }
    }

    private void loadTowns(ConfigurationSection section) {
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            String base = id + ".";
            UUID leader = parseUuid(section.getString(base + "leader"));
            if (leader == null) {
                plugin.getLogger().warning("Skipping town with invalid leader: " + id);
                continue;
            }

            Town town = new Town(id, section.getString(base + "name", id), leader, section.getLong(base + "created-at", System.currentTimeMillis()));
            town.members().clear();
            for (String value : section.getStringList(base + "members")) {
                UUID uuid = parseUuid(value);
                if (uuid != null) {
                    town.members().add(uuid);
                }
            }
            if (!town.members().contains(leader)) {
                town.members().add(leader);
            }

            for (String value : section.getStringList(base + "invites")) {
                UUID uuid = parseUuid(value);
                if (uuid != null) {
                    town.invites().add(uuid);
                }
            }
            for (String value : section.getStringList(base + "invite-names")) {
                if (value != null && !value.isBlank()) {
                    town.inviteNames().add(normalizeName(value));
                }
            }
            for (String value : section.getStringList(base + "claims")) {
                ClaimKey claim = ClaimKey.parse(value);
                if (claim != null) {
                    town.claims().add(claim);
                }
            }
            town.setNationId(blankToNull(section.getString(base + "nation")));
            towns.put(id, town);
        }
    }

    private void loadNations(ConfigurationSection section) {
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            String base = id + ".";
            NationType type = NationType.parse(section.getString(base + "type"));
            if (type == null) {
                plugin.getLogger().warning("Skipping nation with invalid type: " + id);
                continue;
            }

            Nation nation = new Nation(
                    id,
                    section.getString(base + "name", id),
                    type,
                    section.getString(base + "capital-town"),
                    section.getLong(base + "created-at", System.currentTimeMillis())
            );
            nation.townIds().clear();
            nation.townIds().addAll(section.getStringList(base + "towns"));
            if (nation.capitalTownId() != null && !nation.capitalTownId().isBlank()) {
                nation.townIds().add(nation.capitalTownId());
            }
            nation.setBeaconClaim(ClaimKey.parse(section.getString(base + "beacon-claim")));
            nation.setPvpEnabled(section.getBoolean(base + "pvp-enabled", false));
            nation.setBuildProtectionEnabled(section.getBoolean(base + "build-protection-enabled", true));
            nation.setKarma(section.getInt(base + "karma", 0));
            nation.setTreasury(section.getDouble(base + "treasury", 0.0D));
            nation.setSurrenderWinStreak(section.getInt(base + "surrender-win-streak", 0));
            nation.setDebtCreditorNationId(blankToNull(section.getString(base + "debt.creditor")));
            nation.setDebtAmount(section.getDouble(base + "debt.amount", 0.0D));
            nation.setDebtDeadline(section.getLong(base + "debt.deadline", 0L));
            nation.setFunctionsSuspended(section.getBoolean(base + "functions-suspended", false));
            nations.put(id, nation);
        }
    }

    private void loadWars(ConfigurationSection section) {
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            String base = id + ".";
            String attacker = section.getString(base + "attacker");
            String defender = section.getString(base + "defender");
            WarStatus status = parseWarStatus(section.getString(base + "status"));
            if (attacker == null || defender == null || status == null || attacker.isBlank() || defender.isBlank()) {
                plugin.getLogger().warning("Skipping invalid war: " + id);
                continue;
            }
            War war = new War(id, attacker, defender, status, section.getLong(base + "declared-at", System.currentTimeMillis()));
            war.setProtectionUntil(section.getLong(base + "protection-until", 0L));
            war.setDefenderProtectionActive(section.getBoolean(base + "defender-protection-active", false));
            wars.put(id, war);
        }
    }

    private void saveTowns(YamlConfiguration data) {
        for (Town town : towns.values()) {
            String base = "towns." + town.id() + ".";
            data.set(base + "name", town.name());
            data.set(base + "leader", town.leader().toString());
            data.set(base + "created-at", town.createdAt());
            data.set(base + "members", town.members().stream().map(UUID::toString).toList());
            data.set(base + "invites", town.invites().stream().map(UUID::toString).toList());
            data.set(base + "invite-names", new ArrayList<>(town.inviteNames()));
            data.set(base + "claims", town.claims().stream().map(ClaimKey::serialize).toList());
            data.set(base + "nation", town.nationId());
        }
    }

    private void saveNations(YamlConfiguration data) {
        for (Nation nation : nations.values()) {
            String base = "nations." + nation.id() + ".";
            data.set(base + "name", nation.name());
            data.set(base + "type", nation.type().id());
            data.set(base + "capital-town", nation.capitalTownId());
            data.set(base + "created-at", nation.createdAt());
            data.set(base + "towns", new ArrayList<>(nation.townIds()));
            data.set(base + "beacon-claim", nation.beaconClaim() == null ? null : nation.beaconClaim().serialize());
            data.set(base + "pvp-enabled", nation.pvpEnabled());
            data.set(base + "build-protection-enabled", nation.buildProtectionEnabled());
            data.set(base + "karma", nation.karma());
            data.set(base + "treasury", nation.treasury());
            data.set(base + "surrender-win-streak", nation.surrenderWinStreak());
            data.set(base + "debt.creditor", nation.debtCreditorNationId());
            data.set(base + "debt.amount", nation.debtAmount());
            data.set(base + "debt.deadline", nation.debtDeadline());
            data.set(base + "functions-suspended", nation.functionsSuspended());
        }
    }

    private void saveWars(YamlConfiguration data) {
        for (War war : wars.values()) {
            String base = "wars." + war.id() + ".";
            data.set(base + "attacker", war.attackerNationId());
            data.set(base + "defender", war.defenderNationId());
            data.set(base + "status", war.status().name());
            data.set(base + "declared-at", war.declaredAt());
            data.set(base + "protection-until", war.protectionUntil());
            data.set(base + "defender-protection-active", war.defenderProtectionActive());
        }
    }

    public void rebuildIndexes() {
        playerTownIds.clear();
        claimTownIds.clear();
        for (Town town : towns.values()) {
            for (UUID member : town.members()) {
                playerTownIds.put(member, town.id());
            }
            for (ClaimKey claim : town.claims()) {
                claimTownIds.put(claim, town.id());
            }
        }
    }

    public Collection<Town> towns() {
        return towns.values();
    }

    public Collection<Nation> nations() {
        return nations.values();
    }

    public Collection<War> wars() {
        return wars.values();
    }

    public Town town(String id) {
        return towns.get(id);
    }

    public Nation nation(String id) {
        return nations.get(id);
    }

    public Town playerTown(UUID uuid) {
        String townId = playerTownIds.get(uuid);
        return townId == null ? null : towns.get(townId);
    }

    public Town claimTown(ClaimKey claim) {
        String townId = claimTownIds.get(claim);
        return townId == null ? null : towns.get(townId);
    }

    public void addTown(Town town) {
        towns.put(town.id(), town);
        rebuildIndexes();
    }

    public void removeTown(Town town) {
        towns.remove(town.id());
        List<String> emptyNationIds = new ArrayList<>();
        for (Nation nation : nations.values()) {
            nation.townIds().remove(town.id());
            if (town.id().equals(nation.capitalTownId())) {
                nation.setCapitalTownId(nation.townIds().stream().findFirst().orElse(null));
            }
            if (nation.townIds().isEmpty()) {
                emptyNationIds.add(nation.id());
            }
        }
        for (String nationId : emptyNationIds) {
            nations.remove(nationId);
        }
        rebuildIndexes();
    }

    public void addNation(Nation nation) {
        nations.put(nation.id(), nation);
    }

    public void removeNation(Nation nation) {
        nations.remove(nation.id());
        wars.values().removeIf(war -> war.involves(nation.id()));
        for (Town town : towns.values()) {
            if (nation.id().equals(town.nationId())) {
                town.setNationId(null);
            }
        }
    }

    public void addWar(War war) {
        wars.put(war.id(), war);
    }

    public void removeWar(War war) {
        wars.remove(war.id());
    }

    public ChatMode chatMode(UUID uuid) {
        return chatModes.getOrDefault(uuid, ChatMode.GLOBAL);
    }

    public void setChatMode(UUID uuid, ChatMode mode) {
        if (mode == ChatMode.GLOBAL) {
            chatModes.remove(uuid);
        } else {
            chatModes.put(uuid, mode);
        }
    }

    public static String idFromName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase()
                .replaceAll("[^a-z0-9가-힣_\\-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    public static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private WarStatus parseWarStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return WarStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
