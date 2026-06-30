package me.leeseol.proxy.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class SwitchServerCommand implements SimpleCommand {
    private final ProxyServer proxy;
    private final String targetServer;

    public SwitchServerCommand(ProxyServer proxy, String targetServer) {
        this.proxy = proxy;
        this.targetServer = targetServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("이 명령어는 플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return;
        }

        RegisteredServer server = proxy.getServer(targetServer).orElse(null);
        if (server == null) {
            player.sendMessage(Component.text("대상 서버를 찾을 수 없습니다: " + targetServer, NamedTextColor.RED));
            return;
        }

        boolean alreadyConnected = player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName().equalsIgnoreCase(targetServer))
                .orElse(false);

        if (alreadyConnected) {
            player.sendMessage(Component.text("이미 " + targetServer + " 서버에 있습니다.", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text(targetServer + " 서버로 이동합니다...", NamedTextColor.AQUA));
        player.createConnectionRequest(server).connect().thenAccept(result -> {
            if (result.isSuccessful()) {
                player.sendMessage(Component.text(targetServer + " 서버로 이동했습니다.", NamedTextColor.GREEN));
                return;
            }

            Component reason = result.getReasonComponent()
                    .orElse(Component.text("알 수 없는 이유"));
            player.sendMessage(Component.text("서버 이동에 실패했습니다: ", NamedTextColor.RED).append(reason));
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}
