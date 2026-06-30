#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolCleanup.tar.gz"
BUILD_DIR="/tmp/LeeSeolCleanup-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolCleanup"
JAR="$PROJECT_DIR/target/LeeSeolCleanup-0.1.0.jar"
CONFIG="$PROJECT_DIR/src/main/resources/config.yml"
STAMP="$(date +%Y-%m-%d_%H-%M-%S)"
SURVIVAL="/opt/minecraft/server"
BACKUP_DIR="/opt/minecraft/backups"

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

sudo mkdir -p "$SURVIVAL/plugins/LeeSeolCleanup" "$BACKUP_DIR"

if [ -f "$SURVIVAL/plugins/LeeSeolCleanup-0.1.0.jar" ]; then
  sudo cp -f "$SURVIVAL/plugins/LeeSeolCleanup-0.1.0.jar" \
    "$BACKUP_DIR/LeeSeolCleanup-0.1.0-$STAMP.jar"
fi
if [ -f "$SURVIVAL/plugins/LeeSeolCleanup/config.yml" ]; then
  sudo cp -f "$SURVIVAL/plugins/LeeSeolCleanup/config.yml" \
    "$SURVIVAL/plugins/LeeSeolCleanup/config.yml.before-$STAMP"
fi

sudo cp -f "$JAR" "$SURVIVAL/plugins/LeeSeolCleanup-0.1.0.jar"
sudo cp -f "$CONFIG" "$SURVIVAL/plugins/LeeSeolCleanup/config.yml"
sudo chown yuj1973222:yuj1973222 \
  "$SURVIVAL/plugins/LeeSeolCleanup-0.1.0.jar" \
  "$SURVIVAL/plugins/LeeSeolCleanup/config.yml"

sudo systemctl restart minecraft
sleep 8

sudo systemctl status minecraft --no-pager
sudo journalctl -u minecraft --since "2 minutes ago" --no-pager \
  | grep -Ei "LeeSeolCleanup|Done|ERROR|Exception|Could not load" || true

sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
