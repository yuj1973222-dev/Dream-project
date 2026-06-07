#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolEconomy.tar.gz"
BUILD_DIR="/tmp/LeeSeolEconomy-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolEconomy"
JAR="$PROJECT_DIR/target/LeeSeolEconomy-0.1.0.jar"
CONFIG="$PROJECT_DIR/src/main/resources/config.yml"
STAMP="$(date +%Y-%m-%d_%H-%M-%S)"

if [ ! -f "$ARCHIVE" ]; then
  echo "Missing source archive: $ARCHIVE"
  exit 1
fi

sudo rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
tar -xzf "$ARCHIVE" -C "$BUILD_DIR"
cd "$PROJECT_DIR"
mvn -q -DskipTests package

if [ ! -f "$JAR" ]; then
  echo "Build jar not found: $JAR"
  exit 1
fi

sudo mkdir -p /opt/minecraft/backups

for dir in /opt/minecraft/server /opt/minecraft/lobby /opt/minecraft/dungeon; do
  if [ ! -d "$dir" ]; then
    continue
  fi
  name="$(basename "$dir")"
  sudo mkdir -p "$dir/plugins"

  if [ -f "$dir/plugins/LeeSeolEconomy-0.1.0.jar" ]; then
    sudo cp -f "$dir/plugins/LeeSeolEconomy-0.1.0.jar" \
      "/opt/minecraft/backups/LeeSeolEconomy-0.1.0-${name}-${STAMP}.jar"
  fi
  if [ -f "$dir/plugins/LeeSeolEconomy/config.yml" ]; then
    sudo cp -f "$dir/plugins/LeeSeolEconomy/config.yml" \
      "$dir/plugins/LeeSeolEconomy/config.yml.before-servermenu-$STAMP"
  fi

  sudo cp -f "$JAR" "$dir/plugins/LeeSeolEconomy-0.1.0.jar"
  sudo mkdir -p "$dir/plugins/LeeSeolEconomy"
  sudo cp -f "$CONFIG" "$dir/plugins/LeeSeolEconomy/config.yml"
  sudo chown yuj1973222:yuj1973222 "$dir/plugins/LeeSeolEconomy-0.1.0.jar"

  local_server="survival"
  if [ "$name" = "lobby" ]; then
    local_server="lobby"
  elif [ "$name" = "dungeon" ]; then
    local_server="newworld"
  fi

  sudo python3 - "$dir/plugins/LeeSeolEconomy/config.yml" "$local_server" <<'PY'
from pathlib import Path
import re
import sys

path = Path(sys.argv[1])
local_server = sys.argv[2]
text = path.read_text(encoding="utf-8")
if "server-menu:" in text:
    if re.search(r"(?m)^  local-server:", text):
        text = re.sub(r"(?m)^  local-server:.*$", f'  local-server: "{local_server}"', text, count=1)
    else:
        text = text.replace("server-menu:\n", f'server-menu:\n  local-server: "{local_server}"\n', 1)
path.write_text(text, encoding="utf-8")
PY
  sudo chown yuj1973222:yuj1973222 "$dir/plugins/LeeSeolEconomy/config.yml"
done

echo "LeeSeolEconomy jar deployed. Restart Paper servers after related config patches."

sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
