package me.leeseol.cleanup.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Text {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private Text() {
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static void send(CommandSender sender, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        sender.sendMessage(color(message));
    }

    public static Component component(String message) {
        return LEGACY.deserialize(color(message));
    }
}
