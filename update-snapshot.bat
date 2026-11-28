@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

set "REPO=exitishere42/JDMX"
set "INSTALL_DIR=%USERPROFILE%\jdmx"
set "SNAPSHOT_URL=https://github.com/%REPO%/releases/download/snapshot/jdmx-snapshot-all.jar"

echo ==================================================================
echo   jdmx - Schnelles Snapshot-Update (Dev Channel)
echo   Zielordner: %INSTALL_DIR%
echo ==================================================================

if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

echo [*] Lade neuesten jdmx Snapshot herunter...
powershell -NoProfile -Command ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "try { " ^
    "    Invoke-WebRequest -Uri '%SNAPSHOT_URL%' -OutFile '%INSTALL_DIR%\jdmx-snapshot-temp.jar'; " ^
    "    Move-Item -Path '%INSTALL_DIR%\jdmx-snapshot-temp.jar' -Destination '%INSTALL_DIR%\jdmx.jar' -Force; " ^
    "    Write-Host '[OK] Snapshot erfolgreich heruntergeladen und aktualisiert!' -ForegroundColor Green; " ^
    "} catch { " ^
    "    Write-Host ('[!] Fehler beim Herunterladen des Snapshots: ' + $_.Exception.Message) -ForegroundColor Red; " ^
    "    exit 1; " ^
    "}"

if %ERRORLEVEL% NEQ 0 (
    echo [!] Update fehlgeschlagen.
    pause
    exit /b 1
)

echo [*] Starte aktualisiertes jdmx...
start "" javaw -jar "%INSTALL_DIR%\jdmx.jar"
timeout /t 2 >nul
exit /b 0
