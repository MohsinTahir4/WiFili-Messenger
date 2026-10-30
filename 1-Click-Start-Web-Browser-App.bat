@echo off
setlocal
cd /d "%~dp0"
title WiFili Messenger Web Edition
echo ====================================================
echo 📶 Starting WiFili Messenger Web Edition...
echo ====================================================

if not exist "%~dp0WiFili-Messenger-Desktop.jar" (
    echo.
    echo ⚠️ Error: WiFili-Messenger-Desktop.jar not found in folder!
    echo Please ensure WiFili-Messenger-Desktop.jar is in this directory.
    echo.
    pause
    exit /b
)

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo.
    echo ⚠️ Java runtime not found. Opening Java download page...
    start https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b
)

start "" javaw -jar "%~dp0WiFili-Messenger-Desktop.jar" web
if %errorlevel% neq 0 (
    java -jar "%~dp0WiFili-Messenger-Desktop.jar" web
)
