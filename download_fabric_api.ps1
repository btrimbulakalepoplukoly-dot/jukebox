# Знаходить останню версію Fabric API під Minecraft 26.2 (Fabric loader) через
# публічний Modrinth API і качає її прямо в mods-папку Minecraft.
# Викликається з build_and_install.bat - руками запускати не треба.

$apiUrl = 'https://api.modrinth.com/v2/project/fabric-api/version?game_versions=["26.2"]&loaders=["fabric"]'

try {
    $versions = Invoke-RestMethod -Uri $apiUrl -UserAgent "ChaosJukeboxInstaller/1.0"
} catch {
    Write-Host "Не вдалось звʼязатись з Modrinth. Завантаж Fabric API вручну: https://modrinth.com/mod/fabric-api"
    exit 0
}

if (-not $versions -or $versions.Count -eq 0) {
    Write-Host "Modrinth не повернув жодної версії Fabric API для 26.2."
    Write-Host "Завантаж вручну: https://modrinth.com/mod/fabric-api"
    exit 0
}

$latest = $versions[0]
$file = $latest.files | Where-Object { $_.primary -eq $true } | Select-Object -First 1
if (-not $file) { $file = $latest.files[0] }

$modsDir = Join-Path $env:APPDATA ".minecraft\mods"
New-Item -ItemType Directory -Force -Path $modsDir | Out-Null

$outPath = Join-Path $modsDir $file.filename

if (Test-Path $outPath) {
    Write-Host "Fabric API вже встановлено: $($file.filename)"
} else {
    Write-Host "Качаю Fabric API: $($file.filename) ..."
    Invoke-WebRequest -Uri $file.url -OutFile $outPath
    Write-Host "Готово: $outPath"
}
