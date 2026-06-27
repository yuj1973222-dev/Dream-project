package me.leeseol.enchanting.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import me.leeseol.enchanting.LeeSeolEnchantingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class AdvancedEnchantmentsBridge {
    private final LeeSeolEnchantingPlugin plugin;
    private boolean available;
    private Method isAnEnchantment;
    private Method getHighestEnchantmentLevel;
    private Method isApplicable;
    private Method applyEnchant;
    private Method createEnchantmentBook;
    private Method getEnchantmentsOnItem;
    private Method getEnchantmentInstance;
    private Method getDisplay;
    private Method getLevelDescriptionOrFallback;
    private Method getDescription;

    public AdvancedEnchantmentsBridge(LeeSeolEnchantingPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        available = false;
        isAnEnchantment = null;
        getHighestEnchantmentLevel = null;
        isApplicable = null;
        applyEnchant = null;
        createEnchantmentBook = null;
        getEnchantmentsOnItem = null;
        getEnchantmentInstance = null;
        getDisplay = null;
        getLevelDescriptionOrFallback = null;
        getDescription = null;

        if (!Bukkit.getPluginManager().isPluginEnabled("AdvancedEnchantments")) {
            plugin.getLogger().warning("AdvancedEnchantments is not enabled. Custom enchant rolls are disabled.");
            return;
        }
        try {
            Class<?> api = Class.forName("net.advancedplugins.ae.api.AEAPI");
            Class<?> enchantment = Class.forName("net.advancedplugins.ae.enchanthandler.enchantments.AdvancedEnchantment");
            isAnEnchantment = api.getMethod("isAnEnchantment", String.class);
            getHighestEnchantmentLevel = api.getMethod("getHighestEnchantmentLevel", String.class);
            isApplicable = api.getMethod("isApplicable", Material.class, String.class);
            applyEnchant = api.getMethod("applyEnchant", String.class, int.class, ItemStack.class);
            createEnchantmentBook = api.getMethod(
                "createEnchantmentBook",
                String.class,
                int.class,
                int.class,
                int.class,
                Player.class
            );
            getEnchantmentsOnItem = api.getMethod("getEnchantmentsOnItem", ItemStack.class);
            getEnchantmentInstance = api.getMethod("getEnchantmentInstance", String.class);
            getDisplay = enchantment.getMethod("getDisplay", int.class);
            getLevelDescriptionOrFallback = enchantment.getMethod("getLevelDescriptionOrFallback", int.class);
            getDescription = enchantment.getMethod("getDescription");
            available = true;
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            plugin.getLogger().warning("AdvancedEnchantments API is not compatible: " + exception.getMessage());
        }
    }

    public boolean available() {
        return available;
    }

    public boolean enchantExists(String enchant) {
        if (!available) {
            return false;
        }
        Object result = invoke(isAnEnchantment, enchant);
        return result instanceof Boolean value && value;
    }

    public int highestLevel(String enchant) {
        if (!available) {
            return 1;
        }
        Object result = invoke(getHighestEnchantmentLevel, enchant);
        return result instanceof Number number ? Math.max(1, number.intValue()) : 1;
    }

    public boolean applicable(Material material, String enchant) {
        if (!available) {
            return false;
        }
        Object result = invoke(isApplicable, material, enchant);
        return result instanceof Boolean value && value;
    }

    public ItemStack apply(String enchant, int level, ItemStack item) {
        if (!available || item == null) {
            return null;
        }
        Object result = invoke(applyEnchant, enchant, level, item);
        return result instanceof ItemStack stack ? stack : null;
    }

    public ItemStack createBook(String enchant, int level, int success, int destroy, Player player) {
        if (!available) {
            return null;
        }
        Object result = invoke(createEnchantmentBook, enchant, level, success, destroy, player);
        return result instanceof ItemStack stack ? stack : null;
    }

    public Map<String, Integer> enchantmentsOn(ItemStack item) {
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        if (!available || item == null || item.getType().isAir()) {
            return enchantments;
        }
        Object result = invoke(getEnchantmentsOnItem, item);
        if (!(result instanceof Map<?, ?> raw)) {
            return enchantments;
        }
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() == null || !(entry.getValue() instanceof Number level)) {
                continue;
            }
            enchantments.put(String.valueOf(entry.getKey()).toLowerCase(), Math.max(1, level.intValue()));
        }
        return enchantments;
    }

    public String display(String enchant, int level) {
        Object instance = enchantmentInstance(enchant);
        Object result = instance == null ? null : invokeInstance(getDisplay, instance, level);
        return result instanceof String value ? value : enchant + " " + level;
    }

    public String description(String enchant, int level) {
        Object instance = enchantmentInstance(enchant);
        Object levelDescription = instance == null ? null : invokeInstance(getLevelDescriptionOrFallback, instance, level);
        if (levelDescription instanceof String value && !value.isBlank()) {
            return value;
        }
        Object description = instance == null ? null : invokeInstance(getDescription, instance);
        return description instanceof String value ? value : "";
    }

    private Object enchantmentInstance(String enchant) {
        if (!available || enchant == null || enchant.isBlank()) {
            return null;
        }
        return invoke(getEnchantmentInstance, enchant);
    }

    private Object invoke(Method method, Object... args) {
        try {
            return method.invoke(null, args);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            if (plugin.enchantingConfig().debug()) {
                plugin.getLogger().warning("AdvancedEnchantments API call failed: " + exception.getMessage());
            }
            return null;
        }
    }

    private Object invokeInstance(Method method, Object instance, Object... args) {
        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            if (plugin.enchantingConfig().debug()) {
                plugin.getLogger().warning("AdvancedEnchantments API call failed: " + exception.getMessage());
            }
            return null;
        }
    }
}
