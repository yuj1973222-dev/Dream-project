#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolCore.tar.gz"
BUILD_DIR="/tmp/LeeSeolCore-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolCore"
JAR="$PROJECT_DIR/build/libs/LeeSeolCore-0.1.0.jar"
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
  bash scripts/build-with-paper-jar.sh
fi

if [ ! -f "$JAR" ]; then
  echo "Build jar not found: $JAR"
  exit 1
fi

TARGETS=(/opt/minecraft/server /opt/minecraft/lobby)

for dir in "${TARGETS[@]}"; do
  name="$(basename "$dir")"
  sudo mkdir -p "$dir/plugins" /opt/minecraft/backups

  if [ -f "$dir/plugins/LeeSeolCore-0.1.0.jar" ]; then
    sudo cp -f "$dir/plugins/LeeSeolCore-0.1.0.jar" \
      "/opt/minecraft/backups/LeeSeolCore-0.1.0-${name}-${STAMP}.jar"
  fi

  sudo cp -f "$JAR" "$dir/plugins/LeeSeolCore-0.1.0.jar"
  sudo chown yuj1973222:yuj1973222 "$dir/plugins/LeeSeolCore-0.1.0.jar"

  sudo mkdir -p "$dir/plugins/LeeSeolCore"
  if [ -f "$dir/plugins/LeeSeolCore/config.yml" ]; then
    sudo cp -f "$dir/plugins/LeeSeolCore/config.yml" \
      "$dir/plugins/LeeSeolCore/config.yml.before-dimension-gate-$STAMP"
  fi

  mode="disabled"
  if [ "$name" = "server" ]; then
    mode="survival"
  elif [ "$name" = "lobby" ]; then
    mode="lobby"
  fi

  if [ -f "$dir/plugins/LeeSeolCore/config.yml" ]; then
    sudo python3 - "$dir/plugins/LeeSeolCore/config.yml" "$mode" <<'PY'
from pathlib import Path
import re
import sys

path = Path(sys.argv[1])
mode = sys.argv[2]
enabled = "true" if mode in {"survival", "lobby"} else "false"
text = path.read_text(encoding="utf-8")

block = f'''dimension-gate:
  enabled: {enabled}
  mode: "{mode}"
  bypass-permission: "leeseolcore.dimension.bypass"
  survival:
    main-worlds:
      - "world"
    nether-worlds:
      - "world_nether"
    dungeon-worlds:
      - "dungeon"
  messages:
    lobby-blocked: "&c로비에서는 다른 차원으로 이동할 수 없습니다."
    end-blocked: "&c서바이벌에서는 엔더 차원으로 이동할 수 없습니다."
    dungeon-blocked: "&c던전 월드에서는 다른 차원으로 이동할 수 없습니다."
    nether-blocked: "&c이 월드에서는 네더 차원으로 이동할 수 없습니다."

'''

text = re.sub(r'(?ms)^dimension-gate:\n(?:^[ \t].*\n|^\s*$)*', '', text)
if "portal-triggers:" in text:
    text = text.replace("portal-triggers:", block + "portal-triggers:", 1)
else:
    text = text.rstrip() + "\n\n" + block
path.write_text(text, encoding="utf-8")
PY
    sudo chown yuj1973222:yuj1973222 "$dir/plugins/LeeSeolCore/config.yml"
  fi
done

SERVICES=(minecraft lobby)

sudo systemctl restart "${SERVICES[@]}"

sleep 120

echo "== services =="
systemctl is-active velocity "${SERVICES[@]}"

echo "== survival LeeSeolCore logs =="
sudo journalctl -u minecraft -n 240 --no-pager \
  | grep -Ei 'LeeSeolCore|PortalTrigger|LaunchPad|Done|error|exception' \
  | tail -100

echo "== lobby LeeSeolCore logs =="
sudo journalctl -u lobby -n 240 --no-pager \
  | grep -Ei 'LeeSeolCore|PortalTrigger|LaunchPad|Done|error|exception' \
  | tail -100

echo "== deployed jars =="
for dir in "${TARGETS[@]}"; do
  ls -lh "$dir/plugins/LeeSeolCore-0.1.0.jar"
done

echo "== cleanup =="
sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
