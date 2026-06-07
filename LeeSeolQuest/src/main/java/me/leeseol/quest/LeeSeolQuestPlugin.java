package me.leeseol.quest;

import me.leeseol.quest.command.QuestAdminCommand;
import me.leeseol.quest.command.QuestCommand;
import me.leeseol.quest.command.TutorialCommand;
import me.leeseol.quest.gui.QuestGui;
import me.leeseol.quest.hook.QuestPlaceholderExpansion;
import me.leeseol.quest.listener.QuestObjectiveListener;
import me.leeseol.quest.service.QuestService;
import me.leeseol.quest.storage.QuestStore;
import me.leeseol.quest.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolQuestPlugin extends JavaPlugin {
    private QuestStore store;
    private QuestService questService;
    private QuestGui questGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        store = new QuestStore(this);
        questService = new QuestService(this, store);
        questGui = new QuestGui(this, questService);
        reloadAll();

        QuestCommand questCommand = new QuestCommand(this, questService, questGui);
        getCommand("quest").setExecutor(questCommand);
        getCommand("quest").setTabCompleter(questCommand);

        QuestAdminCommand adminCommand = new QuestAdminCommand(this, questService);
        getCommand("lsquest").setExecutor(adminCommand);
        getCommand("lsquest").setTabCompleter(adminCommand);

        TutorialCommand tutorialCommand = new TutorialCommand(this, questService);
        getCommand("tutorial").setExecutor(tutorialCommand);
        getCommand("tutorial").setTabCompleter(tutorialCommand);

        getServer().getPluginManager().registerEvents(new QuestObjectiveListener(this, questService, questGui), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new QuestPlaceholderExpansion(this, questService).register();
            getLogger().info("Registered PlaceholderAPI expansion.");
        }

        getLogger().info("LeeSeolQuest enabled.");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.save();
        }
    }

    public void reloadAll() {
        reloadConfig();
        store.load();
        questService.reload();
    }

    public QuestService questService() {
        return questService;
    }

    public String color(String message) {
        return Text.color(message);
    }

    public void message(CommandSender sender, String key, String... replacements) {
        String message = getConfig().getString("messages." + key, key);
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }
        String prefix = getConfig().getString("messages.prefix", "");
        sender.sendMessage(Text.color(prefix + message));
    }
}
