#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolQuest.tar.gz"
BUILD_DIR="/tmp/LeeSeolQuest-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolQuest"
JAR="$PROJECT_DIR/target/LeeSeolQuest-0.1.0.jar"
CONFIG="$PROJECT_DIR/src/main/resources/config.yml"
STAMP="$(date +%Y-%m-%d_%H-%M-%S)"
SURVIVAL="/opt/minecraft/server"
LOBBY="/opt/minecraft/lobby"
BACKUP_DIR="/opt/minecraft/backups"
SHARED_DIR="/opt/minecraft/shared/quests"

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

jar tf "$JAR" | grep -q '^plugin.yml$'

sudo mkdir -p "$BACKUP_DIR" "$SHARED_DIR"

deploy_one() {
  local name="$1"
  local base="$2"

  sudo mkdir -p "$base/plugins/LeeSeolQuest"
  if [ -f "$base/plugins/LeeSeolQuest-0.1.0.jar" ]; then
    sudo cp -f "$base/plugins/LeeSeolQuest-0.1.0.jar" \
      "$BACKUP_DIR/LeeSeolQuest-0.1.0-${name}-${STAMP}.jar"
  fi
  if [ -f "$base/plugins/LeeSeolQuest/config.yml" ]; then
    sudo cp -f "$base/plugins/LeeSeolQuest/config.yml" \
      "$BACKUP_DIR/LeeSeolQuest-config-${name}-${STAMP}.yml"
  fi

  sudo cp -f "$JAR" "$base/plugins/LeeSeolQuest-0.1.0.jar"
  sudo cp -f "$CONFIG" "$base/plugins/LeeSeolQuest/config.yml"
  sudo chown yuj1973222:yuj1973222 \
    "$base/plugins/LeeSeolQuest-0.1.0.jar" \
    "$base/plugins/LeeSeolQuest/config.yml"
  sudo chown -R yuj1973222:yuj1973222 "$base/plugins/LeeSeolQuest"
}

deploy_one survival "$SURVIVAL"
deploy_one lobby "$LOBBY"
sudo chown -R yuj1973222:yuj1973222 "$SHARED_DIR"

sudo systemctl restart minecraft lobby
sleep 12

echo "== services =="
systemctl is-active minecraft lobby
echo "== survival logs =="
sudo journalctl -u minecraft --since "3 minutes ago" --no-pager \
  | grep -Ei "LeeSeolQuest|Done|ERROR|Exception|Could not load" || true
echo "== lobby logs =="
sudo journalctl -u lobby --since "3 minutes ago" --no-pager \
  | grep -Ei "LeeSeolQuest|Done|ERROR|Exception|Could not load" || true
echo "== deployed =="
ls -lh "$SURVIVAL/plugins/LeeSeolQuest-0.1.0.jar" \
  "$LOBBY/plugins/LeeSeolQuest-0.1.0.jar" \
  "$SURVIVAL/plugins/LeeSeolQuest/config.yml" \
  "$LOBBY/plugins/LeeSeolQuest/config.yml"

sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
