package me.leeseol.lobby.limbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import me.leeseol.lobby.LeeSeolLobbyPlugin;
import org.junit.Test;

public final class LobbyQueuePluginMessageContractTest {
    private static final UUID REQUEST_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PLAYER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Test
    public void lobbyRegistersExpectedQueueChannel() throws Exception {
        Field field = LeeSeolLobbyPlugin.class.getDeclaredField("QUEUE_CHANNEL");
        field.setAccessible(true);

        assertEquals("leeseol:queue", field.get(null));
    }

    @Test
    public void readsProxyLimboRequestWireFormat() {
        Optional<LobbyQueuePluginMessage.Message> message = LobbyQueuePluginMessage.read(write(output -> {
            output.writeUTF(LobbyQueuePluginMessage.LIMBO_REQUEST);
            output.writeUTF(REQUEST_ID.toString());
            output.writeUTF(PLAYER_ID.toString());
            output.writeUTF("LeeSeol");
            output.writeUTF("limbo");
        }));

        assertTrue(message.isPresent());
        assertEquals(LobbyQueuePluginMessage.LIMBO_REQUEST, message.get().action());
        assertEquals(REQUEST_ID, message.get().requestId());
        assertEquals(PLAYER_ID, message.get().playerId());
        assertEquals("LeeSeol", message.get().username());
        assertEquals("limbo", message.get().limboWorld());
    }

    @Test
    public void readsProxyLobbyRequestWireFormat() {
        Optional<LobbyQueuePluginMessage.Message> message = LobbyQueuePluginMessage.read(write(output -> {
            output.writeUTF(LobbyQueuePluginMessage.LOBBY_REQUEST);
            output.writeUTF(PLAYER_ID.toString());
        }));

        assertTrue(message.isPresent());
        assertEquals(LobbyQueuePluginMessage.LOBBY_REQUEST, message.get().action());
        assertEquals(PLAYER_ID, message.get().playerId());
    }

    @Test
    public void limboResultUsesProxyReadableWireFormat() throws Exception {
        byte[] payload = LobbyQueuePluginMessage.limboResult(REQUEST_ID, PLAYER_ID, true, "ok");

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            assertEquals(LobbyQueuePluginMessage.LIMBO_RESULT, input.readUTF());
            assertEquals(REQUEST_ID.toString(), input.readUTF());
            assertEquals(PLAYER_ID.toString(), input.readUTF());
            assertTrue(input.readBoolean());
            assertEquals("ok", input.readUTF());
            assertEquals(0, input.available());
        }
    }

    @Test
    public void queueLeaveUsesProxyReadableWireFormat() throws Exception {
        byte[] payload = LobbyQueuePluginMessage.queueLeave(PLAYER_ID, "lobby-command");

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            assertEquals(LobbyQueuePluginMessage.QUEUE_LEAVE, input.readUTF());
            assertEquals(PLAYER_ID.toString(), input.readUTF());
            assertEquals("lobby-command", input.readUTF());
            assertEquals(0, input.available());
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
            throw new AssertionError(exception);
        }
    }

    private interface Writer {
        void write(DataOutputStream output) throws IOException;
    }
}
