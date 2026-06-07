package me.leeseol.jobs.util;

import org.bukkit.ChatColor;

public final class Text {
    private Text() {
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
