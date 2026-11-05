@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

echo ==================================================================
echo   Starte jdmx (JavaFX 21 LTS)
echo ==================================================================

set "JAR_FILE="
for /f "delims=" %%f in ('dir /b /o-d "target\jdmx-*-all.jar" 2^>nul') do (
    if not defined JAR_FILE set "JAR_FILE=target\%%f"
)

if defined JAR_FILE (
    java -jar "!JAR_FILE!" %*
) else (
    echo Fat JAR nicht gefunden. Starte via Maven exec:java...
    call mvn exec:java -Dexec.args="%*"
)
