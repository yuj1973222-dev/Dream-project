package me.leeseol.proxy.command;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.ProxyServer;
import me.leeseol.proxy.queue.SurvivalQueueController;

public final class ProxyCommandRegistrar {
    private final ProxyServer proxy;
    private final Object plugin;
    private final SurvivalQueueController queueController;

    public ProxyCommandRegistrar(ProxyServer proxy, Object plugin, SurvivalQueueController queueController) {
        this.proxy = proxy;
        this.plugin = plugin;
        this.queueController = queueController;
    }

    public void registerAll() {
        CommandManager commandManager = proxy.getCommandManager();
        registerServersCommand(commandManager);
        registerLobbyCommand(commandManager);
        registerSurvivalCommand(commandManager);
    }

    private void registerServersCommand(CommandManager commandManager) {
        CommandMeta meta = commandManager.metaBuilder("servers")
                .aliases("serverlist", "network")
                .plugin(plugin)
                .build();
        commandManager.register(meta, new ServerListCommand(proxy));
    }

    private void registerLobbyCommand(CommandManager commandManager) {
        CommandMeta meta = commandManager.metaBuilder("lobby")
                .aliases("hub")
                .plugin(plugin)
                .build();
        commandManager.register(meta, new LobbyCommand(queueController));
    }

    private void registerSurvivalCommand(CommandManager commandManager) {
        CommandMeta meta = commandManager.metaBuilder("survival")
                .aliases("wild")
                .plugin(plugin)
                .build();
        commandManager.register(meta, new SurvivalQueueCommand(queueController));
    }
}
