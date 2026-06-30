package me.leeseol.hud.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Text {
    private Text() {
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }
}
