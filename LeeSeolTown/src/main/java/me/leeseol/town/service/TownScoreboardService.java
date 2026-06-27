package me.leeseol.town.service;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.clip.placeholderapi.PlaceholderAPI;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.NeutralZone;
import me.leeseol.town.model.Town;
import me.leeseol.town.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public final class TownScoreboardService {
    private static final String OBJECTIVE_NAME = "leeseoltown_sb";
    private static final String TEAM_PREFIX = "lsb_";
    private static final String[] ENTRIES = {
            ChatColor.BLACK.toString() + ChatColor.RESET,
            ChatColor.DARK_BLUE.toString() + ChatColor.RESET,
            ChatColor.DARK_GREEN.toString() + ChatColor.RESET,
            ChatColor.DARK_AQUA.toString() + ChatColor.RESET,
            ChatColor.DARK_RED.toString() + ChatColor.RESET,
            ChatColor.DARK_PURPLE.toString() + ChatColor.RESET,
            ChatColor.GOLD.toString() + ChatColor.RESET
    };

    private final LeeSeolTownPlugin plugin;
    private final Map<UUID, BoardState> boards = new HashMap<>();
    private final Map<UUID, String> currentZoneLabels = new HashMap<>();
    private BukkitTask task;

    public TownScoreboardService(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop(false);
        if (!enabled()) {
            clearAll();
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadInitialZone(player);
        }
        long intervalTicks = Math.max(20L, plugin.getConfig().getLong("scoreboard.update-interval-ticks", 40L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, intervalTicks);
    }

    public boolean isRunning() {
        return task != null && !task.isCancelled();
    }

    public boolean isEnabledByConfig() {
        return enabled();
    }

    public int boardCount() {
        return boards.size();
    }

    public int zoneLabelCount() {
        return currentZoneLabels.size();
    }

    public int refreshOnlinePlayers() {
        int refreshed = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadInitialZone(player);
            refreshed++;
        }
        return refreshed;
    }

    public void stop() {
        stop(true);
    }

    public void loadInitialZone(Player player) {
        currentZoneLabels.put(player.getUniqueId(), zoneSnapshot(player));
        update(player);
    }

    public void setNeutralZone(Player player, String zoneId) {
        setCurrentZone(player, "중립구역 " + zoneId);
    }

    public void refreshCurrentZone(Player player) {
        setCurrentZone(player, zoneSnapshot(player));
    }

    public void remove(Player player) {
        currentZoneLabels.remove(player.getUniqueId());
        boards.remove(player.getUniqueId());
    }

    private void setCurrentZone(Player player, String zoneLabel) {
        currentZoneLabels.put(player.getUniqueId(), zoneLabel);
        update(player);
    }

    private void stop(boolean clear) {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (clear) {
            clearAll();
        }
    }

    private void updateAll() {
        if (!enabled()) {
            clearAll();
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            currentZoneLabels.put(player.getUniqueId(), zoneSnapshot(player));
            update(player);
        }
        boards.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        currentZoneLabels.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    private void update(Player player) {
        if (!enabled()) {
            return;
        }
        BoardState state = boards.computeIfAbsent(player.getUniqueId(), ignored -> newBoardState());
        if (state == null) {
            return;
        }
        Objective objective = state.scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            return;
        }
        objective.displayName(Text.component(plugin.getConfig().getString("scoreboard.title", "&#8FD9A8EXPEDITION")));

        List<String> lines = lines(player);
        for (int index = 0; index < lines.size() && index < ENTRIES.length; index++) {
            updateLine(state, index, lines.get(index), lines.size());
        }
        if (player.getScoreboard() != state.scoreboard) {
            player.setScoreboard(state.scoreboard);
        }
    }

    private BoardState newBoardState() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return null;
        }
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                "dummy",
                Text.component(plugin.getConfig().getString("scoreboard.title", "&#8FD9A8EXPEDITION"))
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        for (int index = 0; index < ENTRIES.length; index++) {
            String entry = ENTRIES[index];
            Team team = scoreboard.registerNewTeam(TEAM_PREFIX + index);
            team.addEntry(entry);
            objective.getScore(entry).setScore(ENTRIES.length - index);
        }
        return new BoardState(scoreboard);
    }

    private void updateLine(BoardState state, int index, String line, int lineCount) {
        Team team = state.scoreboard.getTeam(TEAM_PREFIX + index);
        if (team == null) {
            return;
        }
        String previous = state.lines.put(index, line);
        if (!line.equals(previous)) {
            team.prefix(Text.component(line));
        }
        Objective objective = state.scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective != null) {
            objective.getScore(ENTRIES[index]).setScore(lineCount - index);
        }
    }

    private List<String> lines(Player player) {
        Town town = plugin.townService().playerTown(player);
        Nation playerNation = plugin.townService().playerNation(player);
        return List.of(
                "&7&m----------------",
                "&f랭크 &7| " + rank(player),
                "&f자산 &7| &6" + balance(player),
                "&f구역 &7| &a" + currentZone(player),
                "&f국가 &7| " + (playerNation == null ? "&b없음" : playerNation.color().legacyPrefix() + playerNation.name()),
                "&f파티 &7| &d" + (town == null ? "없음" : town.name()),
                "&7&m----------------"
        );
    }

    private String rank(Player player) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return "PLAYER";
        }
        String image = PlaceholderAPI.setPlaceholders(player, "%leeseolranks_image%");
        if (validPlaceholderValue(image, "%leeseolranks_image%")) {
            return image;
        }
        String text = PlaceholderAPI.setPlaceholders(player, "%leeseolranks_rank%");
        return validPlaceholderValue(text, "%leeseolranks_rank%") ? text : "PLAYER";
    }

    private boolean validPlaceholderValue(String value, String placeholder) {
        return value != null && !value.isBlank() && !value.equals(placeholder);
    }

    private String balance(Player player) {
        return plugin.economy().available()
                ? plugin.economy().format(plugin.economy().balance(player))
                : "-";
    }

    private String currentZone(Player player) {
        return currentZoneLabels.computeIfAbsent(player.getUniqueId(), ignored -> zoneSnapshot(player));
    }

    private String zoneSnapshot(Player player) {
        NeutralZone neutralZone = plugin.neutralZones().zoneAt(player.getLocation());
        if (neutralZone != null) {
            return "중립구역 " + neutralZone.id();
        }
        Nation claimNation = plugin.townService().nationForClaim(ClaimKey.from(player.getLocation()));
        if (claimNation != null) {
            return claimNation.name();
        }
        return "야생";
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("scoreboard.enabled", true);
    }

    private void clearAll() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            boards.clear();
            currentZoneLabels.clear();
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            BoardState state = boards.get(player.getUniqueId());
            if (state != null && player.getScoreboard() == state.scoreboard) {
                player.setScoreboard(manager.getMainScoreboard());
            }
        }
        boards.clear();
        currentZoneLabels.clear();
    }

    private static final class BoardState {
        private final Scoreboard scoreboard;
        private final Map<Integer, String> lines = new HashMap<>();

        private BoardState(Scoreboard scoreboard) {
            this.scoreboard = scoreboard;
        }
    }
}
