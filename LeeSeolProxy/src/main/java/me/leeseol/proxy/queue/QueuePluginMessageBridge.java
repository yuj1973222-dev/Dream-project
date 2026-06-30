package me.leeseol.proxy.queue;

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import java.util.UUID;
import java.util.function.Supplier;

public final class QueuePluginMessageBridge {
    private final ChannelIdentifier channel;
    private final Supplier<QueueSettings> settingsSupplier;
    private final Handler handler;

    public QueuePluginMessageBridge(ChannelIdentifier channel, Supplier<QueueSettings> settingsSupplier, Handler handler) {
        this.channel = channel;
        this.settingsSupplier = settingsSupplier;
        this.handler = handler;
    }

    public boolean handle(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channel)) {
            return false;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection connection)
                || !connection.getServerInfo().getName().equalsIgnoreCase(settingsSupplier.get().lobbyServer())) {
            return true;
        }
        QueuePluginMessage.read(event.getData()).ifPresent(message -> {
            if (QueuePluginMessage.LIMBO_RESULT.equals(message.action())) {
                handler.handleLimboResult(message);
            } else if (QueuePluginMessage.QUEUE_LEAVE.equals(message.action())) {
                handler.handleQueueLeave(message.playerId());
            }
        });
        return true;
    }

    public interface Handler {
        void handleLimboResult(QueuePluginMessage.Message message);

        void handleQueueLeave(UUID playerId);
    }
}
