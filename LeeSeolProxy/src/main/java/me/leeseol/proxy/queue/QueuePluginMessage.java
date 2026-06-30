package me.leeseol.proxy.queue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public final class QueuePluginMessage {
    public static final String LIMBO_REQUEST = "limbo-request";
    public static final String LIMBO_RESULT = "limbo-result";
    public static final String LOBBY_REQUEST = "lobby-request";
    public static final String QUEUE_LEAVE = "queue-leave";

    private QueuePluginMessage() {
    }

    public static byte[] limboRequest(UUID requestId, UUID playerId, String username, String limboWorld) {
        return write(output -> {
            output.writeUTF(LIMBO_REQUEST);
            output.writeUTF(requestId.toString());
            output.writeUTF(playerId.toString());
            output.writeUTF(username);
            output.writeUTF(limboWorld);
        });
    }

    public static byte[] lobbyRequest(UUID playerId) {
        return write(output -> {
            output.writeUTF(LOBBY_REQUEST);
            output.writeUTF(playerId.toString());
        });
    }

    public static Optional<Message> read(byte[] data) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            String action = input.readUTF();
            if (LIMBO_RESULT.equals(action)) {
                return Optional.of(new Message(
                        action,
                        UUID.fromString(input.readUTF()),
                        UUID.fromString(input.readUTF()),
                        input.readBoolean(),
                        input.readUTF()
                ));
            }
            if (QUEUE_LEAVE.equals(action)) {
                return Optional.of(new Message(
                        action,
                        null,
                        UUID.fromString(input.readUTF()),
                        false,
                        input.readUTF()
                ));
            }
            return Optional.empty();
        } catch (IOException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static byte[] write(Writer writer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                writer.write(output);
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode queue plugin message.", exception);
        }
    }

    private interface Writer {
        void write(DataOutputStream output) throws IOException;
    }

    public record Message(String action, UUID requestId, UUID playerId, boolean success, String message) {
    }
}
