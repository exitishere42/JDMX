#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=================================================================="
echo "  Starte jdmx (JavaFX 21 LTS)"
echo "=================================================================="

JAR_FILE=$(ls -t "$SCRIPT_DIR"/target/jdmx-*-all.jar "$SCRIPT_DIR"/jdmx-*-all.jar "$SCRIPT_DIR"/target/jdmx.jar 2>/dev/null | head -n 1)

if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then
    exec java -jar "$JAR_FILE" "$@"
else
    echo "Fat JAR nicht gefunden. Starte via Maven exec:java..."
    mvn exec:java -Dexec.args="$*"
fi
