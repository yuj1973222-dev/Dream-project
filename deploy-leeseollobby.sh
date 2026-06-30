#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolLobby.tar.gz"
BUILD_DIR="/tmp/LeeSeolLobby-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolLobby"
LOBBY_DIR="/opt/minecraft/lobby"
PLUGIN_JAR="$PROJECT_DIR/build/libs/LeeSeolLobby-0.1.0.jar"

if [ ! -f "$ARCHIVE" ]; then
  echo "Missing $ARCHIVE"
  exit 1
fi

sudo rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
tar -xzf "$ARCHIVE" -C "$BUILD_DIR"

cd "$PROJECT_DIR"
chmod +x scripts/build-on-server.sh
bash scripts/build-on-server.sh

if [ ! -f "$PLUGIN_JAR" ]; then
  echo "Build succeeded but jar is missing: $PLUGIN_JAR"
  exit 1
fi

sudo systemctl stop lobby

sudo cp -f "$PLUGIN_JAR" "$LOBBY_DIR/plugins/LeeSeolLobby-0.1.0.jar"
sudo chown yuj1973222:yuj1973222 "$LOBBY_DIR/plugins/LeeSeolLobby-0.1.0.jar"

sudo python3 - <<'PY'
from pathlib import Path

path = Path("/opt/minecraft/lobby/server.properties")
props = {}
order = []

for line in path.read_text().splitlines():
    if "=" not in line or line.startswith("#"):
        order.append(line)
        continue

    key, value = line.split("=", 1)
    props[key] = value
    order.append(key)

updates = {
    "difficulty": "peaceful",
    "gamemode": "adventure",
    "force-gamemode": "true",
    "pvp": "false",
    "spawn-protection": "0",
    "allow-nether": "false",
}

props.update(updates)

seen = set()
lines = []
for item in order:
    if item in props:
        if item in seen:
            continue
        lines.append(f"{item}={props[item]}")
        seen.add(item)
    else:
        lines.append(item)

for key, value in updates.items():
    if key not in seen:
        lines.append(f"{key}={value}")

path.write_text("\n".join(lines) + "\n")
PY

sudo systemctl reset-failed lobby
sudo systemctl start lobby

sleep 30

echo "== lobby status =="
systemctl is-active lobby

echo "== lobby properties =="
grep -E '^(difficulty|gamemode|force-gamemode|pvp|spawn-protection|allow-nether)=' "$LOBBY_DIR/server.properties"

echo "== lobby plugin log =="
sudo journalctl -u lobby -n 160 --no-pager | grep -Ei 'LeeSeolLobby|Done|error|exception' | tail -80

echo "== cleanup =="
sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
