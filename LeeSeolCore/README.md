# LeeSeolCore

LeeSeolCore is the first custom Paper plugin for the Minecraft server.

## Features

- Sends a welcome message when a player joins.
- Adds `/serverinfo`.
- Adds `/lscore reload` for reloading `config.yml`.

## Build

This project targets Paper `26.1.2` and Java `25`.

```bash
gradle build
```

The plugin jar will be created under:

```text
build/libs/LeeSeolCore-0.1.0.jar
```

If you build directly on the Minecraft server, you can use:

```bash
sudo apt-get install -y maven
bash scripts/build-with-paper-jar.sh
```

This script uses Maven so Paper API's transitive dependencies are resolved
correctly. The server still runs with `/opt/minecraft/server/paper.jar`.

## Install

Upload the jar to the server:

```text
/opt/minecraft/server/plugins/
```

Then restart the Minecraft service:

```bash
sudo systemctl restart minecraft
```

Check that it loaded:

```text
/plugins
```
