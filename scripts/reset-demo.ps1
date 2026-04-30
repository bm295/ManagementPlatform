$ErrorActionPreference = "Stop"

Push-Location (Join-Path $PSScriptRoot "..")
try {
    Write-Host "Resetting demo containers and SQL volume..."
    docker compose down -v
    docker compose up --build -d
}
finally {
    Pop-Location
}
