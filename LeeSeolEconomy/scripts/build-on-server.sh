#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is required. Install it with: sudo apt-get install -y maven" >&2
  exit 1
fi

if [ ! -f /opt/minecraft/server/plugins/Vault.jar ]; then
  echo "Vault jar not found: /opt/minecraft/server/plugins/Vault.jar" >&2
  exit 1
fi

mvn -q -DskipTests package
mkdir -p build/libs
cp -f target/LeeSeolEconomy-0.1.0.jar build/libs/LeeSeolEconomy-0.1.0.jar

echo "Built build/libs/LeeSeolEconomy-0.1.0.jar"
