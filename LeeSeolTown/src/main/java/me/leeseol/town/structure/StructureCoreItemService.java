package me.leeseol.town.structure;

import java.lang.reflect.Method;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.util.Text;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class StructureCoreItemService {
    private final LeeSeolTownPlugin plugin;
    private final NamespacedKey coreKey;
    private final NamespacedKey selectedKey;

    public StructureCoreItemService(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
        this.coreKey = new NamespacedKey(plugin, "structure_core");
        this.selectedKey = new NamespacedKey(plugin, "structure_id");
    }

    public boolean isCore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(coreKey, PersistentDataType.BYTE)) {
            return true;
        }
        String itemsAdderId = itemsAdderId(item);
        return itemsAdderId != null
                && (plugin.structureRegistry().allowedCoreBlocks().contains(itemsAdderId)
                || plugin.structureRegistry().getByItemsAdderBlock(itemsAdderId) != null);
    }

    public String selectedStructureId(ItemStack item) {
        if (!isCore(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String selected = meta.getPersistentDataContainer().get(selectedKey, PersistentDataType.STRING);
        return StructureCoreSelection.selectedStructureId(selected, itemsAdderId(item));
    }

    public ItemStack select(ItemStack source, StructureDefinition definition) {
        ItemStack selected = createItemsAdderItem(definition);
        if (selected == null) {
            return null;
        }
        selected.setAmount(Math.max(1, source == null ? 1 : source.getAmount()));
        ItemMeta meta = selected.getItemMeta();
        meta.displayName(Text.component(definition.name()));
        meta.getPersistentDataContainer().set(coreKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(selectedKey, PersistentDataType.STRING, definition.id());
        selected.setItemMeta(meta);
        return selected;
    }

    public ItemStack createCoreItem(StructureDefinition definition, int amount) {
        ItemStack core = createItemsAdderItem(definition);
        if (core == null) {
            return null;
        }
        core.setAmount(Math.max(1, amount));
        ItemMeta meta = core.getItemMeta();
        meta.displayName(Text.component(definition.name()));
        meta.getPersistentDataContainer().set(coreKey, PersistentDataType.BYTE, (byte) 1);
        core.setItemMeta(meta);
        return core;
    }

    public String itemsAdderId(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method byItemStack = customStackClass.getMethod("byItemStack", ItemStack.class);
            Object customStack = byItemStack.invoke(null, item);
            if (customStack == null) {
                return null;
            }
            Method namespacedId = namespacedIdMethod(customStackClass);
            Object value = namespacedId.invoke(customStack);
            return value instanceof String string ? string : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private ItemStack createItemsAdderItem(StructureDefinition definition) {
        if (definition.itemsAdderBlock().isBlank()) {
            return null;
        }
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method getInstance = customStackClass.getMethod("getInstance", String.class);
            Object customStack = getInstance.invoke(null, definition.itemsAdderBlock());
            if (customStack == null) {
                return null;
            }
            Object item = customStackClass.getMethod("getItemStack").invoke(customStack);
            return item instanceof ItemStack stack ? stack : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private Method namespacedIdMethod(Class<?> customStackClass) throws NoSuchMethodException {
        try {
            return customStackClass.getMethod("getNamespacedID");
        } catch (NoSuchMethodException ignored) {
            return customStackClass.getMethod("getNamespacedId");
        }
    }
}
