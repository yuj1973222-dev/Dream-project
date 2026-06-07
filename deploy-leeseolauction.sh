#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolAuction.tar.gz"
BUILD_DIR="/tmp/LeeSeolAuction-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolAuction"
JAR="$PROJECT_DIR/target/LeeSeolAuction-0.1.0.jar"
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

sudo mkdir -p /opt/minecraft/shared/auction /opt/minecraft/backups
sudo chown -R yuj1973222:yuj1973222 /opt/minecraft/shared/auction

for dir in /opt/minecraft/server /opt/minecraft/lobby /opt/minecraft/dungeon; do
  if [ ! -d "$dir" ]; then
    continue
  fi
  name="$(basename "$dir")"
  sudo mkdir -p "$dir/plugins/LeeSeolAuction"

  if [ -f "$dir/plugins/LeeSeolAuction-0.1.0.jar" ]; then
    sudo cp -f "$dir/plugins/LeeSeolAuction-0.1.0.jar" \
      "/opt/minecraft/backups/LeeSeolAuction-0.1.0-${name}-${STAMP}.jar"
  fi
  if [ -f "$dir/plugins/LeeSeolAuction/config.yml" ]; then
    sudo cp -f "$dir/plugins/LeeSeolAuction/config.yml" \
      "$dir/plugins/LeeSeolAuction/config.yml.before-multiworld-$STAMP"
  fi

  sudo cp -f "$JAR" "$dir/plugins/LeeSeolAuction-0.1.0.jar"
  sudo cp -f "$CONFIG" "$dir/plugins/LeeSeolAuction/config.yml"
  sudo chown yuj1973222:yuj1973222 \
    "$dir/plugins/LeeSeolAuction-0.1.0.jar" \
    "$dir/plugins/LeeSeolAuction/config.yml"
done

echo "LeeSeolAuction jar and config deployed. Restart Paper servers to apply."

sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
