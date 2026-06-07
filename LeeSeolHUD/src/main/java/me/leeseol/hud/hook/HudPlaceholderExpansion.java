package me.leeseol.hud.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.leeseol.hud.LeeSeolHudPlugin;
import me.leeseol.hud.util.Text;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HudPlaceholderExpansion extends PlaceholderExpansion {
    private final LeeSeolHudPlugin plugin;

    public HudPlaceholderExpansion(LeeSeolHudPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "leeseolhud";
    }

    @Override
    public @NotNull String getAuthor() {
        return "lee_seol";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        Player player = offlinePlayer == null ? null : offlinePlayer.getPlayer();
        if (player == null) {
            return "";
        }

        return switch (params.toLowerCase()) {
            case "healthbar" -> healthBar(player);
            case "health" -> String.valueOf(Math.round(player.getHealth()));
            case "max_health" -> String.valueOf(Math.round(maxHealth(player)));
            case "compass_glyph" -> plugin.compassHudService().glyph(player);
            case "compass_degrees" -> String.valueOf(plugin.compassHudService().roundedDegrees(player));
            default -> null;
        };
    }

    private String healthBar(Player player) {
        if (!plugin.getConfig().getBoolean("below-name-health.enabled", true)
                || !player.hasPermission("leeseolhud.below-name-health")) {
            return "";
        }
        double maxHealth = maxHealth(player);
        double health = Math.max(0.0D, Math.min(maxHealth, player.getHealth()));
        int length = Math.max(1, plugin.getConfig().getInt("below-name-health.bar-length", 10));
        int filled = (int) Math.round((health / maxHealth) * length);
        filled = Math.max(0, Math.min(length, filled));

        String full = plugin.getConfig().getString("below-name-health.full-char", "█");
        String empty = plugin.getConfig().getString("below-name-health.empty-char", "█");
        String fullColor = plugin.getConfig().getString("below-name-health.full-color", "&c");
        String emptyColor = plugin.getConfig().getString("below-name-health.empty-color", "&8");

        String bar = fullColor + full.repeat(filled) + emptyColor + empty.repeat(length - filled);
        String heart = plugin.getConfig().getString("below-name-health.heart-symbol", "\u2764");
        String heal = plugin.healthDisplayService().healSuffix(player);
        String format = plugin.getConfig().getString("below-name-health.format", "&c%heart% &f%health%/%max_health%%heal%");
        return Text.color(format
            .replace("%bar%", bar)
            .replace("%heart%", heart)
            .replace("%health%", String.valueOf(Math.round(health)))
            .replace("%max_health%", String.valueOf(Math.round(maxHealth)))
            .replace("%heal%", heal));
    }

    private static double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? 20.0D : Math.max(1.0D, attribute.getValue());
    }
}
