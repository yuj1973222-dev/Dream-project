#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolRanks.tar.gz"
BUILD_DIR="/tmp/LeeSeolRanks-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolRanks"
JAR="$PROJECT_DIR/target/LeeSeolRanks-0.1.0.jar"
CONFIG="$PROJECT_DIR/src/main/resources/config.yml"
STAMP="$(date +%Y-%m-%d_%H-%M-%S)"
SURVIVAL="/opt/minecraft/server"
LOBBY="/opt/minecraft/lobby"
BACKUP_DIR="/opt/minecraft/backups"
SHARED_DIR="/opt/minecraft/shared/ranks"
SHARED_DATA="$SHARED_DIR/ranks.yml"

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

jar tf "$JAR" | grep -q 'plugin.yml'

sudo mkdir -p "$BACKUP_DIR" "$SHARED_DIR"
if [ ! -f "$SHARED_DATA" ]; then
  if [ -f "$SURVIVAL/plugins/LeeSeolRanks/ranks.yml" ]; then
    sudo cp -f "$SURVIVAL/plugins/LeeSeolRanks/ranks.yml" "$SHARED_DATA"
  elif [ -f "$LOBBY/plugins/LeeSeolRanks/ranks.yml" ]; then
    sudo cp -f "$LOBBY/plugins/LeeSeolRanks/ranks.yml" "$SHARED_DATA"
  fi
fi

python3 - "$CONFIG" "$SHARED_DATA" <<'PY'
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
shared = sys.argv[2]
text = path.read_text(encoding="utf-8")
text = text.replace('data-file: ""', f'data-file: "{shared}"')
path.write_text(text, encoding="utf-8")
PY

deploy_one() {
  local name="$1"
  local base="$2"

  sudo mkdir -p "$base/plugins/LeeSeolRanks"
  if [ -f "$base/plugins/LeeSeolRanks-0.1.0.jar" ]; then
    sudo cp -f "$base/plugins/LeeSeolRanks-0.1.0.jar" \
      "$BACKUP_DIR/LeeSeolRanks-0.1.0-${name}-${STAMP}.jar"
  fi
  if [ -f "$base/plugins/LeeSeolRanks/config.yml" ]; then
    sudo cp -f "$base/plugins/LeeSeolRanks/config.yml" \
      "$BACKUP_DIR/LeeSeolRanks-config-${name}-${STAMP}.yml"
  fi

  sudo cp -f "$JAR" "$base/plugins/LeeSeolRanks-0.1.0.jar"
  sudo cp -f "$CONFIG" "$base/plugins/LeeSeolRanks/config.yml"
  sudo chown yuj1973222:yuj1973222 \
    "$base/plugins/LeeSeolRanks-0.1.0.jar" \
    "$base/plugins/LeeSeolRanks/config.yml"
  sudo chown -R yuj1973222:yuj1973222 "$base/plugins/LeeSeolRanks"
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
  | grep -Ei "LeeSeolRanks|PlaceholderAPI|Done|ERROR|Exception|Could not load" || true
echo "== lobby logs =="
sudo journalctl -u lobby --since "3 minutes ago" --no-pager \
  | grep -Ei "LeeSeolRanks|PlaceholderAPI|Done|ERROR|Exception|Could not load" || true
echo "== deployed =="
ls -lh "$SURVIVAL/plugins/LeeSeolRanks-0.1.0.jar" \
  "$LOBBY/plugins/LeeSeolRanks-0.1.0.jar" \
  "$SHARED_DATA"

sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
