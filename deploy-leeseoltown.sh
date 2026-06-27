#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolTown.tar.gz"
BUILD_DIR="/tmp/LeeSeolTown-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolTown"
JAR="$PROJECT_DIR/target/LeeSeolTown-0.1.0.jar"
CONFIG="$PROJECT_DIR/src/main/resources/config.yml"
STAMP="$(date +%Y-%m-%d_%H-%M-%S)"

if [ ! -f "$JAR" ]; then
  if [ ! -f "$ARCHIVE" ]; then
    echo "Missing build jar and source archive."
    exit 1
  fi

  sudo rm -rf "$BUILD_DIR"
  mkdir -p "$BUILD_DIR"
  tar -xzf "$ARCHIVE" -C "$BUILD_DIR"
  cd "$PROJECT_DIR"
  mvn -q -DskipTests package
fi

if [ ! -f "$JAR" ]; then
  echo "Build jar not found: $JAR"
  exit 1
fi

sudo mkdir -p /opt/minecraft/shared/town /opt/minecraft/backups

sudo systemctl stop minecraft

for dir in /opt/minecraft/server; do
  name="$(basename "$dir")"
  sudo mkdir -p "$dir/plugins"

  if [ -f "$dir/plugins/LeeSeolTown-0.1.0.jar" ]; then
    sudo cp -f "$dir/plugins/LeeSeolTown-0.1.0.jar" \
      "/opt/minecraft/backups/LeeSeolTown-0.1.0-${name}-${STAMP}.jar"
  fi

  sudo cp -f "$JAR" "$dir/plugins/LeeSeolTown-0.1.0.jar"
  sudo chown yuj1973222:yuj1973222 "$dir/plugins/LeeSeolTown-0.1.0.jar"

  if [ -f "$CONFIG" ]; then
    sudo mkdir -p "$dir/plugins/LeeSeolTown"
    if [ -f "$dir/plugins/LeeSeolTown/config.yml" ]; then
      sudo cp -f "$dir/plugins/LeeSeolTown/config.yml" \
        "/opt/minecraft/backups/LeeSeolTown-config-${name}-${STAMP}.yml"
    fi
    sudo cp -f "$CONFIG" "$dir/plugins/LeeSeolTown/config.yml"
    sudo chown yuj1973222:yuj1973222 "$dir/plugins/LeeSeolTown/config.yml"
  fi
done

sudo systemctl start minecraft

sleep 90

echo "== services =="
systemctl is-active velocity minecraft

echo "== survival LeeSeolTown logs =="
sudo journalctl -u minecraft -n 220 --no-pager \
  | grep -Ei 'LeeSeolTown|Done|error|exception|Could not load' \
  | tail -100

echo "== deployed jars =="
ls -lh /opt/minecraft/server/plugins/LeeSeolTown-0.1.0.jar

echo "== cleanup =="
sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
