package me.leeseol.proxy.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

public final class QueuePluginMessageContractTest {
    private static final UUID REQUEST_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PLAYER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Test
    public void defaultQueueChannelMatchesLobbyChannel() {
        assertEquals("leeseol:queue", QueueSettings.defaults().pluginMessageChannel());
    }

    @Test
    public void limboRequestUsesLobbyReadableWireFormat() throws Exception {
        byte[] payload = QueuePluginMessage.limboRequest(REQUEST_ID, PLAYER_ID, "LeeSeol", "limbo");

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            assertEquals(QueuePluginMessage.LIMBO_REQUEST, input.readUTF());
            assertEquals(REQUEST_ID.toString(), input.readUTF());
            assertEquals(PLAYER_ID.toString(), input.readUTF());
            assertEquals("LeeSeol", input.readUTF());
            assertEquals("limbo", input.readUTF());
            assertEquals(0, input.available());
        }
    }

    @Test
    public void lobbyRequestUsesLobbyReadableWireFormat() throws Exception {
        byte[] payload = QueuePluginMessage.lobbyRequest(PLAYER_ID);

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            assertEquals(QueuePluginMessage.LOBBY_REQUEST, input.readUTF());
            assertEquals(PLAYER_ID.toString(), input.readUTF());
            assertEquals(0, input.available());
        }
    }

    @Test
    public void readsLobbyResultAndQueueLeaveWireFormat() {
        Optional<QueuePluginMessage.Message> result = QueuePluginMessage.read(write(output -> {
            output.writeUTF(QueuePluginMessage.LIMBO_RESULT);
            output.writeUTF(REQUEST_ID.toString());
            output.writeUTF(PLAYER_ID.toString());
            output.writeBoolean(true);
            output.writeUTF("ok");
        }));

        assertTrue(result.isPresent());
        assertEquals(QueuePluginMessage.LIMBO_RESULT, result.get().action());
        assertEquals(REQUEST_ID, result.get().requestId());
        assertEquals(PLAYER_ID, result.get().playerId());
        assertTrue(result.get().success());
        assertEquals("ok", result.get().message());

        Optional<QueuePluginMessage.Message> leave = QueuePluginMessage.read(write(output -> {
            output.writeUTF(QueuePluginMessage.QUEUE_LEAVE);
            output.writeUTF(PLAYER_ID.toString());
            output.writeUTF("lobby-command");
        }));

        assertTrue(leave.isPresent());
        assertEquals(QueuePluginMessage.QUEUE_LEAVE, leave.get().action());
        assertEquals(PLAYER_ID, leave.get().playerId());
        assertFalse(leave.get().success());
        assertEquals("lobby-command", leave.get().message());
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
