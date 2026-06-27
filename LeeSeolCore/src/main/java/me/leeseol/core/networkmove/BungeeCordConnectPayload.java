package me.leeseol.core.networkmove;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class BungeeCordConnectPayload {
    private BungeeCordConnectPayload() {
    }

    public static byte[] connect(String targetServer) {
        if (targetServer == null || targetServer.isBlank()) {
            throw new IllegalArgumentException("targetServer must not be blank");
        }

        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(byteStream)) {
                output.writeUTF("Connect");
                output.writeUTF(targetServer.trim());
            }
            return byteStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode BungeeCord Connect payload", exception);
        }
    }
}
