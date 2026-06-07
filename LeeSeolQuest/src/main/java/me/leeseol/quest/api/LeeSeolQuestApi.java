package me.leeseol.quest.api;

import java.util.UUID;
import me.leeseol.quest.LeeSeolQuestPlugin;
import me.leeseol.quest.model.ObjectiveType;
import me.leeseol.quest.model.PlayerQuestData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class LeeSeolQuestApi {
    private LeeSeolQuestApi() {
    }

    public static boolean progress(Player player, String objectiveType, String target, int amount) {
        return progress(player, objectiveType, target, null, amount);
    }

    public static boolean progress(Player player, String objectiveType, String target, Material material, int amount) {
        LeeSeolQuestPlugin plugin = plugin();
        ObjectiveType type = ObjectiveType.fromConfig(objectiveType);
        if (plugin == null || player == null || type == null) {
            return false;
        }
        return plugin.questService().progressObjective(player, type, target, material, amount);
    }

    public static String activeQuestId(UUID playerId) {
        LeeSeolQuestPlugin plugin = plugin();
        if (plugin == null || playerId == null) {
            return "";
        }
        PlayerQuestData data = plugin.questService().data(playerId);
        return data.activeQuestId() == null ? "" : data.activeQuestId();
    }

    private static LeeSeolQuestPlugin plugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("LeeSeolQuest");
        return plugin instanceof LeeSeolQuestPlugin questPlugin ? questPlugin : null;
    }
}
