package me.leeseol.ranks.service;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.leeseol.ranks.LeeSeolRanksPlugin;
import me.leeseol.ranks.model.Rank;
import me.leeseol.ranks.model.RankData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class RankRequirementService {
    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance(Locale.KOREA);

    private final LeeSeolRanksPlugin plugin;
    private Economy economy;

    public RankRequirementService(LeeSeolRanksPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
        if (economy == null) {
            plugin.getLogger().warning("Vault economy provider not found. Money rank requirements above 0 will fail.");
        }
    }

    public RequirementResult result(Player player, RankData data, Rank targetRank) {
        List<RequirementLine> lines = new ArrayList<>();
        int killsRequired = requiredKills(targetRank);
        if (killsRequired > 0) {
            lines.add(new RequirementLine("킬", String.valueOf(data.kills()), String.valueOf(killsRequired), data.kills() >= killsRequired));
        }

        long moneyRequired = requirementLong(targetRank, "money", 0L);
        if (moneyRequired > 0L) {
            double balance = economy == null ? 0.0D : economy.getBalance(player);
            lines.add(new RequirementLine("보유 돈", money(balance), money(moneyRequired), economy != null && balance >= moneyRequired));
        }

        int playtimeRequired = requirementInt(targetRank, "playtime-minutes", 0);
        if (playtimeRequired > 0) {
            long playedMinutes = playedMinutes(player);
            lines.add(new RequirementLine("플레이타임", playedMinutes + "분", playtimeRequired + "분", playedMinutes >= playtimeRequired));
        }

        boolean met = player.hasPermission("leeseolranks.bypass.requirements") || lines.stream().allMatch(RequirementLine::met);
        return new RequirementResult(targetRank, List.copyOf(lines), met);
    }

    public int requiredKills(Rank rank) {
        if (rank == null || !rank.progressionRank() || rank == Rank.PLAYER) {
            return 0;
        }
        String path = "rank-up.requirements." + rank.name() + ".kills";
        if (plugin.getConfig().contains(path)) {
            return Math.max(0, plugin.getConfig().getInt(path, 0));
        }
        return Math.max(1, plugin.getConfig().getInt("rank-up.thresholds." + rank.name(), defaultThreshold(rank)));
    }

    private long requirementLong(Rank rank, String key, long fallback) {
        if (rank == null) {
            return fallback;
        }
        return Math.max(0L, plugin.getConfig().getLong("rank-up.requirements." + rank.name() + "." + key, fallback));
    }

    private int requirementInt(Rank rank, String key, int fallback) {
        if (rank == null) {
            return fallback;
        }
        return Math.max(0, plugin.getConfig().getInt("rank-up.requirements." + rank.name() + "." + key, fallback));
    }

    private long playedMinutes(Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 1200L;
    }

    private String money(double amount) {
        return NUMBER.format(Math.floor(amount)) + "원";
    }

    private static int defaultThreshold(Rank rank) {
        return switch (rank.name().toUpperCase(Locale.ROOT)) {
            case "D" -> 10;
            case "C" -> 20;
            case "B" -> 30;
            case "A" -> 50;
            case "S" -> 100;
            default -> 1;
        };
    }
}
