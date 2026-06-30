package me.leeseol.proxy.queue;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

public final class SurvivalQueueController {
    private final ProxyServer proxy;
    private final Object plugin;
    private final Logger logger;
    private final ChannelIdentifier channel;
    private final SurvivalQueue queue = new SurvivalQueue();
    private final Map<UUID, PendingLimboRequest> pendingLimboRequests = new ConcurrentHashMap<>();
    private volatile QueueSettings settings;
    private ScheduledTask actionbarTask;
    private boolean processingQueue;
    private boolean processAgain;

    public SurvivalQueueController(
            ProxyServer proxy,
            Object plugin,
            Logger logger,
            ChannelIdentifier channel,
            QueueSettings settings
    ) {
        this.proxy = proxy;
        this.plugin = plugin;
        this.logger = logger;
        this.channel = channel;
        this.settings = settings;
    }

    public void start() {
        actionbarTask = proxy.getScheduler()
                .buildTask(plugin, this::sendActionBars)
                .repeat(settings.actionbarIntervalSeconds(), TimeUnit.SECONDS)
                .schedule();
    }

    public void close() {
        if (actionbarTask != null) {
            actionbarTask.cancel();
            actionbarTask = null;
        }
    }

    public void updateSettings(QueueSettings settings) {
        this.settings = settings;
    }

    public QueueSettings settings() {
        return settings;
    }

    public void requestSurvival(Player player) {
        QueueSettings current = settings;
        if (isOnServer(player, current.survivalServer())) {
            send(player, current.alreadySurvivalMessage(), NamedTextColor.YELLOW);
            return;
        }

        if (player.hasPermission(current.bypassPermission())) {
            connectDirectlyToSurvival(player, current);
            return;
        }

        Optional<RegisteredServer> survival = proxy.getServer(current.survivalServer());
        if (survival.isEmpty()) {
            send(player, current.survivalMissingMessage(), NamedTextColor.RED);
            return;
        }

        if (normalSurvivalPlayers(current) < current.maxSurvivalPlayers()) {
            connectDirectlyToSurvival(player, current);
            return;
        }

        int existingPosition = queue.positionOf(player.getUniqueId());
        if (existingPosition > 0) {
            send(player, current.queuePositionText(existingPosition), NamedTextColor.YELLOW);
            return;
        }

        if (pendingLimboRequests.containsKey(player.getUniqueId())) {
            send(player, current.limboMoveMessage(), NamedTextColor.YELLOW);
            return;
        }

        moveToLimboBeforeQueue(player, current);
    }

