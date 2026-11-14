@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

set "REPO=exitishere42/JDMX"
set "INSTALL_DIR=%USERPROFILE%\jdmx"
set "RAW_URL=https://raw.githubusercontent.com/%REPO%/main"

echo ==================================================================
echo   jdmx - Windows Installer
echo   Zielordner: %INSTALL_DIR%
echo ==================================================================

:: ------------------------------------------------------------------------------
:: 1. Pruefen ob Java 21+ installiert ist (wenn ja -> skip, wenn nein -> installieren)
:: ------------------------------------------------------------------------------
set "JAVA_OK=0"
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%v in ('powershell -NoProfile -Command "$v = (java -version 2>&1 | Select-Object -First 1); if ($v -match '\"(\d+)') { [int]$Matches[1] } else { 0 }"') do (
        if %%v GEQ 21 set "JAVA_OK=1"
    )
)

if "%JAVA_OK%"=="1" (
    echo [OK] Java 21+ ist bereits installiert - ueberspringe Java-Installation.
) else (
    echo [*] Java 21+ nicht gefunden. Installiere neuestes OpenJDK 21 LTS...
    where winget >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        winget install --id EclipseAdoptium.Temurin.21.JRE -e --accept-source-agreements --accept-package-agreements
    ) else (
        echo [*] Lade OpenJDK 21 MSI direkt von Adoptium herunter...
        powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/installer/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse' -OutFile '$env:TEMP\temurin21.msi'; Start-Process msiexec.exe -ArgumentList '/i', '$env:TEMP\temurin21.msi', '/passive', '/norestart' -Wait"
    )
    echo [OK] Java 21+ Installation abgeschlossen.
)

:: ------------------------------------------------------------------------------
:: 2. Programmdateien nach %USERPROFILE%\jdmx herunterladen
:: ------------------------------------------------------------------------------
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

echo [*] Lade neueste jdmx-Version und Logo herunter...
powershell -NoProfile -Command ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "$rel = Invoke-RestMethod -Uri 'https://api.github.com/repos/%REPO%/releases/latest'; " ^
    "$asset = $rel.assets | Where-Object { $_.name -like 'jdmx-*-all.jar' } | Select-Object -First 1; " ^
    "if ($asset) { " ^
    "    Write-Host ('[*] Lade ' + $asset.name + '...'); " ^
    "    Invoke-WebRequest -Uri $asset.browser_download_url -OutFile ('%INSTALL_DIR%\' + $asset.name); " ^
    "    Copy-Item -Path ('%INSTALL_DIR%\' + $asset.name) -Destination '%INSTALL_DIR%\jdmx.jar' -Force; " ^
    "} else { " ^
    "    Write-Host '[!] Kein Release-JAR gefunden!' -ForegroundColor Red; " ^
    "}; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/jdmx.ico' -OutFile '%INSTALL_DIR%\jdmx.ico'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/jdmx.png' -OutFile '%INSTALL_DIR%\jdmx.png'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/uninstall.bat' -OutFile '%INSTALL_DIR%\uninstall.bat'"

if not exist "%INSTALL_DIR%\jdmx.jar" (
    echo [!] Fehler beim Herunterladen von jdmx.jar.
    pause
    exit /b 1
)

:: Startskript im Zielordner anlegen
(
echo @echo off
echo cd /d "%%~dp0"
echo start "" javaw -jar "%%~dp0jdmx.jar" %%*
) > "%INSTALL_DIR%\run.bat"

:: ------------------------------------------------------------------------------
:: 3. Startmenue- und Desktop-Verknuepfung mit Logo erstellen
:: ------------------------------------------------------------------------------
echo [*] Erstelle Startmenue- und Desktop-Eintrag mit Logo...
powershell -NoProfile -Command ^
    "$ws = New-Object -ComObject WScript.Shell; " ^
    "$javaw = (Get-Command javaw -ErrorAction SilentlyContinue).Source; " ^
    "if (-not $javaw) { $javaw = 'javaw.exe' }; " ^
    "$smPath = Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs\jdmx.lnk'; " ^
    "$dtPath = Join-Path ([Environment]::GetFolderPath('Desktop')) 'jdmx.lnk'; " ^
    "foreach ($lnkPath in @($smPath, $dtPath)) { " ^
    "  $s = $ws.CreateShortcut($lnkPath); " ^
    "  $s.TargetPath = $javaw; " ^
    "  $s.Arguments = '-jar \"' + '%INSTALL_DIR%\jdmx.jar' + '\"'; " ^
    "  $s.WorkingDirectory = '%INSTALL_DIR%'; " ^
    "  $s.IconLocation = '%INSTALL_DIR%\jdmx.ico,0'; " ^
    "  $s.Description = 'jdmx - Modular Java DMX Lighting Controller'; " ^
    "  $s.Save(); " ^
    "}"

echo ==================================================================
echo   [OK] Installation erfolgreich abgeschlossen!
echo   Ordner:      %INSTALL_DIR%
echo   Startmenue:  jdmx (mit Logo direkt im Startmenue & Desktop)
echo ==================================================================
pause
