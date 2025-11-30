#!/usr/bin/env pwsh
# Build script for all supported Minecraft versions
# Usage: ./build-all-versions.ps1

$ErrorActionPreference = "Stop"

# Supported Minecraft versions
# Note: 1.21.2 and 1.21.3 are excluded due to incompatible API changes that require extensive refactoring
$versions = @("1.21.1", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10")

# Output directory for collected JARs
$outputDir = "build/libs/all-versions"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SortCraft Multi-Version Build Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Clean output directory
if (Test-Path $outputDir) {
    Remove-Item -Path $outputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

# Build each version
foreach ($version in $versions) {
    Write-Host ""
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    Write-Host "Building for Minecraft $version" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow

    # Clean before building each version to avoid conflicts
    & ./gradlew.bat clean "-Pmc_version=$version" --quiet

    # Build
    $result = & ./gradlew.bat build "-Pmc_version=$version"

    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Build failed for Minecraft $version" -ForegroundColor Red
        exit 1
    }

    # Copy JARs to output directory
    $fabricJar = Get-ChildItem -Path "fabric/build/libs" -Filter "*+$version.jar" | Where-Object { $_.Name -notmatch "(-sources|-dev)" }
    $neoforgeJar = Get-ChildItem -Path "neoforge/build/libs" -Filter "*+$version.jar" | Where-Object { $_.Name -notmatch "(-sources|-dev)" }

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

