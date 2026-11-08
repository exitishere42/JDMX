#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="/tmp/jdmx-appimage"
JAR_SRC=$(ls -t "$PROJECT_DIR"/target/jdmx-*-all.jar 2>/dev/null | head -n 1)

if [ -z "$JAR_SRC" ] || [ ! -f "$JAR_SRC" ]; then
    echo "[!] Fat JAR nicht gefunden in target/. Bitte zuerst 'mvn package' ausführen."
    exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/AppDir/usr/lib/jdmx"
mkdir -p "$BUILD_DIR/AppDir/usr/share/icons/hicolor/256x256/apps"
mkdir -p "$BUILD_DIR/AppDir/usr/share/applications"

cp "$JAR_SRC" "$BUILD_DIR/AppDir/usr/lib/jdmx/"
cp "$PROJECT_DIR/jdmx.png" "$BUILD_DIR/AppDir/jdmx.png"
cp "$PROJECT_DIR/jdmx.png" "$BUILD_DIR/AppDir/.DirIcon"
cp "$PROJECT_DIR/jdmx.png" "$BUILD_DIR/AppDir/usr/share/icons/hicolor/256x256/apps/jdmx.png"
cp "$PROJECT_DIR/jdmx.svg" "$BUILD_DIR/AppDir/jdmx.svg"

cat > "$BUILD_DIR/AppDir/jdmx.desktop" << 'EOF'
[Desktop Entry]
Type=Application
Name=jdmx
Comment=jdmx - Modular Java DMX Lighting Controller
Exec=AppRun
Icon=jdmx
Categories=AudioVideo;Utility;
Terminal=false
StartupNotify=true
StartupWMClass=de.exit.jdmx.JdmxApplication
EOF
cp "$BUILD_DIR/AppDir/jdmx.desktop" "$BUILD_DIR/AppDir/usr/share/applications/jdmx.desktop"

cat > "$BUILD_DIR/AppDir/AppRun" << 'EOF'
#!/usr/bin/env bash
HERE="$(dirname "$(readlink -f "${0}")")"
JAR_FILE=$(ls -t "$HERE"/usr/lib/jdmx/jdmx-*-all.jar 2>/dev/null | head -n 1)
if ! command -v java &> /dev/null; then
    echo "[!] Fehler: Java 21+ (java) wurde nicht gefunden. Bitte openjdk-21-jre installieren." >&2
    exit 1
fi
exec java -jar "$JAR_FILE" "$@"
EOF
chmod +x "$BUILD_DIR/AppDir/AppRun"

if [ -f "$PROJECT_DIR/.runtime-x86_64" ]; then
    cp "$PROJECT_DIR/.runtime-x86_64" "$BUILD_DIR/runtime-x86_64"
elif ! curl -fsSL -m 15 -o "$BUILD_DIR/runtime-x86_64" "https://github.com/AppImage/type2-runtime/releases/download/continuous/runtime-x86_64"; then
    if [ -f "$PROJECT_DIR/jdmx-x86_64.AppImage" ]; then
        echo "[*] Nutze Runtime aus bestehendem AppImage..."
        head -c 194183 "$PROJECT_DIR/jdmx-x86_64.AppImage" > "$BUILD_DIR/runtime-x86_64"
    else
        echo "[!] Fehler beim Herunterladen der AppImage-Runtime." >&2
        exit 1
    fi
fi
mksquashfs "$BUILD_DIR/AppDir" "$BUILD_DIR/app.squashfs" -root-owned -noappend -comp gzip > /dev/null
cat "$BUILD_DIR/runtime-x86_64" "$BUILD_DIR/app.squashfs" > "$PROJECT_DIR/jdmx-x86_64.AppImage"
chmod +x "$PROJECT_DIR/jdmx-x86_64.AppImage"
echo "[OK] AppImage erfolgreich erstellt: $PROJECT_DIR/jdmx-x86_64.AppImage"
