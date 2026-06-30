package me.leeseol.economy.market;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MarketManager {
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LeeSeolEconomyPlugin plugin;
    private final Map<String, MarketOffer> offersById = new LinkedHashMap<>();
    private final Map<Material, MarketOffer> offersByMaterial = new LinkedHashMap<>();

    private Path dataPath;
    private YamlConfiguration data;

    public MarketManager(LeeSeolEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void reload() {
        offersById.clear();
        offersByMaterial.clear();
        loadOffers();
        loadData();
        ensureCurrentPeriod();
    }

    public synchronized void save() {
        saveData();
    }

    public boolean enabledHere() {
        if (!plugin.featureEnabled("market")) {
            return false;
        }
        List<String> allowed = plugin.getConfig().getStringList("market.allowed-local-servers");
        if (allowed.isEmpty()) {
            return true;
        }
        String local = plugin.getConfig().getString("server-menu.local-server", "");
        return allowed.stream().anyMatch(entry -> entry.equalsIgnoreCase(local));
    }

    public Collection<MarketOffer> offers() {
        return List.copyOf(offersById.values());
    }

    public List<String> offerIds() {
        return new ArrayList<>(offersById.keySet());
    }

    public List<String> categories() {
        return offersById.values().stream()
            .map(MarketOffer::category)
            .distinct()
            .sorted()
            .toList();
    }

    public MarketOffer offerById(String id) {
        if (id == null) {
            return null;
        }
        MarketOffer offer = offersById.get(id.toLowerCase(Locale.ROOT));
        if (offer != null) {
            return offer;
        }
        Material material = Material.matchMaterial(id);
        return material == null ? null : offersByMaterial.get(material);
    }

    public MarketOffer offerByMaterial(Material material) {
        return offersByMaterial.get(material);
    }

    public synchronized long currentUnitPrice(MarketOffer offer, UUID playerId) {
        ensureCurrentPeriod();
        if (!activeToday(offer)) {
            return 0L;
        }
        long remainingBudget = remainingBudget(offer.category());
        if (remainingBudget <= 0L) {
            return 0L;
        }
        long price = calculateUnitPrice(
            offer,
            itemSold(offer.id()),
            rollingSold(offer.id()),
            playerId == null ? 0L : playerSold(playerId, offer.id()),
            stock(offer.id()),
            categorySpent(offer.category()),
            totalSpent()
        );
        return price > remainingBudget ? 0L : price;
    }

    public synchronized MarketSaleResult sell(Player player, MarketOffer offer, int requestedAmount) {
        ensureCurrentPeriod();
        if (!activeToday(offer)) {
            return new MarketSaleResult(offer, requestedAmount, 0, 0L, 0L, "inactive");
        }
        int available = countPlainItems(player.getInventory(), offer.material());
        int requested = Math.max(0, Math.min(requestedAmount, available));
        if (requested <= 0) {
            return new MarketSaleResult(offer, requestedAmount, 0, 0L, currentUnitPrice(offer, player.getUniqueId()), "items");
        }

        long dailySold = itemSold(offer.id());
        long rollingSold = rollingSold(offer.id());
        long personalSold = playerSold(player.getUniqueId(), offer.id());
        long stock = stock(offer.id());
        long categorySpent = categorySpent(offer.category());
        long totalSpent = totalSpent();
        int accepted = 0;
        long payout = 0L;

        for (int index = 0; index < requested; index++) {
            long remainingBudget = remainingBudget(offer.category(), categorySpent, totalSpent);
            if (remainingBudget <= 0L) {
                break;
            }
            long unitPrice = calculateUnitPrice(offer, dailySold, rollingSold, personalSold, stock, categorySpent, totalSpent);
            if (unitPrice <= 0L || unitPrice > remainingBudget) {
                break;
            }
            accepted++;
            payout = safeAdd(payout, unitPrice);
            dailySold++;
            rollingSold++;
            personalSold++;
            stock++;
            categorySpent = safeAdd(categorySpent, unitPrice);
            totalSpent = safeAdd(totalSpent, unitPrice);
        }

        if (accepted <= 0 || payout <= 0L) {
            return new MarketSaleResult(offer, requestedAmount, 0, 0L, currentUnitPrice(offer, player.getUniqueId()), "budget");
        }

        int removed = removePlainItems(player.getInventory(), offer.material(), accepted);
        if (removed != accepted) {
            return new MarketSaleResult(offer, requestedAmount, 0, 0L, currentUnitPrice(offer, player.getUniqueId()), "items");
        }

        plugin.balanceStore().deposit(player.getUniqueId(), payout);
        plugin.ledger().recordIssued("market", payout);
        data.set("items." + offer.id() + ".sold", itemSold(offer.id()) + accepted);
        data.set("stock." + offer.id(), stock(offer.id()) + accepted);
        data.set("players." + player.getUniqueId() + ".name", player.getName());
        data.set("players." + player.getUniqueId() + ".items." + offer.id() + ".sold", playerSold(player.getUniqueId(), offer.id()) + accepted);
        data.set("spent.total", totalSpent() + payout);
        data.set("spent.categories." + offer.category(), categorySpent(offer.category()) + payout);
        saveData();

        return new MarketSaleResult(offer, requestedAmount, accepted, payout, currentUnitPrice(offer, player.getUniqueId()), null);
    }

    public long dailyBudget() {
        return Math.max(0L, plugin.getConfig().getLong("market.bank.daily-total-budget", 0L));
    }

    public long totalSpent() {
        return data == null ? 0L : Math.max(0L, data.getLong("spent.total", 0L));
    }

    public long remainingTotalBudget() {
        return Math.max(0L, dailyBudget() - totalSpent());
    }

    public long categoryBudget(String category) {
        return Math.max(0L, plugin.getConfig().getLong("market.categories." + category + ".daily-budget", dailyBudget()));
    }

    public long categorySpent(String category) {
        return data == null ? 0L : Math.max(0L, data.getLong("spent.categories." + category, 0L));
    }

    public long remainingCategoryBudget(String category) {
        return Math.max(0L, categoryBudget(category) - categorySpent(category));
    }

    public String currentPeriod() {
        ensureCurrentPeriod();
        return data.getString("period", today());
    }

    public long itemSold(String offerId) {
        return data == null ? 0L : Math.max(0L, data.getLong("items." + offerId + ".sold", 0L));
    }

    public long rollingSold(String offerId) {
        ensureCurrentPeriod();
        long total = itemSold(offerId);
        LocalDate current = parsePeriod(data.getString("period", today()));
        for (int offset = 1; offset < rollingDays(); offset++) {
            String period = current.minusDays(offset).format(PERIOD_FORMAT);
            total = safeAdd(total, Math.max(0L, data.getLong("history.days." + period + ".items." + offerId + ".sold", 0L)));
        }
        return total;
    }

    public long stock(String offerId) {
        return data == null ? 0L : Math.max(0L, data.getLong("stock." + offerId, 0L));
    }

    public long playerSold(UUID playerId, String offerId) {
        return data == null ? 0L : Math.max(0L, data.getLong("players." + playerId + ".items." + offerId + ".sold", 0L));
    }

    public List<MarketOffer> offersByCategory(String category) {
        return offersById.values().stream()
            .filter(offer -> category == null || category.isBlank() || offer.category().equalsIgnoreCase(category))
            .sorted(Comparator.comparing(MarketOffer::category).thenComparing(MarketOffer::id))
            .toList();
    }

    public List<MarketOffer> activeOffersByCategory(String category) {
        return offersByCategory(category).stream()
            .filter(this::activeToday)
            .toList();
    }

    public boolean activeToday(MarketOffer offer) {
        if (!plugin.getConfig().getBoolean("market.rotation.enabled", true)) {
            return true;
        }
        List<MarketOffer> categoryOffers = offersByCategory(offer.category());
        if (categoryOffers.isEmpty()) {
            return false;
        }
        int limit = activeOfferLimit(offer.category());
        if (limit >= categoryOffers.size()) {
            return true;
        }
        int index = categoryOffers.indexOf(offer);
        if (index < 0) {
            return false;
        }
        String seed = currentPeriod() + "|" + offer.category();
        int start = Math.floorMod(seed.hashCode(), categoryOffers.size());
        return Math.floorMod(index - start, categoryOffers.size()) < limit;
    }

    public int rollingDays() {
        return Math.max(1, plugin.getConfig().getInt("market.memory.rolling-days", 7));
    }

    private void loadOffers() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("market.offers");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            MarketOffer offer = MarketOffer.fromConfig(id, section);
            if (offer == null) {
                plugin.getLogger().warning("Skipping invalid market offer: " + id);
                continue;
            }
            offersById.put(offer.id(), offer);
            offersByMaterial.putIfAbsent(offer.material(), offer);
        }
    }

    private void loadData() {
        String configured = plugin.getConfig().getString("storage.market-file");
        dataPath = configured == null || configured.isBlank()
            ? plugin.getDataFolder().toPath().resolve("market.yml")
            : Path.of(configured);
        File file = dataPath.toFile();
        data = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    private void ensureCurrentPeriod() {
        if (data == null) {
            loadData();
        }
        String today = today();
        String current = data.getString("period", "");
        if (!today.equals(current)) {
            YamlConfiguration next = new YamlConfiguration();
            next.set("period", today);
            copySection(data, next, "history", "history");
            if (!current.isBlank()) {
                copySection(data, next, "items", "history.days." + current + ".items");
                copySection(data, next, "spent", "history.days." + current + ".spent");
                copyDecayedStock(data, next, current, today);
            } else {
                copySection(data, next, "stock", "stock");
            }
            data = next;
            pruneHistory();
            saveData();
        }
    }

    private String today() {
        return todayDate().format(PERIOD_FORMAT);
    }

    private LocalDate todayDate() {
        String zoneRaw = plugin.getConfig().getString("market.reset-time-zone", "Asia/Seoul");
        ZoneId zone;
        try {
            zone = ZoneId.of(zoneRaw);
        } catch (Exception exception) {
            zone = ZoneId.of("Asia/Seoul");
        }
        return LocalDate.now(zone);
    }

    private long remainingBudget(String category) {
        return remainingBudget(category, categorySpent(category), totalSpent());
    }

    private long remainingBudget(String category, long categorySpent, long totalSpent) {
        long totalRemaining = Math.max(0L, dailyBudget() - totalSpent);
        long categoryRemaining = Math.max(0L, categoryBudget(category) - categorySpent);
        return Math.min(totalRemaining, categoryRemaining);
    }

    private long calculateUnitPrice(
        MarketOffer offer,
        long dailySold,
        long rollingSold,
        long personalSold,
        long stock,
        long categorySpent,
        long totalSpent
    ) {
        if (dailyBudget() <= totalSpent || categoryBudget(offer.category()) <= categorySpent) {
            return 0L;
        }
        double dailyPressure = clamp((double) dailySold / offer.dailyTargetAmount());
        double rollingPressure = clamp((double) rollingSold / (offer.dailyTargetAmount() * (double) rollingDays()));
        double personalPressure = clamp((double) personalSold / offer.personalSoftAmount());
        double stockPressure = clamp((double) stock / stockTargetAmount(offer));
        double budgetPressure = Math.max(
            dailyBudget() <= 0L ? 1D : clamp((double) totalSpent / dailyBudget()),
            categoryBudget(offer.category()) <= 0L ? 1D : clamp((double) categorySpent / categoryBudget(offer.category()))
        );

        double globalDiscount = clamp(plugin.getConfig().getDouble("market.pricing.global-sold-discount-max", 0.45D));
        double rollingDiscount = clamp(plugin.getConfig().getDouble("market.pricing.rolling-sold-discount-max", 0.25D));
        double personalDiscount = clamp(plugin.getConfig().getDouble("market.pricing.personal-sold-discount-max", 0.35D));
        double stockDiscount = clamp(plugin.getConfig().getDouble("market.pricing.stock-discount-max", 0.30D));
        double budgetDiscount = clamp(plugin.getConfig().getDouble("market.pricing.budget-discount-max", 0.40D));

        double multiplier = (1D - globalDiscount * dailyPressure)
            * (1D - rollingDiscount * rollingPressure)
            * (1D - personalDiscount * personalPressure)
            * (1D - stockDiscount * stockPressure)
            * (1D - budgetDiscount * budgetPressure);
        long price = Math.round(offer.basePrice() * Math.max(0D, multiplier));
        return Math.max(offer.minPrice(), Math.min(offer.basePrice(), price));
    }

    private int activeOfferLimit(String category) {
        int categoryLimit = plugin.getConfig().getInt("market.categories." + category + ".active-offers", -1);
        if (categoryLimit >= 0) {
            return categoryLimit == 0 ? Integer.MAX_VALUE : categoryLimit;
        }
        int defaultLimit = plugin.getConfig().getInt("market.rotation.default-active-offers-per-category", Integer.MAX_VALUE);
        return defaultLimit <= 0 ? Integer.MAX_VALUE : defaultLimit;
    }

    private long stockTargetAmount(MarketOffer offer) {
        long defaultTarget = Math.max(1L, Math.round(offer.dailyTargetAmount() * plugin.getConfig().getDouble("market.stock.target-days", 3D)));
        return Math.max(1L, plugin.getConfig().getLong("market.offers." + offer.id() + ".stock-target-amount", defaultTarget));
    }

    private void copyDecayedStock(YamlConfiguration source, YamlConfiguration target, String currentPeriod, String nextPeriod) {
        ConfigurationSection stockRoot = source.getConfigurationSection("stock");
        if (stockRoot == null) {
            return;
        }
        long elapsedDays = Math.max(1L, ChronoUnit.DAYS.between(parsePeriod(currentPeriod), parsePeriod(nextPeriod)));
        double keepMultiplier = Math.pow(1D - stockDecayRate(), elapsedDays);
        for (String key : stockRoot.getKeys(false)) {
            long stock = Math.max(0L, stockRoot.getLong(key, 0L));
            long decayed = Math.max(0L, Math.round(stock * keepMultiplier));
            if (decayed > 0L) {
                target.set("stock." + key, decayed);
            }
        }
    }

    private double stockDecayRate() {
        return clamp(plugin.getConfig().getDouble("market.stock.daily-decay-rate", 0.18D));
    }

    private void pruneHistory() {
        ConfigurationSection days = data.getConfigurationSection("history.days");
        if (days == null) {
            return;
        }
        int keepDays = Math.max(rollingDays(), plugin.getConfig().getInt("market.memory.keep-days", 14));
        LocalDate cutoff = todayDate().minusDays(keepDays);
        for (String period : days.getKeys(false)) {
            LocalDate date = parsePeriod(period);
            if (date.isBefore(cutoff)) {
                data.set("history.days." + period, null);
            }
        }
    }

    private void copySection(YamlConfiguration source, YamlConfiguration target, String sourcePath, String targetPath) {
        ConfigurationSection section = source.getConfigurationSection(sourcePath);
        if (section == null) {
            return;
        }
        copySection(section, target, targetPath);
    }

    private void copySection(ConfigurationSection source, YamlConfiguration target, String targetPath) {
        for (String key : source.getKeys(false)) {
            ConfigurationSection child = source.getConfigurationSection(key);
            if (child != null) {
                copySection(child, target, targetPath + "." + key);
            } else {
                target.set(targetPath + "." + key, source.get(key));
            }
        }
    }

    private LocalDate parsePeriod(String period) {
        try {
            return LocalDate.parse(period, PERIOD_FORMAT);
        } catch (Exception exception) {
            return todayDate();
        }
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || value < 0D) {
            return 0D;
        }
        return Math.min(1D, value);
    }

    private int countPlainItems(PlayerInventory inventory, Material material) {
        int count = 0;
        for (ItemStack item : inventory.getStorageContents()) {
            if (isPlainMaterial(item, material)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private int removePlainItems(PlayerInventory inventory, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();
        for (int index = 0; index < contents.length && remaining > 0; index++) {
            ItemStack item = contents[index];
            if (!isPlainMaterial(item, material)) {
                continue;
            }
            int remove = Math.min(remaining, item.getAmount());
            remaining -= remove;
            int left = item.getAmount() - remove;
            if (left <= 0) {
                contents[index] = null;
            } else {
                item.setAmount(left);
                contents[index] = item;
            }
        }
        inventory.setStorageContents(contents);
        return amount - remaining;
    }

    private boolean isPlainMaterial(ItemStack item, Material material) {
        return item != null && item.getType() == material && !item.hasItemMeta();
    }

    private void saveData() {
        if (dataPath == null || data == null) {
            return;
        }
        try {
            Path parent = dataPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            data.save(dataPath.toFile());
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save market data: " + exception.getMessage());
        }
    }

    private long safeAdd(long left, long right) {
        long result = left + right;
        return result < left ? Long.MAX_VALUE : result;
    }
}
