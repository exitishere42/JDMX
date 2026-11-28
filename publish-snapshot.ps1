param()
$ErrorActionPreference = "Continue"

Write-Host "[*] Erstelle Fat-JAR..." -ForegroundColor Cyan
mvn clean package -DskipTests

$jar = Get-ChildItem target\jdmx-*-all.jar | Select-Object -First 1
if (-not $jar) {
    Write-Host "[!] Keine Fat-JAR in target/ gefunden." -ForegroundColor Red
    exit 1
}

Copy-Item $jar.FullName ".\jdmx-snapshot-all.jar" -Force

Write-Host "[*] Aktualisiere GitHub Snapshot Release..." -ForegroundColor Cyan

# Pruefen ob Snapshot-Release bereits existiert
$releaseExists = $false
try {
    gh release view snapshot >$null 2>&1
    if ($LASTEXITCODE -eq 0) {
        $releaseExists = $true
    }
} catch {}

if ($releaseExists) {
    gh release upload snapshot `
        ".\jdmx-snapshot-all.jar" `
        $jar.FullName `
        "install.bat" `
        "install.sh" `
        "uninstall.bat" `
        "uninstall.sh" `
        "update-snapshot.bat" `
        "update-snapshot.sh" `
        --clobber
} else {
    gh release create snapshot `
        ".\jdmx-snapshot-all.jar" `
        $jar.FullName `
        "install.bat" `
        "install.sh" `
        "uninstall.bat" `
        "uninstall.sh" `
        "update-snapshot.bat" `
        "update-snapshot.sh" `
        --title "JDMX Continuous Snapshot" `
        --notes "Automated rolling development snapshot for stage and testing devices." `
        --prerelease
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Snapshot Release erfolgreich aktualisiert!" -ForegroundColor Green
} else {
    Write-Host "[!] Fehler beim Aktualisieren des Snapshot Releases." -ForegroundColor Red
}
