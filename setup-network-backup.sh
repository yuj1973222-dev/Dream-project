#!/usr/bin/env bash
set -euo pipefail

sudo tee /usr/local/bin/minecraft-backup > /dev/null <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="/opt/minecraft/backups"
KEEP_DAYS=7
KEEP_COUNT=2
MIN_FREE_MB=1200
STAMP="$(date +'%Y-%m-%d_%H-%M-%S')"
ARCHIVE="$BACKUP_DIR/network-$STAMP.tar.gz"
RUNNING_SERVICES=()

was_running() {
  local wanted="$1"
  for service in "${RUNNING_SERVICES[@]}"; do
    if [ "$service" = "$wanted" ]; then
      return 0
    fi
  done
  return 1
}

restart_services() {
  if was_running "minecraft"; then
    systemctl start minecraft || true
  fi

  if was_running "lobby"; then
    systemctl start lobby || true
  fi

  if was_running "minecraft" || was_running "lobby"; then
    sleep 20
  fi

  if was_running "velocity"; then
    systemctl start velocity || true
  fi
}

trap restart_services EXIT

mkdir -p "$BACKUP_DIR"

prune_network_backups() {
  local keep="$1"
  if [ "$keep" -lt 1 ]; then
    keep=1
  fi

  ls -1t "$BACKUP_DIR"/network-*.tar.gz 2>/dev/null | tail -n +"$((keep + 1))" | xargs -r rm -f
}

find "$BACKUP_DIR" -name 'network-*.tar.gz' -type f -mtime +"$KEEP_DAYS" -delete
prune_network_backups "$((KEEP_COUNT - 1))"

FREE_MB="$(df -Pm "$BACKUP_DIR" | awk 'NR == 2 {print $4}')"
if [ "${FREE_MB:-0}" -lt "$MIN_FREE_MB" ]; then
  echo "Not enough free disk for backup. Free=${FREE_MB}MB, required=${MIN_FREE_MB}MB" >&2
  exit 1
fi

for service in velocity lobby minecraft; do
  if systemctl is-active --quiet "$service"; then
    RUNNING_SERVICES+=("$service")
  fi
done

systemctl stop velocity || true
systemctl stop lobby || true
systemctl stop minecraft || true
sleep 5

tar -C / -czf "$ARCHIVE" \
  opt/minecraft/server \
  opt/minecraft/lobby \
  opt/minecraft/velocity \
  etc/systemd/system/minecraft.service \
  etc/systemd/system/lobby.service \
  etc/systemd/system/velocity.service \
  etc/systemd/system/minecraft-backup.service \
  etc/systemd/system/minecraft-backup.timer

prune_network_backups "$KEEP_COUNT"

echo "Network backup created: $ARCHIVE"
EOF

sudo chmod +x /usr/local/bin/minecraft-backup

sudo tee /etc/systemd/system/minecraft-backup.service > /dev/null <<'EOF'
[Unit]
Description=Backup Minecraft Velocity network
Wants=network-online.target
After=network-online.target

[Service]
Type=oneshot
ExecStart=/usr/local/bin/minecraft-backup
EOF

sudo tee /etc/systemd/system/minecraft-backup.timer > /dev/null <<'EOF'
[Unit]
Description=Run Minecraft Velocity network backup daily

[Timer]
OnCalendar=*-*-* 04:00:00
Persistent=true
Unit=minecraft-backup.service

[Install]
WantedBy=timers.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now minecraft-backup.timer

echo "== timer =="
systemctl list-timers minecraft-backup.timer --no-pager

echo "== backup script =="
head -40 /usr/local/bin/minecraft-backup
