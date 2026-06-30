package me.leeseol.core.networkmove;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import org.junit.Test;

public final class BungeeCordConnectPayloadTest {
    @Test
    public void connectPayloadUsesBungeeCordConnectSubchannel() throws Exception {
        byte[] payload = BungeeCordConnectPayload.connect("survival");

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            assertEquals("Connect", input.readUTF());
            assertEquals("survival", input.readUTF());
            assertEquals(0, input.available());
        }
    }

    @Test
    public void connectPayloadTrimsTargetServer() throws Exception {
        byte[] payload = BungeeCordConnectPayload.connect("  lobby  ");

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            assertEquals("Connect", input.readUTF());
            assertEquals("lobby", input.readUTF());
            assertEquals(0, input.available());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectPayloadRejectsBlankTargetServer() {
        BungeeCordConnectPayload.connect(" ");
    }
}
