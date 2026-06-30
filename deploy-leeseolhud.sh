#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolHUD.tar.gz"
BUILD_DIR="/tmp/LeeSeolHUD-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolHUD"
JAR="$PROJECT_DIR/target/LeeSeolHUD-0.1.0.jar"
CONFIG="$PROJECT_DIR/src/main/resources/config.yml"
SURVIVAL="/opt/minecraft/server"
BACKUP_DIR="/opt/minecraft/backups"
STAMP="$(date +%Y-%m-%d_%H-%M-%S)"

if [ ! -f "$ARCHIVE" ]; then
  echo "Missing source archive: $ARCHIVE"
  exit 1
fi

sudo rm -rf "$BUILD_DIR"
mkdir -p "$PROJECT_DIR"
tar -xzf "$ARCHIVE" -C "$PROJECT_DIR"
cd "$PROJECT_DIR"
mvn -q -DskipTests package

if [ ! -f "$JAR" ]; then
  echo "Build jar not found: $JAR"
  exit 1
fi

jar tf "$JAR" | grep -q '^plugin.yml$'

sudo mkdir -p "$SURVIVAL/plugins" "$BACKUP_DIR"
if [ -f "$SURVIVAL/plugins/LeeSeolHUD-0.1.0.jar" ]; then
  sudo cp -f "$SURVIVAL/plugins/LeeSeolHUD-0.1.0.jar" \
    "$BACKUP_DIR/LeeSeolHUD-0.1.0-$STAMP.jar"
fi
if [ -d "$SURVIVAL/plugins/LeeSeolHUD" ]; then
  sudo tar -czf "$BACKUP_DIR/LeeSeolHUD-config-$STAMP.tar.gz" \
    -C "$SURVIVAL/plugins" LeeSeolHUD
fi

sudo mkdir -p "$SURVIVAL/plugins/LeeSeolHUD"
sudo cp -f "$JAR" "$SURVIVAL/plugins/LeeSeolHUD-0.1.0.jar"
sudo cp -f "$CONFIG" "$SURVIVAL/plugins/LeeSeolHUD/config.yml"
sudo chown yuj1973222:yuj1973222 \
  "$SURVIVAL/plugins/LeeSeolHUD-0.1.0.jar" \
  "$SURVIVAL/plugins/LeeSeolHUD/config.yml"

sudo systemctl restart minecraft
sleep 8

sudo systemctl status minecraft --no-pager
sudo journalctl -u minecraft --since "2 minutes ago" --no-pager \
  | grep -Ei "LeeSeolHUD|Done|ERROR|Exception|Could not load" || true

sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
