package me.leeseol.proxy.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PropertiesConfigFile {
    private final Path dataDirectory;

    public PropertiesConfigFile(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public Properties load(String fileName, Properties defaults, String header) throws IOException {
        Properties properties = new Properties();
        properties.putAll(defaults);
        Path configPath = dataDirectory.resolve(fileName);

        Files.createDirectories(dataDirectory);
        if (Files.exists(configPath)) {
            try (var reader = Files.newBufferedReader(configPath)) {
                properties.load(reader);
            }
        } else {
            try (var writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, header);
            }
        }

        return properties;
    }
}
