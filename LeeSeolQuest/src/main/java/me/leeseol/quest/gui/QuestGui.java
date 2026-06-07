package me.leeseol.quest.gui;

import java.util.ArrayList;
import java.util.List;
import me.leeseol.quest.LeeSeolQuestPlugin;
import me.leeseol.quest.model.PlayerQuestData;
import me.leeseol.quest.model.Quest;
import me.leeseol.quest.service.QuestService;
import me.leeseol.quest.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class QuestGui {
    private final LeeSeolQuestPlugin plugin;
    private final QuestService questService;

    public QuestGui(LeeSeolQuestPlugin plugin, QuestService questService) {
        this.plugin = plugin;
        this.questService = questService;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(), 27, Text.color("&8퀘스트"));
        int slot = 10;
        PlayerQuestData data = questService.data(player.getUniqueId());
        for (Quest quest : questService.quests().values()) {
            if (slot > 16) {
                break;
            }
            inventory.setItem(slot++, questItem(quest, data));
        }
        player.openInventory(inventory);
        questService.progressObjective(player, me.leeseol.quest.model.ObjectiveType.OPEN_GUI, "quest", null, 1);
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        String questId = item.getItemMeta().getPersistentDataContainer().get(Holder.QUEST_ID_KEY, org.bukkit.persistence.PersistentDataType.STRING);
        if (questId == null || questId.isBlank()) {
            return;
        }
        player.closeInventory();
        questService.startQuest(player, questId, false);
    }

    private ItemStack questItem(Quest quest, PlayerQuestData data) {
        Material material = data.completedQuests().contains(quest.id()) ? Material.EMERALD : Material.BOOK;
        if (quest.id().equalsIgnoreCase(data.activeQuestId())) {
            material = Material.WRITABLE_BOOK;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Text.color("&b" + quest.displayName()));

        List<String> lore = new ArrayList<>();
        if (quest.id().equalsIgnoreCase(data.activeQuestId())) {
            lore.add(Text.color("&a진행 중"));
        } else if (data.completedQuests().contains(quest.id())) {
            lore.add(Text.color("&6완료됨"));
        } else {
            lore.add(Text.color("&7클릭해서 시작"));
        }
        lore.add(Text.color("&8ID: " + quest.id()));
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(Holder.QUEST_ID_KEY, org.bukkit.persistence.PersistentDataType.STRING, quest.id());
        item.setItemMeta(meta);
        return item;
    }

    public static final class Holder implements InventoryHolder {
        public static final org.bukkit.NamespacedKey QUEST_ID_KEY =
            new org.bukkit.NamespacedKey("leeseolquest", "quest_id");

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
