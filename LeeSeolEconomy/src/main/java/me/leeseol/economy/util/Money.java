package me.leeseol.economy.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private final JavaPlugin plugin;
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.KOREA);

    public Money(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String format(long amount) {
        return numberFormat.format(Math.max(0L, amount)) + plugin.getConfig().getString("currency.name", "원");
    }

    public long normalize(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0D) {
            return -1L;
        }
        return Math.round(amount);
    }

    public long parse(String input) {
        if (input == null) {
            return -1L;
        }
        try {
            long amount = Long.parseLong(input.replace(",", "").trim());
            return amount > 0L ? amount : -1L;
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }
}
