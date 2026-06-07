#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$PROJECT_DIR"

if ! command -v mvn >/dev/null 2>&1; then
    echo "Maven is required. Install it with: sudo apt-get install -y maven"
    exit 1
fi

mvn -q -DskipTests package
mkdir -p build/libs
cp -f target/LeeSeolLobby-0.1.0.jar build/libs/LeeSeolLobby-0.1.0.jar

echo "Built build/libs/LeeSeolLobby-0.1.0.jar"
