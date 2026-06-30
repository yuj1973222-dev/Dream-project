# LeeSeolProxy

Velocity-side custom commands for the LeeSeol Minecraft network.

## Commands

- `/lobby`, `/hub` - connect to the `lobby` server.
- `/survival`, `/wild` - connect to the `survival` server.
- `/servers`, `/serverlist`, `/network` - show registered proxy servers.

## Build on Server

```bash
sudo apt-get install -y maven
bash scripts/build-on-server.sh
```

The jar will be created at:

```text
build/libs/LeeSeolProxy-0.1.0.jar
```

## Install

Copy the jar to Velocity, not Paper:

```bash
sudo cp -f build/libs/LeeSeolProxy-0.1.0.jar /opt/minecraft/velocity/plugins/
sudo systemctl restart velocity
```
