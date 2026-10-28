@echo off
title WiFili Messenger - 1-Click Launcher
echo ====================================================
echo 📶 Starting WiFili Messenger Desktop...
echo ====================================================

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo.
    echo ⚠️ Java runtime not found on this PC.
    echo Please download and install Java 17 or higher from https://adoptium.net
    echo.
    pause
    exit /b
)

start "" javaw -jar "%~dp0WiFili-Messenger-Desktop.jar"
if %errorlevel% neq 0 (
    echo.
    echo Starting with fallback java runner...
    java -jar "%~dp0WiFili-Messenger-Desktop.jar"
)
