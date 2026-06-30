package me.leeseol.auction.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final Pattern MINI_HEX = Pattern.compile("(?i)<#([0-9a-f]{6})>");
    private static final Pattern AMP_HEX = Pattern.compile("(?i)&#([0-9a-f]{6})");

    private Text() {
    }

    public static String color(String input) {
        String value = input == null ? "" : input;
        value = replaceHex(value, MINI_HEX);
        value = replaceHex(value, AMP_HEX);
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    public static List<String> colorList(List<String> input) {
        List<String> output = new ArrayList<>();
        for (String line : input) {
            output.add(color(line));
        }
        return output;
    }

    public static Component component(String input) {
        return LEGACY.deserialize(color(input));
    }

    private static String replaceHex(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(toSectionHex(matcher.group(1))));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String toSectionHex(String hex) {
        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.COLOR_CHAR).append('x');
        for (char character : hex.toCharArray()) {
            builder.append(ChatColor.COLOR_CHAR).append(character);
        }
        return builder.toString();
    }
}
