package me.leeseol.economy.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;

public final class BalanceStore {
    private final JavaPlugin plugin;
    private final Path path;
    private final long startingBalance;

    public BalanceStore(JavaPlugin plugin, String configuredPath, long startingBalance) {
        this.plugin = plugin;
        this.startingBalance = Math.max(0L, startingBalance);
        if (configuredPath == null || configuredPath.isBlank()) {
            this.path = plugin.getDataFolder().toPath().resolve("balances.properties");
        } else {
            this.path = Path.of(configuredPath);
        }
        ensureFile();
    }

    public long getBalance(UUID uuid) {
        return access(properties -> balanceOf(properties, uuid));
    }

    public void setBalance(UUID uuid, long amount) {
        access(properties -> {
            properties.setProperty(uuid.toString(), Long.toString(Math.max(0L, amount)));
            return null;
        });
    }

    public void deposit(UUID uuid, long amount) {
        if (amount <= 0L) {
            return;
        }
        access(properties -> {
            long balance = balanceOf(properties, uuid);
            properties.setProperty(uuid.toString(), Long.toString(safeAdd(balance, amount)));
            return null;
        });
    }

    public boolean withdraw(UUID uuid, long amount) {
        if (amount <= 0L) {
            return false;
        }
        return access(properties -> {
            long balance = balanceOf(properties, uuid);
            if (balance < amount) {
                return false;
            }
            properties.setProperty(uuid.toString(), Long.toString(balance - amount));
            return true;
        });
    }

    public boolean transfer(UUID from, UUID to, long amount) {
        if (amount <= 0L || from.equals(to)) {
            return false;
        }
        return access(properties -> {
            long senderBalance = balanceOf(properties, from);
            if (senderBalance < amount) {
                return false;
            }
            long receiverBalance = balanceOf(properties, to);
            properties.setProperty(from.toString(), Long.toString(senderBalance - amount));
            properties.setProperty(to.toString(), Long.toString(safeAdd(receiverBalance, amount)));
            return true;
        });
    }

    private <T> T access(Function<Properties, T> operation) {
        ensureFile();
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            Properties properties = load(channel);
            T result = operation.apply(properties);
            save(channel, properties);
            return result;
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to access economy balance file: " + exception.getMessage());
            throw new IllegalStateException(exception);
        }
    }

    private long balanceOf(Properties properties, UUID uuid) {
        String raw = properties.getProperty(uuid.toString());
        if (raw == null) {
            properties.setProperty(uuid.toString(), Long.toString(startingBalance));
            return startingBalance;
        }
        try {
            return Math.max(0L, Long.parseLong(raw));
        } catch (NumberFormatException exception) {
            properties.setProperty(uuid.toString(), Long.toString(startingBalance));
            return startingBalance;
        }
    }

    private Properties load(FileChannel channel) throws IOException {
        channel.position(0L);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        int read;
        while ((read = channel.read(buffer)) > 0) {
            buffer.flip();
            output.write(buffer.array(), 0, read);
            buffer.clear();
        }

        Properties properties = new Properties();
        byte[] bytes = output.toByteArray();
        if (bytes.length > 0) {
            properties.load(new java.io.StringReader(new String(bytes, StandardCharsets.UTF_8)));
        }
        return properties;
    }

    private void save(FileChannel channel, Properties properties) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        properties.store(new java.io.OutputStreamWriter(output, StandardCharsets.UTF_8), "LeeSeolEconomy balances");
        channel.truncate(0L);
        channel.position(0L);
        channel.write(ByteBuffer.wrap(output.toByteArray()));
        channel.force(true);
    }

    private void ensureFile() {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to prepare balance file " + path + ": " + exception.getMessage());
        }
    }

    private long safeAdd(long current, long amount) {
        long result = current + amount;
        return result < current ? Long.MAX_VALUE : result;
    }
}
