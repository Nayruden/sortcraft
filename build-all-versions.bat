@echo off
REM Build all supported Minecraft versions using the Gradle buildAllVersions task.
REM The canonical version list is defined in versions.json.
REM Usage: build-all-versions.bat
REM
REM For PowerShell users: ./build-all-versions.ps1
REM Cross-platform:       ./gradlew buildAllVersions

call gradlew.bat buildAllVersions %*

