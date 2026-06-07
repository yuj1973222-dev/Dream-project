#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolCombat.tar.gz"
BUILD_DIR="/tmp/LeeSeolCombat-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolCombat"
JAR="$PROJECT_DIR/target/LeeSeolCombat-0.1.0.jar"
CONFIG="$PROJECT_DIR/src/main/resources/config.yml"
STAMP="$(date +%Y-%m-%d_%H-%M-%S)"
CITIZENS_JAR="Citizens-2.0.42-b4186.jar"
CITIZENS_URL="https://ci.citizensnpcs.co/job/Citizens2/lastStableBuild/artifact/dist/target/${CITIZENS_JAR}"
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

sudo mkdir -p "$SURVIVAL/plugins" "$BACKUP_DIR"

if [ ! -f "$SURVIVAL/plugins/$CITIZENS_JAR" ]; then
  echo "Downloading Citizens: $CITIZENS_JAR"
  curl -fL -o "/tmp/$CITIZENS_JAR" "$CITIZENS_URL"
  for old in "$SURVIVAL"/plugins/Citizens*.jar; do
    [ -e "$old" ] || continue
    sudo cp -f "$old" "$BACKUP_DIR/$(basename "$old").before-leeseolcombat-$STAMP"
    sudo rm -f "$old"
  done
  sudo cp -f "/tmp/$CITIZENS_JAR" "$SURVIVAL/plugins/$CITIZENS_JAR"
  rm -f "/tmp/$CITIZENS_JAR"
fi

if [ -f "$SURVIVAL/plugins/LeeSeolCombat-0.1.0.jar" ]; then
  sudo cp -f "$SURVIVAL/plugins/LeeSeolCombat-0.1.0.jar" \
    "$BACKUP_DIR/LeeSeolCombat-0.1.0-$STAMP.jar"
fi
if [ -f "$SURVIVAL/plugins/LeeSeolCombat/config.yml" ]; then
  sudo cp -f "$SURVIVAL/plugins/LeeSeolCombat/config.yml" \
    "$SURVIVAL/plugins/LeeSeolCombat/config.yml.before-$STAMP"
fi

sudo mkdir -p "$SURVIVAL/plugins/LeeSeolCombat"
sudo cp -f "$JAR" "$SURVIVAL/plugins/LeeSeolCombat-0.1.0.jar"
sudo cp -f "$CONFIG" "$SURVIVAL/plugins/LeeSeolCombat/config.yml"
sudo chown yuj1973222:yuj1973222 \
  "$SURVIVAL/plugins/LeeSeolCombat-0.1.0.jar" \
  "$SURVIVAL/plugins/LeeSeolCombat/config.yml" \
  "$SURVIVAL/plugins/$CITIZENS_JAR"

sudo systemctl restart minecraft
sleep 8

sudo systemctl status minecraft --no-pager
sudo journalctl -u minecraft --since "2 minutes ago" --no-pager \
  | grep -Ei "Citizens|LeeSeolCombat|Done|ERROR|Exception|Could not load" || true

sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
