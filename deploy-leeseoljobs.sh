#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolJobs.tar.gz"
BUILD_DIR="/tmp/LeeSeolJobs-build"
PROJECT_NAME="LeeSeolJobs"
STAMP="$(date +%Y-%m-%d_%H-%M-%S)"
SURVIVAL="/opt/minecraft/server"
BACKUP_DIR="/opt/minecraft/backups"
SHARED_DIR="/opt/minecraft/shared/jobs"

if [ ! -f "$ARCHIVE" ]; then
  echo "Missing source archive: $ARCHIVE"
  exit 1
fi

sudo rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
tar -xzf "$ARCHIVE" -C "$BUILD_DIR"
PROJECT_DIR="$BUILD_DIR/$PROJECT_NAME"
if [ ! -d "$PROJECT_DIR" ]; then
  PROJECT_DIR="$BUILD_DIR"
fi
JAR="$PROJECT_DIR/target/LeeSeolJobs-0.1.0.jar"
CONFIG="$PROJECT_DIR/src/main/resources/config.yml"
cd "$PROJECT_DIR"
mvn -q -DskipTests package

if [ ! -f "$JAR" ]; then
  echo "Build jar not found: $JAR"
  exit 1
fi

jar tf "$JAR" | grep -q '^plugin.yml$'

sudo mkdir -p "$SURVIVAL/plugins/LeeSeolJobs" "$BACKUP_DIR" "$SHARED_DIR"

if [ -f "$SURVIVAL/plugins/LeeSeolJobs-0.1.0.jar" ]; then
  sudo cp -f "$SURVIVAL/plugins/LeeSeolJobs-0.1.0.jar" \
    "$BACKUP_DIR/LeeSeolJobs-0.1.0-$STAMP.jar"
fi
if [ -f "$SURVIVAL/plugins/LeeSeolJobs/config.yml" ]; then
  sudo cp -f "$SURVIVAL/plugins/LeeSeolJobs/config.yml" \
    "$BACKUP_DIR/LeeSeolJobs-config-$STAMP.yml"
fi

sudo cp -f "$JAR" "$SURVIVAL/plugins/LeeSeolJobs-0.1.0.jar"
sudo cp -f "$CONFIG" "$SURVIVAL/plugins/LeeSeolJobs/config.yml"
sudo chown yuj1973222:yuj1973222 \
  "$SURVIVAL/plugins/LeeSeolJobs-0.1.0.jar" \
  "$SURVIVAL/plugins/LeeSeolJobs/config.yml"
sudo chown -R yuj1973222:yuj1973222 "$SURVIVAL/plugins/LeeSeolJobs" "$SHARED_DIR"

sudo systemctl restart minecraft
sleep 12

echo "== service =="
systemctl is-active minecraft
echo "== port =="
ss -ltnp | grep -E ':25566' || true
echo "== logs =="
sudo journalctl -u minecraft --since "3 minutes ago" --no-pager \
  | grep -Ei "LeeSeolJobs|Done|ERROR|Exception|Could not load" || true
echo "== deployed =="
ls -lh "$SURVIVAL/plugins/LeeSeolJobs-0.1.0.jar" \
  "$SURVIVAL/plugins/LeeSeolJobs/config.yml"

sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
