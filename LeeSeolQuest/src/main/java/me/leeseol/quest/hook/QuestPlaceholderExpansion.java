package me.leeseol.quest.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.leeseol.quest.LeeSeolQuestPlugin;
import me.leeseol.quest.service.QuestService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class QuestPlaceholderExpansion extends PlaceholderExpansion {
    private final LeeSeolQuestPlugin plugin;
    private final QuestService questService;

    public QuestPlaceholderExpansion(LeeSeolQuestPlugin plugin, QuestService questService) {
        this.plugin = plugin;
        this.questService = questService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "leeseolquest";
    }

    @Override
    public @NotNull String getAuthor() {
        return "lee_seol";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        Player player = offlinePlayer == null ? null : offlinePlayer.getPlayer();
        if (player == null) {
            return "";
        }

        return switch (params.toLowerCase()) {
            case "active" -> questService.activeQuestName(player);
            case "stage" -> questService.stageText(player);
            case "objective" -> questService.objectiveText(player);
            case "progress" -> questService.progressText(player);
            case "completed_count" -> String.valueOf(questService.completedCount(player));
            default -> "";
        };
    }
}
