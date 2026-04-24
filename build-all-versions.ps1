#!/usr/bin/env pwsh
# Build script for all supported Minecraft versions
# Usage: ./build-all-versions.ps1

$ErrorActionPreference = "Stop"

# Read supported versions from canonical versions.json
$versionsConfig = Get-Content -Path "versions.json" -Raw | ConvertFrom-Json
$versions = $versionsConfig.supported

# Output directory for collected JARs
$outputDir = "build/libs/all-versions"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Sortcraft Multi-Version Build Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Versions: $($versions -join ', ')" -ForegroundColor Cyan
Write-Host ""

# Clean output directory
if (Test-Path $outputDir) {
    Remove-Item -Path $outputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

# Build each version (tests run on all versions)
foreach ($version in $versions) {
    Write-Host ""
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    Write-Host "Building for Minecraft $version" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow

    # Clean before building each version to avoid conflicts
    & ./gradlew.bat clean "-Pmc_version=$version" --quiet

    # Build with tests for all versions
    & ./gradlew.bat build "-Pmc_version=$version"

    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Build failed for Minecraft $version" -ForegroundColor Red
        exit 1
    }

    # Copy JARs to output directory
    $fabricJar = Get-ChildItem -Path "fabric/build/libs" -Filter "*+$version.jar" | Where-Object { $_.Name -notmatch "(-sources|-dev|-shadow)" }
    $neoforgeJar = Get-ChildItem -Path "neoforge/build/libs" -Filter "*+$version.jar" | Where-Object { $_.Name -notmatch "(-sources|-dev|-shadow)" }

    if ($fabricJar) {
        Copy-Item -Path $fabricJar.FullName -Destination $outputDir
        Write-Host "  Copied: $($fabricJar.Name)" -ForegroundColor Green
    }

    if ($neoforgeJar) {
        Copy-Item -Path $neoforgeJar.FullName -Destination $outputDir
        Write-Host "  Copied: $($neoforgeJar.Name)" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Build Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Output JARs in: $outputDir" -ForegroundColor Green
Write-Host ""

# List all output JARs
Get-ChildItem -Path $outputDir -Filter "*.jar" | ForEach-Object {
    Write-Host "  - $($_.Name)" -ForegroundColor White
}
