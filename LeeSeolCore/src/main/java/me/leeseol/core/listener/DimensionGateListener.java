package me.leeseol.core.listener;

import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;
import java.util.Locale;

public final class DimensionGateListener implements Listener {
    private final LeeSeolCorePlugin plugin;

    public DimensionGateListener(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPortalTeleport(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                && cause != PlayerTeleportEvent.TeleportCause.END_PORTAL
                && cause != PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            return;
        }
        if (!plugin.getConfig().getBoolean("dimension-gate.enabled", false)) {
            return;
        }

        Player player = event.getPlayer();
        String bypassPermission = plugin.getConfig().getString("dimension-gate.bypass-permission", "leeseolcore.dimension.bypass");
        if (bypassPermission != null && !bypassPermission.isBlank() && player.hasPermission(bypassPermission)) {
            return;
        }

        String mode = plugin.getConfig().getString("dimension-gate.mode", "disabled").toLowerCase(Locale.ROOT);
        World fromWorld = event.getFrom().getWorld();
        String fromWorldName = fromWorld == null ? "" : fromWorld.getName();

        if (mode.equals("lobby")) {
            cancel(event, "dimension-gate.messages.lobby-blocked", "&c로비에서는 다른 차원으로 이동할 수 없습니다.");
            return;
        }

        if (!mode.equals("survival")) {
            return;
        }

        if (containsIgnoreCase(plugin.getConfig().getStringList("dimension-gate.survival.dungeon-worlds"), fromWorldName)) {
            cancel(event, "dimension-gate.messages.dungeon-blocked", "&c던전 월드에서는 다른 차원으로 이동할 수 없습니다.");
            return;
        }

        if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL
                || cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            cancel(event, "dimension-gate.messages.end-blocked", "&c서바이벌에서는 엔더 차원으로 이동할 수 없습니다.");
            return;
        }

        if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                && !canUseSurvivalNether(fromWorldName)) {
            cancel(event, "dimension-gate.messages.nether-blocked", "&c이 월드에서는 네더 차원으로 이동할 수 없습니다.");
        }
    }

    private boolean canUseSurvivalNether(String worldName) {
        return containsIgnoreCase(plugin.getConfig().getStringList("dimension-gate.survival.main-worlds"), worldName)
                || containsIgnoreCase(plugin.getConfig().getStringList("dimension-gate.survival.nether-worlds"), worldName);
    }

    private boolean containsIgnoreCase(List<String> values, String candidate) {
        if (candidate == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void cancel(PlayerTeleportEvent event, String messagePath, String fallback) {
        event.setCancelled(true);
        String message = plugin.getConfig().getString(messagePath, fallback);
        if (message != null && !message.isBlank()) {
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}
