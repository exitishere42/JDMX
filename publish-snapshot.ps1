param()
$ErrorActionPreference = "Stop"

Write-Host "[*] Erstelle Fat-JAR..." -ForegroundColor Cyan
mvn clean package -DskipTests

$jar = Get-ChildItem target\jdmx-*-all.jar | Select-Object -First 1
if (-not $jar) {
    Write-Error "Keine Fat-JAR in target/ gefunden."
}

Copy-Item $jar.FullName ".\jdmx-snapshot-all.jar" -Force

Write-Host "[*] Aktualisiere GitHub Snapshot Release..." -ForegroundColor Cyan
gh release delete snapshot --yes 2>$null
git tag -d snapshot 2>$null
git push origin :refs/tags/snapshot 2>$null

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

Write-Host "[OK] Snapshot Release erfolgreich aktualisiert!" -ForegroundColor Green
