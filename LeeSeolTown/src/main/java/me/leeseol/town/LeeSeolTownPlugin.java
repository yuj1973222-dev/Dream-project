package me.leeseol.town;

import me.leeseol.town.command.ChannelChatCommand;
import me.leeseol.town.command.TownCommand;
import me.leeseol.town.hook.TownPlaceholderExpansion;
import me.leeseol.town.hook.VaultEconomyHook;
import me.leeseol.town.listener.ClaimProtectionListener;
import me.leeseol.town.listener.IdentityListener;
import me.leeseol.town.listener.NationRuleListener;
import me.leeseol.town.listener.NeutralZoneListener;
import me.leeseol.town.listener.StructurePlacementListener;
import me.leeseol.town.listener.TownChatListener;
import me.leeseol.town.model.ChatMode;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.service.BlueMapNationClaimMarkers;
import me.leeseol.town.service.BlueMapNeutralZoneMarkers;
import me.leeseol.town.service.NeutralZoneManager;
import me.leeseol.town.service.TownScoreboardService;
import me.leeseol.town.service.TownService;
import me.leeseol.town.service.WorldGuardNeutralZoneRegions;
import me.leeseol.town.storage.TownStore;
import me.leeseol.town.structure.StructureCoreItemService;
import me.leeseol.town.structure.StructureRegistry;
import me.leeseol.town.structure.StructureSelectionGui;
import me.leeseol.town.structure.StructureUndoService;
import me.leeseol.town.structure.WorldEditStructurePaster;
import me.leeseol.town.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class LeeSeolTownPlugin extends JavaPlugin {
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private TownStore store;
    private TownService townService;
    private NeutralZoneManager neutralZoneManager;
    private BlueMapNeutralZoneMarkers blueMapNeutralZoneMarkers;
    private BlueMapNationClaimMarkers blueMapNationClaimMarkers;
    private WorldGuardNeutralZoneRegions worldGuardNeutralZoneRegions;
    private TownScoreboardService scoreboardService;
    private StructureRegistry structureRegistry;
    private StructureCoreItemService structureCoreItemService;
    private StructureSelectionGui structureSelectionGui;
    private StructureUndoService structureUndoService;
    private WorldEditStructurePaster worldEditStructurePaster;
    private VaultEconomyHook economy;
    private TownPlaceholderExpansion placeholderExpansion;
    private BukkitTask upkeepTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        createServices();
        registerListeners();
        registerCommands();
        registerPlaceholderExpansion();
        scheduleUpkeepTask();
        syncLegacyNeutralWorldGuardRegions();
        blueMapNeutralZoneMarkers.refreshLater();
        blueMapNationClaimMarkers.refreshLater();
        scoreboardService.start();
        getLogger().info("LeeSeolTown enabled. towns=" + townService.towns().size() + ", nations=" + townService.nations().size());
    }

    @Override
    public void onDisable() {
        if (upkeepTask != null) {
            upkeepTask.cancel();
            upkeepTask = null;
        }
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        if (store != null) {
            store.save();
        }
        if (neutralZoneManager != null) {
            neutralZoneManager.save();
        }
        if (blueMapNeutralZoneMarkers != null) {
            blueMapNeutralZoneMarkers.clear();
        }
        if (blueMapNationClaimMarkers != null) {
            blueMapNationClaimMarkers.clear();
        }
        if (scoreboardService != null) {
            scoreboardService.stop();
        }
    }

    public void reloadAll() {
        reloadConfig();
        structureRegistry = StructureRegistry.from(getConfig());
        economy.reload();
        neutralZoneManager.reload();
        syncLegacyNeutralWorldGuardRegions();
        townService.reload();
        blueMapNeutralZoneMarkers.refreshLater();
        blueMapNationClaimMarkers.refreshLater();
        scheduleUpkeepTask();
        scoreboardService.start();
    }

    private void createServices() {
        this.store = new TownStore(this);
        this.economy = new VaultEconomyHook(this);
        this.neutralZoneManager = new NeutralZoneManager(this);
        this.neutralZoneManager.reload();
        this.worldGuardNeutralZoneRegions = new WorldGuardNeutralZoneRegions(this);
        this.blueMapNeutralZoneMarkers = new BlueMapNeutralZoneMarkers(this, neutralZoneManager);
        this.townService = new TownService(this, store);
        this.townService.reload();
        this.blueMapNationClaimMarkers = new BlueMapNationClaimMarkers(this);
        this.scoreboardService = new TownScoreboardService(this);
        this.structureRegistry = StructureRegistry.from(getConfig());
        this.structureCoreItemService = new StructureCoreItemService(this);
        this.structureSelectionGui = new StructureSelectionGui(this);
        this.structureUndoService = new StructureUndoService(this);
        this.worldEditStructurePaster = new WorldEditStructurePaster(this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new IdentityListener(townService), this);
        getServer().getPluginManager().registerEvents(new ClaimProtectionListener(this, townService), this);
        getServer().getPluginManager().registerEvents(new NeutralZoneListener(this, neutralZoneManager), this);
        getServer().getPluginManager().registerEvents(new NationRuleListener(this, townService), this);
        getServer().getPluginManager().registerEvents(new StructurePlacementListener(this, structureCoreItemService, structureSelectionGui, worldEditStructurePaster), this);
        getServer().getPluginManager().registerEvents(new TownChatListener(this, townService), this);
    }

    private void registerCommands() {
        TownCommand townCommand = new TownCommand(this, townService);
        register("town", townCommand);
        register("tc", new ChannelChatCommand(townService, ChatMode.TOWN));
        register("nc", new ChannelChatCommand(townService, ChatMode.NATION));
    }

    private void register(String commandName, Object executor) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().warning("Command is missing from plugin.yml: " + commandName);
            return;
        }
        if (executor instanceof org.bukkit.command.CommandExecutor commandExecutor) {
            command.setExecutor(commandExecutor);
        }
        if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("PlaceholderAPI not found. Town placeholders are disabled.");
            return;
        }
        placeholderExpansion = new TownPlaceholderExpansion(this);
        placeholderExpansion.register();
        getLogger().info("Registered PlaceholderAPI placeholders: %leeseoltown_rank%, %leeseoltown_affiliation%, %leeseoltown_has_party%, %leeseoltown_party%, %leeseoltown_town%, %leeseoltown_nation%, %leeseoltown_nation_color%, %leeseoltown_nation_color_hex%");
    }

    public String msg(String key) {
        return Text.color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages." + key, ""));
    }

    public String msgRaw(String key) {
        return Text.color(getConfig().getString("messages." + key, ""));
    }

    public int minTownMembers() {
        return getConfig().getInt("settings.min-town-members", 2);
    }

    public int partyMaxMembers() {
        return getConfig().getInt("settings.party-max-members", 4);
    }

    public int nationRequiredMembers() {
        return getConfig().getInt("settings.nation-required-members", 5);
    }

    public boolean economyEnabled() {
        return getConfig().getBoolean("economy.enabled", true);
    }

    public double chunkClaimCost() {
        return getConfig().getDouble("economy.chunk-claim-cost", 10000.0D);
    }

    public long chunkClaimCost(ClaimKey claim, Nation nation, int nextClaimCount) {
        double value = chunkClaimCost() * claimCountMultiplier(nextClaimCount) * claimCostZoneMultiplier(claim);
        return clampMoney(value);
    }

    public long dailyBaseNationUpkeep() {
        return Math.max(0L, getConfig().getLong("economy.upkeep.base-nation-fee", 15000L));
    }

    public long dailyMemberUpkeep(int memberCount) {
        return Math.max(0L, Math.round(Math.max(0, memberCount) * getConfig().getDouble("economy.upkeep.member-fee", 1500.0D)));
    }

    public long dailyChunkUpkeep(ClaimKey claim, int totalClaimCount) {
        double base = getConfig().getDouble("economy.upkeep.base-per-chunk", 1200.0D);
        double value = base * upkeepClaimCountMultiplier(totalClaimCount) * upkeepZoneMultiplier(claim);
        return clampMoney(value);
    }

    public int freeUpkeepChunks() {
        return Math.max(0, getConfig().getInt("economy.upkeep.free-chunks", 6));
    }

    public boolean upkeepEnabled() {
        return economyEnabled() && getConfig().getBoolean("economy.upkeep.enabled", true);
    }

    public int upkeepGraceDays() {
        return Math.max(0, getConfig().getInt("economy.upkeep.grace-days", 3));
    }

    public String currentUpkeepPeriod() {
        return LocalDate.now(economyZone()).format(PERIOD_FORMAT);
    }

    public String claimZoneName(ClaimKey claim) {
        ConfigurationSection root = getConfig().getConfigurationSection("economy.premium-zones");
        if (root == null) {
            return "일반 청크";
        }
        String selected = "일반 청크";
        double selectedMultiplier = 1.0D;
        for (String key : root.getKeys(false)) {
            ConfigurationSection zone = root.getConfigurationSection(key);
            if (zone == null || !claimInZone(claim, zone)) {
                continue;
            }
            double multiplier = Math.max(zone.getDouble("claim-cost-multiplier", 1.0D), zone.getDouble("upkeep-multiplier", 1.0D));
            if (multiplier >= selectedMultiplier) {
                selectedMultiplier = multiplier;
                selected = zone.getString("name", key);
            }
        }
        return selected;
    }

    public long nationTax(int memberCount) {
        int requiredMembers = Math.max(1, nationRequiredMembers());
        double base = getConfig().getDouble("economy.nation-tax-base", 10000.0D);
        double memberFee = getConfig().getDouble("economy.nation-member-tax", 1500.0D);
        return clampMoney(base + Math.max(0, memberCount - requiredMembers) * memberFee);
    }

    public long nationTax(int memberCount, int karma) {
        long baseTax = nationTax(memberCount);
        if (karma >= 0 || baseTax == Long.MAX_VALUE) {
            return baseTax;
        }
        double value = baseTax * (1.0D + Math.abs(karma) / 100.0D);
        if (!Double.isFinite(value) || value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.round(value);
    }

    public double warSurrenderPayment() {
        return getConfig().getDouble("war.surrender-payment", 100000.0D);
    }

    public long warProtectionMillis() {
        return Math.max(1L, getConfig().getLong("war.protection-minutes", 10L)) * 60_000L;
    }

    public long warDebtMillis() {
        return Math.max(1L, getConfig().getLong("war.debt-hours", 12L)) * 3_600_000L;
    }

    public String formatMoney(double amount) {
        return economy.available() ? economy.format(amount) : String.format("%,.0f원", amount);
    }

    public VaultEconomyHook economy() {
        return economy;
    }

    public TownService townService() {
        return townService;
    }

    public NeutralZoneManager neutralZones() {
        return neutralZoneManager;
    }

    public TownScoreboardService scoreboardService() {
        return scoreboardService;
    }

    public StructureRegistry structureRegistry() {
        return structureRegistry;
    }

    public StructureCoreItemService structureCoreItemService() {
        return structureCoreItemService;
    }

    public StructureUndoService structureUndoService() {
        return structureUndoService;
    }

    public BlueMapNeutralZoneMarkers blueMapNeutralZoneMarkers() {
        return blueMapNeutralZoneMarkers;
    }

    public BlueMapNationClaimMarkers blueMapNationClaimMarkers() {
        return blueMapNationClaimMarkers;
    }

    public WorldGuardNeutralZoneRegions worldGuardNeutralZoneRegions() {
        return worldGuardNeutralZoneRegions;
    }

    private void scheduleUpkeepTask() {
        if (upkeepTask != null) {
            upkeepTask.cancel();
            upkeepTask = null;
        }
        if (!upkeepEnabled()) {
            return;
        }
        long intervalMinutes = Math.max(5L, getConfig().getLong("economy.upkeep.check-interval-minutes", 60L));
        long intervalTicks = intervalMinutes * 60L * 20L;
        upkeepTask = getServer().getScheduler().runTaskTimer(this, () -> townService.collectDueUpkeep(false), 20L * 30L, intervalTicks);
    }

    private void syncLegacyNeutralWorldGuardRegions() {
        if (!neutralZoneManager.usingCoreContent()) {
            worldGuardNeutralZoneRegions.syncAll(neutralZoneManager.zones());
        }
    }

    private double claimCountMultiplier(int claimCount) {
        int freeClaims = Math.max(0, getConfig().getInt("economy.claim-scaling.free-claims", 4));
        double step = Math.max(1.0D, getConfig().getDouble("economy.claim-scaling.step-claims", 8.0D));
        double rate = Math.max(0.0D, getConfig().getDouble("economy.claim-scaling.rate", 0.08D));
        double max = Math.max(1.0D, getConfig().getDouble("economy.claim-scaling.max-multiplier", 2.5D));
        double multiplier = 1.0D + Math.max(0, claimCount - freeClaims) / step * rate;
        return Math.min(max, multiplier);
    }

    private double upkeepClaimCountMultiplier(int claimCount) {
        int freeClaims = Math.max(0, freeUpkeepChunks());
        double step = Math.max(1.0D, getConfig().getDouble("economy.upkeep.claim-scale-step", 8.0D));
        double rate = Math.max(0.0D, getConfig().getDouble("economy.upkeep.claim-scale-rate", 0.12D));
        double max = Math.max(1.0D, getConfig().getDouble("economy.upkeep.max-claim-multiplier", 3.0D));
        double multiplier = 1.0D + Math.max(0, claimCount - freeClaims) / step * rate;
        return Math.min(max, multiplier);
    }

    private double claimCostZoneMultiplier(ClaimKey claim) {
        return zoneMultiplier(claim, "claim-cost-multiplier");
    }

    private double upkeepZoneMultiplier(ClaimKey claim) {
        return zoneMultiplier(claim, "upkeep-multiplier");
    }

    private double zoneMultiplier(ClaimKey claim, String key) {
        ConfigurationSection root = getConfig().getConfigurationSection("economy.premium-zones");
        if (root == null) {
            return 1.0D;
        }
        double multiplier = 1.0D;
        for (String zoneKey : root.getKeys(false)) {
            ConfigurationSection zone = root.getConfigurationSection(zoneKey);
            if (zone != null && claimInZone(claim, zone)) {
                multiplier = Math.max(multiplier, zone.getDouble(key, 1.0D));
            }
        }
        return Math.max(1.0D, multiplier);
    }

    private boolean claimInZone(ClaimKey claim, ConfigurationSection zone) {
        String world = zone.getString("world", "");
        if (!world.isBlank() && !world.equalsIgnoreCase(claim.world())) {
            return false;
        }
        int minX = Math.min(zone.getInt("min-x", Integer.MIN_VALUE), zone.getInt("max-x", Integer.MAX_VALUE));
        int maxX = Math.max(zone.getInt("min-x", Integer.MIN_VALUE), zone.getInt("max-x", Integer.MAX_VALUE));
        int minZ = Math.min(zone.getInt("min-z", Integer.MIN_VALUE), zone.getInt("max-z", Integer.MAX_VALUE));
        int maxZ = Math.max(zone.getInt("min-z", Integer.MIN_VALUE), zone.getInt("max-z", Integer.MAX_VALUE));
        return claim.x() >= minX && claim.x() <= maxX && claim.z() >= minZ && claim.z() <= maxZ;
    }

    private ZoneId economyZone() {
        String zoneRaw = getConfig().getString("economy.time-zone", "Asia/Seoul");
        try {
            return ZoneId.of(zoneRaw);
        } catch (Exception exception) {
            return ZoneId.of("Asia/Seoul");
        }
    }

    private long clampMoney(double value) {
        if (!Double.isFinite(value) || value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, Math.round(value));
    }
}
