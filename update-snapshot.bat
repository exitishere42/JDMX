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
    "echo [*] Schließe eventuell laufende jdmx-Instanzen...
taskkill /F /FI "WINDOWTITLE eq jdmx*" /IM javaw.exe >nul 2>&1
timeout /t 1 /nobreak >nul

del /f /q "%INSTALL_DIR%\jdmx-*-all.jar" >nul 2>&1
move /y "%INSTALL_DIR%\jdmx-snapshot-temp.jar" "%INSTALL_DIR%\jdmx.jar" >nul
copy /y "%INSTALL_DIR%\jdmx.jar" "%INSTALL_DIR%\jdmx-snapshot-all.jar" >nul 2>&1 " ^
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
