package me.leeseol.proxy.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Comparator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class ServerListCommand implements SimpleCommand {
    private final ProxyServer proxy;

    public ServerListCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        source.sendMessage(Component.text("=== LeeSeol Network ===", NamedTextColor.GOLD));

        proxy.getAllServers().stream()
                .sorted(Comparator.comparing(server -> server.getServerInfo().getName()))
                .forEach(server -> sendServerLine(source, server));

        if (source instanceof Player) {
            source.sendMessage(Component.text("/lobby, /survival 명령어로 이동할 수 있습니다.", NamedTextColor.GRAY));
        }
    }

    private void sendServerLine(CommandSource source, RegisteredServer server) {
        String name = server.getServerInfo().getName();
        int players = server.getPlayersConnected().size();
        source.sendMessage(Component.text("- " + name + " (" + players + "명)", NamedTextColor.AQUA));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}
