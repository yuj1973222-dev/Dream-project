#!/usr/bin/env bash
set -euo pipefail

ARCHIVE="/tmp/LeeSeolDungeon.tar.gz"
BUILD_DIR="/tmp/LeeSeolDungeon-build"
PROJECT_DIR="$BUILD_DIR/LeeSeolDungeon"
JAR="$PROJECT_DIR/target/LeeSeolDungeon-0.1.0.jar"
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

sudo mkdir -p /opt/minecraft/shared/dungeon /opt/minecraft/backups
sudo chown -R yuj1973222:yuj1973222 /opt/minecraft/shared/dungeon

declare -A TARGETS
TARGETS["/opt/minecraft/server"]="minecraft"

for dir in "${!TARGETS[@]}"; do
  service="${TARGETS[$dir]}"
  sudo mkdir -p "$dir/plugins"
  if [ -f "$dir/plugins/LeeSeolDungeon-0.1.0.jar" ]; then
    sudo cp -f "$dir/plugins/LeeSeolDungeon-0.1.0.jar" \
      "/opt/minecraft/backups/LeeSeolDungeon-0.1.0-${service}-${STAMP}.jar"
  fi
  sudo cp -f "$JAR" "$dir/plugins/LeeSeolDungeon-0.1.0.jar"
  sudo chown yuj1973222:yuj1973222 "$dir/plugins/LeeSeolDungeon-0.1.0.jar"

  sudo mkdir -p "$dir/plugins/LeeSeolDungeon"
  if [ -f "$dir/plugins/LeeSeolDungeon/config.yml" ]; then
    sudo cp -f "$dir/plugins/LeeSeolDungeon/config.yml" \
      "$dir/plugins/LeeSeolDungeon/config.yml.before-multiworld-$STAMP"
  fi
  sudo cp -f "$CONFIG" "$dir/plugins/LeeSeolDungeon/config.yml"
  role="both"
  sudo python3 - "$dir/plugins/LeeSeolDungeon/config.yml" "$role" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
role = sys.argv[2]
text = path.read_text(encoding="utf-8") if path.exists() else ""
if "server:" not in text:
    text = 'server:\n  role: "' + role + '"\n\n' + text
else:
    lines = text.splitlines()
    output = []
    in_server = False
    wrote = False
    for line in lines:
        if line.startswith("server:"):
            in_server = True
            wrote = False
            output.append(line)
            continue
        if in_server and line and not line.startswith(" "):
            if not wrote:
                output.append(f'  role: "{role}"')
            in_server = False
        if in_server and line.strip().startswith("role:"):
            output.append(f'  role: "{role}"')
            wrote = True
            continue
        output.append(line)
    if in_server and not wrote:
        output.append(f'  role: "{role}"')
    text = "\n".join(output) + "\n"
path.write_text(text, encoding="utf-8")
PY
  sudo chown yuj1973222:yuj1973222 "$dir/plugins/LeeSeolDungeon/config.yml"
done

for service in "${TARGETS[@]}"; do
  if systemctl list-unit-files "$service.service" --no-pager --no-legend | grep -q "$service.service"; then
    sudo systemctl restart "$service"
  fi
done

sleep 80

echo "== services =="
for service in velocity "${TARGETS[@]}"; do
  if systemctl list-unit-files "$service.service" --no-pager --no-legend | grep -q "$service.service"; then
    echo "$service: $(systemctl is-active "$service")"
  fi
done

echo "== LeeSeolDungeon logs =="
for service in "${TARGETS[@]}"; do
  echo "-- $service --"
  sudo journalctl -u "$service" -n 220 --no-pager \
    | grep -Ei 'LeeSeolDungeon|Done|error|exception|Could not load' \
    | tail -100 || true
done

echo "== deployed jars =="
for dir in "${!TARGETS[@]}"; do
  ls -lh "$dir/plugins/LeeSeolDungeon-0.1.0.jar"
done

sudo rm -rf "$BUILD_DIR"
rm -f "$ARCHIVE"
