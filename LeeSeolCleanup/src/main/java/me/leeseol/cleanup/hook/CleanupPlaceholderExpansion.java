package me.leeseol.cleanup.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.leeseol.cleanup.LeeSeolCleanupPlugin;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CleanupPlaceholderExpansion extends PlaceholderExpansion {
    private final LeeSeolCleanupPlugin plugin;

    public CleanupPlaceholderExpansion(LeeSeolCleanupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "leeseolcleanup";
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
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("next")) {
            return plugin.cleanupManager().nextCleanupText();
        }
        if (params.equalsIgnoreCase("seconds")) {
            long seconds = plugin.cleanupManager().secondsUntilNextCleanup();
            return Long.toString(Math.max(0L, seconds));
        }
        return null;
    }
}