    public void requestLobby(Player player) {
        QueueSettings current = settings;
        boolean removed = removeQueuedState(player.getUniqueId());
        if (removed) {
            send(player, current.queueLeftMessage(), NamedTextColor.YELLOW);
        }

        Optional<RegisteredServer> lobby = proxy.getServer(current.lobbyServer());
        if (lobby.isEmpty()) {
            send(player, current.lobbyMissingMessage(), NamedTextColor.RED);
            return;
        }

        if (isOnServer(player, current.lobbyServer())) {
            sendLobbyWorldRequest(player, current);
            return;
        }

        send(player, current.lobbyMoveMessage(), NamedTextColor.AQUA);
        player.createConnectionRequest(lobby.get()).connect().thenAccept(result -> {
            if (result.isSuccessful()) {
                sendLobbyWorldRequest(player, current);
            } else {
                send(player, current.lobbyMissingMessage(), NamedTextColor.RED);
            }
        });
    }

    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channel)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection connection)
                || !connection.getServerInfo().getName().equalsIgnoreCase(settings.lobbyServer())) {
            return;
        }
        QueuePluginMessage.read(event.getData()).ifPresent(message -> {
            if (QueuePluginMessage.LIMBO_RESULT.equals(message.action())) {
                handleLimboResult(message);
            } else if (QueuePluginMessage.QUEUE_LEAVE.equals(message.action())) {
                handleQueueLeave(message.playerId());
            }
        });
    }

    public void onServerConnected(ServerConnectedEvent event) {
        QueueSettings current = settings;
        Player player = event.getPlayer();
        String newServer = event.getServer().getServerInfo().getName();
        boolean leftLobby = !newServer.equalsIgnoreCase(current.lobbyServer());
        boolean joinedSurvival = newServer.equalsIgnoreCase(current.survivalServer());
        if (leftLobby || joinedSurvival) {
            removeQueuedState(player.getUniqueId());
        }

        boolean leftSurvival = event.getPreviousServer()
                .map(server -> server.getServerInfo().getName().equalsIgnoreCase(current.survivalServer()))
                .orElse(false)
                && !joinedSurvival;
        if (leftSurvival) {
            processQueue();
        }
    }

    public void onDisconnect(DisconnectEvent event) {
        QueueSettings current = settings;
        Player player = event.getPlayer();
        boolean wasSurvival = isOnServer(player, current.survivalServer());
        removeQueuedState(player.getUniqueId());
        if (wasSurvival) {
            processQueue();
        }
    }

    public void processQueue() {
        synchronized (this) {
            if (processingQueue) {
                processAgain = true;
                return;
            }
            processingQueue = true;
        }
        continueProcessing(new HashSet<>(), settings.maxAdmissionAttempts());
    }

    private void continueProcessing(Set<UUID> attemptedPlayers, int remainingAttempts) {
        QueueSettings current = settings;
        if (remainingAttempts <= 0 || normalSurvivalPlayers(current) >= current.maxSurvivalPlayers()) {
            finishProcessing();
            return;
        }

        Optional<SurvivalQueue.Entry> nextEntry = queue.poll();
        if (nextEntry.isEmpty()) {
            finishProcessing();
            return;
        }

        SurvivalQueue.Entry entry = nextEntry.get();
        if (!attemptedPlayers.add(entry.playerId())) {
            queue.requeueAtBack(entry);
            finishProcessing();
            return;
        }

        Optional<Player> nextPlayer = proxy.getPlayer(entry.playerId());
        if (nextPlayer.isEmpty() || !isOnServer(nextPlayer.get(), current.lobbyServer())) {
            continueProcessing(attemptedPlayers, remainingAttempts - 1);
            return;
        }

        Player player = nextPlayer.get();
        RegisteredServer survival = proxy.getServer(current.survivalServer()).orElse(null);
        if (survival == null) {
            queue.requeueAtBack(entry);
            send(player, current.survivalMissingMessage(), NamedTextColor.RED);
            finishProcessing();
            return;
        }

        send(player, current.survivalMoveMessage(), NamedTextColor.AQUA);
        player.createConnectionRequest(survival).connect().thenAccept(result -> {
            if (result.isSuccessful()) {
                send(player, current.survivalMovedMessage(), NamedTextColor.GREEN);
                finishProcessing();
            } else {
                queue.requeueAtBack(entry);
                send(player, current.survivalMoveFailedMessage(), NamedTextColor.RED);
                continueProcessing(attemptedPlayers, remainingAttempts - 1);
            }
        });
    }

    private void finishProcessing() {
        boolean runAgain;
        synchronized (this) {
            runAgain = processAgain;
            processAgain = false;
            processingQueue = false;
        }
        if (runAgain) {
            processQueue();
        }
    }

    private void connectDirectlyToSurvival(Player player, QueueSettings current) {
        RegisteredServer survival = proxy.getServer(current.survivalServer()).orElse(null);
        if (survival == null) {
            send(player, current.survivalMissingMessage(), NamedTextColor.RED);
            return;
        }

        removeQueuedState(player.getUniqueId());
        send(player, current.survivalMoveMessage(), NamedTextColor.AQUA);
        player.createConnectionRequest(survival).connect().thenAccept(result -> {
            if (result.isSuccessful()) {
                send(player, current.survivalMovedMessage(), NamedTextColor.GREEN);
            } else {
                send(player, current.survivalMoveFailedMessage(), NamedTextColor.RED);
            }
        });
    }

    private void moveToLimboBeforeQueue(Player player, QueueSettings current) {
        RegisteredServer lobby = proxy.getServer(current.lobbyServer()).orElse(null);
        if (lobby == null) {
            send(player, current.lobbyMissingMessage(), NamedTextColor.RED);
            return;
        }

        UUID requestId = UUID.randomUUID();
        pendingLimboRequests.put(player.getUniqueId(), new PendingLimboRequest(requestId));
        send(player, current.limboMoveMessage(), NamedTextColor.YELLOW);

        if (isOnServer(player, current.lobbyServer())) {
            sendLimboRequest(player, requestId, current);
            return;
        }

        player.createConnectionRequest(lobby).connect().thenAccept(result -> {
            if (!result.isSuccessful()) {
                pendingLimboRequests.remove(player.getUniqueId());
                send(player, current.limboMoveFailedMessage(), NamedTextColor.RED);
                return;
            }
            sendLimboRequest(player, requestId, current);
        });
    }

    private void sendLimboRequest(Player player, UUID requestId, QueueSettings current) {
        Optional<ServerConnection> connection = player.getCurrentServer();
        if (connection.isEmpty() || !connection.get().getServerInfo().getName().equalsIgnoreCase(current.lobbyServer())) {
            pendingLimboRequests.remove(player.getUniqueId());
            send(player, current.limboMoveFailedMessage(), NamedTextColor.RED);
            return;
        }

        byte[] payload = QueuePluginMessage.limboRequest(
                requestId,
                player.getUniqueId(),
                player.getUsername(),
                current.limboWorld()
        );
        if (!connection.get().sendPluginMessage(channel, payload)) {
            pendingLimboRequests.remove(player.getUniqueId());
            send(player, current.limboMoveFailedMessage(), NamedTextColor.RED);
            return;
        }

        proxy.getScheduler().buildTask(plugin, () -> handleLimboTimeout(player.getUniqueId(), requestId))
                .delay(current.limboRequestTimeoutSeconds(), TimeUnit.SECONDS)
                .schedule();
    }

    private void handleLimboTimeout(UUID playerId, UUID requestId) {
        PendingLimboRequest pending = pendingLimboRequests.get(playerId);
        if (pending == null || !pending.requestId().equals(requestId)) {
            return;
        }
        pendingLimboRequests.remove(playerId);
        proxy.getPlayer(playerId).ifPresent(player ->
                send(player, settings.limboMoveFailedMessage(), NamedTextColor.RED));
    }

    private void handleLimboResult(QueuePluginMessage.Message message) {
        PendingLimboRequest pending = pendingLimboRequests.get(message.playerId());
        if (pending == null || !pending.requestId().equals(message.requestId())) {
            return;
        }
        pendingLimboRequests.remove(message.playerId());

        Optional<Player> player = proxy.getPlayer(message.playerId());
        if (player.isEmpty()) {
            return;
        }

        QueueSettings current = settings;
        if (!message.success()) {
            send(player.get(), message.message().isBlank() ? current.limboMoveFailedMessage() : message.message(), NamedTextColor.RED);
            return;
        }

        SurvivalQueue.Registration registration = queue.enqueue(player.get().getUniqueId(), player.get().getUsername());
        if (registration.added()) {
            send(player.get(), current.queueAddedText(registration.position()), NamedTextColor.YELLOW);
        } else {
            send(player.get(), current.queuePositionText(registration.position()), NamedTextColor.YELLOW);
        }
        processQueue();
    }

    private void handleQueueLeave(UUID playerId) {
        Optional<Player> player = proxy.getPlayer(playerId);
        boolean removed = removeQueuedState(playerId);
        if (removed) {
            player.ifPresent(value -> send(value, settings.queueLeftMessage(), NamedTextColor.YELLOW));
        }
    }

    private boolean removeQueuedState(UUID playerId) {
        PendingLimboRequest pending = pendingLimboRequests.remove(playerId);
        boolean queued = queue.remove(playerId);
        return pending != null || queued;
    }

    private void sendLobbyWorldRequest(Player player, QueueSettings current) {
        Optional<ServerConnection> connection = player.getCurrentServer();
        if (connection.isPresent() && connection.get().getServerInfo().getName().equalsIgnoreCase(current.lobbyServer())) {
            connection.get().sendPluginMessage(channel, QueuePluginMessage.lobbyRequest(player.getUniqueId()));
        }
    }

    private void sendActionBars() {
        QueueSettings current = settings;
        for (SurvivalQueue.Entry entry : queue.snapshot()) {
            int position = queue.positionOf(entry.playerId());
            if (position <= 0) {
                continue;
            }
            proxy.getPlayer(entry.playerId()).ifPresent(player ->
                    player.sendActionBar(Component.text(current.actionbarText(position), NamedTextColor.YELLOW)));
        }
    }

    private int normalSurvivalPlayers(QueueSettings current) {
        return proxy.getServer(current.survivalServer())
                .map(server -> (int) server.getPlayersConnected().stream()
                        .filter(player -> !player.hasPermission(current.bypassPermission()))
                        .count())
                .orElse(0);
    }

    private static boolean isOnServer(Player player, String serverName) {
        return player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName().equalsIgnoreCase(serverName))
                .orElse(false);
    }

    private void send(Player player, String message, NamedTextColor color) {
        player.sendMessage(Component.text(message, color));
    }

    private record PendingLimboRequest(UUID requestId) {
    }
}
