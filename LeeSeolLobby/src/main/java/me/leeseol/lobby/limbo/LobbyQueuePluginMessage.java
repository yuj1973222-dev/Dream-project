package me.leeseol.lobby.limbo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public final class LobbyQueuePluginMessage {
    public static final String LIMBO_REQUEST = "limbo-request";
    public static final String LIMBO_RESULT = "limbo-result";
    public static final String LOBBY_REQUEST = "lobby-request";
    public static final String QUEUE_LEAVE = "queue-leave";

    private LobbyQueuePluginMessage() {
    }

    public static Optional<Message> read(byte[] data) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            String action = input.readUTF();
            if (LIMBO_REQUEST.equals(action)) {
                return Optional.of(new Message(
                        action,
                        UUID.fromString(input.readUTF()),
                        UUID.fromString(input.readUTF()),
                        input.readUTF(),
                        input.readUTF()
                ));
            }
            if (LOBBY_REQUEST.equals(action)) {
                return Optional.of(new Message(
                        action,
                        null,
                        UUID.fromString(input.readUTF()),
                        "",
                        ""
                ));
            }
            return Optional.empty();
        } catch (IOException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public static byte[] limboResult(UUID requestId, UUID playerId, boolean success, String message) {
        return write(output -> {
            output.writeUTF(LIMBO_RESULT);
            output.writeUTF(requestId.toString());
            output.writeUTF(playerId.toString());
            output.writeBoolean(success);
            output.writeUTF(message);
        });
    }

    public static byte[] queueLeave(UUID playerId, String reason) {
        return write(output -> {
            output.writeUTF(QUEUE_LEAVE);
            output.writeUTF(playerId.toString());
            output.writeUTF(reason);
        });
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

    public record Message(String action, UUID requestId, UUID playerId, String username, String limboWorld) {
    }
}
