@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

echo ==================================================================
echo   JDMX - Continuous Snapshot Release Publisher
echo ==================================================================

echo [*] Erstelle Fat-JAR...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [!] Build fehlgeschlagen.
    exit /b 1
)

for %%f in (target\jdmx-*-all.jar) do (
    set "JAR_PATH=%%f"
)

if not exist "!JAR_PATH!" (
    echo [!] Fat-JAR nicht in target/ gefunden.
    exit /b 1
)

copy /y "!JAR_PATH!" "jdmx-snapshot-all.jar" >nul

echo [*] Aktualisiere GitHub Snapshot Release (Tag: snapshot)...
gh release delete snapshot --yes >nul 2>&1
git tag -d snapshot >nul 2>&1
git push origin :refs/tags/snapshot >nul 2>&1

gh release create snapshot ^
    "jdmx-snapshot-all.jar" ^
    "!JAR_PATH!" ^
    "install.bat" ^
    "install.sh" ^
    "uninstall.bat" ^
    "uninstall.sh" ^
    "update-snapshot.bat" ^
    "update-snapshot.sh" ^
    --title "JDMX Continuous Snapshot" ^
    --notes "Automated rolling development snapshot for stage and testing devices." ^
    --prerelease

if %ERRORLEVEL% EQU 0 (
    echo [OK] Snapshot Release erfolgreich auf GitHub verÃ¶ffentlicht!
) else (
    echo [!] Fehler beim VerÃ¶ffentlichen des GitHub Snapshot Releases.
)
