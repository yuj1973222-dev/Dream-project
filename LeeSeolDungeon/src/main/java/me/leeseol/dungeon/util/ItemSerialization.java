package me.leeseol.dungeon.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class ItemSerialization {
    private ItemSerialization() {
    }

    public static String serialize(ItemStack[] items) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeInt(items == null ? 0 : items.length);
            if (items != null) {
                for (ItemStack item : items) {
                    output.writeObject(item);
                }
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        }
    }

    public static ItemStack[] deserialize(String raw) throws IOException, ClassNotFoundException {
        if (raw == null || raw.isBlank()) {
            return new ItemStack[0];
        }
        byte[] data = Base64.getDecoder().decode(raw);
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(data);
             BukkitObjectInputStream input = new BukkitObjectInputStream(bytes)) {
            int length = input.readInt();
            ItemStack[] items = new ItemStack[Math.max(0, length)];
            for (int index = 0; index < items.length; index++) {
                Object value = input.readObject();
                items[index] = value instanceof ItemStack item ? item : null;
            }
            return items;
        }
    }

    public static ItemStack[] fit(ItemStack[] source, int size) {
        ItemStack[] fitted = new ItemStack[size];
        if (source == null) {
            return fitted;
        }
        System.arraycopy(source, 0, fitted, 0, Math.min(source.length, fitted.length));
        return fitted;
    }
}
