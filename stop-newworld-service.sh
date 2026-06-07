#!/usr/bin/env bash
set -euo pipefail

SERVICE="newworld"

if systemctl list-unit-files "$SERVICE.service" --no-pager --no-legend | grep -q "^$SERVICE.service"; then
  sudo systemctl stop "$SERVICE"
  sudo systemctl disable "$SERVICE"
  sudo systemctl reset-failed "$SERVICE" || true
fi

echo "== newworld service =="
systemctl is-enabled "$SERVICE" 2>/dev/null || true
systemctl is-active "$SERVICE" 2>/dev/null || true

echo "== public services =="
systemctl is-active velocity minecraft lobby
