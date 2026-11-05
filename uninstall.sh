#!/usr/bin/env bash
# ==============================================================================
# jdmx - Automatischer Linux Uninstaller
# Entfernt jdmx, den Ordner ~/jdmx sowie den Desktop-Eintrag und Dash-Icons.
# ==============================================================================

set -e

INSTALL_DIR="${INSTALL_DIR:-$HOME/jdmx}"
APPS_DIR="$HOME/.local/share/applications"
ICON_DIR_PNG="$HOME/.local/share/icons/hicolor/256x256/apps"
ICON_DIR_SVG="$HOME/.local/share/icons/hicolor/scalable/apps"

echo "=================================================================="
echo "  jdmx - Linux Uninstaller"
echo "=================================================================="

# 1. Desktop-Entry und Icons entfernen
if [ -f "$APPS_DIR/jdmx.desktop" ]; then
    echo "[*] Entferne Desktop-Entry..."
    rm -f "$APPS_DIR/jdmx.desktop"
fi

if [ -f "$ICON_DIR_PNG/jdmx.png" ]; then
    echo "[*] Entferne PNG-Icon..."
    rm -f "$ICON_DIR_PNG/jdmx.png"
fi

if [ -f "$ICON_DIR_SVG/jdmx.svg" ]; then
    echo "[*] Entferne SVG-Icon..."
    rm -f "$ICON_DIR_SVG/jdmx.svg"
fi

# Caches aktualisieren
if command -v update-desktop-database &> /dev/null; then
    update-desktop-database "$APPS_DIR" &> /dev/null || true
fi
if command -v gtk-update-icon-cache &> /dev/null; then
    gtk-update-icon-cache -f -t "$HOME/.local/share/icons/hicolor" &> /dev/null || true
fi

# 2. Installationsordner entfernen
if [ -d "$INSTALL_DIR" ]; then
    echo "[*] Entferne Programmordner: $INSTALL_DIR"
    rm -rf "$INSTALL_DIR"
else
    echo "[i] Programmordner $INSTALL_DIR wurde bereits entfernt."
fi

echo "=================================================================="
echo "  [OK] jdmx wurde vollstaendig deinstalliert!"
echo "=================================================================="
