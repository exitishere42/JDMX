#!/usr/bin/env bash
set -e

REPO="exitishere42/JDMX"
INSTALL_DIR="${INSTALL_DIR:-$HOME/jdmx}"
SNAPSHOT_URL="https://github.com/$REPO/releases/download/snapshot/jdmx-snapshot-all.jar"

echo "=================================================================="
echo "  jdmx - Schnelles Snapshot-Update (Dev Channel)"
echo "  Zielordner: $INSTALL_DIR"
echo "=================================================================="

mkdir -p "$INSTALL_DIR"

echo "[*] Lade neuesten jdmx Snapshot herunter..."
if command -v curl &> /dev/null; then
    curl -fL --progress-bar -o "$INSTALL_DIR/jdmx-snapshot-temp.jar" "$SNAPSHOT_URL"
elif command -v wget &> /dev/null; then
    wget -q --show-progress -O "$INSTALL_DIR/jdmx-snapshot-temp.jar" "$SNAPSHOT_URL"
else
    echo "[!] Weder curl noch wget installiert."
    exit 1
fi

mv "$INSTALL_DIR/jdmx-snapshot-temp.jar" "$INSTALL_DIR/jdmx.jar"
echo "[OK] Snapshot erfolgreich aktualisiert!"

if command -v java &> /dev/null; then
    echo "[*] Starte aktualisiertes jdmx..."
    nohup java -jar "$INSTALL_DIR/jdmx.jar" >/dev/null 2>&1 &
fi
