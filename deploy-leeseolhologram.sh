#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolHologram.tar.gz"
BUILD_DIR="/tmp/LeeSeolHologram-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolHologram"
JAR="$PROJECT_DIR/target/LeeSeolHologram-0.1.0.jar"
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

for dir in /opt/minecraft/server /opt/minecraft/lobby; do
  name="$(basename "$dir")"
  sudo mkdir -p "$dir/plugins"

  if [ -f "$dir/plugins/LeeSeolHologram-0.1.0.jar" ]; then
    sudo cp -f "$dir/plugins/LeeSeolHologram-0.1.0.jar" \
      "/opt/minecraft/backups/LeeSeolHologram-0.1.0-${name}-${STAMP}.jar"
  fi

  sudo cp -f "$JAR" "$dir/plugins/LeeSeolHologram-0.1.0.jar"
  sudo chown yuj1973222:yuj1973222 "$dir/plugins/LeeSeolHologram-0.1.0.jar"
done

sudo systemctl restart minecraft lobby
sleep 90

echo "== services =="
systemctl is-active velocity minecraft lobby

echo "== survival LeeSeolHologram logs =="
sudo journalctl -u minecraft -n 220 --no-pager \
  | grep -Ei 'LeeSeolHologram|Done|error|exception|Could not load' \
  | tail -100

echo "== lobby LeeSeolHologram logs =="
sudo journalctl -u lobby -n 220 --no-pager \
  | grep -Ei 'LeeSeolHologram|Done|error|exception|Could not load' \
  | tail -100

echo "== deployed jars =="
ls -lh /opt/minecraft/server/plugins/LeeSeolHologram-0.1.0.jar \
  /opt/minecraft/lobby/plugins/LeeSeolHologram-0.1.0.jar

echo "== cleanup =="
sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
