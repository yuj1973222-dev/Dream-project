package me.leeseol.quest.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import me.leeseol.quest.LeeSeolQuestPlugin;
import me.leeseol.quest.event.QuestCompletedEvent;
import me.leeseol.quest.event.QuestObjectiveProgressEvent;
import me.leeseol.quest.event.QuestStageAdvancedEvent;
import me.leeseol.quest.event.QuestStartedEvent;
import me.leeseol.quest.model.ObjectiveType;
import me.leeseol.quest.model.PlayerQuestData;
import me.leeseol.quest.model.Quest;
import me.leeseol.quest.model.QuestObjective;
import me.leeseol.quest.model.QuestResetPeriod;
import me.leeseol.quest.model.QuestStage;
import me.leeseol.quest.storage.QuestStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class QuestService {
    private static final DateTimeFormatter DATE_KEY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LeeSeolQuestPlugin plugin;
    private final QuestStore store;
    private final Map<String, Quest> quests = new LinkedHashMap<>();

    public QuestService(LeeSeolQuestPlugin plugin, QuestStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void reload() {
        quests.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("quests");
        if (section == null) {
            plugin.getLogger().info("Loaded 0 quests.");
            return;
        }

        for (String id : section.getKeys(false)) {
            Quest quest = loadQuest(id, section.getConfigurationSection(id));
            if (quest != null) {
                quests.put(id.toLowerCase(), quest);
            }
        }
        plugin.getLogger().info("Loaded " + quests.size() + " quests.");
    }

    public Map<String, Quest> quests() {
        return Map.copyOf(quests);
    }

    public Quest quest(String id) {
        return id == null ? null : quests.get(id.toLowerCase());
    }

    public PlayerQuestData data(UUID uuid) {
        return store.data(uuid);
    }

    public Quest activeQuest(Player player) {
        PlayerQuestData data = store.data(player.getUniqueId());
        return data.hasActiveQuest() ? quest(data.activeQuestId()) : null;
    }

    public QuestStage activeStage(Player player) {
        PlayerQuestData data = store.data(player.getUniqueId());
        Quest quest = activeQuest(player);
        return quest == null ? null : quest.stage(data.stageNumber());
    }

    public boolean startQuest(Player player, String questId, boolean force) {
        Quest quest = quest(questId);
        if (quest == null) {
            plugin.message(player, "unknown-quest", "%quest%", questId);
            return false;
        }

        PlayerQuestData data = store.data(player.getUniqueId());
        if (!force && isCompleted(data, quest)) {
            plugin.message(player, "already-completed", "%quest%", quest.displayName());
            return false;
        }
        if (!force && data.hasActiveQuest()) {
            Quest active = quest(data.activeQuestId());
            plugin.message(player, "already-active", "%quest%", active == null ? data.activeQuestId() : active.displayName());
            return false;
        }

        QuestStage firstStage = quest.firstStage();
        if (firstStage == null) {
            plugin.getLogger().warning("Quest has no stages: " + quest.id());
            return false;
        }

        data.activeQuestId(quest.id());
        data.stageNumber(firstStage.number());
        data.progress(0);
        store.save();

        plugin.message(player, "started", "%quest%", quest.displayName());
        plugin.getServer().getPluginManager().callEvent(new QuestStartedEvent(player, quest));
        if (firstStage.message() != null && !firstStage.message().isBlank()) {
            player.sendMessage(plugin.color(firstStage.message()));
        }
        return true;
    }

    public void autoStartTutorial(Player player) {
        if (!plugin.getConfig().getBoolean("settings.auto-start-tutorial", true)) {
            return;
        }

        PlayerQuestData data = store.data(player.getUniqueId());
        if (data.hasActiveQuest()) {
            return;
        }

        for (Quest quest : quests.values()) {
            if (quest.autoStart() && !isCompleted(data, quest)) {
                startQuest(player, quest.id(), false);
                return;
            }
        }
    }

    public void abandon(Player player) {
        PlayerQuestData data = store.data(player.getUniqueId());
        if (!data.hasActiveQuest()) {
            plugin.message(player, "no-active");
            return;
        }
        Quest quest = quest(data.activeQuestId());
        String name = quest == null ? data.activeQuestId() : quest.displayName();
        data.clearActiveQuest();
        store.save();
        plugin.message(player, "abandoned", "%quest%", name);
    }

    public void sendProgress(Player player) {
        PlayerQuestData data = store.data(player.getUniqueId());
        Quest quest = activeQuest(player);
        QuestStage stage = activeStage(player);
        if (quest == null || stage == null) {
            plugin.message(player, "no-active");
            return;
        }

        player.sendMessage(plugin.color("&8&m------------------------------"));
        player.sendMessage(plugin.color("&b현재 퀘스트 &7: &f" + quest.displayName() + " &8(" + quest.id() + ")"));
        player.sendMessage(plugin.color("&b현재 단계 &7: &f" + stage.number()));
        player.sendMessage(plugin.color("&b현재 목표 &7: &f" + objectiveDescription(stage.objective())));
        player.sendMessage(plugin.color("&b진행도 &7: &f" + data.progress() + " / " + stage.objective().requiredAmount()));
        player.sendMessage(plugin.color("&b완료 여부 &7: &f진행 중"));
        QuestStage nextStage = quest.nextStage(stage.number());
        player.sendMessage(plugin.color("&b다음 목표 &7: &f" + (nextStage == null ? "퀘스트 완료" : objectiveDescription(nextStage.objective()))));
        player.sendMessage(plugin.color("&b보상 &7: &f" + rewardDescription(quest)));
        if (stage.message() != null && !stage.message().isBlank()) {
            player.sendMessage(plugin.color("&7안내: " + stage.message()));
        }
        player.sendMessage(plugin.color("&8&m------------------------------"));
    }

    public boolean progressObjective(Player player, ObjectiveType type, String target, Material material, int amount) {
        PlayerQuestData data = store.data(player.getUniqueId());
        Quest quest = activeQuest(player);
        QuestStage stage = activeStage(player);
        if (quest == null || stage == null || !stage.objective().matches(type, target, material)) {
            return false;
        }

        data.progress(data.progress() + Math.max(1, amount));
        plugin.getServer().getPluginManager().callEvent(new QuestObjectiveProgressEvent(
            player,
            quest,
            stage,
            type,
            target,
            data.progress(),
            stage.objective().requiredAmount()
        ));
        if (data.progress() < stage.objective().requiredAmount()) {
            store.save();
            return true;
        }

        completeStage(player, quest, stage, data);
        return true;
    }

    public void advance(Player player) {
        PlayerQuestData data = store.data(player.getUniqueId());
        Quest quest = activeQuest(player);
        QuestStage stage = activeStage(player);
        if (quest == null || stage == null) {
            plugin.message(player, "no-active");
            return;
        }
        completeStage(player, quest, stage, data);
    }

    public void setStage(Player player, String questId, int stageNumber) {
        Quest quest = quest(questId);
        if (quest == null || quest.stage(stageNumber) == null) {
            plugin.message(player, "unknown-quest", "%quest%", questId + ":" + stageNumber);
            return;
        }
        PlayerQuestData data = store.data(player.getUniqueId());
        data.activeQuestId(quest.id());
        data.stageNumber(stageNumber);
        data.progress(0);
        store.save();
    }

    public void reset(OfflinePlayer player) {
        store.reset(player.getUniqueId());
        store.save();
    }

    public void skipTutorial(Player player) {
        if (!plugin.getConfig().getBoolean("settings.allow-tutorial-skip", true)) {
            return;
        }
        PlayerQuestData data = store.data(player.getUniqueId());
        for (Quest quest : quests.values()) {
            if (quest.autoStart()) {
                data.completedQuests().add(completionKey(quest));
            }
        }
        data.clearActiveQuest();
        store.save();
        plugin.message(player, "tutorial-skipped");
    }

    public String activeQuestName(Player player) {
        Quest quest = activeQuest(player);
        return quest == null ? "" : quest.displayName();
    }

    public String objectiveText(Player player) {
        QuestStage stage = activeStage(player);
        return stage == null ? "" : stage.message();
    }

    public String progressText(Player player) {
        PlayerQuestData data = store.data(player.getUniqueId());
        QuestStage stage = activeStage(player);
        if (stage == null) {
            return "";
        }
        return data.progress() + "/" + stage.objective().requiredAmount();
    }

    public String stageText(Player player) {
        QuestStage stage = activeStage(player);
        return stage == null ? "" : String.valueOf(stage.number());
    }

    public int completedCount(Player player) {
        PlayerQuestData data = store.data(player.getUniqueId());
        int count = 0;
        for (Quest quest : quests.values()) {
            if (isCompleted(data, quest)) {
                count++;
            }
        }
        return count;
    }

    public boolean isCompleted(PlayerQuestData data, Quest quest) {
        if (quest == null) {
            return false;
        }
        return data.completedQuests().contains(completionKey(quest));
    }

    public String resetPeriodText(Quest quest) {
        return quest.resetPeriod().displayName();
    }

    private void completeStage(Player player, Quest quest, QuestStage stage, PlayerQuestData data) {
        plugin.message(
            player,
            "stage-complete",
            "%quest%", quest.displayName(),
            "%stage%", String.valueOf(stage.number())
        );

        QuestStage nextStage = quest.nextStage(stage.number());
        if (nextStage == null) {
            data.completedQuests().add(completionKey(quest));
            data.clearActiveQuest();
            store.save();
            plugin.message(player, "completed", "%quest%", quest.displayName());
            plugin.getServer().getPluginManager().callEvent(new QuestCompletedEvent(player, quest));
            giveRewards(player, quest);
            return;
        }

        data.stageNumber(nextStage.number());
        data.progress(0);
        store.save();
        plugin.getServer().getPluginManager().callEvent(new QuestStageAdvancedEvent(player, quest, stage, nextStage));
        if (nextStage.message() != null && !nextStage.message().isBlank()) {
            player.sendMessage(plugin.color(nextStage.message()));
        }
    }

    private void giveRewards(Player player, Quest quest) {
        if (quest.rewardMoney() > 0) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "won give " + player.getName() + " " + quest.rewardMoney());
            plugin.message(player, "reward-money", "%amount%", String.valueOf(quest.rewardMoney()));
        }
        for (String command : quest.rewardCommands()) {
            String parsed = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    private Quest loadQuest(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        ConfigurationSection stagesSection = section.getConfigurationSection("stages");
        if (stagesSection == null) {
            plugin.getLogger().warning("Skipping quest without stages: " + id);
            return null;
        }

        List<QuestStage> stages = new ArrayList<>();
        for (String key : stagesSection.getKeys(false)) {
            try {
                int number = Integer.parseInt(key);
                QuestStage stage = loadStage(number, stagesSection.getConfigurationSection(key));
                if (stage != null) {
                    stages.add(stage);
                }
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning("Skipping invalid quest stage '" + key + "' in " + id);
            }
        }
        stages.sort(Comparator.comparingInt(QuestStage::number));

        if (stages.isEmpty()) {
            plugin.getLogger().warning("Skipping quest with no valid stages: " + id);
            return null;
        }

        ConfigurationSection rewards = section.getConfigurationSection("rewards");
        long rewardMoney = rewards == null ? 0L : Math.max(0L, rewards.getLong("money", 0L));
        List<String> rewardCommands = rewards == null ? List.of() : rewards.getStringList("commands");

        return new Quest(
            id,
            section.getString("display-name", id),
            section.getBoolean("auto-start", false),
            QuestResetPeriod.fromConfig(section.getString("reset", "once")),
            List.copyOf(stages),
            rewardMoney,
            List.copyOf(rewardCommands)
        );
    }

    private QuestStage loadStage(int number, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        ConfigurationSection objectiveSection = section.getConfigurationSection("objective");
        if (objectiveSection == null) {
            plugin.getLogger().warning("Skipping stage without objective: " + number);
            return null;
        }

        ObjectiveType type = ObjectiveType.fromConfig(objectiveSection.getString("type"));
        if (type == null) {
            plugin.getLogger().warning("Skipping stage with invalid objective type: " + number);
            return null;
        }

        Material material = null;
        String materialName = objectiveSection.getString("material");
        if (materialName != null && !materialName.isBlank()) {
            material = Material.matchMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("Invalid quest material '" + materialName + "' in stage " + number);
            }
        }

        QuestObjective objective = new QuestObjective(
            type,
            objectiveSection.getString("target", ""),
            material,
            Math.max(1, objectiveSection.getInt("amount", 1))
        );
        return new QuestStage(number, section.getString("message", ""), objective);
    }

    private String objectiveDescription(QuestObjective objective) {
        StringBuilder builder = new StringBuilder(objective.type().configName());
        if (objective.material() != null) {
            builder.append(" ").append(objective.material().name());
        }
        if (objective.target() != null && !objective.target().isBlank()) {
            builder.append(" ").append(objective.target());
        }
        builder.append(" x").append(objective.requiredAmount());
        return builder.toString();
    }

    private String rewardDescription(Quest quest) {
        List<String> rewards = new ArrayList<>();
        if (quest.rewardMoney() > 0) {
            rewards.add(quest.rewardMoney() + "원");
        }
        if (!quest.rewardCommands().isEmpty()) {
            rewards.add("명령 보상 " + quest.rewardCommands().size() + "개");
        }
        return rewards.isEmpty() ? "없음" : String.join(", ", rewards);
    }

    private String completionKey(Quest quest) {
        if (quest.resetPeriod() == QuestResetPeriod.DAILY) {
            return quest.id() + "@daily:" + today().format(DATE_KEY);
        }
        if (quest.resetPeriod() == QuestResetPeriod.WEEKLY) {
            LocalDate today = today();
            WeekFields weekFields = WeekFields.ISO;
            int year = today.get(weekFields.weekBasedYear());
            int week = today.get(weekFields.weekOfWeekBasedYear());
            return quest.id() + "@weekly:" + year + "-W" + String.format(Locale.ROOT, "%02d", week);
        }
        return quest.id();
    }

    private LocalDate today() {
        String configuredZone = plugin.getConfig().getString("settings.reset-time-zone", "Asia/Seoul");
        try {
            return LocalDate.now(ZoneId.of(configuredZone));
        } catch (Exception exception) {
            plugin.getLogger().warning("Invalid quest reset-time-zone '" + configuredZone + "', using system default.");
            return LocalDate.now();
        }
    }
}
