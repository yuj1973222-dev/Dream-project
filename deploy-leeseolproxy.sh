#!/usr/bin/env bash
set -euo pipefail

sudo rm -rf /tmp/leeseolproxy-build
mkdir -p /tmp/leeseolproxy-build
cd /tmp/leeseolproxy-build

unzip -oq /tmp/LeeSeolProxy.zip || {
    code=$?
    if [ "$code" -ne 1 ]; then
        exit "$code"
    fi
}
sudo chown -R "$(whoami):$(whoami)" LeeSeolProxy
find LeeSeolProxy -type d -exec chmod 755 {} \;
find LeeSeolProxy -type f -exec chmod 644 {} \;
chmod +x LeeSeolProxy/scripts/build-on-server.sh
cd LeeSeolProxy

echo "== Build =="
bash scripts/build-on-server.sh

echo "== Jar metadata =="
jar tf build/libs/LeeSeolProxy-0.1.0.jar | grep velocity-plugin.json

echo "== Deploy =="
sudo mkdir -p /opt/minecraft/velocity/plugins
sudo find /opt/minecraft/velocity/plugins -maxdepth 1 -type f -iname 'LeeSeolProxy*.jar' -delete
sudo cp -f build/libs/LeeSeolProxy-0.1.0.jar /opt/minecraft/velocity/plugins/
sudo systemctl restart velocity

echo "== Velocity status =="
sudo systemctl is-active velocity

echo "== Recent LeeSeolProxy logs =="
sudo journalctl -u velocity -n 160 --no-pager | grep -Ei "LeeSeolProxy|error|exception" || true

echo "== Cleanup =="
sudo find /opt/minecraft/server/plugins -maxdepth 1 -type f -iname 'LeeSeolProxy*.jar' -delete
sudo find /opt/minecraft/velocity/plugins -maxdepth 1 -type f -iname 'LeeSeolCore*.jar' -delete
sudo find /home -maxdepth 2 -type f \( -iname 'LeeSeolProxy*.zip' -o -iname 'LeeSeolCore*.zip' \) -delete || true
sudo find /home -maxdepth 2 -type d -name 'LeeSeolProxy' -exec rm -rf {} + || true
sudo rm -f /tmp/LeeSeolProxy.zip /tmp/deploy-leeseolproxy.sh
sudo rm -rf /tmp/leeseolproxy-build

echo "== Plugin layout =="
echo "Paper plugins:"
ls -1 /opt/minecraft/server/plugins | grep -Ei 'LeeSeol(Core|Proxy)' || true
echo "Velocity plugins:"
ls -1 /opt/minecraft/velocity/plugins | grep -Ei 'LeeSeol(Core|Proxy)' || true
