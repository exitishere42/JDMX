#!/usr/bin/env bash
set -e

echo "=================================================================="
echo "  JDMX - Continuous Snapshot Release Publisher"
echo "=================================================================="

echo "[*] Erstelle Fat-JAR..."
mvn clean package -DskipTests

JAR_PATH=$(ls -t target/jdmx-*-all.jar 2>/dev/null | head -n 1)
if [ -z "$JAR_PATH" ] || [ ! -f "$JAR_PATH" ]; then
    echo "[!] Fat-JAR nicht in target/ gefunden."
    exit 1
fi

cp "$JAR_PATH" "jdmx-snapshot-all.jar"

echo "[*] Aktualisiere GitHub Snapshot Release (Tag: snapshot)..."
gh release delete snapshot --yes 2>/dev/null || true
git tag -d snapshot 2>/dev/null || true
git push origin :refs/tags/snapshot 2>/dev/null || true

gh release create snapshot \
    "jdmx-snapshot-all.jar" \
    "$JAR_PATH" \
    "install.bat" \
    "install.sh" \
    "uninstall.bat" \
    "uninstall.sh" \
    "update-snapshot.bat" \
    "update-snapshot.sh" \
    --title "JDMX Continuous Snapshot" \
    --notes "Automated rolling development snapshot for stage and testing devices." \
    --prerelease

echo "[OK] Snapshot Release erfolgreich auf GitHub veröffentlicht!"
