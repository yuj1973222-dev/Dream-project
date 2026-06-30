package me.leeseol.crafting.util;

import java.util.List;
import org.bukkit.ChatColor;

public final class Text {
    private Text() {
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    public static List<String> colorList(List<String> lines) {
        return lines.stream().map(Text::color).toList();
    }
}
