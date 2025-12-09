@echo off
REM Build script for all supported Minecraft versions
REM Usage: build-all-versions.bat

setlocal enabledelayedexpansion

echo ========================================
echo Sortcraft Multi-Version Build Script
echo ========================================
echo.

REM Supported Minecraft versions
set VERSIONS=1.21.1 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10

REM Output directory
set OUTPUT_DIR=build\libs\all-versions

REM Clean output directory
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"

REM Build each version
for %%v in (%VERSIONS%) do (
    echo.
    echo ----------------------------------------
    echo Building for Minecraft %%v
    echo ----------------------------------------

    call gradlew.bat clean -Pmc_version=%%v --quiet
    call gradlew.bat build -Pmc_version=%%v

    if errorlevel 1 (
        echo ERROR: Build failed for Minecraft %%v
        exit /b 1
    )

    REM Copy Fabric JAR
    for %%f in (fabric\build\libs\*+%%v.jar) do (
        echo %%f | findstr /v "\-sources \-dev" >nul
        if not errorlevel 1 (
            copy "%%f" "%OUTPUT_DIR%\" >nul
            echo   Copied: %%~nxf
        )
    )

    REM Copy NeoForge JAR
    for %%f in (neoforge\build\libs\*+%%v.jar) do (
        echo %%f | findstr /v "\-sources \-dev" >nul
        if not errorlevel 1 (
            copy "%%f" "%OUTPUT_DIR%\" >nul
            echo   Copied: %%~nxf
        )
    )
)

echo.
echo ========================================
echo Build Complete!
echo ========================================
echo.
echo Output JARs in: %OUTPUT_DIR%
echo.

dir /b "%OUTPUT_DIR%\*.jar"

endlocal

