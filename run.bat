@echo off
setlocal
cd /d "%~dp0"

echo ==================================================================
echo   Starte jdmx (JavaFX 21 LTS)
echo ==================================================================

if exist "target\jdmx-1.0.0-all.jar" (
    java -jar target\jdmx-1.0.0-all.jar %*
) else (
    echo Fat JAR nicht gefunden. Starte via Maven exec:java...
    call mvn exec:java -Dexec.args="%*"
)
