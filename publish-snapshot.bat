@echo off
setlocal
echo [*] Erstelle Fat-JAR...
call mvn clean package -DskipTests
if errorlevel 1 exit /b 1

for %%F in (target\jdmx-*-all.jar) do set "JAR=%%F"
copy /y "%JAR%" "jdmx-snapshot-all.jar" >nul

echo [*] Aktualisiere GitHub Snapshot Release...
gh release delete snapshot --yes >nul 2>&1
git tag -d snapshot >nul 2>&1
git push origin :refs/tags/snapshot >nul 2>&1

gh release create snapshot "jdmx-snapshot-all.jar" "%JAR%" install.bat install.sh uninstall.bat uninstall.sh update-snapshot.bat update-snapshot.sh --title "JDMX Continuous Snapshot" --notes "Automated rolling development snapshot for stage and testing devices." --prerelease
if errorlevel 1 (
    echo [!] Fehler beim Veroeffentlichen.
    exit /b 1
)
echo [OK] Snapshot Release erfolgreich aktualisiert!
