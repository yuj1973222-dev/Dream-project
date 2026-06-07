package me.leeseol.combat.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Text {
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
}
